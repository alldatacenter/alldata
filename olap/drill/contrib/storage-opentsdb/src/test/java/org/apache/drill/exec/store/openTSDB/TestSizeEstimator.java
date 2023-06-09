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
package org.apache.drill.exec.store.openTSDB;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestSizeEstimator {

  private static class TestMetricDTO {
    private final String metric;
    private final Map<String, String> tags;
    private final List<String> aggregateTags;
    private final Map<String, String> dps;

    TestMetricDTO(String metric, Map<String, String> tags, List<String> aggregateTags, Map<String, String> dps) {
      this.metric = metric;
      this.tags = tags;
      this.aggregateTags = aggregateTags;
      this.dps = dps;
    }
  }

  @Test
  public void testArrays() {
    assertEquals(32, SizeEstimator.estimate(new byte[10]));
    assertEquals(40, SizeEstimator.estimate(new char[10]));
    assertEquals(40, SizeEstimator.estimate(new short[10]));
    assertEquals(56, SizeEstimator.estimate(new int[10]));
    assertEquals(96, SizeEstimator.estimate(new long[10]));
    assertEquals(56, SizeEstimator.estimate(new float[10]));
    assertEquals(96, SizeEstimator.estimate(new double[10]));
    assertEquals(4016, SizeEstimator.estimate(new int[1000]));
    assertEquals(8016, SizeEstimator.estimate(new long[1000]));
    assertEquals(56, SizeEstimator.estimate(new String[10]));
    assertEquals(56, SizeEstimator.estimate(new Object[10]));
  }

  @Test
  public void testList() {
    ArrayList<Long> list = new ArrayList<>();
    for(int i = 0; i < 10; i++) {
      list.add(Long.valueOf(i));
    }
    assertEquals(320, SizeEstimator.estimate(list));
    for(int i = 0; i < 10; i++) {
      list.add(Long.valueOf(i));
    }
    assertEquals(368, SizeEstimator.estimate(list));
  }

  @Test
  public void testMetricDTO() {
    Map<String, String> tags = new HashMap<>();
    Map<String, String> dps = new HashMap<>();
    tags.put("t1", "v1");
    dps.put("dp1", "dpv1");
    List<String> aggregateTags = new ArrayList<>();
    TestMetricDTO dto = new TestMetricDTO("metric1", tags, aggregateTags, dps);
    long size = SizeEstimator.estimate(dto);
    assertTrue("size less then expected: " + size, size > 550);
    assertTrue("size greater then expected: " + size, size < 800);
  }

}
