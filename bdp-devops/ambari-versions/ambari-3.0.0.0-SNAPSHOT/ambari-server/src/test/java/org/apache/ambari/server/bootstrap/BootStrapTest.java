/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.bootstrap;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.bootstrap.BootStrapStatus.BSStat;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * Test BootStrap Implementation.
 */
public class BootStrapTest extends TestCase {
  private static final Logger LOG = LoggerFactory.getLogger(BootStrapTest.class);
  public TemporaryFolder temp = new TemporaryFolder();

  @Override
  @Before
  public void setUp() throws IOException {
    temp.create();
  }

  @Override
  @After
  public void tearDown() throws IOException {
    temp.delete();
  }

  @Test
  public void testRun() throws Exception {
    Properties properties = new Properties();
    String bootdir = temp.newFolder("bootdir").toString();
    String metadetadir = temp.newFolder("metadetadir").toString();
    String serverVersionFilePath = temp.newFolder("serverVersionFilePath").toString();
    LOG.info("Bootdir is " + bootdir);
    LOG.info("Metadetadir is " + metadetadir);
    LOG.info("ServerVersionFilePath is " + serverVersionFilePath);

    String sharedResourcesDir = "src/test/resources/";
    if (System.getProperty("os.name").contains("Windows")) {
      sharedResourcesDir = ClassLoader.getSystemClassLoader().getResource("").getPath();
    }

    properties.setProperty(Configuration.BOOTSTRAP_DIRECTORY.getKey(), bootdir);
    properties.setProperty(Configuration.BOOTSTRAP_SCRIPT.getKey(), prepareEchoCommand(bootdir));
    properties.setProperty(Configuration.SRVR_KSTR_DIR.getKey(), "target" + File.separator + "classes");
    properties.setProperty(Configuration.METADATA_DIR_PATH.getKey(), metadetadir);
    properties.setProperty(Configuration.SERVER_VERSION_FILE.getKey(), serverVersionFilePath);
    properties.setProperty(Configuration.SHARED_RESOURCES_DIR.getKey(), sharedResourcesDir);
    properties.setProperty(Configuration.MPACKS_V2_STAGING_DIR_PATH.getKey(), "src/main/resources/mpacks-v2");

    Configuration conf = new Configuration(properties);
    AmbariMetaInfo ambariMetaInfo = new AmbariMetaInfo(conf);
    BootStrapImpl impl = new BootStrapImpl(conf, ambariMetaInfo);
    impl.init();
    SshHostInfo info = new SshHostInfo();
    info.setSshKey("xyz");
    ArrayList<String> hosts = new ArrayList<>();
    hosts.add("host1");
    hosts.add("host2");
    info.setUserRunAs("root");
    info.setHosts(hosts);
    info.setUser("user");
    info.setPassword("passwd");
    BSResponse response = impl.runBootStrap(info);
    LOG.info("Response id from bootstrap " + response.getRequestId());
    /* do a query */
    BootStrapStatus status = impl.getStatus(response.getRequestId());
    LOG.info("Status " + status.getStatus());
    int num = 0;
    while ((status.getStatus() == BSStat.RUNNING) && (num < 50)) {
      status = impl.getStatus(response.getRequestId());
      Thread.sleep(1000);
      num++;
    }
    // to give a time for bootstrap thread to finish
    Thread.sleep(5000);
    LOG.info("Status: log " + status.getLog() + " status=" + status.getStatus()
    );
    /* Note its an echo command so it should echo host1,host2 */
    Assert.assertTrue(status.getLog().contains("host1,host2"));
    Assert.assertEquals(BSStat.SUCCESS, status.getStatus());
    Assert.assertFalse(new File(bootdir + File.separator + "1" + File.separator + "sshKey").exists());
    Assert.assertFalse(new File(bootdir + File.separator + "1" + File.separator + "host_pass").exists());
  }

  private static String prepareEchoCommand(String bootdir) throws IOException {
    if (System.getProperty("os.name").contains("Windows")) {
      //The command line becomes "python echo", so create a Python script in the current dir
      String pythonEcho = "import sys;\nif __name__ == '__main__':\n" +
          "  args = sys.argv\n" +
          "  if len(args) > 1:\n" +
          "    print args[1]";
      File echo = new File(bootdir, "echo.py");
      //Ensure the file wasn't there
      echo.delete();
      FileUtils.writeStringToFile(echo, pythonEcho, Charset.defaultCharset());

      return echo.getPath();
    } else {
      return "echo";
    }
  }

  @Test
  public void testHostFailure() throws Exception {
    Properties properties = new Properties();
    String bootdir = temp.newFolder("bootdir").toString();
    String metadetadir = temp.newFolder("metadetadir").toString();
    String serverVersionFilePath = temp.newFolder("serverVersionFilePath").toString();
    LOG.info("Bootdir is " + bootdir);
    LOG.info("Metadetadir is " + metadetadir);
    LOG.info("ServerVersionFilePath is " + serverVersionFilePath);

    String sharedResourcesDir = "src/test/resources/";
    String serverKSTRDir = "target" + File.separator + "classes";
    String mpacksv2staging = "src/main/resources/mpacks-v2";
    if (System.getProperty("os.name").contains("Windows")) {
      sharedResourcesDir = ClassLoader.getSystemClassLoader().getResource("").getPath();
      serverKSTRDir = new File(new File(ClassLoader.getSystemClassLoader().getResource("").getPath()).getParent(), "classes").getPath();
    }

    properties.setProperty(Configuration.BOOTSTRAP_DIRECTORY.getKey(), bootdir);
    properties.setProperty(Configuration.BOOTSTRAP_SCRIPT.getKey(), prepareEchoCommand(bootdir));
    properties.setProperty(Configuration.SRVR_KSTR_DIR.getKey(), serverKSTRDir);
    properties.setProperty(Configuration.METADATA_DIR_PATH.getKey(), metadetadir);
    properties.setProperty(Configuration.SERVER_VERSION_FILE.getKey(), serverVersionFilePath);
    properties.setProperty(Configuration.SHARED_RESOURCES_DIR.getKey(), sharedResourcesDir);
    properties.setProperty(Configuration.MPACKS_V2_STAGING_DIR_PATH.getKey(), mpacksv2staging);
    Configuration conf = new Configuration(properties);
    AmbariMetaInfo ambariMetaInfo = new AmbariMetaInfo(conf);
    BootStrapImpl impl = new BootStrapImpl(conf, ambariMetaInfo);
    impl.init();
    SshHostInfo info = new SshHostInfo();
    info.setSshKey("xyz");
    ArrayList<String> hosts = new ArrayList<>();
    hosts.add("host1");
    hosts.add("host2");
    info.setHosts(hosts);
    info.setUser("user");
    info.setUserRunAs("root");
    info.setPassword("passwd");
    BSResponse response = impl.runBootStrap(info);
    long requestId = response.getRequestId();
    LOG.info("Response id from bootstrap " + requestId);
      /* create failed done file for host2 */
    File requestDir = new File(bootdir, Long.toString(requestId));
      /* wait while directory is created */
    int num = 0;
    while (!requestDir.exists() && num < 500) {
      Thread.sleep(100);
      num++;
    }
    if (!requestDir.exists()) {
      LOG.warn("RequestDir does not exists");
    }
    FileUtils.writeStringToFile(new File(requestDir, "host1.done"), "0", Charset.defaultCharset());
    FileUtils.writeStringToFile(new File(requestDir, "host2.done"), "1", Charset.defaultCharset());
      /* do a query */
    BootStrapStatus status = impl.getStatus(response.getRequestId());
    LOG.info("Status " + status.getStatus());
    num = 0;
    while ((status.getStatus() == BSStat.RUNNING) && (num < 500)) {
      status = impl.getStatus(response.getRequestId());
      Thread.sleep(100);
      num++;
    }
    LOG.info("Status: log " + status.getLog() + " status=" + status.getStatus()
    );
      /* Note its an echo command so it should echo host1,host2 */
    Assert.assertTrue(status.getLog().contains("host1,host2"));
    Assert.assertEquals(BSStat.ERROR, status.getStatus());
    Assert.assertEquals("DONE", status.getHostsStatus().get(0).getStatus());
    Assert.assertEquals("FAILED", status.getHostsStatus().get(1).getStatus());
  }


  @Test
  public void testPolling() throws Exception {
    File tmpFolder = temp.newFolder("bootstrap");
    /* create log and done files */
    FileUtils.writeStringToFile(new File(tmpFolder, "host1.done"), "0", Charset.defaultCharset());
    FileUtils.writeStringToFile(new File(tmpFolder, "host1.log"), "err_log_1",
            Charset.defaultCharset());
    FileUtils.writeStringToFile(new File(tmpFolder, "host2.done"), "1", Charset.defaultCharset());
    FileUtils.writeStringToFile(new File(tmpFolder, "host2.log"), "err_log_2",
            Charset.defaultCharset());

    List<String> listHosts = new ArrayList<>();
    listHosts.add("host1");
    listHosts.add("host2");
    BSHostStatusCollector collector = new BSHostStatusCollector(tmpFolder,
            listHosts);
    collector.run();
    List<BSHostStatus> polledHostStatus = collector.getHostStatus();
    Assert.assertTrue(polledHostStatus.size() == 2);
    Assert.assertEquals(polledHostStatus.get(0).getHostName(), "host1");
    Assert.assertEquals(polledHostStatus.get(0).getLog(), "err_log_1");
    Assert.assertEquals(polledHostStatus.get(0).getStatus(), "DONE");
    Assert.assertEquals(polledHostStatus.get(1).getHostName(), "host2");
    Assert.assertEquals(polledHostStatus.get(1).getLog(), "err_log_2");
    Assert.assertEquals(polledHostStatus.get(1).getStatus(), "FAILED");


  }

}
