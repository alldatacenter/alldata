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

public class ShuffleBlockInfoTest {

  @Test
  public void testToString() {
    List<ShuffleServerInfo> shuffleServerInfos =
        Collections.singletonList(new ShuffleServerInfo("0", "localhost", 1234));
    ShuffleBlockInfo info = new ShuffleBlockInfo(1, 2, 3, 4, 5, new byte[6],
        shuffleServerInfos, 7, 8, 9);
    assertEquals("ShuffleBlockInfo:shuffleId[" + info.getShuffleId()
        + "],partitionId[" + info.getPartitionId()
        + "],blockId[" + info.getBlockId()
        + "],length[" + info.getLength()
        + "],uncompressLength[" + info.getUncompressLength()
        + "],crc[" + info.getCrc()
        + "],shuffleServer[0,]",
        info.toString());

    ShuffleBlockInfo info2 = new ShuffleBlockInfo(1, 2, 3, 4, 5, new byte[6],
        null, 7, 8, 9);
    assertEquals("ShuffleBlockInfo:shuffleId[" + info2.getShuffleId()
        + "],partitionId[" + info2.getPartitionId()
        + "],blockId[" + info2.getBlockId()
        + "],length[" + info2.getLength()
        + "],uncompressLength[" + info2.getUncompressLength()
        + "],crc[" + info2.getCrc()
        + "],shuffleServer is empty",
        info2.toString());
  }
}
