/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.events;

/**
 * The {@link ClusterConfigFinishedEvent} class is fired when a
 * cluster configuration is successfully updated.
 */
public class ClusterConfigFinishedEvent extends AmbariEvent {

  private final long clusterId;
  private final String clusterName;


  public ClusterConfigFinishedEvent(long clusterId, String clusterName) {
    super(AmbariEventType.CLUSTER_CONFIG_FINISHED);
    this.clusterId = clusterId;
    this.clusterName = clusterName;
  }

  /**
   * Get the cluster id
   * @return
   */
  public long getClusterId() {
    return clusterId;
  }

  /**
   * Get the cluster name
   * @return
   */
  public String getClusterName() {
    return clusterName;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    StringBuilder buffer = new StringBuilder("ClusterConfigChangedEvent{");
    buffer.append("clusterId=").append(getClusterId());
    buffer.append("clusterName=").append(getClusterName());
    buffer.append("}");
    return buffer.toString();
  }
}
