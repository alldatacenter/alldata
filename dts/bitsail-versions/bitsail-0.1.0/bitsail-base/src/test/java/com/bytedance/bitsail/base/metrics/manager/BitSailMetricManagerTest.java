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

package com.bytedance.bitsail.base.metrics.manager;

import com.bytedance.bitsail.base.metrics.MetricManager;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.option.CommonOptions;
import com.bytedance.bitsail.common.util.Pair;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BitSailMetricManagerTest {
  private final long jobId = -1L;
  private MetricManager metricManager;

  @Before
  public void init() {
    BitSailConfiguration jobConf = BitSailConfiguration.newDefault();
    jobConf.set(CommonOptions.JOB_ID, jobId);
    jobConf.set(CommonOptions.METRICS_REPORTER_TYPE, "nop");
    List<Pair<String, String>> extraMetricTags = ImmutableList.of(
        Pair.newPair("task", String.valueOf(1)),
        Pair.newPair("input", "input0")
    );
    metricManager = new BitSailMetricManager(jobConf,
        "group",
        false,
        extraMetricTags);
  }

  @Test
  public void testMetricTags() {
    List<Pair<String, String>> allMetricDimensions = metricManager.getAllMetricDimensions();
    assertEquals(5, allMetricDimensions.size());
  }

  @Test
  public void testGetMetricIdentifier() {
    String metricIdentifier = metricManager.getMetricIdentifier("name1");
    assertTrue(metricIdentifier.startsWith("group.name1{job=-1,git_commit="));
    assertTrue(metricIdentifier.contains("version="));
  }
}
