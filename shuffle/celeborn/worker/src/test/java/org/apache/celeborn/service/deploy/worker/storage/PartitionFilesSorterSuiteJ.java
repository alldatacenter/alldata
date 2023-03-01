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

package org.apache.celeborn.service.deploy.worker.storage;

import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.celeborn.common.CelebornConf;
import org.apache.celeborn.common.identity.UserIdentifier;
import org.apache.celeborn.common.meta.FileInfo;
import org.apache.celeborn.common.network.server.memory.MemoryManager;
import org.apache.celeborn.common.unsafe.Platform;
import org.apache.celeborn.common.util.Utils;
import org.apache.celeborn.service.deploy.worker.WorkerSource;

public class PartitionFilesSorterSuiteJ {

  private static Logger logger = LoggerFactory.getLogger(PartitionFilesSorterSuiteJ.class);

  private File shuffleFile;
  private FileInfo fileInfo;
  public final int CHUNK_SIZE = 8 * 1024 * 1024;
  private String originFileName;
  private long originFileLen;
  private FileWriter fileWriter;
  private long sortTimeout = 16 * 1000;
  private UserIdentifier userIdentifier = new UserIdentifier("mock-tenantId", "mock-name");

  public void prepare(boolean largefile) throws IOException {
    byte[] batchHeader = new byte[16];
    Random random = new Random();
    shuffleFile = File.createTempFile("RSS", "sort-suite");

    originFileName = shuffleFile.getAbsolutePath();
    fileInfo = new FileInfo(shuffleFile, userIdentifier);
    FileOutputStream fileOutputStream = new FileOutputStream(shuffleFile);
    FileChannel channel = fileOutputStream.getChannel();
    Map<Integer, Integer> batchIds = new HashMap<>();

    int maxMapId = 50;
    int mapCount = 1000;
    if (largefile) {
      mapCount = 15000;
    }
    for (int i = 0; i < mapCount; i++) {
      int mapId = random.nextInt(maxMapId);
      int currentAttemptId = 0;
      int batchId =
          batchIds.compute(
              mapId,
              (k, v) -> {
                if (v == null) {
                  v = 0;
                } else {
                  v++;
                }
                return v;
              });
      int dataSize = random.nextInt(192 * 1024) + 65525;
      byte[] mockedData = new byte[dataSize];
      Platform.putInt(batchHeader, Platform.BYTE_ARRAY_OFFSET, mapId);
      Platform.putInt(batchHeader, Platform.BYTE_ARRAY_OFFSET + 4, currentAttemptId);
      Platform.putInt(batchHeader, Platform.BYTE_ARRAY_OFFSET + 8, batchId);
      Platform.putInt(batchHeader, Platform.BYTE_ARRAY_OFFSET + 12, dataSize);
      ByteBuffer buf1 = ByteBuffer.wrap(batchHeader);
      while (buf1.hasRemaining()) {
        channel.write(buf1);
      }
      random.nextBytes(mockedData);
      ByteBuffer buf2 = ByteBuffer.wrap(mockedData);
      while (buf2.hasRemaining()) {
        channel.write(buf2);
      }
    }
    originFileLen = channel.size();
    fileInfo.getChunkOffsets().add(originFileLen);
    System.out.println(
        shuffleFile.getAbsolutePath()
            + " filelen "
            + (double) originFileLen / 1024 / 1024.0
            + "MB");

    MemoryManager.initialize(0.8, 0.9, 0.5, 0.6, 0.1, 0.1, 10, 10);
    fileWriter = Mockito.mock(FileWriter.class);
    when(fileWriter.getFile()).thenAnswer(i -> shuffleFile);
    when(fileWriter.getFileInfo()).thenAnswer(i -> fileInfo);
  }

  public void clean() {
    shuffleFile.delete();
  }

  @Test
  public void testSmallFile() throws InterruptedException, IOException {
    prepare(false);
    CelebornConf conf = new CelebornConf();
    PartitionFilesSorter partitionFilesSorter =
        new PartitionFilesSorter(MemoryManager.instance(), conf, new WorkerSource(conf));
    FileInfo info =
        partitionFilesSorter.getSortedFileInfo(
            "application-1", originFileName, fileWriter.getFileInfo(), 5, 10);
    Thread.sleep(1000);
    System.out.println(info.toString());
    Assert.assertTrue(info.numChunks() > 0);
    clean();
  }

  @Test
  @Ignore
  public void testLargeFile() throws InterruptedException, IOException {
    prepare(true);
    CelebornConf conf = new CelebornConf();
    PartitionFilesSorter partitionFilesSorter =
        new PartitionFilesSorter(MemoryManager.instance(), conf, new WorkerSource(conf));
    FileInfo info =
        partitionFilesSorter.getSortedFileInfo(
            "application-1", originFileName, fileWriter.getFileInfo(), 5, 10);
    Thread.sleep(30000);
    System.out.println(info.toString());
    Assert.assertTrue(info.numChunks() > 0);
    clean();
  }

  @Test
  public void testLevelDB() {
    if (Utils.isMacOnAppleSilicon()) {
      logger.info("Skip on Apple Silicon platform");
      return;
    }
    File recoverPath = Utils.createTempDir(System.getProperty("java.io.tmpdir"), "recover_path");
    CelebornConf conf = new CelebornConf();
    conf.set("celeborn.worker.graceful.shutdown.enabled", "true");
    conf.set("celeborn.worker.graceful.shutdown.recoverPath", recoverPath.getPath());
    PartitionFilesSorter partitionFilesSorter =
        new PartitionFilesSorter(MemoryManager.instance(), conf, new WorkerSource(conf));
    partitionFilesSorter.initSortedShuffleFiles("application-1-1");
    partitionFilesSorter.updateSortedShuffleFiles("application-1-1", "0-0-1", 0);
    partitionFilesSorter.updateSortedShuffleFiles("application-1-1", "0-0-2", 0);
    partitionFilesSorter.updateSortedShuffleFiles("application-1-1", "0-0-3", 0);
    partitionFilesSorter.initSortedShuffleFiles("application-2-1");
    partitionFilesSorter.updateSortedShuffleFiles("application-2-1", "0-0-1", 0);
    partitionFilesSorter.updateSortedShuffleFiles("application-2-1", "0-0-2", 0);
    partitionFilesSorter.initSortedShuffleFiles("application-3-1");
    partitionFilesSorter.updateSortedShuffleFiles("application-3-1", "0-0-1", 0);
    partitionFilesSorter.deleteSortedShuffleFiles("application-2-1");
    partitionFilesSorter.close();
    PartitionFilesSorter partitionFilesSorter2 =
        new PartitionFilesSorter(MemoryManager.instance(), conf, new WorkerSource(conf));
    Assert.assertEquals(
        partitionFilesSorter2.getSortedShuffleFiles("application-1-1").toString(),
        "[0-0-3, 0-0-2, 0-0-1]");
    Assert.assertEquals(partitionFilesSorter2.getSortedShuffleFiles("application-2-1"), null);
    Assert.assertEquals(
        partitionFilesSorter2.getSortedShuffleFiles("application-3-1").toString(), "[0-0-1]");
    partitionFilesSorter2.close();
    recoverPath.delete();
  }
}
