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
package org.apache.drill.exec.work.batch;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.DrillBuf;

import java.io.EOFException;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.proto.BitData;
import org.apache.drill.exec.proto.ExecProtos;
import org.apache.drill.exec.proto.helper.QueryIdHelper;
import org.apache.drill.exec.record.RawFragmentBatch;
import org.apache.drill.exec.store.LocalSyncableFileSystem;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import org.apache.drill.shaded.guava.com.google.common.base.Joiner;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.base.Stopwatch;
import org.apache.drill.shaded.guava.com.google.common.collect.Queues;

/**
 * This implementation of RawBatchBuffer starts writing incoming batches to disk once the buffer size reaches a threshold.
 * The order of the incoming buffers is maintained.
 */
public class SpoolingRawBatchBuffer extends BaseRawBatchBuffer<SpoolingRawBatchBuffer.RawFragmentBatchWrapper> {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SpoolingRawBatchBuffer.class);

  private static String DRILL_LOCAL_IMPL_STRING = "fs.drill-local.impl";
  private static final float STOP_SPOOLING_FRACTION = (float) 0.5;
  public static final long ALLOCATOR_INITIAL_RESERVATION = 1*1024*1024;
  public static final long ALLOCATOR_MAX_RESERVATION = 20L*1000*1000*1000;
  private static final int SPOOLING_SENDER_CREDIT = 20;

  private enum SpoolingState {
    NOT_SPOOLING,
    SPOOLING,
    PAUSE_SPOOLING,
    STOP_SPOOLING
  }

  private final BufferAllocator allocator;
  private final long threshold;
  private final int oppositeId;
  private final int bufferIndex;

  private volatile SpoolingState spoolingState;
  private volatile long currentSizeInMemory = 0;
  private volatile Spooler spooler;

  private FileSystem fs;
  private Path path;
  private FSDataOutputStream outputStream;

  public SpoolingRawBatchBuffer(FragmentContext context, int fragmentCount, int oppositeId, int bufferIndex, boolean enableDynamicFC) {
    super(context, fragmentCount, enableDynamicFC);
    this.allocator = context.getNewChildAllocator(
        "SpoolingRawBatchBufer", 100, ALLOCATOR_INITIAL_RESERVATION, ALLOCATOR_MAX_RESERVATION);
    this.threshold = context.getConfig().getLong(ExecConstants.SPOOLING_BUFFER_MEMORY);
    this.oppositeId = oppositeId;
    this.bufferIndex = bufferIndex;
    this.bufferQueue = new SpoolingBufferQueue();
  }

  private class SpoolingBufferQueue implements BufferQueue<RawFragmentBatchWrapper> {

    private final LinkedBlockingDeque<RawFragmentBatchWrapper> buffer = Queues.newLinkedBlockingDeque();

    @Override
    public void addOomBatch(RawFragmentBatch batch) {
      RawFragmentBatchWrapper batchWrapper = new RawFragmentBatchWrapper(batch, true);
      batchWrapper.setOutOfMemory(true);
      buffer.addFirst(batchWrapper);
    }

    @Override
    public RawFragmentBatch poll() throws IOException, InterruptedException {
      RawFragmentBatchWrapper batchWrapper = buffer.poll();
      if (batchWrapper != null) {
        return batchWrapper.get();
      }
      return null;
    }

    @Override
    public RawFragmentBatch take() throws IOException, InterruptedException {
      return buffer.take().get();
    }

    @Override
    public RawFragmentBatch poll(long timeout, TimeUnit timeUnit) throws InterruptedException, IOException {
      RawFragmentBatchWrapper batchWrapper = buffer.poll(timeout, timeUnit);
      if (batchWrapper != null) {
        return batchWrapper.get();
      }
      return null;
    }

    @Override
    public boolean checkForOutOfMemory() {
      return buffer.peek().isOutOfMemory();
    }

    @Override
    public int size() {
      return buffer.size();
    }

    @Override
    public boolean isEmpty() {
      return buffer.size() == 0;
    }

    public void add(RawFragmentBatchWrapper batchWrapper) {
      buffer.add(batchWrapper);
    }
  }

  private synchronized void setSpoolingState(SpoolingState newState) {
    SpoolingState currentState = spoolingState;
    if (newState == SpoolingState.NOT_SPOOLING ||
        currentState == SpoolingState.STOP_SPOOLING) {
      return;
    }
    spoolingState = newState;
  }

  private boolean isCurrentlySpooling() {
    return spoolingState == SpoolingState.SPOOLING;
  }

  private void startSpooling() {
    setSpoolingState(SpoolingState.SPOOLING);
  }

  private void pauseSpooling() {
    setSpoolingState(SpoolingState.PAUSE_SPOOLING);
  }

  private boolean isSpoolingStopped() {
    return spoolingState == SpoolingState.STOP_SPOOLING;
  }

  private void stopSpooling() {
    setSpoolingState(SpoolingState.STOP_SPOOLING);
  }

  public String getDir() {
    List<String> dirs = context.getConfig().getStringList(ExecConstants.TEMP_DIRECTORIES);
    return dirs.get(ThreadLocalRandom.current().nextInt(dirs.size()));
  }

  private synchronized void initSpooler() throws IOException {
    if (spooler != null) {
      return;
    }

    Configuration conf = new Configuration();
    conf.set(FileSystem.FS_DEFAULT_NAME_KEY, context.getConfig().getString(ExecConstants.TEMP_FILESYSTEM));
    conf.set(DRILL_LOCAL_IMPL_STRING, LocalSyncableFileSystem.class.getName());
    fs = FileSystem.get(conf);
    path = getPath();
    outputStream = fs.create(path);
    final String spoolingThreadName = QueryIdHelper.getExecutorThreadName(context.getHandle()).concat(
        ":Spooler-" + oppositeId + "-" + bufferIndex);
    spooler = new Spooler(spoolingThreadName);
    spooler.start();
  }

  @Override
  protected void enqueueInner(RawFragmentBatch batch) throws IOException {
    assert batch.getHeader().getSendingMajorFragmentId() == oppositeId;

    logger.debug("Enqueue batch. Current buffer size: {}. Last batch: {}. Sending fragment: {}", bufferQueue.size(), batch.getHeader().getIsLastBatch(), batch.getHeader().getSendingMajorFragmentId());
    RawFragmentBatchWrapper wrapper;

    boolean spoolCurrentBatch = isCurrentlySpooling();
    wrapper = new RawFragmentBatchWrapper(batch, !spoolCurrentBatch);
    currentSizeInMemory += wrapper.getBodySize();
    if (spoolCurrentBatch) {
      if (spooler == null) {
        initSpooler();
      }
      spooler.addBatchForSpooling(wrapper);
    }
    bufferQueue.add(wrapper);
    if (!spoolCurrentBatch && currentSizeInMemory > threshold) {
      logger.debug("Buffer size {} greater than threshold {}. Start spooling to disk", currentSizeInMemory, threshold);
      startSpooling();
    }
  }

  @Override
  public void kill(FragmentContext context) {
    allocator.close();
    if (spooler != null) {
      spooler.terminate();
    }
  }

  @Override
  protected void upkeep(RawFragmentBatch batch) {
    if (context.getAllocator().isOverLimit()) {
      outOfMemory.set(true);
    }

    DrillBuf body = batch.getBody();
    if (body != null) {
      currentSizeInMemory -= body.capacity();
    }
    if (isCurrentlySpooling() && currentSizeInMemory < threshold * STOP_SPOOLING_FRACTION) {
      logger.debug("buffer size {} less than {}x threshold. Stop spooling.", currentSizeInMemory, STOP_SPOOLING_FRACTION);
      pauseSpooling();
    }
    logger.debug("Got batch. Current buffer size: {}", bufferQueue.size());
  }

  @Override
  public void close() {
    if (spooler != null) {
      spooler.terminate();
      while (spooler.isAlive()) {
        try {
          spooler.join();
        } catch (InterruptedException e) {
          logger.warn("Interrupted while waiting for spooling thread to exit");
          continue;
        }
      }
    }
    allocator.close();
    try {
      if (outputStream != null) {
        outputStream.close();
      }
    } catch (IOException e) {
      logger.warn("Failed to cleanup I/O streams", e);
    }
    if (context.getConfig().getBoolean(ExecConstants.SPOOLING_BUFFER_DELETE)) {
      try {
        if (fs != null) {
          fs.delete(path, false);
          logger.debug("Deleted file {}", path.toString());
        }
      } catch (IOException e) {
        logger.warn("Failed to delete temporary files", e);
      }
    }
    super.close();
  }

  private class Spooler extends Thread {

    private final LinkedBlockingDeque<RawFragmentBatchWrapper> spoolingQueue;
    private volatile boolean shouldContinue = true;
    private Thread spoolingThread;

    public Spooler(String name) {
      setDaemon(true);
      setName(name);
      spoolingQueue = Queues.newLinkedBlockingDeque();
    }

    public void run() {
      try {
        while (shouldContinue) {
          RawFragmentBatchWrapper batch;
          try {
            batch = spoolingQueue.take();
          } catch (InterruptedException e) {
            if (shouldContinue) {
              continue;
            } else {
              break;
            }
          }
          try {
            batch.writeToStream(outputStream);
          } catch (IOException e) {
            context.getExecutorState().fail(e);
          }
        }
      } catch (Throwable e) {
        context.getExecutorState().fail(e);
      } finally {
        logger.info("Spooler thread exiting");
      }
    }

    public void addBatchForSpooling(RawFragmentBatchWrapper batchWrapper) {
      if (isSpoolingStopped()) {
        spoolingQueue.add(batchWrapper);
      } else {
        // will not spill this batch
        batchWrapper.available = true;
        batchWrapper.batch.sendOk();
        batchWrapper.latch.countDown();
      }
    }

    public void terminate() {
      stopSpooling();
      shouldContinue = false;
      if (spoolingThread.isAlive()) {
        spoolingThread.interrupt();
      }
    }
  }

  class RawFragmentBatchWrapper {
    private RawFragmentBatch batch;
    private volatile boolean available;
    private CountDownLatch latch;
    private volatile int bodyLength;
    private volatile boolean outOfMemory = false;
    private long start = -1;
    private long check;

    public RawFragmentBatchWrapper(RawFragmentBatch batch, boolean available) {
      Preconditions.checkNotNull(batch);
      this.batch = batch;
      this.available = available;
      this.latch = new CountDownLatch(available ? 0 : 1);
      if (available) {
        //As we can flush to disc ,we could let the sender to send the batch more rapidly
        if (enableDynamicFC) {
          batch.sendOk(SPOOLING_SENDER_CREDIT);
        } else {
          batch.sendOk();
        }
      }
    }

    public boolean isNull() {
      return batch == null;
    }

    public RawFragmentBatch get() throws InterruptedException, IOException {
      if (available) {
        assert batch.getHeader() != null : "batch header null";
        return batch;
      } else {
        latch.await();
        readFromStream();
        available = true;
        return batch;
      }
    }

    public long getBodySize() {
      if (batch.getBody() == null) {
        return 0;
      }
      assert batch.getBody().capacity() >= 0;
      return batch.getBody().capacity();
    }

    public void writeToStream(FSDataOutputStream stream) throws IOException {
      Stopwatch watch = Stopwatch.createStarted();
      available = false;
      check = ThreadLocalRandom.current().nextLong();
      start = stream.getPos();
      logger.debug("Writing check value {} at position {}", check, start);
      stream.writeLong(check);
      batch.getHeader().writeDelimitedTo(stream);
      ByteBuf buf = batch.getBody();
      if (buf != null) {
        bodyLength = buf.capacity();
      } else {
        bodyLength = 0;
      }
      if (bodyLength > 0) {
        buf.getBytes(0, stream, bodyLength);
      }
      stream.hsync();
      FileStatus status = fs.getFileStatus(path);
      long len = status.getLen();
      logger.debug("After spooling batch, stream at position {}. File length {}", stream.getPos(), len);
      batch.sendOk();
      latch.countDown();
      long t = watch.elapsed(TimeUnit.MICROSECONDS);
      logger.debug("Took {} us to spool {} to disk. Rate {} mb/s", t, bodyLength, bodyLength / t);
      if (buf != null) {
        buf.release();
      }
    }

    public void readFromStream() throws IOException, InterruptedException {
      long pos = start;
      boolean tryAgain = true;
      int duration = 0;

      while (tryAgain) {

        // Sometimes, the file isn't quite done writing when we attempt to read it. As such, we need to wait and retry.
        Thread.sleep(duration);

        try(final FSDataInputStream stream = fs.open(path);
            final DrillBuf buf = allocator.buffer(bodyLength)) {
          stream.seek(start);
          final long currentPos = stream.getPos();
          final long check = stream.readLong();
          pos = stream.getPos();
          assert check == this.check : String.format("Check values don't match: %d %d, Position %d", this.check, check, currentPos);
          Stopwatch watch = Stopwatch.createStarted();
          BitData.FragmentRecordBatch header = BitData.FragmentRecordBatch.parseDelimitedFrom(stream);
          pos = stream.getPos();
          assert header != null : "header null after parsing from stream";
          buf.writeBytes(stream, bodyLength);
          pos = stream.getPos();
          batch = new RawFragmentBatch(header, buf, null);
          available = true;
          latch.countDown();
          long t = watch.elapsed(TimeUnit.MICROSECONDS);
          logger.debug("Took {} us to read {} from disk. Rate {} mb/s", t, bodyLength, bodyLength / t);
          tryAgain = false;
        } catch (EOFException e) {
          FileStatus status = fs.getFileStatus(path);
          logger.warn("EOF reading from file {} at pos {}. Current file size: {}", path, pos, status.getLen());
          duration = Math.max(1, duration * 2);
          if (duration < 60000) {
            continue;
          } else {
            throw e;
          }
        } finally {
          if (tryAgain) {
            // we had a premature exit, release batch memory so we don't leak it.
            if (batch != null) {
              batch.getBody().release();
            }
          }
        }
      }
    }

    private boolean isOutOfMemory() {
      return outOfMemory;
    }

    private void setOutOfMemory(boolean outOfMemory) {
      this.outOfMemory = outOfMemory;
    }
  }

  private Path getPath() {
    ExecProtos.FragmentHandle handle = context.getHandle();

    String qid = QueryIdHelper.getQueryId(handle.getQueryId());

    int majorFragmentId = handle.getMajorFragmentId();
    int minorFragmentId = handle.getMinorFragmentId();

    String fileName = Joiner.on(Path.SEPARATOR).join(getDir(), qid, majorFragmentId, minorFragmentId, oppositeId, bufferIndex);

    return new Path(fileName);
  }
}
