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

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.apache.ambari.server.controller.spi.Resource;

/**
 * Simple concrete resource definition.
 */
public class SimpleResourceDefinition extends BaseResourceDefinition {

  /**
   * The resource singular name.
   */
  private final String singularName;

  /**
   * The resource plural name.
   */
  private final String pluralName;


  // ----- Constructors ------------------------------------------------------

  /**
   * Constructor.
   *
   * @param resourceType the resource type
   * @param singularName the resource singular name
   * @param pluralName   the resource plural name
   * @param subTypes     the sub-resource types
   * @see #SimpleResourceDefinition(Resource.Type, String, String, Collection, Map)
   */
  public SimpleResourceDefinition(Resource.Type resourceType, String singularName, String pluralName,
                                  Resource.Type... subTypes) {
    this(resourceType, singularName, pluralName, (subTypes == null) ? null : Arrays.asList(subTypes), null);
  }

  /**
   * Constructor.
   *
   * @param resourceType the resource type
   * @param singularName the resource singular name
   * @param pluralName   the resource plural name
   * @param subTypes     the sub-resource types
   * @param directives   the a map of directives for the different request types
   */
  public SimpleResourceDefinition(Resource.Type resourceType, String singularName, String pluralName,
                                  Collection<Resource.Type> subTypes,
                                  Map<DirectiveType, ? extends Collection<String>> directives) {
    super(resourceType, subTypes, directives);

    this.singularName = singularName;
    this.pluralName = pluralName;
  }

  // ----- ResourceDefinition ------------------------------------------------

  @Override
  public String getPluralName() {
    return pluralName;
  }

  @Override
  public String getSingularName() {
    return singularName;
  }
}
