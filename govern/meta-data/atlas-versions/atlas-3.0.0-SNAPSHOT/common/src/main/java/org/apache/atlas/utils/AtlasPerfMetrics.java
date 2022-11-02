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

package org.apache.atlas.utils;


import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class AtlasPerfMetrics {
    private final Map<String, Metric> metrics = new LinkedHashMap<>();


    public MetricRecorder getMetricRecorder(String name) {
        return new MetricRecorder(name);
    }

    public void recordMetric(MetricRecorder recorder) {
        if (recorder != null) {
            final String name      = recorder.name;
            final long   timeTaken = recorder.getElapsedTime();

            Metric metric = metrics.get(name);

            if (metric == null) {
                metric = new Metric(name);

                metrics.put(name, metric);
            }

            metric.invocations++;
            metric.totalTimeMSecs += timeTaken;
        }
    }

    public void clear() {
        metrics.clear();
    }

    public boolean isEmpty() {
        return metrics.isEmpty();
    }

    public Set<String> getMetricsNames() {
        return metrics.keySet();
    }

    public Metric getMetric(String name) {
        return metrics.get(name);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("{");

        if (!metrics.isEmpty()) {
            for (Metric metric : metrics.values()) {
                sb.append("\"").append(metric.getName()).append("\":{\"count\":").append(metric.getInvocations()).append(",\"timeTaken\":").append(metric.getTotalTimeMSecs()).append("},");
            }

            sb.setLength(sb.length() - 1); // remove last ","
        }

        sb.append("}");

        return sb.toString();
    }

    public class MetricRecorder {
        private final String name;
        private final long   startTimeMs = System.currentTimeMillis();

        MetricRecorder(String name) {
            this.name = name;
        }

        long getElapsedTime() {
            return System.currentTimeMillis() - startTimeMs;
        }
    }

    public static class Metric {
        private final String name;
        private       short  invocations    = 0;
        private       long   totalTimeMSecs = 0;

        public Metric(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public short getInvocations() {
            return invocations;
        }

        public long getTotalTimeMSecs() {
            return totalTimeMSecs;
        }
    }
}
