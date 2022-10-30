/*
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

package org.apache.inlong.tubemq.client.common;

import java.util.Objects;
import org.apache.inlong.tubemq.corebase.utils.MixedUtils;

/**
 * StatsConfig, configuration settings related to client statistics
 *
 */
public class StatsConfig {
    // client statistics information print period
    private static final long STATS_SELF_PRINT_DEFAULT_PERIOD_MS = 6 * 1000 * 60L;
    private static final long STATS_SELF_PRINT_MIN_PERIOD_MS = 1000 * 60L;
    private static final long STATS_SELF_PRINT_MAX_PERIOD_MS = 60 * 1000 * 60L;
    // client statistics information print period
    private static final long STATS_AUTO_RESET_DEFAULT_PERIOD_MS = 30 * 60 * 1000L;
    private static final long STATS_AUTO_RESET_MIN_PERIOD_MS = 30 * 1000L;
    private static final long STATS_AUTO_RESET_MAX_PERIOD_MS = 24 * 3600 * 1000L;
    // data statistics level
    private StatsLevel statsLevel = StatsLevel.MEDIUM;
    // Enable metric information print
    private boolean enableSelfPrint = true;
    // Metric print period in ms.
    private long selfPrintPeriodMs = STATS_SELF_PRINT_DEFAULT_PERIOD_MS;
    // Metric reset value period in ms.
    private long forcedResetPeriodMs = STATS_AUTO_RESET_DEFAULT_PERIOD_MS;

    /**
     * Initialize instance with the default value
     *
     */
    public StatsConfig() {
        //
    }

    /**
     * Update current configure settings according to the specified configuration
     *
     * @param that   the specified configuration
     */
    public void updateStatsConfig(StatsConfig that) {
        if (that == null) {
            return;
        }
        updateStatsConfig(that.statsLevel, that.enableSelfPrint,
                that.selfPrintPeriodMs, that.forcedResetPeriodMs);
    }

    /**
     * Update current configure settings
     * Attention, if statsLevel is ZERO, then printing will be automatically turned off
     * regardless of the value of enableSelfPrint
     *
     * @param statsLevel          the statistics level
     * @param enableSelfPrint     whether to allow the SDK to print by itself
     * @param selfPrintPeriodMs   the time interval that the SDK prints itself
     * @param forcedResetPeriodMs the resets interval for collecting data
     */
    public void updateStatsConfig(StatsLevel statsLevel, boolean enableSelfPrint,
                                  long selfPrintPeriodMs, long forcedResetPeriodMs) {
        updateStatsControl(statsLevel, enableSelfPrint);
        setStatsPeriodInfo(selfPrintPeriodMs, forcedResetPeriodMs);
    }

    /**
     * Update current statistics control settings
     * Attention, if statsLevel is ZERO, then printing will be automatically turned off
     * regardless of the value of enableSelfPrint
     *
     * @param statsLevel          the statistics level
     * @param enableSelfPrint     whether to allow the SDK to print by itself
     */
    public void updateStatsControl(StatsLevel statsLevel, boolean enableSelfPrint) {
        this.statsLevel = statsLevel;
        this.enableSelfPrint = enableSelfPrint;
    }

    /**
     * Update current period settings
     *
     * @param selfPrintPeriodMs   the time interval that the SDK prints itself
     * @param forcedResetPeriodMs the resets interval for collecting data
     */
    public void setStatsPeriodInfo(long selfPrintPeriodMs,
                                   long forcedResetPeriodMs) {
        this.selfPrintPeriodMs =
                MixedUtils.mid(selfPrintPeriodMs,
                        STATS_SELF_PRINT_MIN_PERIOD_MS, STATS_SELF_PRINT_MAX_PERIOD_MS);
        this.forcedResetPeriodMs =
                MixedUtils.mid(forcedResetPeriodMs,
                        STATS_AUTO_RESET_MIN_PERIOD_MS, STATS_AUTO_RESET_MAX_PERIOD_MS);
    }

    public void setStatsLevel(StatsLevel statsLevel) {
        this.statsLevel = statsLevel;
    }

    public StatsLevel getStatsLevel() {
        return statsLevel;
    }

    public boolean isEnableSelfPrint() {
        return enableSelfPrint;
    }

    public long getSelfPrintPeriodMs() {
        return selfPrintPeriodMs;
    }

    public long getForcedResetPeriodMs() {
        return forcedResetPeriodMs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StatsConfig)) {
            return false;
        }
        StatsConfig that = (StatsConfig) o;
        return ((enableSelfPrint == that.enableSelfPrint)
                && (selfPrintPeriodMs == that.selfPrintPeriodMs)
                && (forcedResetPeriodMs == that.forcedResetPeriodMs)
                && (statsLevel == that.statsLevel));
    }

    @Override
    public int hashCode() {
        return Objects.hash(statsLevel, enableSelfPrint,
                selfPrintPeriodMs, forcedResetPeriodMs);
    }

    @Override
    public String toString() {
        return new StringBuilder(512)
                .append("\"StatsConfig\":{\"statsLevel\":\"").append(statsLevel.getName())
                .append("\",\"enableSelfPrint\":").append(enableSelfPrint)
                .append(",\"selfPrintPeriodMs\":").append(selfPrintPeriodMs)
                .append(",\"forcedResetPeriodMs\":").append(forcedResetPeriodMs)
                .append("}").toString();
    }
}
