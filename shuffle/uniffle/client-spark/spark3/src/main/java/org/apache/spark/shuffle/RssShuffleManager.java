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

package org.apache.spark.shuffle;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.hadoop.conf.Configuration;
import org.apache.spark.MapOutputTracker;
import org.apache.spark.ShuffleDependency;
import org.apache.spark.SparkConf;
import org.apache.spark.SparkEnv;
import org.apache.spark.TaskContext;
import org.apache.spark.executor.ShuffleReadMetrics;
import org.apache.spark.executor.ShuffleWriteMetrics;
import org.apache.spark.shuffle.reader.RssShuffleReader;
import org.apache.spark.shuffle.writer.AddBlockEvent;
import org.apache.spark.shuffle.writer.BufferManagerOptions;
import org.apache.spark.shuffle.writer.RssShuffleWriter;
import org.apache.spark.shuffle.writer.WriteBufferManager;
import org.apache.spark.sql.internal.SQLConf;
import org.apache.spark.storage.BlockId;
import org.apache.spark.storage.BlockManagerId;
import org.apache.spark.util.EventLoop;
import org.roaringbitmap.longlong.Roaring64NavigableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Tuple2;
import scala.Tuple3;
import scala.collection.Iterator;
import scala.collection.Seq;

import org.apache.uniffle.client.api.ShuffleWriteClient;
import org.apache.uniffle.client.factory.ShuffleClientFactory;
import org.apache.uniffle.client.response.SendShuffleDataResult;
import org.apache.uniffle.client.util.ClientUtils;
import org.apache.uniffle.common.PartitionRange;
import org.apache.uniffle.common.RemoteStorageInfo;
import org.apache.uniffle.common.ShuffleAssignmentsInfo;
import org.apache.uniffle.common.ShuffleBlockInfo;
import org.apache.uniffle.common.ShuffleDataDistributionType;
import org.apache.uniffle.common.ShuffleServerInfo;
import org.apache.uniffle.common.config.RssClientConf;
import org.apache.uniffle.common.config.RssConf;
import org.apache.uniffle.common.exception.RssException;
import org.apache.uniffle.common.util.RetryUtils;
import org.apache.uniffle.common.util.RssUtils;
import org.apache.uniffle.common.util.ThreadUtils;

public class RssShuffleManager implements ShuffleManager {

  private static final Logger LOG = LoggerFactory.getLogger(RssShuffleManager.class);
  private final String clientType;
  private final long heartbeatInterval;
  private final long heartbeatTimeout;
  private final ThreadPoolExecutor threadPoolExecutor;
  private AtomicReference<String> id = new AtomicReference<>();
  private SparkConf sparkConf;
  private final int dataReplica;
  private final int dataReplicaWrite;
  private final int dataReplicaRead;
  private final boolean dataReplicaSkipEnabled;
  private final int dataTransferPoolSize;
  private final int dataCommitPoolSize;
  private ShuffleWriteClient shuffleWriteClient;
  private final Map<String, Set<Long>> taskToSuccessBlockIds;
  private final Map<String, Set<Long>> taskToFailedBlockIds;
  private Map<String, WriteBufferManager> taskToBufferManager = Maps.newConcurrentMap();
  private ScheduledExecutorService heartBeatScheduledExecutorService;
  private boolean heartbeatStarted = false;
  private boolean dynamicConfEnabled = false;
  private final ShuffleDataDistributionType dataDistributionType;
  private String user;
  private String uuid;
  private Set<String> failedTaskIds = Sets.newConcurrentHashSet();
  private final EventLoop<AddBlockEvent> eventLoop;
  private final EventLoop<AddBlockEvent> defaultEventLoop = new EventLoop<AddBlockEvent>("ShuffleDataQueue") {

    @Override
    public void onReceive(AddBlockEvent event) {
      threadPoolExecutor.execute(() -> sendShuffleData(event.getTaskId(), event.getShuffleDataInfoList()));
    }

    @Override
    public void onError(Throwable throwable) {
      LOG.info("Shuffle event loop error...", throwable);
    }

    @Override
    public void onStart() {
      LOG.info("Shuffle event loop start...");
    }

    private void sendShuffleData(String taskId, List<ShuffleBlockInfo> shuffleDataInfoList) {
      try {
        SendShuffleDataResult result = shuffleWriteClient.sendShuffleData(
            id.get(),
            shuffleDataInfoList,
            () -> !isValidTask(taskId)
        );
        putBlockId(taskToSuccessBlockIds, taskId, result.getSuccessBlockIds());
        putBlockId(taskToFailedBlockIds, taskId, result.getFailedBlockIds());
      } finally {
        final AtomicLong releaseSize = new AtomicLong(0);
        shuffleDataInfoList.forEach((sbi) -> releaseSize.addAndGet(sbi.getFreeMemory()));
        WriteBufferManager bufferManager = taskToBufferManager.get(taskId);
        if (bufferManager != null) {
          bufferManager.freeAllocatedMemory(releaseSize.get());
        }
        LOG.debug("Spark 3.0 finish send data and release " + releaseSize + " bytes");
      }
    }

    private synchronized void putBlockId(
        Map<String, Set<Long>> taskToBlockIds,
        String taskAttemptId,
        Set<Long> blockIds) {
      if (blockIds == null || blockIds.isEmpty()) {
        return;
      }
      taskToBlockIds.putIfAbsent(taskAttemptId, Sets.newConcurrentHashSet());
      taskToBlockIds.get(taskAttemptId).addAll(blockIds);
    }
  };

  public RssShuffleManager(SparkConf conf, boolean isDriver) {
    this.sparkConf = conf;
    boolean supportsRelocation = Optional.ofNullable(SparkEnv.get())
        .map(env -> env.serializer().supportsRelocationOfSerializedObjects())
        .orElse(true);
    if (!supportsRelocation) {
      LOG.warn("RSSShuffleManager requires a serializer which supports relocations of serialized object. Please set "
          + "spark.serializer to org.apache.spark.serializer.KryoSerializer instead");
    }
    this.user = sparkConf.get("spark.rss.quota.user", "user");
    this.uuid = sparkConf.get("spark.rss.quota.uuid",  Long.toString(System.currentTimeMillis()));
    // set & check replica config
    this.dataReplica = sparkConf.get(RssSparkConfig.RSS_DATA_REPLICA);
    this.dataReplicaWrite =  sparkConf.get(RssSparkConfig.RSS_DATA_REPLICA_WRITE);
    this.dataReplicaRead =  sparkConf.get(RssSparkConfig.RSS_DATA_REPLICA_READ);
    this.dataReplicaSkipEnabled = sparkConf.get(RssSparkConfig.RSS_DATA_REPLICA_SKIP_ENABLED);
    LOG.info("Check quorum config ["
        + dataReplica + ":" + dataReplicaWrite + ":" + dataReplicaRead + ":" + dataReplicaSkipEnabled + "]");
    RssUtils.checkQuorumSetting(dataReplica, dataReplicaWrite, dataReplicaRead);

    this.heartbeatInterval = sparkConf.get(RssSparkConfig.RSS_HEARTBEAT_INTERVAL);
    this.heartbeatTimeout = sparkConf.getLong(RssSparkConfig.RSS_HEARTBEAT_TIMEOUT.key(), heartbeatInterval / 2);
    final int retryMax = sparkConf.get(RssSparkConfig.RSS_CLIENT_RETRY_MAX);
    this.clientType = sparkConf.get(RssSparkConfig.RSS_CLIENT_TYPE);
    this.dynamicConfEnabled = sparkConf.get(RssSparkConfig.RSS_DYNAMIC_CLIENT_CONF_ENABLED);
    this.dataDistributionType = getDataDistributionType(sparkConf);
    long retryIntervalMax = sparkConf.get(RssSparkConfig.RSS_CLIENT_RETRY_INTERVAL_MAX);
    int heartBeatThreadNum = sparkConf.get(RssSparkConfig.RSS_CLIENT_HEARTBEAT_THREAD_NUM);
    this.dataTransferPoolSize = sparkConf.get(RssSparkConfig.RSS_DATA_TRANSFER_POOL_SIZE);
    this.dataCommitPoolSize = sparkConf.get(RssSparkConfig.RSS_DATA_COMMIT_POOL_SIZE);
    int unregisterThreadPoolSize = sparkConf.get(RssSparkConfig.RSS_CLIENT_UNREGISTER_THREAD_POOL_SIZE);
    int unregisterRequestTimeoutSec = sparkConf.get(RssSparkConfig.RSS_CLIENT_UNREGISTER_REQUEST_TIMEOUT_SEC);
    shuffleWriteClient = ShuffleClientFactory
        .getInstance()
        .createShuffleWriteClient(clientType, retryMax, retryIntervalMax, heartBeatThreadNum,
            dataReplica, dataReplicaWrite, dataReplicaRead, dataReplicaSkipEnabled, dataTransferPoolSize,
            dataCommitPoolSize, unregisterThreadPoolSize, unregisterRequestTimeoutSec);
    registerCoordinator();
    // fetch client conf and apply them if necessary and disable ESS
    if (isDriver && dynamicConfEnabled) {
      Map<String, String> clusterClientConf = shuffleWriteClient.fetchClientConf(
          sparkConf.getInt(RssSparkConfig.RSS_ACCESS_TIMEOUT_MS.key(),
              RssSparkConfig.RSS_ACCESS_TIMEOUT_MS.defaultValue().get()));
      RssSparkShuffleUtils.applyDynamicClientConf(sparkConf, clusterClientConf);
    }
    RssSparkShuffleUtils.validateRssClientConf(sparkConf);
    // External shuffle service is not supported when using remote shuffle service
    sparkConf.set("spark.shuffle.service.enabled", "false");
    LOG.info("Disable external shuffle service in RssShuffleManager.");
    sparkConf.set("spark.sql.adaptive.localShuffleReader.enabled", "false");
    LOG.info("Disable local shuffle reader in RssShuffleManager.");
    taskToSuccessBlockIds = Maps.newConcurrentMap();
    taskToFailedBlockIds = Maps.newConcurrentMap();
    // for non-driver executor, start a thread for sending shuffle data to shuffle server
    LOG.info("RSS data send thread is starting");
    eventLoop = defaultEventLoop;
    eventLoop.start();
    int poolSize = sparkConf.get(RssSparkConfig.RSS_CLIENT_SEND_THREAD_POOL_SIZE);
    int keepAliveTime = sparkConf.get(RssSparkConfig.RSS_CLIENT_SEND_THREAD_POOL_KEEPALIVE);
    threadPoolExecutor = new ThreadPoolExecutor(poolSize, poolSize * 2, keepAliveTime, TimeUnit.SECONDS,
        Queues.newLinkedBlockingQueue(Integer.MAX_VALUE),
        ThreadUtils.getThreadFactory("SendData-%d"));
    if (isDriver) {
      heartBeatScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(
          ThreadUtils.getThreadFactory("rss-heartbeat-%d"));
    }
  }

  @VisibleForTesting
  protected static ShuffleDataDistributionType getDataDistributionType(SparkConf sparkConf) {
    RssConf rssConf = RssSparkConfig.toRssConf(sparkConf);
    if ((boolean) sparkConf.get(SQLConf.ADAPTIVE_EXECUTION_ENABLED())
        && !rssConf.containsKey(RssClientConf.DATA_DISTRIBUTION_TYPE.key())) {
      return ShuffleDataDistributionType.LOCAL_ORDER;
    }

    return rssConf.get(RssClientConf.DATA_DISTRIBUTION_TYPE);
  }

  // For testing only
  @VisibleForTesting
  RssShuffleManager(
      SparkConf conf,
      boolean isDriver,
      EventLoop<AddBlockEvent> loop,
      Map<String, Set<Long>> taskToSuccessBlockIds,
      Map<String, Set<Long>> taskToFailedBlockIds) {
    this.sparkConf = conf;
    this.clientType = sparkConf.get(RssSparkConfig.RSS_CLIENT_TYPE);
    this.dataDistributionType = RssSparkConfig.toRssConf(sparkConf).get(RssClientConf.DATA_DISTRIBUTION_TYPE);
    this.heartbeatInterval = sparkConf.get(RssSparkConfig.RSS_HEARTBEAT_INTERVAL);
    this.heartbeatTimeout = sparkConf.getLong(RssSparkConfig.RSS_HEARTBEAT_TIMEOUT.key(), heartbeatInterval / 2);
    this.dataReplica = sparkConf.get(RssSparkConfig.RSS_DATA_REPLICA);
    this.dataReplicaWrite =  sparkConf.get(RssSparkConfig.RSS_DATA_REPLICA_WRITE);
    this.dataReplicaRead =  sparkConf.get(RssSparkConfig.RSS_DATA_REPLICA_READ);
    this.dataReplicaSkipEnabled = sparkConf.get(RssSparkConfig.RSS_DATA_REPLICA_SKIP_ENABLED);
    LOG.info("Check quorum config ["
        + dataReplica + ":" + dataReplicaWrite + ":" + dataReplicaRead + ":" + dataReplicaSkipEnabled + "]");
    RssUtils.checkQuorumSetting(dataReplica, dataReplicaWrite, dataReplicaRead);

    int retryMax = sparkConf.get(RssSparkConfig.RSS_CLIENT_RETRY_MAX);
    long retryIntervalMax = sparkConf.get(RssSparkConfig.RSS_CLIENT_RETRY_INTERVAL_MAX);
    int heartBeatThreadNum = sparkConf.get(RssSparkConfig.RSS_CLIENT_HEARTBEAT_THREAD_NUM);
    this.dataTransferPoolSize = sparkConf.get(RssSparkConfig.RSS_DATA_TRANSFER_POOL_SIZE);
    this.dataCommitPoolSize = sparkConf.get(RssSparkConfig.RSS_DATA_COMMIT_POOL_SIZE);
    int unregisterThreadPoolSize = sparkConf.get(RssSparkConfig.RSS_CLIENT_UNREGISTER_THREAD_POOL_SIZE);
    int unregisterRequestTimeoutSec = sparkConf.get(RssSparkConfig.RSS_CLIENT_UNREGISTER_REQUEST_TIMEOUT_SEC);
    shuffleWriteClient = ShuffleClientFactory
        .getInstance()
        .createShuffleWriteClient(
            clientType,
            retryMax,
            retryIntervalMax,
            heartBeatThreadNum,
            dataReplica,
            dataReplicaWrite,
            dataReplicaRead,
            dataReplicaSkipEnabled,
            dataTransferPoolSize,
            dataCommitPoolSize,
            unregisterThreadPoolSize,
            unregisterRequestTimeoutSec
        );
    this.taskToSuccessBlockIds = taskToSuccessBlockIds;
    this.taskToFailedBlockIds = taskToFailedBlockIds;
    if (loop != null) {
      eventLoop = loop;
    } else {
      eventLoop = defaultEventLoop;
    }
    eventLoop.start();
    threadPoolExecutor = null;
    heartBeatScheduledExecutorService = null;
  }


  // This method is called in Spark driver side,
  // and Spark driver will make some decision according to coordinator,
  // e.g. determining what RSS servers to use.
  // Then Spark driver will return a ShuffleHandle and
  // pass that ShuffleHandle to executors (getWriter/getReader)
  @Override
  public <K, V, C> ShuffleHandle registerShuffle(int shuffleId, ShuffleDependency<K, V, C> dependency) {

    //Spark have three kinds of serializer:
    //org.apache.spark.serializer.JavaSerializer
    //org.apache.spark.sql.execution.UnsafeRowSerializer
    //org.apache.spark.serializer.KryoSerializer,
    //Only org.apache.spark.serializer.JavaSerializer don't support RelocationOfSerializedObjects.
    //So when we find the parameters to use org.apache.spark.serializer.JavaSerializer, We should throw an exception
    if (!SparkEnv.get().serializer().supportsRelocationOfSerializedObjects()) {
      throw new IllegalArgumentException("Can't use serialized shuffle for shuffleId: " + shuffleId + ", because the"
              + " serializer: " + SparkEnv.get().serializer().getClass().getName() + " does not support object "
              + "relocation.");
    }

    if (id.get() == null) {
      id.compareAndSet(null, SparkEnv.get().conf().getAppId() + "_" + uuid);
    }
    LOG.info("Generate application id used in rss: " + id.get());

    if (dependency.partitioner().numPartitions() == 0) {
      LOG.info("RegisterShuffle with ShuffleId[" + shuffleId + "], partitionNum is 0, "
          + "return the empty RssShuffleHandle directly");
      return new RssShuffleHandle<>(shuffleId,
        id.get(),
        dependency.rdd().getNumPartitions(),
        dependency,
        Collections.emptyMap(),
        RemoteStorageInfo.EMPTY_REMOTE_STORAGE);
    }

    String storageType = sparkConf.get(RssSparkConfig.RSS_STORAGE_TYPE.key());
    RemoteStorageInfo defaultRemoteStorage = new RemoteStorageInfo(
        sparkConf.get(RssSparkConfig.RSS_REMOTE_STORAGE_PATH.key(), ""));
    RemoteStorageInfo remoteStorage = ClientUtils.fetchRemoteStorage(
        id.get(), defaultRemoteStorage, dynamicConfEnabled, storageType, shuffleWriteClient);

    Set<String> assignmentTags = RssSparkShuffleUtils.getAssignmentTags(sparkConf);

    int requiredShuffleServerNumber = RssSparkShuffleUtils.getRequiredShuffleServerNumber(sparkConf);

    // retryInterval must bigger than `rss.server.heartbeat.timeout`, or maybe it will return the same result
    long retryInterval = sparkConf.get(RssSparkConfig.RSS_CLIENT_ASSIGNMENT_RETRY_INTERVAL);
    int retryTimes = sparkConf.get(RssSparkConfig.RSS_CLIENT_ASSIGNMENT_RETRY_TIMES);
    int estimateTaskConcurrency = RssSparkShuffleUtils.estimateTaskConcurrency(sparkConf);
    Map<Integer, List<ShuffleServerInfo>> partitionToServers;
    try {
      partitionToServers = RetryUtils.retry(() -> {
        ShuffleAssignmentsInfo response = shuffleWriteClient.getShuffleAssignments(
                id.get(),
                shuffleId,
                dependency.partitioner().numPartitions(),
                1,
                assignmentTags,
                requiredShuffleServerNumber,
                estimateTaskConcurrency);
        registerShuffleServers(id.get(), shuffleId, response.getServerToPartitionRanges(), remoteStorage);
        return response.getPartitionToServers();
      }, retryInterval, retryTimes);
    } catch (Throwable throwable) {
      throw new RssException("registerShuffle failed!", throwable);
    }
    startHeartbeat();

    LOG.info("RegisterShuffle with ShuffleId[" + shuffleId + "], partitionNum[" + partitionToServers.size()
        + "], shuffleServerForResult: " + partitionToServers);
    return new RssShuffleHandle<>(shuffleId,
        id.get(),
        dependency.rdd().getNumPartitions(),
        dependency,
        partitionToServers,
        remoteStorage);
  }

  @Override
  public <K, V> ShuffleWriter<K, V> getWriter(
      ShuffleHandle handle,
      long mapId,
      TaskContext context,
      ShuffleWriteMetricsReporter metrics) {
    if (!(handle instanceof RssShuffleHandle)) {
      throw new RuntimeException("Unexpected ShuffleHandle:" + handle.getClass().getName());
    }
    RssShuffleHandle<K, V, ?> rssHandle = (RssShuffleHandle<K, V, ?>) handle;
    // todo: this implement is tricky, we should refactor it
    if (id.get() == null) {
      id.compareAndSet(null, rssHandle.getAppId());
    }
    int shuffleId = rssHandle.getShuffleId();
    String taskId = "" + context.taskAttemptId() + "_" + context.attemptNumber();
    BufferManagerOptions bufferOptions = new BufferManagerOptions(sparkConf);
    ShuffleWriteMetrics writeMetrics;
    if (metrics != null) {
      writeMetrics = new WriteMetrics(metrics);
    } else {
      writeMetrics = context.taskMetrics().shuffleWriteMetrics();
    }
    WriteBufferManager bufferManager = new WriteBufferManager(
        shuffleId, context.taskAttemptId(), bufferOptions, rssHandle.getDependency().serializer(),
        rssHandle.getPartitionToServers(), context.taskMemoryManager(),
        writeMetrics, RssSparkConfig.toRssConf(sparkConf));
    taskToBufferManager.put(taskId, bufferManager);
    LOG.info("RssHandle appId {} shuffleId {} ", rssHandle.getAppId(), rssHandle.getShuffleId());
    return new RssShuffleWriter<>(rssHandle.getAppId(), shuffleId, taskId, context.taskAttemptId(), bufferManager,
        writeMetrics, this, sparkConf, shuffleWriteClient, rssHandle,
        (Function<String, Boolean>) this::markFailedTask);
  }

  @Override
  public <K, C> ShuffleReader<K, C> getReader(
      ShuffleHandle handle,
      int startPartition,
      int endPartition,
      TaskContext context,
      ShuffleReadMetricsReporter metrics) {
    return getReader(handle, 0, Integer.MAX_VALUE, startPartition, endPartition,
        context, metrics);
  }

  // The interface is used for compatibility with spark 3.0.1
  public <K, C> ShuffleReader<K, C> getReader(
      ShuffleHandle handle,
      int startMapIndex,
      int endMapIndex,
      int startPartition,
      int endPartition,
      TaskContext context,
      ShuffleReadMetricsReporter metrics) {
    long start = System.currentTimeMillis();
    Roaring64NavigableMap taskIdBitmap = getExpectedTasksByExecutorId(
        handle.shuffleId(),
        startPartition,
        endPartition,
        startMapIndex,
        endMapIndex);
    LOG.info("Get taskId cost " + (System.currentTimeMillis() - start) + " ms, and request expected blockIds from "
        + taskIdBitmap.getLongCardinality() + " tasks for shuffleId[" + handle.shuffleId() + "], partitionId["
        + startPartition + ", " + endPartition + "]");
    return getReaderImpl(handle, startMapIndex, endMapIndex, startPartition, endPartition,
        context, metrics, taskIdBitmap);
  }

  // The interface is used for compatibility with spark 3.0.1
  public <K, C> ShuffleReader<K, C> getReaderForRange(
      ShuffleHandle handle,
      int startMapIndex,
      int endMapIndex,
      int startPartition,
      int endPartition,
      TaskContext context,
      ShuffleReadMetricsReporter metrics) {
    long start = System.currentTimeMillis();
    Roaring64NavigableMap taskIdBitmap = getExpectedTasksByRange(
        handle.shuffleId(),
        startPartition,
        endPartition,
        startMapIndex,
        endMapIndex);
    LOG.info("Get taskId cost " + (System.currentTimeMillis() - start) + " ms, and request expected blockIds from "
        + taskIdBitmap.getLongCardinality() + " tasks for shuffleId[" + handle.shuffleId() + "], partitionId["
        + startPartition + ", " + endPartition + "]");
    return getReaderImpl(handle, startMapIndex, endMapIndex, startPartition, endPartition,
        context, metrics, taskIdBitmap);
  }

  public <K, C> ShuffleReader<K, C> getReaderImpl(
      ShuffleHandle handle,
      int startMapIndex,
      int endMapIndex,
      int startPartition,
      int endPartition,
      TaskContext context,
      ShuffleReadMetricsReporter metrics,
      Roaring64NavigableMap taskIdBitmap) {
    if (!(handle instanceof RssShuffleHandle)) {
      throw new RuntimeException("Unexpected ShuffleHandle:" + handle.getClass().getName());
    }
    final String storageType = sparkConf.get(RssSparkConfig.RSS_STORAGE_TYPE.key());
    final int indexReadLimit = sparkConf.get(RssSparkConfig.RSS_INDEX_READ_LIMIT);
    RssShuffleHandle<K, C, ?> rssShuffleHandle = (RssShuffleHandle<K, C, ?>) handle;
    final int partitionNum = rssShuffleHandle.getDependency().partitioner().numPartitions();
    long readBufferSize = sparkConf.getSizeAsBytes(RssSparkConfig.RSS_CLIENT_READ_BUFFER_SIZE.key(),
        RssSparkConfig.RSS_CLIENT_READ_BUFFER_SIZE.defaultValue().get());
    if (readBufferSize > Integer.MAX_VALUE) {
      LOG.warn(RssSparkConfig.RSS_CLIENT_READ_BUFFER_SIZE.key() + " can support 2g as max");
      readBufferSize = Integer.MAX_VALUE;
    }
    int shuffleId = rssShuffleHandle.getShuffleId();
    Map<Integer, List<ShuffleServerInfo>> allPartitionToServers = rssShuffleHandle.getPartitionToServers();
    Map<Integer, List<ShuffleServerInfo>> requirePartitionToServers = allPartitionToServers.entrySet()
        .stream().filter(x -> x.getKey() >= startPartition && x.getKey() < endPartition)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    Map<ShuffleServerInfo, Set<Integer>> serverToPartitions = RssUtils.generateServerToPartitions(
        requirePartitionToServers);
    long start = System.currentTimeMillis();
    Roaring64NavigableMap blockIdBitmap = shuffleWriteClient.getShuffleResultForMultiPart(
        clientType, serverToPartitions, rssShuffleHandle.getAppId(), shuffleId);
    LOG.info("Get shuffle blockId cost " + (System.currentTimeMillis() - start) + " ms, and get "
        + blockIdBitmap.getLongCardinality() + " blockIds for shuffleId[" + shuffleId + "], startPartition["
        + start + "], endPartition[" + endPartition + "]");

    ShuffleReadMetrics readMetrics;
    if (metrics != null) {
      readMetrics = new ReadMetrics(metrics);
    } else {
      readMetrics = context.taskMetrics().shuffleReadMetrics();
    }

    final RemoteStorageInfo shuffleRemoteStorageInfo = rssShuffleHandle.getRemoteStorage();
    LOG.info("Shuffle reader using remote storage {}", shuffleRemoteStorageInfo);
    final String shuffleRemoteStoragePath = shuffleRemoteStorageInfo.getPath();
    Configuration readerHadoopConf = RssSparkShuffleUtils.getRemoteStorageHadoopConf(
        sparkConf, shuffleRemoteStorageInfo);

    return new RssShuffleReader<K, C>(
        startPartition,
        endPartition,
        startMapIndex,
        endMapIndex,
        context,
        rssShuffleHandle,
        shuffleRemoteStoragePath,
        indexReadLimit,
        readerHadoopConf,
        storageType,
        (int) readBufferSize,
        partitionNum,
        RssUtils.generatePartitionToBitmap(blockIdBitmap, startPartition, endPartition),
        taskIdBitmap,
        readMetrics,
        RssSparkConfig.toRssConf(sparkConf),
        dataDistributionType
    );
  }

  @SuppressFBWarnings("REC_CATCH_EXCEPTION")
  private Roaring64NavigableMap getExpectedTasksByExecutorId(
      int shuffleId,
      int startPartition,
      int endPartition,
      int startMapIndex,
      int endMapIndex) {
    Roaring64NavigableMap taskIdBitmap = Roaring64NavigableMap.bitmapOf();
    Iterator<Tuple2<BlockManagerId, Seq<Tuple3<BlockId, Object, Object>>>> mapStatusIter = null;
    // Since Spark 3.1 refactors the interface of getMapSizesByExecutorId,
    // we use reflection and catch for the compatibility with 3.0 & 3.1 & 3.2
    try {
      // attempt to use Spark 3.1's API
      mapStatusIter = (Iterator<Tuple2<BlockManagerId, Seq<Tuple3<BlockId, Object, Object>>>>)
          SparkEnv.get().mapOutputTracker().getClass()
              .getDeclaredMethod("getMapSizesByExecutorId",
                  int.class, int.class, int.class, int.class, int.class)
              .invoke(SparkEnv.get().mapOutputTracker(),
                  shuffleId,
                  startMapIndex,
                  endMapIndex,
                  startPartition,
                  endPartition);
    } catch (Exception ignored) {
      // fallback and attempt to use Spark 3.0's API
      try {
        mapStatusIter = (Iterator<Tuple2<BlockManagerId, Seq<Tuple3<BlockId, Object, Object>>>>)
            SparkEnv.get().mapOutputTracker().getClass()
                .getDeclaredMethod("getMapSizesByExecutorId",
                    int.class, int.class, int.class)
                .invoke(
                    SparkEnv.get().mapOutputTracker(),
                    shuffleId,
                    startPartition,
                    endPartition);
      } catch (Exception ignored1) {
        try {
          // attempt to use Spark 3.2.0's API
          // Each Spark release will be versioned: [MAJOR].[FEATURE].[MAINTENANCE].
          // Usually we only need to adapt [MAJOR].[FEATURE] . Unfortunately,
          // some interfaces were removed wrongly in Spark 3.2.0. And they were added by Spark 3.2.1.
          // So we need to adapt Spark 3.2.0 here
          mapStatusIter = (Iterator<Tuple2<BlockManagerId, Seq<Tuple3<BlockId, Object, Object>>>>)
              MapOutputTracker.class.getDeclaredMethod("getMapSizesByExecutorId",
                  int.class, int.class, int.class, int.class, int.class)
                  .invoke(SparkEnv.get().mapOutputTracker(),
                      shuffleId,
                      startMapIndex,
                      endMapIndex,
                      startPartition,
                      endPartition);
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    }
    while (mapStatusIter.hasNext()) {
      Tuple2<BlockManagerId, Seq<Tuple3<BlockId, Object, Object>>> tuple2 = mapStatusIter.next();
      if (!tuple2._1().topologyInfo().isDefined()) {
        throw new RuntimeException("Can't get expected taskAttemptId");
      }
      taskIdBitmap.add(Long.parseLong(tuple2._1().topologyInfo().get()));
    }
    return taskIdBitmap;
  }

  // This API is only used by Spark3.0 and removed since 3.1,
  // so we extract it from getExpectedTasksByExecutorId.
  private Roaring64NavigableMap getExpectedTasksByRange(
      int shuffleId,
      int startPartition,
      int endPartition,
      int startMapIndex,
      int endMapIndex) {
    Roaring64NavigableMap taskIdBitmap = Roaring64NavigableMap.bitmapOf();
    Iterator<Tuple2<BlockManagerId, Seq<Tuple3<BlockId, Object, Object>>>> mapStatusIter = null;
    try {
      mapStatusIter = (Iterator<Tuple2<BlockManagerId, Seq<Tuple3<BlockId, Object, Object>>>>)
          SparkEnv.get().mapOutputTracker().getClass()
              .getDeclaredMethod("getMapSizesByRange",
                  int.class, int.class, int.class, int.class, int.class)
              .invoke(SparkEnv.get().mapOutputTracker(),
                  shuffleId,
                  startMapIndex,
                  endMapIndex,
                  startPartition,
                  endPartition);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    while (mapStatusIter.hasNext()) {
      Tuple2<BlockManagerId, Seq<Tuple3<BlockId, Object, Object>>> tuple2 = mapStatusIter.next();
      if (!tuple2._1().topologyInfo().isDefined()) {
        throw new RuntimeException("Can't get expected taskAttemptId");
      }
      taskIdBitmap.add(Long.parseLong(tuple2._1().topologyInfo().get()));
    }
    return taskIdBitmap;
  }

  @Override
  public boolean unregisterShuffle(int shuffleId) {
    try {
      if (SparkEnv.get().executorId().equals("driver")) {
        shuffleWriteClient.unregisterShuffle(id.get(), shuffleId);
      }
    } catch (Exception e) {
      LOG.warn("Errors on unregister to remote shuffle-servers", e);
    }
    return true;
  }

  @Override
  public ShuffleBlockResolver shuffleBlockResolver() {
    throw new RuntimeException("RssShuffleManager.shuffleBlockResolver is not implemented");
  }

  @Override
  public void stop() {
    if (heartBeatScheduledExecutorService != null) {
      heartBeatScheduledExecutorService.shutdownNow();
    }
    if (threadPoolExecutor != null) {
      threadPoolExecutor.shutdownNow();
    }
    if (shuffleWriteClient != null) {
      shuffleWriteClient.close();
    }
    if (eventLoop != null) {
      eventLoop.stop();
    }
  }

  public void clearTaskMeta(String taskId) {
    taskToSuccessBlockIds.remove(taskId);
    taskToFailedBlockIds.remove(taskId);
    taskToBufferManager.remove(taskId);
  }

  @VisibleForTesting
  protected void registerShuffleServers(
      String appId,
      int shuffleId,
      Map<ShuffleServerInfo,
      List<PartitionRange>> serverToPartitionRanges,
      RemoteStorageInfo remoteStorage) {
    if (serverToPartitionRanges == null || serverToPartitionRanges.isEmpty()) {
      return;
    }
    LOG.info("Start to register shuffleId[" + shuffleId + "]");
    long start = System.currentTimeMillis();
    Set<Map.Entry<ShuffleServerInfo, List<PartitionRange>>> entries = serverToPartitionRanges.entrySet();
    entries.stream()
        .forEach(entry -> {
          shuffleWriteClient.registerShuffle(
              entry.getKey(),
              appId,
              shuffleId,
              entry.getValue(),
              remoteStorage,
              dataDistributionType
          );
        });
    LOG.info("Finish register shuffleId[" + shuffleId + "] with " + (System.currentTimeMillis() - start) + " ms");
  }

  @VisibleForTesting
  protected void registerCoordinator() {
    String coordinators = sparkConf.get(RssSparkConfig.RSS_COORDINATOR_QUORUM.key());
    LOG.info("Start Registering coordinators {}", coordinators);
    shuffleWriteClient.registerCoordinators(coordinators);
  }

  @VisibleForTesting
  public SparkConf getSparkConf() {
    return sparkConf;
  }

  private synchronized void startHeartbeat() {
    shuffleWriteClient.registerApplicationInfo(id.get(), heartbeatTimeout, user);
    if (!heartbeatStarted) {
      heartBeatScheduledExecutorService.scheduleAtFixedRate(
          () -> {
            try {
              String appId = id.get();
              shuffleWriteClient.sendAppHeartbeat(appId, heartbeatTimeout);
              LOG.info("Finish send heartbeat to coordinator and servers");
            } catch (Exception e) {
              LOG.warn("Fail to send heartbeat to coordinator and servers", e);
            }
          },
          heartbeatInterval / 2,
          heartbeatInterval,
          TimeUnit.MILLISECONDS);
      heartbeatStarted = true;
    }
  }

  public void postEvent(AddBlockEvent addBlockEvent) {
    if (eventLoop != null) {
      eventLoop.post(addBlockEvent);
    }
  }

  public Set<Long> getFailedBlockIds(String taskId) {
    Set<Long> result = taskToFailedBlockIds.get(taskId);
    if (result == null) {
      result = Sets.newHashSet();
    }
    return result;
  }

  public Set<Long> getSuccessBlockIds(String taskId) {
    Set<Long> result = taskToSuccessBlockIds.get(taskId);
    if (result == null) {
      result = Sets.newHashSet();
    }
    return result;
  }

  static class ReadMetrics extends ShuffleReadMetrics {
    private ShuffleReadMetricsReporter reporter;

    ReadMetrics(ShuffleReadMetricsReporter reporter) {
      this.reporter = reporter;
    }

    @Override
    public void incRemoteBytesRead(long v) {
      reporter.incRemoteBytesRead(v);
    }

    @Override
    public void incFetchWaitTime(long v) {
      reporter.incFetchWaitTime(v);
    }

    @Override
    public void incRecordsRead(long v) {
      reporter.incRecordsRead(v);
    }
  }

  static class WriteMetrics extends ShuffleWriteMetrics {
    private ShuffleWriteMetricsReporter reporter;

    WriteMetrics(ShuffleWriteMetricsReporter reporter) {
      this.reporter = reporter;
    }

    @Override
    public void incBytesWritten(long v) {
      reporter.incBytesWritten(v);
    }

    @Override
    public void incRecordsWritten(long v) {
      reporter.incRecordsWritten(v);
    }

    @Override
    public void incWriteTime(long v) {
      reporter.incWriteTime(v);
    }
  }

  @VisibleForTesting
  public void setAppId(String appId) {
    this.id = new AtomicReference<>(appId);
  }

  public String getId() {
    return id.get();
  }

  public boolean markFailedTask(String taskId) {
    LOG.info("Mark the task: {} failed.", taskId);
    failedTaskIds.add(taskId);
    return true;
  }

  public boolean isValidTask(String taskId) {
    return !failedTaskIds.contains(taskId);
  }
}
