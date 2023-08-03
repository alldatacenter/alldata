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

package org.apache.uniffle.coordinator.strategy.assignment;

import org.junit.jupiter.api.Test;

import org.apache.uniffle.common.PartitionRange;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

public class PartitionRangeTest {

  @Test
  public void test() {
    PartitionRange range1 = new PartitionRange(0, 5);
    PartitionRange range2 = new PartitionRange(0, 5);
    assertNotSame(range1, range2);
    assertEquals(range1, range2);
    assertEquals(0, range1.getStart());
    assertEquals(5, range1.getEnd());
  }

}
