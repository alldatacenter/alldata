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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RawKeyValueIterator;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.ShuffleConsumerPlugin;
import org.apache.hadoop.mapred.Task;
import org.apache.hadoop.mapred.TaskStatus;
import org.apache.hadoop.mapred.TaskUmbilicalProtocol;
import org.apache.hadoop.mapreduce.MRIdHelper;
import org.apache.hadoop.mapreduce.RssMRConfig;
import org.apache.hadoop.mapreduce.RssMRUtils;
import org.apache.hadoop.util.Progress;
import org.roaringbitmap.longlong.Roaring64NavigableMap;

import org.apache.uniffle.client.api.ShuffleReadClient;
import org.apache.uniffle.client.api.ShuffleWriteClient;
import org.apache.uniffle.client.factory.ShuffleClientFactory;
import org.apache.uniffle.client.request.CreateShuffleReadClientRequest;
import org.apache.uniffle.common.RemoteStorageInfo;
import org.apache.uniffle.common.ShuffleServerInfo;
import org.apache.uniffle.common.util.UnitConverter;

public class RssShuffle<K, V> implements ShuffleConsumerPlugin<K, V>, ExceptionReporter {

  private static final Log LOG = LogFactory.getLog(RssShuffle.class);

  private static final int MAX_EVENTS_TO_FETCH = 10000;

  private ShuffleConsumerPlugin.Context context;

  private org.apache.hadoop.mapreduce.TaskAttemptID reduceId;
  private JobConf mrJobConf;
  private JobConf rssJobConf;
  private Reporter reporter;
  private ShuffleClientMetrics metrics;
  private TaskUmbilicalProtocol umbilical;

  private MergeManager<K, V> merger;
  private Throwable throwable = null;
  private String throwingThreadName = null;
  private Progress copyPhase;
  private TaskStatus taskStatus;
  private Task reduceTask; //Used for status updates

  private String appId;
  private String storageType;
  private String clientType;
  private int replicaWrite;
  private int replicaRead;
  private int replica;

  private int partitionNum;
  private int partitionNumPerRange;
  private String basePath;
  private int indexReadLimit;
  private int readBufferSize;
  private RemoteStorageInfo remoteStorageInfo;
  private int appAttemptId;

  @Override
  public void init(ShuffleConsumerPlugin.Context context) {
    // mapreduce's builtin init
    this.context = context;

    this.reduceId = context.getReduceId();
    this.mrJobConf = context.getJobConf();
    this.rssJobConf = new JobConf(RssMRConfig.RSS_CONF_FILE);

    this.umbilical = context.getUmbilical();
    this.reporter = context.getReporter();
    this.metrics = new ShuffleClientMetrics(reduceId, mrJobConf);
    this.copyPhase = context.getCopyPhase();
    this.taskStatus = context.getStatus();
    this.reduceTask = context.getReduceTask();

    // rss init
    this.appId = RssMRUtils.getApplicationAttemptId().toString();
    this.appAttemptId = RssMRUtils.getApplicationAttemptId().getAttemptId();
    this.storageType = RssMRUtils.getString(rssJobConf, mrJobConf, RssMRConfig.RSS_STORAGE_TYPE);
    this.replicaWrite = RssMRUtils.getInt(rssJobConf, mrJobConf, RssMRConfig.RSS_DATA_REPLICA_WRITE,
        RssMRConfig.RSS_DATA_REPLICA_WRITE_DEFAULT_VALUE);
    this.replicaRead = RssMRUtils.getInt(rssJobConf, mrJobConf, RssMRConfig.RSS_DATA_REPLICA_READ,
        RssMRConfig.RSS_DATA_REPLICA_READ_DEFAULT_VALUE);
    this.replica = RssMRUtils.getInt(rssJobConf, mrJobConf, RssMRConfig.RSS_DATA_REPLICA,
        RssMRConfig.RSS_DATA_REPLICA_DEFAULT_VALUE);

    this.partitionNum = mrJobConf.getNumReduceTasks();
    this.partitionNumPerRange = RssMRUtils.getInt(rssJobConf, mrJobConf, RssMRConfig.RSS_PARTITION_NUM_PER_RANGE,
        RssMRConfig.RSS_PARTITION_NUM_PER_RANGE_DEFAULT_VALUE);
    this.basePath = RssMRUtils.getString(rssJobConf, mrJobConf, RssMRConfig.RSS_REMOTE_STORAGE_PATH);
    this.indexReadLimit = RssMRUtils.getInt(rssJobConf, mrJobConf, RssMRConfig.RSS_INDEX_READ_LIMIT,
        RssMRConfig.RSS_INDEX_READ_LIMIT_DEFAULT_VALUE);
    this.readBufferSize = (int)UnitConverter.byteStringAsBytes(
        RssMRUtils.getString(rssJobConf, mrJobConf, RssMRConfig.RSS_CLIENT_READ_BUFFER_SIZE,
            RssMRConfig.RSS_CLIENT_READ_BUFFER_SIZE_DEFAULT_VALUE));
    String remoteStorageConf = RssMRUtils.getString(rssJobConf, mrJobConf, RssMRConfig.RSS_REMOTE_STORAGE_CONF, "");
    this.remoteStorageInfo = new RemoteStorageInfo(basePath, remoteStorageConf);
    this.merger = createMergeManager(context);
  }

  protected MergeManager<K, V> createMergeManager(
      ShuffleConsumerPlugin.Context context) {
    boolean useRemoteSpill = RssMRUtils.getBoolean(rssJobConf, mrJobConf,
        RssMRConfig.RSS_REDUCE_REMOTE_SPILL_ENABLED, RssMRConfig.RSS_REDUCE_REMOTE_SPILL_ENABLED_DEFAULT);
    if (useRemoteSpill) {
      // Use minimized replica, because spilled data can be recomputed by reduce task.
      // Instead, we use more retries on HDFS client.
      int replication = RssMRUtils.getInt(rssJobConf, mrJobConf,
          RssMRConfig.RSS_REDUCE_REMOTE_SPILL_REPLICATION,
          RssMRConfig.RSS_REDUCE_REMOTE_SPILL_REPLICATION_DEFAULT);
      int retries = RssMRUtils.getInt(rssJobConf, mrJobConf,
          RssMRConfig.RSS_REDUCE_REMOTE_SPILL_RETRIES,
          RssMRConfig.RSS_REDUCE_REMOTE_SPILL_RETRIES_DEFAULT);
      return new RssRemoteMergeManagerImpl(appId, reduceId, mrJobConf,
        basePath,
        replication,
        retries,
        context.getLocalFS(),
        context.getLocalDirAllocator(), reporter, context.getCodec(),
        context.getCombinerClass(), context.getCombineCollector(),
        context.getSpilledRecordsCounter(),
        context.getReduceCombineInputCounter(),
        context.getMergedMapOutputsCounter(), this, context.getMergePhase(),
        context.getMapOutputFile(),
        getRemoteConf()
      );
    } else {
      return new MergeManagerImpl<K, V>(reduceId, mrJobConf, context.getLocalFS(),
        context.getLocalDirAllocator(), reporter, context.getCodec(),
        context.getCombinerClass(), context.getCombineCollector(),
        context.getSpilledRecordsCounter(),
        context.getReduceCombineInputCounter(),
        context.getMergedMapOutputsCounter(), this, context.getMergePhase(),
        context.getMapOutputFile());
    }
  }

  @Override
  public RawKeyValueIterator run() throws IOException, InterruptedException {

    // get assigned RSS servers
    Set<ShuffleServerInfo> serverInfoSet = RssMRUtils.getAssignedServers(rssJobConf,
        reduceId.getTaskID().getId());
    List<ShuffleServerInfo> serverInfoList = new ArrayList<>();
    for (ShuffleServerInfo server: serverInfoSet) {
      serverInfoList.add(server);
    }

    // just get blockIds from RSS servers
    ShuffleWriteClient writeClient = RssMRUtils.createShuffleClient(mrJobConf);
    Roaring64NavigableMap blockIdBitmap = writeClient.getShuffleResult(
        clientType, serverInfoSet, appId, 0, reduceId.getTaskID().getId());
    writeClient.close();

    // get map-completion events to generate RSS taskIDs
    final RssEventFetcher<K,V> eventFetcher =
        new RssEventFetcher<K,V>(appAttemptId, reduceId, umbilical, mrJobConf, MAX_EVENTS_TO_FETCH);
    Roaring64NavigableMap taskIdBitmap = eventFetcher.fetchAllRssTaskIds();

    LOG.info("In reduce: " + reduceId
        + ", RSS MR client has fetched blockIds and taskIds successfully");

    // start fetcher to fetch blocks from RSS servers
    if (!taskIdBitmap.isEmpty()) {
      LOG.info("In reduce: " + reduceId
          + ", Rss MR client starts to fetch blocks from RSS server");
      JobConf readerJobConf = getRemoteConf();
      boolean expectedTaskIdsBitmapFilterEnable = serverInfoList.size() > 1;
      CreateShuffleReadClientRequest request = new CreateShuffleReadClientRequest(
          appId, 0, reduceId.getTaskID().getId(), storageType, basePath, indexReadLimit, readBufferSize,
          partitionNumPerRange, partitionNum, blockIdBitmap, taskIdBitmap, serverInfoList,
          readerJobConf, new MRIdHelper(), expectedTaskIdsBitmapFilterEnable);
      ShuffleReadClient shuffleReadClient = ShuffleClientFactory.getInstance().createShuffleReadClient(request);
      RssFetcher fetcher = new RssFetcher(mrJobConf, reduceId, taskStatus, merger, copyPhase, reporter, metrics,
          shuffleReadClient, blockIdBitmap.getLongCardinality(), RssMRConfig.toRssConf(rssJobConf));
      fetcher.fetchAllRssBlocks();
      LOG.info("In reduce: " + reduceId
          + ", Rss MR client fetches blocks from RSS server successfully");
    }

    copyPhase.complete();
    taskStatus.setPhase(TaskStatus.Phase.SORT);
    reduceTask.statusUpdate(umbilical);

    // Finish the on-going merges...
    RawKeyValueIterator kvIter = null;
    try {
      kvIter = merger.close();
    } catch (Throwable e) {
      throw new Shuffle.ShuffleError("Error while doing final merge ", e);
    }

    // Sanity check
    synchronized (this) {
      if (throwable != null) {
        throw new Shuffle.ShuffleError("error in shuffle in " + throwingThreadName,
            throwable);
      }
    }

    LOG.info("In reduce: " + reduceId
        + ", Rss MR client returns sorted data to reduce successfully");

    return kvIter;
  }

  private JobConf getRemoteConf() {
    JobConf readerJobConf = new JobConf((mrJobConf));
    if (!remoteStorageInfo.isEmpty()) {
      for (Map.Entry<String, String> entry : remoteStorageInfo.getConfItems().entrySet()) {
        readerJobConf.set(entry.getKey(), entry.getValue());
      }
    }
    return readerJobConf;
  }

  @Override
  public void close() {
  }

  @Override
  public synchronized void reportException(Throwable t) {
    if (throwable == null) {
      throwable = t;
      throwingThreadName = Thread.currentThread().getName();
    }
  }
}
