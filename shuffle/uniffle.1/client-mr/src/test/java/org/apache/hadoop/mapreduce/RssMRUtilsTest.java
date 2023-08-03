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

package org.apache.hadoop.mapreduce;

import java.util.Map;

import com.google.common.collect.Maps;
import org.apache.hadoop.mapred.JobConf;
import org.junit.jupiter.api.Test;

import org.apache.uniffle.client.util.RssClientConfig;
import org.apache.uniffle.common.exception.RssException;
import org.apache.uniffle.common.util.Constants;
import org.apache.uniffle.storage.util.StorageType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class RssMRUtilsTest {
  
  private static final String EXPECTED_EXCEPTION_MESSAGE = "Exception should be thrown";

  @Test
  public void baskAttemptIdTest() {
    long taskAttemptId = 0x1000ad12;
    JobID jobID = new JobID();
    TaskID taskId =  new TaskID(jobID, TaskType.MAP, (int) taskAttemptId);
    TaskAttemptID mrTaskAttemptId = new TaskAttemptID(taskId, 3);
    boolean isException = false;
    try {
      RssMRUtils.convertTaskAttemptIdToLong(mrTaskAttemptId, 1);
    } catch (RssException e) {
      isException = true;
    }
    assertTrue(isException);
    taskAttemptId = (1 << 20) + 0x123;
    mrTaskAttemptId = RssMRUtils.createMRTaskAttemptId(new JobID(), TaskType.MAP, taskAttemptId, 1);
    long testId = RssMRUtils.convertTaskAttemptIdToLong(mrTaskAttemptId, 1);
    assertEquals(taskAttemptId, testId);
    TaskID taskID = new TaskID(new org.apache.hadoop.mapred.JobID(), TaskType.MAP, (int)(1 << 21));
    mrTaskAttemptId = new TaskAttemptID(taskID, 2);
    isException = false;
    try {
      RssMRUtils.convertTaskAttemptIdToLong(mrTaskAttemptId, 1);
    } catch (RssException e) {
      isException = true;
    }
    assertTrue(isException);
  }

  @Test
  public void blockConvertTest() {
    JobID jobID =  new JobID();
    TaskID taskId =  new TaskID(jobID, TaskType.MAP, 233);
    TaskAttemptID taskAttemptID = new TaskAttemptID(taskId, 1);
    long taskAttemptId = RssMRUtils.convertTaskAttemptIdToLong(taskAttemptID, 1);
    long blockId = RssMRUtils.getBlockId(1, taskAttemptId, 0);
    long newTaskAttemptId = RssMRUtils.getTaskAttemptId(blockId);
    assertEquals(taskAttemptId, newTaskAttemptId);
    blockId = RssMRUtils.getBlockId(2, taskAttemptId, 2);
    newTaskAttemptId = RssMRUtils.getTaskAttemptId(blockId);
    assertEquals(taskAttemptId, newTaskAttemptId);
  }

  @Test
  public void partitionIdConvertBlockTest() {
    JobID jobID =  new JobID();
    TaskID taskId =  new TaskID(jobID, TaskType.MAP, 233);
    TaskAttemptID taskAttemptID = new TaskAttemptID(taskId, 1);
    long taskAttemptId = RssMRUtils.convertTaskAttemptIdToLong(taskAttemptID, 1);
    long mask = (1L << Constants.PARTITION_ID_MAX_LENGTH) - 1;
    for (int partitionId = 0; partitionId <= 3000; partitionId++) {
      for (int seqNo = 0; seqNo <= 10; seqNo++) {
        long blockId = RssMRUtils.getBlockId(Long.valueOf(partitionId), taskAttemptId, seqNo);
        int newPartitionId = Math.toIntExact((blockId >> Constants.TASK_ATTEMPT_ID_MAX_LENGTH) & mask);
        assertEquals(partitionId, newPartitionId);
      }
    }
  }

  @Test
  public void applyDynamicClientConfTest() {
    final JobConf conf = new JobConf();
    Map<String, String> clientConf = Maps.newHashMap();
    String remoteStoragePath = "hdfs://path1";
    String mockKey = "mapreduce.mockKey";
    String mockValue = "v";

    clientConf.put(RssClientConfig.RSS_REMOTE_STORAGE_PATH, remoteStoragePath);
    clientConf.put(RssClientConfig.RSS_CLIENT_TYPE, RssClientConfig.RSS_CLIENT_TYPE_DEFAULT_VALUE);
    clientConf.put(RssClientConfig.RSS_CLIENT_RETRY_MAX,
        Integer.toString(RssClientConfig.RSS_CLIENT_RETRY_MAX_DEFAULT_VALUE));
    clientConf.put(RssClientConfig.RSS_CLIENT_RETRY_INTERVAL_MAX,
        Long.toString(RssClientConfig.RSS_CLIENT_RETRY_INTERVAL_MAX_DEFAULT_VALUE));
    clientConf.put(RssClientConfig.RSS_DATA_REPLICA,
        Integer.toString(RssClientConfig.RSS_DATA_REPLICA_DEFAULT_VALUE));
    clientConf.put(RssClientConfig.RSS_DATA_REPLICA_WRITE,
        Integer.toString(RssClientConfig.RSS_DATA_REPLICA_WRITE_DEFAULT_VALUE));
    clientConf.put(RssClientConfig.RSS_DATA_REPLICA_READ,
        Integer.toString(RssClientConfig.RSS_DATA_REPLICA_READ_DEFAULT_VALUE));
    clientConf.put(RssClientConfig.RSS_HEARTBEAT_INTERVAL,
        Long.toString(RssClientConfig.RSS_HEARTBEAT_INTERVAL_DEFAULT_VALUE));
    clientConf.put(RssClientConfig.RSS_STORAGE_TYPE, StorageType.MEMORY_LOCALFILE_HDFS.name());
    clientConf.put(RssClientConfig.RSS_CLIENT_SEND_CHECK_INTERVAL_MS,
        Long.toString(RssClientConfig.RSS_CLIENT_SEND_CHECK_INTERVAL_MS_DEFAULT_VALUE));
    clientConf.put(RssClientConfig.RSS_CLIENT_SEND_CHECK_TIMEOUT_MS,
        Long.toString(RssClientConfig.RSS_CLIENT_SEND_CHECK_TIMEOUT_MS_DEFAULT_VALUE));
    clientConf.put(RssClientConfig.RSS_PARTITION_NUM_PER_RANGE,
        Integer.toString(RssClientConfig.RSS_PARTITION_NUM_PER_RANGE_DEFAULT_VALUE));
    clientConf.put(RssClientConfig.RSS_INDEX_READ_LIMIT,
        Integer.toString(RssClientConfig.RSS_INDEX_READ_LIMIT_DEFAULT_VALUE));
    clientConf.put(RssClientConfig.RSS_CLIENT_READ_BUFFER_SIZE,
        RssClientConfig.RSS_CLIENT_READ_BUFFER_SIZE_DEFAULT_VALUE);
    clientConf.put(mockKey, mockValue);

    RssMRUtils.applyDynamicClientConf(conf, clientConf);
    assertEquals(remoteStoragePath, conf.get(RssMRConfig.RSS_REMOTE_STORAGE_PATH));
    assertEquals(RssClientConfig.RSS_CLIENT_TYPE_DEFAULT_VALUE,
        conf.get(RssMRConfig.RSS_CLIENT_TYPE));
    assertEquals(Integer.toString(RssClientConfig.RSS_CLIENT_RETRY_MAX_DEFAULT_VALUE),
        conf.get(RssMRConfig.RSS_CLIENT_RETRY_MAX));
    assertEquals(Long.toString(RssClientConfig.RSS_CLIENT_RETRY_INTERVAL_MAX_DEFAULT_VALUE),
        conf.get(RssMRConfig.RSS_CLIENT_RETRY_INTERVAL_MAX));
    assertEquals(Integer.toString(RssClientConfig.RSS_DATA_REPLICA_DEFAULT_VALUE),
        conf.get(RssMRConfig.RSS_DATA_REPLICA));
    assertEquals(Integer.toString(RssClientConfig.RSS_DATA_REPLICA_WRITE_DEFAULT_VALUE),
        conf.get(RssMRConfig.RSS_DATA_REPLICA_WRITE));
    assertEquals(Integer.toString(RssClientConfig.RSS_DATA_REPLICA_READ_DEFAULT_VALUE),
        conf.get(RssMRConfig.RSS_DATA_REPLICA_READ));
    assertEquals(Long.toString(RssClientConfig.RSS_HEARTBEAT_INTERVAL_DEFAULT_VALUE),
        conf.get(RssMRConfig.RSS_HEARTBEAT_INTERVAL));
    assertEquals(StorageType.MEMORY_LOCALFILE_HDFS.name(), conf.get(RssMRConfig.RSS_STORAGE_TYPE));
    assertEquals(Long.toString(RssClientConfig.RSS_CLIENT_SEND_CHECK_INTERVAL_MS_DEFAULT_VALUE),
        conf.get(RssMRConfig.RSS_CLIENT_SEND_CHECK_INTERVAL_MS));
    assertEquals(Long.toString(RssClientConfig.RSS_CLIENT_SEND_CHECK_TIMEOUT_MS_DEFAULT_VALUE),
        conf.get(RssMRConfig.RSS_CLIENT_SEND_CHECK_TIMEOUT_MS));
    assertEquals(Integer.toString(RssClientConfig.RSS_PARTITION_NUM_PER_RANGE_DEFAULT_VALUE),
        conf.get(RssMRConfig.RSS_PARTITION_NUM_PER_RANGE));
    assertEquals(Integer.toString(RssClientConfig.RSS_INDEX_READ_LIMIT_DEFAULT_VALUE),
        conf.get(RssMRConfig.RSS_INDEX_READ_LIMIT));
    assertEquals(RssClientConfig.RSS_CLIENT_READ_BUFFER_SIZE_DEFAULT_VALUE,
        conf.get(RssMRConfig.RSS_CLIENT_READ_BUFFER_SIZE));
    assertEquals(mockValue, conf.get(mockKey));

    String remoteStoragePath2 = "hdfs://path2";
    clientConf = Maps.newHashMap();
    clientConf.put(RssClientConfig.RSS_STORAGE_TYPE, StorageType.MEMORY_HDFS.name());
    clientConf.put(RssMRConfig.RSS_REMOTE_STORAGE_PATH, remoteStoragePath2);
    clientConf.put(mockKey, "won't be rewrite");
    clientConf.put(RssClientConfig.RSS_CLIENT_RETRY_MAX, "99999");
    RssMRUtils.applyDynamicClientConf(conf, clientConf);
    // overwrite
    assertEquals(remoteStoragePath2, conf.get(RssMRConfig.RSS_REMOTE_STORAGE_PATH));
    assertEquals(StorageType.MEMORY_HDFS.name(), conf.get(RssMRConfig.RSS_STORAGE_TYPE));
    // won't be overwrite
    assertEquals(mockValue, conf.get(mockKey));
    assertEquals(Integer.toString(RssClientConfig.RSS_CLIENT_RETRY_MAX_DEFAULT_VALUE),
        conf.get(RssMRConfig.RSS_CLIENT_RETRY_MAX));
  }

  @Test
  public void testEstimateTaskConcurrency() {
    JobConf jobConf = new JobConf();
    jobConf.setInt("mapreduce.job.maps", 500);
    jobConf.setInt("mapreduce.job.reduces", 20);
    assertEquals(495, RssMRUtils.estimateTaskConcurrency(jobConf));

    jobConf.setDouble(Constants.MR_SLOW_START, 1.0);
    assertEquals(500, RssMRUtils.estimateTaskConcurrency(jobConf));
    jobConf.setInt(Constants.MR_MAP_LIMIT, 200);
    jobConf.setInt(Constants.MR_REDUCE_LIMIT, 200);
    assertEquals(200, RssMRUtils.estimateTaskConcurrency(jobConf));

    jobConf.setDouble("mapreduce.rss.estimate.task.concurrency.dynamic.factor", 0.5);
    assertEquals(100, RssMRUtils.estimateTaskConcurrency(jobConf));
  }

  @Test
  public void testGetRequiredShuffleServerNumber() {
    JobConf jobConf = new JobConf();
    jobConf.setInt("mapreduce.job.maps", 500);
    jobConf.setInt("mapreduce.job.reduces", 20);
    jobConf.setInt(RssMRConfig.RSS_CLIENT_ASSIGNMENT_SHUFFLE_SERVER_NUMBER, 10);
    assertEquals(10, RssMRUtils.getRequiredShuffleServerNumber(jobConf));

    jobConf.setBoolean(RssMRConfig.RSS_ESTIMATE_SERVER_ASSIGNMENT_ENABLED, true);
    assertEquals(10, RssMRUtils.getRequiredShuffleServerNumber(jobConf));

    jobConf.unset(RssMRConfig.RSS_CLIENT_ASSIGNMENT_SHUFFLE_SERVER_NUMBER);
    assertEquals(7, RssMRUtils.getRequiredShuffleServerNumber(jobConf));

    jobConf.setDouble(Constants.MR_SLOW_START, 1.0);
    assertEquals(7, RssMRUtils.getRequiredShuffleServerNumber(jobConf));

    jobConf.setInt(Constants.MR_MAP_LIMIT, 200);
    jobConf.setInt(Constants.MR_REDUCE_LIMIT, 200);
    assertEquals(3, RssMRUtils.getRequiredShuffleServerNumber(jobConf));

    jobConf.setDouble("mapreduce.rss.estimate.task.concurrency.dynamic.factor", 0.5);
    assertEquals(2, RssMRUtils.getRequiredShuffleServerNumber(jobConf));
  }

  @Test
  public void testValidateRssClientConf() {
    JobConf jobConf = new JobConf();
    JobConf rssJobConf = new JobConf();
    rssJobConf.setInt("mapreduce.job.maps", 500);
    rssJobConf.setInt("mapreduce.job.reduces", 20);
    RssMRUtils.validateRssClientConf(rssJobConf, jobConf);
    rssJobConf.setInt(RssMRConfig.RSS_CLIENT_RETRY_MAX, 5);
    rssJobConf.setLong(RssMRConfig.RSS_CLIENT_RETRY_INTERVAL_MAX, 1000L);
    rssJobConf.setLong(RssMRConfig.RSS_CLIENT_SEND_CHECK_TIMEOUT_MS, 4999L);
    try {
      RssMRUtils.validateRssClientConf(rssJobConf, jobConf);
      fail(EXPECTED_EXCEPTION_MESSAGE);
    } catch (IllegalArgumentException e) {
      assertTrue(e.getMessage().contains("should not bigger than"));
    }
  }
}
