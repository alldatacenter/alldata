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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.uniffle.common.BufferSegment;
import org.apache.uniffle.common.ShuffleDataResult;
import org.apache.uniffle.storage.handler.ClientReadHandlerMetric;
import org.apache.uniffle.storage.handler.api.ClientReadHandler;

public abstract class AbstractClientReadHandler implements ClientReadHandler {
  private static final Logger LOG = LoggerFactory.getLogger(AbstractClientReadHandler.class);
  protected String appId;
  protected int shuffleId;
  protected int partitionId;
  protected int readBufferSize;
  protected ClientReadHandlerMetric readHandlerMetric = new ClientReadHandlerMetric();

  @Override
  public ShuffleDataResult readShuffleData() {
    return null;
  }

  @Override
  public void close() {
  }

  @Override
  public void updateConsumedBlockInfo(BufferSegment bs, boolean isSkippedMetrics) {
    if (bs == null) {
      return;
    }
    updateBlockMetric(readHandlerMetric, bs, isSkippedMetrics);
  }

  @Override
  public void logConsumedBlockInfo() {
    LOG.info("Client read [" + readHandlerMetric.getReadBlockNum() + " blocks,"
        + " bytes:" +  readHandlerMetric.getReadLength() + " uncompressed bytes:"
        + readHandlerMetric.getReadUncompressLength()
        + "], skipped[" + readHandlerMetric.getSkippedReadBlockNum() + " blocks,"
        + " bytes:" +  readHandlerMetric.getSkippedReadLength() + " uncompressed bytes:"
        + readHandlerMetric.getSkippedReadUncompressLength() + "]");
  }

  protected void updateBlockMetric(ClientReadHandlerMetric metric, BufferSegment bs, boolean isSkippedMetrics) {
    if (isSkippedMetrics) {
      metric.incSkippedReadBlockNum();
      metric.incSkippedReadLength(bs.getLength());
      metric.incSkippedReadUncompressLength(bs.getUncompressLength());
    } else {
      metric.incReadBlockNum();
      metric.incReadLength(bs.getLength());
      metric.incReadUncompressLength(bs.getUncompressLength());
    }
  }

  public ClientReadHandlerMetric getReadHandlerMetric() {
    return readHandlerMetric;
  }

}
