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

package org.apache.ambari.server.state;

import javax.xml.bind.annotation.XmlElement;

/**
 * Represents auto-deployment stack information.
 * This element may be a child of the component and dependency elements.
 */
public class AutoDeployInfo {
  /**
   * Whether auto-deploy is enabled
   */
  private boolean m_enabled = true;

  /**
   * Optional component name to co-locate with.
   * Specified in the form serviceName/componentName.
   */
  @XmlElement(name="co-locate")
  private String m_coLocate;

  /**
   * Setter for enabled property.
   *
   * @param enabled true if enabled, false otherwise
   */
  public void setEnabled(boolean enabled) {
    m_enabled = enabled;
  }

  /**
   * Getter for the enabled property.
   *
   * @return true if enabled, false otherwise
   */
  public boolean isEnabled() {
    return m_enabled;
  }

  /**
   * Setter for the co-locate property.
   *
   * @param coLocate a component name in the form serviceName/componentName
   */
  public void setCoLocate(String coLocate) {
    m_coLocate = coLocate;
  }

  /**
   * Getter for the co-located property.
   *
   * @return a component name in the form serviceName/componentName
   */
  public String getCoLocate() {
    return m_coLocate;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    AutoDeployInfo that = (AutoDeployInfo) o;

    if (m_enabled != that.m_enabled) return false;
    if (m_coLocate != null ? !m_coLocate.equals(that.m_coLocate) : that.m_coLocate != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = (m_enabled ? 1 : 0);
    result = 31 * result + (m_coLocate != null ? m_coLocate.hashCode() : 0);
    return result;
  }
}
