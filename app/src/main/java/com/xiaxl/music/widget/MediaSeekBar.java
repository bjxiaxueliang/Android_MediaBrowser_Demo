package com.xiaxl.music.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.support.v7.widget.AppCompatSeekBar;
import android.util.AttributeSet;
import android.view.animation.LinearInterpolator;
import android.widget.SeekBar;

/**
 * SeekBar
 */
public class MediaSeekBar extends AppCompatSeekBar {


    /**
     * 数据
     */
    private boolean mIsTracking = false;


    public MediaSeekBar(Context context) {
        super(context);
    }

    public MediaSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MediaSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setOnSeekBarChangeListener(final OnSeekBarChangeListener listener) {
        super.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (listener != null) {
                    listener.onProgressChanged(seekBar, progress, fromUser);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //
                if (listener != null) {
                    listener.onStartTrackingTouch(seekBar);
                }
                //
                mIsTracking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //
                if (listener != null) {
                    listener.onStopTrackingTouch(seekBar);
                }
                //
                mIsTracking = false;
            }
        });
    }


    // #########################################################################################


    /**
     * 属性动画
     */
    private ValueAnimator mProgressAnimator;

    /**
     * 开始
     *
     * @param start
     * @param end
     * @param duration
     */
    public void startProgressAnima(int start, int end, int duration) {
        // 停止播放动画
        stopProgressAnima();
        // 开始播放动画
        mProgressAnimator = ValueAnimator.ofInt(start, end).setDuration(duration);
        mProgressAnimator.setInterpolator(new LinearInterpolator());
        mProgressAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                //
                onProgressUpdate(animation);
            }
        });
        mProgressAnimator.start();
    }

    /**
     * 停止播放动画
     */
    public void stopProgressAnima() {
        if (mProgressAnimator != null) {
            mProgressAnimator.cancel();
            mProgressAnimator = null;
        }
    }


    // #########################################################################################

    /**
     * 更新进度
     *
     * @param valueAnimator
     */
    public void onProgressUpdate(final ValueAnimator valueAnimator) {
        // If the user is changing the slider, cancel the animation.
        if (mIsTracking) {
            valueAnimator.cancel();
            return;
        }
        // 设置播放进度
        final int animatedIntValue = (int) valueAnimator.getAnimatedValue();
        // 设置进度
        setProgress(animatedIntValue);
    }


}
