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

package org.apache.ambari.server.controller.internal;

import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;

/**
 * Resource provider event used to update resource provider observers.
 */
public class ResourceProviderEvent {
  private final Resource.Type resourceType;
  private final Type type;
  private final Request request;
  private final Predicate predicate;


  // ----- Constructors ------------------------------------------------------

  public ResourceProviderEvent(Resource.Type resourceType, Type type, Request request, Predicate predicate) {
    this.resourceType = resourceType;
    this.type = type;
    this.request = request;
    this.predicate = predicate;
  }

  // ----- ResourceProviderEvent ---------------------------------------------

  /**
   * Get the associated resource type.
   *
   * @return the resource type
   */
  public Resource.Type getResourceType() {
    return resourceType;
  }

  /**
   * Get the event type.
   *
   * @return the event type
   */
  public Type getType() {
    return type;
  }

  /**
   * Get the request object that was used for the operation that generated this event.
   *
   * @return the request object
   */
  public Request getRequest() {
    return request;
  }

  /**
   * Get the predicate object that was used for the operation that generated this event.
   *
   * @return the predicate object
   */
  public Predicate getPredicate() {
    return predicate;
  }


  // ----- event type enumeration --------------------------------------------

  public enum Type {
    Create,
    Update,
    Delete
  }
}
