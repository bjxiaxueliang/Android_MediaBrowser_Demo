package com.mediabrowser.xiaxl.client.model;


/**
 * 音乐信息
 * Created by xiaxl on 2017/9/7.
 */

public interface IMusicInfo {

    // 音频id
    String getMediaId();
    //播放地址
    String getSource();
    //音频封面
    String getArtUrl();
    //音频名称
    String getTitle();
    //音频描述
    String getDescription();
    //作者
    String getArtist();
    //合集名称
    String getAlbum();
    //合集封面
    String getAlbumArtUrl();
    //
    String getGenre();
    // 类型：付费 or 免费
    String freeType();
    // 返回 ms 数
    long getDuration();
}
