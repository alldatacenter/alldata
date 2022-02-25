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

import java.util.Collection;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 * Represents a Host Group which is embedded in a Blueprint.
 */
@javax.persistence.IdClass(HostGroupEntityPK.class)
@Table(name = "hostgroup")
@Entity
public class HostGroupEntity {

  @Id
  @Column(name = "blueprint_name", nullable = false, insertable = false, updatable = false)
  private String blueprintName;

  @Id
  @Column(name = "name", nullable = false, insertable = true, updatable = false)
  private String name;

  @Column
  @Basic
  private String cardinality = "NOT SPECIFIED";

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "hostGroup")
  private Collection<HostGroupComponentEntity> components;

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "hostGroup")
  private Collection<HostGroupConfigEntity> configurations;

  @ManyToOne
  @JoinColumn(name = "blueprint_name", referencedColumnName = "blueprint_name", nullable = false)
  private BlueprintEntity blueprint;


  /**
   * Get the host group name.
   *
   * @return host group name
   */
  public String getName() {
    return name;
  }

  /**
   * Set the host group name.
   *
   * @param name  host group name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Get the blueprint entity instance.
   *
   * @return blueprint entity
   */
  public BlueprintEntity getBlueprintEntity() {
    return blueprint;
  }

  /**
   * Set the blueprint entity instance.
   *
   * @param entity  blueprint entity
   */
  public void setBlueprintEntity(BlueprintEntity entity) {
    this.blueprint = entity;
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
   * '
   * @param blueprintName  blueprint name
   */
  public void setBlueprintName(String blueprintName) {
    this.blueprintName = blueprintName;
  }

  /**
   * Get the collection of associated component entities.
   *
   * @return collection of components
   */
  public Collection<HostGroupComponentEntity> getComponents() {
    return components;
  }

  /**
   * Set thge collection of associated component entities.
   *
   * @param components  collection of components
   */
  public void setComponents(Collection<HostGroupComponentEntity> components) {
    this.components = components;
  }

  /**
   * Add a component to the host group.
   *
   * @param component  component to add
   */
  public void addComponent(HostGroupComponentEntity component) {
    this.components.add(component);
  }

  /**
   * Get the collection of associated configuration entities.
   *
   * @return collection of configurations
   */
  public Collection<HostGroupConfigEntity> getConfigurations() {
    return configurations;
  }

  /**
   * Set the collection of associated configuration entities.
   *
   * @param configurations  collection of configurations
   */
  public void setConfigurations(Collection<HostGroupConfigEntity> configurations) {
    this.configurations = configurations;
  }

  /**
   * Get the cardinality for this host group.
   *
   * @return cardinality
   */
  public String getCardinality() {
    return cardinality;
  }

  /**
   * Set the cardinality value for this host group.
   *
   * @param cardinality cardinality value
   */
  public void setCardinality(String cardinality) {
    if (cardinality != null) {
      this.cardinality = cardinality;
    }
  }
}
