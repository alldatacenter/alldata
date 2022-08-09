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

import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;

public abstract class ReadOnlyResourceProvider extends AbstractControllerResourceProvider {

  private static final String READ_ONLY_MSG = "Read-only resource";

  /**
   * Create a new resource provider for the given management controller. This
   * constructor will initialize the specified {@link Resource.Type} with the
   * provided keys. It should be used in cases where the provider declares its
   * own keys instead of reading them from a JSON file.
   *
   * @param type
   *          the type to set the properties for (not {@code null}).
   * @param propertyIds
   *          the property ids
   * @param keyPropertyIds
   *          the key property ids
   * @param managementController
   *          the management controller
   */
  ReadOnlyResourceProvider(Resource.Type type, Set<String> propertyIds,
      Map<Resource.Type, String> keyPropertyIds, AmbariManagementController managementController) {
    super(type, propertyIds, keyPropertyIds, managementController);
  }

  @Override
  public RequestStatus createResources(Request request) throws SystemException,
      UnsupportedPropertyException, ResourceAlreadyExistsException,
      NoSuchParentResourceException {
    throw new SystemException(READ_ONLY_MSG, null);
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {
    throw new SystemException(READ_ONLY_MSG, null);
  }

  @Override
  public RequestStatus updateResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {
     throw new SystemException(READ_ONLY_MSG, null);
  }

  @Override
  public RequestStatus deleteResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {
    throw new SystemException(READ_ONLY_MSG, null);
  }

  /**
   * Get an amended predicate used when use {@link #getResources(Request, Predicate)}.  This
   * is needed in cases where the resource might incorrectly be filtered during predicate processing.
   * Changing the predicate allows them to be included.
   * @param predicate the predicate passed in the request
   * @return the new predicate, or {@code null} to leave the predicate unchanged.
   */
  public Predicate amendPredicate(Predicate predicate) {
    // TODO Auto-generated method stub
    return null;
  }
}