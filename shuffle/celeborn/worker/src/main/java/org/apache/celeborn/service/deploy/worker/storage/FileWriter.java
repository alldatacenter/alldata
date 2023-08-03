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

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.concurrent.GuardedBy;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import org.roaringbitmap.RoaringBitmap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.celeborn.common.CelebornConf;
import org.apache.celeborn.common.exception.AlreadyClosedException;
import org.apache.celeborn.common.meta.DiskStatus;
import org.apache.celeborn.common.meta.FileInfo;
import org.apache.celeborn.common.metrics.source.AbstractSource;
import org.apache.celeborn.common.protocol.PartitionSplitMode;
import org.apache.celeborn.common.protocol.PartitionType;
import org.apache.celeborn.common.protocol.StorageInfo;
import org.apache.celeborn.common.unsafe.Platform;
import org.apache.celeborn.service.deploy.worker.WorkerSource;
import org.apache.celeborn.service.deploy.worker.congestcontrol.CongestionController;
import org.apache.celeborn.service.deploy.worker.memory.MemoryManager;

/*
 * Note: Once FlushNotifier.exception is set, the whole file is not available.
 *       That's fine some of the internal state(e.g. bytesFlushed) may be inaccurate.
 */
public abstract class FileWriter implements DeviceObserver {
  private static final Logger logger = LoggerFactory.getLogger(FileWriter.class);
  private static final long WAIT_INTERVAL_MS = 5;

  protected final FileInfo fileInfo;
  private FileChannel channel;
  private volatile boolean closed;
  private volatile boolean destroyed;

  protected final AtomicInteger numPendingWrites = new AtomicInteger();

  public final Flusher flusher;
  private final int flushWorkerIndex;

  @GuardedBy("flushLock")
  private CompositeByteBuf flushBuffer;

  private final Object flushLock = new Object();
  private final long writerCloseTimeoutMs;

  protected final long flusherBufferSize;

  protected final DeviceMonitor deviceMonitor;
  protected final AbstractSource source; // metrics

  private long splitThreshold = 0;
  private final PartitionSplitMode splitMode;
  private final PartitionType partitionType;
  private final boolean rangeReadFilter;
  protected boolean deleted = false;
  private RoaringBitmap mapIdBitMap = null;
  protected final FlushNotifier notifier = new FlushNotifier();

  public FileWriter(
      FileInfo fileInfo,
      Flusher flusher,
      AbstractSource workerSource,
      CelebornConf conf,
      DeviceMonitor deviceMonitor,
      long splitThreshold,
      PartitionSplitMode splitMode,
      PartitionType partitionType,
      boolean rangeReadFilter)
      throws IOException {
    this.fileInfo = fileInfo;
    this.flusher = flusher;
    this.flushWorkerIndex = flusher.getWorkerIndex();
    this.writerCloseTimeoutMs = conf.workerWriterCloseTimeoutMs();
    this.splitThreshold = splitThreshold;
    this.deviceMonitor = deviceMonitor;
    this.splitMode = splitMode;
    this.partitionType = partitionType;
    this.rangeReadFilter = rangeReadFilter;
    if (!fileInfo.isHdfs()) {
      this.flusherBufferSize = conf.workerFlusherBufferSize();
      channel = FileChannelUtils.createWritableFileChannel(fileInfo.getFilePath());
    } else {
      this.flusherBufferSize = conf.workerHdfsFlusterBufferSize();
      // We open the stream and close immediately because HDFS output stream will
      // create a DataStreamer that is a thread.
      // If we reuse HDFS output stream, we will exhaust the memory soon.
      try {
        StorageManager.hadoopFs().create(fileInfo.getHdfsPath(), true).close();
      } catch (IOException e) {
        try {
          // If create file failed, wait 10 ms and retry
          Thread.sleep(10);
        } catch (InterruptedException ex) {
          throw new RuntimeException(ex);
        }
        StorageManager.hadoopFs().create(fileInfo.getHdfsPath(), true).close();
      }
    }
    source = workerSource;
    logger.debug("FileWriter {} split threshold {} mode {}", this, splitThreshold, splitMode);
    if (rangeReadFilter) {
      this.mapIdBitMap = new RoaringBitmap();
    }
    takeBuffer();
  }

  public FileInfo getFileInfo() {
    return fileInfo;
  }

  public File getFile() {
    return fileInfo.getFile();
  }

  public void incrementPendingWrites() {
    numPendingWrites.incrementAndGet();
  }

  public void decrementPendingWrites() {
    numPendingWrites.decrementAndGet();
  }

  @GuardedBy("flushLock")
  protected void flush(boolean finalFlush) throws IOException {
    // flushBuffer == null here means writer already closed
    if (flushBuffer != null) {
      int numBytes = flushBuffer.readableBytes();
      if (numBytes != 0) {
        notifier.checkException();
        notifier.numPendingFlushes.incrementAndGet();
        FlushTask task = null;
        if (channel != null) {
          task = new LocalFlushTask(flushBuffer, channel, notifier);
        } else if (fileInfo.isHdfs()) {
          task = new HdfsFlushTask(flushBuffer, fileInfo.getHdfsPath(), notifier);
        }
        addTask(task);
        flushBuffer = null;
        fileInfo.updateBytesFlushed(numBytes);
        if (!finalFlush) {
          takeBuffer();
        }
      }
    }
  }

  /** assume data size is less than chunk capacity */
  public void write(ByteBuf data) throws IOException {
    if (closed) {
      String msg = "FileWriter has already closed!, fileName " + fileInfo.getFilePath();
      logger.warn(msg);
      throw new AlreadyClosedException(msg);
    }

    if (notifier.hasException()) {
      return;
    }

    int mapId = 0;
    if (rangeReadFilter) {
      byte[] header = new byte[4];
      data.markReaderIndex();
      data.readBytes(header);
      data.resetReaderIndex();
      mapId = Platform.getInt(header, Platform.BYTE_ARRAY_OFFSET);
    }

    final int numBytes = data.readableBytes();
    MemoryManager.instance().incrementDiskBuffer(numBytes);

    Optional.ofNullable(CongestionController.instance())
        .ifPresent(
            congestionController ->
                congestionController.produceBytes(fileInfo.getUserIdentifier(), numBytes));

    synchronized (flushLock) {
      if (closed) {
        String msg = "FileWriter has already closed!, fileName " + fileInfo.getFilePath();
        logger.warn(msg);
        throw new AlreadyClosedException(msg);
      }
      if (rangeReadFilter) {
        mapIdBitMap.add(mapId);
      }
      if (flushBuffer.readableBytes() != 0
          && flushBuffer.readableBytes() + numBytes >= flusherBufferSize) {
        flush(false);
      }

      data.retain();
      flushBuffer.addComponent(true, data);
    }

    numPendingWrites.decrementAndGet();
  }

  public RoaringBitmap getMapIdBitMap() {
    return mapIdBitMap;
  }

  public StorageInfo getStorageInfo() {
    if (flusher instanceof LocalFlusher) {
      LocalFlusher localFlusher = (LocalFlusher) flusher;
      return new StorageInfo(localFlusher.diskType(), localFlusher.mountPoint(), true);
    } else {
      if (deleted) {
        return null;
      } else {
        return new StorageInfo(StorageInfo.Type.HDFS, true, fileInfo.getFilePath());
      }
    }
  }

  public abstract long close() throws IOException;

  @FunctionalInterface
  public interface RunnableWithIOException {
    void run() throws IOException;
  }

  protected synchronized long close(
      RunnableWithIOException tryClose,
      RunnableWithIOException streamClose,
      RunnableWithIOException finalClose)
      throws IOException {
    if (closed) {
      String msg = "FileWriter has already closed! fileName " + fileInfo.getFilePath();
      logger.error(msg);
      throw new AlreadyClosedException(msg);
    }

    try {
      waitOnNoPending(numPendingWrites);
      closed = true;

      synchronized (flushLock) {
        if (flushBuffer.readableBytes() > 0) {
          flush(true);
        }
      }

      tryClose.run();
      waitOnNoPending(notifier.numPendingFlushes);
    } finally {
      returnBuffer();
      try {
        if (channel != null) {
          channel.close();
        }
        if (fileInfo.isHdfs()) {
          streamClose.run();
        }
      } catch (IOException e) {
        logger.warn("close file writer {} failed", this, e);
      }

      finalClose.run();

      // unregister from DeviceMonitor
      if (!fileInfo.isHdfs()) {
        logger.debug("file info {} register from device monitor", fileInfo);
        deviceMonitor.unregisterFileWriter(this);
      }
    }
    return fileInfo.getFileLength();
  }

  public synchronized void destroy(IOException ioException) {
    if (!closed) {
      closed = true;
      if (!notifier.hasException()) {
        notifier.setException(ioException);
      }
      returnBuffer();
      try {
        if (channel != null) {
          channel.close();
        }
      } catch (IOException e) {
        logger.warn(
            "Close channel failed for file {} caused by {}.",
            fileInfo.getFilePath(),
            e.getMessage());
      }
    }

    if (!destroyed) {
      destroyed = true;
      fileInfo.deleteAllFiles(StorageManager.hadoopFs());

      // unregister from DeviceMonitor
      if (!fileInfo.isHdfs()) {
        deviceMonitor.unregisterFileWriter(this);
      }
    }
  }

  public IOException getException() {
    if (notifier.hasException()) {
      return notifier.exception.get();
    } else {
      return null;
    }
  }

  protected void waitOnNoPending(AtomicInteger counter) throws IOException {
    long waitTime = writerCloseTimeoutMs;
    while (counter.get() > 0 && waitTime > 0) {
      try {
        notifier.checkException();
        TimeUnit.MILLISECONDS.sleep(WAIT_INTERVAL_MS);
      } catch (InterruptedException e) {
        IOException ioe = new IOException(e);
        notifier.setException(ioe);
        throw ioe;
      }
      waitTime -= WAIT_INTERVAL_MS;
    }
    if (counter.get() > 0) {
      IOException ioe = new IOException("Wait pending actions timeout.");
      notifier.setException(ioe);
      throw ioe;
    }
    notifier.checkException();
  }

  protected void takeBuffer() {
    // metrics start
    String metricsName = null;
    String fileAbsPath = null;
    if (source.metricsCollectCriticalEnabled()) {
      metricsName = WorkerSource.TAKE_BUFFER_TIME();
      fileAbsPath = fileInfo.getFilePath();
      source.startTimer(metricsName, fileAbsPath);
    }

    // real action
    flushBuffer = flusher.takeBuffer();

    // metrics end
    if (source.metricsCollectCriticalEnabled()) {
      source.stopTimer(metricsName, fileAbsPath);
    }
  }

  protected void addTask(FlushTask task) throws IOException {
    if (!flusher.addTask(task, writerCloseTimeoutMs, flushWorkerIndex)) {
      IOException e = new IOException("Add flush task timeout.");
      notifier.setException(e);
      throw e;
    }
  }

  protected void returnBuffer() {
    synchronized (flushLock) {
      if (flushBuffer != null) {
        flusher.returnBuffer(flushBuffer);
        flushBuffer = null;
      }
    }
  }

  public int hashCode() {
    return fileInfo.getFilePath().hashCode();
  }

  public boolean equals(Object obj) {
    return (obj instanceof FileWriter)
        && fileInfo.getFilePath().equals(((FileWriter) obj).fileInfo.getFilePath());
  }

  public String toString() {
    return fileInfo.getFilePath();
  }

  public void flushOnMemoryPressure() throws IOException {
    synchronized (flushLock) {
      flush(false);
    }
  }

  public long getSplitThreshold() {
    return splitThreshold;
  }

  public PartitionSplitMode getSplitMode() {
    return splitMode;
  }

  @Override
  public void notifyError(String mountPoint, DiskStatus diskStatus) {
    destroy(
        new IOException(
            "Destroy FileWriter "
                + this
                + " by device ERROR."
                + " Disk: "
                + mountPoint
                + " Status: "
                + diskStatus));
  }

  // These empty methods are intended to match scala 2.11 restrictions that
  // trait can not be used as an interface with default implementation.
  @Override
  public void notifyHealthy(String mountPoint) {}

  @Override
  public void notifyHighDiskUsage(String mountPoint) {}

  @Override
  public void notifyNonCriticalError(String mountPoint, DiskStatus diskStatus) {}

  public PartitionType getPartitionType() {
    return partitionType;
  }
}
