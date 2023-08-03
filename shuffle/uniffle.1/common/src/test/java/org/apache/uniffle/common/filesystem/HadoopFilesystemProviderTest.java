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

package org.apache.uniffle.common.filesystem;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.security.PrivilegedExceptionAction;

import org.apache.commons.io.IOUtils;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.security.AccessControlException;
import org.apache.hadoop.security.UserGroupInformation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.apache.uniffle.common.KerberizedHdfsBase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HadoopFilesystemProviderTest extends KerberizedHdfsBase {

  @BeforeAll
  public static void beforeAll() throws Exception {
    testRunner = HadoopFilesystemProvider.class;
    KerberizedHdfsBase.init();
    UserGroupInformation.reset();
  }

  /**
   * When visiting secured HDFS but not initialize hadoop security context, it will throw exception
   */
  @Test
  public void testGetSecuredFilesystemButNotInitializeHadoopSecurityContext() throws Exception {

    removeHadoopSecurityContext();

    try {
      FileSystem fileSystem = HadoopFilesystemProvider.getFilesystem(new Path("/hdfs"), kerberizedHdfs.getConf());
      fileSystem.mkdirs(new Path("/hdfs/HadoopFilesystemProviderTest"));
    } catch (AccessControlException e) {
      // ignore
    }
  }

  @Test
  public void testGetSecuredFilesystem() throws Exception {
    initHadoopSecurityContext();

    // case1: it should throw exception when user is empty or null.
    try {
      FileSystem fileSystem = HadoopFilesystemProvider.getFilesystem(null, new Path("/hdfs"), kerberizedHdfs.getConf());
    } catch (Exception e) {
      assertTrue(e.getMessage().contains("User must be not null or empty"));
    }

    // case2: it should return the proxy user's filesystem
    FileSystem fileSystem = HadoopFilesystemProvider.getFilesystem("alex", new Path("/alex"), kerberizedHdfs.getConf());
    Path alexPath = new Path("/alex/HadoopFilesystemProviderTest-testGetSecuredFilesystem");
    assertTrue(fileSystem.mkdirs(alexPath));

    assertEquals("alex", fileSystem.getFileStatus(alexPath).getOwner());

    // case3: it should return the login user's filesystem
    fileSystem = HadoopFilesystemProvider.getFilesystem(new Path("/hdfs"), kerberizedHdfs.getConf());
    Path hdfsPath = new Path("/hdfs/HadoopFilesystemProviderTest-testGetSecuredFilesystem");
    assertTrue(fileSystem.mkdirs(hdfsPath));

    assertEquals("hdfs", fileSystem.getFileStatus(hdfsPath).getOwner());
  }

  @Test
  public void testWriteAndReadBySecuredFilesystem() throws Exception {
    initHadoopSecurityContext();

    // write file by proxy user.
    String fileContent = "hello world";
    Path filePath = new Path("/alex/HadoopFilesystemProviderTest-testWriteAndReadBySecuredFilesystem.file");
    FileSystem writeFs = HadoopFilesystemProvider.getFilesystem("alex", filePath, kerberizedHdfs.getConf());

    boolean ok = writeFs.exists(new org.apache.hadoop.fs.Path("/alex"));
    assertTrue(ok);
    assertEquals("alex", writeFs.getFileStatus(new org.apache.hadoop.fs.Path("/alex")).getOwner());

    FSDataOutputStream fsDataOutputStream = writeFs.create(filePath);
    BufferedWriter br = new BufferedWriter(new OutputStreamWriter(fsDataOutputStream, "UTF-8"));
    br.write(fileContent);
    br.close();

    assertTrue(writeFs.exists(filePath));
    assertEquals("alex", writeFs.getFileStatus(filePath).getOwner());

    // Read content from HDFS by alex user directly
    UserGroupInformation readerUGI = UserGroupInformation.loginUserFromKeytabAndReturnUGI(
        kerberizedHdfs.getAlexPrincipal() + "@" + kerberizedHdfs.getKdc().getRealm(),
        kerberizedHdfs.getAlexKeytab()
    );
    readerUGI.doAs((PrivilegedExceptionAction<Object>) () -> {
      FileSystem fs = FileSystem.get(kerberizedHdfs.getConf());
      FSDataInputStream inputStream = fs.open(filePath);
      String fetchedResult = IOUtils.toString(inputStream);
      assertEquals(fileContent, fetchedResult);
      return null;
    });
  }
}
