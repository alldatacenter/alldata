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

package org.apache.inlong.sort.hudi.metric;

import org.apache.flink.util.Preconditions;
import org.apache.hudi.com.codahale.metrics.Counter;
import org.apache.hudi.com.codahale.metrics.Gauge;
import org.apache.hudi.com.codahale.metrics.Histogram;
import org.apache.hudi.com.codahale.metrics.Meter;
import org.apache.hudi.com.codahale.metrics.MetricFilter;
import org.apache.hudi.com.codahale.metrics.MetricRegistry;
import org.apache.hudi.com.codahale.metrics.ScheduledReporter;
import org.apache.hudi.com.codahale.metrics.Timer;
import org.apache.inlong.audit.AuditOperator;
import org.apache.inlong.sort.base.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.inlong.sort.base.Constants.DELIMITER;
import static org.apache.inlong.sort.base.Constants.GROUP_ID;
import static org.apache.inlong.sort.base.Constants.STREAM_ID;
import static org.apache.inlong.sort.hudi.metric.HudiMetricsConst.ACTION_TYPES;
import static org.apache.inlong.sort.hudi.metric.HudiMetricsConst.METRIC_TOTAL_BYTES_WRITTEN;
import static org.apache.inlong.sort.hudi.metric.HudiMetricsConst.METRIC_TOTAL_RECORDS_WRITTEN;
import static org.apache.inlong.sort.hudi.metric.HudiMetricsUtil.getMetricsName;

/**
 * The schedule reporter for submit rowCount and rowSize of writing hudi
 */
public class HudiAuditReporter extends ScheduledReporter {

    private static final String IP_OR_HOST_PORT = "^(.*):([0-9]|[1-9]\\d|[1-9]\\d{"
            + "2}|[1-9]\\d{"
            + "3}|[1-5]\\d{"
            + "4}|6[0-4]\\d{"
            + "3}|65[0-4]\\d{"
            + "2}|655[0-2]\\d|6553[0-5])$";
    private static final Logger LOG = LoggerFactory.getLogger(HudiAuditReporter.class);
    private final LinkedHashMap<String, String> labels;
    private Set<String> byteMetricNames;
    private Set<String> recordMetricNames;
    private AuditOperator auditOperator;

    protected HudiAuditReporter(
            String inLongLabels,
            String inLongAudit,
            String metricNamePrefix,
            MetricRegistry registry,
            String name,
            MetricFilter filter,
            TimeUnit rateUnit,
            TimeUnit durationUnit) {
        super(registry, name, filter, rateUnit, durationUnit);
        LOG.info("Create HudiAuditReporter, inLongLabels: {}, inLongAudit: {}, reportName: {}.",
                inLongLabels, inLongAudit, name);

        labels = new LinkedHashMap<>();
        String[] inLongLabelArray = inLongLabels.split(DELIMITER);
        Preconditions.checkArgument(Stream.of(inLongLabelArray).allMatch(label -> label.contains("=")),
                "InLong metric label format must be xxx=xxx");
        Stream.of(inLongLabelArray).forEach(label -> {
            String key = label.substring(0, label.indexOf('='));
            String value = label.substring(label.indexOf('=') + 1);
            labels.put(key, value);
        });
        String ipPorts = inLongAudit;
        HashSet<String> ipPortList = new HashSet<>();

        if (ipPorts != null) {
            Preconditions.checkArgument(labels.containsKey(GROUP_ID) && labels.containsKey(STREAM_ID),
                    "groupId and streamId must be set when enable inlong audit collect.");
            String[] ipPortStrs = inLongAudit.split(DELIMITER);
            for (String ipPort : ipPortStrs) {
                Preconditions.checkArgument(Pattern.matches(IP_OR_HOST_PORT, ipPort),
                        "Error inLong audit format: " + inLongAudit);
                ipPortList.add(ipPort);
            }
        }

        if (!ipPortList.isEmpty()) {
            AuditOperator.getInstance().setAuditProxy(ipPortList);
            auditOperator = AuditOperator.getInstance();
        }

        recordMetricNames = Arrays.stream(ACTION_TYPES)
                .map(action -> getMetricsName(metricNamePrefix, action, METRIC_TOTAL_RECORDS_WRITTEN))
                .collect(Collectors.toSet());
        byteMetricNames = Arrays.stream(ACTION_TYPES)
                .map(action -> getMetricsName(metricNamePrefix, action, METRIC_TOTAL_BYTES_WRITTEN))
                .collect(Collectors.toSet());
    }

    @Override
    public void report(
            SortedMap<String, Gauge> gaugeMap,
            SortedMap<String, Counter> countMap,
            SortedMap<String, Histogram> histogramMap,
            SortedMap<String, Meter> meterMap,
            SortedMap<String, Timer> timerMap) {

        if (auditOperator != null && !gaugeMap.isEmpty()) {
            long rowCount = getGaugeValue(gaugeMap, recordMetricNames);
            long rowSize = getGaugeValue(gaugeMap, byteMetricNames);
            auditOperator.add(
                    Constants.AUDIT_SORT_OUTPUT,
                    getGroupId(),
                    getStreamId(),
                    System.currentTimeMillis(),
                    rowCount,
                    rowSize);
        }
    }

    private Long getGaugeValue(
            SortedMap<String, Gauge> gaugeMap,
            Set<String> metricNames) {
        return metricNames
                .stream()
                .mapToLong(metricName -> getGaugeValue(gaugeMap, metricName))
                .sum();
    }

    private Long getGaugeValue(
            SortedMap<String, Gauge> gaugeMap,
            String metricName) {
        return Optional.ofNullable(gaugeMap.get(metricName))
                .map(Gauge::getValue)
                .map(v -> (Long) v)
                .orElse(0L);
    }

    public LinkedHashMap<String, String> getLabels() {
        return labels;
    }

    private String getStreamId() {
        return getLabels().get(STREAM_ID);
    }

    private String getGroupId() {
        return getLabels().get(GROUP_ID);
    }

    public boolean isReady() {
        return auditOperator != null;
    }
}
