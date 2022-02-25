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

package org.apache.ambari.server.events;

/**
 * The {@link ClusterConfigChangedEvent} class is fired when a
 * cluster configuration is successfully updated.
 */
public class ClusterConfigChangedEvent extends AmbariEvent {
  private String m_clusterName;
  private String m_configType;
  private String m_versionTag;
  private Long m_version;

  public ClusterConfigChangedEvent(String clusterName, String configType, String versionTag, Long version) {
    super(AmbariEventType.CLUSTER_CONFIG_CHANGED);
    m_clusterName = clusterName;
    m_configType = configType;
    m_versionTag = versionTag;
    m_version = version;
  }

  /**
   * Get the cluster name
   *
   * @return
   */
  public String getClusterName() {
    return m_clusterName;
  }

  /**
   * Get the configuration type
   *
   * @return
   */
  public String getConfigType() {
    return m_configType;
  }

  /**
   * Get the version tag
   *
   * @return
   */
  public String getVersionTag() {
    return m_versionTag;
  }

  /**
   * Get the version
   *
   * @return
   */
  public Long getVersion() {
    return m_version;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    StringBuilder buffer = new StringBuilder("ClusterConfigChangedEvent{");
    buffer.append("clusterName=").append(getClusterName());
    buffer.append(", configType=").append(getConfigType());
    buffer.append(", versionTag=").append(getVersionTag());
    buffer.append(", version=").append(getVersion());
    buffer.append("}");
    return buffer.toString();
  }
}
