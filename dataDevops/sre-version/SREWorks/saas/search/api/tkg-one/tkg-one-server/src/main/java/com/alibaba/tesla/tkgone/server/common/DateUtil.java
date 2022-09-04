package com.alibaba.tesla.tkgone.server.common;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 常用日期处理函数
 *
 * @author feiquan
 */
public class DateUtil {
    /**
     * 日期格式
     */
    public static final String PATTERN_YYYYMMDD_HHMMSS = "yyyy-MM-dd HH:mm:ss";
    public static final String PATTERN_YYYYMMDD_HHMM = "yyyy-MM-dd HH:mm";
    public static final String PATTERN_YYYYMMDD = "yyyy-MM-dd";
    /**
     * 对应：Tue Apr 16 16:43:15 CST 2019
     */
    public static final String PATTERN_ENGLISH_FULL = "EEE MMM dd HH:mm:ss z yyyy";
    /**
     * 对应：Tue, 07 May 2019 02:07:18 GMT  多用于Http头部
     */
    public static final String PATTERN_HTTP_HEADER = "EEE, dd MMM yyyy HH:mm:ss z";
    public static final String[] PATTERNS = new String[]{PATTERN_YYYYMMDD_HHMMSS, PATTERN_YYYYMMDD, PATTERN_ENGLISH_FULL, PATTERN_YYYYMMDD_HHMM};
    private static ThreadLocal<Map<String, SimpleDateFormat>> formaters = ThreadLocal.withInitial(HashMap::new);
    private static Date testNow;

    /**
     * 使用指定的格式来格式化字符串
     *
     * @param date
     * @param format
     * @return
     */
    public static String format(Date date, String format) {
        return getFormater(format).format(date);
    }

    /**
     * 使用指定格式进行时间解析
     *
     * @param text
     * @param pattern
     * @return
     * @throws ParseException
     */
    public static Date parse(String text, String pattern) {
        try {
            return getFormater(pattern).parse(text);
        } catch (ParseException e) {
            throw new IllegalArgumentException(String.format("Parse date failed: %s -> %s", pattern, text), e);
        }
    }

    private static SimpleDateFormat getFormater(String format) {
        Map<String, SimpleDateFormat> map = formaters.get();
        SimpleDateFormat formater = map.get(format);
        if (formater == null) {
            formater = new SimpleDateFormat(format, isEnglish(format) ? Locale.ENGLISH : Locale.CHINESE);
            map.put(format, formater);
        }
        return formater;
    }

    private static boolean isEnglish(String format) {
        return format.contains("EEE") || format.contains("MMM");
    }

    /**
     * 判断time是否在起止时间段内
     *
     * @param time
     * @param start
     * @param end
     * @return
     */
    public static boolean isBetween(Date time, Date start, Date end) {
        return time.getTime() >= start.getTime() && time.getTime() < end.getTime();
    }

    /**
     * 判断两个时间段是否存在交集
     *
     * @param start1
     * @param end1
     * @param start2
     * @param end2
     * @return
     */
    public static boolean isRelate(Date start1, Date end1, Date start2, Date end2) {
        return isBetween(start1, start2, end2)
            || isBetween(end1, start2, end2)
            || start1.getTime() <= start2.getTime() && end1.getTime() > end2.getTime();
    }

    /**
     * 获取当前时间，如果是日常环境，则可以返回指定时间，主要用于单元测试或日常测试
     *
     * @return
     */
    public static Date now() {
        return testNow != null ? testNow : new Date();
    }

    /**
     * 设置单元测试或日志环境的当前时间
     *
     * @param testNow
     */
    public static void setTestNow(Date testNow) {
        DateUtil.testNow = testNow;
    }

    /**
     * 使用常见的日期格式模式来逐个尝试解析
     * @param text
     * @return
     * @throws ParseException
     */
    public static Date parse(String text) {
        for (String pattern : PATTERNS) {
            try {
                return parse(text, pattern);
            } catch (IllegalArgumentException e) {
                // try next pattern
            }
        }
        throw new IllegalArgumentException("Invalid date: " + text);
    }
}
