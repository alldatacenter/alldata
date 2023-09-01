/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.uniffle.common.metrics.prometheus;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.common.annotations.VisibleForTesting;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.PushGateway;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.uniffle.common.config.RssConf;
import org.apache.uniffle.common.metrics.AbstractMetricReporter;
import org.apache.uniffle.common.util.ThreadUtils;

public class PrometheusPushGatewayMetricReporter extends AbstractMetricReporter {
  private static final Logger LOG = LoggerFactory.getLogger(PrometheusPushGatewayMetricReporter.class);
  static final String PUSHGATEWAY_ADDR = "rss.metrics.prometheus.pushgateway.addr";
  static final String GROUPING_KEY = "rss.metrics.prometheus.pushgateway.groupingkey";
  static final String JOB_NAME = "rss.metrics.prometheus.pushgateway.jobname";
  static final String REPORT_INTEVAL = "rss.metrics.prometheus.pushgateway.report.interval.seconds";
  private ScheduledExecutorService scheduledExecutorService;
  private PushGateway pushGateway;

  public PrometheusPushGatewayMetricReporter(RssConf conf, String instanceId)  {
    super(conf, instanceId);
  }

  @Override
  public void start() {
    if (pushGateway == null) {
      String address = conf.getString(PUSHGATEWAY_ADDR, null);
      if (StringUtils.isEmpty(address)) {
        throw new RuntimeException(PUSHGATEWAY_ADDR + " should not be empty!");
      }
      pushGateway = new PushGateway(address);
    }
    String jobName = conf.getString(JOB_NAME, null);
    if (StringUtils.isEmpty(jobName)) {
      throw new RuntimeException(JOB_NAME + " should not be empty!");
    }
    Map<String, String> groupingKey = parseGroupingKey(conf.getString(GROUPING_KEY, ""));
    groupingKey.put("instance", instanceId);
    int reportInterval = conf.getInteger(REPORT_INTEVAL, 10);
    scheduledExecutorService = Executors.newScheduledThreadPool(1,
        ThreadUtils.getThreadFactory("PrometheusPushGatewayMetricReporter-%d"));
    scheduledExecutorService.scheduleWithFixedDelay(() -> {
      for (CollectorRegistry registry : registryList) {
        try {
          pushGateway.pushAdd(registry, jobName, groupingKey);
        } catch (Throwable e) {
          LOG.error("Failed to send metrics to push gateway.", e);
        }
      }
    }, 0, reportInterval, TimeUnit.SECONDS);
  }

  @Override
  public void stop() {
    if (scheduledExecutorService != null) {
      scheduledExecutorService.shutdownNow();
    }
  }

  @VisibleForTesting
  void setPushGateway(PushGateway pushGateway) {
    this.pushGateway = pushGateway;
  }

  static Map<String, String> parseGroupingKey(final String groupingKeyConfig) {
    Map<String, String> groupingKey = new HashMap<>();
    if (!groupingKeyConfig.isEmpty()) {
      String[] kvs = groupingKeyConfig.split(";");
      for (String kv : kvs) {
        int idx = kv.indexOf("=");
        if (idx < 0) {
          LOG.warn("Invalid prometheusPushGateway groupingKey:{}, will be ignored", kv);
          continue;
        }

        String labelKey = kv.substring(0, idx);
        String labelValue = kv.substring(idx + 1);
        if (StringUtils.isEmpty(labelKey)
            || StringUtils.isEmpty(labelValue)) {
          LOG.warn(
              "Invalid groupingKey {labelKey:{}, labelValue:{}} must not be empty",
              labelKey,
              labelValue);
          continue;
        }
        groupingKey.put(labelKey, labelValue);
      }

      return groupingKey;
    }

    return groupingKey;
  }
}
