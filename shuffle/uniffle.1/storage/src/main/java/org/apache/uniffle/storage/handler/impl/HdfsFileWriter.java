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

import java.io.Closeable;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.uniffle.common.util.ChecksumUtils;
import org.apache.uniffle.storage.api.FileWriter;
import org.apache.uniffle.storage.common.FileBasedShuffleSegment;
import org.apache.uniffle.storage.util.ShuffleStorageUtils;

public class HdfsFileWriter implements FileWriter, Closeable {

  private static final Logger LOG = LoggerFactory.getLogger(HdfsFileWriter.class);

  private final FileSystem fileSystem;

  private Path path;
  private Configuration hadoopConf;
  private FSDataOutputStream fsDataOutputStream;
  private long nextOffset;

  public HdfsFileWriter(FileSystem fileSystem, Path path, Configuration hadoopConf) throws IOException {
    this.path = path;
    this.hadoopConf = hadoopConf;
    this.fileSystem = fileSystem;
    initStream();
  }

  private void initStream() throws IOException, IllegalStateException {
    final FileSystem writerFs = fileSystem;
    if (writerFs.isFile(path)) {
      if (hadoopConf.getBoolean("dfs.support.append", true)) {
        fsDataOutputStream = writerFs.append(path);
        nextOffset = fsDataOutputStream.getPos();
      } else {
        String msg = path + " exists but append mode is not support!";
        LOG.error(msg);
        throw new IllegalStateException(msg);
      }
    } else if (writerFs.isDirectory(path)) {
      String msg = path + " is a directory!";
      LOG.error(msg);
      throw new IllegalStateException(msg);
    } else {
      fsDataOutputStream = writerFs.create(path);
      nextOffset = fsDataOutputStream.getPos();
    }
  }

  public void writeData(byte[] data) throws IOException {
    if (data != null && data.length > 0) {
      fsDataOutputStream.write(data);
      nextOffset = fsDataOutputStream.getPos();
    }
  }

  public void writeData(ByteBuffer byteBuffer) throws IOException {
    if (byteBuffer.hasArray()) {
      fsDataOutputStream.write(
          byteBuffer.array(), byteBuffer.arrayOffset() + byteBuffer.position(), byteBuffer.remaining());
    } else {
      byte[] byteArray = new byte[byteBuffer.remaining()];
      byteBuffer.get(byteArray);
      fsDataOutputStream.write(byteArray);
    }
    nextOffset = fsDataOutputStream.getPos();
  }

  public void writeIndex(FileBasedShuffleSegment segment) throws IOException {
    fsDataOutputStream.writeLong(segment.getOffset());
    fsDataOutputStream.writeInt(segment.getLength());
    fsDataOutputStream.writeInt(segment.getUncompressLength());
    fsDataOutputStream.writeLong(segment.getCrc());
    fsDataOutputStream.writeLong(segment.getBlockId());
    fsDataOutputStream.writeLong(segment.getTaskAttemptId());
  }

  // index file header is PartitionNum | [(PartitionId | PartitionFileLength | PartitionDataFileLength), ] | CRC
  public void writeHeader(List<Integer> partitionList,
      List<Long> indexFileSizeList,
      List<Long> dataFileSizeList) throws IOException {
    ByteBuffer headerContentBuf = ByteBuffer.allocate(
        (int)ShuffleStorageUtils.getIndexFileHeaderLen(partitionList.size()) - ShuffleStorageUtils.getHeaderCrcLen());
    fsDataOutputStream.writeInt(partitionList.size());
    headerContentBuf.putInt(partitionList.size());
    for (int i = 0; i < partitionList.size(); i++) {
      fsDataOutputStream.writeInt(partitionList.get(i));
      fsDataOutputStream.writeLong(indexFileSizeList.get(i));
      fsDataOutputStream.writeLong(dataFileSizeList.get(i));
      headerContentBuf.putInt(partitionList.get(i));
      headerContentBuf.putLong(indexFileSizeList.get(i));
      headerContentBuf.putLong(dataFileSizeList.get(i));
    }
    headerContentBuf.flip();
    fsDataOutputStream.writeLong(ChecksumUtils.getCrc32(headerContentBuf));
    long len = ShuffleStorageUtils.getIndexFileHeaderLen(partitionList.size());
    if (fsDataOutputStream.getPos() != len) {
      throw new IOException("Fail to write index header");
    }
  }

  public long nextOffset() {
    return nextOffset;
  }

  public void flush() throws IOException {
    if (fsDataOutputStream != null) {
      fsDataOutputStream.flush();
    }
  }

  @Override
  public synchronized void close() throws IOException {
    if (fsDataOutputStream != null) {
      fsDataOutputStream.close();
    }
  }

  public long copy(FileInputStream inputStream, int bufferSize) throws IOException {
    long start = fsDataOutputStream.getPos();
    IOUtils.copyBytes(inputStream, fsDataOutputStream, bufferSize);
    return fsDataOutputStream.getPos() - start;
  }
}
