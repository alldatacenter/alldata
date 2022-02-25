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

package org.apache.ambari.server.orm.entities;

import javax.persistence.Column;
import javax.persistence.Id;

/**
 * Composite primary key for HostGroupConfigEntity.
 */
public class HostGroupConfigEntityPK {

  @Id
  @Column(name = "blueprint_name", nullable = false, insertable = true, updatable = false, length = 100)
  private String blueprintName;

  @Id
  @Column(name = "hostgroup_name", nullable = false, insertable = false, updatable = false)
  private String hostGroupName;

  @Id
  @Column(name = "type_name", nullable = false, insertable = true, updatable = false, length = 100)
  private String type;

  /**
   * Get the name of the associated blueprint.
   *
   * @return blueprint name
   */
  public String getBlueprintName() {
    return blueprintName;
  }

  /**
   * Set the name of the associated blueprint.
   *
   * @param blueprintName  blueprint name
   */
  public void setBlueprintName(String blueprintName) {
    this.blueprintName = blueprintName;
  }

  /**
   * Get the name of the associated host group.
   *
   * @return host group name
   */
  public String getHostGroupName() {
    return hostGroupName;
  }

  /**
   * Set the name of the associated host group.
   *
   * @param hostGroupName  host group name
   */
  public void setHostGroupName(String hostGroupName) {
    this.hostGroupName = hostGroupName;
  }

  /**
   * Get the configuration type.
   *
   * @return configuration type
   */
  public String getType() {
    return type;
  }

  /**
   * Set the configuration type.
   *
   * @param type  configuration type
   */
  public void setType(String type) {
    this.type = type;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    HostGroupConfigEntityPK that = (HostGroupConfigEntityPK) o;

    return this.blueprintName.equals(that.blueprintName) &&
           this.hostGroupName.equals(that.hostGroupName) &&
           this.type.equals(that.type);
  }

  @Override
  public int hashCode() {
    return 31 * blueprintName.hashCode() +
           hostGroupName.hashCode() +
           type.hashCode();
  }
}
