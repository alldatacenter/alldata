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

import static org.apache.ambari.server.controller.internal.BaseClusterRequest.PROVISION_ACTION_PROPERTY;

import java.util.Objects;
import java.util.Optional;

import org.apache.ambari.annotations.ApiIgnore;
import org.apache.ambari.server.controller.internal.ProvisionAction;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public final class Service {

  private static final String NAME = "name";

  private final String name;
  private final ProvisionAction provisionAction;

  @JsonCreator
  public Service(
    @JsonProperty(NAME) String name,
    @JsonProperty(PROVISION_ACTION_PROPERTY) ProvisionAction provisionAction
  ) {
    this.name = name;
    this.provisionAction = provisionAction;
  }

  public static Service of(String name) {
    return of(name, null);
  }

  public static Service of(String name, ProvisionAction provisionAction) {
    return new Service(name, provisionAction);
  }

  @JsonProperty(NAME)
  @ApiModelProperty(name = NAME)
  public String getName() {
    return name;
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
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Service service = (Service) o;
    return Objects.equals(name, service.name) &&
      Objects.equals(provisionAction, service.provisionAction);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, provisionAction);
  }

  @Override
  public String toString() {
    return name;
  }
}
