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

package org.apache.uniffle.storage.handler.impl;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.apache.uniffle.common.ShuffleDataResult;
import org.apache.uniffle.common.ShuffleIndexResult;
import org.apache.uniffle.common.config.RssBaseConf;
import org.apache.uniffle.storage.util.ShuffleStorageUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LocalFileHandlerTest {

  @Test
  public void writeTest(@TempDir File tmpDir) throws Exception {
    File dataDir1 = new File(tmpDir, "data1");
    File dataDir2 = new File(tmpDir, "data2");
    String[] basePaths = new String[]{dataDir1.getAbsolutePath(),
        dataDir2.getAbsolutePath()};
    final LocalFileWriteHandler writeHandler1 = new LocalFileWriteHandler("appId", 0, 1, 1,
        basePaths[0], "pre");
    final LocalFileWriteHandler writeHandler2 = new LocalFileWriteHandler("appId", 0, 2, 2,
        basePaths[0], "pre");

    String possiblePath1 = ShuffleStorageUtils.getFullShuffleDataFolder(dataDir1.getAbsolutePath(),
        ShuffleStorageUtils.getShuffleDataPath("appId", 0, 1, 1));
    String possiblePath2 = ShuffleStorageUtils.getFullShuffleDataFolder(dataDir2.getAbsolutePath(),
        ShuffleStorageUtils.getShuffleDataPath("appId", 0, 1, 1));
    assertTrue(writeHandler1.getBasePath().endsWith(possiblePath1)
        || writeHandler1.getBasePath().endsWith(possiblePath2));

    Map<Long, byte[]> expectedData = Maps.newHashMap();
    final Set<Long> expectedBlockIds1 = Sets.newHashSet();
    final Set<Long> expectedBlockIds2 = Sets.newHashSet();

    LocalFileHandlerTestBase.writeTestData(LocalFileHandlerTestBase.generateBlocks(1, 32),
        writeHandler1, expectedData, expectedBlockIds1);
    LocalFileHandlerTestBase.writeTestData(LocalFileHandlerTestBase.generateBlocks(2, 32),
        writeHandler1, expectedData, expectedBlockIds1);
    LocalFileHandlerTestBase.writeTestData(LocalFileHandlerTestBase.generateBlocks(3, 32),
        writeHandler1, expectedData, expectedBlockIds1);
    LocalFileHandlerTestBase.writeTestData(LocalFileHandlerTestBase.generateBlocks(4, 32),
        writeHandler1, expectedData, expectedBlockIds1);

    LocalFileHandlerTestBase.writeTestData(LocalFileHandlerTestBase.generateBlocks(3, 32),
        writeHandler2, expectedData, expectedBlockIds2);
    LocalFileHandlerTestBase.writeTestData(LocalFileHandlerTestBase.generateBlocks(3, 32),
        writeHandler2, expectedData, expectedBlockIds2);
    LocalFileHandlerTestBase.writeTestData(LocalFileHandlerTestBase.generateBlocks(2, 32),
        writeHandler2, expectedData, expectedBlockIds2);
    LocalFileHandlerTestBase.writeTestData(LocalFileHandlerTestBase.generateBlocks(1, 32),
        writeHandler2, expectedData, expectedBlockIds2);

    RssBaseConf conf = new RssBaseConf();
    conf.setString("rss.storage.basePath", dataDir1.getAbsolutePath() + "," + dataDir2.getAbsolutePath());
    LocalFileServerReadHandler readHandler1 = new LocalFileServerReadHandler(
        "appId", 0, 1, 1, 10, dataDir1.getAbsolutePath());
    LocalFileServerReadHandler readHandler2 = new LocalFileServerReadHandler(
        "appId", 0, 2, 1, 10, dataDir1.getAbsolutePath());

    LocalFileHandlerTestBase.validateResult(readHandler1, expectedBlockIds1, expectedData);
    LocalFileHandlerTestBase.validateResult(readHandler2, expectedBlockIds2, expectedData);

    // after first read, write more data
    LocalFileHandlerTestBase.writeTestData(LocalFileHandlerTestBase.generateBlocks(1, 32),
        writeHandler1, expectedData, expectedBlockIds1);
    // new data should be read
    LocalFileHandlerTestBase.validateResult(readHandler1, expectedBlockIds1, expectedData);

    ShuffleIndexResult shuffleIndexResult = LocalFileHandlerTestBase.readIndex(readHandler1);
    assertFalse(shuffleIndexResult.isEmpty());
    assertEquals(352, shuffleIndexResult.getDataFileLen());
    List<ShuffleDataResult> shuffleDataResults = LocalFileHandlerTestBase.readData(readHandler1, shuffleIndexResult);
    assertFalse(shuffleDataResults.isEmpty());
    File targetDataFile = new File(possiblePath1, "pre.data");
    targetDataFile.delete();
    shuffleDataResults = LocalFileHandlerTestBase.readData(readHandler1, shuffleIndexResult);
    for (ShuffleDataResult shuffleData : shuffleDataResults) {
      assertEquals(0, shuffleData.getData().length);
      assertTrue(shuffleData.isEmpty());
    }
  }

  @Test
  public void writeBigDataTest(@TempDir File tmpDir) throws IOException  {
    File writeFile = new File(tmpDir, "writetest");
    LocalFileWriter writer = new LocalFileWriter(writeFile);
    int  size = Integer.MAX_VALUE / 100;
    byte[] data = new byte[size];
    for (int i = 0; i < 200; i++) {
      writer.writeData(data);
    }
    long totalSize = 200L * size;
    assertEquals(writer.nextOffset(), totalSize);
  }

  @Test
  public void testReadIndex() {

  }

}
