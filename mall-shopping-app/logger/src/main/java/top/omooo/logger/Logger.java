package top.omooo.logger;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

/**
 * Created by Omooo
 * Date:2019/4/21
 * 统一日志类
 */
public final class Logger {
    private static final String TAG = "Logger";
    private static final String TOP_LINE = "┌────────────────────────────────────────────────────────────────────";
    private static final String BOTTOM_LINE = "└────────────────────────────────────────────────────────────────────";
    private static final String CENTER_LINE = "├┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄";

    private static StringBuilder sSbSpace = new StringBuilder("│ ");
    private static StringBuilder sSbValue = new StringBuilder();

    private Logger() {

    }

    public static void e(String mag) {
        e(TAG, null, mag);
    }

    public static void e(@NonNull String tag, @Nullable String desc, String... mags) {
        Log.e(tag, TOP_LINE);
        for (int i = 0; i < mags.length; i++) {
            if (i != 0) {
                sSbSpace.append("  ");
            }
            Log.e(tag, sSbValue.append(sSbSpace.toString()).append(mags[i]).toString());
            reset();
        }
        if (!TextUtils.isEmpty(desc)) {
            Log.e(tag, CENTER_LINE);
            Log.e(tag, sSbValue.append(sSbSpace.toString()).append(desc).toString());
        }
        Log.e(tag, BOTTOM_LINE);
        reset();
    }

    public static void i(String mag) {
        i(TAG, mag);
    }

    public static void i(@NonNull String tag, String mag) {
        Log.i(tag, TOP_LINE);
        Log.i(tag, "│ " + tag);
        Log.i(tag, CENTER_LINE);
        Log.i(tag, "│ " + mag);
        Log.i(tag, BOTTOM_LINE);

    }

    public static void d(String mag) {
        d(TAG, null, mag);
    }

    public static void d(@NonNull String tag, String desc, String... mags) {
        Log.d(tag, TOP_LINE);
        for (int i = 0; i < mags.length; i++) {
            if (i != 0) {
                sSbSpace.append("  ");
            }
            Log.d(tag, sSbValue.append(sSbSpace.toString()).append(mags[i]).toString());
            reset();
        }
        if (!TextUtils.isEmpty(desc)) {
            Log.d(tag, CENTER_LINE);
            Log.d(tag, sSbValue.append(sSbSpace.toString()).append(desc).toString());
        }
        Log.d(tag, BOTTOM_LINE);
        reset();
    }

    private static void reset() {
        sSbSpace = new StringBuilder("│ ");
        sSbValue = new StringBuilder();
    }

}