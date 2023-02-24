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
package org.apache.atlas.model.metrics;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.HashMap;
import java.util.Map;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;

/**
 * Atlas metrics
 */
@JsonAutoDetect(getterVisibility=PUBLIC_ONLY, setterVisibility=PUBLIC_ONLY, fieldVisibility=NONE)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
public class AtlasMetrics {
    public static final String PREFIX_CONNECTION_STATUS = "ConnectionStatus:";
    public static final String PREFIX_NOTIFICATION      = "Notification:";
    public static final String PREFIX_SERVER            = "Server:";

    public static final String STAT_NOTIFY_COUNT_CURR_DAY              = PREFIX_NOTIFICATION + "currentDay";
    public static final String STAT_NOTIFY_AVG_TIME_CURR_DAY           = PREFIX_NOTIFICATION + "currentDayAvgTime";
    public static final String STAT_NOTIFY_CREATES_COUNT_CURR_DAY      = PREFIX_NOTIFICATION + "currentDayEntityCreates";
    public static final String STAT_NOTIFY_UPDATES_COUNT_CURR_DAY      = PREFIX_NOTIFICATION + "currentDayEntityUpdates";
    public static final String STAT_NOTIFY_DELETES_COUNT_CURR_DAY      = PREFIX_NOTIFICATION + "currentDayEntityDeletes";
    public static final String STAT_NOTIFY_FAILED_COUNT_CURR_DAY       = PREFIX_NOTIFICATION + "currentDayFailed";
    public static final String STAT_NOTIFY_START_TIME_CURR_DAY         = PREFIX_NOTIFICATION + "currentDayStartTime";
    public static final String STAT_NOTIFY_COUNT_CURR_HOUR             = PREFIX_NOTIFICATION + "currentHour";
    public static final String STAT_NOTIFY_AVG_TIME_CURR_HOUR          = PREFIX_NOTIFICATION + "currentHourAvgTime";
    public static final String STAT_NOTIFY_CREATES_COUNT_CURR_HOUR     = PREFIX_NOTIFICATION + "currentHourEntityCreates";
    public static final String STAT_NOTIFY_UPDATES_COUNT_CURR_HOUR     = PREFIX_NOTIFICATION + "currentHourEntityUpdates";
    public static final String STAT_NOTIFY_DELETES_COUNT_CURR_HOUR     = PREFIX_NOTIFICATION + "currentHourEntityDeletes";
    public static final String STAT_NOTIFY_FAILED_COUNT_CURR_HOUR      = PREFIX_NOTIFICATION + "currentHourFailed";
    public static final String STAT_NOTIFY_START_TIME_CURR_HOUR        = PREFIX_NOTIFICATION + "currentHourStartTime";
    public static final String STAT_NOTIFY_LAST_MESSAGE_PROCESSED_TIME = PREFIX_NOTIFICATION + "lastMessageProcessedTime";
    public static final String STAT_NOTIFY_TOPIC_DETAILS               = PREFIX_NOTIFICATION + "topicDetails";
    public static final String STAT_NOTIFY_COUNT_PREV_DAY              = PREFIX_NOTIFICATION + "previousDay";
    public static final String STAT_NOTIFY_AVG_TIME_PREV_DAY           = PREFIX_NOTIFICATION + "previousDayAvgTime";
    public static final String STAT_NOTIFY_CREATES_COUNT_PREV_DAY      = PREFIX_NOTIFICATION + "previousDayEntityCreates";
    public static final String STAT_NOTIFY_UPDATES_COUNT_PREV_DAY      = PREFIX_NOTIFICATION + "previousDayEntityUpdates";
    public static final String STAT_NOTIFY_DELETES_COUNT_PREV_DAY      = PREFIX_NOTIFICATION + "previousDayEntityDeletes";
    public static final String STAT_NOTIFY_FAILED_COUNT_PREV_DAY       = PREFIX_NOTIFICATION + "previousDayFailed";
    public static final String STAT_NOTIFY_COUNT_PREV_HOUR             = PREFIX_NOTIFICATION + "previousHour";
    public static final String STAT_NOTIFY_AVG_TIME_PREV_HOUR          = PREFIX_NOTIFICATION + "previousHourAvgTime";
    public static final String STAT_NOTIFY_CREATES_COUNT_PREV_HOUR     = PREFIX_NOTIFICATION + "previousHourEntityCreates";
    public static final String STAT_NOTIFY_UPDATES_COUNT_PREV_HOUR     = PREFIX_NOTIFICATION + "previousHourEntityUpdates";
    public static final String STAT_NOTIFY_DELETES_COUNT_PREV_HOUR     = PREFIX_NOTIFICATION + "previousHourEntityDeletes";
    public static final String STAT_NOTIFY_FAILED_COUNT_PREV_HOUR      = PREFIX_NOTIFICATION + "previousHourFailed";
    public static final String STAT_NOTIFY_COUNT_TOTAL                 = PREFIX_NOTIFICATION + "total";
    public static final String STAT_NOTIFY_AVG_TIME_TOTAL              = PREFIX_NOTIFICATION + "totalAvgTime";
    public static final String STAT_NOTIFY_CREATES_COUNT_TOTAL         = PREFIX_NOTIFICATION + "totalCreates";
    public static final String STAT_NOTIFY_UPDATES_COUNT_TOTAL         = PREFIX_NOTIFICATION + "totalUpdates";
    public static final String STAT_NOTIFY_DELETES_COUNT_TOTAL         = PREFIX_NOTIFICATION + "totalDeletes";
    public static final String STAT_NOTIFY_FAILED_COUNT_TOTAL          = PREFIX_NOTIFICATION + "totalFailed";
    public static final String STAT_SERVER_ACTIVE_TIMESTAMP            = PREFIX_SERVER + "activeTimeStamp";
    public static final String STAT_SERVER_START_TIMESTAMP             = PREFIX_SERVER + "startTimeStamp";
    public static final String STAT_SERVER_STATUS_BACKEND_STORE        = PREFIX_SERVER + "statusBackendStore";
    public static final String STAT_SERVER_STATUS_INDEX_STORE          = PREFIX_SERVER + "statusIndexStore";
    public static final String STAT_SERVER_UP_TIME                     = PREFIX_SERVER + "upTime";

    private Map<String, Map<String, Object>> data;

    public AtlasMetrics() {
        setData(null);
    }

    public AtlasMetrics(Map<String, Map<String, Object>> data) {
        setData(data);
    }

    public AtlasMetrics(AtlasMetrics other) {
        if (other != null) {
            setData(other.getData());
        }
    }

    public Map<String, Map<String, Object>> getData() {
        return data;
    }

    public void setData(Map<String, Map<String, Object>> data) {
        this.data = data;
    }

    @JsonIgnore
    public void addMetric(String groupKey, String key, Object value) {
        Map<String, Map<String, Object>> data = this.data;

        if (data == null) {
            data = new HashMap<>();

            this.data = data;
        }

        Map<String, Object> metricMap = data.computeIfAbsent(groupKey, k -> new HashMap<>());

        metricMap.put(key, value);
    }

    @JsonIgnore
    public Number getNumericMetric(String groupKey, String key) {
        Object metric = getMetric(groupKey, key);

        return metric instanceof Number ? (Number) metric : null;
    }

    @JsonIgnore
    public Object getMetric(String groupKey, String key) {
        Object                           ret  = null;
        Map<String, Map<String, Object>> data = this.data;

        if (data != null) {
            Map<String, Object> metricMap = data.get(groupKey);

            if (metricMap != null && !metricMap.isEmpty()) {
                ret = metricMap.get(key);
            }
        }

        return ret;
    }
}
