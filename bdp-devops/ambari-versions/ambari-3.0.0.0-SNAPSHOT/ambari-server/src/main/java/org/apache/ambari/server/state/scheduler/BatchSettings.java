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

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;

public class BatchSettings {
  private Integer batchSeparationInSeconds;
  private Integer taskFailureTolerance;
  private Integer taskFailureTolerancePerBatch;
  private Boolean pauseAfterFirstBatch = false;

  @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
  @JsonProperty("batch_separation_in_seconds")
  public Integer getBatchSeparationInSeconds() {
    return batchSeparationInSeconds;
  }

  public void setBatchSeparationInSeconds(Integer batchSeparationInSeconds) {
    this.batchSeparationInSeconds = batchSeparationInSeconds;
  }

  @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
  @JsonProperty("task_failure_tolerance_limit")
  public Integer getTaskFailureToleranceLimit() {
    return taskFailureTolerance;
  }

  public void setTaskFailureToleranceLimit(Integer taskFailureTolerance) {
    this.taskFailureTolerance = taskFailureTolerance;
  }

  @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
  @JsonProperty("task_failure_tolerance_limit_per_batch")
  public Integer getTaskFailureToleranceLimitPerBatch() {
    return taskFailureTolerancePerBatch;
  }

  @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
  @JsonProperty("pause_after_first_batch")
  public Boolean isPauseAfterFirstBatch() {
    return pauseAfterFirstBatch;
  }

  public void setPauseAfterFirstBatch(Boolean pauseAfterFirstBatch) {
    this.pauseAfterFirstBatch = pauseAfterFirstBatch;
  }

  public void setTaskFailureToleranceLimitPerBatch(Integer taskFailureTolerancePerBatch) {
    this.taskFailureTolerancePerBatch = taskFailureTolerancePerBatch;
  }
}
