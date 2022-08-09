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

package org.apache.ambari.server.api.services;

import java.util.Map;

/**
 * A named set of related properties.
 */
public class NamedPropertySet {
  /**
   * The name of this set of properties.
   */
  private String m_name;

  /**
   * Property name/value pairs.
   */
  private Map<String, Object> m_mapProperties;


  /**
   * Constructor.
   *
   * @param name           name of this property set
   * @param mapProperties  associated properties
   */
  public NamedPropertySet(String name, Map<String, Object> mapProperties) {
    m_name = name;
    m_mapProperties = mapProperties;
  }

  /**
   * Obtain the name of this property set.
   *
   * @return the name of this property set
   */
  public String getName() {
    return m_name;
  }

  /**
   * Obtain the associated properties.
   *
   * @return  the associated properties
   */
  public Map<String, Object> getProperties() {
    return m_mapProperties;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    NamedPropertySet that = (NamedPropertySet) o;

    return (m_mapProperties == null ? that.m_mapProperties == null : m_mapProperties.equals(that.m_mapProperties)) &&
        (m_name == null ? that.m_name == null : m_name.equals(that.m_name));

  }

  @Override
  public int hashCode() {
    int result = m_name != null ? m_name.hashCode() : 0;
    result = 31 * result + (m_mapProperties != null ? m_mapProperties.hashCode() : 0);
    return result;
  }
}
