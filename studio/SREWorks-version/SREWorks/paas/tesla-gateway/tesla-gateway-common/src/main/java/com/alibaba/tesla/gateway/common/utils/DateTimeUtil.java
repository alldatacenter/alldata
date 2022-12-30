package com.alibaba.tesla.gateway.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Slf4j
public class DateTimeUtil {

    public static final String DATE_FORMAT1 = "yyyy-MM-dd";
    public static final String DATE_FORMAT2 = "yyyy/MM/dd";
    public static final String DATE_FORMAT3 = "yyyyMMdd";
    public static final String DATE_TIME_FORMAT1 = "yyyy-MM-dd HH:mm:ss";
    public static final String DATE_TIME_FORMAT2 = "yyyy/MM/dd HH:mm:ss";
    public static final String DATE_TIME_FORMAT3 = "yyyyMMdd HH:mm:ss";
    public static final String DATE_TIME_FORMAT4 = "yyyy-MM-dd HH:mm";
    public static final String DATE_TIME_FORMAT5 = "yyyy-MM-dd HH:mm:ss SSS";

    private static DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT5);

    public DateTimeUtil() {
    }

    public static String getCurrentDate() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        return df.format(new Date());
    }

    public static String getCurrentDate(String format) {
        if (StringUtils.isEmpty(format)) {
            format = "yyyy-MM-dd HH:mm:ss";
        }

        SimpleDateFormat df = new SimpleDateFormat(format);
        return df.format(new Date());
    }

    public static String getCurrentDateTime() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return df.format(new Date());
    }

    public static String getCurrentDateTime(String format) {
        if (StringUtils.isEmpty(format)) {
            format = "yyyy-MM-dd HH:mm:ss";
        }

        SimpleDateFormat df = new SimpleDateFormat(format);
        return df.format(new Date());
    }

    public static String date2String(Date date, String format) {
        if (null == date) {
            return null;
        } else {
            if (StringUtils.isEmpty(format)) {
                format = "yyyy-MM-dd HH:mm:ss";
            }

            SimpleDateFormat df = new SimpleDateFormat(format);
            return df.format(date);
        }
    }

    public static String date2String(Date date) {
        if (null == date) {
            return null;
        } else {
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return df.format(date);
        }
    }

    public static Date string2Date(String date, String format) {
        if (StringUtils.isEmpty(format)) {
            format = "yyyy-MM-dd HH:mm:ss";
        }

        SimpleDateFormat df = new SimpleDateFormat(format);

        try {
            return df.parse(date);
        } catch (ParseException var4) {
            log.error("Date parse error:", var4);
            return null;
        }
    }

    public static Date string2Date(String date) {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        try {
            return df.parse(date);
        } catch (ParseException var3) {
            log.error("Date parse error:", var3);
            return null;
        }
    }

    public static long getTimeInMillis(String dateTime) {
        long ret = 0L;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        try {
            ret = simpleDateFormat.parse(dateTime).getTime();
        } catch (ParseException var5) {
            log.error("日期格式解析错误", var5);
        }

        return ret;
    }

    public static Date getLastHour() {
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        c.add(11, -1);
        Date hDate = c.getTime();
        return hDate;
    }

    public static Date getLastDay() {
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        c.add(5, -1);
        Date hDate = c.getTime();
        return hDate;
    }

    public static Date getLastWeek() {
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        c.add(5, -7);
        Date wDate = c.getTime();
        return wDate;
    }

    public static Date getLastMonth() {
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        c.add(2, -1);
        Date mDate = c.getTime();
        return mDate;
    }

    public static Date getLastYear() {
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        c.add(1, -1);
        Date yDate = c.getTime();
        return yDate;
    }

    public static long getPreHourTime(Date dataTime) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(dataTime);
        calendar.set(11, calendar.get(11) - 1);
        return calendar.getTimeInMillis();
    }

    public static long getPreMinMillis(Date dataTime, int min) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(dataTime);
        calendar.set(12, calendar.get(12) - min);
        return calendar.getTimeInMillis();
    }

    public static long getAfterMinMillis(Date dataTime, int min) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(dataTime);
        calendar.set(12, calendar.get(12) + min);
        return calendar.getTimeInMillis();
    }

    public static long getMillis(Date dataTime) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(dataTime);
        return calendar.getTimeInMillis();
    }

    public static String getAfterMinTime(int min, String format) {
        long curren = System.currentTimeMillis();
        curren += (long)(min * 60 * 1000);
        Date date = new Date(curren);
        if (null == format || format.length() == 0) {
            format = "yyyy-MM-dd HH:mm:ss";
        }

        return date2String(date, format);
    }

    public static String getBeforDayDateTime(String format, int befor) {
        if (null == format || format.length() == 0) {
            format = "yyyy-MM-dd HH:mm:ss";
        }

        Date dNow = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(dNow);
        calendar.add(5, -1);
        Date dBefore = calendar.getTime();
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(dBefore);
    }

    public static String getIso8601Datetime() {
        return toIso8601UtcDatetime(Instant.now());
    }

    public static String toIso8601UtcDatetime(Instant instant) {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssX").withZone(ZoneOffset.UTC).format(instant);
    }

    public static String toIso8601LocalDatetime(Instant instant) {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss").withZone(ZoneId.systemDefault()).format(instant);
    }

    /**
     * 格式化
     * @param timestampe 时间戳
     * @return
     */
    public static String formart(Long timestampe) {
        LocalDateTime localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestampe), ZoneId.systemDefault());
        return localDateTime.format(DATE_TIME_FORMATTER);
    }
}
