package com.xiaxl.music;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.mediabrowser.xiaxl.client.MusicManager;
import com.mediabrowser.xiaxl.client.listener.OnSaveRecordListener;
import com.mediabrowser.xiaxl.setting.SettingConfig;
import com.xiaxl.music.bean.MusicInfo;
import com.xiaxl.music.widget.MediaSeekBar;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends Activity {


    /**
     * UI
     */
    // 歌曲标题
    private TextView mTitleTv;
    // 歌曲作者
    private TextView mArtistTv;
    // 歌曲图片
    private ImageView mAlbumArtImg;
    // 播放控制器 背景
    private View mControlBgLayout;

    // seekbar
    private MediaSeekBar mSeekBarAudio;

    /**
     * 数据
     */
    // 是否正在播放的标识
    private boolean mIsPlaying;
    // 音频数据
    List<MusicInfo> mMusicInfos = new ArrayList<MusicInfo>();

    /**
     *
     */
    MusicManager mMusicManager = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // 初始化音乐引擎
        initMusicAgent();
        // 添加音频数据
        initData();
        // 初始化UI
        initUI();

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        //
        if (mMusicManager != null) {
            mMusicManager.removeAudioStateListener(mAudioStatusChangeListener);
        }
    }

    /**
     * 初始化音乐引擎
     */
    private void initMusicAgent() {
        // 初始化
        if (mMusicManager == null) {
            mMusicManager = MusicManager.getInstance();
        }
        mMusicManager.init(this);
        // 音频变化的监听类
        mMusicManager.addOnAudioStatusListener(mAudioStatusChangeListener);
        // 记录播放记录的监听
        mMusicManager.addOnRecorListener(mOnRecordListener);
    }


    /**
     * 初始化数据
     */
    private void initData() {
        // 允许GPRS播放
        SettingConfig.setGPRSPlayAllowed(this, true);
        // 添加音频数据
        mMusicInfos.add(new MusicInfo());

    }


    /**
     * 初始化UI
     */
    private void initUI() {
        // 歌曲标题
        mTitleTv = (TextView) findViewById(R.id.song_title_tv);
        // 歌曲作者
        mArtistTv = (TextView) findViewById(R.id.song_artist_tv);
        // 歌曲图片
        mAlbumArtImg = (ImageView) findViewById(R.id.album_art_img);
        // 播放控制器背景
        mControlBgLayout = findViewById(R.id.control_bg_layout);

        // 上一首
        final Button previousBtn = (Button) findViewById(R.id.previous_btn);
        previousBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMusicManager != null) {
                    mMusicManager.skipToPrevious();
                }
            }
        });
        // 播放按钮
        final Button playBtn = (Button) findViewById(R.id.play_btn);
        playBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mIsPlaying) {
                    if (mMusicManager != null) {
                        mMusicManager.pause();
                    }
                } else {
                    if (mMusicManager != null) {
                        mMusicManager.playMusicList(mMusicInfos, 0);
                    }
                }
            }
        });
        // 下一首
        final Button nextBtn = (Button) findViewById(R.id.next_btn);
        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMusicManager != null) {
                    mMusicManager.skipToNext();
                }
            }
        });
        // seekbar
        mSeekBarAudio = (MediaSeekBar) findViewById(R.id.seekbar_audio);
        mSeekBarAudio.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // seek
                if (mMusicManager != null) {
                    mMusicManager.seekTo(seekBar.getProgress());
                }
            }
        });
    }


    /**
     * 更改播放按钮背景状态
     *
     * @param isPlaying
     */
    private void setControlBg(boolean isPlaying) {
        if (isPlaying) {
            mControlBgLayout.setBackgroundResource(R.drawable.ic_media_with_pause);
        } else {
            mControlBgLayout.setBackgroundResource(R.drawable.ic_media_with_play);
        }
    }


    // ############################################################################################


    /**
     * 音频播放状态变化的回调
     *
     * @param playbackState
     */
    private void onMediaPlaybackStateChanged(PlaybackStateCompat playbackState) {
        if (playbackState == null) {
            return;
        }
        // 正在播放
        mIsPlaying =
                playbackState.getState() == PlaybackStateCompat.STATE_PLAYING;

        // 更新UI
        setControlBg(mIsPlaying);

        /**
         * 设置播放进度
         */
        final int progress = (int) playbackState.getPosition();
        mSeekBarAudio.setProgress(progress);
        switch (playbackState.getState()) {
            case PlaybackStateCompat.STATE_PLAYING:
                final int timeToEnd = (int) ((mSeekBarAudio.getMax() - progress) / playbackState.getPlaybackSpeed());
                mSeekBarAudio.startProgressAnima(progress, mSeekBarAudio.getMax(), timeToEnd);
                break;
            case PlaybackStateCompat.STATE_PAUSED:
                mSeekBarAudio.stopProgressAnima();
                break;

        }

    }


    /**
     * 播放音频数据 发生变化的回调
     *
     * @param mediaMetadata
     */
    private void onMediaMetadataChanged(MediaMetadataCompat mediaMetadata) {
        if (mediaMetadata == null) {
            return;
        }
        // 音频的标题
        mTitleTv.setText(
                mediaMetadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE));
        // 音频作者
        mArtistTv.setText(
                mediaMetadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST));
        // 音频图片
//        mAlbumArtImg.setImageBitmap(MusicLibrary.getAlbumBitmap(
//                MainActivity.this,
//                mediaMetadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)));

        // 进度条
        final int max = mediaMetadata != null
                ? (int) mediaMetadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
                : 0;
        mSeekBarAudio.setProgress(0);
        mSeekBarAudio.setMax(max);
    }

    // ############################################################################################


    /**
     * 音频变化回调
     */
    MusicManager.OnAudioStatusChangeListener mAudioStatusChangeListener = new MusicManager.OnAudioStatusChangeListener() {
        @Override
        public void onPlaybackStateChanged(@NonNull PlaybackStateCompat state) {
            // 播放音频 状态变化
            onMediaPlaybackStateChanged(state);
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            // 播放音频变化的回调
            onMediaMetadataChanged(metadata);
        }

        @Override
        public void onQueueChanged(List<MediaSessionCompat.QueueItem> queue) {
            // TODO 播放队列发生变化
        }
    };

    /**
     * 记录播放位置的回调
     */
    OnSaveRecordListener mOnRecordListener = new OnSaveRecordListener() {
        @Override
        public void onSaveRecord(MediaMetadataCompat mediaMetadataCompat, long postion) {
            // TODO 保存播放记录用
        }
    };


}
