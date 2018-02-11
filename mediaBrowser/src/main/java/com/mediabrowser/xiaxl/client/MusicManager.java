package com.mediabrowser.xiaxl.client;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;

import com.mediabrowser.xiaxl.client.listener.OnSaveRecordListener;
import com.mediabrowser.xiaxl.client.utils.MusicConvertUtil;
import com.mediabrowser.xiaxl.client.utils.NetUtils;
import com.mediabrowser.xiaxl.service.MusicService;
import com.mediabrowser.xiaxl.client.model.IMusicInfo;
import com.mediabrowser.xiaxl.service.MusicPlaybackManager;
import com.mediabrowser.xiaxl.setting.SettingConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * 音频播放管理类
 */
public class MusicManager {

    private static final String TAG = "MusicManager";


    private static MusicManager instance = null;

    public static MusicManager getInstance() {
        if (instance == null) {
            synchronized (MusicManager.class) {
                if (instance == null)
                    instance = new MusicManager();
            }
        }
        return instance;
    }


    // ###########################################################################################

    /**
     * obj
     */
    // 上下文对象
    private Context mContext = null;
    // MediaBrowserCompat 客户端
    private MediaBrowserCompat mMediaBrowser;
    // MediaControllerCompat
    private MediaControllerCompat mMediaController;
    // TransportControls
    private MediaControllerCompat.TransportControls mTransportControls;

    /**
     * 数据
     */
    //当前播放列表
    private List<?> mPlayList;

    // ####################################对外暴露的方法##############################################


    /**
     * 是否正在播放
     */
    public boolean isPlaying() {
        if (mMediaController == null)
            return false;
        PlaybackStateCompat state = mMediaController.getPlaybackState();
        if (state == null) {
            return false;
        }
        // 正在播放 或者 正在缓冲 ，则认为是在播放
        return state.getState() == PlaybackStateCompat.STATE_PLAYING
                || state.getState() == PlaybackStateCompat.STATE_BUFFERING;
    }


    /**
     * 当前播放 音频 mediaId
     */
    public String getCurrentPlayMediaId() {
        if (mMediaController == null)
            return null;
        MediaMetadataCompat metadata = mMediaController.getMetadata();
        if (metadata == null)
            return null;
        return metadata.getDescription().getMediaId();
    }


    /**
     * 播放单曲
     */
    public void playMusic(IMusicInfo musicInfo) {
        if (musicInfo == null) {
            return;
        }
        List<IMusicInfo> list = new ArrayList<>(1);
        list.add(musicInfo);
        playMusicList(list, 0);
    }

    /**
     * 播放列表
     *
     * @param playIndex 当前播放位置
     */
    public <T extends IMusicInfo> void playMusicList(final List<T> list, final int playIndex) {
        // 数据为空
        if (list == null || list.isEmpty()) {
            return;
        }
        //
        if (mMediaController == null) {
            return;
        }
        //相同的列表则不刷新播放队列
        if (mPlayList != null && list.equals(mPlayList)) {
            if (playIndex >= 0 || playIndex < mPlayList.size())
                mMediaController.getTransportControls().playFromMediaId(list.get(playIndex).getMediaId(), null);
        }
        // 当前没有播放数据
        else {
            // 列表赋值
            mPlayList = list;
            // 设置数据
            Bundle args = new Bundle();
            args.putParcelableArrayList(MusicPlaybackManager.KEY_MUSIC_QUEUE, MusicConvertUtil.convertToMediaMetadataList(list));
            args.putInt(MusicPlaybackManager.KEY_MUSIC_QUEUE_PLAY_INDEX, playIndex);
            // 播放队列数据
            mMediaController.getTransportControls()
                    .sendCustomAction(MusicPlaybackManager.CUSTOM_ACTION_MUSIC_PLAY_QUNEN, args);
        }
    }

    /**
     * 更新播放队列
     *
     * @param playIndex 当前播放位置
     */
    public <T extends IMusicInfo> void updatePlayList(final List<T> list, final int playIndex) {
        if (list == null || list.isEmpty()) return;
        if (mMediaController == null) return;
        //相同的列表则不刷新播放队列
        if (mPlayList != null && list.equals(mPlayList)) {
        } else {
            mPlayList = list;
            Bundle args = new Bundle();
            args.putParcelableArrayList(MusicPlaybackManager.KEY_MUSIC_QUEUE, MusicConvertUtil.convertToMediaMetadataList(list));
            args.putInt(MusicPlaybackManager.KEY_MUSIC_QUEUE_PLAY_INDEX, playIndex);
            mMediaController.getTransportControls()
                    .sendCustomAction(MusicPlaybackManager.CUSTOM_ACTION_MUSIC_UPDATE_QUNEN, args);
        }
    }

    /**
     * 播放
     */
    public void play() {
        if (mTransportControls != null)
            mTransportControls.play();
    }

    /**
     * 暂停
     */
    public void pause() {
        if (mTransportControls != null)
            mTransportControls.pause();
    }

    /**
     * 停止
     */
    public void stop() {
        if (mTransportControls != null)
            mTransportControls.stop();
    }

    /**
     * 上一首
     */
    public void skipToPrevious() {
        if (mTransportControls != null) {
            mTransportControls.skipToPrevious();
        }
    }

    /**
     * 下一首
     */
    public void skipToNext() {
        if (mTransportControls != null) {
            mTransportControls.skipToNext();
        }

    }

    /**
     * seek
     */
    public void seekTo(long pos) {
        if (mTransportControls != null) {
            mTransportControls.seekTo(pos);
        }

    }


    // #########################################初始化###############################################

    /**
     * 初始化
     *
     * @param context
     */
    public void init(Context context) {
        this.mContext = context;
        // uid 包名 判断
        if (!isValidPackage(mContext.getPackageName(), Binder.getCallingUid())) {
            return;
        }
        // 创建MediaBrowserCompat
        this.mMediaBrowser = new MediaBrowserCompat(
                //
                mContext,
                //
                new ComponentName(mContext, MusicService.class),
                // 音频连接状态回调
                mConnectionCallback,
                //
                null);
        // 链接service
        connect();
    }

    /**
     * 链接 {@link MusicService}
     */
    public void connect() {
        if (mMediaBrowser == null) {
            return;
        }
        if (!mMediaBrowser.isConnected()) {
            try {
                mMediaBrowser.connect();
            } catch (Exception e) {
                Log.e(TAG, "connect failed : \n" + e.getMessage());
            }
        }
    }

    /**
     * 断开链接
     */
    public void disconnect() {
        if (mMediaController != null) {
            mMediaController.unregisterCallback(mMediaControllerCallback);
            mMediaController = null;
        }
        if (mMediaBrowser.isConnected()) {
            mMediaBrowser.disconnect();
        }
    }

    /**
     * 链接状态
     *
     * @return
     */
    public boolean isConnected() {
        return mMediaBrowser.isConnected();
    }


    // ####################################音频状态变化回调##########################################

    /**
     * 音频播放状态变化的监听者
     */
    private List<OnAudioStatusChangeListener> mAudioChangeListeners = new ArrayList<>();


    /**
     *
     */
    public interface OnAudioStatusChangeListener {

        /**
         * 播放状态修改
         */
        void onPlaybackStateChanged(@NonNull PlaybackStateCompat state);

        /**
         * 当前播放歌曲信息修改
         */
        void onMetadataChanged(MediaMetadataCompat metadata);

        /**
         * 播放队列修改
         */
        void onQueueChanged(List<MediaSessionCompat.QueueItem> queue);
    }


    /**
     * 添加监听
     *
     * @param l
     */
    public void addOnAudioStatusListener(OnAudioStatusChangeListener l) {
        mAudioChangeListeners.add(l);
        // 添加监听  会回调一次当前播放状态
        if (mMediaController != null) {
            // 回到 播放状态
            l.onPlaybackStateChanged(getPlaybackState());
            // 回调 数据变化
            l.onMetadataChanged(getMediaMetadata());
            // 回调 播放队列
            l.onQueueChanged(getQueue());
        }
    }

    /**
     * 移除监听
     *
     * @param l
     */
    public void removeAudioStateListener(OnAudioStatusChangeListener l) {
        mAudioChangeListeners.remove(l);
    }


    /**
     * 播放状态、当前播放信息修改回调
     */
    private final MediaControllerCompat.Callback mMediaControllerCallback =
            new MediaControllerCompat.Callback() {
                @Override
                public void onPlaybackStateChanged(@NonNull PlaybackStateCompat state) {
                    Log.d(TAG, "mediaControllerCallback.onPlaybackStateChanged: " +
                            "state is " + state.getState());
                    MusicManager.this.onPlaybackStateChanged(state);
                }

                @Override
                public void onMetadataChanged(MediaMetadataCompat metadata) {
                    Log.d(TAG, "mediaControllerCallback.onMetadataChanged: " + metadata);
                    MusicManager.this.onMetadataChanged(metadata);
                }

                @Override
                public void onQueueChanged(List<MediaSessionCompat.QueueItem> queue) {
                    Log.d(TAG, "mediaControllerCallback.onQueueChanged: " + (queue == null ? "null" : queue.size()));
                    MusicManager.this.onQueueChanged(queue);
                }
            };

    /**
     * 回调：播放状态变化
     *
     * @param state
     */
    private void onPlaybackStateChanged(@NonNull PlaybackStateCompat state) {
        try {
            for (OnAudioStatusChangeListener l : mAudioChangeListeners) {
                l.onPlaybackStateChanged(state);
            }
        } catch (Exception e) {
        }
    }

    /**
     * 回调：音频数据变化
     *
     * @param metadata
     */
    private void onMetadataChanged(MediaMetadataCompat metadata) {
        try {
            for (OnAudioStatusChangeListener l : mAudioChangeListeners) {
                l.onMetadataChanged(metadata);
            }
        } catch (Exception e) {
        }
    }

    /**
     * 回调：播放队列变化
     *
     * @param queue
     */
    private void onQueueChanged(List<MediaSessionCompat.QueueItem> queue) {
        try {
            for (OnAudioStatusChangeListener l : mAudioChangeListeners) {
                l.onQueueChanged(queue);
            }
        } catch (Exception e) {

        }
    }

    // #################################Service连接成功回调###########################################


    /**
     * 音乐播放服务连接回调
     */
    private final MediaBrowserCompat.ConnectionCallback mConnectionCallback =
            new MediaBrowserCompat.ConnectionCallback() {
                @Override
                public void onConnected() {
                    Log.d(TAG, "onConnected");
                    // 连接成功
                    try {
                        connectToSession(mMediaBrowser.getSessionToken());
                    } catch (RemoteException e) {
                        Log.e(TAG, "could not connect media controller");
                    } catch (Exception e) {
                        Log.e(TAG, "could not connect media controller");
                    }
                }
            };


    /**
     * 创建 MediaControllerCompat
     * 创建 TransportControls
     * MediaSessionCompat 与 MediaControllerCompat 关联
     *
     * @param token
     * @throws RemoteException
     */
    private void connectToSession(MediaSessionCompat.Token token) throws RemoteException {
        /**
         * 获取 MediaControllerCompat
         * 获取 TransportControls
         */
        // MediaControllerCompat
        mMediaController = new MediaControllerCompat(mContext, token);
        // 音频变化监听
        mMediaController.registerCallback(mMediaControllerCallback);
        // 获取TransportControls
        mTransportControls = mMediaController.getTransportControls();
        /**
         * 回调 音频变化
         */
        // 回调播放状态
        onPlaybackStateChanged(getPlaybackState());
        // 回调播放数据
        onMetadataChanged(getMediaMetadata());
        // 回调播放队列
        List<MediaSessionCompat.QueueItem> queue = getQueue();
        Log.d(TAG, "queue : " + (queue == null ? "null" : "size = " + queue.size()));
        onQueueChanged(queue);
    }


    /**
     * 回调播放队列
     *
     * @return
     */
    public List<MediaSessionCompat.QueueItem> getQueue() {
        if (mMediaController == null)
            return null;
        return mMediaController.getQueue();
    }

    /**
     * 播放队列
     *
     * @return
     */
    public boolean hasPlayQueue() {
        List<MediaSessionCompat.QueueItem> queue = getQueue();
        return queue != null && !queue.isEmpty();
    }


    /**
     * 当前播放状态
     *
     * @return
     */
    public PlaybackStateCompat getPlaybackState() {
        if (mMediaController == null)
            return null;
        return mMediaController.getPlaybackState();
    }

    /**
     * 当前播放数据
     *
     * @return
     */
    public MediaMetadataCompat getMediaMetadata() {
        if (mMediaController == null)
            return null;
        return mMediaController.getMetadata();
    }


    // ###########################################播放记录回调############################################

    /**
     * {@link MusicPlaybackManager}
     * <p>
     * 在音频队列发生变化 和 音频播放停止时，回调用该方法，client端即可保存播放记录
     *
     * @return
     */
    private OnSaveRecordListener mRecordListener = null;

    /**
     * 设置播放记录回调
     *
     * @param recordListener
     */
    public void addOnRecorListener(OnSaveRecordListener recordListener) {
        this.mRecordListener = recordListener;
    }

    /**
     * {@link MusicPlaybackManager}
     * <p>
     * 在音频队列发生变化 和 音频播放停止时，回调用该方法，client端即可保存播放记录
     *
     * @return
     */
    public OnSaveRecordListener getRecordListener() {
        return mRecordListener;
    }


    /**
     * ------------------------移动网络播放-----------------------------------------------------------
     */

    /**
     * 网络是否允许播放
     */
    public boolean isNetWorkAllow() {
        if (!NetUtils.isConnected(mContext)) {
            return false;
        }
        return NetUtils.isWIFI(mContext)
                || SettingConfig.isGPRSPlayAllowed(mContext);
    }

    /**
     * 是否允许播放
     */
    public boolean isAllowPlay() {
        return isNetWorkAllow();
    }


    public String getVersion() {
        return "1.0.0";
    }


    // ##########################################################################################

    /**
     * Return whether the given package is one of the ones that is owned by the uid.
     * <p>
     * 根据uid 判断给定的包名
     */
    private boolean isValidPackage(String pkg, int uid) {
        // 包名为空
        if (TextUtils.isEmpty(pkg)) {
            return false;
        }
        // PackageManager
        final PackageManager pm = mContext.getPackageManager();
        final String[] packages = pm.getPackagesForUid(uid);
        final int N = packages.length;
        for (int i = 0; i < N; i++) {
            if (packages[i].equals(pkg)) {
                return true;
            }
        }
        return false;
    }

}
