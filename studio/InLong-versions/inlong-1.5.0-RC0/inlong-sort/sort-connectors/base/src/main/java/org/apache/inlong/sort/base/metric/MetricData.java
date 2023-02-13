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

import org.apache.flink.metrics.Counter;
import org.apache.flink.metrics.Meter;
import org.apache.flink.metrics.MeterView;
import org.apache.flink.metrics.MetricGroup;
import org.apache.flink.metrics.SimpleCounter;

import java.util.Map;

import static org.apache.inlong.sort.base.Constants.GROUP_ID;
import static org.apache.inlong.sort.base.Constants.NODE_ID;
import static org.apache.inlong.sort.base.Constants.STREAM_ID;
import static org.apache.inlong.sort.base.Constants.TIME_SPAN_IN_SECONDS;

/**
 * This class is the top-level interface of metric data, it defines common metric data methods.
 */
public interface MetricData {

    /**
     * Get metric group
     *
     * @return The MetricGroup
     */
    MetricGroup getMetricGroup();

    /**
     * Get labels
     *
     * @return The labels defined in inlong
     */
    Map<String, String> getLabels();

    /**
     * Get group id
     *
     * @return The group id defined in inlong
     */
    default String getGroupId() {
        return getLabels().get(GROUP_ID);
    }

    /**
     * Get stream id
     *
     * @return The stream id defined in inlong
     */
    default String getStreamId() {
        return getLabels().get(STREAM_ID);
    }

    /**
     * Get node id
     *
     * @return The node id defined in inlong
     */
    default String getNodeId() {
        return getLabels().get(NODE_ID);
    }

    /**
     * Register a counter metric
     *
     * @param metricName The metric name
     * @param counter The counter of metric
     * @return Counter of registered
     */
    default Counter registerCounter(String metricName, Counter counter) {
        MetricGroup inlongMetricGroup = getMetricGroup();
        for (Map.Entry<String, String> label : getLabels().entrySet()) {
            inlongMetricGroup = inlongMetricGroup.addGroup(label.getKey(), label.getValue());
        }
        return inlongMetricGroup.counter(metricName, counter);
    }

    /**
     * Register a counter metric
     *
     * @param metricName The metric name
     * @return Counter of registered
     */
    default Counter registerCounter(String metricName) {
        return registerCounter(metricName, new SimpleCounter());
    }

    /**
     * Register a meter metric
     *
     * @param metricName The metric name
     * @return Meter of registered
     */
    default Meter registerMeter(String metricName, Counter counter) {
        MetricGroup inlongMetricGroup = getMetricGroup();
        for (Map.Entry<String, String> label : getLabels().entrySet()) {
            inlongMetricGroup = inlongMetricGroup.addGroup(label.getKey(), label.getValue());
        }
        return inlongMetricGroup.meter(metricName, new MeterView(counter, TIME_SPAN_IN_SECONDS));
    }

}
