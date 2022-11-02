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
package org.apache.ambari.server.orm.entities;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Id;

/**
 * PK class for host config mappings.
 *
 */
public class HostConfigMappingEntityPK implements Serializable {
  private Long clusterId;
  private Long hostId;
  private String type;
  private Long createTimestamp;

  @Column(name = "cluster_id", nullable = false, insertable = true, updatable = true, length = 10)
  @Id
  public Long getClusterId() {
    return clusterId;
  }
  
  public void setClusterId(Long id) {
    clusterId = id;
  }
  
  @Column(name = "host_id", insertable = true, updatable = true, nullable = false)
  @Id
  public Long getHostId() {
    return hostId;
  }
  
  public void setHostId(Long hostId) {
    this.hostId = hostId;
  }
  
  @Column(name = "type_name", insertable = true, updatable = true, nullable = false)
  @Id
  public String getType() {
    return type;
  }
  
  public void setType(String type) {
    this.type = type;
  }
  
  @Column(name = "create_timestamp", insertable = true, updatable = true, nullable = false)
  @Id
  public Long getCreateTimestamp() {
    return createTimestamp;
  }
  
  public void setCreateTimestamp(Long timestamp) {
    createTimestamp = timestamp;
  }
  
  
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    
    if (o == null || getClass() != o.getClass()) return false;

    HostConfigMappingEntityPK that = (HostConfigMappingEntityPK) o;

    if (clusterId != null ? !clusterId.equals(that.clusterId) : that.clusterId != null) return false;
    if (hostId != null ? !hostId.equals(that.hostId) : that.hostId != null) return false;
    if (type != null ? !type.equals(that.type) : that.type != null) return false;
    if (createTimestamp != null ? !createTimestamp.equals (that.createTimestamp) : that.createTimestamp != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = clusterId !=null ? clusterId.intValue() : 0;
    result = 31 * result + (type != null ? type.hashCode() : 0);
    result = 31 * result + (hostId != null ? hostId.hashCode() : 0);
    result = 31 * result + createTimestamp.intValue();
    return result;
  }
}
