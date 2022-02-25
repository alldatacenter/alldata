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
 * Composite primary key for HostGroupEntity.
 */
public class HostGroupEntityPK {

  @Id
  @Column(name = "blueprint_name", nullable = false, insertable = true, updatable = false, length = 100)
  private String blueprintName;

  @Id
  @Column(name = "name", nullable = false, insertable = true, updatable = false, length = 100)
  private String name;

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
   * Get the name of the host group.
   *
   * @return host group name
   */
  public String getName() {
    return name;
  }

  /**
   * Set the name of the host group.
   *
   * @param name  host group name
   */
  public void setName(String name) {
    this.name = name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    HostGroupEntityPK that = (HostGroupEntityPK) o;

    return this.blueprintName.equals(that.blueprintName) &&
           this.name.equals(that.name);
  }

  @Override
  public int hashCode() {
    return 31 * blueprintName.hashCode() + name.hashCode();
  }
}
