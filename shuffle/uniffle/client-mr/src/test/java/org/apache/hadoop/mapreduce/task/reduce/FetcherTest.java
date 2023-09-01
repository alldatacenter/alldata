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

package org.apache.hadoop.mapreduce.task.reduce;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Supplier;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocalDirAllocator;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparator;
import org.apache.hadoop.io.compress.BZip2Codec;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.io.serializer.SerializationFactory;
import org.apache.hadoop.mapred.Counters;
import org.apache.hadoop.mapred.IFile;
import org.apache.hadoop.mapred.InputSplit;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MROutputFiles;
import org.apache.hadoop.mapred.MapOutputFile;
import org.apache.hadoop.mapred.RawKeyValueIterator;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.SortWriteBufferManager;
import org.apache.hadoop.mapred.Task;
import org.apache.hadoop.mapred.TaskStatus;
import org.apache.hadoop.mapreduce.JobID;
import org.apache.hadoop.mapreduce.MRConfig;
import org.apache.hadoop.mapreduce.RssMRUtils;
import org.apache.hadoop.mapreduce.TaskAttemptID;
import org.apache.hadoop.mapreduce.TaskID;
import org.apache.hadoop.mapreduce.TaskType;
import org.apache.hadoop.util.Progress;
import org.junit.jupiter.api.Test;
import org.roaringbitmap.longlong.Roaring64NavigableMap;

import org.apache.uniffle.client.api.ShuffleReadClient;
import org.apache.uniffle.client.api.ShuffleWriteClient;
import org.apache.uniffle.client.response.CompressedShuffleBlock;
import org.apache.uniffle.client.response.SendShuffleDataResult;
import org.apache.uniffle.common.PartitionRange;
import org.apache.uniffle.common.RemoteStorageInfo;
import org.apache.uniffle.common.ShuffleAssignmentsInfo;
import org.apache.uniffle.common.ShuffleBlockInfo;
import org.apache.uniffle.common.ShuffleDataDistributionType;
import org.apache.uniffle.common.ShuffleServerInfo;
import org.apache.uniffle.common.compression.Codec;
import org.apache.uniffle.common.compression.Lz4Codec;
import org.apache.uniffle.common.config.RssConf;
import org.apache.uniffle.common.exception.RssException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FetcherTest {
  static JobID jobId = new JobID("a", 0);
  static TaskAttemptID reduceId1 = new TaskAttemptID(
      new TaskID(jobId, TaskType.REDUCE, 0), 0);
  static Configuration conf = new Configuration();
  static JobConf jobConf = new JobConf();
  static LocalDirAllocator lda = new LocalDirAllocator(MRConfig.LOCAL_DIR);

  static TaskStatus taskStatus = new MockedTaskStatus();
  static ShuffleClientMetrics metrics = new ShuffleClientMetrics(reduceId1, jobConf);
  static Reporter reporter = new MockedReporter();
  static FileSystem fs;
  static List<byte[]> data;
  static MergeManagerImpl<Text, Text> merger;

  static Codec codec = new Lz4Codec();

  @Test
  public void writeAndReadDataTestWithRss() throws Throwable {
    fs = FileSystem.getLocal(conf);
    initRssData();
    merger = new MergeManagerImpl<Text, Text>(
        reduceId1, jobConf, fs, lda, Reporter.NULL, null, null, null, null, null,
        null, null, new Progress(), new MROutputFiles());
    ShuffleReadClient shuffleReadClient = new MockedShuffleReadClient(data);
    RssFetcher fetcher = new RssFetcher(jobConf, reduceId1, taskStatus, merger, new Progress(),
        reporter, metrics, shuffleReadClient, 3, new RssConf());
    fetcher.fetchAllRssBlocks();


    RawKeyValueIterator iterator = merger.close();
    // the final output of merger.close() must be sorted
    List<String> allKeysExpected = Lists.newArrayList("k11", "k22", "k22", "k33", "k44", "k55", "k55");
    List<String> allValuesExpected = Lists.newArrayList("v11", "v22", "v22", "v33", "v44", "v55", "v55");
    List<String> allKeys = Lists.newArrayList();
    List<String> allValues = Lists.newArrayList();
    while (iterator.next()) {
      byte[] key = new byte[iterator.getKey().getLength()];
      byte[] value = new byte[iterator.getValue().getLength()];
      System.arraycopy(iterator.getKey().getData(), 0, key, 0, key.length);
      System.arraycopy(iterator.getValue().getData(), 0, value, 0, value.length);
      allKeys.add(new Text(key).toString().trim());
      allValues.add(new Text(value).toString().trim());
    }
    assertEquals(allKeysExpected, allKeys);
    assertEquals(allValuesExpected, allValues);
  }

  @Test
  public void writeAndReadDataTestWithoutRss() throws Throwable {
    fs = FileSystem.getLocal(conf);
    initLocalData();
    merger = new MergeManagerImpl<Text, Text>(
        reduceId1, jobConf, fs, lda, Reporter.NULL, null, null, null, null, null,
        null, null, new Progress(), new MROutputFiles());
    ShuffleReadClient shuffleReadClient = new MockedShuffleReadClient(data);
    RssFetcher fetcher = new RssFetcher(jobConf, reduceId1, taskStatus, merger, new Progress(),
        reporter, metrics, shuffleReadClient, 3, new RssConf());
    fetcher.fetchAllRssBlocks();


    RawKeyValueIterator iterator = merger.close();
    // the final output of merger.close() must be sorted
    List<String> allKeysExpected = Lists.newArrayList("k11", "k22", "k22", "k33", "k44", "k55", "k55");
    List<String> allValuesExpected = Lists.newArrayList("v11", "v22", "v22", "v33", "v44", "v55", "v55");
    List<String> allKeys = Lists.newArrayList();
    List<String> allValues = Lists.newArrayList();
    while (iterator.next()) {
      byte[] key = new byte[iterator.getKey().getLength()];
      byte[] value = new byte[iterator.getValue().getLength()];
      System.arraycopy(iterator.getKey().getData(), 0, key, 0, key.length);
      System.arraycopy(iterator.getValue().getData(), 0, value, 0, value.length);
      allKeys.add(new Text(key).toString().trim());
      allValues.add(new Text(value).toString().trim());
    }
    assertEquals(allKeysExpected, allKeys);
    assertEquals(allValuesExpected, allValues);
  }

  @Test
  public void writeAndReadDataMergeFailsTestWithRss() throws Throwable {
    fs = FileSystem.getLocal(conf);
    initRssData();
    // The 1-th and 2-th mapout will reserve null
    Set<Integer> expectedFails = Sets.newHashSet(1, 2);
    merger = new MockMergeManagerImpl<Text, Text>(
      reduceId1, jobConf, fs, lda, Reporter.NULL, null, null, null, null, null,
      null, null, new Progress(), new MROutputFiles(), expectedFails);
    ShuffleReadClient shuffleReadClient = new MockedShuffleReadClient(data);
    RssFetcher fetcher = new RssFetcher(jobConf, reduceId1, taskStatus, merger, new Progress(),
        reporter, metrics, shuffleReadClient, 3, new RssConf());
    fetcher.fetchAllRssBlocks();

    RawKeyValueIterator iterator = merger.close();
    // the final output of merger.close() must be sorted
    List<String> allKeysExpected = Lists.newArrayList("k11", "k22", "k22", "k33", "k44", "k55", "k55");
    List<String> allValuesExpected = Lists.newArrayList("v11", "v22", "v22", "v33", "v44", "v55", "v55");
    List<String> allKeys = Lists.newArrayList();
    List<String> allValues = Lists.newArrayList();
    while (iterator.next()) {
      byte[] key = new byte[iterator.getKey().getLength()];
      byte[] value = new byte[iterator.getValue().getLength()];
      System.arraycopy(iterator.getKey().getData(), 0, key, 0, key.length);
      System.arraycopy(iterator.getValue().getData(), 0, value, 0, value.length);
      allKeys.add(new Text(key).toString().trim());
      allValues.add(new Text(value).toString().trim());
    }
    assertEquals(allKeysExpected, allKeys);
    assertEquals(allValuesExpected, allValues);
    // There will be 2 retries
    assertEquals(2, fetcher.getRetryCount());
    assertEquals(2, ((MockMergeManagerImpl)merger).happenedFails.size());
  }

  public void testCodecIsDuplicated() throws Exception {
    fs = FileSystem.getLocal(conf);
    BZip2Codec codec = new BZip2Codec();
    codec.setConf(new Configuration());
    merger = new MergeManagerImpl<Text, Text>(
        reduceId1, jobConf, fs, lda, Reporter.NULL, codec, null, null, null, null,
        null, null, new Progress(), new MROutputFiles());
    TaskAttemptID taskAttemptID = RssMRUtils.createMRTaskAttemptId(new JobID(), TaskType.MAP, 1, 1);
    byte[] buffer = new byte[10];
    MapOutput  mapOutput1 = merger.reserve(taskAttemptID, 10, 1);
    RssBypassWriter.write(mapOutput1, buffer);
    MapOutput mapOutput2 = merger.reserve(taskAttemptID, 10, 1);
    RssBypassWriter.write(mapOutput2, buffer);
    assertEquals(RssBypassWriter.getDecompressor(
        (InMemoryMapOutput) mapOutput1), RssBypassWriter.getDecompressor((InMemoryMapOutput) mapOutput2));
  }

  private static void initRssData() throws Exception {
    data = new LinkedList<>();
    Map<String, String> map1 = new TreeMap<>();
    map1.put("k11", "v11");
    map1.put("k22", "v22");
    map1.put("k44", "v44");
    data.add(writeMapOutputRss(conf, map1));
    Map<String, String> map2 = new TreeMap<>();
    map2.put("k33", "v33");
    map2.put("k55", "v55");
    data.add(writeMapOutputRss(conf, map2));
    Map<String, String> map3 = new TreeMap<>();
    map3.put("k22", "v22");
    map3.put("k55", "v55");
    data.add(writeMapOutputRss(conf, map3));
  }

  private static void initLocalData() throws Exception {
    data = new LinkedList<>();
    Map<String, String> map1 = new TreeMap<>();
    map1.put("k11", "v11");
    map1.put("k22", "v22");
    map1.put("k44", "v44");
    data.add(writeMapOutput(conf, map1));
    Map<String, String> map2 = new TreeMap<>();
    map2.put("k33", "v33");
    map2.put("k55", "v55");
    data.add(writeMapOutput(conf, map2));
    Map<String, String> map3 = new TreeMap<>();
    map3.put("k22", "v22");
    map3.put("k55", "v55");
    data.add(writeMapOutput(conf, map3));
  }

  private static byte[] writeMapOutputRss(Configuration conf, Map<String, String> keysToValues)
      throws IOException, InterruptedException {
    SerializationFactory serializationFactory = new SerializationFactory(jobConf);
    MockShuffleWriteClient client = new MockShuffleWriteClient();
    client.setMode(2);
    Map<Integer, List<ShuffleServerInfo>> partitionToServers = Maps.newConcurrentMap();
    Set<Long> successBlocks = Sets.newConcurrentHashSet();
    Set<Long> failedBlocks = Sets.newConcurrentHashSet();
    Counters.Counter mapOutputByteCounter = new Counters.Counter();
    Counters.Counter mapOutputRecordCounter = new Counters.Counter();
    SortWriteBufferManager<Text, Text> manager = new SortWriteBufferManager(
        10240,
        1L,
        10,
        serializationFactory.getSerializer(Text.class),
        serializationFactory.getSerializer(Text.class),
        WritableComparator.get(Text.class),
        0.9,
        "test",
        client,
        500,
        5 * 1000,
        partitionToServers,
        successBlocks,
        failedBlocks,
        mapOutputByteCounter,
        mapOutputRecordCounter,
        1,
        100,
        2000,
        true,
        5,
        0.2f,
        1024000L,
        new RssConf());

    for (String key : keysToValues.keySet()) {
      String value = keysToValues.get(key);
      manager.addRecord(0, new Text(key), new Text(value));
    }
    manager.waitSendFinished();
    return client.data.get(0);
  }


  private static byte[] writeMapOutput(Configuration conf, Map<String, String> keysToValues)
      throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    FSDataOutputStream fsdos = new FSDataOutputStream(baos, null);
    IFile.Writer<Text, Text> writer = new IFile.Writer<Text, Text>(conf, fsdos,
        Text.class, Text.class, null, null);
    for (String key : keysToValues.keySet()) {
      String value = keysToValues.get(key);
      writer.append(new Text(key), new Text(value));
    }
    writer.close();
    return baos.toByteArray();
  }


  static class MockMergeManagerImpl<K,V> extends MergeManagerImpl {
    public Set<Integer> happenedFails = Sets.newHashSet();
    private Set<Integer> expectedFails;
    private int currentMapout = 0;

    @SuppressWarnings("checkstyle:RedundantModifier")
    public MockMergeManagerImpl(TaskAttemptID reduceId, JobConf jobConf,
                                FileSystem localFS, LocalDirAllocator localDirAllocator,
                                Reporter reporter, CompressionCodec codec, Class combinerClass,
                                Task.CombineOutputCollector combineCollector,
                                Counters.Counter spilledRecordsCounter, Counters.Counter reduceCombineInputCounter,
                                Counters.Counter mergedMapOutputsCounter, ExceptionReporter exceptionReporter,
                                Progress mergePhase, MapOutputFile mapOutputFile, Set<Integer> expectedFails) {
      super(reduceId, jobConf, localFS, localDirAllocator, reporter, codec, combinerClass,
          combineCollector, spilledRecordsCounter, reduceCombineInputCounter,
          mergedMapOutputsCounter, exceptionReporter, mergePhase, mapOutputFile);
      this.expectedFails = expectedFails;
    }

    @Override
    public synchronized MapOutput<K,V> reserve(TaskAttemptID mapId,
                                               long requestedSize,
                                               int fetcher) throws IOException {
      if (expectedFails.contains(currentMapout) && !happenedFails.contains(currentMapout)) {
        happenedFails.add(currentMapout);
        return null;
      } else {
        currentMapout++;
        return super.reserve(mapId, requestedSize, fetcher);
      }
    }
  }

  static class MockShuffleWriteClient implements ShuffleWriteClient {

    int mode = 0;

    public void setMode(int mode) {
      this.mode = mode;
    }

    public List<byte[]> data = new LinkedList<>();

    @Override
    public SendShuffleDataResult sendShuffleData(String appId, List<ShuffleBlockInfo> shuffleBlockInfoList,
        Supplier<Boolean> needCancelRequest) {
      if (mode == 0) {
        throw new RssException("send data failed");
      } else if (mode == 1) {
        return new SendShuffleDataResult(Sets.newHashSet(2L), Sets.newHashSet(1L));
      } else {
        Set<Long> successBlockIds = Sets.newHashSet();
        for (ShuffleBlockInfo blockInfo : shuffleBlockInfoList) {
          successBlockIds.add(blockInfo.getBlockId());
        }
        shuffleBlockInfoList.forEach(block -> {
          ByteBuffer uncompressedBuffer = ByteBuffer.allocate(block.getUncompressLength());
          codec.decompress(
              ByteBuffer.wrap(block.getData()),
              block.getUncompressLength(),
              uncompressedBuffer,
              0
          );
          data.add(uncompressedBuffer.array());
        });
        return new SendShuffleDataResult(successBlockIds, Sets.newHashSet());
      }
    }

    @Override
    public void sendAppHeartbeat(String appId, long timeoutMs) {

    }

    @Override
    public void registerApplicationInfo(String appId, long timeoutMs, String user) {

    }

    @Override
    public void registerShuffle(
        ShuffleServerInfo shuffleServerInfo,
        String appId,
        int shuffleId,
        List<PartitionRange> partitionRanges,
        RemoteStorageInfo storageType,
        ShuffleDataDistributionType distributionType) {

    }

    @Override
    public boolean sendCommit(Set<ShuffleServerInfo> shuffleServerInfoSet, String appId, int shuffleId, int numMaps) {
      return false;
    }

    @Override
    public void registerCoordinators(String coordinators) {

    }

    @Override
    public Map<String, String> fetchClientConf(int timeoutMs) {
      return null;
    }

    @Override
    public RemoteStorageInfo fetchRemoteStorage(String appId) {
      return null;
    }

    @Override
    public void reportShuffleResult(Map<Integer, List<ShuffleServerInfo>> partitionToServers, String appId,
        int shuffleId, long taskAttemptId, Map<Integer, List<Long>> partitionToBlockIds, int bitmapNum) {

    }

    @Override
    public ShuffleAssignmentsInfo getShuffleAssignments(String appId, int shuffleId, int partitionNum,
        int partitionNumPerRange, Set<String> requiredTags, int assignmentShuffleServerNumber,
        int estimateTaskConcurrency) {
      return null;
    }

    @Override
    public Roaring64NavigableMap getShuffleResult(String clientType, Set<ShuffleServerInfo> shuffleServerInfoSet,
                                                  String appId, int shuffleId, int partitionId) {
      return null;
    }

    @Override
    public Roaring64NavigableMap getShuffleResultForMultiPart(String clientType, Map<ShuffleServerInfo,
        Set<Integer>> serverToPartitions, String appId, int shuffleId) {
      return null;
    }

    @Override
    public void close() {

    }

    @Override
    public void unregisterShuffle(String appId, int shuffleId) {

    }
  }

  static class MockedShuffleReadClient implements ShuffleReadClient {
    List<CompressedShuffleBlock> blocks;
    int index = 0;

    MockedShuffleReadClient(List<byte[]> data) {
      this.blocks = new LinkedList<>();
      data.forEach(bytes -> {
        byte[] compressed = codec.compress(bytes);
        blocks.add(new CompressedShuffleBlock(ByteBuffer.wrap(compressed), bytes.length));
      });
    }

    @Override
    public CompressedShuffleBlock readShuffleBlockData() {
      if (index < blocks.size()) {
        return blocks.get(index++);
      } else {
        return null;
      }
    }

    @Override
    public void checkProcessedBlockIds() {
    }

    @Override
    public void close() {
    }

    @Override
    public void logStatics() {
    }
  }

  static class MockedReporter implements Reporter {
    MockedReporter() {
    }

    @Override
    public void setStatus(String s) {
    }

    @Override
    public Counters.Counter getCounter(Enum<?> anEnum) {
      return null;
    }

    @Override
    public Counters.Counter getCounter(String s, String s1) {
      return null;
    }

    @Override
    public void incrCounter(Enum<?> anEnum, long l) {
    }

    @Override
    public void incrCounter(String s, String s1, long l) {
    }

    @Override
    public InputSplit getInputSplit() throws UnsupportedOperationException {
      return null;
    }

    @Override
    public float getProgress() {
      return 0;
    }

    @Override
    public void progress() {
    }
  }

  static class MockedTaskStatus extends TaskStatus {

    @Override
    public boolean getIsMap() {
      return false;
    }

    @Override
    public void addFetchFailedMap(org.apache.hadoop.mapred.TaskAttemptID taskAttemptID) {
    }
  }
}

