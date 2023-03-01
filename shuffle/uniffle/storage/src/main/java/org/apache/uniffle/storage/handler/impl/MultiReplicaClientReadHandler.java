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

import org.apache.uniffle.common.BufferSegment;
import org.apache.uniffle.common.ShuffleDataResult;
import org.apache.uniffle.common.ShuffleServerInfo;
import org.apache.uniffle.common.exception.RssException;
import org.apache.uniffle.common.util.RssUtils;
import org.apache.uniffle.storage.handler.api.ClientReadHandler;

public class MultiReplicaClientReadHandler extends AbstractClientReadHandler {

  private static final Logger LOG = LoggerFactory.getLogger(MultiReplicaClientReadHandler.class);

  private final List<ClientReadHandler> handlers;
  private final List<ShuffleServerInfo> shuffleServerInfos;
  private final Roaring64NavigableMap blockIdBitmap;
  private final Roaring64NavigableMap processedBlockIds;

  private int readHandlerIndex;

  public MultiReplicaClientReadHandler(
      List<ClientReadHandler> handlers,
      List<ShuffleServerInfo> shuffleServerInfos,
      Roaring64NavigableMap blockIdBitmap,
      Roaring64NavigableMap processedBlockIds) {
    this.handlers = handlers;
    this.blockIdBitmap = blockIdBitmap;
    this.processedBlockIds = processedBlockIds;
    this.shuffleServerInfos = shuffleServerInfos;
  }

  @Override
  public ShuffleDataResult readShuffleData() {
    ClientReadHandler handler;
    ShuffleDataResult result = null;
    do {
      if (readHandlerIndex >= handlers.size()) {
        return result;
      }
      handler = handlers.get(readHandlerIndex);
      try {
        result = handler.readShuffleData();
      } catch (Exception e) {
        LOG.warn("Failed to read a replica from [{}] due to ",
            shuffleServerInfos.get(readHandlerIndex).getId(), e);
      }
      if (result != null && !result.isEmpty()) {
        return result;
      } else {
        try {
          RssUtils.checkProcessedBlockIds(blockIdBitmap, processedBlockIds);
          return result;
        } catch (RssException e) {
          LOG.warn("Finished read from [{}], but haven't finished read all the blocks.",
              shuffleServerInfos.get(readHandlerIndex).getId(), e);
        }
        readHandlerIndex++;
      }
    } while (true);
  }

  @Override
  public void updateConsumedBlockInfo(BufferSegment bs, boolean isSkippedMetrics) {
    super.updateConsumedBlockInfo(bs, isSkippedMetrics);
    handlers.get(Math.min(readHandlerIndex, handlers.size() - 1)).updateConsumedBlockInfo(bs, isSkippedMetrics);
  }

  @Override
  public void logConsumedBlockInfo() {
    super.logConsumedBlockInfo();
    handlers.forEach(ClientReadHandler::logConsumedBlockInfo);
  }
}
