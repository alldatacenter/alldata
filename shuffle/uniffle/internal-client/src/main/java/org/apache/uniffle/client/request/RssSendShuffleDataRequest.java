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

package org.apache.uniffle.client.request;

import java.util.List;
import java.util.Map;

import org.apache.uniffle.common.ShuffleBlockInfo;

public class RssSendShuffleDataRequest {

  private String appId;
  private int retryMax;
  private long retryIntervalMax;
  private Map<Integer, Map<Integer, List<ShuffleBlockInfo>>> shuffleIdToBlocks;

  public RssSendShuffleDataRequest(String appId, int retryMax, long retryIntervalMax,
      Map<Integer, Map<Integer, List<ShuffleBlockInfo>>> shuffleIdToBlocks) {
    this.appId = appId;
    this.retryMax = retryMax;
    this.retryIntervalMax = retryIntervalMax;
    this.shuffleIdToBlocks = shuffleIdToBlocks;
  }

  public String getAppId() {
    return appId;
  }

  public int getRetryMax() {
    return retryMax;
  }

  public long getRetryIntervalMax() {
    return retryIntervalMax;
  }

  public Map<Integer, Map<Integer, List<ShuffleBlockInfo>>> getShuffleIdToBlocks() {
    return shuffleIdToBlocks;
  }
}
