/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas.util;


import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicLong;

import static org.apache.atlas.util.AtlasMetricsCounter.Period.*;

public class AtlasMetricsCounter {
    public enum Period { ALL, CURR_DAY, CURR_HOUR, PREV_HOUR, PREV_DAY };

    private final String  name;
    private final Stats   stats;
    private       Clock   clock;
    private       Instant lastIncrTime;
    private       Instant dayStartTime;
    private       Instant dayEndTime;
    private       Instant hourStartTime;
    private       Instant hourEndTime;

    public AtlasMetricsCounter(String name) {
        this(name, Clock.systemUTC());
    }

    public AtlasMetricsCounter(String name, Clock clock) {
        this.name  = name;
        this.stats = new Stats();

        init(clock);
    }

    public String getName() { return name; }

    public Instant getLastIncrTime() { return lastIncrTime; }

    public void incr() {
        incrByWithMeasure(1, 0);
    }

    public void incrBy(long count) {
        incrByWithMeasure(count, 0);
    }

    public void incrWithMeasure(long measure) {
        incrByWithMeasure(1, measure);
    }

    public void incrByWithMeasure(long count, long measure) {
        Instant instant  = clock.instant();

        stats.addCount(ALL, count);
        stats.addMeasure(ALL, measure);

        if (instant.isAfter(dayStartTime)) { // ignore times earlier than start of current day
            lastIncrTime = instant;

            updateForTime(instant);

            stats.addCount(CURR_DAY, count);
            stats.addMeasure(CURR_DAY, measure);

            if (instant.isAfter(hourStartTime)) { // ignore times earlier than start of current hour
                stats.addCount(CURR_HOUR, count);
                stats.addMeasure(CURR_HOUR, measure);
            }
        }
    }

    public StatsReport report() {
        updateForTime(clock.instant());

        return new StatsReport(stats, dayStartTime.toEpochMilli(), hourStartTime.toEpochMilli());
    }

    // visible only for testing
    void init(Clock clock) {
        this.clock         = clock;
        this.lastIncrTime  = Instant.ofEpochSecond(0);
        this.dayStartTime  = Instant.ofEpochSecond(0);
        this.dayEndTime    = Instant.ofEpochSecond(0);
        this.hourStartTime = Instant.ofEpochSecond(0);
        this.hourEndTime   = Instant.ofEpochSecond(0);

        updateForTime(clock.instant());
    }

    protected void updateForTime(Instant now) {
        Instant dayEndTime  = this.dayEndTime;
        Instant hourEndTime = this.hourEndTime;

        if (now.isAfter(dayEndTime)) {
            rolloverDay(dayEndTime, now);
            rolloverHour(hourEndTime, now);
        } else if (now.isAfter(hourEndTime)) {
            rolloverHour(hourEndTime, now);
        }
    }

    protected synchronized void rolloverDay(Instant fromDayEndTime, Instant now) {
        if (fromDayEndTime == dayEndTime) { // only if rollover was not done already
            Instant dayStartTime = getDayStartTime(now);

            if (dayStartTime.equals(dayEndTime)) {
                stats.copy(CURR_DAY, PREV_DAY);
            } else {
                stats.reset(PREV_DAY);
            }

            stats.reset(CURR_DAY);

            this.dayStartTime = dayStartTime;
            this.dayEndTime   = getNextDayStartTime(now);
        }
    }

    protected synchronized void rolloverHour(Instant fromHourEndTime, Instant now) {
        if (fromHourEndTime == hourEndTime) { // only if rollover was not done already
            Instant hourStartTime = getHourStartTime(now);

            if (hourStartTime.equals(hourEndTime)) {
                stats.copy(CURR_HOUR, PREV_HOUR);
            } else {
                stats.reset(PREV_HOUR);
            }

            stats.reset(CURR_HOUR);

            this.hourStartTime = hourStartTime;
            this.hourEndTime   = getNextHourStartTime(now);
        }
    }

    public static LocalDateTime getLocalDateTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    public static Instant getHourStartTime(Instant instant) {
        LocalDateTime time = getLocalDateTime(instant);

        return LocalDateTime.of(time.toLocalDate(), LocalTime.MIN).plusHours(time.getHour()).toInstant(ZoneOffset.UTC);
    }

    public static Instant getNextHourStartTime(Instant instant) {
        LocalDateTime time = getLocalDateTime(instant);

        return LocalDateTime.of(time.toLocalDate(), LocalTime.MIN).plusHours(time.getHour() + 1).toInstant(ZoneOffset.UTC);
    }

    public static Instant getDayStartTime(Instant instant) {
        LocalDateTime time = getLocalDateTime(instant);

        return LocalDateTime.of(time.toLocalDate(), LocalTime.MIN).toInstant(ZoneOffset.UTC);
    }

    public static Instant getNextDayStartTime(Instant instant) {
        LocalDateTime time = getLocalDateTime(instant);

        return LocalDateTime.of(time.toLocalDate().plusDays(1), LocalTime.MIN).toInstant(ZoneOffset.UTC);
    }

    public static class Stats {
        private static final int NUM_PERIOD = Period.values().length;

        private final long         dayStartTimeMs;
        private final long         hourStartTimeMs;
        private final AtomicLong[] count           = new AtomicLong[NUM_PERIOD];
        private final AtomicLong[] measureSum      = new AtomicLong[NUM_PERIOD];
        private final AtomicLong[] measureMin      = new AtomicLong[NUM_PERIOD];
        private final AtomicLong[] measureMax      = new AtomicLong[NUM_PERIOD];


        public Stats() {
            dayStartTimeMs  = 0;
            hourStartTimeMs = 0;

            for (Period period : Period.values()) {
                reset(period);
            }
        }

        public void addCount(Period period, long num) {
            count[period.ordinal()].addAndGet(num);
        }

        public void addMeasure(Period period, long measure) {
            int idx = period.ordinal();

            measureSum[idx].addAndGet(measure);

            if (measureMin[idx].get() > measure) {
                measureMin[idx].set(measure);
            }

            if (measureMax[idx].get() < measure) {
                measureMax[idx].set(measure);
            }
        }

        private void copy(Period src, Period dest) {
            int srcIdx  = src.ordinal();
            int destIdx = dest.ordinal();

            count[destIdx].set(count[srcIdx].get());
            measureSum[destIdx].set(measureSum[srcIdx].get());
            measureMin[destIdx].set(measureMin[srcIdx].get());
            measureMax[destIdx].set( measureMax[srcIdx].get());
        }

        private void reset(Period period) {
            int idx = period.ordinal();

            count[idx]      = new AtomicLong(0);
            measureSum[idx] = new AtomicLong(0);
            measureMin[idx] = new AtomicLong(Long.MAX_VALUE);
            measureMax[idx] = new AtomicLong(Long.MIN_VALUE);
        }

    }

    public static class StatsReport {
        private static final int NUM_PERIOD = Period.values().length;

        private final long   dayStartTimeMs;
        private final long   hourStartTimeMs;
        private final long[] count           = new long[NUM_PERIOD];
        private final long[] measureSum      = new long[NUM_PERIOD];
        private final long[] measureMin      = new long[NUM_PERIOD];
        private final long[] measureMax      = new long[NUM_PERIOD];


        public StatsReport(Stats other, long dayStartTimeMs, long hourStartTimeMs) {
            this.dayStartTimeMs  = dayStartTimeMs;
            this.hourStartTimeMs = hourStartTimeMs;

            copy(other.count, this.count);
            copy(other.measureSum, this.measureSum);
            copy(other.measureMin, this.measureMin);
            copy(other.measureMax, this.measureMax);
        }

        public long getDayStartTimeMs() { return dayStartTimeMs; }

        public long getHourStartTimeMs() { return hourStartTimeMs; }

        public long getCount(Period period) { return count[period.ordinal()]; }

        public long getMeasureSum(Period period) { return measureSum[period.ordinal()]; }

        public long getMeasureMin(Period period) { return measureMin[period.ordinal()]; }

        public long getMeasureMax(Period period) { return measureMax[period.ordinal()]; }

        public long getMeasureAvg(Period period) {
            int  idx = period.ordinal();
            long c   = count[idx];

            return c != 0 ? (measureSum[idx] / c) : 0;
        }

        private void copy(AtomicLong[] src, long[] dest) {
            for (int i = 0; i < dest.length; i++) {
                dest[i] = src[i].get();
            }
        }
    }
}
