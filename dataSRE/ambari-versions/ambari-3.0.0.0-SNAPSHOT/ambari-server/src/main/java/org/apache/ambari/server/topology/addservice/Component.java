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
package org.apache.ambari.server.topology.addservice;

import static java.util.stream.Collectors.toSet;
import static org.apache.ambari.server.controller.internal.BaseClusterRequest.PROVISION_ACTION_PROPERTY;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.apache.ambari.annotations.ApiIgnore;
import org.apache.ambari.server.controller.internal.ProvisionAction;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableSet;

import io.swagger.annotations.ApiModelProperty;

public final class Component {

  private static final String COMPONENT_NAME = "name";
  private static final String HOSTS = "hosts";

  private final String name;
  private final Set<Host> hosts;
  private final ProvisionAction provisionAction;

  @JsonCreator
  public Component(
    @JsonProperty(COMPONENT_NAME) String name,
    @JsonProperty(PROVISION_ACTION_PROPERTY) ProvisionAction provisionAction,
    @JsonProperty(HOSTS) Set<Host> hosts
  ) {
    this.name = name;
    this.provisionAction = provisionAction;
    this.hosts = hosts != null ? ImmutableSet.copyOf(hosts) : ImmutableSet.of();
  }

  public static Component of(String name, String... hosts) {
    return of(name, null, hosts);
  }

  public static Component of(String name, ProvisionAction provisionAction, String... hosts) {
    return new Component(name, provisionAction, Arrays.stream(hosts).map(Host::new).collect(toSet()));
  }

  @JsonProperty(COMPONENT_NAME)
  @ApiModelProperty(name = COMPONENT_NAME)
  public String getName() {
    return name;
  }

  @JsonProperty(HOSTS)
  @ApiModelProperty(name = HOSTS)
  public Set<Host> getHosts() {
    return hosts;
  }

  @JsonProperty(PROVISION_ACTION_PROPERTY)
  @ApiModelProperty(name = PROVISION_ACTION_PROPERTY)
  public ProvisionAction _getProvisionAction() {
    return provisionAction;
  }

  @ApiIgnore
  @JsonIgnore
  public Optional<ProvisionAction> getProvisionAction() {
    return Optional.ofNullable(provisionAction);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Component other = (Component) o;

    return Objects.equals(name, other.name) &&
      Objects.equals(hosts, other.hosts) &&
      Objects.equals(provisionAction, other.provisionAction);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, hosts, provisionAction);
  }

  @Override
  public String toString() {
    return name;
  }
}
