/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.tubemq.corebase.utils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * Datetime, string and timestamp conversion tools
 *
 * This class includes the date and time-related methods, for unified use and management.
 *
 */
public class DateTimeConvertUtils {

    private static final ZoneId defZoneId = ZoneId.systemDefault();
    public static final String PAT_YYYYMMDDHHMM = "yyyyMMddHHmm";
    public static final int LENGTH_YYYYMMDDHHMM = PAT_YYYYMMDDHHMM.length();
    private static final DateTimeFormatter sdf4yyyyMMddHHmm
            = DateTimeFormatter.ofPattern(PAT_YYYYMMDDHHMM);
    public static final String PAT_YYYYMMDDHHMMSS = "yyyyMMddHHmmss";
    public static final int LENGTH_YYYYMMDDHHMMSS = PAT_YYYYMMDDHHMMSS.length();
    private static final DateTimeFormatter sdf4yyyyMMddHHmmss
            = DateTimeFormatter.ofPattern(PAT_YYYYMMDDHHMMSS);

    /**
     * Converts the specified timestamp value to a string
     * in the format yyyyMMddHHmm under the current system default time zone
     *
     * @param timestamp The millisecond value of the specified time
     * @return the time string in yyyyMMddHHmm format
     */
    public static String ms2yyyyMMddHHmm(long timestamp) {
        return ms2yyyyMMddHHmm(timestamp, defZoneId);
    }

    /**
     * Converts the specified timestamp value to a string
     * in the format yyyyMMddHHmm under the specified time zone
     *
     * @param timestamp The millisecond value of the specified time
     * @param zoneId the specified time zone
     * @return the time string in yyyyMMddHHmm format
     */
    public static String ms2yyyyMMddHHmm(long timestamp, ZoneId zoneId) {
        LocalDateTime  localDateTime =
                LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), zoneId);
        return sdf4yyyyMMddHHmm.format(localDateTime);
    }

    /**
     * Converts the time string in yyyyMMddHHmm format to timestamp value
     * under the current system default time zone
     *
     * @param yyyyMMddHHmm the time string in yyyyMMddHHmm format
     * @return the timestamp value
     */
    public static long yyyyMMddHHmm2ms(String yyyyMMddHHmm) {
        return yyyyMMddHHmm2ms(yyyyMMddHHmm, defZoneId);
    }

    /**
     * Converts the time string in yyyyMMddHHmm format to timestamp value
     * under the specified time zone
     *
     * @param yyyyMMddHHmm the time string in yyyyMMddHHmm format
     * @param zoneId the specified time zone
     * @return the timestamp value
     */
    public static long yyyyMMddHHmm2ms(String yyyyMMddHHmm, ZoneId zoneId) {
        LocalDateTime localDateTime =
                LocalDateTime.parse(yyyyMMddHHmm, sdf4yyyyMMddHHmm);
        return LocalDateTime.from(localDateTime).atZone(zoneId).toInstant().toEpochMilli();
    }

    /**
     * Converts the specified timestamp value to a string
     * in the format yyyyMMddHHmmss under the current system default time zone
     *
     * @param timestamp The millisecond value of the specified time
     * @return the time string in yyyyMMddHHmmss format
     */
    public static String ms2yyyyMMddHHmmss(long timestamp) {
        return ms2yyyyMMddHHmmss(timestamp, defZoneId);
    }

    /**
     * Converts the specified timestamp value to a string
     * in the format yyyyMMddHHmmss under the specified time zone
     *
     * @param timestamp The millisecond value of the specified time
     * @param zoneId the specified time zone
     * @return the time string in yyyyMMddHHmmss format
     */
    public static String ms2yyyyMMddHHmmss(long timestamp, ZoneId zoneId) {
        LocalDateTime  localDateTime =
                LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), zoneId);
        return sdf4yyyyMMddHHmmss.format(localDateTime);
    }

    /**
     * Converts the specified timestamp value to a string
     * in the format yyyyMMddHHmmss under the current system default time zone
     *
     * @param date the date time
     * @return the string in yyyyMMddHHmmss format
     */
    public static String date2yyyyMMddHHmmss(Date date) {
        return date2yyyyMMddHHmmss(date, defZoneId);
    }

    /**
     * Converts the specified timestamp value to a string
     * in the format yyyyMMddHHmmss under the specified time zone
     *
     * @param date the date time
     * @param zoneId the specified time zone
     * @return the string in yyyyMMddHHmmss format
     */
    public static String date2yyyyMMddHHmmss(Date date, ZoneId zoneId) {
        if (date == null) {
            return TStringUtils.EMPTY;
        }
        try {
            LocalDateTime  localDateTime =
                    LocalDateTime.ofInstant(date.toInstant(), zoneId);
            return sdf4yyyyMMddHHmmss.format(localDateTime);
        } catch (Throwable ex) {
            return TStringUtils.EMPTY;
        }
    }

    /**
     * Converts the time string in yyyyMMddHHmmss format to timestamp value
     * under the current system default time zone
     *
     * @param yyyyMMddHHmmss the time string in yyyyMMddHHmmss format
     * @return the timestamp value
     */
    public static long yyyyMMddHHmmss2ms(String yyyyMMddHHmmss) {
        return yyyyMMddHHmmss2ms(yyyyMMddHHmmss, defZoneId);
    }

    /**
     * Converts the time string in yyyyMMddHHmmss format to timestamp value
     * under the specified time zone
     *
     * @param yyyyMMddHHmmss the time string in yyyyMMddHHmmss format
     * @param zoneId the specified time zone
     * @return the timestamp value
     */
    public static long yyyyMMddHHmmss2ms(String yyyyMMddHHmmss, ZoneId zoneId) {
        LocalDateTime localDateTime =
                LocalDateTime.parse(yyyyMMddHHmmss, sdf4yyyyMMddHHmmss);
        return LocalDateTime.from(localDateTime).atZone(zoneId).toInstant().toEpochMilli();
    }

    /**
     * Converts the time string in yyyyMMddHHmmss format to date value
     * under the specified time zone
     *
     * @param yyyyMMddHHmmss the time string in yyyyMMddHHmmss format
     * @return the Date value
     */
    public static Date yyyyMMddHHmmss2date(String yyyyMMddHHmmss) {
        return yyyyMMddHHmmss2date(yyyyMMddHHmmss, defZoneId);
    }

    /**
     * Converts the time string in yyyyMMddHHmmss format to date value
     * under the specified time zone
     *
     * @param yyyyMMddHHmmss the time string in yyyyMMddHHmmss format
     * @param zoneId the specified time zone
     * @return the Date value
     */
    public static Date yyyyMMddHHmmss2date(String yyyyMMddHHmmss, ZoneId zoneId) {
        if (yyyyMMddHHmmss == null) {
            return null;
        }
        try {
            LocalDateTime localDateTime =
                    LocalDateTime.parse(yyyyMMddHHmmss, sdf4yyyyMMddHHmmss);
            return Date.from(localDateTime.atZone(zoneId).toInstant());
        } catch (Throwable ex) {
            return null;
        }
    }
}
