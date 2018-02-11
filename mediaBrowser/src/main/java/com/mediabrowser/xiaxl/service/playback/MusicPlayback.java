package com.mediabrowser.xiaxl.service.playback;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import com.mediabrowser.xiaxl.service.MusicService;

import java.io.IOException;

import static android.media.AudioManager.AUDIOFOCUS_REQUEST_GRANTED;


public class MusicPlayback implements Playback, AudioManager.OnAudioFocusChangeListener,
        MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnSeekCompleteListener {

    private static final String TAG = "MusicPlayback";

    // 失去焦点时，降低音量后的音量
    // The volume we set the media player to when we lose audio focus, but are
    // allowed to reduce the volume instead of stopping playback.
    public static final float VOLUME_DUCK = 0.2f;
    // 默认的音量 0~1之间
    // The volume we set the media player when we have audio focus.
    public static final float VOLUME_NORMAL = 1.0f;


    /**
     * obj
     */
    // 上下文对象
    private final Context mContext;
    // 音频播放器
    private MediaPlayer mMediaPlayer;
    // AudioManager
    private AudioManager mAudioManager;
    // 保持wifi连接状态的WifiLock
    private WifiManager.WifiLock mWifiLock;
    /**
     *
     */
    // 音频播放状态的监听
    private PlaybackCallback mPlaybackCallback;

    /**
     *
     */
    // 当前播放状态
    private int mPlaybackState;
    // 音频焦点是否获取
    private boolean mPlayOnFocusGain;
    // Type of audio focus we have:
    private int mAudioFocus = AudioManager.AUDIOFOCUS_LOSS;
    // 插拔耳机的广播是否已注册
    private volatile boolean mAudioNoisyReceiverRegistered;
    // 当前播放到的位置
    private volatile long mCurrentPosition;
    // 当前播放速度
    private float mCurrentSpeed = 1f;

    /**
     * 构造方法
     *
     * @param context
     */
    public MusicPlayback(Context context) {
        // 获取 ApplicationContext
        Context applicationContext = context.getApplicationContext();
        // context赋值
        this.mContext = applicationContext;
        // 获取AudioManager
        try {
            mAudioManager = (AudioManager) applicationContext.getSystemService(Context.AUDIO_SERVICE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 保持wifi连接状态的WifiLock
        try {
            mWifiLock =
                    ((WifiManager) applicationContext.getSystemService(Context.WIFI_SERVICE))
                            .createWifiLock(WifiManager.WIFI_MODE_FULL, "awakening_lock");
        } catch (Exception e) {
            e.printStackTrace();
        }
        /**
         * 数据
         */
        // 播放状态
        this.mPlaybackState = PlaybackStateCompat.STATE_NONE;
    }

    /**
     * 播放音频
     */
    @Override
    public void start() {
        // mMediaPlayer 未创建
        if (mMediaPlayer == null && mPlaybackCallback != null) {
            // 回调错误
            mPlaybackCallback.onPlaybackStatusChanged(PlaybackStateCompat.STATE_ERROR);
            mPlaybackState = PlaybackStateCompat.STATE_ERROR;
            return;
        }
        // 焦点获取
        mPlayOnFocusGain = true;
        // 获取音频焦点
        tryToGetAudioFocus();
        // 注册 监听耳机插拔的广播
        registerAudioNoisyReceiver();
        //
        try {
            if (mMediaPlayer == null) {
                return;
            }
            Log.i(TAG, "start : " + "configMediaPlayerState");
            // 根据音频降焦点情况：暂停播放、正常播放、降低音量播放
            configMediaPlayerState();
        } catch (Exception ex) {
            // 回调错误
            mPlaybackState = PlaybackStateCompat.STATE_ERROR;
            // 回调错误
            if (mPlaybackCallback != null) {
                mPlaybackCallback.onError(ex.getMessage());
            }
        }
    }

    /**
     * 停止播放
     *
     * @param notifyListeners if true and a callback has been set by setCallback,
     *                        callback.onPlaybackStatusChanged will be called after changing
     */
    @Override
    public void stop(boolean notifyListeners) {
        // 回调已播放停止  停止播放
        if (mMediaPlayer == null && mPlaybackCallback != null) {
            mPlaybackCallback.onPlaybackStatusChanged(PlaybackStateCompat.STATE_STOPPED);
            mPlaybackState = PlaybackStateCompat.STATE_STOPPED;
            return;
        }
        // 状态为停止播放
        mPlaybackState = PlaybackStateCompat.STATE_STOPPED;
        // 回调STATE_STOPPED
        if (notifyListeners && mPlaybackCallback != null) {
            mPlaybackCallback.onPlaybackStatusChanged(mPlaybackState);
        }
        // 当前播放位置
        mCurrentPosition = getCurrentStreamPosition();
        // 放弃焦点
        // Give up Audio focus
        giveUpAudioFocus();
        // 取消音频插拔广播接受者注册
        unregisterAudioNoisyReceiver();
        // Relax all resources
        relaxResources(true);
    }

    /**
     * 设置播放状态
     *
     * @param state
     */
    @Override
    public void setState(int state) {
        this.mPlaybackState = state;
    }

    /**
     * 获取当前的播放状态
     *
     * @return
     */
    @Override
    public int getState() {
        return mPlaybackState;
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    public boolean isPlaying() {
        return (mMediaPlayer != null && mMediaPlayer.isPlaying());
    }

    /**
     * 当前的播放位置
     *
     * @return
     */
    @Override
    public long getCurrentStreamPosition() {
        return mMediaPlayer != null ?
                mMediaPlayer.getCurrentPosition() : mCurrentPosition;
    }

    /**
     * 播放
     *
     * @param source
     */
    @Override
    public void play(String source) {
        //
        mPlayOnFocusGain = false;
        // 当前播放位置为0
        mCurrentPosition = 0;
        // 获取焦点
        tryToGetAudioFocus();
        // 注册耳机插拔的广播接受者
        registerAudioNoisyReceiver();
        // 释放资源
        relaxResources(false);
        //
        try {
            // 创建MediaPlayer
            createMediaPlayerIfNeeded();
            // 回调错误
            if (mMediaPlayer == null && mPlaybackCallback != null) {
                mPlaybackCallback.onPlaybackStatusChanged(PlaybackStateCompat.STATE_ERROR);
                mPlaybackState = PlaybackStateCompat.STATE_ERROR;
                return;
            }
            // 设置播放状态
            mPlaybackState = PlaybackStateCompat.STATE_BUFFERING;
            // 播放路径
            mMediaPlayer.setDataSource(source);
            mMediaPlayer.prepareAsync();
            // wifi锁定，保持wifi连接状态
            if (mWifiLock != null) {
                mWifiLock.acquire();
            }
        } catch (IOException ex) {
            // 播放错误
            mPlaybackState = PlaybackStateCompat.STATE_ERROR;
            if (mPlaybackCallback != null) {
                mPlaybackCallback.onError(ex.getMessage());
            }
        } catch (Exception e) {
            // 播放错误
            mPlaybackState = PlaybackStateCompat.STATE_ERROR;
            if (mPlaybackCallback != null) {
                mPlaybackCallback.onError(e.getMessage());
            }
        }
        // 播放状态变化回调
        if (mPlaybackCallback != null) {
            mPlaybackCallback.onPlaybackStatusChanged(mPlaybackState);
        }
    }

    /**
     * 暂停
     */
    @Override
    public void pause() {
        // 尚未播放
        if (mMediaPlayer == null && mPlaybackCallback != null) {
            mPlaybackCallback.onPlaybackStatusChanged(PlaybackStateCompat.STATE_NONE);
            mPlaybackState = PlaybackStateCompat.STATE_NONE;
            return;
        }
        // 暂停
        if (mMediaPlayer != null) {
            mMediaPlayer.pause();
            // 记录当前播放状态
            mCurrentPosition = mMediaPlayer.getCurrentPosition();
        }
        // 释放wifiLock
        relaxResources(false);
        // 暂停
        mPlaybackState = PlaybackStateCompat.STATE_PAUSED;
        if (mPlaybackCallback != null) {
            mPlaybackCallback.onPlaybackStatusChanged(mPlaybackState);
        }
        // 取消音频插拔广播注册
        unregisterAudioNoisyReceiver();
    }

    /**
     * seek
     *
     * @param position
     */
    @Override
    public void seekTo(long position) {
        // 播放错误
        if (mMediaPlayer == null) {
            mCurrentPosition = position;
            mPlaybackState = PlaybackStateCompat.STATE_ERROR;
            return;
        }
        // buffer
        if (mMediaPlayer.isPlaying()) {
            mPlaybackState = PlaybackStateCompat.STATE_BUFFERING;
        }
        // 当前播放位置
        mCurrentPosition = position;
        // 注册耳机插拔的广播接受者
        registerAudioNoisyReceiver();
        // seek 到对应位置
        mMediaPlayer.seekTo((int) position);
        // 播放状态回调
        if (mPlaybackCallback != null) {
            mPlaybackCallback.onPlaybackStatusChanged(mPlaybackState);
        }

    }

    /**
     * 播放速度
     *
     * @return
     */
    @Override
    public float getSpeed() {
        return mCurrentSpeed;
    }


    // ##########################################################################################

    /**
     * Makes sure the media player exists and has been reset. This will create
     * the media player if needed, or reset the existing media player if one
     * already exists.
     */
    private void createMediaPlayerIfNeeded() {
        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();
            // 设置播放监听
            mMediaPlayer.setOnPreparedListener(this);
            mMediaPlayer.setOnCompletionListener(this);
            mMediaPlayer.setOnErrorListener(this);
            mMediaPlayer.setOnSeekCompleteListener(this);
        } else {
            mMediaPlayer.reset();
        }
    }

    /**
     * Releases resources used by the service for playback. This includes the
     * "foreground service" status, the wake locks and possibly the MediaPlayer.
     * <p>
     * 释放资源
     *
     * @param releaseMediaPlayer Indicates whether the Media Player should also
     *                           be released or not
     */
    private void relaxResources(boolean releaseMediaPlayer) {
        // 释放MediaPlayer
        // stop and release the Media Player, if it's available
        if (releaseMediaPlayer && mMediaPlayer != null) {
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        // 释放WifiLock
        // we can also release the Wifi lock, if we're holding it
        if (mWifiLock != null && mWifiLock.isHeld()) {
            mWifiLock.release();
        }
    }


    /**
     * 继续上次播放状态开始播放
     */
    private void playGain() {
        if (mMediaPlayer != null && !mMediaPlayer.isPlaying()) {
            // 播放音频
            if (mCurrentPosition == 0 || mCurrentPosition == mMediaPlayer.getCurrentPosition()) {
                mMediaPlayer.start();
                mPlaybackState = PlaybackStateCompat.STATE_PLAYING;
            }
            // seek到对应位置
            else {
                mMediaPlayer.seekTo((int) mCurrentPosition);
                mPlaybackState = PlaybackStateCompat.STATE_BUFFERING;
            }
        } else {
            mPlaybackState = PlaybackStateCompat.STATE_PLAYING;
        }
    }


    // ##########################################################################################

    /**
     * 设置{@link Playback.PlaybackCallback}回调
     *
     * @param callback
     */
    @Override
    public void setCallback(PlaybackCallback callback) {
        this.mPlaybackCallback = callback;
    }

    // ##########################################################################################

    /**
     * Called by AudioManager on audio focus changes.
     * Implementation of {@link AudioManager.OnAudioFocusChangeListener}
     */
    @Override
    public void onAudioFocusChange(int focusChange) {
        // 音频焦点状态赋值
        mAudioFocus = focusChange;
        // 根据音频降焦点情况：暂停播放、正常播放、降低音量播放
        configMediaPlayerState();
    }


    // ##########################################################################################


    /**
     * {@link MediaPlayer.OnSeekCompleteListener}的回调方法
     * <p>
     * MediaPlayer seek ok
     * <p>
     * Called when MediaPlayer has completed a seek
     */
    @Override
    public void onSeekComplete(MediaPlayer mp) {
        if (mMediaPlayer == null && mPlaybackCallback != null) {
            mPlaybackCallback.onPlaybackStatusChanged(PlaybackStateCompat.STATE_ERROR);
            mPlaybackState = PlaybackStateCompat.STATE_ERROR;
            return;
        }
        mCurrentPosition = mp.getCurrentPosition();
        if (mPlaybackState == PlaybackStateCompat.STATE_BUFFERING) {
            registerAudioNoisyReceiver();
            mMediaPlayer.start();
            mPlaybackState = PlaybackStateCompat.STATE_PLAYING;
        }
        if (mPlaybackCallback != null) {
            mPlaybackCallback.onPlaybackStatusChanged(mPlaybackState);
        }
    }

    // ##########################################################################################

    /**
     * {@link MediaPlayer.OnPreparedListener}的回调方法
     * <p>
     * MediaPlayer音频准备ok
     * <p>
     * Called when media player is done preparing.
     */
    @Override
    public void onPrepared(MediaPlayer player) {
        // The media player is done preparing. That means we can start playing if we
        // have audio focus.
        mPlaybackState = PlaybackStateCompat.STATE_PLAYING;
        mCurrentPosition = Math.min(player.getDuration(), mCurrentPosition);
        mMediaPlayer.seekTo((int) mCurrentPosition);
        mMediaPlayer.start();
        if (mPlaybackCallback != null) {
            mPlaybackCallback.onPlaybackStatusChanged(mPlaybackState);
        }
    }


    // ##########################################################################################


    /**
     * {@link MediaPlayer.OnErrorListener}的回调方法
     * <p>
     * MediaPlayer播放错误
     * <p>
     * Called when there's an error playing media. When this happens, the media
     * player goes to the Error state. We warn the user about the error and
     * reset the media player.
     */
    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        mPlaybackState = PlaybackStateCompat.STATE_ERROR;
        mCurrentPosition = getCurrentStreamPosition();
        if (mPlaybackCallback != null) {
            mPlaybackCallback.onError("MediaPlayer error " + what + " (" + extra + ")");
        }
        return true; // true indicates we handled the error
    }


    // ##########################################################################################


    /**
     * {@link MediaPlayer.OnCompletionListener}的回调方法
     * <p>
     * MediaPlayer播放完成
     * <p>
     * Called when media player is done playing current song.
     */
    @Override
    public void onCompletion(MediaPlayer player) {
        // 播放完成的回调
        if (mPlaybackCallback != null) {
            mPlaybackCallback.onCompletion();
        }
    }


    // #########################################################################################


    /**
     * 插拔耳机的广播
     */
    private final IntentFilter mAudioNoisyIntentFilter =
            new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
    //
    private final BroadcastReceiver mAudioNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // 收到耳机插拔的广播
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                // 音频正在播放
                if (isPlaying()) {
                    // 暂停音频播放
                    Intent i = new Intent(context, MusicService.class);
                    i.setAction(MusicService.ACTION_CMD);
                    i.putExtra(MusicService.CMD_NAME, MusicService.CMD_PAUSE);
                    mContext.startService(i);
                }
            }
        }
    };

    /**
     * 注册耳机插拔的广播接受者
     */
    private void registerAudioNoisyReceiver() {
        if (!mAudioNoisyReceiverRegistered) {
            mContext.registerReceiver(mAudioNoisyReceiver, mAudioNoisyIntentFilter);
            mAudioNoisyReceiverRegistered = true;
        }
    }

    /**
     * 取消注册耳机插拔的广播接受者
     */
    private void unregisterAudioNoisyReceiver() {
        if (mAudioNoisyReceiverRegistered) {
            mContext.unregisterReceiver(mAudioNoisyReceiver);
            mAudioNoisyReceiverRegistered = false;
        }
    }


    // #########################################################################################

    /**
     * Try to get the system audio focus.
     * <p>
     * 获取音频焦点
     */
    private void tryToGetAudioFocus() {
        //
        if (mAudioManager == null) {
            onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN);
            return;
        }
        // 请求音频焦点
        int result = mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            mAudioFocus = AudioManager.AUDIOFOCUS_GAIN;
        }
    }

    /**
     * Give up the audio focus.
     * 放弃音频焦点
     */
    private void giveUpAudioFocus() {
        if (mAudioManager == null) {
            return;
        }
        if (mAudioManager.abandonAudioFocus(this) == AUDIOFOCUS_REQUEST_GRANTED) {
            mAudioFocus = AudioManager.AUDIOFOCUS_LOSS;
        }
    }


    /**
     * 根据音频降焦点情况：暂停播放、正常播放、降低音量播放
     */
    private void configMediaPlayerState() {
        // MediaPlayer尚未创建
        if (mMediaPlayer == null && mPlaybackCallback != null) {
            // 回调音频播放状态
            mPlaybackCallback.onPlaybackStatusChanged(PlaybackStateCompat.STATE_NONE);
            // 当前音频播放状态
            mPlaybackState = PlaybackStateCompat.STATE_NONE;
            return;
        }
        //
        switch (mAudioFocus) {
            case AudioManager.AUDIOFOCUS_GAIN:
                //重新获取到焦点
                registerAudioNoisyReceiver();
                // 设置音量为正常音量
                mMediaPlayer.setVolume(VOLUME_NORMAL, VOLUME_NORMAL);
                //
                if (mPlayOnFocusGain) {
                    playGain();
                    mPlayOnFocusGain = false;
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                //永久性失去焦点，不会继续播放
                pause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                //暂时失去焦点，降低音量
                if (mPlaybackState == PlaybackStateCompat.STATE_PLAYING) {
                    mMediaPlayer.setVolume(VOLUME_DUCK, VOLUME_DUCK);
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                //暂时失去焦点
                if (mPlaybackState == PlaybackStateCompat.STATE_PLAYING) {
                    mPlayOnFocusGain = true;
                }
                pause();
                break;
        }
        // 回调播放状态
        if (mPlaybackCallback != null) {
            mPlaybackCallback.onPlaybackStatusChanged(mPlaybackState);
        }
    }


}