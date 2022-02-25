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
package org.apache.atlas.web.service;

import org.apache.atlas.web.model.DebugMetrics;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.metrics2.AbstractMetric;
import org.apache.hadoop.metrics2.MetricsRecord;
import org.apache.hadoop.metrics2.MetricsSink;
import org.springframework.stereotype.Component;

import java.util.HashMap;

import static org.apache.atlas.web.service.DebugMetricsWrapper.Constants.AVG_TIME;
import static org.apache.atlas.web.service.DebugMetricsWrapper.Constants.DEBUG_METRICS_SOURCE;
import static org.apache.atlas.web.service.DebugMetricsWrapper.Constants.MAX_TIME;
import static org.apache.atlas.web.service.DebugMetricsWrapper.Constants.MIN_TIME;
import static org.apache.atlas.web.service.DebugMetricsWrapper.Constants.NUM_OPS;
import static org.apache.atlas.web.service.DebugMetricsWrapper.Constants.STD_DEV_TIME;

@Component
public class AtlasDebugMetricsSink implements MetricsSink {
    private final HashMap<String, DebugMetrics> metricStructuredSnapshot = new HashMap<>();

    @Override
    public void putMetrics(MetricsRecord metricsRecord) {
        if (!DEBUG_METRICS_SOURCE.equals(metricsRecord.name())) {
            return;
        }

        for (AbstractMetric metric : metricsRecord.metrics()) {
            float value = metric.value().floatValue();

            if (value == 0 || value == Float.MAX_VALUE || value == Float.MIN_VALUE) {
                continue;
            }

            setMetricsData(metric);
        }
    }

    public HashMap getMetrics() {
        return metricStructuredSnapshot;
    }

    @Override
    public void init(org.apache.commons.configuration2.SubsetConfiguration subsetConfiguration) {
    }

    @Override
    public void flush() {
    }

    private void setMetricsData(AbstractMetric metric) {
        String name      = metric.name().toLowerCase();
        String fieldCaps = AtlasDebugMetricsSource.fieldLowerCaseUpperCaseMap.get(name);

        if (!StringUtils.isEmpty(fieldCaps)) {
            String       fieldNameLower = fieldCaps.toLowerCase();
            String       metricType = inferMeasureType(name, fieldNameLower);
            DebugMetrics debugMetrics = metricStructuredSnapshot.get(fieldCaps);

            if (debugMetrics == null) {
                debugMetrics = new DebugMetrics(fieldCaps);

                metricStructuredSnapshot.put(fieldCaps, debugMetrics);
            }

            updateMetricType(debugMetrics, metricType, metric);
        }
    }

    private void updateMetricType(DebugMetrics debugMetrics, String metricType, AbstractMetric metric) {
        switch (metricType) {
            case NUM_OPS:
                debugMetrics.setNumops(metric.value().intValue());
                break;

            case MIN_TIME:
                debugMetrics.setMinTime(metric.value().floatValue());
                break;

            case MAX_TIME:
                debugMetrics.setMaxTime(metric.value().floatValue());
                break;

            case STD_DEV_TIME:
                debugMetrics.setStdDevTime(metric.value().floatValue());
                break;

            case AVG_TIME:
                debugMetrics.setAvgTime(metric.value().floatValue());
                break;
        }
    }

    private static String inferMeasureType(String fullName, String nameWithoutMetricType) {
        return fullName.replaceFirst(nameWithoutMetricType, "");
    }
}