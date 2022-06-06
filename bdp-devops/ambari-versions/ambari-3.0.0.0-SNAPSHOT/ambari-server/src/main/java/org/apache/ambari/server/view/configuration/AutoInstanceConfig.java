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

import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * View auto instance configuration.
 * </p>
 * Used by Ambari to automatically create an instance of a view and associate it with
 * a cluster if the cluster's stack and services match those given in this configuration.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class AutoInstanceConfig extends InstanceConfig {
  /**
   * The stack id.
   */
  @XmlElement(name="stack-id")
  private String stackId;

  /**
   * The list of view instances.
   */
  @XmlElementWrapper
  @XmlElement(name="service")
  @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
  private List<String> services;

  /**
   * A list of roles that should have access to this view.
   * <p>
   * Example values:
   * <ul>
   * <li>CLUSTER.ADMINISTRATOR</li>
   * <li>CLUSTER.OPERATOR</li>
   * <li>SERVICE.ADMINISTRATOR</li>
   * <li>SERVICE.OPERATOR</li>
   * <li>CLUSTER.USER</li>
   * </ul>
   */
  @XmlElementWrapper
  @XmlElement(name="role")
  @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
  private Set<String> roles;

  /**
   * Get the stack id used for auto instance creation.
   *
   * @return the stack id
   */
  public String getStackId() {
    return stackId;
  }

  /**
   * Get the services used for auto instance creation.
   *
   * @return the services
   */
  public List<String> getServices() {
    return services;
  }

  /**
   * @return the set of roles that should have access to this view
   */
  public Set<String> getRoles() {
    return roles;
  }
}
