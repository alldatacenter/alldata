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
@javax.persistence.IdClass(ViewInstanceDataEntityPK.class)
@Table(name = "viewinstancedata")
@Entity
public class ViewInstanceDataEntity {

  @Id
  @Column(name = "view_instance_id", nullable = false, insertable = false, updatable = false)
  private Long viewInstanceId;

  @Column(name = "view_name", nullable = false, insertable = false, updatable = false)
  private String viewName;

  @Column(name = "view_instance_name", nullable = false, insertable = false, updatable = false)
  private String viewInstanceName;

  /**
   * The data key.
   */
  @Id
  @Column(name = "name", nullable = false, insertable = true, updatable = false)
  private String name;

  /**
   * The user.
   */
  @Id
  @Column(name = "user_name", nullable = false, insertable = true, updatable = false)
  private String user;

  /**
   * The property value.
   */
  @Column
  @Basic
  private String value;

  @ManyToOne
  @JoinColumns({
    @JoinColumn(name = "view_instance_id", referencedColumnName = "view_instance_id", nullable = false),
    @JoinColumn(name = "view_name", referencedColumnName = "view_name", nullable = false),
    @JoinColumn(name = "view_instance_name", referencedColumnName = "name", nullable = false)
  })
  private ViewInstanceEntity viewInstance;


  public Long getViewInstanceId() {
    return viewInstanceId;
  }

  public void setViewInstanceId(Long viewInstanceId) {
    this.viewInstanceId = viewInstanceId;
  }

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
   * Get the instance name.
   *
   * @return the instance name
   */
  public String getViewInstanceName() {
    return viewInstanceName;
  }

  /**
   * Set the instance name.
   *
   * @param viewInstanceName  the instance name
   */
  public void setViewInstanceName(String viewInstanceName) {
    this.viewInstanceName = viewInstanceName;
  }

  /**
   * Get the data key.
   *
   * @return the data key
   */
  public String getName() {
    return name;
  }

  /**
   * Set the data key.
   *
   * @param name  the data key
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Get the user.
   *
   * @return the user
   */
  public String getUser() {
    return user;
  }

  /**
   * Set the user.
   *
   * @param user  the user
   */
  public void setUser(String user) {
    this.user = user;
  }

  /**
   * Get the data value.
   *
   * @return the data value
   */
  public String getValue() {
    return value;
  }

  /**
   * Set the data value.
   *
   * @param value  the data value
   */
  public void setValue(String value) {
    this.value = value;
  }

  /**
   * Get the view instance.
   *
   * @return  the view instance
   */
  public ViewInstanceEntity getViewInstanceEntity() {
    return viewInstance;
  }

  /**
   * Set the view instance
   *
   * @param viewInstance  the view instance
   */
  public void setViewInstanceEntity(ViewInstanceEntity viewInstance) {
    this.viewInstance = viewInstance;
  }
}
