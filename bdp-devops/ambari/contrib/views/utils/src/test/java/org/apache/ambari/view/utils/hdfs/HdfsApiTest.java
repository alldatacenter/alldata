/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.view.utils.hdfs;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.hdfs.protocol.ErasureCodingPolicy;
import org.apache.hadoop.io.erasurecode.ECSchema;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import static org.apache.ambari.view.utils.hdfs.HdfsApi.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HdfsApiTest {
  private FileSystem fs;
  private HdfsApi hdfsApi;
  private Configuration conf;
  private MiniDFSCluster hdfsCluster;

  @Before
  public void setup() throws IOException, HdfsApiException, InterruptedException {
    File baseDir = new File("./target/hdfs/" + "HdfsApiTest.filterAndTruncateDirStatus").getAbsoluteFile();
    FileUtil.fullyDelete(baseDir);

    conf = new Configuration();
    conf.set(MiniDFSCluster.HDFS_MINIDFS_BASEDIR, baseDir.getAbsolutePath());
    MiniDFSCluster.Builder builder = new MiniDFSCluster.Builder(conf);
    hdfsCluster = builder.build();
    String hdfsURI = hdfsCluster.getURI() + "/";
    conf.set("webhdfs.url", hdfsURI);
    conf.set("fs.defaultFS", hdfsURI);
    fs = FileSystem.get(conf);
    hdfsApi = new HdfsApi(conf, fs, null);

  }

  @After
  public void tearDown(){
    hdfsCluster.shutdown();
  }

  @Test
  public void testWith_EC_And_Encryption(){
//    Have to mock DummyFileStatus, because we cannot rely on internal class of hdfs
    DummyFileStatus fileStatus = mock(DummyFileStatus.class);
    FsPermission fsPermission = new FsPermission((short)0777);
    String ecPolicyName = "Some-EC-Policy";
    ECSchema ecSchema = new ECSchema("someSchema", 1, 1);
    ErasureCodingPolicy erasureCodingPolicy = new ErasureCodingPolicy(ecPolicyName, ecSchema, 1024, (byte)0);
    when(fileStatus.getPermission()).thenReturn(fsPermission);
    when(fileStatus.getPath()).thenReturn(new Path("/test/path"));
    when(fileStatus.getErasureCodingPolicy()).thenReturn(erasureCodingPolicy);
    when(fileStatus.isErasureCoded()).thenReturn(true);
    when(fileStatus.isEncrypted()).thenReturn(true);
    Map<String, Object> json = hdfsApi.fileStatusToJSON(fileStatus);

    Assert.assertEquals(Boolean.TRUE, json.get(KeyIsErasureCoded));
    Assert.assertEquals(Boolean.TRUE, json.get(KeyIsEncrypted));
    Assert.assertEquals(json.get(KeyErasureCodingPolicyName), ecPolicyName);
  }

  @Test
  public void testWithout_EC_And_Encryption(){
//    Have to mock DummyFileStatus, because we cannot rely on internal class of hdfs
    FsPermission fsPermission = new FsPermission((short)0777);

    DummyFileStatus fileStatus = mock(DummyFileStatus.class);
    when(fileStatus.getPermission()).thenReturn(fsPermission);

    when(fileStatus.getPath()).thenReturn(new Path("/test/path"));
    when(fileStatus.getErasureCodingPolicy()).thenReturn(null);
    when(fileStatus.isErasureCoded()).thenReturn(false);
    when(fileStatus.isEncrypted()).thenReturn(false);
    Map<String, Object> json = hdfsApi.fileStatusToJSON(fileStatus);

    Assert.assertEquals(Boolean.FALSE, json.get(KeyIsErasureCoded));
    Assert.assertEquals(Boolean.FALSE, json.get(KeyIsEncrypted));
    Assert.assertNull(json.get(KeyErasureCodingPolicyName));
  }

  @Test
  public void testNonHdfsFileStatus(){
//    Have to mock DummyNonHdfsFileStatus, because we cannot rely on internal class of hdfs
    DummyNonHdfsFileStatus fileStatus = mock(DummyNonHdfsFileStatus.class);
    FsPermission fsPermission = new FsPermission((short)0777);
    when(fileStatus.getPermission()).thenReturn(fsPermission);
    when(fileStatus.getPath()).thenReturn(new Path("/test/path"));

    when(fileStatus.isErasureCoded()).thenReturn(false);
    when(fileStatus.isEncrypted()).thenReturn(false);
    Map<String, Object> json = hdfsApi.fileStatusToJSON(fileStatus);

    Assert.assertEquals(Boolean.FALSE, json.get(KeyIsErasureCoded));
    Assert.assertEquals( Boolean.FALSE,json.get(KeyIsEncrypted));
    Assert.assertNull(json.get(KeyErasureCodingPolicyName));
  }
  @Test
  public void filterAndTruncateDirStatus() throws Exception {
    {
      // null fileStatuses
      DirStatus dirStatus = hdfsApi.filterAndTruncateDirStatus("", 0, null);
      Assert.assertEquals(new DirStatus(null, new DirListInfo(0, false, 0, "")), dirStatus);
    }

    {
      FileStatus[] fileStatuses = getFileStatuses(10);
      DirStatus dirStatus1 = hdfsApi.filterAndTruncateDirStatus("", 0, fileStatuses);
      Assert.assertEquals(new DirStatus(new FileStatus[0], new DirListInfo(10, true, 0, "")), dirStatus1);
    }

    {
      int originalSize = 10;
      int maxAllowedSize = 5;
      String nameFilter = "";
      FileStatus[] fileStatuses = getFileStatuses(originalSize);
      DirStatus dirStatus2 = hdfsApi.filterAndTruncateDirStatus(nameFilter, maxAllowedSize, fileStatuses);
      Assert.assertEquals(new DirStatus(Arrays.copyOf(fileStatuses, maxAllowedSize), new DirListInfo(originalSize, true, maxAllowedSize, nameFilter)), dirStatus2);
    }

    {
      int originalSize = 10;
      int maxAllowedSize = 10;
      String nameFilter = "";
      FileStatus[] fileStatuses = getFileStatuses(originalSize);
      DirStatus dirStatus2 = hdfsApi.filterAndTruncateDirStatus(nameFilter, maxAllowedSize, fileStatuses);
      Assert.assertEquals(new DirStatus(Arrays.copyOf(fileStatuses, maxAllowedSize), new DirListInfo(originalSize, false, maxAllowedSize, nameFilter)), dirStatus2);
    }

    {
      int originalSize = 11;
      int maxAllowedSize = 2;
      String nameFilter = "1";
      FileStatus[] fileStatuses = getFileStatuses(originalSize);
      DirStatus dirStatus = hdfsApi.filterAndTruncateDirStatus(nameFilter, maxAllowedSize, fileStatuses);

      Assert.assertEquals(new DirStatus(new FileStatus[]{fileStatuses[1], fileStatuses[10]}, new DirListInfo(originalSize, false, 2, nameFilter)), dirStatus);
    }

    {
      int originalSize = 20;
      int maxAllowedSize = 3;
      String nameFilter = "1";
      FileStatus[] fileStatuses = getFileStatuses(originalSize);
      DirStatus dirStatus = hdfsApi.filterAndTruncateDirStatus(nameFilter, maxAllowedSize, fileStatuses);

      Assert.assertEquals(new DirStatus(new FileStatus[]{fileStatuses[1], fileStatuses[10], fileStatuses[11]}, new DirListInfo(originalSize, true, 3, nameFilter)), dirStatus);
    }

    {
      int originalSize = 12;
      int maxAllowedSize = 3;
      String nameFilter = "1";
      FileStatus[] fileStatuses = getFileStatuses(originalSize);
      DirStatus dirStatus = hdfsApi.filterAndTruncateDirStatus(nameFilter, maxAllowedSize, fileStatuses);

      Assert.assertEquals(new DirStatus(new FileStatus[]{fileStatuses[1], fileStatuses[10], fileStatuses[11]}, new DirListInfo(originalSize, false, 3, nameFilter)), dirStatus);
    }

    {
      int originalSize = 13;
      int maxAllowedSize = 3;
      String nameFilter = "1";
      FileStatus[] fileStatuses = getFileStatuses(originalSize);
      DirStatus dirStatus = hdfsApi.filterAndTruncateDirStatus(nameFilter, maxAllowedSize, fileStatuses);

      Assert.assertEquals(new DirStatus(new FileStatus[]{fileStatuses[1], fileStatuses[10], fileStatuses[11]}, new DirListInfo(originalSize, true, 3, nameFilter)), dirStatus);
    }

    {
      int originalSize = 0;
      int maxAllowedSize = 3;
      String nameFilter = "1";
      FileStatus[] fileStatuses = getFileStatuses(originalSize);
      DirStatus dirStatus = hdfsApi.filterAndTruncateDirStatus(nameFilter, maxAllowedSize, fileStatuses);

      Assert.assertEquals(new DirStatus(new FileStatus[0], new DirListInfo(originalSize, false, originalSize, nameFilter)), dirStatus);
    }

    {
      int originalSize = 20;
      int maxAllowedSize = 3;
      String nameFilter = "";
      FileStatus[] fileStatuses = getFileStatuses(originalSize);
      DirStatus dirStatus = hdfsApi.filterAndTruncateDirStatus(nameFilter, maxAllowedSize, fileStatuses);

      Assert.assertEquals(new DirStatus(new FileStatus[]{fileStatuses[0], fileStatuses[1], fileStatuses[2]}, new DirListInfo(originalSize, true, maxAllowedSize, nameFilter)), dirStatus);
    }

    {
      int originalSize = 20;
      int maxAllowedSize = 3;
      String nameFilter = null;
      FileStatus[] fileStatuses = getFileStatuses(originalSize);
      DirStatus dirStatus = hdfsApi.filterAndTruncateDirStatus(nameFilter, maxAllowedSize, fileStatuses);

      Assert.assertEquals(new DirStatus(new FileStatus[]{fileStatuses[0], fileStatuses[1], fileStatuses[2]}, new DirListInfo(originalSize, true, maxAllowedSize, nameFilter)), dirStatus);
    }

    {
      int originalSize = 3;
      int maxAllowedSize = 3;
      String nameFilter = null;
      FileStatus[] fileStatuses = getFileStatuses(originalSize);
      DirStatus dirStatus = hdfsApi.filterAndTruncateDirStatus(nameFilter, maxAllowedSize, fileStatuses);

      Assert.assertEquals(new DirStatus(new FileStatus[]{fileStatuses[0], fileStatuses[1], fileStatuses[2]}, new DirListInfo(originalSize, false, maxAllowedSize, nameFilter)), dirStatus);
    }

    {
      int originalSize = 20;
      int maxAllowedSize = 3;
      String nameFilter = "a";
      FileStatus[] fileStatuses = getFileStatuses(originalSize);
      DirStatus dirStatus = hdfsApi.filterAndTruncateDirStatus(nameFilter, maxAllowedSize, fileStatuses);

      Assert.assertEquals(new DirStatus(new FileStatus[0], new DirListInfo(originalSize, false, 0, nameFilter)), dirStatus);
    }

  }

  private FileStatus[] getFileStatuses(int numberOfFiles) {
    FileStatus[] fileStatuses = new FileStatus[numberOfFiles];
    for(int i = 0 ; i < numberOfFiles; i++){
      fileStatuses[i] = getFileStatus("/"+i);
    }

    return fileStatuses;
  }

  private FileStatus getFileStatus(String path) {
    return new FileStatus(10, false, 3, 1000, 10000, new Path(path));
  }

}