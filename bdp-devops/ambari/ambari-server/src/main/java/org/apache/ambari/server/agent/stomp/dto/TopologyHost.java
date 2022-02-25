/**
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
package org.apache.ambari.server.agent.stomp.dto;

import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TopologyHost {
  private Long hostId;
  private String hostName;
  private String rackName;
  private String ipv4;

  public TopologyHost(Long hostId, String hostName) {
    this.hostId = hostId;
    this.hostName = hostName;
  }

  public TopologyHost(Long hostId, String hostName, String rackName, String ipv4) {
    this.hostId = hostId;
    this.hostName = hostName;
    this.rackName = rackName;
    this.ipv4 = ipv4;
  }

  public boolean updateHost(TopologyHost hostToUpdate) {
    boolean changed = false;
    if (hostToUpdate.getHostId().equals(getHostId())) {
      if (StringUtils.isNotEmpty(hostToUpdate.getHostName()) && !hostToUpdate.getHostName().equals(getHostName())) {
        setHostName(hostToUpdate.getHostName());
        changed = true;
      }
      if (StringUtils.isNotEmpty(hostToUpdate.getRackName()) && !hostToUpdate.getRackName().equals(getRackName())) {
        setRackName(hostToUpdate.getRackName());
        changed = true;
      }
      if (StringUtils.isNotEmpty(hostToUpdate.getIpv4()) && !hostToUpdate.getIpv4().equals(getIpv4())) {
        setIpv4(hostToUpdate.getIpv4());
        changed = true;
      }
    }
    return changed;
  }

  public TopologyHost deepCopy() {
    return new TopologyHost(getHostId(), getHostName(), getRackName(), getIpv4());
  }

  public Long getHostId() {
    return hostId;
  }

  public void setHostId(Long hostId) {
    this.hostId = hostId;
  }

  public String getHostName() {
    return hostName;
  }

  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

  public String getRackName() {
    return rackName;
  }

  public void setRackName(String rackName) {
    this.rackName = rackName;
  }

  public String getIpv4() {
    return ipv4;
  }

  public void setIpv4(String ipv4) {
    this.ipv4 = ipv4;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    TopologyHost that = (TopologyHost) o;

    return hostId.equals(that.hostId);
  }

  @Override
  public int hashCode() {
    return hostId.hashCode();
  }
}
