package com.platform.app.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;

/**
 * Created by wulinhao
 * Time  2019/9/4
 * Describe:
 */

public class Utils {

    @SuppressLint("StaticFieldLeak")
    private static Context context;

    private Utils() {
        throw new UnsupportedOperationException("u can't instantiate me...");
    }

    /**
     * 初始化工具类
     *
     * @param context 上下文
     */
    public static void init(@NonNull final Context context) {
        Utils.context = context.getApplicationContext();
    }

    /**
     * 获取ApplicationContext
     *
     * @return ApplicationContext
     */
    public static Context getContext() {
        if (context != null) return context;
        throw new NullPointerException("u should config first");
    }
}
