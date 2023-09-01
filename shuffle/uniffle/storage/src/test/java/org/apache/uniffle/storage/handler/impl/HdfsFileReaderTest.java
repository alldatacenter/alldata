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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Random;

import org.apache.hadoop.fs.Path;
import org.junit.jupiter.api.Test;

import org.apache.uniffle.common.util.ChecksumUtils;
import org.apache.uniffle.storage.HdfsTestBase;
import org.apache.uniffle.storage.common.FileBasedShuffleSegment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HdfsFileReaderTest extends HdfsTestBase {

  @Test
  public void createStreamTest() throws Exception {
    Path path = new Path(HDFS_URI, "createStreamTest");
    fs.create(path);

    try (HdfsFileReader reader = new HdfsFileReader(path, conf)) {
      assertTrue(fs.isFile(path));
      assertEquals(0L, reader.getOffset());
    }

    fs.deleteOnExit(path);
  }

  @Test
  public void createStreamAppendTest() throws IOException {
    Path path = new Path(HDFS_URI, "createStreamFirstTest");

    assertFalse(fs.isFile(path));
    Throwable ise = assertThrows(IllegalStateException.class, () -> new HdfsFileReader(path, conf));
    assertTrue(ise.getMessage().startsWith(HDFS_URI + "createStreamFirstTest don't exist"));
  }

  @Test
  public void readDataTest() throws Exception {
    Path path = new Path(HDFS_URI, "readDataTest");
    byte[] data = new byte[160];
    int offset = 128;
    int length = 32;
    new Random().nextBytes(data);
    long crc11 = ChecksumUtils.getCrc32(ByteBuffer.wrap(data, offset, length));

    try (HdfsFileWriter writer = new HdfsFileWriter(fs, path, conf)) {
      writer.writeData(data);
    }
    FileBasedShuffleSegment segment = new FileBasedShuffleSegment(23, offset, length, length, 0xdeadbeef, 1);
    try (HdfsFileReader reader = new HdfsFileReader(path, conf)) {
      byte[] actual = reader.read(segment.getOffset(), segment.getLength());
      long crc22 = ChecksumUtils.getCrc32(actual);

      for (int i = 0; i < length; ++i) {
        assertEquals(data[i + offset], actual[i]);
      }
      assertEquals(crc11, crc22);
      // EOF exception is expected
      segment = new FileBasedShuffleSegment(23, offset * 2, length, length, 1, 1);
      assertEquals(0, reader.read(segment.getOffset(), segment.getLength()).length);
    }
  }
}
