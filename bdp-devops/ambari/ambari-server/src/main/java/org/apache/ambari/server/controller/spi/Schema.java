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
package org.apache.ambari.server.controller.spi;


import java.util.Set;

/**
 * The schema is used to describe all of the properties that a resource type
 * supports.
 */
public interface Schema {

  /**
   * Get the property id for the property that uniquely identifies
   * the given resource type for the resource described by this schema.
   * </p>
   * For example, the resource 'HostComponent' is uniquely identified by
   * its associated 'Cluster', 'Host' and 'Component' resources.  Passing
   * the 'Host' resource type to this method on a schema object of a 'HostComponent'
   * resource will return the id of the property of the foreign key reference from
   * the 'HostComponent' to the 'Host'.
   *
   * @param type the resource type
   * @return the key property id for the given resource type
   */
  String getKeyPropertyId(Resource.Type type);

  /**
   * Get the set of resource types used in the key that uniquely identifies
   * the resource type described by this schema.
   * </p>
   * For example, the resource 'HostComponent' is uniquely identified by
   * its associated 'Cluster', 'Host' and 'Component' resources.  Calling
   * this method should return a set of those resource types.
   *
   * @return the set of resource types used in the key that uniquely identifies
   *         the resource type described by this schema.
   */
  Set<Resource.Type> getKeyTypes();
  }
