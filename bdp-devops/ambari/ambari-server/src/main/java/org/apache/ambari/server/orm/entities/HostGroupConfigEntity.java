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

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Represents a blueprint host group configuration.
 */
@IdClass(HostGroupConfigEntityPK.class)
@Table(name = "hostgroup_configuration")
@Entity
public class HostGroupConfigEntity implements BlueprintConfiguration {

  @Id
  @Column(name = "blueprint_name", nullable = false, insertable = false, updatable = false)
  private String blueprintName;

  @Id
  @Column(name = "hostgroup_name", nullable = false, insertable = false, updatable = false)
  private String hostGroupName;

  @Id
  @Column(name = "type_name", nullable = false, insertable = true, updatable = false)
  private String type;

  @Column(name = "config_data", nullable = false, insertable = true, updatable = false)
  @Basic(fetch = FetchType.LAZY)
  @Lob
  private String configData;

  @Column(name = "config_attributes", nullable = true, insertable = true, updatable = false)
  @Basic(fetch = FetchType.LAZY)
  @Lob
  private String configAttributes;


  @ManyToOne
  @JoinColumns({
      @JoinColumn(name = "hostgroup_name", referencedColumnName = "name", nullable = false),
      @JoinColumn(name = "blueprint_name", referencedColumnName = "blueprint_name", nullable = false)
  })
  private HostGroupEntity hostGroup;


  /**
   * Get the configuration type.
   *
   * @return configuration type
   */
  @Override
  public String getType() {
    return type;
  }

  /**
   * Set the configuration type.
   *
   * @param type  configuration type
   */
  @Override
  public void setType(String type) {
    this.type = type;
  }

  /**
   * Get the host group entity instance.
   *
   * @return host group entity
   */
  public HostGroupEntity getHostGroupEntity() {
    return hostGroup;
  }

  /**
   * Set the host group entity instance.
   *
   * @param entity  host group entity
   */
  public void setHostGroupEntity(HostGroupEntity entity) {
    this.hostGroup = entity;
  }

  /**
   * Get the name of the associated blueprint.
   *
   * @return blueprint name
   */
  @Override
  public String getBlueprintName() {
    return blueprintName;
  }

  /**
   * Set the name of the associated blueprint.
   * '
   * @param blueprintName  blueprint name
   */
  @Override
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
   * '
   * @param hostGroupName  host group name
   */
  public void setHostGroupName(String hostGroupName) {
    this.hostGroupName = hostGroupName;
  }

  /**
   * Get the config data.
   *
   * @return config data in json format
   */
  @Override
  public String getConfigData() {
    return configData;
  }

  /**
   * Set the config data.
   *
   * @param configData  all config data in json format
   */
  @Override
  public void setConfigData(String configData) {
    this.configData = configData;
  }

  /**
   * Gets the attributes of configs in this host group.
   *
   * @return config attributes in JSON format
   */
  @Override
  public String getConfigAttributes() {
    return configAttributes;
  }

  /**
   * Sets attributes of configs in this host group.
   *
   * @param configAttributes  all attribute values of configs in JSON format
   */
  @Override
  public void setConfigAttributes(String configAttributes) {
    this.configAttributes = configAttributes;
  }
}
