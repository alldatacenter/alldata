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
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import io.netty.buffer.ByteBuf;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.celeborn.client.read.RssInputStream;
import org.apache.celeborn.common.CelebornConf;
import org.apache.celeborn.common.identity.UserIdentifier;
import org.apache.celeborn.common.protocol.PartitionLocation;
import org.apache.celeborn.common.rpc.RpcEndpointRef;
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
      RpcEndpointRef driverRef, CelebornConf conf, UserIdentifier userIdentifier) {
    if (null == _instance || !initialized) {
      synchronized (ShuffleClient.class) {
        if (null == _instance) {
          // During the execution of Spark tasks, each task may be interrupted due to speculative
          // tasks. If the Task is interrupted while obtaining the ShuffleClient and the
          // ShuffleClient is building a singleton, it may cause the MetaServiceEndpoint to not be
          // assigned. An Executor will only construct a ShuffleClient singleton once. At this time,
          // when communicating with MetaService, it will cause a NullPointerException.
          _instance = new ShuffleClientImpl(conf, userIdentifier);
          _instance.setupMetaServiceRef(driverRef);
          initialized = true;
        } else if (!initialized) {
          _instance.shutdown();
          _instance = new ShuffleClientImpl(conf, userIdentifier);
          _instance.setupMetaServiceRef(driverRef);
          initialized = true;
        }
      }
    }
    return _instance;
  }

  public static ShuffleClient get(
      String driverHost, int port, CelebornConf conf, UserIdentifier userIdentifier) {
    if (null == _instance || !initialized) {
      synchronized (ShuffleClient.class) {
        if (null == _instance) {
          // During the execution of Spark tasks, each task may be interrupted due to speculative
          // tasks. If the Task is interrupted while obtaining the ShuffleClient and the
          // ShuffleClient is building a singleton, it may cause the MetaServiceEndpoint to not be
          // assigned. An Executor will only construct a ShuffleClient singleton once. At this time,
          // when communicating with MetaService, it will cause a NullPointerException.
          _instance = new ShuffleClientImpl(conf, userIdentifier);
          _instance.setupMetaServiceRef(driverHost, port);
          initialized = true;
        } else if (!initialized) {
          _instance.shutdown();
          _instance = new ShuffleClientImpl(conf, userIdentifier);
          _instance.setupMetaServiceRef(driverHost, port);
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
          Configuration hdfsConfiguration = new Configuration();
          // enable fs cache to avoid too many fs instances
          hdfsConfiguration.set("fs.hdfs.impl.disable.cache", "false");
          logger.info(
              "Celeborn client will ignore cluster"
                  + " settings about fs.hdfs.impl.disable.cache and set it to false");
          try {
            hdfsFs = FileSystem.get(hdfsConfiguration);
          } catch (IOException e) {
            System.err.println("Rss initialize hdfs failed.");
            e.printStackTrace(System.err);
          }
        }
      }
    }
    return hdfsFs;
  }

  public abstract void setupMetaServiceRef(String host, int port);

  public abstract void setupMetaServiceRef(RpcEndpointRef endpointRef);

  // Write data to a specific reduce partition
  public abstract int pushData(
      String applicationId,
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
      String applicationId,
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

  public abstract void pushMergedData(String applicationId, int shuffleId, int mapId, int attemptId)
      throws IOException;

  // Report partition locations written by the completed map task of ReducePartition Shuffle Type
  public abstract void mapperEnd(
      String applicationId, int shuffleId, int mapId, int attemptId, int numMappers)
      throws IOException;

  // Report partition locations written by the completed map task of MapPartition Shuffle Type
  public abstract void mapPartitionMapperEnd(
      String applicationId,
      int shuffleId,
      int mapId,
      int attemptId,
      int numMappers,
      int partitionId)
      throws IOException;

  // Cleanup states of the map task
  public abstract void cleanup(String applicationId, int shuffleId, int mapId, int attemptId);

  // Reduce side read partition which is deduplicated by mapperId+mapperAttemptNum+batchId, batchId
  // is a self-incrementing variable hidden in the implementation when sending data.
  public abstract RssInputStream readPartition(
      String applicationId,
      int shuffleId,
      int partitionId,
      int attemptNumber,
      int startMapIndex,
      int endMapIndex)
      throws IOException;

  public abstract RssInputStream readPartition(
      String applicationId, int shuffleId, int partitionId, int attemptNumber) throws IOException;

  public abstract boolean unregisterShuffle(String applicationId, int shuffleId, boolean isDriver);

  public abstract void shutdown();

  // Write data to a specific map partition, input data's type is Bytebuf.
  // data's type is Bytebuf to avoid copy between application and netty
  // closecallback will do some clean operations like memory release.
  public abstract int pushDataToLocation(
      String applicationId,
      int shuffleId,
      int mapId,
      int attemptId,
      int partitionId,
      ByteBuf data,
      PartitionLocation location,
      Runnable closeCallBack)
      throws IOException;

  public abstract Optional<PartitionLocation> regionStart(
      String applicationId,
      int shuffleId,
      int mapId,
      int attemptId,
      PartitionLocation location,
      int currentRegionIdx,
      boolean isBroadcast)
      throws IOException;

  public abstract void regionFinish(
      String applicationId, int shuffleId, int mapId, int attemptId, PartitionLocation location)
      throws IOException;

  public abstract void pushDataHandShake(
      String applicationId,
      int shuffleId,
      int mapId,
      int attemptId,
      int numPartitions,
      int bufferSize,
      PartitionLocation location)
      throws IOException;

  public abstract PartitionLocation registerMapPartitionTask(
      String appId, int shuffleId, int numMappers, int mapId, int attemptId) throws IOException;

  public abstract ConcurrentHashMap<Integer, PartitionLocation> getPartitionLocation(
      String applicationId, int shuffleId, int numMappers, int numPartitions);

  public abstract PushState getPushState(String mapKey);
}
