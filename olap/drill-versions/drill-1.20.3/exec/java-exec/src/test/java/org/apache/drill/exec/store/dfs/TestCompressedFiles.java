/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.store.dfs;

import org.apache.drill.categories.UnlikelyTest;
import org.apache.drill.exec.ExecTest;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterFixtureBuilder;
import org.apache.drill.test.ClusterTest;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.CommonConfigurationKeys;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.io.compress.CompressionCodecFactory;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.junit.Assert.assertNotNull;

@Category(UnlikelyTest.class)
public class TestCompressedFiles extends ClusterTest {

  private static FileSystem fs;
  private static CompressionCodecFactory factory;

  @BeforeClass
  public static void setup() throws Exception {
    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher);
    startCluster(builder);

    fs = ExecTest.getLocalFileSystem();
    Configuration conf = fs.getConf();
    conf.set(CommonConfigurationKeys.IO_COMPRESSION_CODECS_KEY, ZipCodec.class.getCanonicalName());
    factory = new CompressionCodecFactory(conf);
  }

  @Test
  public void testGzip() throws Exception {
    String fileName = "gz_data.csvh.gz";
    writeData(fileName, "gzip", "id,name\n1,Fred\n2,Wilma");

    testBuilder()
      .sqlQuery("select * from dfs.`root`.`%s`", fileName)
      .unOrdered()
      .baselineColumns("id", "name")
      .baselineValues("1", "Fred")
      .baselineValues("2", "Wilma")
      .go();
  }

  @Test
  public void testBzip2() throws Exception {
    String fileName = "bzip2_data.csvh.bz2";
    writeData(fileName, "bzip2", "id,name\n3,Bamm-Bamm\n4,Barney");

    testBuilder()
      .sqlQuery("select * from dfs.`root`.`%s`", fileName)
      .unOrdered()
      .baselineColumns("id", "name")
      .baselineValues("3", "Bamm-Bamm")
      .baselineValues("4", "Barney")
      .go();
  }

  @Test
  public void testZip() throws Exception {
    String fileName = "zip_data.csvh.zip";
    writeData(fileName, "zip", "id,name\n5,Dino\n6,Pebbles");

    testBuilder()
      .sqlQuery("select * from dfs.`root`.`%s`", fileName)
      .unOrdered()
      .baselineColumns("id", "name")
      .baselineValues("5", "Dino")
      .baselineValues("6", "Pebbles")
      .go();
  }

  private void writeData(String fileName, String codecName, String data) throws IOException {
    CompressionCodec codec = factory.getCodecByName(codecName);
    assertNotNull(codecName + " is not found", codec);
    Path outFile = new Path(dirTestWatcher.getRootDir().getAbsolutePath(), fileName);
    try (InputStream inputStream = new ByteArrayInputStream(data.getBytes());
         OutputStream outputStream = codec.createOutputStream(fs.create(outFile))) {
      IOUtils.copyBytes(inputStream, outputStream, fs.getConf(), false);
    }
  }
}
