package com.alibaba.tesla.appmanager.common.util;

import java.util.Date;

public class DateUtil {

    public static Date now() {
        return new Date();
    }

    /**
     * 计算指定开始时间到当前时间花费了多少 ms
     *
     * @param start 起始时间，微秒
     * @return 字符串
     */
    public static String costTime(long start) {
        return String.format("%dms", (System.currentTimeMillis() - start) / 1000);
    }
}
