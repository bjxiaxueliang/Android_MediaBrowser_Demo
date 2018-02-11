package com.xiaxl.music.bean;


import com.mediabrowser.xiaxl.client.model.IMusicInfo;

public class MusicInfo implements IMusicInfo {


    @Override
    public String getDescription() {
        return "满满的青春，满满的能量";
    }

    @Override
    public String getMediaId() {
        return "adfadfadsf";
    }

    @Override
    public String getSource() {
        return "http://nos.netease.com/test-open-audio/nos/mp3/2017/12/04/AT08IH87FHUG1LNA_sd.mp3";
    }

    @Override
    public String getArtUrl() {
        return "http://d040779c2cd49.scdn.itc.cn/s_w_z/pic/20161213/184474627999795968.jpg";
    }

    @Override
    public String getTitle() {
        return "满满的青春，满满的能量";
    }

    @Override
    public String getArtist() {
        return "满满的青春，满满的能量";
    }

    @Override
    public String getAlbum() {
        return "满满的青春，满满的能量";
    }

    @Override
    public String getAlbumArtUrl() {
        return "http://d040779c2cd49.scdn.itc.cn/s_w_z/pic/20161213/184474627999795968.jpg";
    }

    @Override
    public String getGenre() {
        return "";
    }

    @Override
    public String freeType() {
        return "1";
    }

    @Override
    public long getDuration() {
        return 277 * 1000;
    }


}
