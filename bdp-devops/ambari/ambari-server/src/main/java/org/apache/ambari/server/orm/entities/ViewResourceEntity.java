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

import java.util.Arrays;
import java.util.Collection;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Represents a resource of a View.
 */
@javax.persistence.IdClass(ViewResourceEntityPK.class)
@Table(name = "viewresource")
@Entity
public class ViewResourceEntity {
  /**
   * The view name.
   */
  @Id
  @Column(name = "view_name", nullable = false, insertable = false, updatable = false)
  private String viewName;

  /**
   * The resource name.
   */
  @Id
  @Column(name = "name", nullable = false, insertable = true, updatable = false)
  private String name;

  /**
   * The plural name of the resource.
   */
  @Column(name = "plural_name")
  @Basic
  private String pluralName;

  /**
   * The id property of the resource.
   */
  @Column(name = "id_property")
  @Basic
  private String idProperty;

  /**
   * The list of sub resource names.
   */
  @Column(name = "subResource_names")
  @Basic
  private String subResourceNames;

  /**
   * The resource provider class name.
   */
  @Column
  @Basic
  private String provider;

  /**
   * The resource service class name.
   */
  @Column
  @Basic
  private String service;

  /**
   * The resource class name.
   */
  @Column(name = "\"resource\"")
  @Basic
  private String resource;

  /**
   * The view entity.
   */
  @ManyToOne
  @JoinColumn(name = "view_name", referencedColumnName = "view_name", nullable = false)
  private ViewEntity view;


  // ----- ViewResourceEntity ------------------------------------------------

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

  /**
   * Get the resource name.
   *
   * @return the resource name
   */
  public String getName() {
    return name;
  }

  /**
   * Set the resource name.
   *
   * @param name  the resource name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Get the resource plural name.
   *
   * @return the resource plural name
   */
  public String getPluralName() {
    return pluralName;
  }

  /**
   * Set the resource plural name.
   *
   * @param pluralName  the plural name
   */
  public void setPluralName(String pluralName) {
    this.pluralName = pluralName;
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

  /**
   * Get the sub-resource names.
   *
   * @return the sub resource names
   */
  public Collection<String> getSubResourceNames() {
    return Arrays.asList(subResourceNames.split("\\s*,\\s*"));
  }

  /**
   * Set the sub-resource names.
   *
   * @param subResourceNames  the sub-resource names
   */
  public void setSubResourceNames(Collection<String> subResourceNames) {
    String s = subResourceNames.toString();
    this.subResourceNames = subResourceNames.size() > 0 ? s.substring(1, s.length()-1) : null;
  }

  /**
   * Get the resource provider class name.
   *
   * @return the resource provider class name.
   */
  public String getProvider() {
    return provider;
  }

  /**
   * Set the resource provider class name.
   *
   * @param provider  the resource provider class name.
   */
  public void setProvider(String provider) {
    this.provider = provider;
  }

  /**
   * Get the resource service class name.
   *
   * @return the resource service class name
   */
  public String getService() {
    return service;
  }

  /**
   * Set the resource service class name.
   *
   * @param service  the resource service class name
   */
  public void setService(String service) {
    this.service = service;
  }

  /**
   * Get the resource class name.
   *
   * @return the resource class name
   */
  public String getResource() {
    return resource;
  }

  /**
   * Set the resource class name.
   *
   * @param resource  the resource class name
   */
  public void setResource(String resource) {
    this.resource = resource;
  }

  /**
   * Get the parent view entity.
   *
   * @return the view entity
   */
  public ViewEntity getViewEntity() {
    return view;
  }

  /**
   * Set the parent view entity.
   *
   * @param view  the parent view entity
   */
  public void setViewEntity(ViewEntity view) {
    this.view = view;
  }
}
