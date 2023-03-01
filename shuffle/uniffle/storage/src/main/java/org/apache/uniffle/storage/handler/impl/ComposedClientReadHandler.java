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

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.uniffle.common.BufferSegment;
import org.apache.uniffle.common.ShuffleDataResult;
import org.apache.uniffle.common.ShuffleServerInfo;
import org.apache.uniffle.common.exception.RssException;
import org.apache.uniffle.storage.handler.ClientReadHandlerMetric;
import org.apache.uniffle.storage.handler.api.ClientReadHandler;

/**
 * Composed read handler for all storage types and one replicas.
 * The storage types reading order is as follows: HOT -> WARM -> COLD -> FROZEN
 * @see <a href="https://github.com/apache/incubator-uniffle/pull/276">PR-276</a>
 */
public class ComposedClientReadHandler extends AbstractClientReadHandler {

  private static final Logger LOG = LoggerFactory.getLogger(ComposedClientReadHandler.class);

  private enum Tier {
    HOT, WARM, COLD, FROZEN;

    static final Tier[] VALUES = Tier.values();

    Tier next() {
      return VALUES[this.ordinal() + 1];
    }
  }

  private final ShuffleServerInfo serverInfo;
  private final Map<Tier, Supplier<ClientReadHandler>> supplierMap = new EnumMap<>(Tier.class);
  private final Map<Tier, ClientReadHandler> handlerMap = new EnumMap<>(Tier.class);
  private final Map<Tier, ClientReadHandlerMetric> metricsMap = new EnumMap<>(Tier.class);
  private Tier currentTier = Tier.VALUES[0]; // == Tier.HOT
  private final int numTiers;

  {
    for (Tier tier : Tier.VALUES) {
      metricsMap.put(tier, new ClientReadHandlerMetric());
    }
  }

  public ComposedClientReadHandler(ShuffleServerInfo serverInfo, ClientReadHandler... handlers) {
    Preconditions.checkArgument(handlers.length <= Tier.VALUES.length,
        "Too many handlers, got %d, max %d", handlers.length, Tier.VALUES.length);
    this.serverInfo = serverInfo;
    numTiers = handlers.length;
    for (int i = 0; i < numTiers; i++) {
      handlerMap.put(Tier.VALUES[i], handlers[i]);
    }
  }

  public ComposedClientReadHandler(ShuffleServerInfo serverInfo, List<Supplier<ClientReadHandler>> suppliers) {
    Preconditions.checkArgument(suppliers.size() <= Tier.VALUES.length,
        "Too many suppliers, got %d, max %d", suppliers.size(), Tier.VALUES.length);
    this.serverInfo = serverInfo;
    numTiers = suppliers.size();
    for (int i = 0; i < numTiers; i++) {
      supplierMap.put(Tier.VALUES[i], suppliers.get(i));
    }
  }

  @Override
  public ShuffleDataResult readShuffleData() {
    ClientReadHandler handler = handlerMap.computeIfAbsent(currentTier,
        key -> supplierMap.getOrDefault(key, () -> null).get());
    if (handler == null) {
      throw new RssException("Unexpected null when getting " + currentTier.name() + " handler");
    }
    ShuffleDataResult shuffleDataResult;
    try {
      shuffleDataResult = handler.readShuffleData();
    } catch (Exception e) {
      throw new RssException("Failed to read shuffle data from " + currentTier.name() + " handler", e);
    }
    // when is no data for current handler, and the upmostLevel is not reached,
    // then try next one if there has
    if (shuffleDataResult == null || shuffleDataResult.isEmpty()) {
      if (currentTier.ordinal() + 1 < numTiers) {
        currentTier = currentTier.next();
      } else {
        return null;
      }
      return readShuffleData();
    }

    return shuffleDataResult;
  }

  @Override
  public void close() {
    handlerMap.values().stream().filter(Objects::nonNull).forEach(ClientReadHandler::close);
  }

  @Override
  public void updateConsumedBlockInfo(BufferSegment bs, boolean isSkippedMetrics) {
    if (bs == null) {
      return;
    }
    super.updateConsumedBlockInfo(bs, isSkippedMetrics);
    updateBlockMetric(metricsMap.get(currentTier), bs, isSkippedMetrics);
  }

  @Override
  public void logConsumedBlockInfo() {
    LOG.info(getReadBlockNumInfo());
    LOG.info(getReadLengthInfo());
    LOG.info(getReadUncompressLengthInfo());
  }

  @VisibleForTesting
  public String getReadBlockNumInfo() {
    return getMetricsInfo("blocks", ClientReadHandlerMetric::getReadBlockNum,
        ClientReadHandlerMetric::getSkippedReadBlockNum);
  }

  @VisibleForTesting
  public String getReadLengthInfo() {
    return getMetricsInfo("bytes", ClientReadHandlerMetric::getReadLength,
        ClientReadHandlerMetric::getSkippedReadLength);
  }

  @VisibleForTesting
  public String getReadUncompressLengthInfo() {
    return getMetricsInfo("uncompressed bytes", ClientReadHandlerMetric::getReadUncompressLength,
        ClientReadHandlerMetric::getSkippedReadUncompressLength);
  }

  private String getMetricsInfo(String name, Function<ClientReadHandlerMetric, Long> consumed,
      Function<ClientReadHandlerMetric, Long> skipped) {
    StringBuilder sb = new StringBuilder("Client read ").append(consumed.apply(readHandlerMetric))
        .append(" ").append(name).append(" from [").append(serverInfo).append("], Consumed[");
    for (Tier tier : Tier.VALUES) {
      sb.append(" ").append(tier.name().toLowerCase()).append(":")
          .append(consumed.apply(metricsMap.get(tier)));
    }
    sb.append(" ], Skipped[");
    for (Tier tier : Tier.VALUES) {
      sb.append(" ").append(tier.name().toLowerCase()).append(":")
          .append(skipped.apply(metricsMap.get(tier)));
    }
    sb.append(" ]");
    return sb.toString();
  }

}
