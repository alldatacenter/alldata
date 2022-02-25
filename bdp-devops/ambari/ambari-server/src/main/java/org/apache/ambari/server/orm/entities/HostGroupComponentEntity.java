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
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Represents a Host Group Component which is embedded in a Blueprint.
 */
@IdClass(HostGroupComponentEntityPK.class)
@Table(name = "hostgroup_component")
@Entity
public class HostGroupComponentEntity {

  @Id
  @Column(name = "hostgroup_name", nullable = false, insertable = false, updatable = false)
  private String hostGroupName;

  @Id
  @Column(name = "blueprint_name", nullable = false, insertable = false, updatable = false)
  private String blueprintName;

  @Id
  @Column(name = "name", nullable = false, insertable = true, updatable = false)
  private String name;

  @Column(name = "provision_action", nullable = true, insertable = true, updatable = false)
  private String provisionAction;

  @ManyToOne
  @JoinColumns({
      @JoinColumn(name = "hostgroup_name", referencedColumnName = "name", nullable = false),
      @JoinColumn(name = "blueprint_name", referencedColumnName = "blueprint_name", nullable = false)
  })
  private HostGroupEntity hostGroup;


  /**
   * Get the name of the host group component.
   *
   * @return component name
   */
  public String getName() {
    return name;
  }

  /**
   * Set the name of the host group component.
   *
   * @param name component name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Get the host group entity.
   *
   * @return host group entity
   */
  public HostGroupEntity getHostGroupEntity() {
    return hostGroup;
  }

  /**
   * Set the host group entity.
   *
   * @param entity  host group entity
   */
  public void setHostGroupEntity(HostGroupEntity entity) {
    this.hostGroup = entity;
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
   * @param hostGroupName host group name
   */
  public void setHostGroupName(String hostGroupName) {
    this.hostGroupName = hostGroupName;
  }

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
   * Get the provision action associated with this
   *   component.
   *
   * @return provision action
   */
  public String getProvisionAction() {
    return provisionAction;
  }

  /**
   * Set the provision action associated with this
   *   component.
   *
   * @param provisionAction action associated with the component (example: INSTALL_ONLY, INSTALL_AND_START)
   */
  public void setProvisionAction(String provisionAction) {
    this.provisionAction = provisionAction;
  }
}
