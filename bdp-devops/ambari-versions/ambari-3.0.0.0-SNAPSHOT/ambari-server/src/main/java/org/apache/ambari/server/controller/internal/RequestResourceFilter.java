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
package org.apache.ambari.server.controller.internal;

import java.util.ArrayList;
import java.util.List;

import org.apache.ambari.server.controller.ApiModel;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;

public class RequestResourceFilter implements ApiModel {
  private String serviceName;
  private String componentName;
  private final List<String> hostNames = new ArrayList<>();

  public RequestResourceFilter() {

  }

  public RequestResourceFilter(String serviceName, String componentName, List<String> hostNames) {
    this.serviceName = serviceName;
    this.componentName = componentName;
    if (hostNames != null) {
      this.hostNames.addAll(hostNames);
    }
  }

  @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
  @JsonProperty("service_name")
  public String getServiceName() {
    return serviceName;
  }

  @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
  @JsonProperty("component_name")
  public String getComponentName() {
    return componentName;
  }

  @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
  @JsonProperty("hosts")
  public List<String> getHostNames() {
    return hostNames;
  }

  @Override
  public String toString() {
    return "RequestResourceFilter{" +
      "serviceName='" + serviceName + '\'' +
      ", componentName='" + componentName + '\'' +
      ", hostNames=" + hostNames +
      '}';
  }
}
