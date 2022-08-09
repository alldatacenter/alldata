/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.agent.stomp;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Default stomp topic response. Minimal response is {#StompResponseStatus.OK}, also is used to return correlation id to agent.
 */
public class StompResponse {

  @JsonProperty("status")
  private StompResponseStatus status = StompResponseStatus.OK;

  public StompResponseStatus getStatus() {
    return status;
  }

  public void setStatus(StompResponseStatus status) {
    this.status = status;
  }

  public enum StompResponseStatus {
    OK,
    FAILED
  }
}
