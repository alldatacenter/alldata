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

import org.roaringbitmap.longlong.Roaring64NavigableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.uniffle.client.api.ShuffleServerClient;
import org.apache.uniffle.client.request.RssGetShuffleDataRequest;
import org.apache.uniffle.client.request.RssGetShuffleIndexRequest;
import org.apache.uniffle.client.response.RssGetShuffleDataResponse;
import org.apache.uniffle.common.ShuffleDataDistributionType;
import org.apache.uniffle.common.ShuffleDataResult;
import org.apache.uniffle.common.ShuffleDataSegment;
import org.apache.uniffle.common.ShuffleIndexResult;
import org.apache.uniffle.common.exception.RssException;

public class LocalFileClientReadHandler extends DataSkippableReadHandler {
  private static final Logger LOG = LoggerFactory.getLogger(
      LocalFileClientReadHandler.class);
  private final int partitionNumPerRange;
  private final int partitionNum;
  private ShuffleServerClient shuffleServerClient;

  public LocalFileClientReadHandler(
      String appId,
      int shuffleId,
      int partitionId,
      int indexReadLimit,
      int partitionNumPerRange,
      int partitionNum,
      int readBufferSize,
      Roaring64NavigableMap expectBlockIds,
      Roaring64NavigableMap processBlockIds,
      ShuffleServerClient shuffleServerClient,
      ShuffleDataDistributionType distributionType,
      Roaring64NavigableMap expectTaskIds) {
    super(
        appId, shuffleId, partitionId, readBufferSize, expectBlockIds,
        processBlockIds, distributionType, expectTaskIds
    );
    this.shuffleServerClient = shuffleServerClient;
    this.partitionNumPerRange = partitionNumPerRange;
    this.partitionNum = partitionNum;
  }

  /**
   * Only for test
   */
  public LocalFileClientReadHandler(
      String appId,
      int shuffleId,
      int partitionId,
      int indexReadLimit,
      int partitionNumPerRange,
      int partitionNum,
      int readBufferSize,
      Roaring64NavigableMap expectBlockIds,
      Roaring64NavigableMap processBlockIds,
      ShuffleServerClient shuffleServerClient) {
    this(
        appId, shuffleId, partitionId, indexReadLimit, partitionNumPerRange,
        partitionNum, readBufferSize, expectBlockIds, processBlockIds,
        shuffleServerClient, ShuffleDataDistributionType.NORMAL, Roaring64NavigableMap.bitmapOf()
    );
  }

  @Override
  public ShuffleIndexResult readShuffleIndex() {
    ShuffleIndexResult shuffleIndexResult = null;
    RssGetShuffleIndexRequest request = new RssGetShuffleIndexRequest(
        appId, shuffleId, partitionId, partitionNumPerRange, partitionNum);
    try {
      shuffleIndexResult = shuffleServerClient.getShuffleIndex(request).getShuffleIndexResult();
    } catch (Exception e) {
      throw new RssException("Failed to read shuffle index for appId[" + appId + "], shuffleId["
        + shuffleId + "], partitionId[" + partitionId + "]", e);
    }
    return shuffleIndexResult;
  }

  @Override
  public ShuffleDataResult readShuffleData(ShuffleDataSegment shuffleDataSegment) {
    ShuffleDataResult result = null;
    int expectedLength = shuffleDataSegment.getLength();
    if (expectedLength <= 0) {
      throw new RssException("Failed to read shuffle data for appId[" + appId + "], shuffleId["
          + shuffleId + "], partitionId[" + partitionId + "], "
          + "the length field in the index segment is " + expectedLength + " <= 0!");
    }
    RssGetShuffleDataRequest request = new RssGetShuffleDataRequest(
        appId, shuffleId, partitionId, partitionNumPerRange, partitionNum,
        shuffleDataSegment.getOffset(), expectedLength);
    try {
      RssGetShuffleDataResponse response = shuffleServerClient.getShuffleData(request);
      result = new ShuffleDataResult(response.getShuffleData(), shuffleDataSegment.getBufferSegments());
    } catch (Exception e) {
      throw new RssException("Failed to read shuffle data with "
          + shuffleServerClient.getClientInfo() + " due to " + e.getMessage());
    }
    if (result.getData().length != expectedLength) {
      throw new RssException("Wrong data length expect " + result.getData().length
          + " but actual is " + expectedLength);
    }
    return result;
  }
}
