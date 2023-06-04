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

package org.apache.inlong.sort.base.metric;

import java.io.Serializable;
import java.util.Map;

/**
 * metric state for supporting {@link org.apache.flink.metrics.Counter} metric snapshot and restore
 */
public class MetricState implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer subtaskIndex;

    private Map<String, Long> metrics;

    private Map<String, MetricState> subMetricStateMap;

    public MetricState() {
    }

    public MetricState(Integer subtaskIndex, Map<String, Long> metrics) {
        this.subtaskIndex = subtaskIndex;
        this.metrics = metrics;
    }

    public Integer getSubtaskIndex() {
        return subtaskIndex;
    }

    public void setSubtaskIndex(Integer subtaskIndex) {
        this.subtaskIndex = subtaskIndex;
    }

    public Map<String, Long> getMetrics() {
        return metrics;
    }

    public void setMetrics(Map<String, Long> metrics) {
        this.metrics = metrics;
    }

    public Map<String, MetricState> getSubMetricStateMap() {
        return subMetricStateMap;
    }

    public void setSubMetricStateMap(
            Map<String, MetricState> subMetricStateMap) {
        this.subMetricStateMap = subMetricStateMap;
    }

    public Long getMetricValue(String metricName) {
        if (metrics != null) {
            return metrics.getOrDefault(metricName, 0L);
        }
        return 0L;
    }

    @Override
    public String toString() {
        return "MetricState{"
                + "subtaskIndex=" + subtaskIndex
                + ", metrics=" + metrics.toString()
                + '}';
    }
}
