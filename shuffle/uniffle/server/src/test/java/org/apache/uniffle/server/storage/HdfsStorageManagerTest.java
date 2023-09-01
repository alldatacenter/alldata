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

package org.apache.uniffle.server.storage;

import java.util.Arrays;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.apache.uniffle.common.RemoteStorageInfo;
import org.apache.uniffle.server.ShuffleServerConf;
import org.apache.uniffle.server.ShuffleServerMetrics;
import org.apache.uniffle.server.event.AppPurgeEvent;
import org.apache.uniffle.server.event.ShufflePurgeEvent;
import org.apache.uniffle.storage.common.HdfsStorage;
import org.apache.uniffle.storage.util.StorageType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

public class HdfsStorageManagerTest {

  @BeforeAll
  public static void prepare() {
    ShuffleServerMetrics.register();
  }

  @AfterAll
  public static void clear() {
    ShuffleServerMetrics.clear();
  }

  @Test
  public void testRemoveResources() {
    ShuffleServerConf conf = new ShuffleServerConf();
    conf.setString(ShuffleServerConf.RSS_STORAGE_TYPE, StorageType.MEMORY_LOCALFILE_HDFS.name());
    HdfsStorageManager hdfsStorageManager = new HdfsStorageManager(conf);
    final String remoteStoragePath1 = "hdfs://path1";
    String appId = "testRemoveResources_appId";
    hdfsStorageManager.registerRemoteStorage(
        appId,
        new RemoteStorageInfo(remoteStoragePath1, ImmutableMap.of("k1", "v1", "k2", "v2"))
    );
    Map<String, HdfsStorage> appStorageMap =  hdfsStorageManager.getAppIdToStorages();

    // case1
    assertEquals(1, appStorageMap.size());
    ShufflePurgeEvent shufflePurgeEvent = new ShufflePurgeEvent(appId, "", Arrays.asList(1));
    hdfsStorageManager.removeResources(shufflePurgeEvent);
    assertEquals(1, appStorageMap.size());

    // case2
    AppPurgeEvent appPurgeEvent = new AppPurgeEvent(appId, "");
    hdfsStorageManager.removeResources(appPurgeEvent);
    assertEquals(0, appStorageMap.size());
  }

  @Test
  public void testRegisterRemoteStorage() {
    ShuffleServerConf conf = new ShuffleServerConf();
    conf.setLong(ShuffleServerConf.FLUSH_COLD_STORAGE_THRESHOLD_SIZE, 2000L);
    conf.set(ShuffleServerConf.RSS_STORAGE_BASE_PATH, Arrays.asList("test"));
    conf.setLong(ShuffleServerConf.DISK_CAPACITY, 1024L);
    conf.setString(ShuffleServerConf.RSS_STORAGE_TYPE, StorageType.MEMORY_LOCALFILE_HDFS.name());
    HdfsStorageManager hdfsStorageManager = new HdfsStorageManager(conf);
    final String remoteStoragePath1 = "hdfs://path1";
    final String remoteStoragePath2 = "hdfs://path2";
    final String remoteStoragePath3 = "hdfs://path3";
    hdfsStorageManager.registerRemoteStorage(
        "app1",
        new RemoteStorageInfo(remoteStoragePath1, ImmutableMap.of("k1", "v1", "k2", "v2"))
    );
    hdfsStorageManager.registerRemoteStorage(
        "app2",
        new RemoteStorageInfo(remoteStoragePath2, ImmutableMap.of("k3", "v3"))
    );
    hdfsStorageManager.registerRemoteStorage(
        "app3",
        new RemoteStorageInfo(remoteStoragePath3, Maps.newHashMap())
    );
    Map<String, HdfsStorage> appStorageMap =  hdfsStorageManager.getAppIdToStorages();
    assertEquals(3, appStorageMap.size());
    assertEquals(Sets.newHashSet("app1", "app2", "app3"), appStorageMap.keySet());
    HdfsStorage hs1 = hdfsStorageManager.getAppIdToStorages().get("app1");
    assertSame(hdfsStorageManager.getPathToStorages().get(remoteStoragePath1), hs1);
    assertEquals("v1", hs1.getConf().get("k1"));
    assertEquals("v2", hs1.getConf().get("k2"));
    assertNull(hs1.getConf().get("k3"));
    HdfsStorage hs2 = hdfsStorageManager.getAppIdToStorages().get("app2");
    assertSame(hdfsStorageManager.getPathToStorages().get(remoteStoragePath2), hs2);
    assertEquals("v3", hs2.getConf().get("k3"));
    assertNull(hs2.getConf().get("k1"));
    assertNull(hs2.getConf().get("k2"));
    HdfsStorage hs3 = hdfsStorageManager.getAppIdToStorages().get("app3");
    assertSame(hdfsStorageManager.getPathToStorages().get(remoteStoragePath3), hs3);
    assertNull(hs3.getConf().get("k1"));
    assertNull(hs3.getConf().get("k2"));
    assertNull(hs3.getConf().get("k3"));
  }
}

