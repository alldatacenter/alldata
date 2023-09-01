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

package org.apache.uniffle.storage.util;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Random;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.apache.uniffle.storage.HdfsTestBase;
import org.apache.uniffle.storage.handler.impl.HdfsFileWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class ShuffleHdfsStorageUtilsTest extends HdfsTestBase {

  @Test
  public void testUploadFile(@TempDir File tempDir) throws Exception {
    createAndRunCases(tempDir, fs, HDFS_URI, HdfsTestBase.conf);
  }

  public static void createAndRunCases(
      File tempDir,
      FileSystem fileSystem,
      String clusterPathPrefix,
      Configuration hadoopConf) throws Exception {
    FileOutputStream fileOut = null;
    DataOutputStream dataOut = null;
    try {
      File file = new File(tempDir, "test");
      fileOut = new FileOutputStream(file);
      dataOut = new DataOutputStream(fileOut);
      byte[] buf = new byte[2096];
      new Random().nextBytes(buf);
      dataOut.write(buf);
      dataOut.close();
      fileOut.close();
      String path = clusterPathPrefix + "test";
      HdfsFileWriter writer = new HdfsFileWriter(fileSystem, new Path(path), hadoopConf);
      long size = ShuffleStorageUtils.uploadFile(file, writer, 1024);
      assertEquals(2096, size);
      size = ShuffleStorageUtils.uploadFile(file, writer, 100);
      assertEquals(2096, size);
      writer.close();
    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }
}
