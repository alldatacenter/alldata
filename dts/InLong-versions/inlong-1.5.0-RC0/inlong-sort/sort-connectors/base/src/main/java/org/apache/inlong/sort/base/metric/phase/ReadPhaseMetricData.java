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

package org.apache.inlong.sort.base.metric.phase;

import static org.apache.inlong.sort.base.Constants.READ_PHASE_TIMESTAMP;

import java.util.Map;
import org.apache.flink.metrics.Counter;
import org.apache.flink.metrics.MetricGroup;
import org.apache.inlong.sort.base.metric.MetricData;
import org.apache.inlong.sort.base.metric.MetricOption;
import org.apache.inlong.sort.base.metric.ThreadSafeCounter;

/**
 * A collection class for handling read phase metric data
 */
public class ReadPhaseMetricData implements MetricData {

    private final MetricGroup metricGroup;
    private Counter readPhaseTimestamp;
    private final Map<String, String> labels;

    public ReadPhaseMetricData(MetricOption option, MetricGroup metricGroup) {
        this.metricGroup = metricGroup;
        this.labels = option.getLabels();
        ThreadSafeCounter readPhaseCounter = new ThreadSafeCounter();
        readPhaseCounter.inc(option.getReadPhase());
        registerMetricsForReadPhase(readPhaseCounter);
    }

    /**
     * Register read phase time metrics and user can use custom counter that extends from {@link Counter}, and note that
     * we just keep the time to change that moment
     * groupId and streamId and nodeId and readPhase are label value, user can use it filter metric data when use metric reporter
     * prometheus
     */
    private void registerMetricsForReadPhase(Counter counter) {
        readPhaseTimestamp = registerCounter(READ_PHASE_TIMESTAMP, counter);
    }

    /**
     * Get metric group
     *
     * @return The MetricGroup
     */
    @Override
    public MetricGroup getMetricGroup() {
        return metricGroup;
    }

    /**
     * Get labels
     *
     * @return The labels defined in inlong
     */
    @Override
    public Map<String, String> getLabels() {
        return labels;
    }

    /**
     * get read phase counter
     *
     * @return the read phase counter
     */
    public Counter getReadPhase() {
        return readPhaseTimestamp;
    }

    /**
     * output read phase metric,just keep the time to change that moment
     */
    public void outputMetrics() {
        if (readPhaseTimestamp != null) {
            long count = readPhaseTimestamp.getCount();
            if (count == 0) {
                readPhaseTimestamp.inc(System.currentTimeMillis());
            }
        }
    }

    @Override
    public String toString() {
        return "ReadPhaseMetricData{"
                + "metricGroup=" + metricGroup
                + ", labels=" + labels
                + ", readPhaseTimestamp=" + readPhaseTimestamp.getCount()
                + '}';
    }
}