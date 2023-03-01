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
import java.nio.ByteBuffer;
import java.text.DecimalFormat;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.mapred.Counters;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.TaskStatus;
import org.apache.hadoop.mapreduce.TaskAttemptID;
import org.apache.hadoop.mapreduce.TaskID;
import org.apache.hadoop.mapreduce.TaskType;
import org.apache.hadoop.util.Progress;

import org.apache.uniffle.client.api.ShuffleReadClient;
import org.apache.uniffle.client.response.CompressedShuffleBlock;
import org.apache.uniffle.common.compression.Codec;
import org.apache.uniffle.common.config.RssConf;
import org.apache.uniffle.common.exception.RssException;
import org.apache.uniffle.common.util.ByteUnit;

public class RssFetcher<K,V> {

  private static final Log LOG = LogFactory.getLog(RssFetcher.class);

  private final Reporter reporter;

  private enum ShuffleErrors {
    IO_ERROR, WRONG_LENGTH, BAD_ID, WRONG_MAP,
    CONNECTION, WRONG_REDUCE
  }

  private static final String SHUFFLE_ERR_GRP_NAME = "Shuffle Errors";
  private static final double BYTES_PER_MILLIS_TO_MBS = 1000d / (ByteUnit.MiB.toBytes(1));
  private final DecimalFormat mbpsFormat = new DecimalFormat("0.00");

  private final JobConf jobConf;
  private final Counters.Counter connectionErrs;
  private final Counters.Counter ioErrs;
  private final Counters.Counter wrongLengthErrs;
  private final Counters.Counter badIdErrs;
  private final Counters.Counter wrongMapErrs;
  private final Counters.Counter wrongReduceErrs;
  private final TaskStatus status;
  private final MergeManager<K,V> merger;
  private final Progress progress;
  private final ShuffleClientMetrics metrics;
  private long totalBlockCount;
  private long copyBlockCount = 0;

  private volatile boolean stopped = false;

  private ShuffleReadClient shuffleReadClient;
  private long readTime = 0;
  private long decompressTime = 0;
  private long serializeTime = 0;
  private long waitTime = 0;
  private long copyTime = 0;  // the sum of readTime + decompressTime + serializeTime + waitTime
  private long unCompressionLength = 0;
  private final TaskAttemptID reduceId;
  private int uniqueMapId = 0;

  private boolean hasPendingData = false;
  private long startWait;
  private int waitCount = 0;
  private byte[] uncompressedData = null;
  private RssConf rssConf;
  private Codec codec;

  RssFetcher(JobConf job, TaskAttemptID reduceId,
      TaskStatus status,
      MergeManager<K, V> merger,
      Progress progress,
      Reporter reporter, ShuffleClientMetrics metrics,
      ShuffleReadClient shuffleReadClient,
      long totalBlockCount,
      RssConf rssConf) {
    this.jobConf = job;
    this.reporter = reporter;
    this.status = status;
    this.merger = merger;
    this.progress = progress;
    this.metrics = metrics;
    this.reduceId = reduceId;
    ioErrs = reporter.getCounter(SHUFFLE_ERR_GRP_NAME,
        RssFetcher.ShuffleErrors.IO_ERROR.toString());
    wrongLengthErrs = reporter.getCounter(SHUFFLE_ERR_GRP_NAME,
        RssFetcher.ShuffleErrors.WRONG_LENGTH.toString());
    badIdErrs = reporter.getCounter(SHUFFLE_ERR_GRP_NAME,
        RssFetcher.ShuffleErrors.BAD_ID.toString());
    wrongMapErrs = reporter.getCounter(SHUFFLE_ERR_GRP_NAME,
        RssFetcher.ShuffleErrors.WRONG_MAP.toString());
    connectionErrs = reporter.getCounter(SHUFFLE_ERR_GRP_NAME,
        RssFetcher.ShuffleErrors.CONNECTION.toString());
    wrongReduceErrs = reporter.getCounter(SHUFFLE_ERR_GRP_NAME,
        RssFetcher.ShuffleErrors.WRONG_REDUCE.toString());

    this.shuffleReadClient = shuffleReadClient;
    this.totalBlockCount = totalBlockCount;

    this.rssConf = rssConf;
    this.codec = Codec.newInstance(rssConf);
  }

  public void fetchAllRssBlocks() throws IOException, InterruptedException {
    while (!stopped) {
      try {
        // If merge is on, block
        merger.waitForResource();
        // Do shuffle
        metrics.threadBusy();
        copyFromRssServer();
      } finally {
        metrics.threadFree();
      }
    }
  }

  @VisibleForTesting
  public void copyFromRssServer() throws IOException {
    CompressedShuffleBlock compressedBlock = null;
    ByteBuffer compressedData = null;
    // fetch a block
    if (!hasPendingData) {
      final long startFetch = System.currentTimeMillis();
      compressedBlock = shuffleReadClient.readShuffleBlockData();
      if (compressedBlock != null) {
        compressedData = compressedBlock.getByteBuffer();
      }
      long fetchDuration = System.currentTimeMillis() - startFetch;
      readTime += fetchDuration;
    }

    // uncompress the block
    if (!hasPendingData && compressedData != null) {
      final long startDecompress = System.currentTimeMillis();
      int uncompressedLen = compressedBlock.getUncompressLength();
      ByteBuffer decompressedBuffer = ByteBuffer.allocate(uncompressedLen);
      codec.decompress(compressedData, uncompressedLen, decompressedBuffer, 0);
      uncompressedData = decompressedBuffer.array();
      unCompressionLength += compressedBlock.getUncompressLength();
      long decompressDuration = System.currentTimeMillis() - startDecompress;
      decompressTime += decompressDuration;
    }

    if (uncompressedData != null) {
      // start to merge
      final long startSerialization = System.currentTimeMillis();
      if (issueMapOutputMerge()) {
        long serializationDuration = System.currentTimeMillis() - startSerialization;
        serializeTime += serializationDuration;
        // if reserve successes, reset status for next fetch
        if (hasPendingData) {
          waitTime += System.currentTimeMillis() - startWait;
        }
        hasPendingData = false;
        uncompressedData = null;
      } else {
        // if reserve fail, return and wait
        startWait = System.currentTimeMillis();
        return;
      }

      // update some status
      copyBlockCount++;
      copyTime = readTime + decompressTime + serializeTime + waitTime;
      updateStatus();
      reporter.progress();
    } else {
      // finish reading data, close related reader and check data consistent
      shuffleReadClient.close();
      shuffleReadClient.checkProcessedBlockIds();
      shuffleReadClient.logStatics();
      metrics.inputBytes(unCompressionLength);
      LOG.info("reduce task " + reduceId.toString() + " cost " + readTime + " ms to fetch and "
          + decompressTime + " ms to decompress with unCompressionLength["
          + unCompressionLength + "] and " + serializeTime + " ms to serialize and "
          + waitTime + " ms to wait resource");
      stopFetch();
    }
  }

  private boolean issueMapOutputMerge() throws IOException {
    // Allocate a MapOutput (either in-memory or on-disk) to put uncompressed block
    // In Rss, a MapOutput is sent as multiple blocks, so the reducer needs to
    // treat each "block" as a faked "mapout".
    // To avoid name conflicts, we use getNextUniqueTaskAttemptID instead.
    // It will generate a unique TaskAttemptID(increased_seq++, 0).
    TaskAttemptID mapId = getNextUniqueTaskAttemptID();
    MapOutput<K, V> mapOutput = null;
    try {
      mapOutput = merger.reserve(mapId, uncompressedData.length, 0);
    } catch (IOException ioe) {
      // kill this reduce attempt
      ioErrs.increment(1);
      throw ioe;
    }
    // Check if we can shuffle *now* ...
    if (mapOutput == null) {
      LOG.info("RssMRFetcher" + " - MergeManager returned status WAIT ...");
      // Not an error but wait to process data.
      // Use a retry flag to avoid re-fetch and re-uncompress.
      hasPendingData = true;
      waitCount++;
      return false;
    }

    // write data to mapOutput
    try {
      RssBypassWriter.write(mapOutput, uncompressedData);
      // let the merger knows this block is ready for merging
      mapOutput.commit();
      if (mapOutput instanceof OnDiskMapOutput) {
        LOG.info("Reduce: " + reduceId + " allocates disk to accept block "
            + " with byte sizes: " + uncompressedData.length);
      }
    } catch (Throwable t) {
      ioErrs.increment(1);
      mapOutput.abort();
      throw new RssException("Reduce: " + reduceId + " cannot write block to "
          + mapOutput.getClass().getSimpleName() + " due to: " + t.getClass().getName());
    }
    return true;
  }

  private TaskAttemptID getNextUniqueTaskAttemptID() {
    TaskID taskID = new TaskID(reduceId.getJobID(), TaskType.MAP, uniqueMapId++);
    return new TaskAttemptID(taskID, 0);
  }

  private void stopFetch() {
    stopped = true;
  }

  private void updateStatus() {
    progress.set((float) copyBlockCount / totalBlockCount);
    String statusString = copyBlockCount + " / " + totalBlockCount + " copied.";
    status.setStateString(statusString);

    if (copyTime == 0) {
      copyTime = 1;
    }
    double bytesPerMillis = (double) unCompressionLength / copyTime;
    double transferRate = bytesPerMillis * BYTES_PER_MILLIS_TO_MBS;

    progress.setStatus("copy(" + copyBlockCount + " of " + totalBlockCount + " at "
        + mbpsFormat.format(transferRate) + " MB/s)");
  }

  @VisibleForTesting
  public int getRetryCount() {
    return waitCount;
  }
}
