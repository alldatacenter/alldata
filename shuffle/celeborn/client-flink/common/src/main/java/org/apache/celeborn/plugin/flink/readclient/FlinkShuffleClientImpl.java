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

package org.apache.celeborn.plugin.flink.readclient;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.celeborn.client.ShuffleClientImpl;
import org.apache.celeborn.common.CelebornConf;
import org.apache.celeborn.common.identity.UserIdentifier;
import org.apache.celeborn.common.network.TransportContext;
import org.apache.celeborn.common.network.util.TransportConf;
import org.apache.celeborn.common.protocol.PartitionLocation;
import org.apache.celeborn.common.protocol.TransportModuleConstants;
import org.apache.celeborn.common.util.Utils;
import org.apache.celeborn.plugin.flink.network.FlinkTransportClientFactory;
import org.apache.celeborn.plugin.flink.network.ReadClientHandler;

public class FlinkShuffleClientImpl extends ShuffleClientImpl {
  public static final Logger logger = LoggerFactory.getLogger(FlinkShuffleClientImpl.class);
  private static volatile FlinkShuffleClientImpl _instance;
  private static volatile boolean initialized = false;
  private FlinkTransportClientFactory flinkTransportClientFactory;
  private ReadClientHandler readClientHandler = new ReadClientHandler();

  public static FlinkShuffleClientImpl get(
      String driverHost, int port, CelebornConf conf, UserIdentifier userIdentifier) {
    if (null == _instance || !initialized) {
      synchronized (FlinkShuffleClientImpl.class) {
        if (null == _instance) {
          _instance = new FlinkShuffleClientImpl(driverHost, port, conf, userIdentifier);
          _instance.setupMetaServiceRef(driverHost, port);
          initialized = true;
        } else if (!initialized) {
          _instance.shutdown();
          _instance = new FlinkShuffleClientImpl(driverHost, port, conf, userIdentifier);
          _instance.setupMetaServiceRef(driverHost, port);
          initialized = true;
        }
      }
    }
    return _instance;
  }

  @Override
  public void shutdown() {
    super.shutdown();
    if (flinkTransportClientFactory != null) {
      flinkTransportClientFactory.close();
    }
    if (readClientHandler != null) {
      readClientHandler.close();
    }
  }

  public FlinkShuffleClientImpl(
      String driverHost, int port, CelebornConf conf, UserIdentifier userIdentifier) {
    super(conf, userIdentifier);
    String module = TransportModuleConstants.DATA_MODULE;
    TransportConf dataTransportConf =
        Utils.fromCelebornConf(conf, module, conf.getInt("celeborn." + module + ".io.threads", 8));
    TransportContext context =
        new TransportContext(
            dataTransportConf, readClientHandler, conf.clientCloseIdleConnections());
    this.flinkTransportClientFactory = new FlinkTransportClientFactory(context);
    this.setupMetaServiceRef(driverHost, port);
  }

  public RssBufferStream readBufferedPartition(
      String applicationId,
      int shuffleId,
      int partitionId,
      int subPartitionIndexStart,
      int subPartitionIndexEnd)
      throws IOException {
    String shuffleKey = Utils.makeShuffleKey(applicationId, shuffleId);
    ReduceFileGroups fileGroups = loadFileGroup(applicationId, shuffleKey, shuffleId, partitionId);
    if (fileGroups.partitionGroups.size() == 0
        || !fileGroups.partitionGroups.containsKey(partitionId)) {
      logger.warn("Shuffle data is empty for shuffle {} partitionId {}.", shuffleId, partitionId);
      return RssBufferStream.empty();
    } else {
      return RssBufferStream.create(
          conf,
          flinkTransportClientFactory,
          shuffleKey,
          fileGroups.partitionGroups.get(partitionId).toArray(new PartitionLocation[0]),
          subPartitionIndexStart,
          subPartitionIndexEnd);
    }
  }

  @Override
  protected ReduceFileGroups updateFileGroup(
      String applicationId, String shuffleKey, int shuffleId, int partitionId) throws IOException {
    ReduceFileGroups reduceFileGroups =
        reduceFileGroupsMap.computeIfAbsent(shuffleId, (id) -> new ReduceFileGroups());
    synchronized (reduceFileGroups) {
      if (reduceFileGroups.partitionIds != null
          && reduceFileGroups.partitionIds.contains(partitionId)) {
        logger.debug(
            "use cached file groups for partition: {}",
            Utils.makeReducerKey(applicationId, shuffleId, partitionId));
        return reduceFileGroups;
      } else {
        // refresh file groups
        ReduceFileGroups newGroups = loadFileGroupInternal(applicationId, shuffleKey, shuffleId);
        if (newGroups == null || !newGroups.partitionIds.contains(partitionId)) {
          throw new IOException(
              "shuffle data lost for partition: "
                  + Utils.makeReducerKey(applicationId, shuffleId, partitionId));
        }

        reduceFileGroupsMap.put(shuffleId, newGroups);
        return newGroups;
      }
    }
  }

  public ReadClientHandler getReadClientHandler() {
    return readClientHandler;
  }
}
