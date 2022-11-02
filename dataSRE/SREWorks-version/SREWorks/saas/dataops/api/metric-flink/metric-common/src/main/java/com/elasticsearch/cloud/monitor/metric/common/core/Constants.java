package com.elasticsearch.cloud.monitor.metric.common.core;

import com.elasticsearch.cloud.monitor.metric.common.utils.TimeUtils;

public class Constants {

    public static String CHECK_INTERVAL_UNIT = "m";

    public static String CHECK_INTERVAL_STR = "1" + CHECK_INTERVAL_UNIT;

    public static long CHECK_INTERVAL = TimeUtils.parseDuration(CHECK_INTERVAL_STR);

    public static long MAX_DURATION_TIME = TimeUtils.parseDuration("60m");

    public static long MAX_COMPARE_TO_TIME = TimeUtils.parseDuration("1d");
}
