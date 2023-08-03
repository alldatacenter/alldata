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

package org.apache.celeborn.plugin.flink;

import java.util.Optional;

import org.apache.flink.api.common.JobID;
import org.apache.flink.runtime.clusterframework.types.ResourceID;
import org.apache.flink.runtime.io.network.partition.ResultPartitionID;
import org.apache.flink.runtime.shuffle.ShuffleDescriptor;

public class RemoteShuffleDescriptor implements ShuffleDescriptor {
  private final String celebornAppId;
  private final JobID jobId;
  // jobId-datasetId
  private final String shuffleId;
  private final ResultPartitionID resultPartitionID;
  private final RemoteShuffleResource shuffleResource;

  public RemoteShuffleDescriptor(
      String celebornAppId,
      JobID jobId,
      String shuffleId,
      ResultPartitionID resultPartitionID,
      RemoteShuffleResource shuffleResource) {
    this.celebornAppId = celebornAppId;
    this.jobId = jobId;
    this.shuffleId = shuffleId;
    this.resultPartitionID = resultPartitionID;
    this.shuffleResource = shuffleResource;
  }

  @Override
  public ResultPartitionID getResultPartitionID() {
    return resultPartitionID;
  }

  public String getCelebornAppId() {
    return celebornAppId;
  }

  public JobID getJobId() {
    return jobId;
  }

  public String getShuffleId() {
    return shuffleId;
  }

  public RemoteShuffleResource getShuffleResource() {
    return shuffleResource;
  }

  @Override
  public Optional<ResourceID> storesLocalResourcesOn() {
    return Optional.empty();
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("RemoteShuffleDescriptor{");
    sb.append("celebornAppId='").append(celebornAppId).append('\'');
    sb.append(", jobID=").append(jobId);
    sb.append(", shuffleId='").append(shuffleId).append('\'');
    sb.append(", resultPartitionID=").append(resultPartitionID);
    sb.append(", shuffleResource=").append(shuffleResource);
    sb.append('}');
    return sb.toString();
  }
}
