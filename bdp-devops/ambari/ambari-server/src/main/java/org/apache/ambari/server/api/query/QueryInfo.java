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

package org.apache.ambari.server.api.query;

import java.util.HashSet;
import java.util.Set;

import org.apache.ambari.server.api.resources.ResourceDefinition;

/**
 * Query related information.
 */
public class QueryInfo {
  /**
   * Resource definition
   */
  private ResourceDefinition m_resource;

  /**
   * Requested properties for the query.
   * These properties comprise the select portion of a query.
   */
  private Set<String> m_properties;

  // ----- Constructors ------------------------------------------------------

  /**
   * Constructor
   *
   * @param resource    resource definition
   * @param properties  query properties
   */
  public QueryInfo(ResourceDefinition resource, Set<String> properties) {
    m_resource   = resource;
    m_properties = new HashSet<>(properties);
  }

  // ----- QueryInfo ---------------------------------------------------------

  /**
   * Obtain the resource definition associated with the query.
   *
   * @return associated resource definition
   */
  public ResourceDefinition getResource() {
    return m_resource;
  }

  /**
   * Obtain the properties associated with the query.
   * These are the requested properties which comprise
   * the select portion of the query.
   *
   * @return requested properties
   */
  public Set<String> getProperties() {
    return m_properties;
  }
}
