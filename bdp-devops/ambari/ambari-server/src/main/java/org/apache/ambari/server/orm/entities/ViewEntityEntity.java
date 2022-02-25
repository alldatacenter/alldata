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
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

/**
 * Represents an entity of a View.
 */
@Table(name = "viewentity")
@Entity
@TableGenerator(name = "viewentity_id_generator",
    table = "ambari_sequences", pkColumnName = "sequence_name", valueColumnName = "sequence_value"
    , pkColumnValue = "viewentity_id_seq"
    , initialValue = 1
)
public class ViewEntityEntity {

  @Column(name = "id")
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "viewentity_id_generator")
  private Long id;

  /**
   * The view name.
   */
  @Column(name = "view_name", nullable = false, insertable = false, updatable = false)
  private String viewName;

  @Column(name = "view_instance_name", nullable = false, insertable = false, updatable = false)
  private String viewInstanceName;

  /**
   * The entity class name.
   */
  @Column(name = "class_name", nullable = false)
  @Basic
  private String className;

  /**
   * The id property of the entity.
   */
  @Column(name = "id_property")
  @Basic
  private String idProperty;


  @ManyToOne
  @JoinColumns({
      @JoinColumn(name = "view_name", referencedColumnName = "view_name", nullable = false),
      @JoinColumn(name = "view_instance_name", referencedColumnName = "name", nullable = false)
  })
  private ViewInstanceEntity viewInstance;


  // ----- ViewEntityEntity ------------------------------------------------

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
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
   * Set the view name
   *
   * @param viewName  the view name
   */
  public void setViewName(String viewName) {
    this.viewName = viewName;
  }

  public String getViewInstanceName() {
    return viewInstanceName;
  }

  public void setViewInstanceName(String viewInstanceName) {
    this.viewInstanceName = viewInstanceName;
  }

  /**
   * Get the entity class name.
   *
   * @return the entity class name
   */
  public String getClassName() {
    return className;
  }

  /**
   * Set the entity class name.
   *
   * @param name  the entity class name
   */
  public void setClassName(String name) {
    this.className = name;
  }

  /**
   * Get the id property.
   *
   * @return the id property
   */
  public String getIdProperty() {
    return idProperty;
  }

  /**
   * Set the id property.
   *
   * @param idProperty  the id property
   */
  public void setIdProperty(String idProperty) {
    this.idProperty = idProperty;
  }

  public ViewInstanceEntity getViewInstance() {
    return viewInstance;
  }

  public void setViewInstance(ViewInstanceEntity viewInstance) {
    this.viewInstance = viewInstance;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ViewEntityEntity that = (ViewEntityEntity) o;

    if (!className.equals(that.className)) return false;
    if (id != null ? !id.equals(that.id) : that.id != null) return false;
    if (idProperty != null ? !idProperty.equals(that.idProperty) : that.idProperty != null) return false;
    if (!viewInstanceName.equals(that.viewInstanceName)) return false;
    if (!viewName.equals(that.viewName)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = id != null ? id.hashCode() : 0;
    result = 31 * result + viewName.hashCode();
    result = 31 * result + viewInstanceName.hashCode();
    result = 31 * result + className.hashCode();
    result = 31 * result + (idProperty != null ? idProperty.hashCode() : 0);
    return result;
  }
}
