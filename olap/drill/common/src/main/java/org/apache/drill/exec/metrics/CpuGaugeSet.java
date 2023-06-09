/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.metrics;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.HashMap;
import java.util.Map;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;
import com.sun.management.OperatingSystemMXBean;

/**
 * Creates a Cpu GaugeSet
 */
@SuppressWarnings("restriction")
public class CpuGaugeSet implements MetricSet {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CpuGaugeSet.class);

  private final OperatingSystemMXBean osMXBean;
  private final RuntimeMXBean rtMXBean;

  public CpuGaugeSet() {
    this.rtMXBean = ManagementFactory.getRuntimeMXBean();
    //DRILL-6702: Instead of worrying about compiling with IBM JDK, for now, we shall provide no CPU metrics for non-HotSpot JVMs
    this.osMXBean = getOSMXBeanForCpuMetrics();
  }

  @Override
  public Map<String, Metric> getMetrics() {
    final Map<String, Metric> metric = new HashMap<>(1);
    metric.put("os.load.avg", new OperatingSystemLoad(osMXBean));
    metric.put("drillbit.load.avg", new DrillbitProcessLoad(osMXBean));
    metric.put("drillbit.uptime", new DrillbitUptime(rtMXBean));
    return metric;
  }

  private static OperatingSystemMXBean getOSMXBeanForCpuMetrics() {
    try {
      return (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    } catch (ClassCastException ex) {
      logger.warn("{}. Detected non-Supported JVM [{}]. CPU Metrics in the WebUI will not be available!", ex.getMessage(), System.getProperty("java.vm.name"));
    }
    return null;
  }
}

/**
 * Creating an AverageSystemLoad Gauge
 */
@SuppressWarnings("restriction")
final class OperatingSystemLoad implements Gauge<Double> {
  private OperatingSystemMXBean osMXBean;
  public OperatingSystemLoad(OperatingSystemMXBean osBean) {
    this.osMXBean = osBean;
  }

  @Override
  public Double getValue() {
    return (osMXBean != null) ? osMXBean.getSystemLoadAverage() : null;
  }

}

/**
 * Creating an AverageDrillbitLoad Gauge
 */
@SuppressWarnings("restriction")
final class DrillbitProcessLoad implements Gauge<Double> {
  private OperatingSystemMXBean osMXBean;
  public DrillbitProcessLoad(OperatingSystemMXBean osBean) {
    this.osMXBean = osBean;
  }

  @Override
  public Double getValue() {
    return (osMXBean != null) ? osMXBean.getProcessCpuLoad() : null;
  }

}
/**
 * Creating an DrillbitUptime Gauge
 */
final class DrillbitUptime implements Gauge<Long> {
  private RuntimeMXBean rtMXBean;
  public DrillbitUptime(RuntimeMXBean osBean) {
    this.rtMXBean = osBean;
  }

  @Override
  public Long getValue() {
    return rtMXBean.getUptime();
  }

}
