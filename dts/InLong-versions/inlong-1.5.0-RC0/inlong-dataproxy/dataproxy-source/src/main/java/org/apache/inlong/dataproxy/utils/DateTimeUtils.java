/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.dataproxy.utils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class DateTimeUtils {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
    private static final ZoneId defZoneId = ZoneId.systemDefault();

    /**
     * convert ms value to 'yyyyMMddHHmm' string
     *
     * @param timestamp The millisecond value of the specified time
     * @return the time string in yyyyMMddHHmm format
     */
    public static String ms2yyyyMMddHHmm(long timestamp) {
        LocalDateTime localDateTime =
                LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), defZoneId);
        return DATE_FORMATTER.format(localDateTime);
    }
}
