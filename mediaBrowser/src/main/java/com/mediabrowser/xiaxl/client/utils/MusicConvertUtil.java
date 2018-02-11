package com.mediabrowser.xiaxl.client.utils;

import android.support.v4.media.MediaMetadataCompat;


import com.mediabrowser.xiaxl.client.model.IMusicInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 类型转换工具
 * Created by xiaxl on 2017/9/9.
 */

public class MusicConvertUtil {

    /**
     * 数据列表转化
     *
     * @param list
     * @param <T>
     * @return
     */
    public static <T extends IMusicInfo> ArrayList<MediaMetadataCompat> convertToMediaMetadataList(List<T> list) {
        // 创建MediaMetadataCompat 播放队列
        ArrayList<MediaMetadataCompat> metaList = new ArrayList<>(list.size());
        // 队列中添加数据
        for (T item : list) {
            metaList.add(convertToMediaMetadata(item));
        }
        return metaList;
    }

    /**
     * {@link IMusicInfo} 转为{@link MediaMetadataCompat}
     *
     * @param info
     * @return
     */
    public static MediaMetadataCompat convertToMediaMetadata(IMusicInfo info) {
        //
        return new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, info.getMediaId())
                .putString(MusicMetadataConstant.CUSTOM_METADATA_PAY_TYPE, info.freeType())
                .putString(MusicMetadataConstant.CUSTOM_METADATA_TRACK_SOURCE, info.getSource())
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, info.getAlbum())
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, info.getArtist())
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION, info.getDescription())
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, info.getDuration())
                .putString(MediaMetadataCompat.METADATA_KEY_GENRE, info.getGenre())
                .putString(MediaMetadataCompat.METADATA_KEY_ART_URI, info.getArtUrl())
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, info.getAlbumArtUrl())
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, info.getTitle())
                .build();
    }
}
