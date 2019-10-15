package com.platform.app.http.okhttp.utils;

import android.util.Log;

/**
 * Created by wulinhao on 2019/9/6.
 */
public class L
{
    private static boolean debug = false;

    public static void e(String msg)
    {
        if (debug)
        {
            Log.e("OkHttp", msg);
        }
    }

}

