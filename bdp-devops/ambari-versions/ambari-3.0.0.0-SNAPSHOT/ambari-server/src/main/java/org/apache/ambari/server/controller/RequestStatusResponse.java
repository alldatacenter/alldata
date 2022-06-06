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

package org.apache.ambari.server.controller;

import java.util.List;

public class RequestStatusResponse {

  // Request ID for tracking async operations
  private final Long requestId;

  List<ShortTaskStatus> tasks;

  // TODO how are logs to be sent back?
  private String logs;

  /**
   * Request message
   */
  private String message;

  /**
   * Request context
   */
  private String requestContext;

  // TODO stage specific information

  public RequestStatusResponse(Long requestId) {
    this.requestId = requestId;
  }

  /**
   * @return the logs
   */
  public String getLogs() {
    return logs;
  }

  /**
   * @param logs the logs to set
   */
  public void setLogs(String logs) {
    this.logs = logs;
  }

  /**
   * @return the requestId
   */
  public long getRequestId() {
    return requestId;
  }

  public List<ShortTaskStatus> getTasks() {
    return tasks;
  }

  public void setTasks(List<ShortTaskStatus> tasks) {
    this.tasks = tasks;
  }

  public String getRequestContext() {
    return requestContext;
  }

  public void setRequestContext(String requestContext) {
    this.requestContext = requestContext;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }
}
