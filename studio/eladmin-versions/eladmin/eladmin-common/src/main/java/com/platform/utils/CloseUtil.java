
package com.platform.utils;

import java.io.Closeable;

/**
 * @author AllDataDC
 * @website https://eladmin.vip
 * @description 用于关闭各种连接，缺啥补啥
 * @date 2022-10-27
 **/
public class CloseUtil {

    public static void close(Closeable closeable) {
        if (null != closeable) {
            try {
                closeable.close();
            } catch (Exception e) {
                // 静默关闭
            }
        }
    }

    public static void close(AutoCloseable closeable) {
        if (null != closeable) {
            try {
                closeable.close();
            } catch (Exception e) {
                // 静默关闭
            }
        }
    }
}
