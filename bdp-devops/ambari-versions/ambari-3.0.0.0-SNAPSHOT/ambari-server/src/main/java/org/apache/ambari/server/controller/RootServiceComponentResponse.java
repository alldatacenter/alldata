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
package org.apache.ambari.server.controller;

import java.util.Map;
import java.util.Objects;

import org.apache.ambari.server.controller.internal.RootServiceComponentResourceProvider;

import io.swagger.annotations.ApiModelProperty;

public class RootServiceComponentResponse {

  private final String serviceName;
  private final String componentName;
  private final Map<String, String> properties;
  private final String componentVersion;
  private final long serverClock = System.currentTimeMillis() / 1000L;

  public RootServiceComponentResponse(String serviceName, String componentName, String componentVersion, Map<String, String> properties) {
    this.serviceName = serviceName;
    this.componentName = componentName;
    this.componentVersion = componentVersion;
    this.properties = properties;
  }

  @ApiModelProperty(name = RootServiceComponentResourceProvider.SERVICE_NAME)
  public String getServiceName() {
    return serviceName;
  }

  @ApiModelProperty(name = RootServiceComponentResourceProvider.COMPONENT_NAME)
  public String getComponentName() {
    return componentName;
  }

  @ApiModelProperty(name = RootServiceComponentResourceProvider.PROPERTIES)
  public Map<String, String> getProperties() {
    return properties;
  }

  @ApiModelProperty(name = RootServiceComponentResourceProvider.COMPONENT_VERSION)
  public String getComponentVersion() {
    return componentVersion;
  }

  @ApiModelProperty(name = RootServiceComponentResourceProvider.SERVER_CLOCK)
  public long getServerClock() { return serverClock; }
  
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    RootServiceComponentResponse other = (RootServiceComponentResponse) o;

    return Objects.equals(serviceName, other.serviceName) &&
      Objects.equals(componentName, other.componentName) &&
      Objects.equals(componentVersion, other.componentVersion) &&
      Objects.equals(properties, other.properties);
  }

  @Override
  public int hashCode() {
    return Objects.hash(serviceName, componentName, componentVersion);
  }

}
