package com.mediabrowser.xiaxl.client.utils;


import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;


/**
 * <br/>网络工具类.
 * <br/>主要用于检测当前网络是否可用，检测当前网络连接类型
 *
 * @author wjying
 */
public class NetUtils {
    public static final String TAG = "NetUtils";


    /**
     * 检测是否是wifi连网.
     */
    public static boolean isWIFI(Context context) {
        NetworkInfo info = getNetWorkInfo(context);
        return (info != null && info.getType() == ConnectivityManager.TYPE_WIFI);
    }


    /**
     * 获取网络类型
     */
    public static NetworkInfo getNetWorkInfo(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        return connectivityManager.getActiveNetworkInfo();
    }

    public static boolean isConnected(Context context) {
        NetworkInfo info = getNetWorkInfo(context);
        return info != null && info.isConnected();
    }
}
