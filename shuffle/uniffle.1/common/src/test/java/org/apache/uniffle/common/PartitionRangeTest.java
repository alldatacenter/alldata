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

package org.apache.uniffle.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class PartitionRangeTest {

  @Test
  public void testPartitionRange() {
    PartitionRange partitionRange = new PartitionRange(3, 4);
    assertEquals(3, partitionRange.getStart());
    assertEquals(4, partitionRange.getEnd());
    assertEquals(2, partitionRange.getPartitionNum());
    assertThrows(IllegalArgumentException.class, () -> new PartitionRange(-1, 1));
    assertThrows(IllegalArgumentException.class, () -> new PartitionRange(2, 1));
    assertDoesNotThrow(() -> new PartitionRange(0, 0));
  }

  @Test
  public void testEquals() {
    PartitionRange partitionRange1 = new PartitionRange(1, 2);
    PartitionRange partitionRange2 = new PartitionRange(1, 2);
    assertEquals(partitionRange1, partitionRange1);
    assertEquals(partitionRange1.hashCode(), partitionRange1.hashCode());
    assertEquals(partitionRange1, partitionRange2);
    assertEquals(partitionRange1.hashCode(), partitionRange2.hashCode());
    PartitionRange partitionRange3 = new PartitionRange(1, 1);
    assertNotEquals(partitionRange1, partitionRange3);
    PartitionRange partitionRange4 = new PartitionRange(2, 2);
    assertNotEquals(partitionRange1, partitionRange4);
    assertNotEquals(partitionRange1, null);
    assertNotEquals(partitionRange1, new Object());
  }

  @Test
  public void testToString() {
    PartitionRange partitionRange = new PartitionRange(1, 2);
    assertEquals("PartitionRange[1, 2]", partitionRange.toString());
  }

  @Test
  public void testHashCode() {
    PartitionRange partitionRange1 = new PartitionRange(1, 2);
    PartitionRange partitionRange2 = new PartitionRange(1, 2);
    assertEquals(partitionRange1.hashCode(), partitionRange2.hashCode());
  }

  @Test
  public void testCompareTo() {
    PartitionRange partitionRange1 = new PartitionRange(1, 2);
    PartitionRange partitionRange2 = new PartitionRange(1, 3);
    PartitionRange partitionRange3 = new PartitionRange(2, 3);
    assertEquals(0, partitionRange1.compareTo(partitionRange2));
    assertEquals(-1, partitionRange1.compareTo(partitionRange3));
    assertEquals(1, partitionRange3.compareTo(partitionRange2));
  }

}
