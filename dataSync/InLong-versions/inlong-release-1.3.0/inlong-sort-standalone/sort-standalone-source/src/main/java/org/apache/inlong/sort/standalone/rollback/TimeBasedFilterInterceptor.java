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

package org.apache.inlong.sort.standalone.rollback;

import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.interceptor.Interceptor;
import org.apache.inlong.sort.standalone.channel.ProfileEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Interceptor that filters events selectively based on a configured time interval.
 * Usually, it's used to roll back data in a certain time interval.
 * It supports config either one of start, stop time or both of them.
 * Multiple TimeBasedFilterInterceptor can be chained together to create more complex time intervals.
 */
public class TimeBasedFilterInterceptor implements Interceptor {

    private static final Logger logger = LoggerFactory.getLogger(TimeBasedFilterInterceptor.class);
    private final long startTime;
    private final long stopTime;

    public TimeBasedFilterInterceptor(long startTime, long stopTime) {
        this.startTime = startTime;
        this.stopTime = stopTime;
        logger.info("create TimeBasedFilterInterceptor successfully.");
    }

    @Override
    public void initialize() {
        // no-op
    }

    @Override
    public Event intercept(Event event) {
        long logTime;
        ProfileEvent profile;
        if (event instanceof ProfileEvent) {
            profile = (ProfileEvent) event;
            logTime = profile.getRawLogTime();
        } else {
            return event;
        }

        if (logTime > stopTime || logTime < startTime) {
            profile.ack();
            return null;
        }
        return event;
    }

    @Override
    public List<Event> intercept(List<Event> events) {
        return events.stream()
                .map(this::intercept)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public void close() {
        // no-op
    }

    /**
     * Builder of {@link TimeBasedFilterInterceptor}.
     * Should be configured before build called.
     */
    public static class Builder implements Interceptor.Builder  {

        private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        private static final String START_TIME = "rollback.startTime";
        private static final long DEFAULT_START_TIME = 0L;
        private static final String STOP_TIME = "rollback.stopTime";
        private static final long DEFAULT_STOP_TIME = Long.MAX_VALUE;

        private long startTime;
        private long stopTime;

        @Override
        public Interceptor build() {
            return new TimeBasedFilterInterceptor(startTime, stopTime);
        }

        @Override
        public void configure(Context context) {
            startTime = Optional.ofNullable(context.getString(START_TIME))
                    .map(s -> {
                        logger.info("config TimeBasedFilterInterceptor, start time is {}", s);
                        try {
                            return DATE_FORMAT.parse(s).getTime();
                        } catch (ParseException e) {
                            logger.error("parse start time failed, plz check the format of start time : {}", s);
                        }
                        return DEFAULT_START_TIME;
                    })
                    .orElse(DEFAULT_START_TIME);

            stopTime = Optional.ofNullable(context.getString(STOP_TIME))
                    .map(s -> {
                        logger.info("config TimeBasedFilterInterceptor, stop time is {}", s);
                        try {
                            return DATE_FORMAT.parse(s).getTime();
                        } catch (ParseException e) {
                            logger.error("parse stop time failed, plz check the format of stop time : {}", s);
                        }
                        return DEFAULT_STOP_TIME;
                    })
                    .orElse(DEFAULT_STOP_TIME);
        }
    }

}
