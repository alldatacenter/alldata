/**
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
package org.apache.ambari.view;

/**
 * Indicates that a resource already exists.
 */
public class ResourceAlreadyExistsException extends Exception{
  /**
   * The resource id.
   */
  private final String resourceId;

  /**
   * Constructor.
   *
   * @param resourceId the resource id
   */
  public ResourceAlreadyExistsException(String resourceId) {
    super("The resource " + resourceId +
        " specified in the request already exists.");
    this.resourceId = resourceId;
  }

  /**
   * Get the id of the non-existent resource.
   *
   * @return the resource id
   */
  public String getResourceId() {
    return resourceId;
  }
}
