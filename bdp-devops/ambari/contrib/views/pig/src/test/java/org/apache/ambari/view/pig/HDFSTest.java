/**
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

package org.apache.ambari.view.pig;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsAction;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.security.UserGroupInformation;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.io.File;
import java.util.Map;

public abstract class HDFSTest extends BasePigTest {
  protected static MiniDFSCluster hdfsCluster;
  protected static String hdfsURI;

  @BeforeClass
  public static void startUp() throws Exception {
    BasePigTest.startUp(); // super
    File hdfsDir = new File("./target/PigTest/hdfs/")
        .getAbsoluteFile();
    FileUtil.fullyDelete(hdfsDir);

    Configuration conf = new Configuration();
    conf.set(MiniDFSCluster.HDFS_MINIDFS_BASEDIR, hdfsDir.getAbsolutePath());
    conf.set("hadoop.proxyuser." + System.getProperty("user.name") + ".groups", "*");
    conf.set("hadoop.proxyuser." + System.getProperty("user.name") + ".hosts", "*");

    MiniDFSCluster.Builder builder = new MiniDFSCluster.Builder(conf);
    hdfsCluster = builder.build();
    hdfsURI = hdfsCluster.getURI().toString();
    hdfsCluster.getFileSystem().mkdir(new Path("/tmp"), FsPermission.getDefault());
    hdfsCluster.getFileSystem().setPermission(new Path("/tmp"), new FsPermission(FsAction.ALL, FsAction.ALL, FsAction.ALL));
  }

  @AfterClass
  public static void shutDown() throws Exception {
    BasePigTest.shutDown();
    hdfsCluster.shutdown();
    hdfsCluster = null;
  }

  @Override
  protected void setupProperties(Map<String, String> properties, File baseDir) throws Exception {
    super.setupProperties(properties, baseDir);
    properties.put("webhdfs.url", hdfsURI);
    properties.put("webhdfs.username", System.getProperty("user.name"));
  }
}
