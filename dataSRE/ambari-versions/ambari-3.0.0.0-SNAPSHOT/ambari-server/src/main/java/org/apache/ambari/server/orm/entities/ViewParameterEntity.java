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
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Represents a parameter of a View.
 */
@javax.persistence.IdClass(ViewParameterEntityPK.class)
@Table(name = "viewparameter")
@Entity
public class ViewParameterEntity {
  @Id
  @Column(name = "view_name", nullable = false, insertable = false, updatable = false)
  private String viewName;

  /**
   * The parameter name.
   */
  @Id
  @Column(name = "name", nullable = false, insertable = true, updatable = false)
  private String name;

  /**
   * The parameter description.
   */
  @Column(name = "description")
  private String description;

  /**
   * The parameter label.
   */
  @Column(name = "label")
  private String label;

  /**
   * The placeholder.
   */
  @Column(name = "placeholder")
  private String placeholder;

  /**
   * The default value.
   */
  @Column(name = "default_value")
  private String defaultValue;

  /**
   * The cluster configuration id used to populate the property through cluster association.
   */
  @Column(name = "cluster_config")
  private String clusterConfig;

  /**
   * Indicates whether or not the parameter is required.
   */
  @Column
  @Basic
  private char required;

  /**
   * Indicates whether or not the parameter is masked when persisted.
   */
  @Column
  @Basic
  private char masked;

  @ManyToOne
  @JoinColumn(name = "view_name", referencedColumnName = "view_name", nullable = false)
  private ViewEntity view;


  // ----- ViewParameterEntity -----------------------------------------------

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
   * Get the parameter name.
   *
   * @return the parameter name
   */
  public String getName() {
    return name;
  }

  /**
   * Set the parameter name.
   *
   * @param name  the parameter name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Get the parameter description.
   *
   * @return the parameter description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Set the parameter description.
   *
   * @param description  the parameter description
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * Determine whether or not the parameter is required.
   *
   * @return true if the parameter is required
   */
  public boolean isRequired() {
    return required == 'y' || required == 'Y';
  }

  /**
   * Set the flag which indicate whether or not the parameter is required.
   *
   * @param required  the required flag; true if the parameter is required
   */
  public void setRequired(boolean required) {
    this.required = (required ? 'Y' : 'N');
  }

  /**
   * Determine whether or not the parameter is masked.
   *
   * @return true if parameter is masked
   */
  public boolean isMasked() {
    return masked == 'y' || masked == 'Y';
  }

  /**
   * Set the flag which indicate whether or not the parameter is masked
   * @param masked the masked flag; true if the parameter is masked
   */
  public void setMasked(boolean masked) {
    this.masked = (masked ? 'Y' : 'N');
  }

  /**
   * Get the associated view entity.
   *
   * @return the view entity
   */
  public ViewEntity getViewEntity() {
    return view;
  }

  /**
   * Set the associated view entity.
   *
   * @param view the view entity
   */
  public void setViewEntity(ViewEntity view) {
    this.view = view;
  }

  /**
   * Get the parameter label.
   *
   * @return the label
   */
  public String getLabel() {
    return label;
  }

  /**
   * Set the parameter label.
   *
   * @param label  the label
   */
  public void setLabel(String label) {
    this.label = label;
  }

  /**
   * Get the parameter placeholder.
   *
   * @return the placeholder
   */
  public String getPlaceholder() {
    return placeholder;
  }

  /**
   * Set the parameter placeholder.
   *
   * @param placeholder  the placeholder
   */
  public void setPlaceholder(String placeholder) {
    this.placeholder = placeholder;
  }

  /**
   * Get the parameter default value.
   *
   * @return the default value
   */
  public String getDefaultValue() {
    return defaultValue;
  }

  /**
   * Set the parameter default value.
   *
   * @param defaultValue  the parameter default value
   */
  public void setDefaultValue(String defaultValue) {
    this.defaultValue = defaultValue;
  }

  /**
   * Get the cluster configuration id used to populate the property for this parameter through
   * cluster association.
   *
   * @return the cluster configuration id
   */
  public String getClusterConfig() {
    return clusterConfig;
  }

  /**
   * Set the cluster configuration id used to populate the property for this parameter through
   * cluster association.
   *
   * @param clusterConfig  the cluster configuration id
   */
  public void setClusterConfig(String clusterConfig) {
    this.clusterConfig = clusterConfig;
  }
}
