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

import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.apache.uniffle.common.RemoteStorageInfo;
import org.apache.uniffle.common.util.Constants;
import org.apache.uniffle.coordinator.metric.CoordinatorMetrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ApplicationManagerTest {

  private ApplicationManager applicationManager;
  private long appExpiredTime = 2000L;
  private String remotePath1 = "hdfs://path1";
  private String remotePath2 = "hdfs://path2";
  private String remotePath3 = "hdfs://path3";
  private String remoteStorageConf = "path1,k1=v1,k2=v2;path2,k3=v3";

  @BeforeAll
  public static void setup() {
    CoordinatorMetrics.register();
  }

  @AfterAll
  public static void clear() {
    CoordinatorMetrics.clear();
  }

  @BeforeEach
  public void setUp() {
    CoordinatorConf conf = new CoordinatorConf();
    conf.set(CoordinatorConf.COORDINATOR_APP_EXPIRED, appExpiredTime);
    applicationManager = new ApplicationManager(conf);
  }

  @Test
  public void refreshTest() {
    String remoteStoragePath = remotePath1 + Constants.COMMA_SPLIT_CHAR + remotePath2;
    Set<String> expectedAvailablePath = Sets.newHashSet(remotePath1, remotePath2);
    applicationManager.refreshRemoteStorage(remoteStoragePath, "");
    assertEquals(expectedAvailablePath, applicationManager.getAvailableRemoteStorageInfo().keySet());
    assertEquals(expectedAvailablePath, applicationManager.getRemoteStoragePathRankValue().keySet());

    remoteStoragePath = remotePath3;
    expectedAvailablePath = Sets.newHashSet(remotePath3);
    applicationManager.refreshRemoteStorage(remoteStoragePath, "");
    assertEquals(expectedAvailablePath, applicationManager.getAvailableRemoteStorageInfo().keySet());
    assertEquals(expectedAvailablePath, applicationManager.getRemoteStoragePathRankValue().keySet());

    remoteStoragePath = remotePath1 + Constants.COMMA_SPLIT_CHAR + remotePath3;
    expectedAvailablePath = Sets.newHashSet(remotePath1, remotePath3);
    applicationManager.refreshRemoteStorage(remoteStoragePath, remoteStorageConf);
    assertEquals(expectedAvailablePath, applicationManager.getAvailableRemoteStorageInfo().keySet());
    assertEquals(expectedAvailablePath, applicationManager.getRemoteStoragePathRankValue().keySet());
    Map<String, RemoteStorageInfo> storages = applicationManager.getAvailableRemoteStorageInfo();
    RemoteStorageInfo remoteStorageInfo = storages.get(remotePath1);
    assertEquals(2, remoteStorageInfo.getConfItems().size());
    assertEquals("v1", remoteStorageInfo.getConfItems().get("k1"));
    assertEquals("v2", remoteStorageInfo.getConfItems().get("k2"));
    remoteStorageInfo = storages.get(remotePath3);
    assertTrue(remoteStorageInfo.getConfItems().isEmpty());

    remoteStoragePath = remotePath1 + Constants.COMMA_SPLIT_CHAR + remotePath2;
    expectedAvailablePath = Sets.newHashSet(remotePath1, remotePath2);
    applicationManager.refreshRemoteStorage(remoteStoragePath, remoteStorageConf);
    remoteStorageInfo = storages.get(remotePath2);
    assertEquals(1, remoteStorageInfo.getConfItems().size());
    assertEquals("v3", remoteStorageInfo.getConfItems().get("k3"));
    assertEquals(expectedAvailablePath, applicationManager.getAvailableRemoteStorageInfo().keySet());
    assertEquals(expectedAvailablePath, applicationManager.getRemoteStoragePathRankValue().keySet());
    assertFalse(applicationManager.hasErrorInStatusCheck());
  }

  @Test
  public void clearWithoutRemoteStorageTest() throws Exception {
    // test case for storage type without remote storage,
    // NPE shouldn't happen when clear the resource
    String testApp = "application_clearWithoutRemoteStorageTest";
    applicationManager.registerApplicationInfo(testApp, "user");
    applicationManager.refreshAppId(testApp);
    // just set a value != 0, it should be reset to 0 if everything goes well
    CoordinatorMetrics.gaugeRunningAppNum.set(100.0);
    assertEquals(1, applicationManager.getAppIds().size());
    Thread.sleep(appExpiredTime + 2000);
    assertEquals(0, applicationManager.getAppIds().size());
    assertFalse(applicationManager.hasErrorInStatusCheck());
  }
}
