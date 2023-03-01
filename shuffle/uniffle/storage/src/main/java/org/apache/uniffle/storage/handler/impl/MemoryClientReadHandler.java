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

package org.apache.uniffle.storage.handler.impl;

import java.util.List;

import org.roaringbitmap.longlong.Roaring64NavigableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.uniffle.client.api.ShuffleServerClient;
import org.apache.uniffle.client.request.RssGetInMemoryShuffleDataRequest;
import org.apache.uniffle.client.response.RssGetInMemoryShuffleDataResponse;
import org.apache.uniffle.common.BufferSegment;
import org.apache.uniffle.common.ShuffleDataResult;
import org.apache.uniffle.common.exception.RssException;
import org.apache.uniffle.common.util.Constants;


public class MemoryClientReadHandler extends AbstractClientReadHandler {

  private static final Logger LOG = LoggerFactory.getLogger(MemoryClientReadHandler.class);
  private long lastBlockId = Constants.INVALID_BLOCK_ID;
  private ShuffleServerClient shuffleServerClient;
  private Roaring64NavigableMap expectTaskIds;
  
  public MemoryClientReadHandler(
      String appId,
      int shuffleId,
      int partitionId,
      int readBufferSize,
      ShuffleServerClient shuffleServerClient,
      Roaring64NavigableMap expectTaskIds) {
    this.appId = appId;
    this.shuffleId = shuffleId;
    this.partitionId = partitionId;
    this.readBufferSize = readBufferSize;
    this.shuffleServerClient = shuffleServerClient;
    this.expectTaskIds = expectTaskIds;
  }

  @Override
  public ShuffleDataResult readShuffleData() {
    ShuffleDataResult result = null;

    RssGetInMemoryShuffleDataRequest request = new RssGetInMemoryShuffleDataRequest(
        appId,
        shuffleId,
        partitionId,
        lastBlockId,
        readBufferSize,
        expectTaskIds
    );

    try {
      RssGetInMemoryShuffleDataResponse response =
          shuffleServerClient.getInMemoryShuffleData(request);
      result = new ShuffleDataResult(response.getData(), response.getBufferSegments());
    } catch (Exception e) {
      // todo: fault tolerance solution should be added
      throw new RssException("Failed to read in memory shuffle data with "
          + shuffleServerClient.getClientInfo() + " due to " + e);
    }

    // update lastBlockId for next rpc call
    if (!result.isEmpty()) {
      List<BufferSegment> bufferSegments = result.getBufferSegments();
      lastBlockId = bufferSegments.get(bufferSegments.size() - 1).getBlockId();
    }

    return result;
  }
}
