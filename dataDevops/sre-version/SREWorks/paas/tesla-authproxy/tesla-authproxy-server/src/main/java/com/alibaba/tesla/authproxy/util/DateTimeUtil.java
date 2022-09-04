package com.alibaba.tesla.authproxy.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * 日期时间公共组件
 *
 * @author tandong.td@alibaba-inc.com
 */
public class DateTimeUtil {

    static final Logger LOG = LoggerFactory.getLogger(DateTimeUtil.class);

    /**
     * yyyy-MM-dd
     */
    public static final String DATE_FORMAT1 = "yyyy-MM-dd";

    /**
     * yyyy/MM/dd
     */
    public static final String DATE_FORMAT2 = "yyyy/MM/dd";

    /**
     * yyyyMMdd
     */
    public static final String DATE_FORMAT3 = "yyyyMMdd";

    /**
     * yyyy-MM-dd HH:mm:ss
     */
    public static final String DATE_TIME_FORMAT1 = "yyyy-MM-dd HH:mm:ss";

    /**
     * yyyy/MM/dd HH:mm:ss
     */
    public static final String DATE_TIME_FORMAT2 = "yyyy/MM/dd HH:mm:ss";

    /**
     * yyyyMMdd HH:mm:ss
     */
    public static final String DATE_TIME_FORMAT3 = "yyyyMMdd HH:mm:ss";

    /**
     * yyyyMMdd HH:mm
     */
    public static final String DATE_TIME_FORMAT4 = "yyyy-MM-dd HH:mm";

    /**
     * 获取系统当前日期 默认格式，yyyy-MM-dd
     *
     * @return
     */
    public static String getCurrentDate() {
        SimpleDateFormat df = new SimpleDateFormat(DATE_FORMAT1);
        return df.format(new Date());
    }

    /**
     * 获取系统当前日期 指定格式,如yyyyMMdd
     *
     * @return
     */
    public static String getCurrentDate(String format) {
        if (StringUtils.isEmpty(format)) {
            format = DATE_TIME_FORMAT1;
        }
        SimpleDateFormat df = new SimpleDateFormat(format);
        return df.format(new Date());
    }

    /**
     * 获取系统当前日期时间 默认格式，yyyy-MM-dd HH:mm:ss
     *
     * @return
     */
    public static String getCurrentDateTime() {
        SimpleDateFormat df = new SimpleDateFormat(DATE_TIME_FORMAT1);
        return df.format(new Date());
    }

    /**
     * 获取系统当前日期时间 指定格式,如yyyyMMdd HH:mm:ss
     *
     * @return
     */
    public static String getCurrentDateTime(String format) {
        if (StringUtils.isEmpty(format)) {
            format = DATE_TIME_FORMAT1;
        }
        SimpleDateFormat df = new SimpleDateFormat(format);
        return df.format(new Date());
    }

    /**
     * 将DATE转换为String类型
     *
     * @param date
     * @param format
     * @return
     */
    public static String date2String(Date date, String format) {
        if (null == date) {
            return null;
        }
        if (StringUtils.isEmpty(format)) {
            format = DATE_TIME_FORMAT1;
        }
        SimpleDateFormat df = new SimpleDateFormat(format);
        return df.format(date);
    }

    public static String date2String(Date date) {
        if (null == date) {
            return null;
        }
        SimpleDateFormat df = new SimpleDateFormat(DATE_TIME_FORMAT1);
        return df.format(date);
    }

    /**
     * 将String转换为Date类型
     *
     * @param date
     * @param format
     * @return
     */
    public static Date string2Date(String date, String format) {
        if (StringUtils.isEmpty(format)) {
            format = DATE_TIME_FORMAT1;
        }
        SimpleDateFormat df = new SimpleDateFormat(format);
        try {
            return df.parse(date);
        } catch (ParseException e) {
            LOG.error("Date parse error:", e);
            // do nothing
            return null;
        }
    }

    public static Date string2Date(String date) {
        SimpleDateFormat df = new SimpleDateFormat(DATE_TIME_FORMAT1);
        try {
            return df.parse(date);
        } catch (ParseException e) {
            LOG.error("Date parse error:", e);
            return null;
        }
    }

    /**
     * 根据日期时间获取毫秒数
     *
     * @param dateTime 日期时间
     * @return 毫秒数
     */
    public static long getTimeInMillis(String dateTime) {
        long ret = 0;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATE_TIME_FORMAT1);
        try {
            ret = simpleDateFormat.parse(dateTime).getTime();
        } catch (ParseException e) {
            LOG.error("日期格式解析错误", e);
        }
        return ret;
    }

    /**
     * 获取一小时前时间
     * @return
     */
    public static Date getLastHour() {
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        c.add(Calendar.HOUR_OF_DAY, -1);
        Date hDate = c.getTime();
        return hDate;
    }

    /**
     * 获取一天前时间
     * @return
     */
    public static Date getLastDay() {
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        c.add(Calendar.DAY_OF_MONTH, -1);
        Date hDate = c.getTime();
        return hDate;
    }

    /**
     * 获取一周前时间
     * @return
     */
    public static Date getLastWeek() {
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        c.add(Calendar.DATE, - 7);
        Date wDate = c.getTime();
        return wDate;
    }

    /**
     * 获取一个月前时间
     * @return
     */
    public static Date getLastMonth() {
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        c.add(Calendar.MONTH, -1);
        Date mDate = c.getTime();
        return mDate;
    }

    /**
     * 获取一年前时间
     * @return
     */
    public static Date getLastYear() {
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        c.add(Calendar.YEAR, -1);
        Date yDate = c.getTime();
        return yDate;
    }



    /**
     * 获取指定时间上一小时的时间戳
     *
     * @param dataTime
     * @return
     */
    public static long getPreHourTime(Date dataTime) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(dataTime);
        calendar.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY) - 1);
        return calendar.getTimeInMillis();
    }

    /**
     * 获取指定当前时间之前多少分钟的时间
     *
     * @param dataTime 指定时间Date
     * @param min      多少分钟之前
     * @return
     */
    public static long getPreMinMillis(Date dataTime, int min) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(dataTime);
        calendar.set(Calendar.MINUTE, calendar.get(Calendar.MINUTE) - min);
        return calendar.getTimeInMillis();
    }

    /**
     * 获取指定当前时间之后多少分钟的时间
     *
     * @param dataTime 指定时间Date
     * @param min      多少分钟之后
     * @return
     */
    public static long getAfterMinMillis(Date dataTime, int min) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(dataTime);
        calendar.set(Calendar.MINUTE, calendar.get(Calendar.MINUTE) + min);
        return calendar.getTimeInMillis();
    }

    /**
     * 获取指定date的毫秒数
     *
     * @param dataTime
     * @return
     */
    public static long getMillis(Date dataTime) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(dataTime);
        return calendar.getTimeInMillis();
    }

    /**
     * 获取当前时间指定分钟之后的时间
     *
     * @param min 指定多少分钟之后的时间
     * @return
     */
    public static String getAfterMinTime(int min, String format) {
        long curren = System.currentTimeMillis();
        curren += min * 60 * 1000;
        Date date = new Date(curren);
        if (null == format || format.length() == 0) {
            format = DATE_TIME_FORMAT1;
        }
        return date2String(date, format);
    }

    /**
     * 获取当前日期前几天的日期
     *
     * @param format 格式化，传空的化为默认
     * @param befor  之前多少天
     * @return
     */
    public static String getBeforDayDateTime(String format, int befor) {
        if (null == format || format.length() == 0) {
            format = DATE_TIME_FORMAT1;
        }
        Date dNow = new Date();
        Date dBefore;

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(dNow);
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        dBefore = calendar.getTime();

        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(dBefore);
    }

    /**
     * 获取 ISO8601 格式的当前时间字符串
     *
     * @return str 示例：2018-08-22T11:36:23Z
     */
    public static String getIso8601Datetime() {
        return toIso8601UtcDatetime(Instant.now());
    }

    /**
     * 将指定的时间转换 ISO8601 格式的当前时间字符串 (UTC)
     *
     * @param instant Instant 对象
     * @return str 示例：2018-08-22T11:36:23Z
     */
    public static String toIso8601UtcDatetime(Instant instant) {
        return DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ssX")
            .withZone(ZoneOffset.UTC)
            .format(instant);
    }

    /**
     * 将指定的时间转换 ISO8601 格式的当前时间字符串 (当地)
     *
     * @param instant Instant 对象
     * @return str 示例：2018-08-22T11:36:23
     */
    public static String toIso8601LocalDatetime(Instant instant) {
        return DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss")
            .withZone(ZoneId.systemDefault())
            .format(instant);
    }


}