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

package org.apache.uniffle.common.storage;

import java.util.Map;

import com.google.common.collect.Maps;
import org.junit.jupiter.api.Test;

import org.apache.uniffle.proto.RssProtos;

import static org.apache.uniffle.common.storage.StorageInfoUtils.fromProto;
import static org.apache.uniffle.common.storage.StorageInfoUtils.toProto;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class StorageInfoUtilsTest {
  @Test
  public void testFromProto() {
    // empty map should return empty result
    assertEquals(0, fromProto(Maps.newHashMap()).size());
    RssProtos.StorageInfo info = RssProtos.StorageInfo.newBuilder()
        .setMountPoint("/mnt")
        .setStorageMedia(RssProtos.StorageInfo.StorageMedia.HDD)
        .setCapacity(100)
        .setUsedBytes(95)
        .setStatus(RssProtos.StorageInfo.StorageStatus.NORMAL)
        .build();
    Map<String, RssProtos.StorageInfo> tmp = Maps.newHashMap();
    tmp.put(info.getMountPoint(), info);
    Map<String, StorageInfo> result = fromProto(tmp);
    assertEquals(1, result.size());
    assertNotNull(result.get(info.getMountPoint()));
    StorageInfo storageInfo = result.get(info.getMountPoint());
    StorageInfo expected = new StorageInfo(
        "/mnt",
        StorageMedia.HDD,
        100,
        95,
        StorageStatus.NORMAL
    );
    assertEquals(expected, storageInfo);
  }

  @Test
  public void testToProto() {
    // empty input
    assertEquals(0, toProto(Maps.newHashMap()).size());
    StorageInfo info = new StorageInfo(
        "/mnt",
        StorageMedia.HDD,
        100,
        95,
        StorageStatus.NORMAL
    );
    Map<String, StorageInfo> tmp = Maps.newHashMap();
    tmp.put("/mnt", info);
    Map<String, RssProtos.StorageInfo> result = toProto(tmp);
    assertEquals(1, result.size());
    assertEquals(info.toProto(), result.get("/mnt"));
  }

}
