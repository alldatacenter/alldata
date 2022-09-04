package com.elasticsearch.cloud.monitor.metric.common.utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class TimeUtils {

    /**
     * Mask to verify a timestamp on 4 bytes in seconds
     */
    public static final long SECOND_MASK = 0xFFFFFFFF00000000L;

    /**
     * Parses a human-readable duration (e.g, "10m", "3h", "14d") into seconds.
     * <p>
     * Formats supported:<ul>
     * <li>{@code ms}: milliseconds</li>
     * <li>{@code s}: seconds</li>
     * <li>{@code m}: minutes</li>
     * <li>{@code h}: hours</li>
     * <li>{@code d}: days</li>
     * <li>{@code w}: weeks</li>
     * <li>{@code n}: month (30 days)</li>
     * <li>{@code y}: years (365 days)</li></ul>
     *
     * @param duration The human-readable duration to parse.
     * @return A strictly positive number of milliseconds.
     * @throws IllegalArgumentException if the interval was malformed.
     */
    public static final long parseDuration(final String duration) {
        if (duration == null || duration.isEmpty()) {
            throw new IllegalArgumentException("duration is empty");
        }
        long interval;
        long multiplier;
        double temp;
        int unit = 0;
        while (unit < duration.length() && Character.isDigit(duration.charAt(unit))) {
            unit++;
        }
        try {
            interval = Long.parseLong(duration.substring(0, unit));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid duration (number): " + duration);
        }
        if (interval <= 0) {
            throw new IllegalArgumentException("Zero or negative duration: " + duration);
        }
        if (unit == duration.length()) {
            throw new IllegalArgumentException("empty duration suffix");
        }
        switch (duration.toLowerCase().charAt(duration.length() - 1)) {
            case 's':
                if (duration.charAt(duration.length() - 2) == 'm') {
                    return interval;
                }
                multiplier = 1;
                break;                      // seconds
            case 'm':
                multiplier = 60;
                break;               // minutes
            case 'h':
                multiplier = 3600;
                break;             // hours
            case 'd':
                multiplier = 3600 * 24;
                break;        // days
            case 'w':
                multiplier = 3600 * 24 * 7;
                break;    // weeks
            case 'n':
                multiplier = 3600 * 24 * 30;
                break;   // month (average)
            case 'y':
                multiplier = 3600 * 24 * 365;
                break;  // years (screw leap years)
            default:
                throw new IllegalArgumentException("Invalid duration (suffix): " + duration);
        }
        multiplier *= 1000;
        temp = (double) interval * multiplier;
        if (temp > Long.MAX_VALUE) {
            throw new IllegalArgumentException("Duration must be < Long.MAX_VALUE ms: " + duration);
        }
        return interval * multiplier;
    }

    public static final String getDurationChineseName(final String duration) {
        parseDuration(duration); // validate duration
        switch (duration.toLowerCase().charAt(duration.length() - 1)) {
            case 's':
                return duration.substring(0, duration.length() - 1) + "秒钟";
            case 'm':
                return duration.substring(0, duration.length() - 1) + "分钟";
            case 'h':
                return duration.substring(0, duration.length() - 1) + "小时";
            case 'd':
                return duration.substring(0, duration.length() - 1) + "天";
            case 'w':
                return duration.substring(0, duration.length() - 1) + "周";
            case 'n':
                return duration.substring(0, duration.length() - 1) + "月";
            case 'y':
                return duration.substring(0, duration.length() - 1) + "年";
            default:
                throw new IllegalArgumentException("Invalid duration (suffix): " + duration);
        }
    }

    public static long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    public static boolean isMillisecond(long timestamp) {
        return (timestamp & SECOND_MASK) != 0;
    }

    public static long toSecond(long timestamp) {
        return TimeUtils.isMillisecond(timestamp) ? timestamp / 1000 : timestamp;
    }

    public static long toMillisecond(long timestamp) {
        return TimeUtils.isMillisecond(timestamp) ? timestamp : timestamp * 1000;
    }

    public static String toISOTime(long timestamp) {
        DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(toMillisecond(timestamp));
        return sdf.format(calendar.getTime());
    }

    public static long getDownsampleTimestamp(long timestamp, long downsampleMs) {
        boolean isMs = TimeUtils.isMillisecond(timestamp);
        long tsMs = isMs ? timestamp : timestamp * 1000;
        tsMs = (tsMs / downsampleMs) * downsampleMs;
        return isMs ? tsMs : tsMs / 1000;
    }

    public static long getShiftTimestamp(long timestamp, String timeShift) {
        return getShiftTimestamp(timestamp, parseDuration(timeShift));
    }

    public static long getShiftTimestamp(long timestamp, long timeShiftMs) {
        boolean isMs = TimeUtils.isMillisecond(timestamp);
        long tsMs = isMs ? timestamp : timestamp * 1000;
        long shiftTsMs = tsMs - timeShiftMs;
        return isMs ? shiftTsMs : shiftTsMs / 1000;
    }

    public static long getRightShiftTimestamp(long timestamp, String timeShift) {
        return getRightShiftTimestamp(timestamp, parseDuration(timeShift));
    }

    public static long getRightShiftTimestamp(long timestamp, long timeShiftMs) {
        boolean isMs = TimeUtils.isMillisecond(timestamp);
        long tsMs = isMs ? timestamp : timestamp * 1000;
        long shiftTsMs = tsMs + timeShiftMs;
        return isMs ? shiftTsMs : shiftTsMs / 1000;
    }

}
