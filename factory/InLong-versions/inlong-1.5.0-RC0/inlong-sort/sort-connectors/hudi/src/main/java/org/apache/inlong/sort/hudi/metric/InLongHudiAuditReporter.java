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

import static org.apache.hudi.config.metrics.HoodieMetricsConfig.METRICS_REPORTER_PREFIX;
import static org.apache.inlong.sort.base.Constants.INLONG_AUDIT;
import static org.apache.inlong.sort.base.Constants.INLONG_METRIC;
import static org.apache.inlong.sort.hudi.metric.HudiMetricsConfig.REPORT_PERIOD_IN_SECONDS;
import static org.apache.inlong.sort.hudi.metric.HudiMetricsConfig.getConfig;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.flink.util.StringUtils;
import org.apache.hudi.com.codahale.metrics.MetricFilter;
import org.apache.hudi.com.codahale.metrics.MetricRegistry;
import org.apache.hudi.metrics.custom.CustomizableMetricsReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The main entry of hudi audit reporter.
 */
public class InLongHudiAuditReporter extends CustomizableMetricsReporter {

    private static final Logger LOG = LoggerFactory.getLogger(InLongHudiAuditReporter.class);

    private HudiAuditReporter hudiAuditReporter;
    private Integer reportPeriodSeconds;

    public InLongHudiAuditReporter(Properties props, MetricRegistry registry) {
        super(props, registry);

        String inLongLabels = getConfig(props, INLONG_METRIC);
        String inLongAudit = getConfig(props, INLONG_AUDIT);
        String metricNamePrefix = getConfig(props, METRICS_REPORTER_PREFIX);
        this.reportPeriodSeconds = getConfig(props, REPORT_PERIOD_IN_SECONDS);

        if (StringUtils.isNullOrWhitespaceOnly(inLongLabels)) {
            LOG.error("Fatal error on create InLongHudiReporter, inLongLabels is empty!");
            return;
        }
        if (StringUtils.isNullOrWhitespaceOnly(inLongAudit)) {
            LOG.error("Fatal error on create InLongHudiReporter, inLongAudit is empty!");
            return;
        }

        this.hudiAuditReporter = new HudiAuditReporter(
                inLongLabels,
                inLongAudit,
                metricNamePrefix,
                registry,
                "inlong-hudi-audit-reporter",
                MetricFilter.ALL,
                TimeUnit.SECONDS,
                TimeUnit.SECONDS);
    }

    @Override
    public void start() {
        if (hudiAuditReporter != null && hudiAuditReporter.isReady()) {
            this.hudiAuditReporter.start(reportPeriodSeconds, TimeUnit.SECONDS);
        }
    }

    @Override
    public void report() {

    }

    @Override
    public void stop() {

    }
}
