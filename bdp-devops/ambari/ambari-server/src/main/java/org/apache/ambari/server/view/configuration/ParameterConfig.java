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

package org.apache.ambari.server.view.configuration;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

/**
 * View parameter configuration.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class ParameterConfig {
  /**
   * The parameter name.
   */
  private String name;

  /**
   * The parameter description.
   */
  private String description;

  /**
   * The parameter label.
   */
  private String label;

  /**
   * The parameter placeholder.
   */
  private String placeholder;

  /**
   * The parameter default value.
   */
  @XmlElement(name="default-value")
  private String defaultValue;

  /**
   * The parameter cluster configuration id value.
   */
  @XmlElement(name="cluster-config")
  private String clusterConfig;

  /**
   * Indicates whether or not the parameter is required.
   */
  private boolean required;

  /**
   * Indicates whether or not the parameter is masked when persisted.
   */
  private boolean masked;

  /**
   * Get the parameter name.
   *
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * Get the parameter description.
   *
   * @return the description
   */
  public String getDescription() {
    return description;
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
   * Get the parameter placeholder.
   *
   * @return the placeholder
   */
  public String getPlaceholder() {
    return placeholder;
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
   * Get the cluster configuration id used to pull configuration from an associated Ambari cluster.
   *
   * @return the cluster configuration id
   */
  public String getClusterConfig() {
    return clusterConfig;
  }

  /**
   * Indicates whether or not the parameter is required.
   *
   * @return true if the parameter is required; false otherwise
   */
  public boolean isRequired() {
    return required;
  }

  /**
   * Indicates whether or not the parameter is masked when persisted.
   *
   * @return true if the parameter is masked; false otherwise
   */
  public boolean isMasked() {
    return masked;
  }
}
