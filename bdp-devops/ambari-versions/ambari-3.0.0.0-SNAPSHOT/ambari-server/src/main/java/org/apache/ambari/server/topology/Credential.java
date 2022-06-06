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

package org.apache.ambari.server.topology;

import static org.apache.ambari.server.controller.internal.ProvisionClusterRequest.ALIAS;
import static org.apache.ambari.server.controller.internal.ProvisionClusterRequest.KEY;
import static org.apache.ambari.server.controller.internal.ProvisionClusterRequest.PRINCIPAL;
import static org.apache.ambari.server.controller.internal.ProvisionClusterRequest.TYPE;

import java.util.Objects;

import org.apache.ambari.server.security.encryption.CredentialStoreType;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * Holds credential info submitted in a cluster create template.
 */
@ApiModel
public class Credential {

  /**
   * Credential alias like kdc.admin.credential.
   */
  private final String alias;

  /**
   * Name of a principal.
   */
  private final String principal;

  /**
   * Key of credential.
   */
  private final String key;

  /**
   * Type of credential store.
   */
  private final CredentialStoreType type;

  @JsonCreator
  public Credential(
    @JsonProperty(ALIAS) String alias,
    @JsonProperty(PRINCIPAL) String principal,
    @JsonProperty(KEY) String key,
    @JsonProperty(TYPE) CredentialStoreType type
  ) {
    this.alias = alias;
    this.principal = principal;
    this.key = key;
    this.type = type;
  }

  @JsonProperty(ALIAS)
  @ApiModelProperty(name = ALIAS)
  public String getAlias() {
    return alias;
  }

  @JsonProperty(PRINCIPAL)
  @ApiModelProperty(name = PRINCIPAL)
  public String getPrincipal() {
    return principal;
  }

  @JsonProperty(KEY)
  @ApiModelProperty(name = KEY)
  public String getKey() {
    return key;
  }

  @JsonProperty(TYPE)
  @ApiModelProperty(name = TYPE)
  public CredentialStoreType getType() {
    return type;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || obj.getClass() != getClass()) {
      return false;
    }

    Credential other = (Credential) obj;

    return Objects.equals(alias, other.alias) &&
      Objects.equals(principal, other.principal) &&
      Objects.equals(key, other.key) &&
      Objects.equals(type, other.type);
  }

  @Override
  public int hashCode() {
    return Objects.hash(alias, principal, key, type);
  }

  @Override
  public String toString() {
    return alias;
  }
}
