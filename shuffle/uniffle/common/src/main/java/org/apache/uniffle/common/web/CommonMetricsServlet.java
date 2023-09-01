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

package org.apache.uniffle.common.web;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.prometheus.client.Collector;
import io.prometheus.client.Collector.MetricFamilySamples.Sample;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.MetricsServlet;

public class CommonMetricsServlet extends MetricsServlet {

  final boolean isPrometheus;
  private CollectorRegistry registry;

  public CommonMetricsServlet(CollectorRegistry registry) {
    this(registry, false);
  }

  public CommonMetricsServlet(CollectorRegistry registry, boolean isPrometheus) {
    super(registry);
    this.registry = registry;
    this.isPrometheus = isPrometheus;
  }

  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    if (isPrometheus) {
      super.doGet(req, resp);
    } else {
      resp.setStatus(200);
      resp.setContentType("text/plain; version=0.0.4; charset=utf-8");

      try (BufferedWriter writer = new BufferedWriter(resp.getWriter())) {
        toJson(writer, getSamples(req));
        writer.flush();
      }
    }
  }

  private Set<String> parse(HttpServletRequest req) {
    String[] includedParam = req.getParameterValues("name[]");
    return includedParam == null ? Collections.emptySet() : new HashSet<>(Arrays.asList(includedParam));
  }

  private Enumeration<Collector.MetricFamilySamples> getSamples(HttpServletRequest req) {
    return this.registry.filteredMetricFamilySamples(this.parse(req));
  }

  public void toJson(Writer writer, Enumeration<Collector.MetricFamilySamples> mfs) throws IOException {

    List<Collector.MetricFamilySamples.Sample> metrics = new LinkedList<>();
    while (mfs.hasMoreElements()) {
      Collector.MetricFamilySamples metricFamilySamples = mfs.nextElement();
      metrics.addAll(metricFamilySamples.samples);
    }

    MetricsJsonObj res = new MetricsJsonObj(metrics, System.currentTimeMillis());
    ObjectMapper objectMapper = new ObjectMapper();
    String json = objectMapper.writeValueAsString(res);
    writer.write(json);
  }

  private static class MetricsJsonObj {

    private final List<Collector.MetricFamilySamples.Sample> metrics;
    private final long timeStamp;

    MetricsJsonObj(List<Collector.MetricFamilySamples.Sample> metrics, long timeStamp) {
      this.metrics = metrics;
      this.timeStamp = timeStamp;
    }

    public List<Sample> getMetrics() {
      return metrics;
    }

    public long getTimeStamp() {
      return timeStamp;
    }

  }
}
