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
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.junit.jupiter.api.Test;

import org.apache.uniffle.coordinator.ApplicationManager;
import org.apache.uniffle.coordinator.ClientConfManager;
import org.apache.uniffle.coordinator.CoordinatorConf;
import org.apache.uniffle.storage.HdfsTestBase;

import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ClientConfManagerHdfsTest extends HdfsTestBase {

  @Test
  public void test() throws Exception {
    String cfgFile = HDFS_URI + "/test/client_conf";
    createAndRunClientConfManagerCases(HDFS_URI, cfgFile, fs, HdfsTestBase.conf);
  }

  public static void createAndRunClientConfManagerCases(
      String clusterPathPrefix,
      String cfgFile,
      FileSystem fileSystem,
      Configuration hadoopConf) throws Exception {

    CoordinatorConf conf = new CoordinatorConf();
    conf.set(CoordinatorConf.COORDINATOR_DYNAMIC_CLIENT_CONF_PATH, clusterPathPrefix);
    conf.set(CoordinatorConf.COORDINATOR_DYNAMIC_CLIENT_CONF_UPDATE_INTERVAL_SEC, 1);
    conf.set(CoordinatorConf.COORDINATOR_DYNAMIC_CLIENT_CONF_ENABLED, true);

    // file load checking at startup
    Exception expectedException = null;
    try {
      new ClientConfManager(conf, new Configuration(), new ApplicationManager(conf));
    } catch (RuntimeException e) {
      expectedException = e;
    }
    assertNotNull(expectedException);
    assertTrue(expectedException.getMessage().endsWith("is not a file."));

    Path path = new Path(cfgFile);
    FSDataOutputStream out = fileSystem.create(path);
    conf.set(CoordinatorConf.COORDINATOR_DYNAMIC_CLIENT_CONF_PATH, cfgFile);
    ClientConfManager clientConfManager = new ClientConfManager(
        conf, new Configuration(), new ApplicationManager(conf));
    assertEquals(0, clientConfManager.getClientConf().size());

    PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(out));
    printWriter.println("spark.mock.1 abc");
    printWriter.println(" spark.mock.2   123 ");
    printWriter.println("spark.mock.3 true  ");
    printWriter.flush();
    printWriter.close();
    clientConfManager = new ClientConfManager(conf, hadoopConf, new ApplicationManager(conf));
    sleep(1200);
    Map<String, String> clientConf = clientConfManager.getClientConf();
    assertEquals("abc", clientConf.get("spark.mock.1"));
    assertEquals("123", clientConf.get("spark.mock.2"));
    assertEquals("true", clientConf.get("spark.mock.3"));
    assertEquals(3, clientConf.size());

    // ignore empty or wrong content
    printWriter.println("");
    printWriter.flush();
    printWriter.close();
    sleep(1300);
    assertTrue(fileSystem.exists(path));
    clientConf = clientConfManager.getClientConf();
    assertEquals("abc", clientConf.get("spark.mock.1"));
    assertEquals("123", clientConf.get("spark.mock.2"));
    assertEquals("true", clientConf.get("spark.mock.3"));
    assertEquals(3, clientConf.size());

    // the config will not be changed when the conf file is deleted
    fileSystem.delete(path, true);
    assertFalse(fileSystem.exists(path));
    sleep(1200);
    clientConf = clientConfManager.getClientConf();
    assertEquals("abc", clientConf.get("spark.mock.1"));
    assertEquals("123", clientConf.get("spark.mock.2"));
    assertEquals("true", clientConf.get("spark.mock.3"));
    assertEquals(3, clientConf.size());

    // the normal update config process, move the new conf file to the old one
    Path tmpPath = new Path(cfgFile + ".tmp");
    out = fileSystem.create(tmpPath);
    printWriter = new PrintWriter(new OutputStreamWriter(out));
    printWriter.println("spark.mock.4 deadbeaf");
    printWriter.println("spark.mock.5 9527");
    printWriter.println("spark.mock.6 9527 3423");
    printWriter.println("spark.mock.7");
    printWriter.close();
    fileSystem.rename(tmpPath, path);
    sleep(1200);
    clientConf = clientConfManager.getClientConf();
    assertEquals("deadbeaf", clientConf.get("spark.mock.4"));
    assertEquals("9527", clientConf.get("spark.mock.5"));
    assertEquals(2, clientConf.size());
    assertFalse(clientConf.containsKey("spark.mock.6"));
    assertFalse(clientConf.containsKey("spark.mock.7"));
    clientConfManager.close();
  }
}
