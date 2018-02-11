package com.mediabrowser.xiaxl.setting;

import android.content.Context;
import android.content.SharedPreferences;


/**
 * Created by xiaxl on 2017/9/16.
 */

public class SettingConfig {

    private static final String TAG = "SettingConfig";
    //
    private static final String PREFERENCE_NAME = "SettingConfig";
    private static final String KEY_PLAY_WITH_GPRS = "play_with_GPRS";

    /**
     * 2g3g4g网络下是否允许播放
     *
     * @return
     */
    public static final boolean isGPRSPlayAllowed(Context context) {
        return getConfigSharedPreferences(context).getBoolean(KEY_PLAY_WITH_GPRS, false);
    }


    /**
     * 设置2g3g4g网络下是否允许播放
     *
     * @param value
     */
    public static final void setGPRSPlayAllowed(Context context, boolean value) {
        getConfigSharedPreferences(context).edit().putBoolean(KEY_PLAY_WITH_GPRS, value).commit();
    }


    /**
     *
     * @param context
     * @return
     */
    private static SharedPreferences getConfigSharedPreferences(Context context) {
        SharedPreferences pref = context.getSharedPreferences(PREFERENCE_NAME, 0);
        return pref;
    }

}
