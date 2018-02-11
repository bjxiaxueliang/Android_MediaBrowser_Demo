package com.mediabrowser.xiaxl.client.listener;

import android.support.v4.media.MediaMetadataCompat;

/**
 * Created by xiaxl on 2017/11/10.
 * <p>
 * 音频历史记录时间点监听
 */

public interface OnSaveRecordListener {
    // 记录音频播放记录
    void onSaveRecord(MediaMetadataCompat mediaMetadataCompat, long postion);
}
