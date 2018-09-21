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

package com.mediabrowser.xiaxl.service.notification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import com.mediabrowser.xiaxl.R;
import com.mediabrowser.xiaxl.service.MusicService;
import com.mediabrowser.xiaxl.service.utils.ResourceUtil;

/**
 * Keeps track of a notification and updates it automatically for a given
 * MediaSession. Maintaining a visible notification (usually) guarantees that the music service
 * won't be killed during playback.
 */
public class MediaNotificationManager extends BroadcastReceiver {
    private static final String TAG = "MediaNotification";

    private static final String CHANNEL_ID = "com.example.android.uamp.MUSIC_CHANNEL_ID";

    // notification id
    private static final int NOTIFICATION_ID = 412;
    //
    private static final int REQUEST_CODE = 100;

    /**
     * action
     */
    public static final String ACTION_PAUSE = "com.netease.awakeing.music.pause";
    public static final String ACTION_PLAY = "com.netease.awakeing.music.play";
    public static final String ACTION_PREV = "com.netease.awakeing.music.prev";
    public static final String ACTION_NEXT = "com.netease.awakeing.music.next";
    public static final String ACTION_STOP_CASTING = "com.netease.awakeing.music.stop_cast";

    /**
     * service
     */
    // MusicService
    private final MusicService mMusicService;
    // SessionToken
    private MediaSessionCompat.Token mSessionToken;

    /**
     * client
     */
    // 创建了一个MediaControllerCompat
    private MediaControllerCompat mMediaController;
    // TransportControls
    private MediaControllerCompat.TransportControls mTransportControls;
    // 数据
    private MediaMetadataCompat mMediaMetadata;
    // 播放状态
    private PlaybackStateCompat mPlaybackState;


    // 暂停
    private final PendingIntent mPauseIntent;
    // 播放
    private final PendingIntent mPlayIntent;
    // 上一个
    private final PendingIntent mPreviousIntent;
    // 下一个
    private final PendingIntent mNextIntent;

    /**
     * 获取NotificationManager
     */
    private final NotificationManager mNotificationManager;


    /**
     * 数据
     */
    // notification 颜色
    private final int mNotificationColor;
    //
    private boolean mStarted = false;


    /**
     * 构造方法
     *
     * @param service
     * @throws RemoteException
     */
    public MediaNotificationManager(MusicService service) throws RemoteException {
        // MusicService
        mMusicService = service;
        // 创建了 {@link MediaControllerCompat} 获取了{@link MediaControllerCompat.TransportControls}
        getMediaControllerBySessionToken();
        /**
         *
         */
        // NotificationManagerCompat
        mNotificationManager = (NotificationManager) mMusicService.getSystemService(Context.NOTIFICATION_SERVICE);

        /**
         *
         */
        // notification 颜色
        mNotificationColor = ResourceUtil.getThemeColor(mMusicService, R.color.trans, Color.DKGRAY);

        //
        String pkg = mMusicService.getPackageName();
        // 暂定
        mPauseIntent = PendingIntent.getBroadcast(mMusicService, REQUEST_CODE,
                new Intent(ACTION_PAUSE).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        // 播放
        mPlayIntent = PendingIntent.getBroadcast(mMusicService, REQUEST_CODE,
                new Intent(ACTION_PLAY).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        // 上一个
        mPreviousIntent = PendingIntent.getBroadcast(mMusicService, REQUEST_CODE,
                new Intent(ACTION_PREV).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        // 下一个
        mNextIntent = PendingIntent.getBroadcast(mMusicService, REQUEST_CODE,
                new Intent(ACTION_NEXT).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);

        // 默认  移除所有的通知栏消息
        try {
            mNotificationManager.cancel(NOTIFICATION_ID);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Posts the notification and starts tracking the session to keep it
     * updated. The notification will automatically be removed if the session is
     * destroyed before {@link #stopNotification} is called.
     */
    public void startNotification() {
        //
        if (!mStarted) {
            // 音频数据
            mMediaMetadata = mMediaController.getMetadata();
            // 播放状态
            mPlaybackState = mMediaController.getPlaybackState();

            // The notification must be updated after setting started to true
            Notification notification = createNotification();
            setIcon(mMediaMetadata);
            if (notification != null) {
                mMediaController.registerCallback(mMediaControllerCallBack);
                IntentFilter filter = new IntentFilter();
                filter.addAction(ACTION_NEXT);
                filter.addAction(ACTION_PAUSE);
                filter.addAction(ACTION_PLAY);
                filter.addAction(ACTION_PREV);
                filter.addAction(ACTION_STOP_CASTING);
                mMusicService.registerReceiver(this, filter);

                mMusicService.startForeground(NOTIFICATION_ID, notification);
                mStarted = true;
            }
        }
    }


    /**
     * Removes the notification and stops tracking the session. If the session
     * was destroyed this has no effect.
     */
    public void stopNotification() {
        if (mStarted) {
            mStarted = false;
            mMediaController.unregisterCallback(mMediaControllerCallBack);
            try {
                mNotificationManager.cancel(NOTIFICATION_ID);
                mMusicService.unregisterReceiver(this);
            } catch (IllegalArgumentException ex) {
                // ignore if the receiver is not registered.
            }
            mMusicService.stopForeground(true);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        Log.d(TAG, "Received intent with action " + action);
        switch (action) {
            case ACTION_PAUSE:
                mTransportControls.pause();
                break;
            case ACTION_PLAY:
                mTransportControls.play();
                break;
            case ACTION_NEXT:
                mTransportControls.skipToNext();
                break;
            case ACTION_PREV:
                mTransportControls.skipToPrevious();
                break;
            case ACTION_STOP_CASTING:
                Intent i = new Intent(context, MusicService.class);
                i.setAction(MusicService.ACTION_CMD);
                i.putExtra(MusicService.CMD_NAME, MusicService.CMD_STOP_CASTING);
                mMusicService.startService(i);
                break;
            default:
        }
    }

    /**
     * 创建了 {@link MediaControllerCompat} 获取了{@link MediaControllerCompat.TransportControls}
     * <p>
     * Update the state based on a change on the session token. Called either when
     * we are running for the first time or when the media session owner has destroyed the session
     * (see {@link android.media.session.MediaController.Callback#onSessionDestroyed()})
     * <p>
     */
    private void getMediaControllerBySessionToken() throws RemoteException {
        // 获取token
        MediaSessionCompat.Token freshToken = mMusicService.getSessionToken();
        //
        if (mSessionToken == null && freshToken != null ||
                mSessionToken != null && !mSessionToken.equals(freshToken)) {
            //
            if (mMediaController != null) {
                mMediaController.unregisterCallback(mMediaControllerCallBack);
            }
            //
            mSessionToken = freshToken;
            //
            if (mSessionToken != null) {
                // 创建MediaControllerCompat
                mMediaController = new MediaControllerCompat(mMusicService, mSessionToken);
                mTransportControls = mMediaController.getTransportControls();
                if (mStarted) {
                    mMediaController.registerCallback(mMediaControllerCallBack);
                }
            }
        }
    }

    private PendingIntent createContentIntent(MediaDescriptionCompat description) {
        try {
            Intent intent = mMusicService.getPackageManager().getLaunchIntentForPackage(mMusicService.getPackageName())
                    .setPackage(null)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            return PendingIntent.getActivity(mMusicService, REQUEST_CODE, intent, 0);
        } catch (Exception e) {
            return null;
        }

    }


    private void setIcon(MediaMetadataCompat metadata) {
        if (metadata == null || metadata.getDescription() == null || metadata.getDescription().getIconUri() == null)
            return;
        MediaDescriptionCompat description = metadata.getDescription();
        String artUrl = description.getIconUri().toString();
        if (artUrl != null) {
            fetchBitmapFromURLAsync(artUrl);
        }
    }


    // #######################################################################################

    private Notification refreshNotification() {
        try {
            return createNotification();
        } catch (Exception e) {
            return null;
        }
    }


    /**
     * @return
     */
    private Notification createNotification() {
        Log.d(TAG, "createNotification. mMediaMetadata=" + mMediaMetadata);
        // 创建通知栏
        if (mMediaMetadata == null || mPlaybackState == null) {
            return null;
        }


        // Notification channels are only supported on Android O+.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
        }


        final NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(mMusicService, CHANNEL_ID);
        // 上一曲
        notificationBuilder.addAction(R.drawable.ic_skip_previous_white_36dp,
                mMusicService.getString(R.string.label_previous), mPreviousIntent);

        // 添加 暂停 播放
        addPlayPauseAction(notificationBuilder);
        // 下一曲
        notificationBuilder.addAction(R.drawable.ic_skip_next_white_36dp,
                mMusicService.getString(R.string.label_next), mNextIntent);

        //
        MediaDescriptionCompat description = mMediaMetadata.getDescription();

        Bitmap art = null;
        if (description.getIconUri() != null) {
            // This sample assumes the iconUri will be a valid URL formatted String, but
            // it can actually be any valid Android Uri formatted String.
            // async fetch the album art icon
            String artUrl = description.getIconUri().toString();
            art = AlbumArtCache.getInstance().getBigImage(artUrl);
            if (art == null) {
                // use a placeholder art while the remote art is being downloaded
                art = BitmapFactory.decodeResource(mMusicService.getResources(),
                        R.drawable.ic_notification);
            }
        }

        notificationBuilder
                .setStyle(new android.support.v4.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(
                                new int[]{0, 1, 2})
                        .setMediaSession(mSessionToken))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setColor(mNotificationColor)
                .setSmallIcon(R.drawable.ic_notification)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
//                .setUsesChronometer(true)
                .setShowWhen(false)
                .setContentIntent(createContentIntent(description))
                .setContentTitle(description.getTitle())
                .setContentText(description.getSubtitle())
                .setLargeIcon(art);
        setNotificationPlaybackState(notificationBuilder);

        return notificationBuilder.build();
    }

    /**
     * 添加暂停、播放
     *
     * @param builder
     */
    private void addPlayPauseAction(NotificationCompat.Builder builder) {
        Log.d(TAG, "updatePlayPauseAction");
        String label;
        int icon;
        PendingIntent intent;

        if (mPlaybackState.getState() == PlaybackStateCompat.STATE_PLAYING) {
            label = mMusicService.getString(R.string.label_pause);
            icon = R.drawable.ic_pause_white_36dp;
            intent = mPauseIntent;
        } else {
            label = mMusicService.getString(R.string.label_play);
            icon = R.drawable.ic_play_white_36dp;
            intent = mPlayIntent;
        }
        builder.addAction(new NotificationCompat.Action(icon, label, intent));
    }

    private void setNotificationPlaybackState(NotificationCompat.Builder builder) {
        if (mPlaybackState == null || !mStarted) {
            mMusicService.stopForeground(true);
            return;
        }
        // Make sure that the notification can be dismissed by the user when we are not playing:
        builder.setOngoing(mPlaybackState.getState() == PlaybackStateCompat.STATE_PLAYING);
    }

    private void fetchBitmapFromURLAsync(final String bitmapUrl) {
        AlbumArtCache.getInstance().fetch(bitmapUrl, new AlbumArtCache.FetchListener() {
            @Override
            public void onFetched(String artUrl, Bitmap bitmap, Bitmap icon) {
                if (mMediaMetadata != null && mMediaMetadata.getDescription().getIconUri() != null &&
                        mMediaMetadata.getDescription().getIconUri().toString().equals(artUrl)) {
                    // If the media is still the same, update the notification:
//                    builder.setLargeIcon(bitmap);
                    mNotificationManager.notify(NOTIFICATION_ID, createNotification());
                }
            }
        });
    }


    // ############################################################################################


    private final MediaControllerCompat.Callback mMediaControllerCallBack = new MediaControllerCompat.Callback() {
        @Override
        public void onPlaybackStateChanged(@NonNull PlaybackStateCompat state) {
            mPlaybackState = state;
            Log.d(TAG, "Received new playback state" + state);
            if (state.getState() == PlaybackStateCompat.STATE_STOPPED ||
                    state.getState() == PlaybackStateCompat.STATE_NONE) {
                stopNotification();
            } else {
                Notification notification = refreshNotification();
                if (notification != null) {
                    mNotificationManager.notify(NOTIFICATION_ID, notification);
                }
            }
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            mMediaMetadata = metadata;
            Log.d(TAG, "Received new metadata " + metadata);
            Notification notification = refreshNotification();
            setIcon(metadata);
            if (notification != null) {
                mNotificationManager.notify(NOTIFICATION_ID, notification);
            }
        }

        @Override
        public void onSessionDestroyed() {
            super.onSessionDestroyed();
            Log.d(TAG, "Session was destroyed, resetting to the new session token");
            try {
                getMediaControllerBySessionToken();
            } catch (RemoteException e) {
                Log.e(TAG, "could not connect media controller");
            }
        }
    };


    /**
     * Creates Notification Channel. This is required in Android O+ to display notifications.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private void createNotificationChannel() {
        if (mNotificationManager.getNotificationChannel(CHANNEL_ID) == null) {
            NotificationChannel notificationChannel =
                    new NotificationChannel(CHANNEL_ID,
                            "UAMP_Channel_ID",
                            NotificationManager.IMPORTANCE_LOW);

            notificationChannel.setDescription("Channel ID for UAMP");

            mNotificationManager.createNotificationChannel(notificationChannel);
        }
    }

}
