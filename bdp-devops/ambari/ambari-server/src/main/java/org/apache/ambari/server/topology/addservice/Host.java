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

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModelProperty;

public final class Host {

  private static final String FQDN = "fqdn";

  private final String fqdn;

  @JsonCreator
  public Host(@JsonProperty(FQDN) String fqdn) {
    this.fqdn = fqdn;
  }

  @JsonProperty(FQDN)
  @ApiModelProperty(name = FQDN)
  public String getFqdn() {
    return fqdn;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Host other = (Host) o;

    return Objects.equals(fqdn, other.fqdn);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(fqdn);
  }

  @Override
  public String toString() {
    return "host: " + fqdn;
  }

}
