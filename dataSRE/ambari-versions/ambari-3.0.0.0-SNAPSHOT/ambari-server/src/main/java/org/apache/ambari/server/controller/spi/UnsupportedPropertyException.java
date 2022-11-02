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
 * Thrown to indicate that the requested properties are not supported for the
 * associated resource type.
 */
public class UnsupportedPropertyException extends Exception {
  /**
   * The resource type.
   */
  private final Resource.Type type;

  /**
   * The unsupported property ids.
   */
  private final Set<String> propertyIds;

  /**
   * Construct an UnsupportedPropertyException.
   *
   * @param type         the resource type
   * @param propertyIds  the unsupported property ids
   */
  public UnsupportedPropertyException(Resource.Type type, Set<String> propertyIds) {
    super("The properties " + propertyIds +
        " specified in the request or predicate are not supported for the resource type " +
        type + ".");
    this.type = type;
    this.propertyIds = propertyIds;
  }

  /**
   * Get the resource type.
   *
   * @return the resource type
   */
  public Resource.Type getType() {
    return type;
  }

  /**
   * Get the unsupported property ids.
   *
   * @return the unsupported property ids
   */
  public Set<String> getPropertyIds() {
    return propertyIds;
  }
}
