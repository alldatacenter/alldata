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

package org.apache.ambari.server.api.services.persistence;

import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.api.services.RequestBody;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;

/**
 * Persistence manager which is responsible for persisting a resource state to the back end.
 * This includes create, update and delete operations.
 */
public interface PersistenceManager {

  /**
   * Create resources.
   *
   * @param resource     associated resource representation
   * @param requestBody  body of the current request
   * @return a request status
   *
   * @throws UnsupportedPropertyException
   * @throws ResourceAlreadyExistsException
   * @throws NoSuchParentResourceException
   * @throws SystemException
   */
  RequestStatus create(ResourceInstance resource, RequestBody requestBody)
      throws UnsupportedPropertyException,
             ResourceAlreadyExistsException,
             NoSuchParentResourceException,
             SystemException;

  /**
   *
   * @param resource     associated resource representation
   * @param requestBody  body of the current request
   * @return a request status
   *
   * @throws UnsupportedPropertyException
   * @throws SystemException
   * @throws NoSuchParentResourceException
   * @throws NoSuchResourceException
   */
  RequestStatus update(ResourceInstance resource, RequestBody requestBody)
      throws UnsupportedPropertyException, SystemException, NoSuchParentResourceException, NoSuchResourceException;

  /**
   *
   * @param resource     associated resource representation
   * @param requestBody  body of the current request
   * @return a request status
   *
   * @throws UnsupportedPropertyException
   * @throws SystemException
   * @throws NoSuchParentResourceException
   * @throws NoSuchResourceException
   */
  RequestStatus delete(ResourceInstance resource, RequestBody requestBody)
      throws UnsupportedPropertyException, SystemException, NoSuchParentResourceException, NoSuchResourceException;
}
