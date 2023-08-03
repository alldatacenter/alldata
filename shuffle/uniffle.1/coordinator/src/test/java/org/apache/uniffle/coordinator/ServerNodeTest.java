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

package org.apache.uniffle.coordinator;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.Test;

import org.apache.uniffle.common.storage.StorageInfo;
import org.apache.uniffle.common.storage.StorageMedia;
import org.apache.uniffle.common.storage.StorageStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ServerNodeTest {

  @Test
  public void compareTest() {
    Set<String> tags = Sets.newHashSet("test");
    ServerNode sn1 = new ServerNode("sn1", "ip", 0, 100L, 50L, 20,
        10, tags, true);
    ServerNode sn2 = new ServerNode("sn2", "ip", 0, 100L, 50L, 21,
        10, tags, true);
    ServerNode sn3 = new ServerNode("sn3", "ip", 0, 100L, 50L, 20,
        11, tags, true);
    List<ServerNode> nodes = Lists.newArrayList(sn1, sn2, sn3);
    Collections.sort(nodes);
    assertEquals("sn2", nodes.get(0).getId());
    assertEquals("sn1", nodes.get(1).getId());
    assertEquals("sn3", nodes.get(2).getId());
  }

  @Test
  public void testStorageInfoOfServerNode() {
    Set<String> tags = Sets.newHashSet("tag");
    ServerNode sn1 = new ServerNode("sn1", "ip", 0, 100L, 50L, 20, 10, tags, true);
    // default constructor creates ServerNode with zero size of LocalStorage
    assertEquals(0, sn1.getStorageInfo().size());
    Map<String, StorageInfo> localStorageInfo = Maps.newHashMap();
    StorageInfo info = new StorageInfo(
        "/mnt",
        StorageMedia.SSD,
        100L,
        60L,
        StorageStatus.NORMAL);
    localStorageInfo.put("/mnt", info);
    ServerNode sn2 = new ServerNode("sn2", "ip", 0, 100L, 50L, 20, 10, tags, true, localStorageInfo);
    assertEquals(1, sn2.getStorageInfo().size());
  }
}
