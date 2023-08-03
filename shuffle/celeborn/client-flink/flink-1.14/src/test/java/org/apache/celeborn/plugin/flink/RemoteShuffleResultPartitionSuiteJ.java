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

package org.apache.celeborn.plugin.flink;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;

import org.apache.flink.api.common.JobID;
import org.apache.flink.core.memory.MemorySegment;
import org.apache.flink.core.memory.MemorySegmentFactory;
import org.apache.flink.runtime.io.network.api.EndOfPartitionEvent;
import org.apache.flink.runtime.io.network.api.serialization.EventSerializer;
import org.apache.flink.runtime.io.network.buffer.Buffer;
import org.apache.flink.runtime.io.network.buffer.BufferCompressor;
import org.apache.flink.runtime.io.network.buffer.BufferDecompressor;
import org.apache.flink.runtime.io.network.buffer.BufferPool;
import org.apache.flink.runtime.io.network.buffer.NetworkBuffer;
import org.apache.flink.runtime.io.network.buffer.NetworkBufferPool;
import org.apache.flink.runtime.io.network.partition.ResultPartitionID;
import org.apache.flink.runtime.io.network.partition.ResultPartitionManager;
import org.apache.flink.runtime.io.network.partition.ResultPartitionType;
import org.apache.flink.util.function.SupplierWithException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.apache.celeborn.common.CelebornConf;
import org.apache.celeborn.plugin.flink.buffer.BufferPacker;
import org.apache.celeborn.plugin.flink.buffer.SortBuffer;
import org.apache.celeborn.plugin.flink.readclient.FlinkShuffleClientImpl;
import org.apache.celeborn.plugin.flink.utils.BufferUtils;

public class RemoteShuffleResultPartitionSuiteJ {
  private final int networkBufferSize = 32 * 1024;
  private BufferCompressor bufferCompressor = new BufferCompressor(networkBufferSize, "lz4");
  private RemoteShuffleOutputGate remoteShuffleOutputGate = mock(RemoteShuffleOutputGate.class);
  private final String compressCodec = "LZ4";
  private final CelebornConf conf = new CelebornConf();
  BufferDecompressor bufferDecompressor = new BufferDecompressor(networkBufferSize, "LZ4");

  private static final int totalBuffers = 1000;

  private static final int bufferSize = 1024;

  private NetworkBufferPool globalBufferPool;

  private BufferPool sortBufferPool;

  private BufferPool nettyBufferPool;

  private RemoteShuffleResultPartition partitionWriter;

  private FakedRemoteShuffleOutputGate outputGate;

  @Before
  public void setup() {
    globalBufferPool = new NetworkBufferPool(totalBuffers, bufferSize);
  }

  @After
  public void tearDown() throws Exception {
    if (outputGate != null) {
      outputGate.release();
    }

    if (sortBufferPool != null) {
      sortBufferPool.lazyDestroy();
    }
    if (nettyBufferPool != null) {
      nettyBufferPool.lazyDestroy();
    }
    assertEquals(totalBuffers, globalBufferPool.getNumberOfAvailableMemorySegments());
    globalBufferPool.destroy();
  }

  @Test
  public void tesSimpleFlush() throws IOException, InterruptedException {
    List<SupplierWithException<BufferPool, IOException>> bufferPool = createBufferPoolFactory();
    RemoteShuffleResultPartition remoteShuffleResultPartition =
        new RemoteShuffleResultPartition(
            "test",
            0,
            new ResultPartitionID(),
            ResultPartitionType.BLOCKING,
            2,
            2,
            32 * 1024,
            new ResultPartitionManager(),
            bufferCompressor,
            bufferPool.get(0),
            remoteShuffleOutputGate);
    remoteShuffleResultPartition.setup();
    doNothing().when(remoteShuffleOutputGate).regionStart(anyBoolean());
    doNothing().when(remoteShuffleOutputGate).regionFinish();
    when(remoteShuffleOutputGate.getBufferPool()).thenReturn(bufferPool.get(1).get());
    SortBuffer sortBuffer = remoteShuffleResultPartition.getDelegation().getUnicastSortBuffer();
    ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[] {1, 2, 3});
    sortBuffer.append(byteBuffer, 0, Buffer.DataType.DATA_BUFFER);
    remoteShuffleResultPartition.getDelegation().flushSortBuffer(sortBuffer, true);
  }

  private List<SupplierWithException<BufferPool, IOException>> createBufferPoolFactory() {
    NetworkBufferPool networkBufferPool =
        new NetworkBufferPool(256 * 8, 32 * 1024, Duration.ofMillis(1000));

    int numBuffersPerPartition = 64 * 1024 / 32;
    int numForResultPartition = numBuffersPerPartition * 7 / 8;
    int numForOutputGate = numBuffersPerPartition - numForResultPartition;

    List<SupplierWithException<BufferPool, IOException>> factories = new ArrayList<>();
    factories.add(
        () -> networkBufferPool.createBufferPool(numForResultPartition, numForResultPartition));
    factories.add(() -> networkBufferPool.createBufferPool(numForOutputGate, numForOutputGate));
    return factories;
  }

  @Test
  public void testWriteNormalRecordWithCompressionEnabled() throws Exception {
    testWriteNormalRecord(true);
  }

  @Test
  public void testWriteNormalRecordWithCompressionDisabled() throws Exception {
    testWriteNormalRecord(false);
  }

  @Test
  public void testWriteLargeRecord() throws Exception {
    int numSubpartitions = 2;
    int numBuffers = 100;
    initResultPartitionWriter(numSubpartitions, 10, 200, false, conf, 10);

    partitionWriter.setup();

    byte[] dataWritten = new byte[bufferSize * numBuffers];
    Random random = new Random();
    random.nextBytes(dataWritten);
    ByteBuffer recordWritten = ByteBuffer.wrap(dataWritten);
    partitionWriter.emitRecord(recordWritten, 0);
    assertEquals(0, sortBufferPool.bestEffortGetNumOfUsedBuffers());

    partitionWriter.finish();
    partitionWriter.close();

    List<Buffer> receivedBuffers = outputGate.getReceivedBuffers()[0];

    ByteBuffer recordRead = ByteBuffer.allocate(bufferSize * numBuffers);
    for (Buffer buffer : receivedBuffers) {
      if (buffer.isBuffer()) {
        recordRead.put(
            buffer.getNioBuffer(
                BufferUtils.HEADER_LENGTH, buffer.readableBytes() - BufferUtils.HEADER_LENGTH));
      }
    }
    recordWritten.rewind();
    recordRead.flip();
    assertEquals(recordWritten, recordRead);
  }

  @Test
  public void testBroadcastLargeRecord() throws Exception {
    int numSubpartitions = 2;
    int numBuffers = 100;
    initResultPartitionWriter(numSubpartitions, 10, 200, false, conf, 10);

    partitionWriter.setup();

    byte[] dataWritten = new byte[bufferSize * numBuffers];
    Random random = new Random();
    random.nextBytes(dataWritten);
    ByteBuffer recordWritten = ByteBuffer.wrap(dataWritten);
    partitionWriter.broadcastRecord(recordWritten);
    assertEquals(0, sortBufferPool.bestEffortGetNumOfUsedBuffers());

    partitionWriter.finish();
    partitionWriter.close();

    ByteBuffer recordRead0 = ByteBuffer.allocate(bufferSize * numBuffers);
    for (Buffer buffer : outputGate.getReceivedBuffers()[0]) {
      if (buffer.isBuffer()) {
        recordRead0.put(
            buffer.getNioBuffer(
                BufferUtils.HEADER_LENGTH, buffer.readableBytes() - BufferUtils.HEADER_LENGTH));
      }
    }
    recordWritten.rewind();
    recordRead0.flip();
    assertEquals(recordWritten, recordRead0);

    ByteBuffer recordRead1 = ByteBuffer.allocate(bufferSize * numBuffers);
    for (Buffer buffer : outputGate.getReceivedBuffers()[1]) {
      if (buffer.isBuffer()) {
        recordRead1.put(
            buffer.getNioBuffer(
                BufferUtils.HEADER_LENGTH, buffer.readableBytes() - BufferUtils.HEADER_LENGTH));
      }
    }
    recordWritten.rewind();
    recordRead1.flip();
    assertEquals(recordWritten, recordRead0);
  }

  @Test
  public void testFlush() throws Exception {
    int numSubpartitions = 10;

    initResultPartitionWriter(numSubpartitions, 10, 20, false, conf, 10);
    partitionWriter.setup();

    partitionWriter.emitRecord(ByteBuffer.allocate(bufferSize), 0);
    partitionWriter.emitRecord(ByteBuffer.allocate(bufferSize), 1);
    assertEquals(3, sortBufferPool.bestEffortGetNumOfUsedBuffers());

    partitionWriter.broadcastRecord(ByteBuffer.allocate(bufferSize));
    assertEquals(2, sortBufferPool.bestEffortGetNumOfUsedBuffers());

    partitionWriter.flush(0);
    assertEquals(0, sortBufferPool.bestEffortGetNumOfUsedBuffers());

    partitionWriter.emitRecord(ByteBuffer.allocate(bufferSize), 2);
    partitionWriter.emitRecord(ByteBuffer.allocate(bufferSize), 3);
    assertEquals(3, sortBufferPool.bestEffortGetNumOfUsedBuffers());

    partitionWriter.flushAll();
    assertEquals(0, sortBufferPool.bestEffortGetNumOfUsedBuffers());

    partitionWriter.finish();
    partitionWriter.close();
  }

  private void testWriteNormalRecord(boolean compressionEnabled) throws Exception {
    int numSubpartitions = 4;
    int numRecords = 100;
    Random random = new Random();

    initResultPartitionWriter(numSubpartitions, 100, 500, compressionEnabled, conf, 10);
    partitionWriter.setup();
    assertTrue(outputGate.isSetup());

    Queue<DataAndType>[] dataWritten = new Queue[numSubpartitions];
    IntStream.range(0, numSubpartitions).forEach(i -> dataWritten[i] = new ArrayDeque<>());
    int[] numBytesWritten = new int[numSubpartitions];
    Arrays.fill(numBytesWritten, 0);

    for (int i = 0; i < numRecords; i++) {
      byte[] data = new byte[random.nextInt(2 * bufferSize) + 1];
      if (compressionEnabled) {
        byte randomByte = (byte) random.nextInt();
        Arrays.fill(data, randomByte);
      } else {
        random.nextBytes(data);
      }
      ByteBuffer record = ByteBuffer.wrap(data);
      boolean isBroadCast = random.nextBoolean();

      if (isBroadCast) {
        partitionWriter.broadcastRecord(record);
        IntStream.range(0, numSubpartitions)
            .forEach(
                subpartition ->
                    recordDataWritten(
                        record,
                        Buffer.DataType.DATA_BUFFER,
                        subpartition,
                        dataWritten,
                        numBytesWritten));
      } else {
        int subpartition = random.nextInt(numSubpartitions);
        partitionWriter.emitRecord(record, subpartition);
        recordDataWritten(
            record, Buffer.DataType.DATA_BUFFER, subpartition, dataWritten, numBytesWritten);
      }
    }

    partitionWriter.finish();
    assertTrue(outputGate.isFinished());
    partitionWriter.close();
    assertTrue(outputGate.isClosed());

    for (int subpartition = 0; subpartition < numSubpartitions; ++subpartition) {
      ByteBuffer record = EventSerializer.toSerializedEvent(EndOfPartitionEvent.INSTANCE);
      recordDataWritten(
          record, Buffer.DataType.EVENT_BUFFER, subpartition, dataWritten, numBytesWritten);
    }

    outputGate
        .getFinishedRegions()
        .forEach(
            regionIndex -> assertTrue(outputGate.getNumBuffersByRegion().containsKey(regionIndex)));

    int[] numBytesRead = new int[numSubpartitions];
    List<Buffer>[] receivedBuffers = outputGate.getReceivedBuffers();
    List<Buffer>[] validateTarget = new List[numSubpartitions];
    Arrays.fill(numBytesRead, 0);
    for (int i = 0; i < numSubpartitions; i++) {
      validateTarget[i] = new ArrayList<>();
      for (Buffer buffer : receivedBuffers[i]) {
        for (Buffer unpackedBuffer : BufferPacker.unpack(buffer.asByteBuf())) {
          if (compressionEnabled && unpackedBuffer.isCompressed()) {
            Buffer decompressedBuffer =
                bufferDecompressor.decompressToIntermediateBuffer(unpackedBuffer);
            ByteBuffer decompressed = decompressedBuffer.getNioBufferReadable();
            int numBytes = decompressed.remaining();
            MemorySegment segment = MemorySegmentFactory.allocateUnpooledSegment(numBytes);
            segment.put(0, decompressed, numBytes);
            decompressedBuffer.recycleBuffer();
            validateTarget[i].add(
                new NetworkBuffer(segment, buf -> {}, unpackedBuffer.getDataType(), numBytes));
            numBytesRead[i] += numBytes;
          } else {
            numBytesRead[i] += buffer.readableBytes();
            validateTarget[i].add(buffer);
          }
        }
      }
    }
    IntStream.range(0, numSubpartitions).forEach(subpartitions -> {});
    checkWriteReadResult(
        numSubpartitions, numBytesWritten, numBytesWritten, dataWritten, validateTarget);
  }

  private void initResultPartitionWriter(
      int numSubpartitions,
      int sortBufferPoolSize,
      int nettyBufferPoolSize,
      boolean compressionEnabled,
      CelebornConf conf,
      int numMappers)
      throws Exception {

    sortBufferPool = globalBufferPool.createBufferPool(sortBufferPoolSize, sortBufferPoolSize);
    nettyBufferPool = globalBufferPool.createBufferPool(nettyBufferPoolSize, nettyBufferPoolSize);

    outputGate =
        new FakedRemoteShuffleOutputGate(
            getShuffleDescriptor(), numSubpartitions, () -> nettyBufferPool, conf, numMappers);
    outputGate.setup();

    if (compressionEnabled) {
      partitionWriter =
          new RemoteShuffleResultPartition(
              "RemoteShuffleResultPartitionWriterTest",
              0,
              new ResultPartitionID(),
              ResultPartitionType.BLOCKING,
              numSubpartitions,
              numSubpartitions,
              bufferSize,
              new ResultPartitionManager(),
              bufferCompressor,
              () -> sortBufferPool,
              outputGate);
    } else {
      partitionWriter =
          new RemoteShuffleResultPartition(
              "RemoteShuffleResultPartitionWriterTest",
              0,
              new ResultPartitionID(),
              ResultPartitionType.BLOCKING,
              numSubpartitions,
              numSubpartitions,
              bufferSize,
              new ResultPartitionManager(),
              null,
              () -> sortBufferPool,
              outputGate);
    }
  }

  private void recordDataWritten(
      ByteBuffer record,
      Buffer.DataType dataType,
      int subpartition,
      Queue<DataAndType>[] dataWritten,
      int[] numBytesWritten) {

    record.rewind();
    dataWritten[subpartition].add(new DataAndType(record, dataType));
    numBytesWritten[subpartition] += record.remaining();
  }

  private static class FakedRemoteShuffleOutputGate extends RemoteShuffleOutputGate {

    private boolean isSetup;
    private boolean isFinished;
    private boolean isClosed;
    private final List<Buffer>[] receivedBuffers;
    private final Map<Integer, Integer> numBuffersByRegion;
    private final Set<Integer> finishedRegions;
    private int currentRegionIndex;
    private boolean currentIsBroadcast;

    FakedRemoteShuffleOutputGate(
        RemoteShuffleDescriptor shuffleDescriptor,
        int numSubpartitions,
        SupplierWithException<BufferPool, IOException> bufferPoolFactory,
        CelebornConf celebornConf,
        int numMappers) {

      super(
          shuffleDescriptor,
          numSubpartitions,
          bufferSize,
          bufferPoolFactory,
          celebornConf,
          numMappers);
      isSetup = false;
      isFinished = false;
      isClosed = false;
      numBuffersByRegion = new HashMap<>();
      finishedRegions = new HashSet<>();
      currentRegionIndex = -1;
      receivedBuffers = new ArrayList[numSubpartitions];
      IntStream.range(0, numSubpartitions).forEach(i -> receivedBuffers[i] = new ArrayList<>());
      currentIsBroadcast = false;
    }

    @Override
    FlinkShuffleClientImpl getShuffleClient() {
      FlinkShuffleClientImpl client = mock(FlinkShuffleClientImpl.class);
      doNothing().when(client).cleanup(anyInt(), anyInt(), anyInt());
      return client;
    }

    @Override
    public void setup() throws IOException, InterruptedException {
      bufferPool = bufferPoolFactory.get();
      isSetup = true;
    }

    @Override
    public void write(Buffer buffer, int subIdx) {
      if (currentIsBroadcast) {
        assertEquals(0, subIdx);
        ByteBuffer byteBuffer = buffer.getNioBufferReadable();
        for (int i = 0; i < numSubs; i++) {
          int numBytes = buffer.readableBytes();
          MemorySegment segment = MemorySegmentFactory.allocateUnpooledSegment(numBytes);
          byteBuffer.rewind();
          segment.put(0, byteBuffer, numBytes);
          receivedBuffers[i].add(
              new NetworkBuffer(
                  segment, buf -> {}, buffer.getDataType(), buffer.isCompressed(), numBytes));
        }
        buffer.recycleBuffer();
      } else {
        receivedBuffers[subIdx].add(buffer);
      }
      if (numBuffersByRegion.containsKey(currentRegionIndex)) {
        int prev = numBuffersByRegion.get(currentRegionIndex);
        numBuffersByRegion.put(currentRegionIndex, prev + 1);
      } else {
        numBuffersByRegion.put(currentRegionIndex, 1);
      }
    }

    @Override
    public void regionStart(boolean isBroadcast) {
      currentIsBroadcast = isBroadcast;
      currentRegionIndex++;
    }

    @Override
    public void regionFinish() {
      if (finishedRegions.contains(currentRegionIndex)) {
        throw new IllegalStateException("Unexpected region: " + currentRegionIndex);
      }
      finishedRegions.add(currentRegionIndex);
    }

    @Override
    public void finish() throws InterruptedException {
      isFinished = true;
    }

    @Override
    public void close() {
      isClosed = true;
    }

    public List<Buffer>[] getReceivedBuffers() {
      return receivedBuffers;
    }

    public Map<Integer, Integer> getNumBuffersByRegion() {
      return numBuffersByRegion;
    }

    public Set<Integer> getFinishedRegions() {
      return finishedRegions;
    }

    public boolean isSetup() {
      return isSetup;
    }

    public boolean isFinished() {
      return isFinished;
    }

    public boolean isClosed() {
      return isClosed;
    }

    public void release() throws Exception {
      IntStream.range(0, numSubs)
          .forEach(
              subpartitionIndex -> {
                receivedBuffers[subpartitionIndex].forEach(Buffer::recycleBuffer);
                receivedBuffers[subpartitionIndex].clear();
              });
      numBuffersByRegion.clear();
      finishedRegions.clear();
      super.close();
    }
  }

  private RemoteShuffleDescriptor getShuffleDescriptor() throws Exception {
    Random random = new Random();
    byte[] bytes = new byte[16];
    random.nextBytes(bytes);
    return new RemoteShuffleDescriptor(
        new JobID(bytes).toString(),
        new JobID(bytes),
        new JobID(bytes).toString(),
        new ResultPartitionID(),
        new RemoteShuffleResource(
            "1", 2, System.currentTimeMillis(), new ShuffleResourceDescriptor(1, 1, 1, 0)));
  }

  /** Data written and its {@link Buffer.DataType}. */
  public static class DataAndType {
    private final ByteBuffer data;
    private final Buffer.DataType dataType;

    DataAndType(ByteBuffer data, Buffer.DataType dataType) {
      this.data = data;
      this.dataType = dataType;
    }
  }

  public static void checkWriteReadResult(
      int numSubpartitions,
      int[] numBytesWritten,
      int[] numBytesRead,
      Queue<DataAndType>[] dataWritten,
      Collection<Buffer>[] buffersRead) {
    for (int subpartitionIndex = 0; subpartitionIndex < numSubpartitions; ++subpartitionIndex) {
      assertEquals(numBytesWritten[subpartitionIndex], numBytesRead[subpartitionIndex]);

      List<DataAndType> eventsWritten = new ArrayList<>();
      List<Buffer> eventsRead = new ArrayList<>();

      ByteBuffer subpartitionDataWritten = ByteBuffer.allocate(numBytesWritten[subpartitionIndex]);
      for (DataAndType dataAndType : dataWritten[subpartitionIndex]) {
        subpartitionDataWritten.put(dataAndType.data);
        dataAndType.data.rewind();
        if (dataAndType.dataType.isEvent()) {
          eventsWritten.add(dataAndType);
        }
      }

      ByteBuffer subpartitionDataRead = ByteBuffer.allocate(numBytesRead[subpartitionIndex]);
      for (Buffer buffer : buffersRead[subpartitionIndex]) {
        subpartitionDataRead.put(buffer.getNioBufferReadable());
        if (!buffer.isBuffer()) {
          eventsRead.add(buffer);
        }
      }

      subpartitionDataWritten.flip();
      subpartitionDataRead.flip();
      assertEquals(subpartitionDataWritten, subpartitionDataRead);

      assertEquals(eventsWritten.size(), eventsRead.size());
      for (int i = 0; i < eventsWritten.size(); ++i) {
        assertEquals(eventsWritten.get(i).dataType, eventsRead.get(i).getDataType());
        assertEquals(eventsWritten.get(i).data, eventsRead.get(i).getNioBufferReadable());
      }
    }
  }
}
