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

package org.apache.ambari.server.orm.cache;


public class HostConfigMappingImpl implements HostConfigMapping {
  
  private Long clusterId;
  private Long hostId;
  private String type;
  private Long createTimestamp;
  private String version;
  private String serviceName;
  private String user;
  private Integer selected;
  

  
  public HostConfigMappingImpl(HostConfigMapping entry) {
    setClusterId(entry.getClusterId());
    setHostId(entry.getHostId());
    setType(entry.getType());
    setCreateTimestamp(entry.getCreateTimestamp());
    setVersion(entry.getVersion());
    setServiceName(entry.getServiceName());
    setUser(entry.getUser());
    setSelected(entry.getSelected());
  }

  public HostConfigMappingImpl() {
  }

  @Override
  public Long getClusterId() {
    return clusterId;
  }
  
  @Override
  public void setClusterId(Long clusterId) {
    if (clusterId == null)
      throw new RuntimeException("ClusterId couldn't be null");
    this.clusterId = clusterId;
  }

  @Override
  public Long getHostId() {
    return hostId;
  }

  @Override
  public void setHostId(Long hostId) {
    if (hostId == null)
      throw new RuntimeException("HostId couldn't be null");
    this.hostId = hostId;
  }
  
  @Override
  public String getType() {
    return type;
  }
  
  @Override
  public void setType(String type) {
    if (type == null)
      throw new RuntimeException("Type couldn't be null");
    this.type = type;
  }
  
  @Override
  public Long getCreateTimestamp() {
    return createTimestamp;
  }
  
  @Override
  public void setCreateTimestamp(Long createTimestamp) {
    if (createTimestamp == null)
      throw new RuntimeException("CreateTimestamp couldn't be null");
    this.createTimestamp = createTimestamp;
  }
  
  @Override
  public String getVersion() {
    return version;
  }
  
  @Override
  public void setVersion(String version) {
    if (version == null)
      throw new RuntimeException("Version couldn't be null");
    this.version = version;
  }
  @Override
  public String getServiceName() {
    return serviceName;
  }
  
  @Override
  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }
  
  @Override
  public String getUser() {
    return user;
  }
  
  @Override
  public void setUser(String user) {
    if (user == null)
      throw new RuntimeException("User couldn't be null");
    this.user = user;
  }

  @Override
  public Integer getSelected() {
    return selected;
  }

  @Override
  public void setSelected(Integer selected) {
    if (selected == null)
      throw new RuntimeException("Selected couldn't be null");
    this.selected = selected;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((clusterId == null) ? 0 : clusterId.hashCode());
    result = prime * result + ((createTimestamp == null) ? 0 : createTimestamp.hashCode());
    result = prime * result + ((hostId == null) ? 0 : hostId.hashCode());
    result = prime * result + ((type == null) ? 0 : type.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;

    HostConfigMappingImpl other = (HostConfigMappingImpl) obj;
    if (clusterId != null ? !clusterId.equals(other.clusterId) : other.clusterId != null) return false;
    if (createTimestamp != null ? !createTimestamp.equals(other.createTimestamp) : other.createTimestamp != null) return false;
    if (hostId != null ? !hostId.equals(other.hostId) : other.hostId != null) return false;
    if (type != null ? !type.equals(other.type) : other.type != null) return false;

    return true;
  }
}
