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

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class ShuffleRegisterInfoTest {

  @Test
  public void testEquals() {
    ShuffleServerInfo shuffleServerInfo = new ShuffleServerInfo("1", "localhost", 1234);
    List<PartitionRange> partitionRanges = Collections.singletonList(new PartitionRange(2, 3));
    ShuffleRegisterInfo info1 = new ShuffleRegisterInfo(shuffleServerInfo, partitionRanges);
    ShuffleRegisterInfo info2 = new ShuffleRegisterInfo(info1.getShuffleServerInfo(), info1.getPartitionRanges());
    assertEquals(info1, info1);
    assertEquals(info1.hashCode(), info1.hashCode());
    assertEquals(info1, info2);
    assertEquals(info1.hashCode(), info2.hashCode());
    assertNotEquals(info1, null);
    assertNotEquals(info1, new Object());
    ShuffleRegisterInfo info3 = new ShuffleRegisterInfo(info1.getShuffleServerInfo(), null);
    assertNotEquals(info1, info3);
    ShuffleRegisterInfo info4 = new ShuffleRegisterInfo(null, info1.getPartitionRanges());
    assertNotEquals(info1, info4);
  }

  @Test
  public void testToString() {
    ShuffleServerInfo shuffleServerInfo = new ShuffleServerInfo("1", "localhost", 1234);
    List<PartitionRange> partitionRanges = Collections.singletonList(new PartitionRange(2, 3));
    ShuffleRegisterInfo info = new ShuffleRegisterInfo(shuffleServerInfo, partitionRanges);
    assertEquals("ShuffleRegisterInfo: shuffleServerInfo[1], [PartitionRange[2, 3]]", info.toString());
  }

}
