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

package org.apache.uniffle.test;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Uninterruptibles;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.apache.uniffle.client.request.RssFetchClientConfRequest;
import org.apache.uniffle.client.request.RssFetchRemoteStorageRequest;
import org.apache.uniffle.client.response.RssFetchClientConfResponse;
import org.apache.uniffle.client.response.RssFetchRemoteStorageResponse;
import org.apache.uniffle.common.RemoteStorageInfo;
import org.apache.uniffle.common.rpc.StatusCode;
import org.apache.uniffle.coordinator.ApplicationManager;
import org.apache.uniffle.coordinator.CoordinatorConf;

import static org.apache.uniffle.coordinator.ApplicationManager.StrategyName.IO_SAMPLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FetchClientConfTest extends CoordinatorTestBase {

  @Test
  public void test(@TempDir File tempDir) throws Exception {
    File clientConfFile = File.createTempFile("tmp", ".conf", tempDir);
    FileWriter fileWriter = new FileWriter(clientConfFile);
    PrintWriter printWriter = new PrintWriter(fileWriter);
    printWriter.println("spark.mock.1  1234");
    printWriter.println(" spark.mock.2 true ");
    printWriter.flush();
    printWriter.close();

    CoordinatorConf coordinatorConf = getCoordinatorConf();
    coordinatorConf.setBoolean(CoordinatorConf.COORDINATOR_DYNAMIC_CLIENT_CONF_ENABLED, true);
    coordinatorConf.setString(CoordinatorConf.COORDINATOR_DYNAMIC_CLIENT_CONF_PATH,
        clientConfFile.getAbsolutePath());
    coordinatorConf.setInteger("rss.coordinator.dynamicClientConf.updateIntervalSec", 10);
    createCoordinatorServer(coordinatorConf);
    startServers();

    Uninterruptibles.sleepUninterruptibly(2, TimeUnit.SECONDS);
    RssFetchClientConfRequest request = new RssFetchClientConfRequest(2000);
    RssFetchClientConfResponse response = coordinatorClient.fetchClientConf(request);
    assertEquals(StatusCode.SUCCESS, response.getStatusCode());
    assertEquals(2, response.getClientConf().size());
    assertEquals("1234", response.getClientConf().get("spark.mock.1"));
    assertEquals("true", response.getClientConf().get("spark.mock.2"));
    assertNull(response.getClientConf().get("spark.mock.3"));
    shutdownServers();

    // dynamic client conf is disabled by default
    coordinatorConf = getCoordinatorConf();
    coordinatorConf.setString("rss.coordinator.dynamicClientConf.path", clientConfFile.getAbsolutePath());
    coordinatorConf.setInteger("rss.coordinator.dynamicClientConf.updateIntervalSec", 10);
    createCoordinatorServer(coordinatorConf);
    startServers();
    Uninterruptibles.sleepUninterruptibly(2, TimeUnit.SECONDS);
    request = new RssFetchClientConfRequest(2000);
    response = coordinatorClient.fetchClientConf(request);
    assertEquals(StatusCode.SUCCESS, response.getStatusCode());
    assertEquals(0, response.getClientConf().size());
    shutdownServers();

    // Fetch conf will not throw exception even if the request fails
    Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);
    request = new RssFetchClientConfRequest(10);
    response = coordinatorClient.fetchClientConf(request);
    assertEquals(StatusCode.INTERNAL_ERROR, response.getStatusCode());
    assertEquals(0, response.getClientConf().size());
    shutdownServers();
  }

  @Test
  public void testFetchRemoteStorageByApp(@TempDir File tempDir) throws Exception {
    String remotePath1 = "hdfs://path1";
    File cfgFile = File.createTempFile("tmp", ".conf", tempDir);
    String contItem = "path2,key1=test1,key2=test2";
    Map<String, String> dynamicConf = Maps.newHashMap();
    dynamicConf.put(CoordinatorConf.COORDINATOR_REMOTE_STORAGE_PATH.key(), remotePath1);
    dynamicConf.put(CoordinatorConf.COORDINATOR_REMOTE_STORAGE_CLUSTER_CONF.key(), contItem);
    writeRemoteStorageConf(cfgFile, dynamicConf);

    CoordinatorConf coordinatorConf = getCoordinatorConf();
    coordinatorConf.setBoolean(CoordinatorConf.COORDINATOR_DYNAMIC_CLIENT_CONF_ENABLED, true);
    coordinatorConf.setString(CoordinatorConf.COORDINATOR_DYNAMIC_CLIENT_CONF_PATH, cfgFile.toURI().toString());
    coordinatorConf.setInteger(CoordinatorConf.COORDINATOR_DYNAMIC_CLIENT_CONF_UPDATE_INTERVAL_SEC, 3);
    coordinatorConf.setLong(CoordinatorConf.COORDINATOR_REMOTE_STORAGE_SCHEDULE_TIME, 1000);
    coordinatorConf.setInteger(CoordinatorConf.COORDINATOR_REMOTE_STORAGE_SCHEDULE_ACCESS_TIMES, 1);
    createCoordinatorServer(coordinatorConf);
    startServers();

    waitForUpdate(Sets.newHashSet(remotePath1), coordinators.get(0).getApplicationManager());
    String appId = "application_testFetchRemoteStorageApp_" + 1;
    RssFetchRemoteStorageRequest request = new RssFetchRemoteStorageRequest(appId);
    RssFetchRemoteStorageResponse response = coordinatorClient.fetchRemoteStorage(request);
    RemoteStorageInfo remoteStorageInfo = response.getRemoteStorageInfo();
    assertTrue(remoteStorageInfo.getConfItems().isEmpty());
    assertEquals(remotePath1, remoteStorageInfo.getPath());

    // update remote storage info
    String remotePath2 = "hdfs://path2";
    dynamicConf.put(CoordinatorConf.COORDINATOR_REMOTE_STORAGE_PATH.key(), remotePath2);
    writeRemoteStorageConf(cfgFile, dynamicConf);
    waitForUpdate(Sets.newHashSet(remotePath2), coordinators.get(0).getApplicationManager());
    request = new RssFetchRemoteStorageRequest(appId);
    Thread.sleep(1500);
    response = coordinatorClient.fetchRemoteStorage(request);
    // remotePath1 will be return because (appId -> remote storage path) is in cache
    remoteStorageInfo = response.getRemoteStorageInfo();
    assertEquals(remotePath1, remoteStorageInfo.getPath());
    assertTrue(remoteStorageInfo.getConfItems().isEmpty());

    String newAppId = "application_testFetchRemoteStorageApp_" + 2;
    request = new RssFetchRemoteStorageRequest(newAppId);
    response = coordinatorClient.fetchRemoteStorage(request);
    // got the remotePath2 for new appId
    remoteStorageInfo = response.getRemoteStorageInfo();
    assertEquals(remotePath2, remoteStorageInfo.getPath());
    assertEquals(2, remoteStorageInfo.getConfItems().size());
    assertEquals("test1", remoteStorageInfo.getConfItems().get("key1"));
    assertEquals("test2", remoteStorageInfo.getConfItems().get("key2"));
    shutdownServers();
  }

  @Test
  public void testFetchRemoteStorageByIO(@TempDir File tempDir) throws Exception {
    String remotePath1 = "hdfs://path1";
    File cfgFile = File.createTempFile("tmp", ".conf", tempDir);
    String contItem = "path2,key1=test1,key2=test2";
    Map<String, String> dynamicConf = Maps.newHashMap();
    dynamicConf.put(CoordinatorConf.COORDINATOR_REMOTE_STORAGE_PATH.key(), remotePath1);
    dynamicConf.put(CoordinatorConf.COORDINATOR_REMOTE_STORAGE_CLUSTER_CONF.key(), contItem);
    writeRemoteStorageConf(cfgFile, dynamicConf);

    CoordinatorConf coordinatorConf = getCoordinatorConf();
    coordinatorConf.setBoolean(CoordinatorConf.COORDINATOR_DYNAMIC_CLIENT_CONF_ENABLED, true);
    coordinatorConf.setString(CoordinatorConf.COORDINATOR_DYNAMIC_CLIENT_CONF_PATH, cfgFile.toURI().toString());
    coordinatorConf.setInteger(CoordinatorConf.COORDINATOR_DYNAMIC_CLIENT_CONF_UPDATE_INTERVAL_SEC, 2);
    coordinatorConf.setLong(CoordinatorConf.COORDINATOR_REMOTE_STORAGE_SCHEDULE_TIME, 1000);
    coordinatorConf.setInteger(CoordinatorConf.COORDINATOR_REMOTE_STORAGE_SCHEDULE_ACCESS_TIMES, 1);
    coordinatorConf.set(CoordinatorConf.COORDINATOR_REMOTE_STORAGE_SELECT_STRATEGY, IO_SAMPLE);
    createCoordinatorServer(coordinatorConf);
    startServers();

    waitForUpdate(Sets.newHashSet(remotePath1), coordinators.get(0).getApplicationManager());
    String appId = "application_testFetchRemoteStorageApp_" + 1;
    RssFetchRemoteStorageRequest request = new RssFetchRemoteStorageRequest(appId);
    RssFetchRemoteStorageResponse response = coordinatorClient.fetchRemoteStorage(request);
    RemoteStorageInfo remoteStorageInfo = response.getRemoteStorageInfo();
    assertTrue(remoteStorageInfo.getConfItems().isEmpty());
    assertEquals(remotePath1, remoteStorageInfo.getPath());

    // update remote storage info
    String remotePath2 = "hdfs://path2";
    dynamicConf.put(CoordinatorConf.COORDINATOR_REMOTE_STORAGE_PATH.key(), remotePath2);
    writeRemoteStorageConf(cfgFile, dynamicConf);
    waitForUpdate(Sets.newHashSet(remotePath2), coordinators.get(0).getApplicationManager());
    request = new RssFetchRemoteStorageRequest(appId);
    response = coordinatorClient.fetchRemoteStorage(request);
    // remotePath1 will be return because (appId -> remote storage path) is in cache
    remoteStorageInfo = response.getRemoteStorageInfo();
    assertEquals(remotePath1, remoteStorageInfo.getPath());
    assertTrue(remoteStorageInfo.getConfItems().isEmpty());

    // ensure sizeList can be updated
    Thread.sleep(2000);
    String newAppId = "application_testFetchRemoteStorageApp_" + 2;
    request = new RssFetchRemoteStorageRequest(newAppId);
    response = coordinatorClient.fetchRemoteStorage(request);
    // got the remotePath2 for new appId
    remoteStorageInfo = response.getRemoteStorageInfo();
    assertEquals(remotePath2, remoteStorageInfo.getPath());
    assertEquals(2, remoteStorageInfo.getConfItems().size());
    assertEquals("test1", remoteStorageInfo.getConfItems().get("key1"));
    assertEquals("test2", remoteStorageInfo.getConfItems().get("key2"));
    shutdownServers();
  }

  private void waitForUpdate(
      Set<String> expectedAvailablePath,
      ApplicationManager applicationManager) throws Exception {
    int maxAttempt = 10;
    int attempt = 0;
    while (true) {
      if (attempt > maxAttempt) {
        throw new RuntimeException("Timeout when update configuration");
      }
      Thread.sleep(1000);
      try {
        assertEquals(expectedAvailablePath, applicationManager.getAvailableRemoteStorageInfo().keySet());
        break;
      } catch (Throwable e) {
        // ignore
      }
      attempt++;
    }
  }
}
