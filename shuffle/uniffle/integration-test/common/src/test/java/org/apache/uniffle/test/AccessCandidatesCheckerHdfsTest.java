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

import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import com.google.common.collect.Sets;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.apache.uniffle.coordinator.AccessManager;
import org.apache.uniffle.coordinator.ApplicationManager;
import org.apache.uniffle.coordinator.CoordinatorConf;
import org.apache.uniffle.coordinator.access.AccessInfo;
import org.apache.uniffle.coordinator.access.checker.AccessCandidatesChecker;
import org.apache.uniffle.coordinator.metric.CoordinatorMetrics;
import org.apache.uniffle.storage.HdfsTestBase;

import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AccessCandidatesCheckerHdfsTest extends HdfsTestBase {
  @BeforeEach
  public void setUp() {
    CoordinatorMetrics.register();
  }

  @AfterEach
  public void clear() {
    CoordinatorMetrics.clear();
  }

  @Test
  public void test() throws Exception {
    String candidatesFile = HDFS_URI + "/test/access_checker_candidates";
    createAndRunCases(HDFS_URI, candidatesFile, fs, HdfsTestBase.conf);
  }

  public static void createAndRunCases(
      String clusterPrefix,
      String candidatesFile,
      FileSystem fs,
      Configuration hadoopConf) throws Exception {

    CoordinatorConf conf = new CoordinatorConf();
    conf.set(CoordinatorConf.COORDINATOR_ACCESS_CANDIDATES_UPDATE_INTERVAL_SEC, 1);
    conf.set(CoordinatorConf.COORDINATOR_ACCESS_CANDIDATES_PATH, clusterPrefix);
    conf.setString(CoordinatorConf.COORDINATOR_ACCESS_CHECKERS.key(),
        "org.apache.uniffle.coordinator.access.checker.AccessCandidatesChecker");
    ApplicationManager applicationManager = new ApplicationManager(conf);
    // file load checking at startup
    Exception expectedException = null;
    try {
      new AccessManager(conf, null, applicationManager.getQuotaManager(), new Configuration());
    } catch (RuntimeException e) {
      expectedException = e;
    }
    assertNotNull(expectedException);
    assertTrue(expectedException.getMessage().contains(
        "NoSuchMethodException: org.apache.uniffle.coordinator.access.checker.AccessCandidatesChecker.<init>()"));
    conf.set(CoordinatorConf.COORDINATOR_ACCESS_CANDIDATES_PATH, candidatesFile);
    expectedException = null;
    try {
      new AccessManager(conf, null, applicationManager.getQuotaManager(), new Configuration());
    } catch (RuntimeException e) {
      expectedException = e;
    }
    assertNotNull(expectedException);
    assertTrue(expectedException.getMessage().contains(
        "NoSuchMethodException: org.apache.uniffle.coordinator.access.checker.AccessCandidatesChecker.<init>()"));

    Path path = new Path(candidatesFile);
    FSDataOutputStream out = fs.create(path);

    PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(out));
    printWriter.println("9527");
    printWriter.println(" 135 ");
    printWriter.println("2 ");
    printWriter.flush();
    printWriter.close();
    AccessManager accessManager = new AccessManager(conf, null,
        applicationManager.getQuotaManager(), hadoopConf);
    AccessCandidatesChecker checker = (AccessCandidatesChecker) accessManager.getAccessCheckers().get(0);
    // load the config at the beginning
    sleep(1200);
    assertEquals(Sets.newHashSet("2", "9527", "135"), checker.getCandidates().get());
    assertTrue(checker.check(new AccessInfo("9527")).isSuccess());
    assertTrue(checker.check(new AccessInfo("135")).isSuccess());
    assertFalse(checker.check(new AccessInfo("1")).isSuccess());
    assertFalse(checker.check(new AccessInfo("1_2")).isSuccess());

    // ignore empty or wrong content
    printWriter.println("");
    printWriter.flush();
    printWriter.close();
    sleep(1300);
    assertTrue(fs.exists(path));
    assertEquals(Sets.newHashSet("2", "9527", "135"), checker.getCandidates().get());
    assertTrue(checker.check(new AccessInfo("9527")).isSuccess());
    assertTrue(checker.check(new AccessInfo("135")).isSuccess());
    assertFalse(checker.check(new AccessInfo("1")).isSuccess());
    assertFalse(checker.check(new AccessInfo("1_2")).isSuccess());

    // the config will not be changed when the conf file is deleted
    fs.delete(path, true);
    assertFalse(fs.exists(path));
    sleep(1200);
    assertEquals(Sets.newHashSet("2", "9527", "135"), checker.getCandidates().get());
    assertTrue(checker.check(new AccessInfo("9527")).isSuccess());
    assertTrue(checker.check(new AccessInfo("135")).isSuccess());
    assertFalse(checker.check(new AccessInfo("1")).isSuccess());
    assertFalse(checker.check(new AccessInfo("1_2")).isSuccess());

    // the normal update config process, move the new conf file to the old one
    Path tmpPath = new Path(candidatesFile + ".tmp");
    out = fs.create(tmpPath);
    printWriter = new PrintWriter(new OutputStreamWriter(out));
    printWriter.println("9527");
    printWriter.println(" 1357 ");
    printWriter.flush();
    printWriter.close();
    fs.rename(tmpPath, path);
    sleep(1200);
    assertEquals(Sets.newHashSet("1357", "9527"), checker.getCandidates().get());
    assertTrue(checker.check(new AccessInfo("1357")).isSuccess());
    assertTrue(checker.check(new AccessInfo("9527")).isSuccess());
    checker.close();
  }
}
