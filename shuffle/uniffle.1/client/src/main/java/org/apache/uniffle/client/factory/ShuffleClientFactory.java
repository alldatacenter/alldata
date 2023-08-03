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

package org.apache.uniffle.client.factory;

import org.apache.uniffle.client.api.ShuffleReadClient;
import org.apache.uniffle.client.api.ShuffleWriteClient;
import org.apache.uniffle.client.impl.ShuffleReadClientImpl;
import org.apache.uniffle.client.impl.ShuffleWriteClientImpl;
import org.apache.uniffle.client.request.CreateShuffleReadClientRequest;

public class ShuffleClientFactory {

  private static final ShuffleClientFactory INSTANCE = new ShuffleClientFactory();

  private ShuffleClientFactory() {
  }

  public static ShuffleClientFactory getInstance() {
    return INSTANCE;
  }

  /**
   * Only for MR engine, which won't used to unregister to remote shuffle-servers
   */
  public ShuffleWriteClient createShuffleWriteClient(
      String clientType, int retryMax, long retryIntervalMax, int heartBeatThreadNum,
      int replica, int replicaWrite, int replicaRead, boolean replicaSkipEnabled, int dataTransferPoolSize,
      int dataCommitPoolSize) {
    return createShuffleWriteClient(clientType, retryMax, retryIntervalMax, heartBeatThreadNum, replica,
        replicaWrite, replicaRead, replicaSkipEnabled, dataTransferPoolSize, dataCommitPoolSize, 10, 10);
  }

  public ShuffleWriteClient createShuffleWriteClient(
      String clientType, int retryMax, long retryIntervalMax, int heartBeatThreadNum,
      int replica, int replicaWrite, int replicaRead, boolean replicaSkipEnabled, int dataTransferPoolSize,
      int dataCommitPoolSize, int unregisterThreadPoolSize, int unregisterRequestTimeoutSec) {
    // If replica > replicaWrite, blocks maybe be sent for 2 rounds.
    // We need retry less times in this case for let the first round fail fast.
    if (replicaSkipEnabled && replica > replicaWrite) {
      retryMax = retryMax / 2;
    }
    return new ShuffleWriteClientImpl(
        clientType,
        retryMax,
        retryIntervalMax,
        heartBeatThreadNum,
        replica,
        replicaWrite,
        replicaRead,
        replicaSkipEnabled,
        dataTransferPoolSize,
        dataCommitPoolSize,
        unregisterThreadPoolSize,
        unregisterRequestTimeoutSec
    );
  }

  public ShuffleReadClient createShuffleReadClient(CreateShuffleReadClientRequest request) {
    return new ShuffleReadClientImpl(
        request.getStorageType(),
        request.getAppId(),
        request.getShuffleId(),
        request.getPartitionId(),
        request.getIndexReadLimit(),
        request.getPartitionNumPerRange(),
        request.getPartitionNum(),
        request.getReadBufferSize(),
        request.getBasePath(),
        request.getBlockIdBitmap(),
        request.getTaskIdBitmap(),
        request.getShuffleServerInfoList(),
        request.getHadoopConf(),
        request.getIdHelper(),
        request.getShuffleDataDistributionType(),
        request.isExpectedTaskIdsBitmapFilterEnable()
    );
  }
}
