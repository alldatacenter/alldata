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

import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.configgroup.ConfigGroup;

public class ConfigGroupHostMappingImpl implements ConfigGroupHostMapping {

  private Long configGroupId;
  private Long hostId;
  private Host host;
  private ConfigGroup configGroup;

  @Override
  public Long getConfigGroupId() {
    return configGroupId;
  }

  @Override
  public Long getHostId() {
    return hostId;
  }

  @Override
  public Host getHost() {
    return host;
  }

  @Override
  public ConfigGroup getConfigGroup() {
    return configGroup;
  }

  @Override
  public void setConfigGroupId(Long configGroupId) {
    this.configGroupId = configGroupId;
  }

  @Override
  public void setHostId(Long hostId) {
    this.hostId = hostId;
  }

  @Override
  public void setHost(Host host) {
    this.host = host;
  }

  @Override
  public void setConfigGroup(ConfigGroup configGroup) {
    this.configGroup = configGroup;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result =
        prime * result + ((configGroup == null) ? 0 : configGroup.hashCode());
    result =
        prime * result
            + ((configGroupId == null) ? 0 : configGroupId.hashCode());
    result = prime * result + ((host == null) ? 0 : host.hashCode());
    result = prime * result + ((hostId == null) ? 0 : hostId.hashCode());
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
    ConfigGroupHostMappingImpl other = (ConfigGroupHostMappingImpl) obj;

    if (configGroup != null ? !configGroup.equals(other.configGroup) : other.configGroup != null) return false;
    if (configGroupId != null ? !configGroupId.equals(other.configGroupId) : other.configGroupId != null) return false;
    if (host != null ? !host.equals(other.host) : other.host != null) return false;
    if (hostId != null ? !hostId.equals(other.hostId) : other.hostId != null) return false;
    
    return true;
  }
}
