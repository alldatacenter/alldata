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
package org.apache.drill;

import org.apache.drill.categories.OperatorTest;
import org.apache.drill.exec.ops.OperatorMetricRegistry;
import org.apache.drill.exec.physical.config.ExternalSort;
import org.apache.drill.exec.physical.config.NestedLoopJoinPOP;
import org.apache.drill.exec.physical.config.Screen;
import org.apache.drill.test.BaseTestQuery;
import org.junit.Test;
import org.junit.experimental.categories.Category;


import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNull;

@Category(OperatorTest.class)
public class TestOperatorMetrics extends BaseTestQuery {

  @Test
  public void testMetricNames() {
    assertArrayEquals(new String[]{"BYTES_SENT"},
              OperatorMetricRegistry.getMetricNames(Screen.OPERATOR_TYPE));

    assertArrayEquals(new String[]{"SPILL_COUNT", "NOT_USED", "PEAK_BATCHES_IN_MEMORY", "MERGE_COUNT", "MIN_BUFFER",
                      "SPILL_MB"},
              OperatorMetricRegistry.getMetricNames(ExternalSort.OPERATOR_TYPE));
  }

  @Test
  public void testNonExistentMetricNames() {
    assertNull(OperatorMetricRegistry.getMetricNames(NestedLoopJoinPOP.OPERATOR_TYPE));

    assertNull(OperatorMetricRegistry.getMetricNames("FOO_BAR"));
  }
}
