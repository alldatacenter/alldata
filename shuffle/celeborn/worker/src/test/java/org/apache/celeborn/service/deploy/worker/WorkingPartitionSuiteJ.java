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

package org.apache.celeborn.service.deploy.worker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import org.apache.celeborn.common.protocol.PartitionLocation;

public class WorkingPartitionSuiteJ {
  @Test
  public void testEquals() {
    List<WorkingPartition> list = new ArrayList<>();

    PartitionLocation p1 =
        new PartitionLocation(0, 0, "host1", 10, 9, 8, 14, PartitionLocation.Mode.SLAVE);
    PartitionLocation p2 =
        new PartitionLocation(1, 1, "host1", 11, 12, 13, 15, PartitionLocation.Mode.SLAVE);

    WorkingPartition pd1 = new WorkingPartition(p1, null);
    WorkingPartition pd2 = new WorkingPartition(p2, null);
    list.add(pd1);
    list.add(pd2);
    assert list.size() == 2;
    list.remove(p1);
    assert list.size() == 1;
    list.remove(p2);
    assert list.size() == 0;

    Map<PartitionLocation, PartitionLocation> map = new HashMap<>();
    map.put(pd1, pd1);
    map.put(pd2, pd2);

    PartitionLocation p =
        new PartitionLocation(0, 0, "host1", 10, 9, 8, 11, PartitionLocation.Mode.SLAVE);
    assert map.containsKey(p);

    map.remove(p1);
    assert map.size() == 1;
    map.put(p2, p2);
    assert map.size() == 1;

    Map<PartitionLocation, PartitionLocation> map2 = new HashMap<>();
    PartitionLocation p3 =
        new WorkingPartition(
            new PartitionLocation(
                2, 1, "30.225.12.48", 9096, 9097, 9098, 9099, PartitionLocation.Mode.MASTER),
            null);
    map2.put(p3, p3);
    PartitionLocation p4 =
        new PartitionLocation(
            2, 1, "30.225.12.48", 9096, 9097, 9098, 9099, PartitionLocation.Mode.SLAVE);
    assert map2.containsKey(p4);
  }
}
