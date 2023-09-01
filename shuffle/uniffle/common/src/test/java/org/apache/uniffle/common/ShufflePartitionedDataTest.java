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

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ShufflePartitionedDataTest {

  @Test
  public void testToString() {
    ShufflePartitionedData data = new ShufflePartitionedData(0, new ShufflePartitionedBlock[]{});
    assertEquals("ShufflePartitionedData{partitionId=" + data.getPartitionId() + ", blockList="
        + Arrays.toString(data.getBlockList()) + "}", data.toString());
    ShufflePartitionedData data1 = new ShufflePartitionedData(1,
        new ShufflePartitionedBlock[]{new ShufflePartitionedBlock(2, 3, 4, 5, 6, new byte[0])});
    assertEquals("ShufflePartitionedData{partitionId=" + data1.getPartitionId() + ", blockList="
        + Arrays.toString(data1.getBlockList()) + "}", data1.toString());
    ShufflePartitionedData data2 = new ShufflePartitionedData(0, null);
    assertEquals("ShufflePartitionedData{partitionId=0, blockList=null}", data2.toString());
    data2.setPartitionId(1);
    assertEquals("ShufflePartitionedData{partitionId=1, blockList=null}", data2.toString());
  }

}
