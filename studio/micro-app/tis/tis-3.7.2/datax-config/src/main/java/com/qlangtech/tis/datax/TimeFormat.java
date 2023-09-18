package com.qlangtech.tis.datax;

import org.apache.commons.lang3.StringUtils;

import java.time.*;
import java.time.format.DateTimeFormatter;

/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2023-02-23 13:31
 **/
public enum TimeFormat {
    yyyyMMddHHmmss(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")), yyyyMMdd(DateTimeFormatter.ofPattern("yyyyMMdd"));

    public static final ZoneId sysZoneId = ZoneOffset.systemDefault();
    public final DateTimeFormatter timeFormatter;

    TimeFormat(DateTimeFormatter timeFormatter) {
        this.timeFormatter = timeFormatter;
    }

    public static long getCurrentTimeStamp() {
        //  Clock.systemDefaultZone().
        return Clock.system(sysZoneId).millis();
        //  return LocalDateTime.now().toInstant()
    }

    public String format(long epochMilli) {
        return LocalDateTime.ofInstant(
                Instant.ofEpochMilli(epochMilli), sysZoneId).format(this.timeFormatter);
    }

    public static TimeFormat parse(String token) {

        for (TimeFormat f : TimeFormat.values()) {
            if (StringUtils.endsWithIgnoreCase(f.name(), token)) {
                return f;
            }
        }
        throw new IllegalStateException("illegal token:" + token);
    }
}
