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

package org.apache.uniffle.client.response;

import java.util.List;

import org.apache.uniffle.common.rpc.StatusCode;

public class RssSendShuffleDataResponse extends ClientResponse {

  private List<Long> successBlockIds;
  private List<Long> failedBlockIds;

  public RssSendShuffleDataResponse(StatusCode statusCode) {
    super(statusCode);
  }

  public List<Long> getSuccessBlockIds() {
    return successBlockIds;
  }

  public void setSuccessBlockIds(List<Long> successBlockIds) {
    this.successBlockIds = successBlockIds;
  }

  public List<Long> getFailedBlockIds() {
    return failedBlockIds;
  }

  public void setFailedBlockIds(List<Long> failedBlockIds) {
    this.failedBlockIds = failedBlockIds;
  }
}
