package com.elasticsearch.cloud.monitor.metric.common.blink.utils;

import com.elasticsearch.cloud.monitor.metric.common.utils.TimeUtils;

/**
 * @author: fangzong.lyj
 * @date: 2021/09/06 17:51
 */
public class FlinkTimeUtil extends TimeUtils {

    private static Long YEAR = 31536000L;
    private static Long MONTH = 2592000L;
    private static Long WEEK = 604800L;
    private static Long DAY = 86400L;
    private static Long HOUR = 3600L;
    private static Long MINUTE = 60L;

    public static String getDuration(Long timestamp) {
        Long seconds = timestamp/1000;
        if (seconds < MINUTE) {
            return seconds + "s";
        } else if (seconds < HOUR) {
            return seconds/MINUTE + "m";
        } else if (seconds < DAY) {
            return seconds/HOUR + "h";
        } else if (seconds < WEEK) {
            return seconds/DAY + "d";
        } else if (seconds < MONTH) {
            return seconds/WEEK + "w";
        } else if (seconds < YEAR) {
            return seconds/MONTH + "n";
        } else {
            return seconds/YEAR + "y";
        }
    }

    public static String getMinuteDuration(Integer minute) {
        return minute + "m";
    }
}
