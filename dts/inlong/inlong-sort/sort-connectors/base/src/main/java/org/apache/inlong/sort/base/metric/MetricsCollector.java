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

import org.apache.flink.util.Collector;
import org.apache.inlong.sort.formats.base.collectors.TimestampedCollector;

/**
 * sending metrics each time a record is collected.
 * @param <T>
 */
public class MetricsCollector<T> implements TimestampedCollector<T> {

    private final Collector<T> collector;

    private long timestampMillis;

    SourceMetricData metricData;

    public MetricsCollector(Collector<T> collector,
            SourceMetricData sourceMetricData) {
        this.metricData = sourceMetricData;
        this.collector = collector;
    }

    public void resetTimestamp(long timestampMillis) {
        this.timestampMillis = timestampMillis;
    }

    @Override
    public void collect(T record) {
        if (metricData != null) {
            metricData.outputMetricsWithEstimate(record, timestampMillis);
        }
        collector.collect(record);
    }

    @Override
    public void close() {
    }

}
