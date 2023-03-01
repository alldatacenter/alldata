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

package org.apache.celeborn.common.network.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.concurrent.GuardedBy;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.celeborn.common.meta.FileInfo;
import org.apache.celeborn.common.network.protocol.BacklogAnnouncement;
import org.apache.celeborn.common.network.protocol.ReadData;
import org.apache.celeborn.common.network.server.memory.Recycler;
import org.apache.celeborn.common.network.server.memory.WrappedDataBuffer;
import org.apache.celeborn.common.util.Utils;

public class DataPartitionReader implements Comparable<DataPartitionReader> {
  private static final Logger logger = LoggerFactory.getLogger(DataPartitionReader.class);

  private final ByteBuffer indexBuffer;
  private final ByteBuffer headerBuffer;
  private final int startPartitionIndex;
  private final int endPartitionIndex;
  private int numRegions;
  private int numRemainingPartitions;
  private int currentDataRegion = -1;
  private long dataConsumingOffset;
  private volatile long currentPartitionRemainingBytes;
  private FileInfo fileInfo;
  private int INDEX_ENTRY_SIZE = 16;
  private long streamId;
  protected final Object lock = new Object();

  private AtomicInteger credits = new AtomicInteger();

  @GuardedBy("lock")
  protected final Queue<WrappedDataBuffer> buffersRead = new ArrayDeque<>();

  /** Whether all the data has been successfully read or not. */
  @GuardedBy("lock")
  private boolean isClosed;

  /** Whether this partition reader has been released or not. */
  @GuardedBy("lock")
  protected boolean isReleased;

  /** Exception causing the release of this partition reader. */
  @GuardedBy("lock")
  protected Throwable errorCause;

  /** Whether there is any error at the consumer side or not. */
  @GuardedBy("lock")
  protected boolean isError;

  private FileChannel dataFileChannel;
  private FileChannel indexFileChannel;

  private Channel associatedChannel;

  private Runnable recycleStream;

  private AtomicInteger numInFlightRequests = new AtomicInteger(0);

  public DataPartitionReader(
      int startPartitionIndex,
      int endPartitionIndex,
      FileInfo fileInfo,
      long streamId,
      Channel associatedChannel,
      Runnable recycleStream) {
    this.startPartitionIndex = startPartitionIndex;
    this.endPartitionIndex = endPartitionIndex;

    int indexBufferSize = 16 * (endPartitionIndex - startPartitionIndex + 1);
    this.indexBuffer = ByteBuffer.allocateDirect(indexBufferSize);

    this.headerBuffer = ByteBuffer.allocateDirect(16);
    this.streamId = streamId;
    this.associatedChannel = associatedChannel;
    this.recycleStream = recycleStream;

    this.fileInfo = fileInfo;
    this.isClosed = false;
  }

  public void open(FileChannel dataFileChannel, FileChannel indexFileChannel) throws IOException {
    this.dataFileChannel = dataFileChannel;
    this.indexFileChannel = indexFileChannel;
    long indexFileSize = indexFileChannel.size();
    // index is (offset,length)
    long indexRegionSize = fileInfo.getNumReducerPartitions() * (long) INDEX_ENTRY_SIZE;
    this.numRegions = Utils.checkedDownCast(indexFileSize / indexRegionSize);

    updateConsumingOffset();
  }

  public boolean sendWithCredit(int credit) {
    int oldCredit = credits.getAndAdd(credit);
    if (oldCredit == 0) {
      return true;
    }

    return false;
  }

  public synchronized boolean readAndSend(Queue<ByteBuf> bufferQueue, Recycler bufferRecycler)
      throws IOException {
    boolean hasRemaining = hasRemaining();
    boolean continueReading = hasRemaining;
    int numDataBuffers = 0;
    while (continueReading) {

      ByteBuf buffer = bufferQueue.poll();
      // this is used for control bytebuf manually.
      if (buffer == null) {
        break;
      } else {
        buffer.retain();
      }

      try {
        continueReading = readBuffer(buffer);
      } catch (Throwable throwable) {
        bufferRecycler.release(buffer);
        throw throwable;
      }

      hasRemaining = hasRemaining();
      addBuffer(buffer, hasRemaining, bufferRecycler);
      ++numDataBuffers;
    }
    if (numDataBuffers > 0) {
      notifyBacklog(numDataBuffers);
    }

    if (!hasRemaining) {
      closeReader();
    }

    return hasRemaining;
  }

  private void addBuffer(ByteBuf buffer, boolean hasRemaining, Recycler bufferRecycler) {
    if (buffer == null) {
      return;
    }
    final boolean recycleBuffer;
    boolean notifyDataAvailable = false;
    final Throwable throwable;
    synchronized (lock) {
      recycleBuffer = isReleased || isClosed || isError;
      throwable = errorCause;
      isClosed = !hasRemaining;

      if (!recycleBuffer) {
        notifyDataAvailable = buffersRead.isEmpty();
        buffersRead.add(new WrappedDataBuffer(buffer, bufferRecycler));
      }
    }

    if (recycleBuffer) {
      bufferRecycler.release(buffer);
      throw new RuntimeException("Partition reader has been failed or finished.", throwable);
    }
    if (notifyDataAvailable) {
      sendData();
    }
  }

  public synchronized void sendData() {
    while (!buffersRead.isEmpty() && credits.get() > 0) {
      WrappedDataBuffer wrappedBuffer;
      synchronized (lock) {
        wrappedBuffer = buffersRead.poll();
        numInFlightRequests.incrementAndGet();
      }

      ByteBuf byteBuf = wrappedBuffer.byteBuf;
      int backlog = buffersRead.size();
      int readableBytes = byteBuf.readableBytes();
      logger.debug("send data start: {}, {}, {}", streamId, readableBytes, backlog);
      ReadData readData = new ReadData(streamId, backlog, 0, byteBuf);
      associatedChannel
          .writeAndFlush(readData)
          .addListener(
              (ChannelFutureListener)
                  future -> {
                    try {
                      if (!future.isSuccess()) {
                        wrappedBuffer.release();
                        recycleOnError(future.cause());
                      } else {
                        wrappedBuffer.recycle();
                      }
                    } finally {
                      logger.debug("send data start: {}, {}, {}", streamId, readableBytes, backlog);
                      numInFlightRequests.decrementAndGet();
                    }
                  });

      credits.decrementAndGet();
    }

    if (isClosed && buffersRead.isEmpty()) {
      recycle();
    }
  }

  private long getIndexRegionSize() {
    return fileInfo.getNumReducerPartitions() * (long) INDEX_ENTRY_SIZE;
  }

  private void readHeaderOrIndexBuffer(FileChannel channel, ByteBuffer buffer, int length)
      throws IOException {
    Utils.checkFileIntegrity(channel, length);
    buffer.clear();
    buffer.limit(length);
    while (buffer.hasRemaining()) {
      channel.read(buffer);
    }
    buffer.flip();
  }

  private void readBufferIntoReadBuffer(FileChannel channel, ByteBuf buf, int length)
      throws IOException {
    Utils.checkFileIntegrity(channel, length);
    ByteBuffer tmpBuffer = ByteBuffer.allocate(length);
    while (tmpBuffer.hasRemaining()) {
      channel.read(tmpBuffer);
    }
    tmpBuffer.flip();
    buf.writeBytes(tmpBuffer);
  }

  private int readBuffer(
      String filename, FileChannel channel, ByteBuffer header, ByteBuf buffer, int headerSize)
      throws IOException {
    readHeaderOrIndexBuffer(channel, header, headerSize);
    // header is combined of mapId(4),attemptId(4),nextBatchId(4) and total Compresszed Length(4)
    // we need size here,so we read length directly
    int bufferLength = header.getInt(12);
    if (bufferLength <= 0 || bufferLength > buffer.capacity()) {
      logger.error("Incorrect buffer header, buffer length: {}.", bufferLength);
      throw new RuntimeException("File " + filename + " is corrupted");
    }
    buffer.writeBytes(header);
    readBufferIntoReadBuffer(channel, buffer, bufferLength);
    return bufferLength + headerSize;
  }

  private void updateConsumingOffset() throws IOException {
    while (currentPartitionRemainingBytes == 0
        && (currentDataRegion < numRegions - 1 || numRemainingPartitions > 0)) {
      if (numRemainingPartitions <= 0) {
        ++currentDataRegion;
        numRemainingPartitions = endPartitionIndex - startPartitionIndex + 1;

        // read the target index entry to the target index buffer
        indexFileChannel.position(
            currentDataRegion * getIndexRegionSize()
                + (long) startPartitionIndex * INDEX_ENTRY_SIZE);
        readHeaderOrIndexBuffer(indexFileChannel, indexBuffer, indexBuffer.capacity());
      }

      // get the data file offset and the data size
      dataConsumingOffset = indexBuffer.getLong();
      currentPartitionRemainingBytes = indexBuffer.getLong();
      --numRemainingPartitions;

      logger.debug(
          "readBuffer updateConsumingOffset, {},  {}, {}, {}",
          streamId,
          dataFileChannel.size(),
          dataConsumingOffset,
          currentPartitionRemainingBytes);

      // if these checks fail, the partition file must be corrupted
      if (dataConsumingOffset < 0
          || dataConsumingOffset + currentPartitionRemainingBytes > dataFileChannel.size()
          || currentPartitionRemainingBytes < 0) {
        throw new RuntimeException("File " + fileInfo.getFilePath() + " is corrupted");
      }
    }
  }

  private boolean readBuffer(ByteBuf buffer) throws IOException {
    try {
      dataFileChannel.position(dataConsumingOffset);

      int readSize =
          readBuffer(
              fileInfo.getFilePath(),
              dataFileChannel,
              headerBuffer,
              buffer,
              headerBuffer.capacity());
      currentPartitionRemainingBytes -= readSize;

      logger.debug(
          "readBuffer data: {}, {}, {}, {}, {}, {}",
          streamId,
          currentPartitionRemainingBytes,
          readSize,
          dataConsumingOffset,
          fileInfo.getFilePath(),
          System.identityHashCode(buffer));

      // if this check fails, the partition file must be corrupted
      if (currentPartitionRemainingBytes < 0) {
        throw new RuntimeException("File is corrupted");
      } else if (currentPartitionRemainingBytes == 0) {
        logger.debug(
            "readBuffer end, {},  {}, {}, {}",
            streamId,
            dataFileChannel.size(),
            dataConsumingOffset,
            currentPartitionRemainingBytes);
        int prevDataRegion = currentDataRegion;
        updateConsumingOffset();
        return prevDataRegion == currentDataRegion && currentPartitionRemainingBytes > 0;
      }

      dataConsumingOffset = dataFileChannel.position();

      logger.debug(
          "readBuffer run: {}, {}, {}, {}",
          streamId,
          dataFileChannel.size(),
          dataConsumingOffset,
          currentPartitionRemainingBytes);
      return true;
    } catch (Throwable throwable) {
      logger.debug("Failed to read partition file.", throwable);
      isReleased = true;
      throw throwable;
    }
  }

  public boolean hasRemaining() {
    return currentPartitionRemainingBytes > 0;
  }

  private void notifyBacklog(int backlog) {
    logger.debug("stream manager stream id {} backlog:{}", streamId, backlog);
    associatedChannel.writeAndFlush(new BacklogAnnouncement(streamId, backlog));
  }

  private void notifyError(Throwable throwable) {
    logger.error("read error stream id {} message:{}", streamId, throwable.getMessage(), throwable);
    // TODO notify client the exception
  }

  public long getPriority() {
    return dataConsumingOffset;
  }

  @Override
  public int compareTo(DataPartitionReader that) {
    return Long.compare(getPriority(), that.getPriority());
  }

  public FileInfo getFileInfo() {
    return fileInfo;
  }

  public void closeReader() {
    synchronized (lock) {
      isClosed = true;
    }

    logger.debug("Closed read for stream {}", this.streamId);
  }

  private void recycle() {
    synchronized (lock) {
      if (!isReleased) {
        release();
        recycleStream.run();
      }
    }
  }

  public void recycleOnError(Throwable throwable) {
    synchronized (lock) {
      if (!isError) {
        isError = true;
        errorCause = throwable;
        notifyError(throwable);
        recycle();
      }
    }
  }

  public void release() {
    // we can safely release if reader reaches error or (read/send finished)
    synchronized (lock) {
      if (!isReleased) {
        logger.debug("release reader for stream {}", this.streamId);
        isReleased = true;
        if (!buffersRead.isEmpty()) {
          buffersRead.forEach(WrappedDataBuffer::release);
        }
      }
    }
  }

  public boolean isFinished() {
    synchronized (lock) {
      // ensure every buffer are return to bufferQueue or release in buffersRead
      return numInFlightRequests.get() == 0 && isReleased;
    }
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("DataPartitionReader{");
    sb.append("startPartitionIndex=").append(startPartitionIndex);
    sb.append(", endPartitionIndex=").append(endPartitionIndex);
    sb.append(", streamId=").append(streamId);
    sb.append('}');
    return sb.toString();
  }
}
