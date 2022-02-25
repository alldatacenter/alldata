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
package org.apache.ambari.server.state.alert;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * The {@link AlertGroup} class is used to represent a grouping of alert
 * definitions when returning alert targets.
 */
public class AlertGroup {
  private long m_id;
  private String m_name;
  private long m_clusterId;
  private boolean m_isDefault;

  /**
   * @return the id
   */
  @JsonProperty("id")
  public long getId() {
    return m_id;
  }

  /**
   * @param id
   *          the id to set
   */
  public void setId(long id) {
    m_id = id;
  }

  /**
   * @return the name
   */
  @JsonProperty("name")
  public String getName() {
    return m_name;
  }

  /**
   * @param name
   *          the name to set
   */
  public void setName(String name) {
    m_name = name;
  }

  /**
   * @return the cluster ID
   */
  @JsonProperty("cluster_id")
  public long getClusterId() {
    return m_clusterId;
  }

  /**
   * @param clusterId
   *          the cluster id to set
   */
  public void setClusterName(long clusterId) {
    m_clusterId = clusterId;
  }

  /**
   * @return the isDefault
   */
  @JsonProperty("default")
  public boolean isDefault() {
    return m_isDefault;
  }

  /**
   * @param isDefault
   *          the isDefault to set
   */
  public void setDefault(boolean isDefault) {
    m_isDefault = isDefault;
  }
}
