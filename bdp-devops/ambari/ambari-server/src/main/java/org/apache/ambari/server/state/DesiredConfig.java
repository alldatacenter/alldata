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
package org.apache.ambari.server.state;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;

/**
 * Class that holds information about a desired config and is suitable for output
 * in a web service response.
 */
public class DesiredConfig {

  private String tag;
  private String serviceName;
  private Long version;
  private List<HostOverride> hostOverrides = new ArrayList<>();

  /**
   * Sets the tag
   * @param tag the tag
   */
  public void setTag(String tag) {
    this.tag = tag;
  }

  /**
   * Gets the tag
   * @return the tag
   */
  @JsonProperty("tag")
  public String getTag() {
    return tag;
  }

  /**
   * Gets the service name (if any) for the desired config.
   * @return the service name
   */
  @JsonSerialize(include = Inclusion.NON_NULL)
  @JsonProperty("service_name")
  public String getServiceName() {
    return serviceName;
  }

  /**
   * Sets the service name (if any) for the desired config.
   * @param name the service name
   */
  public void setServiceName(String name) {
    serviceName = name;
  }
  
  /**
   * Sets the host overrides for the desired config.  Cluster-based desired configs only.
   * @param overrides the host names
   */
  public void setHostOverrides(List<HostOverride> overrides) {
    hostOverrides = overrides;
  }
  
  /**
   * Gets the host overrides for the desired config.  Cluster-based desired configs only.
   * @return the host names that override the desired config
   */
  @JsonSerialize(include = Inclusion.NON_EMPTY)
  @JsonProperty("host_overrides")
  public List<HostOverride> getHostOverrides() {
    return hostOverrides;
  }

  @JsonProperty("version")
  public Long getVersion() {
    return version;
  }

  public void setVersion(Long version) {
    this.version = version;
  }

  /**
   * Used to represent an override on a host.
   */
  //TODO include changes for config versions
  public final static class HostOverride {
    private final String hostName;
    private final String versionOverrideTag;

    /**
     * @param name the host name
     * @param tag the config tag
     */
    public HostOverride(String name, String tag) {
      hostName = name;
      versionOverrideTag = tag;
    }

    /**
     * @return the override host name
     */
    @JsonProperty("host_name")
    public String getName() {
      return hostName;
    }

    /**
     * @return the override tag tag
     */
    @JsonProperty("tag")
    public String getVersionTag() {
      return versionOverrideTag;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;

      if (o == null || getClass() != o.getClass()) return false;

      HostOverride that = (HostOverride) o;

      return new EqualsBuilder()
        .append(hostName, that.hostName)
        .append(versionOverrideTag, that.versionOverrideTag)
        .isEquals();
    }

    @Override
    public int hashCode() {
      return new HashCodeBuilder(17, 37)
        .append(hostName)
        .append(versionOverrideTag)
        .toHashCode();
    }
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    sb.append("tag=").append(tag);
    if (null != serviceName)
      sb.append(", service=").append(serviceName);
    if (null != hostOverrides && hostOverrides.size() > 0) {
      sb.append(", hosts=[");
      int i = 0;
      for (DesiredConfig.HostOverride h : hostOverrides)
      {
        if (i++ != 0)
          sb.append(",");
        sb.append(h.getName()).append(':').append(h.getVersionTag());
      }

      sb.append(']');
    }
    sb.append("}");

    return sb.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;

    if (o == null || getClass() != o.getClass()) return false;

    DesiredConfig that = (DesiredConfig) o;

    return new EqualsBuilder()
      .append(tag, that.tag)
      .append(serviceName, that.serviceName)
      .append(version, that.version)
      .append(hostOverrides, that.hostOverrides)
      .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
      .append(tag)
      .append(serviceName)
      .append(version)
      .append(hostOverrides)
      .toHashCode();
  }
}
