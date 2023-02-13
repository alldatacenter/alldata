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

package org.apache.inlong.sdk.sort.util;

import org.apache.inlong.sdk.sort.entity.InLongTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class TimeUtil {

    private static final Logger logger = LoggerFactory.getLogger(TimeUtil.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String KEY_SDK_START_TIME = "sortSdk.startTime";
    private static final String KEY_SDK_STOP_TIME = "sortSdk.stopTime";
    private static final long DEFAULT_START_TIME = -1L;
    private static final long DEFAULT_STOP_TIME = Long.MAX_VALUE;

    public static long parseStartTime(InLongTopic inLongTopic) {
        return Optional.ofNullable(inLongTopic.getProperties().get(KEY_SDK_START_TIME))
                .map(s -> {
                    try {
                        LocalDateTime time = LocalDateTime.parse(s.toString(), DATE_FORMAT);
                        return LocalDateTime.from(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                    } catch (Throwable t) {
                        logger.error("parse start time failed, plz check the format of start time : {}", s, t);
                    }
                    return DEFAULT_START_TIME;
                })
                .orElse(DEFAULT_START_TIME);
    }

    public static long parseStopTime(InLongTopic inLongTopic) {
        return Optional.ofNullable(inLongTopic.getProperties().get(KEY_SDK_STOP_TIME))
                .map(s -> {
                    try {
                        LocalDateTime time = LocalDateTime.parse(s.toString(), DATE_FORMAT);
                        return LocalDateTime.from(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                    } catch (Throwable t) {
                        logger.error("parse start time failed, plz check the format of stop time : {}", s, t);
                    }
                    return DEFAULT_STOP_TIME;
                })
                .orElse(DEFAULT_STOP_TIME);
    }

}
