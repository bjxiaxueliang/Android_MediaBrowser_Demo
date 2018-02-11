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

import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;

import com.mediabrowser.xiaxl.client.utils.MusicMetadataConstant;
import com.mediabrowser.xiaxl.service.utils.QueueUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Simple data provider for queues. Keeps track of a current queue and a current index in the
 * queue. Also provides methods to set the current queue based on common queries, relying on a
 * given MusicProvider to provide the actual media metadata.
 */
public class MusicQueue {
    private static final String TAG = "MusicQueue";


    /**
     *
     */
    // 数据变化的观察者
    private MetadataUpdateListener mMetadataUpdateListener;
    // Resources
    private Resources mResources;


    /**
     * 上次数据
     */
    // 上次的播放队列
    private List<MediaSessionCompat.QueueItem> mLastQueue;
    // 上次的数据列表
    private LinkedHashMap<String, MediaMetadataCompat> mLastMusicListById;
    // 上次播放的index
    private int mLastIndex;

    /**
     * 当前播放数据
     */
    // 当前播放队列（同步队列）
    private List<MediaSessionCompat.QueueItem> mPlayingQueue;
    // 当前数据列表
    private LinkedHashMap<String, MediaMetadataCompat> mMusicListById;
    // 当前播放的index
    private int mCurrentIndex;


    /**
     * 构造方法
     *
     * @param resources
     * @param listener
     */
    public MusicQueue(@NonNull Resources resources, @NonNull MetadataUpdateListener listener) {
        this.mMetadataUpdateListener = listener;
        this.mResources = resources;
        /**
         * 当前
         */
        // 当前播放队列（同步队列） MediaSessionCompat.QueueItem
        mPlayingQueue = Collections.synchronizedList(new ArrayList<MediaSessionCompat.QueueItem>());
        // 当前数据列表
        mMusicListById = new LinkedHashMap<>();
        // 当前播放的index
        mCurrentIndex = 0;
        /**
         * 上次
         */
        // 上次的数据列表
        mLastMusicListById = new LinkedHashMap<>();

    }


    /**
     * 设置播放数据列表
     *
     * @param title 队列名
     * @param list  队列列表
     * @param index
     */
    public void setNewMediaMetadatas(String title, List<MediaMetadataCompat> list, int index) {
        // 暂存上次的播放队列
        mLastMusicListById.clear();
        mLastMusicListById.putAll(mMusicListById);
        // 清空队列
        mMusicListById.clear();
        for (MediaMetadataCompat item : list) {
            String musicId = item.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID);
            mMusicListById.put(musicId, item);
        }
        // MediaMetadataCompat 转化为 MediaSessionCompat.QueueItem
        List<MediaSessionCompat.QueueItem> queueItemList = QueueUtil.convertToQueue(mMusicListById.values());
        // 设置当前播放队列
        setNewQueue(title, queueItemList, index);
    }


    /**
     * 设置当前播放队列
     *
     * @param title
     * @param newQueue
     * @param index
     */
    private void setNewQueue(String title, List<MediaSessionCompat.QueueItem> newQueue, int index) {
        // 保存上次播放记录
        mLastQueue = mPlayingQueue;
        // 当前播放记录
        mPlayingQueue = newQueue;
        // 判断当前播放的index是否存在
        if (!QueueUtil.isIndexPlayable(index, mPlayingQueue)) {
            index = 0;
        }
        // 当前播放队列的index
        setCurrentQueueIndex(index);
        // 回调 播放队列发生变化
        mMetadataUpdateListener.onQueueUpdated(title, newQueue);
    }

    /**
     * 设置当前播放的index
     *
     * @param index
     * @return
     */
    private boolean setCurrentQueueIndex(int index) {
        // 如果当前index存在
        if (QueueUtil.isIndexPlayable(index, mPlayingQueue)) {
            // 保存上次播放index
            mLastIndex = mCurrentIndex;
            // 设置当前播放的index
            mCurrentIndex = index;
            // 返回上次播放的Item
            MediaSessionCompat.QueueItem lastMusic = getLastQueueItem();
            // 回调上次的播放数据
            if (lastMusic != null) {
                final String musicId = lastMusic.getDescription().getMediaId();
                MediaMetadataCompat metadata = mLastMusicListById.get(musicId);
                mMetadataUpdateListener.onBeforeMetadataChanged(metadata);
            }
            // 清空上次播放数据
            mLastMusicListById.clear();
            mLastQueue = null;
            mLastIndex = -1;
            // 回调当前播放数据
            callBackMetadaChanged();
            return true;
        }
        return false;
    }

    /**
     * 当前的MediaMetadataCompat
     *
     * @return
     */
    public MediaMetadataCompat getCurrentMetadata() {
        MediaSessionCompat.QueueItem currentQueueItem = getCurrentQueueItem();
        if (currentQueueItem == null)
            return null;
        else {
            final String musicId = currentQueueItem.getDescription().getMediaId();
            return mMusicListById.get(musicId);
        }
    }

    /**
     * Return the Music Source
     * if downloaded already, return the local file path
     * otherwise, return the music url
     * <p>
     * 获取网络播放地址
     */
    public String getMusicSource(String musicId) {
        // 音频数据
        MediaMetadataCompat track = mMusicListById.get(musicId);
        if (track == null) {
            return null;
        }
        // 获取网络播放地址
        return track.getString(MusicMetadataConstant.CUSTOM_METADATA_TRACK_SOURCE);
    }


    /**
     * 获取所有的MediaMetadata数据
     *
     * @return
     */
    public List<MediaMetadataCompat> getAllMediaMetadatas() {
        if (mMusicListById == null) {
            return Collections.emptyList();
        }
        List<MediaMetadataCompat> list = new ArrayList<>(mMusicListById.size());
        for (MediaMetadataCompat metadata : mMusicListById.values()) {
            list.add(metadata);
        }
        return list;
    }


    /**
     * 获取当前的 QueueItem
     *
     * @return
     */
    public MediaSessionCompat.QueueItem getCurrentQueueItem() {
        // 判断对应index的数据是否存在
        if (!QueueUtil.isIndexPlayable(mCurrentIndex, mPlayingQueue)) {
            return null;
        }
        // 根据index 取播放队列数据
        return mPlayingQueue.get(mCurrentIndex);
    }

    /**
     * 返回上次播放的 QueueItem
     *
     * @return
     */
    public MediaSessionCompat.QueueItem getLastQueueItem() {
        // 返回上次播放的Item
        if (QueueUtil.isIndexPlayable(mLastIndex, mLastQueue)) {
            return mLastQueue.get(mLastIndex);
        }
        //
        if (QueueUtil.isIndexPlayable(mLastIndex, mPlayingQueue)) {
            return mPlayingQueue.get(mLastIndex);
        }
        return null;
    }

    /**
     * 根据queueId查找对应的index，并设置当前播放的index
     *
     * @param queueId
     * @return
     */
    public boolean setCurrentQueueItem(long queueId) {
        // set the current index on queue from the queue Id:
        int index = QueueUtil.getMusicIndexOnQueue(mPlayingQueue, queueId);
        return setCurrentQueueIndex(index);
    }

    /**
     * 根据mediaId查找对应的index，并设置当前播放的index
     *
     * @param mediaId
     * @return
     */
    public boolean setCurrentQueueItem(String mediaId) {
        // set the current index on queue from the music Id:
        int index = QueueUtil.getMusicIndexOnQueue(mPlayingQueue, mediaId);
        return setCurrentQueueIndex(index);
    }

    /**
     * 跳过音频
     *
     * @param amount
     * @return
     */
    public boolean skipQueuePosition(int amount) {
        int index = mCurrentIndex + amount;
        if (!QueueUtil.isIndexPlayable(index, mPlayingQueue)) {
            return false;
        }
        setCurrentQueueIndex(index);
        return true;
    }


    // ##########################################################################################

    /**
     * 回调当前播放数据
     */
    private void callBackMetadaChanged() {
        // 获取当前的 QueueItem
        MediaSessionCompat.QueueItem currentMusic = getCurrentQueueItem();
        // 当前播放队列为null,回调数据错误
        if (currentMusic == null) {
            mMetadataUpdateListener.onMetadataRetrieveError();
            return;
        }
        // 回调当前播放数据
        final String musicId = currentMusic.getDescription().getMediaId();
        MediaMetadataCompat metadata = mMusicListById.get(musicId);
        if (metadata == null) {
            throw new IllegalArgumentException("Invalid musicId " + musicId);
        }
        // 回调数据变化
        mMetadataUpdateListener.onMetadataChanged(metadata);

    }


    // ##########################################################################################

    /**
     * 数据变化的接口类
     */
    public interface MetadataUpdateListener {
        // 数据变化之前
        void onBeforeMetadataChanged(MediaMetadataCompat metadata);

        // 数据变化
        void onMetadataChanged(MediaMetadataCompat metadata);

        // 数据检索失败
        void onMetadataRetrieveError();

        // 队列更新
        void onQueueUpdated(String title, List<MediaSessionCompat.QueueItem> newQueue);
    }
}
