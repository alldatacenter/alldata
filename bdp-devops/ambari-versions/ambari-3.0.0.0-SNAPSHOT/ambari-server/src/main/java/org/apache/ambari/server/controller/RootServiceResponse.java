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

import java.util.Objects;

import org.apache.ambari.server.controller.internal.RootServiceResourceProvider;

import io.swagger.annotations.ApiModelProperty;

public class RootServiceResponse {

  private final String serviceName;

  public RootServiceResponse(String serviceName) {
    this.serviceName = serviceName;
  }

  @ApiModelProperty(name = RootServiceResourceProvider.SERVICE_NAME)
  public String getServiceName() {
    return serviceName;
  }
  
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    RootServiceResponse other = (RootServiceResponse) o;

    return Objects.equals(serviceName, other.serviceName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(serviceName);
  }

}
