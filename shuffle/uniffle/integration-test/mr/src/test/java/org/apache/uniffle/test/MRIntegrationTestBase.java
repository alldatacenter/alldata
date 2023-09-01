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

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import com.google.common.collect.Lists;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapreduce.MRConfig;
import org.apache.hadoop.mapreduce.MRJobConfig;
import org.apache.hadoop.mapreduce.RssMRConfig;
import org.apache.hadoop.mapreduce.filecache.DistributedCache;
import org.apache.hadoop.mapreduce.v2.MiniMRYarnCluster;
import org.apache.hadoop.mapreduce.v2.TestMRJobs;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class MRIntegrationTestBase extends IntegrationTestBase {

  protected static MiniMRYarnCluster mrYarnCluster;
  protected static FileSystem localFs;

  static {
    try {
      localFs = FileSystem.getLocal(conf);
    } catch (IOException io) {
      throw new RuntimeException("problem getting local fs", io);
    }
  }

  private static Path TEST_ROOT_DIR = localFs.makeQualified(
      new Path("target", TestMRJobs.class.getName() + "-tmpDir"));
  static Path APP_JAR = new Path(TEST_ROOT_DIR, "MRAppJar.jar");
  private static final String OUTPUT_ROOT_DIR = "/tmp/"
      + TestMRJobs.class.getSimpleName();
  private static final Path TEST_RESOURCES_DIR = new Path(TEST_ROOT_DIR,
      "localizedResources");

  @BeforeAll
  public static void setUpMRYarn() throws IOException {
    mrYarnCluster = new MiniMRYarnCluster("test");
    conf.set(MRJobConfig.MR_AM_STAGING_DIR, "/apps_staging_dir");
    conf.setInt(YarnConfiguration.MAX_CLUSTER_LEVEL_APPLICATION_PRIORITY, 10);
    mrYarnCluster.init(conf);
    mrYarnCluster.start();
    localFs.copyFromLocalFile(new Path(MiniMRYarnCluster.APPJAR), APP_JAR);
    localFs.setPermission(APP_JAR, new FsPermission("700"));
  }

  @AfterAll
  public static void tearDown() throws IOException {
    if (mrYarnCluster != null) {
      mrYarnCluster.stop();
      mrYarnCluster = null;
    }

    if (localFs.exists(TEST_RESOURCES_DIR)) {
      // clean up resource directory
      localFs.delete(TEST_RESOURCES_DIR, true);
    }
  }

  public void run() throws Exception {
    JobConf appConf = new JobConf(mrYarnCluster.getConfig());
    updateCommonConfiguration(appConf);
    runOriginApp(appConf);
    final String originPath = appConf.get("mapreduce.output.fileoutputformat.outputdir");
    appConf = new JobConf(mrYarnCluster.getConfig());
    updateCommonConfiguration(appConf);
    runRssApp(appConf);
    String rssPath = appConf.get("mapreduce.output.fileoutputformat.outputdir");
    verifyResults(originPath, rssPath);

    appConf = new JobConf(mrYarnCluster.getConfig());
    appConf.set("mapreduce.rss.reduce.remote.spill.enable", "true");
    runRssApp(appConf);
    String rssRemoteSpillPath = appConf.get("mapreduce.output.fileoutputformat.outputdir");
    verifyResults(originPath, rssRemoteSpillPath);
  }

  private void updateCommonConfiguration(Configuration jobConf) {
    long mapMb = MRJobConfig.DEFAULT_MAP_MEMORY_MB;
    jobConf.set(MRJobConfig.MAP_JAVA_OPTS, "-Xmx" + mapMb + "m");
  }

  private void runOriginApp(Configuration jobConf) throws Exception {
    runMRApp(jobConf, getTestTool(), getTestArgs());
  }

  private void runRssApp(Configuration jobConf) throws Exception {
    URL url = MRIntegrationTestBase.class.getResource("/");
    final String parentPath = new Path(url.getPath()).getParent()
        .getParent().getParent().getParent().toString();
    if (System.getenv("JAVA_HOME") == null) {
      throw new RuntimeException("We must set JAVA_HOME");
    }
    jobConf.set(MRJobConfig.MR_AM_COMMAND_OPTS,
        "-XX:+TraceClassLoading org.apache.hadoop.mapreduce.v2.app.RssMRAppMaster");
    jobConf.set(MRJobConfig.REDUCE_JAVA_OPTS, "-XX:+TraceClassLoading -XX:MaxDirectMemorySize=419430400");
    jobConf.setInt(MRJobConfig.MAP_MEMORY_MB, 500);
    jobConf.setInt(MRJobConfig.REDUCE_MEMORY_MB, 2048);
    jobConf.setInt(MRJobConfig.IO_SORT_MB, 128);
    jobConf.set(MRJobConfig.MAP_OUTPUT_COLLECTOR_CLASS_ATTR, "org.apache.hadoop.mapred.RssMapOutputCollector");
    jobConf.set(MRConfig.SHUFFLE_CONSUMER_PLUGIN, "org.apache.hadoop.mapreduce.task.reduce.RssShuffle");
    jobConf.set(RssMRConfig.RSS_REDUCE_REMOTE_SPILL_ENABLED, "true");

    File file = new File(parentPath, "client-mr/target/shaded");
    File[] jars = file.listFiles();
    File localFile = null;
    for (File jar : jars) {
      if (jar.getName().startsWith("rss-client-mr")) {
        localFile = jar;
        break;
      }
    }
    assertNotNull(localFile);
    String props = System.getProperty("java.class.path");
    String newProps = "";
    String[] splittedProps = props.split(":");
    for (String prop : splittedProps)  {
      if (!prop.contains("classes") && !prop.contains("grpc") && !prop.contains("rss-")
          && !prop.contains("shuffle-storage")) {
        newProps = newProps + ":" + prop;
      } else if (prop.contains("mr") && prop.contains("integration-test")) {
        newProps = newProps + ":" + prop;
      }
    }
    System.setProperty("java.class.path", newProps);
    Path newPath = new Path(HDFS_URI + "/rss.jar");
    FileUtil.copy(file, fs, newPath, false, jobConf);
    DistributedCache.addFileToClassPath(
        new Path(newPath.toUri().getPath()), jobConf, fs);
    jobConf.set(MRJobConfig.MAPREDUCE_APPLICATION_CLASSPATH,
        "$PWD/rss.jar/" + localFile.getName() + "," + MRJobConfig.DEFAULT_MAPREDUCE_APPLICATION_CLASSPATH);
    jobConf.set(RssMRConfig.RSS_COORDINATOR_QUORUM, COORDINATOR_QUORUM);
    updateRssConfiguration(jobConf);
    runMRApp(jobConf, getTestTool(), getTestArgs());

  }

  protected String[] getTestArgs() {
    return new String[0];
  }

  protected void updateRssConfiguration(Configuration jobConf) {

  }

  private void runMRApp(Configuration conf, Tool tool, String[] args) throws Exception {
    assertEquals(0, ToolRunner.run(conf, tool, args), tool.getClass().getName() + " failed");
  }

  protected Tool getTestTool() {
    return null;
  }

  private void verifyResults(String originPath, String rssPath) throws Exception {
    if (originPath == null && rssPath == null) {
      return;
    }
    Path originPathFs = new Path(originPath);
    Path rssPathFs = new Path(rssPath);
    FileStatus[] originFiles = fs.listStatus(originPathFs);
    FileStatus[] rssFiles = fs.listStatus(rssPathFs);
    long originLen = 0;
    long rssLen = 0;
    List<String> originFileList = Lists.newArrayList();
    List<String> rssFileList = Lists.newArrayList();
    for (FileStatus file : originFiles) {
      originLen += file.getLen();
      String name = file.getPath().getName();
      if (!name.equals("_SUCCESS")) {
        originFileList.add(name);
      }
    }
    for (FileStatus file : rssFiles) {
      rssLen += file.getLen();
      String name = file.getPath().getName();
      if (!name.equals("_SUCCESS")) {
        rssFileList.add(name);
      }
    }
    assertEquals(originFileList.size(), rssFileList.size());
    for (int i = 0; i < originFileList.size(); i++) {
      assertEquals(originFileList.get(i), rssFileList.get(i));
      Path p1 = new Path(originPath, originFileList.get(i));
      FSDataInputStream f1 = fs.open(p1);
      Path p2 = new Path(rssPath, rssFileList.get(i));
      FSDataInputStream f2 = fs.open(p2);
      boolean isNotEof1 = true;
      boolean isNotEof2 = true;
      while (isNotEof1 && isNotEof2) {
        byte b1 = 1;
        byte b2 = 1;
        try {
          b1 = f1.readByte();
        } catch (EOFException ee) {
          isNotEof1 = false;
        }
        try {
          b2 = f2.readByte();
        } catch (EOFException ee) {
          isNotEof2 = false;
        }
        assertEquals(b1, b2);
      }
      assertEquals(isNotEof1, isNotEof2);
    }
    assertEquals(originLen, rssLen);

  }
}
