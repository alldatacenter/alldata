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

package org.apache.uniffle.coordinator.strategy.storage;

import java.util.concurrent.CountDownLatch;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import org.apache.uniffle.common.util.Constants;
import org.apache.uniffle.coordinator.ApplicationManager;
import org.apache.uniffle.coordinator.CoordinatorConf;
import org.apache.uniffle.coordinator.metric.CoordinatorMetrics;
import org.apache.uniffle.coordinator.util.CoordinatorUtils;

import static org.apache.uniffle.coordinator.ApplicationManager.StrategyName.APP_BALANCE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

public class AppBalanceSelectStorageStrategyTest {

  private ApplicationManager applicationManager;
  private long appExpiredTime = 2000L;
  private String remotePath1 = "hdfs://path1";
  private String remotePath2 = "hdfs://path2";
  private String remotePath3 = "hdfs://path3";

  @BeforeAll
  public static void setup() {
    CoordinatorMetrics.register();
  }

  @AfterAll
  public static void clear() {
    CoordinatorMetrics.clear();
  }

  @BeforeEach
  public void setUp() throws InterruptedException {
    CoordinatorConf conf = new CoordinatorConf();
    conf.set(CoordinatorConf.COORDINATOR_APP_EXPIRED, appExpiredTime);
    conf.set(CoordinatorConf.COORDINATOR_REMOTE_STORAGE_SELECT_STRATEGY, APP_BALANCE);
    conf.setLong(CoordinatorConf.COORDINATOR_REMOTE_STORAGE_SCHEDULE_TIME, 1000);
    conf.setString(CoordinatorUtils.COORDINATOR_ID, "TESTXXX");
    applicationManager = new ApplicationManager(conf);
    // to ensure that the reading and writing of hdfs can be controlled
    applicationManager.closeDetectStorageScheduler();
  }

  @Test
  public void selectStorageTest() throws Exception {
    String remoteStoragePath = remotePath1 + Constants.COMMA_SPLIT_CHAR + remotePath2;
    applicationManager.refreshRemoteStorage(remoteStoragePath, "");
    applicationManager.getSelectStorageStrategy().detectStorage();
    assertEquals(0, applicationManager.getRemoteStoragePathRankValue().get(remotePath1).getAppNum().get());
    assertEquals(0, applicationManager.getRemoteStoragePathRankValue().get(remotePath2).getAppNum().get());
    String storageHost1 = "path1";
    assertEquals(0.0, CoordinatorMetrics.GAUGE_USED_REMOTE_STORAGE.get(storageHost1).get(), 0.5);
    String storageHost2 = "path2";
    assertEquals(0.0, CoordinatorMetrics.GAUGE_USED_REMOTE_STORAGE.get(storageHost2).get(), 0.5);
    // init readWriteRankScheduler
    Thread.sleep(2000);
    // do inc for remotePath1 to make sure pick storage will be remotePath2 in next call
    applicationManager.incRemoteStorageCounter(remotePath1);
    applicationManager.incRemoteStorageCounter(remotePath1);
    String testApp1 = "application_test_" + 1;
    applicationManager.registerApplicationInfo(testApp1, "user");
    applicationManager.refreshAppId(testApp1);
    // in this case, ensure that all the paths are read and written normally
    applicationManager.getRemoteStoragePathRankValue().get(remotePath1).getCostTime().set(0);
    applicationManager.getRemoteStoragePathRankValue().get(remotePath2).getCostTime().set(0);
    assertEquals(remotePath2, applicationManager.pickRemoteStorage(testApp1).getPath());
    assertEquals(remotePath2, applicationManager.getAppIdToRemoteStorageInfo().get(testApp1).getPath());
    assertEquals(1, applicationManager.getRemoteStoragePathRankValue().get(remotePath2).getAppNum().get());
    // return the same value if did the assignment already
    assertEquals(remotePath2, applicationManager.pickRemoteStorage(testApp1).getPath());
    assertEquals(1, applicationManager.getRemoteStoragePathRankValue().get(remotePath2).getAppNum().get());

    Thread.sleep(appExpiredTime + 2000);
    assertNull(applicationManager.getAppIdToRemoteStorageInfo().get(testApp1));
    assertEquals(0, applicationManager.getRemoteStoragePathRankValue().get(remotePath2).getAppNum().get());
    assertEquals(0.0, CoordinatorMetrics.GAUGE_USED_REMOTE_STORAGE.get(storageHost2).get(), 0.5);

    // refresh app1, got remotePath2, then remove remotePath2,
    // it should be existed in counter until it expired
    applicationManager.registerApplicationInfo(testApp1, "user");
    applicationManager.refreshAppId(testApp1);
    assertEquals(remotePath2, applicationManager.pickRemoteStorage(testApp1).getPath());
    remoteStoragePath = remotePath1;
    applicationManager.refreshRemoteStorage(remoteStoragePath, "");
    assertEquals(Sets.newConcurrentHashSet(Sets.newHashSet(remotePath1, remotePath2)),
        applicationManager.getRemoteStoragePathRankValue().keySet());
    assertEquals(1, applicationManager.getRemoteStoragePathRankValue().get(remotePath2).getAppNum().get());
    // app1 is expired, remotePath2 is removed because of counter = 0
    Thread.sleep(appExpiredTime + 2000);
    assertEquals(Sets.newConcurrentHashSet(Sets.newHashSet(remotePath1)),
        applicationManager.getRemoteStoragePathRankValue().keySet());

    // restore previous manually inc for next test case
    applicationManager.decRemoteStorageCounter(remotePath1);
    applicationManager.decRemoteStorageCounter(remotePath1);
    // remove all remote storage
    applicationManager.refreshRemoteStorage("", "");
    assertEquals(0, applicationManager.getAvailableRemoteStorageInfo().size());
    assertEquals(0, applicationManager.getRemoteStoragePathRankValue().size());
    assertFalse(applicationManager.hasErrorInStatusCheck());
  }

  @Test
  @Timeout(30)
  public void storageCounterMulThreadTest() throws Exception {
    String remoteStoragePath = remotePath1 + Constants.COMMA_SPLIT_CHAR + remotePath2
        + Constants.COMMA_SPLIT_CHAR + remotePath3;
    applicationManager.refreshRemoteStorage(remoteStoragePath, "");
    applicationManager.getSelectStorageStrategy().detectStorage();
    CountDownLatch cdl = new CountDownLatch(3);
    String testApp1 = "application_testAppId";
    Thread pickThread1 = new Thread(() -> {
      for (int i = 0; i < 1000; i++) {
        String appId = testApp1 + i;
        applicationManager.registerApplicationInfo(appId, "user");
        applicationManager.refreshAppId(appId);
        applicationManager.pickRemoteStorage(appId);
      }
      cdl.countDown();
    });

    Thread pickThread2 = new Thread(() -> {
      for (int i = 1000; i < 2000; i++) {
        String appId = testApp1 + i;
        applicationManager.registerApplicationInfo(appId, "user");
        applicationManager.refreshAppId(appId);
        applicationManager.pickRemoteStorage(appId);
      }
      cdl.countDown();
    });

    Thread pickThread3 = new Thread(() -> {
      for (int i = 2000; i < 3000; i++) {
        String appId = testApp1 + i;
        applicationManager.registerApplicationInfo(appId, "user");
        applicationManager.refreshAppId(appId);
        applicationManager.pickRemoteStorage(appId);
      }
      cdl.countDown();
    });
    pickThread1.start();
    pickThread2.start();
    pickThread3.start();
    cdl.await();
    Thread.sleep(appExpiredTime + 2000);

    applicationManager.refreshRemoteStorage("", "");
    assertEquals(0, applicationManager.getAvailableRemoteStorageInfo().size());
    assertEquals(0, applicationManager.getRemoteStoragePathRankValue().size());
    assertFalse(applicationManager.hasErrorInStatusCheck());
    assertEquals("TESTXXX",
        ((AppBalanceSelectStorageStrategy)applicationManager.getSelectStorageStrategy()).getCoordinatorId());
  }
}
