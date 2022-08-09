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


package org.apache.ambari.server.api.resources;

import java.util.Collections;
import java.util.Set;

import org.apache.ambari.server.controller.spi.Resource;

/**
 * Definition of a sub-resource.
 * Defines a resource instance that is added to another resource as a sub-resource.
 */
public class SubResourceDefinition {

  /**
   * Resource type.
   */
  private Resource.Type m_type;

  /**
   * Additional foreign key properties to include by default in the sub-resource.
   */
  private Set<Resource.Type> m_setForeignKeys;

  /**
   * Whether the sub-resource is a collection or a single instance.
   */
  private boolean m_isCollection = true;


  /**
   * Constructor.
   * Simple constructor which uses default state for everything except resource type.
   *
   * @param type  resource type
   */
  public SubResourceDefinition(Resource.Type type) {
    m_type = type;
  }

  /**
   * Constructor.
   * This constructor allows all state to be set.
   *
   * @param type            resource type
   * @param setForeignKeys  set of additional foreign keys to include in resource by default
   * @param isCollection    whether the sub-resource is a collection
   */
  public SubResourceDefinition(Resource.Type type, Set<Resource.Type> setForeignKeys, boolean isCollection) {
    m_type = type;
    m_setForeignKeys = setForeignKeys;
    m_isCollection = isCollection;
  }

  /**
   * Obtain the sub-resource type.
   *
   * @return  the sub-resource type
   */
  public Resource.Type getType() {
    return m_type;
  }

  /**
   * Get the set of additional foreign key properties that are included in the resource by default.
   *
   * @return  set of additional foreign key properties
   */
  public Set<Resource.Type> getAdditionalForeignKeys() {
    return m_setForeignKeys == null ? Collections.emptySet() : m_setForeignKeys;
  }

  /**
   * Whether the sub-resource is a collection.
   *
   * @return  true if a collection, false if an instance
   */
  public boolean isCollection() {
    return m_isCollection;
  }
}

