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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.concurrent.GuardedBy;

import com.google.common.annotations.VisibleForTesting;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.celeborn.common.exception.FileCorruptedException;
import org.apache.celeborn.common.meta.FileInfo;
import org.apache.celeborn.common.network.protocol.BacklogAnnouncement;
import org.apache.celeborn.common.network.protocol.ReadData;
import org.apache.celeborn.common.network.protocol.TransportableError;
import org.apache.celeborn.common.util.ExceptionUtils;
import org.apache.celeborn.common.util.Utils;
import org.apache.celeborn.service.deploy.worker.memory.BufferQueue;
import org.apache.celeborn.service.deploy.worker.memory.BufferRecycler;
import org.apache.celeborn.service.deploy.worker.memory.RecyclableBuffer;

public class MapDataPartitionReader implements Comparable<MapDataPartitionReader> {
  private static final Logger logger = LoggerFactory.getLogger(MapDataPartitionReader.class);

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

  private final AtomicInteger credits = new AtomicInteger();

  @GuardedBy("lock")
  protected final Queue<RecyclableBuffer> buffersToSend = new ArrayDeque<>();

  /** Whether all the data has been successfully read or not. */
  @GuardedBy("lock")
  private boolean readFinished;

  /** Whether this partition reader has been released or not. */
  @GuardedBy("lock")
  protected boolean isReleased;

  /** Exception causing the release of this partition reader. */
  @GuardedBy("lock")
  protected Throwable errorCause;

  /** Whether there is any error at the consumer side or not. */
  @GuardedBy("lock")
  protected boolean errorNotified;

  private FileChannel dataFileChannel;
  private FileChannel indexFileChannel;

  private Channel associatedChannel;

  private Runnable recycleStream;

  private AtomicInteger numInUseBuffers = new AtomicInteger(0);
  private boolean isOpen = false;

  public MapDataPartitionReader(
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
    this.readFinished = false;
  }

  public void open(FileChannel dataFileChannel, FileChannel indexFileChannel, long indexSize)
      throws IOException {
    if (!isOpen) {
      this.dataFileChannel = dataFileChannel;
      this.indexFileChannel = indexFileChannel;
      // index is (offset,length)
      long indexRegionSize = fileInfo.getNumSubpartitions() * (long) INDEX_ENTRY_SIZE;
      this.numRegions = Utils.checkedDownCast(indexSize / indexRegionSize);

      updateConsumingOffset();
      isOpen = true;
    }
  }

  public void addCredit(int credit) {
    credits.getAndAdd(credit);
  }

  public void readData(BufferQueue bufferQueue, BufferRecycler bufferRecycler) throws IOException {
    boolean hasRemaining = hasRemaining();
    boolean continueReading = hasRemaining;
    int numDataBuffers = 0;
    while (continueReading) {

      ByteBuf buffer = bufferQueue.poll();
      if (buffer == null) {
        // if there are no buffers available, halt current read and waiting for next triggered read
        break;
      } else {
        buffer.retain();
        numInUseBuffers.incrementAndGet();
      }

      try {
        continueReading = readBuffer(buffer);
      } catch (Throwable throwable) {
        bufferRecycler.recycle(buffer);
        numInUseBuffers.decrementAndGet();
        throw throwable;
      }

      hasRemaining = hasRemaining();
      addBuffer(buffer, bufferRecycler);
      ++numDataBuffers;
    }
    if (numDataBuffers > 0) {
      notifyBacklog(numDataBuffers);
    }

    if (!hasRemaining) {
      closeReader();
    }
  }

  private void addBuffer(ByteBuf buffer, BufferRecycler bufferRecycler) {
    if (buffer == null) {
      return;
    }
    synchronized (lock) {
      if (!isReleased) {
        buffersToSend.add(new RecyclableBuffer(buffer, bufferRecycler));
      } else {
        bufferRecycler.recycle(buffer);
        numInUseBuffers.decrementAndGet();
        throw new RuntimeException("Partition reader has been failed or finished.", errorCause);
      }
    }
  }

  public synchronized void sendData() {
    while (!buffersToSend.isEmpty() && credits.get() > 0) {
      RecyclableBuffer wrappedBuffer;
      synchronized (lock) {
        if (!isReleased) {
          wrappedBuffer = buffersToSend.poll();
        } else {
          return;
        }
      }

      int backlog = buffersToSend.size();
      int readableBytes = wrappedBuffer.byteBuf.readableBytes();
      logger.debug("send data start: {}, {}, {}", streamId, readableBytes, backlog);
      ReadData readData = new ReadData(streamId, wrappedBuffer.byteBuf);
      associatedChannel
          .writeAndFlush(readData)
          .addListener(
              (ChannelFutureListener)
                  future -> {
                    try {
                      if (!future.isSuccess()) {
                        recycleOnError(future.cause());
                      }
                    } finally {
                      logger.debug("send data end: {}, {}", streamId, readableBytes);
                      wrappedBuffer.recycle();
                      numInUseBuffers.decrementAndGet();
                    }
                  });

      int currentCredit = credits.decrementAndGet();
      logger.debug("stream {} credit {}", streamId, currentCredit);
    }

    if (readFinished && buffersToSend.isEmpty()) {
      recycle();
    }
  }

  private long getIndexRegionSize() {
    return fileInfo.getNumSubpartitions() * (long) INDEX_ENTRY_SIZE;
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
      throw new FileCorruptedException("File " + filename + " is corrupted");
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
        throw new FileCorruptedException("File " + fileInfo.getFilePath() + " is corrupted");
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
        throw new FileCorruptedException("File is corrupted");
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
      logger.error("Failed to read partition file.", throwable);
      isReleased = true;
      throw throwable;
    }
  }

  public boolean hasRemaining() {
    return currentPartitionRemainingBytes > 0;
  }

  private void notifyBacklog(int backlog) {
    logger.debug("stream manager stream id {} backlog:{}", streamId, backlog);
    associatedChannel
        .writeAndFlush(new BacklogAnnouncement(streamId, backlog))
        .addListener(
            future -> {
              if (!future.isSuccess()) {
                logger.error("send backlog {} to stream {} failed", backlog, streamId);
              }
            });
  }

  private void notifyError(Throwable throwable) {
    logger.error("read error stream id {} message:{}", streamId, throwable.getMessage(), throwable);
    if (throwable instanceof ClosedChannelException) {
      return;
    }

    if (this.associatedChannel.isActive()) {
      // If a stream is failed, send exceptions with the best effort, do not expect response.
      // And do not close channel because multiple streams are using the very same channel.
      // wrapIOException to PartitionUnRetryAbleException, client may choose regenerate the data.
      this.associatedChannel.writeAndFlush(
          new TransportableError(
              streamId, ExceptionUtils.wrapIOExceptionToUnRetryable(throwable, true)));
    }
  }

  public long getPriority() {
    return dataConsumingOffset;
  }

  @Override
  public int compareTo(MapDataPartitionReader that) {
    return Long.compare(getPriority(), that.getPriority());
  }

  public FileInfo getFileInfo() {
    return fileInfo;
  }

  public void closeReader() {
    synchronized (lock) {
      readFinished = true;
    }

    logger.debug("Closed read for stream {}", this.streamId);
  }

  public void recycle() {
    synchronized (lock) {
      if (!isReleased) {
        release();
        recycleStream.run();
      }
    }
  }

  public void recycleOnError(Throwable throwable) {
    synchronized (lock) {
      if (!errorNotified) {
        errorNotified = true;
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
        if (!buffersToSend.isEmpty()) {
          numInUseBuffers.addAndGet(-1 * buffersToSend.size());
          buffersToSend.forEach(RecyclableBuffer::recycle);
          buffersToSend.clear();
        }
        isReleased = true;
      }
    }
  }

  public boolean isFinished() {
    synchronized (lock) {
      // ensure every buffer are return to bufferQueue or release in buffersRead
      return numInUseBuffers.get() == 0 && isReleased;
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

  public long getStreamId() {
    return streamId;
  }

  @VisibleForTesting
  public AtomicInteger getNumInUseBuffers() {
    return numInUseBuffers;
  }

  public boolean shouldReadData() {
    return !isReleased && !readFinished;
  }
}
