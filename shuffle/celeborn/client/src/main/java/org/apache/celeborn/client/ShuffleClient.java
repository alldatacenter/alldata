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

package org.apache.celeborn.client;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.hadoop.fs.FileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.celeborn.client.read.CelebornInputStream;
import org.apache.celeborn.common.CelebornConf;
import org.apache.celeborn.common.identity.UserIdentifier;
import org.apache.celeborn.common.protocol.PartitionLocation;
import org.apache.celeborn.common.rpc.RpcEndpointRef;
import org.apache.celeborn.common.util.CelebornHadoopUtils;
import org.apache.celeborn.common.write.PushState;

/**
 * ShuffleClient may be a process singleton, the specific PartitionLocation should be hidden in the
 * implementation
 */
public abstract class ShuffleClient {
  private static volatile ShuffleClient _instance;
  private static volatile boolean initialized = false;
  private static volatile FileSystem hdfsFs;
  private static Logger logger = LoggerFactory.getLogger(ShuffleClient.class);

  // for testing
  public static void reset() {
    _instance = null;
    initialized = false;
    hdfsFs = null;
  }

  protected ShuffleClient() {}

  public static ShuffleClient get(
      String appUniqueId,
      String driverHost,
      int port,
      CelebornConf conf,
      UserIdentifier userIdentifier,
      boolean isDriver) {
    if (null == _instance || !initialized) {
      synchronized (ShuffleClient.class) {
        if (null == _instance) {
          // During the execution of Spark tasks, each task may be interrupted due to speculative
          // tasks. If the Task is interrupted while obtaining the ShuffleClient and the
          // ShuffleClient is building a singleton, it may cause the LifecycleManagerEndpoint to not
          // be
          // assigned. An Executor will only construct a ShuffleClient singleton once. At this time,
          // when communicating with LifecycleManager, it will cause a NullPointerException.
          _instance = new ShuffleClientImpl(appUniqueId, conf, userIdentifier, isDriver);
          _instance.setupLifecycleManagerRef(driverHost, port);
          initialized = true;
        } else if (!initialized) {
          _instance.shutdown();
          _instance = new ShuffleClientImpl(appUniqueId, conf, userIdentifier, isDriver);
          _instance.setupLifecycleManagerRef(driverHost, port);
          initialized = true;
        }
      }
    }
    return _instance;
  }

  public static FileSystem getHdfsFs(CelebornConf conf) {
    if (null == hdfsFs) {
      synchronized (ShuffleClient.class) {
        if (null == hdfsFs) {
          try {
            hdfsFs = CelebornHadoopUtils.getHadoopFS(conf);
          } catch (Exception e) {
            logger.error("Celeborn initialize HDFS failed.", e);
          }
        }
      }
    }
    return hdfsFs;
  }

  public abstract void setupLifecycleManagerRef(String host, int port);

  public abstract void setupLifecycleManagerRef(RpcEndpointRef endpointRef);

  // Write data to a specific reduce partition
  public abstract int pushData(
      int shuffleId,
      int mapId,
      int attemptId,
      int partitionId,
      byte[] data,
      int offset,
      int length,
      int numMappers,
      int numPartitions)
      throws IOException;

  public abstract void prepareForMergeData(int shuffleId, int mapId, int attemptId)
      throws IOException;

  public abstract int mergeData(
      int shuffleId,
      int mapId,
      int attemptId,
      int partitionId,
      byte[] data,
      int offset,
      int length,
      int numMappers,
      int numPartitions)
      throws IOException;

  public abstract void pushMergedData(int shuffleId, int mapId, int attemptId) throws IOException;

  // Report partition locations written by the completed map task of ReducePartition Shuffle Type
  public abstract void mapperEnd(int shuffleId, int mapId, int attemptId, int numMappers)
      throws IOException;

  // Report partition locations written by the completed map task of MapPartition Shuffle Type
  public abstract void mapPartitionMapperEnd(
      int shuffleId, int mapId, int attemptId, int numMappers, int partitionId) throws IOException;

  // Cleanup states of the map task
  public abstract void cleanup(int shuffleId, int mapId, int attemptId);

  // Reduce side read partition which is deduplicated by mapperId+mapperAttemptNum+batchId, batchId
  // is a self-incrementing variable hidden in the implementation when sending data.
  public abstract CelebornInputStream readPartition(
      int shuffleId, int partitionId, int attemptNumber, int startMapIndex, int endMapIndex)
      throws IOException;

  public abstract CelebornInputStream readPartition(
      int shuffleId, int partitionId, int attemptNumber) throws IOException;

  public abstract boolean unregisterShuffle(int shuffleId, boolean isDriver);

  public abstract void shutdown();

  public abstract PartitionLocation registerMapPartitionTask(
      int shuffleId, int numMappers, int mapId, int attemptId, int partitionId) throws IOException;

  public abstract ConcurrentHashMap<Integer, PartitionLocation> getPartitionLocation(
      int shuffleId, int numMappers, int numPartitions);

  public abstract PushState getPushState(String mapKey);
}
