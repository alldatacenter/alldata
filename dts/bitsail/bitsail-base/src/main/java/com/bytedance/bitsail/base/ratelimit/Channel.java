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

package com.bytedance.bitsail.base.ratelimit;

import com.google.common.util.concurrent.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

/**
 * Created at 2018/6/25.
 */
public class Channel implements Serializable {

  private static final long serialVersionUID = 1L;

  private static final Logger LOG = LoggerFactory.getLogger(Channel.class);

  private final long recordSpeed;
  private final long byteSpeed;

  private final Communication currentCommunication = new Communication();
  private final Communication lastCommunication = new Communication();
  boolean isChannelByteSpeedLimit;
  boolean isChannelRecordSpeedLimit;
  private transient RateLimiter recordRateLimiter;
  private transient RateLimiter byteRateLimiter;

  public Channel(long recordSpeed, long byteSpeed) {
    this.recordSpeed = recordSpeed;
    this.byteSpeed = byteSpeed;
    LOG.info("Records Speed Per Second: {}", recordSpeed);
    LOG.info("Bytes Speed Per Second: {}", byteSpeed);

    this.isChannelByteSpeedLimit = (this.byteSpeed > 0);
    this.isChannelRecordSpeedLimit = (this.recordSpeed > 0);
  }

  private RateLimiter getRecordRateLimiter() {
    if (recordRateLimiter == null && isChannelRecordSpeedLimit) {
      recordRateLimiter = RateLimiter.create(this.recordSpeed);
    }
    return recordRateLimiter;
  }

  private RateLimiter getByteRateLimiter() {
    if (byteRateLimiter == null && isChannelByteSpeedLimit) {
      byteRateLimiter = RateLimiter.create(this.byteSpeed);
    }
    return byteRateLimiter;
  }

  public void checkFlowControl(long succRecordSize, long succByteSize, long failedRecordSize) {
    if (!this.isChannelByteSpeedLimit && !this.isChannelRecordSpeedLimit) {
      return;
    }

    currentCommunication.setCounterVal(CommunicationTool.SUCCEED_RECORDS, succRecordSize);
    currentCommunication.setCounterVal(CommunicationTool.SUCCEED_BYTES, succByteSize);
    currentCommunication.setCounterVal(CommunicationTool.FAILED_RECORDS, failedRecordSize);

    if (this.isChannelRecordSpeedLimit) {
      long currentRecords = (CommunicationTool.getTotalRecords(currentCommunication) -
          CommunicationTool.getTotalRecords(lastCommunication));
      getRecordRateLimiter().acquire((int) currentRecords);
    }

    if (this.isChannelByteSpeedLimit) {
      long currentBytes = (CommunicationTool.getTotalBytes(currentCommunication) -
          CommunicationTool.getTotalBytes(lastCommunication));
      getByteRateLimiter().acquire((int) currentBytes);
    }

    lastCommunication.setCounterVal(CommunicationTool.SUCCEED_RECORDS,
        currentCommunication.getCounterVal(CommunicationTool.SUCCEED_RECORDS));
    lastCommunication.setCounterVal(CommunicationTool.SUCCEED_BYTES,
        currentCommunication.getCounterVal(CommunicationTool.SUCCEED_BYTES));
    lastCommunication.setCounterVal(CommunicationTool.FAILED_RECORDS,
        currentCommunication.getCounterVal(CommunicationTool.FAILED_RECORDS));
  }
}