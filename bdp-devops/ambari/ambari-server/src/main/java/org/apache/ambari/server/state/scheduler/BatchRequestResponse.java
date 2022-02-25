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
package org.apache.ambari.server.state.scheduler;

/**
 * Captures the response of a batch request. Provides functionality to
 * capture the status of @Request status and @Task status in order to allow
 * tolerance calculations
 */
public class BatchRequestResponse {

  private Long requestId;
  private String status;
  private int returnCode;
  private String returnMessage;

  private int failedTaskCount;
  private int abortedTaskCount;
  private int timedOutTaskCount;
  private int totalTaskCount;

  public Long getRequestId() {
    return requestId;
  }

  public void setRequestId(Long requestId) {
    this.requestId = requestId;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public int getReturnCode() {
    return returnCode;
  }

  public void setReturnCode(int returnCode) {
    this.returnCode = returnCode;
  }

  public String getReturnMessage() {
    return returnMessage;
  }

  public void setReturnMessage(String returnMessage) {
    this.returnMessage = returnMessage;
  }

  public int getFailedTaskCount() {
    return failedTaskCount;
  }

  public void setFailedTaskCount(int failedTaskCount) {
    this.failedTaskCount = failedTaskCount;
  }

  public int getAbortedTaskCount() {
    return abortedTaskCount;
  }

  public void setAbortedTaskCount(int abortedTaskCount) {
    this.abortedTaskCount = abortedTaskCount;
  }

  public int getTimedOutTaskCount() {
    return timedOutTaskCount;
  }

  public void setTimedOutTaskCount(int timedOutTaskCount) {
    this.timedOutTaskCount = timedOutTaskCount;
  }

  public int getTotalTaskCount() {
    return totalTaskCount;
  }

  public void setTotalTaskCount(int totalTaskCount) {
    this.totalTaskCount = totalTaskCount;
  }
}
