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
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Represents a property of a View instance.
 */
@javax.persistence.IdClass(ViewInstancePropertyEntityPK.class)
@Table(name = "viewinstanceproperty")
@Entity
public class ViewInstancePropertyEntity {

  /**
   * The view name.
   */
  @Id
  @Column(name = "view_name", nullable = false, insertable = false, updatable = false)
  private String viewName;

  /**
   * The view instance name.
   */
  @Id
  @Column(name = "view_instance_name", nullable = false, insertable = false, updatable = false)
  private String viewInstanceName;

  /**
   * The property key.
   */
  @Id
  @Column(name = "name", nullable = false, insertable = true, updatable = false)
  private String name;

  /**
   * The property value.
   */
  @Column
  @Basic
  private String value;

  /**
   * The view instance entity.
   */
  @ManyToOne
  @JoinColumns({
      @JoinColumn(name = "view_name", referencedColumnName = "view_name", nullable = false),
      @JoinColumn(name = "view_instance_name", referencedColumnName = "name", nullable = false)
  })
  private ViewInstanceEntity viewInstance;


  // ----- ViewInstancePropertyEntity ----------------------------------------

  /**
   * Get the view name.
   *
   * @return the view name
   */
  public String getViewName() {
    return viewName;
  }

  /**
   * Set the view name.
   *
   * @param viewName  the view name
   */
  public void setViewName(String viewName) {
    this.viewName = viewName;
  }

  /**
   * Get the view instance name.
   *
   * @return the view instance name
   */
  public String getViewInstanceName() {
    return viewInstanceName;
  }

  /**
   * Set the view instance name.
   *
   * @param viewInstanceName  the view instance name
   */
  public void setViewInstanceName(String viewInstanceName) {
    this.viewInstanceName = viewInstanceName;
  }

  /**
   * Get the property name.
   *
   * @return the property name
   */
  public String getName() {
    return name;
  }

  /**
   * Set the property name.
   *
   * @param name  the property name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Get the property value.
   *
   * @return the property value
   */
  public String getValue() {
    return value;
  }

  /**
   * Set the property value.
   *
   * @param value  the property value
   */
  public void setValue(String value) {
    this.value = value;
  }

  /**
   * Get the parent instance entity.
   *
   * @return the parent instance entity
   */
  public ViewInstanceEntity getViewInstanceEntity() {
    return viewInstance;
  }

  /**
   * Set the parent instance entity.
   *
   * @param viewInstance  the parent instance entity
   */
  public void setViewInstanceEntity(ViewInstanceEntity viewInstance) {
    this.viewInstance = viewInstance;
  }
}
