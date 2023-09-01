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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.Sets;
import org.apache.commons.io.FileUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.apache.uniffle.common.RemoteStorageInfo;
import org.apache.uniffle.common.util.Constants;
import org.apache.uniffle.coordinator.metric.CoordinatorMetrics;
import org.apache.uniffle.coordinator.strategy.storage.LowestIOSampleCostSelectStorageStrategy;

import static org.apache.uniffle.coordinator.ApplicationManager.StrategyName.IO_SAMPLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ClientConfManagerTest {

  @TempDir
  private final File remotePath = new File("hdfs://rss");
  private static MiniDFSCluster cluster;
  private final Configuration hdfsConf = new Configuration();

  @BeforeEach
  public void setUp() {
    CoordinatorMetrics.register();
  }

  @AfterEach
  public void clear() {
    CoordinatorMetrics.clear();
  }

  @AfterAll
  public static void close() {
    cluster.close();
  }

  public void createMiniHdfs(String hdfsPath) throws IOException {
    hdfsConf.set("fs.defaultFS", remotePath.getAbsolutePath());
    hdfsConf.set("dfs.nameservices", "rss");
    hdfsConf.set(MiniDFSCluster.HDFS_MINIDFS_BASEDIR, hdfsPath);
    cluster = (new MiniDFSCluster.Builder(hdfsConf)).build();
  }

  @Test
  public void test(@TempDir File tempDir) throws Exception {
    File cfgFile = File.createTempFile("tmp", ".conf", tempDir);
    final String cfgFileName = cfgFile.getAbsolutePath();
    final String filePath = Objects.requireNonNull(
        getClass().getClassLoader().getResource("coordinator.conf")).getFile();
    CoordinatorConf conf = new CoordinatorConf(filePath);
    conf.set(CoordinatorConf.COORDINATOR_DYNAMIC_CLIENT_CONF_PATH, tempDir.toURI().toString());
    conf.set(CoordinatorConf.COORDINATOR_DYNAMIC_CLIENT_CONF_ENABLED, true);
    ApplicationManager applicationManager = new ApplicationManager(conf);

    // file load checking at startup
    Exception expectedException = null;
    try {
      new ClientConfManager(conf, new Configuration(), applicationManager);
    } catch (RuntimeException e) {
      expectedException = e;
    }
    assertNotNull(expectedException);
    assertTrue(expectedException.getMessage().endsWith("is not a file."));

    conf.set(CoordinatorConf.COORDINATOR_DYNAMIC_CLIENT_CONF_PATH, cfgFile.toURI().toString());
    ClientConfManager clientConfManager = new ClientConfManager(conf, new Configuration(), applicationManager);
    assertEquals(0, clientConfManager.getClientConf().size());

    FileWriter fileWriter = new FileWriter(cfgFile);
    PrintWriter printWriter = new PrintWriter(fileWriter);
    printWriter.println("spark.mock.1 abc");
    printWriter.println(" spark.mock.2   123 ");
    printWriter.println("spark.mock.3 true  ");
    printWriter.flush();
    printWriter.close();
    // load config at the beginning
    clientConfManager = new ClientConfManager(conf, new Configuration(), applicationManager);
    Thread.sleep(1200);
    Map<String, String> clientConf = clientConfManager.getClientConf();
    assertEquals("abc", clientConf.get("spark.mock.1"));
    assertEquals("123", clientConf.get("spark.mock.2"));
    assertEquals("true", clientConf.get("spark.mock.3"));
    assertEquals(3, clientConf.size());

    // ignore empty or wrong content
    printWriter.println("");
    printWriter.flush();
    printWriter.close();
    Thread.sleep(1300);
    assertTrue(cfgFile.exists());
    clientConf = clientConfManager.getClientConf();
    assertEquals("abc", clientConf.get("spark.mock.1"));
    assertEquals("123", clientConf.get("spark.mock.2"));
    assertEquals("true", clientConf.get("spark.mock.3"));
    assertEquals(3, clientConf.size());

    // the config will not be changed when the conf file is deleted
    assertTrue(cfgFile.delete());
    Thread.sleep(1300);
    assertFalse(cfgFile.exists());
    clientConf = clientConfManager.getClientConf();
    assertEquals("abc", clientConf.get("spark.mock.1"));
    assertEquals("123", clientConf.get("spark.mock.2"));
    assertEquals("true", clientConf.get("spark.mock.3"));
    assertEquals(3, clientConf.size());

    // the normal update config process, move the new conf file to the old one
    File cfgFileTmp = new File(cfgFileName + ".tmp");
    fileWriter = new FileWriter(cfgFileTmp);
    printWriter = new PrintWriter(fileWriter);
    printWriter.println("spark.mock.4 deadbeaf");
    printWriter.println("spark.mock.5 9527");
    printWriter.println("spark.mock.6 9527 3423");
    printWriter.println("spark.mock.7");
    printWriter.close();
    FileUtils.moveFile(cfgFileTmp, cfgFile);
    Thread.sleep(1200);
    clientConf = clientConfManager.getClientConf();
    assertEquals("deadbeaf", clientConf.get("spark.mock.4"));
    assertEquals("9527", clientConf.get("spark.mock.5"));
    assertEquals(2, clientConf.size());
    assertFalse(clientConf.containsKey("spark.mock.6"));
    assertFalse(clientConf.containsKey("spark.mock.7"));
    clientConfManager.close();
  }

  @Test
  public void dynamicRemoteByAppNumStrategyStorageTest() throws Exception {
    int updateIntervalSec = 2;
    final String remotePath1 = "hdfs://host1/path1";
    final String remotePath2 = "hdfs://host2/path2";
    final String remotePath3 = "hdfs://host3/path3";
    File cfgFile = Files.createTempFile("dynamicRemoteStorageTest", ".conf").toFile();
    cfgFile.deleteOnExit();
    writeRemoteStorageConf(cfgFile, remotePath1);

    CoordinatorConf conf = new CoordinatorConf();
    conf.set(CoordinatorConf.COORDINATOR_DYNAMIC_CLIENT_CONF_UPDATE_INTERVAL_SEC, updateIntervalSec);
    conf.set(CoordinatorConf.COORDINATOR_DYNAMIC_CLIENT_CONF_PATH, cfgFile.toURI().toString());
    conf.setLong(CoordinatorConf.COORDINATOR_REMOTE_STORAGE_SCHEDULE_TIME, 60000);
    conf.setInteger(CoordinatorConf.COORDINATOR_REMOTE_STORAGE_SCHEDULE_ACCESS_TIMES, 1);
    conf.set(CoordinatorConf.COORDINATOR_DYNAMIC_CLIENT_CONF_ENABLED, true);
    ApplicationManager applicationManager = new ApplicationManager(conf);

    final ClientConfManager clientConfManager = new ClientConfManager(conf, new Configuration(), applicationManager);
    applicationManager.getSelectStorageStrategy().detectStorage();
    Set<String> expectedAvailablePath = Sets.newHashSet(remotePath1);
    assertEquals(expectedAvailablePath, applicationManager.getAvailableRemoteStorageInfo().keySet());
    RemoteStorageInfo remoteStorageInfo = applicationManager.pickRemoteStorage("testAppId1");
    assertEquals(remotePath1, remoteStorageInfo.getPath());
    assertTrue(remoteStorageInfo.getConfItems().isEmpty());

    writeRemoteStorageConf(cfgFile, remotePath3);
    expectedAvailablePath = Sets.newHashSet(remotePath3);
    waitForUpdate(expectedAvailablePath, applicationManager);
    applicationManager.getSelectStorageStrategy().detectStorage();
    remoteStorageInfo = applicationManager.pickRemoteStorage("testAppId2");
    assertEquals(remotePath3, remoteStorageInfo.getPath());

    String confItems = "host2,k1=v1,k2=v2;host3,k3=v3";
    writeRemoteStorageConf(cfgFile, remotePath2 + Constants.COMMA_SPLIT_CHAR + remotePath3, confItems);
    expectedAvailablePath = Sets.newHashSet(remotePath2, remotePath3);
    waitForUpdate(expectedAvailablePath, applicationManager);
    applicationManager.getSelectStorageStrategy().detectStorage();
    remoteStorageInfo = applicationManager.pickRemoteStorage("testAppId3");
    assertEquals(remotePath2, remoteStorageInfo.getPath());
    assertEquals(2, remoteStorageInfo.getConfItems().size());
    assertEquals("v1", remoteStorageInfo.getConfItems().get("k1"));
    assertEquals("v2", remoteStorageInfo.getConfItems().get("k2"));

    confItems = "host1,keyTest1=test1,keyTest2=test2;host2,k1=deadbeaf";
    writeRemoteStorageConf(cfgFile, remotePath1 + Constants.COMMA_SPLIT_CHAR + remotePath2, confItems);
    expectedAvailablePath = Sets.newHashSet(remotePath1, remotePath2);
    waitForUpdate(expectedAvailablePath, applicationManager);
    remoteStorageInfo = applicationManager.pickRemoteStorage("testAppId4");
    // one of remote storage will be chosen
    assertTrue(
        (remotePath1.equals(remoteStorageInfo.getPath())
                      && (remoteStorageInfo.getConfItems().size() == 2)
                      && (remoteStorageInfo.getConfItems().get("keyTest1").equals("test1")))
                      && (remoteStorageInfo.getConfItems().get("keyTest2").equals("test2"))
            || (remotePath2.equals(remoteStorageInfo.getPath())
                      && remoteStorageInfo.getConfItems().size() == 1)
                      && remoteStorageInfo.getConfItems().get("k1").equals("deadbeaf"));

    clientConfManager.close();
  }

  @Test
  public void dynamicRemoteByHealthStrategyStorageTest() throws Exception {
    final int updateIntervalSec = 2;
    final String remotePath1 = "hdfs://host1/path1";
    final String remotePath2 = "hdfs://host2/path2";
    final String remotePath3 = "hdfs://host3/path3";
    File cfgFile = Files.createTempFile("dynamicRemoteStorageTest", ".conf").toFile();
    cfgFile.deleteOnExit();
    writeRemoteStorageConf(cfgFile, remotePath1);
    createMiniHdfs(remotePath.getAbsolutePath());

    CoordinatorConf conf = new CoordinatorConf();
    conf.set(CoordinatorConf.COORDINATOR_DYNAMIC_CLIENT_CONF_UPDATE_INTERVAL_SEC, updateIntervalSec);
    conf.set(CoordinatorConf.COORDINATOR_DYNAMIC_CLIENT_CONF_PATH, cfgFile.toURI().toString());
    conf.set(CoordinatorConf.COORDINATOR_DYNAMIC_CLIENT_CONF_ENABLED, true);
    conf.setLong(CoordinatorConf.COORDINATOR_REMOTE_STORAGE_SCHEDULE_TIME, 60000);
    conf.setInteger(CoordinatorConf.COORDINATOR_REMOTE_STORAGE_SCHEDULE_ACCESS_TIMES, 1);
    conf.set(CoordinatorConf.COORDINATOR_REMOTE_STORAGE_SELECT_STRATEGY, IO_SAMPLE);

    ApplicationManager applicationManager = new ApplicationManager(conf);
    // init readWriteRankScheduler
    LowestIOSampleCostSelectStorageStrategy selectStorageStrategy =
        (LowestIOSampleCostSelectStorageStrategy) applicationManager.getSelectStorageStrategy();
    String testPath = "/test";

    final ClientConfManager clientConfManager = new ClientConfManager(conf, new Configuration(), applicationManager);
    // the reason for sleep here is to ensure that threads can be scheduled normally, the same below
    applicationManager.getSelectStorageStrategy().detectStorage();
    Set<String> expectedAvailablePath = Sets.newHashSet(remotePath1);
    assertEquals(expectedAvailablePath, applicationManager.getAvailableRemoteStorageInfo().keySet());
    selectStorageStrategy.sortPathByRankValue(remotePath1, testPath, System.currentTimeMillis());
    RemoteStorageInfo remoteStorageInfo = applicationManager.pickRemoteStorage("testAppId1");
    assertEquals(remotePath1, remoteStorageInfo.getPath());
    assertTrue(remoteStorageInfo.getConfItems().isEmpty());

    writeRemoteStorageConf(cfgFile, remotePath3);
    expectedAvailablePath = Sets.newHashSet(remotePath3);
    waitForUpdate(expectedAvailablePath, applicationManager);

    selectStorageStrategy.sortPathByRankValue(remotePath3, testPath, System.currentTimeMillis());
    applicationManager.getSelectStorageStrategy().detectStorage();
    remoteStorageInfo = applicationManager.pickRemoteStorage("testAppId2");
    assertEquals(remotePath3, remoteStorageInfo.getPath());

    String confItems = "host2,k1=v1,k2=v2;host3,k3=v3";
    final long current = System.currentTimeMillis();
    writeRemoteStorageConf(cfgFile, remotePath2 + Constants.COMMA_SPLIT_CHAR + remotePath3, confItems);
    expectedAvailablePath = Sets.newHashSet(remotePath2, remotePath3);
    waitForUpdate(expectedAvailablePath, applicationManager);
    selectStorageStrategy.sortPathByRankValue(remotePath2, testPath, current);
    selectStorageStrategy.sortPathByRankValue(remotePath3, testPath, current);
    applicationManager.getSelectStorageStrategy().detectStorage();
    remoteStorageInfo = applicationManager.pickRemoteStorage("testAppId3");
    assertEquals(remotePath2, remoteStorageInfo.getPath());
    assertEquals(2, remoteStorageInfo.getConfItems().size());
    assertEquals("v1", remoteStorageInfo.getConfItems().get("k1"));
    assertEquals("v2", remoteStorageInfo.getConfItems().get("k2"));

    confItems = "host1,keyTest1=test1,keyTest2=test2;host2,k1=deadbeaf";
    writeRemoteStorageConf(cfgFile, remotePath1 + Constants.COMMA_SPLIT_CHAR + remotePath2, confItems);
    expectedAvailablePath = Sets.newHashSet(remotePath1, remotePath2);
    waitForUpdate(expectedAvailablePath, applicationManager);
    remoteStorageInfo = applicationManager.pickRemoteStorage("testAppId4");
    // one of remote storage will be chosen
    assertTrue(
        (remotePath1.equals(remoteStorageInfo.getPath())
            && (remoteStorageInfo.getConfItems().size() == 2)
            && (remoteStorageInfo.getConfItems().get("keyTest1").equals("test1")))
            && (remoteStorageInfo.getConfItems().get("keyTest2").equals("test2"))
            || (remotePath2.equals(remoteStorageInfo.getPath())
            && remoteStorageInfo.getConfItems().size() == 1)
            && remoteStorageInfo.getConfItems().get("k1").equals("deadbeaf"));

    clientConfManager.close();
  }

  private void writeRemoteStorageConf(File cfgFile, String value) throws Exception {
    writeRemoteStorageConf(cfgFile, value, null);
  }

  private void writeRemoteStorageConf(File cfgFile, String pathItems, String confItems) throws Exception {
    // sleep 2 secs to make sure the modified time will be updated
    Thread.sleep(2000);
    FileWriter fileWriter = new FileWriter(cfgFile);
    PrintWriter printWriter = new PrintWriter(fileWriter);
    printWriter.println(CoordinatorConf.COORDINATOR_REMOTE_STORAGE_PATH.key() + " " + pathItems);
    if (confItems != null) {
      printWriter.println(CoordinatorConf.COORDINATOR_REMOTE_STORAGE_CLUSTER_CONF.key() + " " + confItems);
    }
    printWriter.flush();
    printWriter.close();
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
