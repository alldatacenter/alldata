/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
*/

package org.apache.griffin.core.util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimeUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(TimeUtil
        .class);
    private static final String MILLISECONDS_PATTERN =
        "(?i)m(illi)?s(ec(ond)?)?";
    private static final String SECONDS_PATTERN =
        "(?i)s(ec(ond)?)?";
    private static final String MINUTES_PATTERN =
        "(?i)m(in(ute)?)?";
    private static final String HOURS_PATTERN =
        "(?i)h((ou)?r)?";
    private static final String DAYS_PATTERN =
        "(?i)d(ay)?";

    private static class TimeUnitPair {
        private long t;
        private String unit;

        TimeUnitPair(long t, String unit) {
            this.t = t;
            this.unit = unit;
        }
    }

    public static Long str2Long(String timeStr) {
        if (timeStr == null) {
            LOGGER.warn("Time string can not be empty.");
            return 0L;
        }
        String trimTimeStr = timeStr.trim();
        boolean positive = true;
        if (trimTimeStr.startsWith("-")) {
            trimTimeStr = trimTimeStr.substring(1);
            positive = false;
        }
        List<TimeUnitPair> list = getTimeUnitPairs(trimTimeStr);
        return str2Long(positive, list);
    }

    private static Long str2Long(boolean positive, List<TimeUnitPair> list) {
        long time = 0;
        for (TimeUnitPair tu : list) {
            long t = milliseconds(tu);
            if (positive) {
                time += t;
            } else {
                time -= t;
            }
        }
        return time;
    }

    private static List<TimeUnitPair> getTimeUnitPairs(String timeStr) {
        // "1d2h3m" -> "1d", "2h", "3m"
        String timePattern = "(?i)(\\d+)([a-zA-Z]+)";
        Pattern pattern = Pattern.compile(timePattern);
        Matcher matcher = pattern.matcher(timeStr);
        List<TimeUnitPair> list = new ArrayList<>();
        while (matcher.find()) {
            String num = matcher.group(1);
            String unit = matcher.group(2);
            TimeUnitPair tu = new TimeUnitPair(Long.valueOf(num), unit);
            list.add(tu);
        }
        return list;
    }

    private static Long milliseconds(TimeUnitPair tu) {
        long t = tu.t;
        String unit = tu.unit;
        if (unit.matches(MILLISECONDS_PATTERN)) {
            return milliseconds(t, TimeUnit.MILLISECONDS);
        } else if (unit.matches(SECONDS_PATTERN)) {
            return milliseconds(t, TimeUnit.SECONDS);
        } else if (unit.matches(MINUTES_PATTERN)) {
            return milliseconds(t, TimeUnit.MINUTES);
        } else if (unit.matches(HOURS_PATTERN)) {
            return milliseconds(t, TimeUnit.HOURS);
        } else if (unit.matches(DAYS_PATTERN)) {
            return milliseconds(t, TimeUnit.DAYS);
        } else {
            LOGGER.warn("Time string format ERROR. " +
                "It only supports d(day),h(hour), m(minute), " +
                "s(second), ms(millsecond). " +
                "Please check your time format.");
            return 0L;
        }
    }

    private static Long milliseconds(long duration, TimeUnit unit) {
        return unit.toMillis(duration);
    }

    public static String format(String timeFormat, long time, TimeZone timeZone) {
        String timePattern = "#(?:\\\\#|[^#])*#";
        Date t = new Date(time);
        Pattern ptn = Pattern.compile(timePattern);
        Matcher matcher = ptn.matcher(timeFormat);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String group = matcher.group();
            String content = group.substring(1, group.length() - 1);
            String pattern = refreshEscapeHashTag(content);
            SimpleDateFormat sdf = new SimpleDateFormat(pattern);
            sdf.setTimeZone(timeZone);
            matcher.appendReplacement(sb, sdf.format(t));
        }
        matcher.appendTail(sb);
        return refreshEscapeHashTag(sb.toString());
    }

    private static String refreshEscapeHashTag(String str) {
        String escapeHashTagPattern = "\\\\#";
        String hashTag = "#";
        return str.replaceAll(escapeHashTagPattern, hashTag);
    }

    public static TimeZone getTimeZone(String timezone) {
        if (StringUtils.isEmpty(timezone)) {
            return TimeZone.getDefault();
        }
        return TimeZone.getTimeZone(timezone);
    }

}
