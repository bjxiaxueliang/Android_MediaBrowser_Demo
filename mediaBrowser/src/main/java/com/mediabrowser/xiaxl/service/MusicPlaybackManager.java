/*
* Copyright (C) 2014 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.mediabrowser.xiaxl.service;

import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import com.mediabrowser.xiaxl.client.MusicManager;
import com.mediabrowser.xiaxl.service.playback.Playback;

import java.util.List;


/**
 * Manage the interactions among the container service, the queue manager and the actual playback.
 */
public class MusicPlaybackManager implements Playback.PlaybackCallback {

    private static final String TAG = "MusicPlaybackManager";

    /**
     * action
     */
    // 播放音频列表
    public static final String CUSTOM_ACTION_MUSIC_PLAY_QUNEN = "com.netease.awakeing.music.MUSIC_QUEUE_PLAY";
    // 更新队列
    public static final String CUSTOM_ACTION_MUSIC_UPDATE_QUNEN = "com.netease.awakeing.music.MUSIC_QUEUE_UPDATE";
    // 重置队列
    public static final String CUSTOM_ACTION_MUSIC_QUEUE_RESET = "com.netease.awakeing.music.MUSIC_QUEUE_RESET";

    /**
     * key
     */
    // 音频队列数据
    public static final String KEY_MUSIC_QUEUE = "com.netease.awakeing.music.KEY_MUSIC_QUEUE";
    // 音频队列的title数据
    public static final String KEY_MUSIC_QUEUE_TITLE = "com.netease.awakeing.music.KEY_MUSIC_QUEUE_TITLE";
    // 播放index，小于0表示不播
    public static final String KEY_MUSIC_QUEUE_PLAY_INDEX = "com.netease.awakeing.music.KEY_MUSIC_QUEUE_PLAY_INDEX";


    /**
     *
     */
    private Context mContext = null;
    // 音频播放队列
    private MusicQueue mMusicQueue;
    // 播放器的封装类
    private Playback mMusicPlayback;

    // 用户通过MediaControllerCompat对UI的操作，
    // 会通过MediaSessionCompat.Callback 回调到Service端，
    // 来操纵“播放器”进行播放、暂定、快进、上一曲、下一曲等操作
    private MediaSessionCallback mMediaSessionCallback;

    // 回调到service的播放状态
    private PlaybackServiceCallback mServiceCallback;

    /**
     * 数据
     */
    // 正在播放的MediaId
    private String mPlayingMediaId = "";


    /**
     * 构造方法
     *
     * @param context
     * @param serviceCallback
     * @param musicQueue
     * @param playback
     */
    public MusicPlaybackManager(Context context, PlaybackServiceCallback serviceCallback, MusicQueue musicQueue, Playback playback) {
        this.mContext = context;
        mServiceCallback = serviceCallback;
        mMusicQueue = musicQueue;
        mMediaSessionCallback = new MediaSessionCallback();
        // 播放
        mMusicPlayback = playback;
        mMusicPlayback.setCallback(this);
    }


    // #########################################################################################

    /**
     * 播放音频
     */
    public void handlePlayRequest() {
        // 当前音频
        MediaSessionCompat.QueueItem currentMusic = mMusicQueue.getCurrentQueueItem();
        //
        if (currentMusic != null) {
            // 回调Service播放开始
            mServiceCallback.onPlaybackStart();
            // 获取当前播放的音频id
            String toPlayMediaId = currentMusic.getDescription().getMediaId();
            //
            if (mPlayingMediaId.equals(toPlayMediaId)) {
                // 当前正在播放状态  do noting
                if (mMusicPlayback.getState() == PlaybackStateCompat.STATE_PLAYING) {
                }
                // 当前为暂停状态，则播放
                else if (mMusicPlayback.getState() == PlaybackStateCompat.STATE_PAUSED) {
                    mMusicPlayback.start();
                }
                // 其他状态，则播放该音频
                else {
                    mMusicPlayback.play(mMusicQueue.getMusicSource(toPlayMediaId));
                }
            }
            // 播放该音频
            else {
                mMusicPlayback.play(mMusicQueue.getMusicSource(toPlayMediaId));
            }
            // 当前播放的音频
            mPlayingMediaId = currentMusic.getDescription().getMediaId();
        }
    }

    /**
     * 暂停
     */
    public void handlePauseRequest() {
        if (mMusicPlayback.isPlaying()) {
            mMusicPlayback.pause();
            mServiceCallback.onPlaybackPause();
        }
    }

    /**
     * 停止播放
     *
     * @param withError
     */
    public void handleStopRequest(String withError) {
        // 停止音频播放
        mMusicPlayback.stop(true);
        // 回调Service 播放停止
        mServiceCallback.onPlaybackStop();
        // 回调播放状态
        callbackServicePlaybackState(withError);
    }


    /**
     * 播放音频列表
     *
     * @param extras
     */
    private void playMusicQueue(Bundle extras) {
        if (extras == null) {
            return;
        }
        extras.setClassLoader(MediaDescriptionCompat.class.getClassLoader());
        // 列表数据
        List<MediaMetadataCompat> list = extras.getParcelableArrayList(KEY_MUSIC_QUEUE);
        if (list == null) {
            return;
        }
        // 标题
        String title = extras.getString(KEY_MUSIC_QUEUE_TITLE, "new queue");
        // 播放的index
        int index = extras.getInt(KEY_MUSIC_QUEUE_PLAY_INDEX, -1);
        // 回调 保存播放记录
        callbackClient2SavePlayRecord();
        // 设置播放队列
        mMusicQueue.setNewMediaMetadatas(title, list, index);
        // 播放音频
        if (index >= 0 && index < list.size()) {
            handlePlayRequest();
        }
    }

    /**
     * 更新播放队列
     *
     * @param extras
     */
    private void updateMusicQueue(Bundle extras) {
        //
        if (extras == null) {
            return;
        }
        extras.setClassLoader(MediaDescriptionCompat.class.getClassLoader());
        // 列表数据
        List<MediaMetadataCompat> list = extras.getParcelableArrayList(KEY_MUSIC_QUEUE);
        if (list == null) {
            return;
        }
        // 标题
        String title = extras.getString(KEY_MUSIC_QUEUE_TITLE, "new queue");
        // 播放的index
        int index = extras.getInt(KEY_MUSIC_QUEUE_PLAY_INDEX, -1);
        // 设置播放队列
        mMusicQueue.setNewMediaMetadatas(title, list, index);
    }


    /**
     * 重置播放队列
     */
    public void handleResetPlayerQueue(Bundle extras) {
        handleStopRequest("队列重置");
        if (extras == null) {
            return;
        }
        extras.setClassLoader(MediaDescriptionCompat.class.getClassLoader());
        // 获取音频队列数据
        List<MediaMetadataCompat> list = extras.getParcelableArrayList(KEY_MUSIC_QUEUE);
        if (list == null) {
            return;
        }
        // 队列标题
        String title = extras.getString(KEY_MUSIC_QUEUE_TITLE, "new queue");
        // 播放的index
        int index = extras.getInt(KEY_MUSIC_QUEUE_PLAY_INDEX, -1);
        // 回调 保存播放记录
        callbackClient2SavePlayRecord();
        // 设置播放队列
        mMusicQueue.setNewMediaMetadatas(title, list, index);
    }


    // #########################################################################################


    /**
     * 用户通过MediaControllerCompat对UI的操作，
     * 会通过MediaSessionCompat.Callback 回调到Service端，
     * 来操纵“播放器”进行播放、暂定、快进、上一曲、下一曲等操作
     */
    private class MediaSessionCallback extends MediaSessionCompat.Callback {
        /**
         * client 端播放音频
         */
        @Override
        public void onPlay() {
            // 播放音频
            handlePlayRequest();
        }

        @Override
        public void onSkipToQueueItem(long queueId) {
            // 保存播放记录
            callbackClient2SavePlayRecord();
            // 播放音频
            if (mMusicQueue.setCurrentQueueItem(queueId)) {
                handlePlayRequest();
            }
        }

        @Override
        public void onSeekTo(long position) {
            mMusicPlayback.seekTo((int) position);
        }

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            //
            callbackClient2SavePlayRecord();
            //
            if (mMusicQueue.setCurrentQueueItem(mediaId)) {
                handlePlayRequest();
            }
        }

        @Override
        public void onPause() {
            handlePauseRequest();
        }

        @Override
        public void onStop() {
            handleStopRequest(null);
        }

        @Override
        public void onSkipToNext() {
            //
            callbackClient2SavePlayRecord();
            //
            if (mMusicQueue.skipQueuePosition(1)) {
                handlePlayRequest();
            }
        }

        @Override
        public void onSkipToPrevious() {
            //
            callbackClient2SavePlayRecord();
            //
            if (mMusicQueue.skipQueuePosition(-1)) {
                handlePlayRequest();
            }
        }

        @Override
        public void onCustomAction(@NonNull String action, Bundle extras) {
            // 更新播放队列
            if (CUSTOM_ACTION_MUSIC_UPDATE_QUNEN.equals(action)) {
                updateMusicQueue(extras);
            }
            // 播放音频列表
            else if (CUSTOM_ACTION_MUSIC_PLAY_QUNEN.equals(action)) {
                playMusicQueue(extras);
            }
            // 重置播放队列
            else if (CUSTOM_ACTION_MUSIC_QUEUE_RESET.equals(action)) {
                handleResetPlayerQueue(extras);
            }
        }
    }


    public Playback getPlayback() {
        return mMusicPlayback;
    }

    /**
     * 用户通过MediaControllerCompat对UI的操作，
     * 会通过MediaSessionCompat.Callback 回调到Service端，
     * 来操纵“播放器”进行播放、暂定、快进、上一曲、下一曲等操作
     *
     * @return
     */
    public MediaSessionCompat.Callback getMediaSessionCallback() {
        return mMediaSessionCallback;
    }


    // ############################################################################################


    /**
     * Implementation of the Playback.PlaybackCallback interface
     * <p>
     * {@link Playback.PlaybackCallback} 回调音频播放状态的方法
     * <p>
     * 音频播放完成的回调
     */
    @Override
    public void onCompletion() {
        // 保存播放记录
        callbackClient2SavePlayRecord();
        // The media player finished playing the current song, so we go ahead and start the next.
        if (mMusicQueue.skipQueuePosition(1)) {
            handlePlayRequest();
        } else {
            // 停止音频播放
            // If skipping was not possible, we stop and release the resources:
            handleStopRequest(null);
        }
    }

    /**
     * Implementation of the Playback.PlaybackCallback interface
     * <p>
     * {@link Playback.PlaybackCallback} 回调音频播放状态的方法
     *
     * @param state
     */
    @Override
    public void onPlaybackStatusChanged(int state) {
        callbackServicePlaybackState(null);
        //
        if (state == PlaybackStateCompat.STATE_PAUSED || state == PlaybackStateCompat.STATE_STOPPED) {
            callbackClient2SavePlayRecord();
        }

    }

    /**
     * Implementation of the Playback.PlaybackCallback interface
     * <p>
     * {@link Playback.PlaybackCallback} 回调音频播放状态的方法
     *
     * @param error to be added to the PlaybackState
     */
    @Override
    public void onError(String error) {
        callbackServicePlaybackState(error);
        // 保存播放记录
        callbackClient2SavePlayRecord();
    }


    // ############################################################################################

    /**
     *
     */
    public interface PlaybackServiceCallback {
        void onPlaybackStart();

        void onNotificationRequired();

        void onPlaybackPause();

        void onPlaybackStop();

        void onPlaybackStateUpdated(PlaybackStateCompat newState);
    }


    // ############################################################################################

    /**
     * 回调到Client  保存播放记录
     * <p>
     * 1、播放新音频时，保存上一音频的播放记录
     */
    public void callbackClient2SavePlayRecord() {
        // 回调到client端 保存当前播放记录
        if (mMusicPlayback != null && mMusicPlayback.isConnected() && MusicManager.getInstance().getRecordListener() != null) {
            MediaMetadataCompat mediaMetadataCompat = mMusicQueue.getCurrentMetadata();
            if (mediaMetadataCompat == null) {
                return;
            }
            long position = mMusicPlayback.getCurrentStreamPosition();
            // 回调到client端 保存当前播放记录
            MusicManager.getInstance().getRecordListener().onSaveRecord(mMusicQueue.getCurrentMetadata(), position);
        }
    }

    // ############################################################################################


    /**
     * 回调到service 当前音频的播放状态
     * <p>
     * Update the current media player state, optionally showing an error message.
     *
     * @param error if not null, error message to present to the user.
     */
    public void callbackServicePlaybackState(String error) {
        // 当前播放到的位置
        long position = PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN;
        // 当前播放到的位置
        if (mMusicPlayback != null && mMusicPlayback.isConnected()) {
            position = mMusicPlayback.getCurrentStreamPosition();
        }
        // 当前播放状态
        int state = mMusicPlayback.getState();

        /**
         * 回调到service 当前音频的播放状态
         */
        // 创建PlaybackStateCompat
        //noinspection ResourceType
        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder()
                .setActions(getAvailableActions());
        // 设置错误信息
        // If there is an error message, send it to the playback state:
        if (error != null) {
            // Error states are really only supposed to be used for errors that cause playback to
            // stop unexpectedly and persist until the user takes action to fix it.
            stateBuilder.setErrorMessage(error);
            state = PlaybackStateCompat.STATE_ERROR;
        }
        // 设置当前状态
        //noinspection ResourceType
        stateBuilder.setState(state, position, mMusicPlayback.getSpeed(), SystemClock.elapsedRealtime());
        // 当前播放id
        // Set the activeQueueItemId if the current index is valid.
        MediaSessionCompat.QueueItem currentMusic = mMusicQueue.getCurrentQueueItem();
        //
        if (currentMusic != null) {
            stateBuilder.setActiveQueueItemId(currentMusic.getQueueId());
        }
        // 回调当前数据
        mServiceCallback.onPlaybackStateUpdated(stateBuilder.build());
        //
        if (state == PlaybackStateCompat.STATE_PLAYING ||
                state == PlaybackStateCompat.STATE_PAUSED) {
            mServiceCallback.onNotificationRequired();
        }
    }

    // ############################################################################################


    /**
     * 可用的播放状态
     *
     * @return
     */
    private long getAvailableActions() {
        // 可用的播放状态
        long actions =
                PlaybackStateCompat.ACTION_PLAY_PAUSE |
                        PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID |
                        PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH |
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT;

        // 这五行代码根本就没有作用
        if (mMusicPlayback.isPlaying()) {
            actions |= PlaybackStateCompat.ACTION_PAUSE;
        } else {
            actions |= PlaybackStateCompat.ACTION_PLAY;
        }
        return actions;
    }


}
