/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.datavines.common.utils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;
import java.util.TimeZone;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * date utils
 */
@Slf4j
public class DateUtils {

    private static final Logger logger = LoggerFactory.getLogger(DateUtils.class);

    public static final String YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm:ss";

    public static final String YYYYMMDDHHMMSS = "yyyyMMddHHmmss";

    public static final String YYYYMMDD = "yyyyMMdd";

    /**
     * date to local datetime
     *
     * @param date date
     * @return local datetime
     */
    public static LocalDateTime date2LocalDateTime(Date date) {
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }

    /**
     * local datetime to date
     *
     * @param localDateTime local datetime
     * @return date
     */
    public static Date localDateTime2Date(LocalDateTime localDateTime) {
        Instant instant = localDateTime.atZone(ZoneId.systemDefault()).toInstant();
        return Date.from(instant);
    }

    /**
     * get current date str
     *
     * @return date string
     */
    public static String getCurrentTime() {
        return getCurrentTime(YYYY_MM_DD_HH_MM_SS);
    }

    /**
     * get the date string in the specified format of the current time
     *
     * @param format date format
     * @return date string
     */
    public static String getCurrentTime(String format) {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern(format));
    }

    /**
     * get the formatted date string
     *
     * @param date date
     * @param format e.g. yyyy-MM-dd HH:mm:ss
     * @return date string
     */
    public static String format(Date date, String format) {
        return format(date2LocalDateTime(date), format);
    }

    /**
     * get the formatted date string
     *
     * @param localDateTime local data time
     * @param format        yyyy-MM-dd HH:mm:ss
     * @return date string
     */
    public static String format(LocalDateTime localDateTime, String format) {
        return localDateTime.format(DateTimeFormatter.ofPattern(format));
    }

    /**
     * convert time to yyyy-MM-dd HH:mm:ss format
     *
     * @param date date
     * @return date string
     */
    public static String dateToString(Date date) {
        return format(date, YYYY_MM_DD_HH_MM_SS);
    }


    /**
     * convert string to date and time
     *
     * @param date date
     * @param format  format
     * @return date
     */
    public static Date parse(String date, String format) {
        try {
            LocalDateTime ldt = LocalDateTime.parse(date, DateTimeFormatter.ofPattern(format));
            return localDateTime2Date(ldt);
        } catch (Exception e) {
            logger.error("error while parse date:" + date, e);
        }
        return null;
    }


    /**
     * convert date str to yyyy-MM-dd HH:mm:ss format
     *
     * @param str date string
     * @return yyyy-MM-dd HH:mm:ss format
     */
    public static Date stringToDate(String str) {
        return parse(str, YYYY_MM_DD_HH_MM_SS);
    }

    /**
     * get seconds between two dates
     *
     * @param d1 date1
     * @param d2 date2
     * @return differ seconds
     */
    public static long differSec(Date d1, Date d2) {
        if(d1 == null || d2 == null){
            return 0;
        }
        return (long) Math.ceil(differMs(d1, d2) / 1000.0);
    }

    /**
     * get ms between two dates
     *
     * @param d1 date1
     * @param d2 date2
     * @return differ ms
     */
    public static long differMs(Date d1, Date d2) {
        return Math.abs(d1.getTime() - d2.getTime());
    }


    /**
     * get hours between two dates
     *
     * @param d1 date1
     * @param d2 date2
     * @return differ hours
     */
    public static long diffHours(Date d1, Date d2) {
        return (long) Math.ceil(diffMin(d1, d2) / 60.0);
    }

    /**
     * get minutes between two dates
     *
     * @param d1 date1
     * @param d2 date2
     * @return differ minutes
     */
    public static long diffMin(Date d1, Date d2) {
        return (long) Math.ceil(differSec(d1, d2) / 60.0);
    }


    /**
     * get the date of the specified date in the days before and after
     *
     * @param date date
     * @param day day
     * @return the date of the specified date in the days before and after
     */
    public static Date getSomeDay(Date date, int day) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DATE, day);
        return calendar.getTime();
    }

    /**
     * compare two dates
     *
     * @param future future date
     * @param old old date
     * @return true if future time greater than old time
     */
    public static boolean compare(Date future, Date old) {
        return future.getTime() > old.getTime();
    }

    /**
     * convert schedule string to date
     *
     * @param schedule schedule
     * @return convert schedule string to date
     */
    public static Date getScheduleDate(String schedule) {
        return stringToDate(schedule);
    }

    /**
     * format time to readable
     *
     * @param ms ms
     * @return format time
     */
    public static String format2Readable(long ms) {

        long days = ms / (1000 * 60 * 60 * 24);
        long hours = (ms % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60);
        long minutes = (ms % (1000 * 60 * 60)) / (1000 * 60);
        long seconds = (ms % (1000 * 60)) / 1000;

        return String.format("%02d %02d:%02d:%02d", days, hours, minutes, seconds);

    }

    /**
     * get monday
     *
     * note: Set the first day of the week to Monday, the default is Sunday
     * @param date date
     * @return get monday
     */
    public static Date getMonday(Date date) {
        Calendar cal = Calendar.getInstance();

        cal.setTime(date);

        cal.setFirstDayOfWeek(Calendar.MONDAY);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);

        return cal.getTime();
    }

    /**
     * get sunday
     *
     * note: Set the first day of the week to Monday, the default is Sunday
     * @param date date
     * @return get sunday
     */
    public static Date getSunday(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        cal.setFirstDayOfWeek(Calendar.MONDAY);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);

        return cal.getTime();
    }

    /**
     * get first day of month
     *
     * @param date date
     * @return first day of month
     * */
    public static Date getFirstDayOfMonth(Date date) {
        Calendar cal = Calendar.getInstance();

        cal.setTime(date);
        cal.set(Calendar.DAY_OF_MONTH, 1);

        return cal.getTime();
    }

    /**
     * get some hour of day
     *
     * @param date date
     * @param hours hours
     * @return some hour of day
     * */
    public static Date getSomeHourOfDay(Date date, int hours) {
        Calendar cal = Calendar.getInstance();

        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, cal.get(Calendar.HOUR_OF_DAY) - hours);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        return cal.getTime();
    }

    /**
     * get last day of month
     *
     * @param  date date
     * @return  get last day of month
     */
    public static Date getLastDayOfMonth(Date date) {
        Calendar cal = Calendar.getInstance();

        cal.setTime(date);

        cal.add(Calendar.MONTH, 1);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.add(Calendar.DAY_OF_MONTH, -1);

        return cal.getTime();
    }

    /**
     * return YYYY-MM-DD 00:00:00
     *
     * @param inputDay date
     * @return start day
     */
    public static Date getStartOfDay(Date inputDay) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(inputDay);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    /**
     * return YYYY-MM-DD 23:59:59
     *
     * @param inputDay day
     * @return end of day
     */
    public static Date getEndOfDay(Date inputDay) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(inputDay);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTime();
    }

    /**
     * return YYYY-MM-DD 00:00:00
     *
     * @param inputDay day
     * @return start of hour
     */
    public static Date getStartOfHour(Date inputDay) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(inputDay);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    /**
     * return YYYY-MM-DD 23:59:59
     *
     * @param inputDay day
     * @return end of hour
     */
    public static Date getEndOfHour(Date inputDay) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(inputDay);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTime();
    }

    /**
     * get current date
     * @return current date
     */
    public static Date getCurrentDate() {
        return DateUtils.parse(DateUtils.getCurrentTime(),
                YYYY_MM_DD_HH_MM_SS);
    }


    public static Long localDateTime2Long(LocalDateTime localDateTime) {
        Instant instant = localDateTime.atZone(ZoneId.systemDefault()).toInstant();
        return instant.toEpochMilli();
    }

    public static LocalDateTime long2LocalDateTime(long timestamp) {
        Instant instant = Instant.ofEpochMilli(timestamp);
        ZoneId zone = ZoneId.systemDefault();
        return LocalDateTime.ofInstant(instant, zone);
    }

    /**
     * get date
     *
     * @param date          date
     * @param calendarField calendarField
     * @param amount        amount
     * @return date
     */
    public static Date add(final Date date, final int calendarField, final int amount) {
        if (date == null) {
            throw new IllegalArgumentException("The date must not be null");
        }
        final Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(calendarField, amount);
        return c.getTime();
    }

    /**
     * starting from the current time, get how many seconds are left before the target time.
     * targetTime = baseTime + intervalSeconds
     *
     * @param baseTime        base time
     * @param intervalSeconds a period of time
     * @return the number of seconds
     */
    public static long getRemainTime(Date baseTime, long intervalSeconds) {
        if (baseTime == null) {
            return 0;
        }
        long usedTime = (System.currentTimeMillis() - baseTime.getTime()) / 1000;
        return intervalSeconds - usedTime;
    }

    public static long getRemainTime(Long baseTime, long intervalSeconds) {
        if (baseTime == null) {
            return 0;
        }
        long usedTime = (System.currentTimeMillis() - baseTime) / 1000;
        return intervalSeconds - usedTime;
    }

    /**
     * get current time stamp : yyyyMMddHHmmssSSS
     *
     * @return date string
     */
    public static String getCurrentTimeStamp() {
        return getCurrentTime(YYYYMMDDHHMMSS);
    }

    /**
     * get timezone by timezoneId
     */
    public static TimeZone getTimezone(String timezoneId) {
        if (StringUtils.isEmpty(timezoneId)) {
            return null;
        }
        return TimeZone.getTimeZone(timezoneId);
    }

    /**
     * Time unit representing one thousandth of a second
     */
    public static class MILLISECONDS {

        static final long C0 = 1L;
        static final long C1 = C0 * 1000L;
        static final long C2 = C1 * 1000L;
        static final long C3 = C2 * 1000L;
        static final long C4 = C3 * 60L;
        static final long C5 = C4 * 60L;
        static final long C6 = C5 * 24L;

        public static long toDays(long d) {
            return d / (C6 / C2);
        }

        public static long toDurationSeconds(long d) {
            return (d % (C4 / C2)) / (C3 / C2);
        }

        public static long toDurationMinutes(long d) {
            return (d % (C5 / C2)) / (C4 / C2);
        }

        public static long toDurationHours(long d) {
            return (d % (C6 / C2)) / (C5 / C2);
        }

    }

    /**
     * transform timeStamp to local date
     *
     * @param timeStamp time stamp (milliseconds)
     * @return local date
     */
    public static Date timeStampToDate(long timeStamp) {
        return timeStamp <= 0L ? null : new Date(timeStamp);
    }

    /**
     * transform date to timeStamp
     * @param date date
     * @return time stamp (milliseconds)
     */
    public static long dateToTimeStamp(Date date) {
        return date == null ? 0L : date.getTime();
    }

    /**
     * a default datetime formatter for the timestamp
     */
    private static final DateTimeFormatter DEFAULT_DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * @param timeMillis timeMillis like System.currentTimeMillis()
     * @return string formatted as yyyy-MM-dd HH:mm:ss
     */
    public static String formatTimeStamp(long timeMillis) {
        return formatTimeStamp(timeMillis, DEFAULT_DATETIME_FORMATTER);
    }

    /**
     * @param timeMillis timeMillis like System.currentTimeMillis()
     * @param dateTimeFormatter expect formatter, like yyyy-MM-dd HH:mm:ss
     * @return formatted string
     */
    public static String formatTimeStamp(long timeMillis, DateTimeFormatter dateTimeFormatter) {
        Objects.requireNonNull(dateTimeFormatter);
        return dateTimeFormatter.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(timeMillis),
                ZoneId.systemDefault()));
    }

    public static Date addMonths(Date date, int amount) {
        return add(date, 2, amount);
    }

    public static Date addDays(Date date, int amount) {
        return add(date, 5, amount);
    }

    public static Date addMinutes(Date date, int amount) {
        return add(date, 12, amount);
    }

    public static String getTimestampString() {
        return String.valueOf(System.currentTimeMillis());
    }
}
