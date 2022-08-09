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
 * A RequestStatus represents the result of an asynchronous operation on resources. Methods are
 * provided to check the status of the operation and to retrieve the set of resources involved
 * in the operation.
 */
public interface RequestStatus {

  /**
   * Get the resources involved in the operation initiated by the request.
   *
   * @return the set of resources
   */
  Set<Resource> getAssociatedResources();

  /**
   * Get the resource of type request for the asynchronous request.
   *
   * @return the request resource
   */
  Resource getRequestResource();

  /**
   * Get the status of the operation initiated by the request.
   *
   * @return the status
   */
  Status getStatus();

  RequestStatusMetaData getStatusMetadata();

  /**
   * Request status.
   */
  enum Status {
    Accepted,
    InProgress,
    Complete
  }
}
