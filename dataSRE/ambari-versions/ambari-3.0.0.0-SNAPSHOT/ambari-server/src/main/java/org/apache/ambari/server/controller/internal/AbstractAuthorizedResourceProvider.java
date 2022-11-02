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

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.PropertyProvider;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.security.authorization.AuthorizationHelper;
import org.apache.ambari.server.security.authorization.ResourceType;
import org.apache.ambari.server.security.authorization.RoleAuthorization;
import org.springframework.security.core.Authentication;

/**
 * AbstractAuthorizedResourceProvider helps to provide an authorization layer for a resource provider.
 * <p/>
 * Resource providers that need to perform authorization checks should extend ths abstract class and
 * then override the <code>*ResourcesAuthorized</code> methods and set the <code>required*Authorizations</code>
 * properties.  For more sophisticated authorization checks, the <code>isAuthorizedTo*Resources</code>
 * methods may be overwritten.
 * <p/>
 * Additionally, the {@link AuthorizationHelper#isAuthorized(ResourceType, Long, Set)} or
 * {@link AuthorizationHelper#verifyAuthorization(ResourceType, Long, Set)} methods may be called
 * within the logic of the resource provider implementation to provide checks on particular resources.
 *
 * @see AuthorizationHelper
 */
public abstract class AbstractAuthorizedResourceProvider extends AbstractResourceProvider {

  /**
   * The set of authorizations for which one is needed to the grant access to <b>create</b> resources
   * or a particular resource.
   */
  private Set<RoleAuthorization> requiredCreateAuthorizations = Collections.emptySet();

  /**
   * The set of authorizations for which one is needed to the grant access to <b>get</b> resources
   * or a particular resource.
   */
  private Set<RoleAuthorization> requiredGetAuthorizations = Collections.emptySet();

  /**
   * The set of authorizations for which one is needed to the grant access to <b>update</b> resources
   * or a particular resource.
   */
  private Set<RoleAuthorization> requiredUpdateAuthorizations = Collections.emptySet();

  /**
   * The set of authorizations for which one is needed to the grant access to <b>delete</b> resources
   * or a particular resource.
   */
  private Set<RoleAuthorization> requiredDeleteAuthorizations = Collections.emptySet();

  /**
   * Create a new resource provider. This constructor will initialize the
   * specified {@link Resource.Type} with the provided keys. It should be used
   * in cases where the provider declares its own keys instead of reading them
   * from a JSON file.
   *
   * @param type
   *          the type to set the properties for (not {@code null}).
   * @param propertyIds
   *          the property ids
   * @param keyPropertyIds
   *          the key property ids
   */
  AbstractAuthorizedResourceProvider(Resource.Type type, Set<String> propertyIds,
      Map<Resource.Type, String> keyPropertyIds) {
    super(propertyIds, keyPropertyIds);
    PropertyHelper.setPropertyIds(type, propertyIds);
    PropertyHelper.setKeyPropertyIds(type, keyPropertyIds);
  }

  /**
   * Gets the authorizations for which one is needed to the grant access to <b>create</b> resources
   * or a particular resource.
   * <p/>
   * A null or empty set indicates no authorization check needs to be performed.
   *
   * @return a set of authorizations
   */
  public Set<RoleAuthorization> getRequiredCreateAuthorizations() {
    return requiredCreateAuthorizations;
  }

  /**
   * Sets the authorizations for which one is needed to the grant access to <b>create</b> resources
   * or a particular resource.
   * <p/>
   * A null or empty set indicates no authorization check needs to be performed.
   *
   * @param requiredCreateAuthorizations a set of authorizations
   */
  public void setRequiredCreateAuthorizations(Set<RoleAuthorization> requiredCreateAuthorizations) {
    this.requiredCreateAuthorizations = createUnmodifiableSet(requiredCreateAuthorizations);
  }

  /**
   * Gets the authorizations for which one is needed to the grant access to <b>get</b> resources
   * or a particular resource.
   * <p/>
   * A null or empty set indicates no authorization check needs to be performed.
   *
   * @return a set of authorizations
   */
  public Set<RoleAuthorization> getRequiredGetAuthorizations() {
    return requiredGetAuthorizations;
  }

  /**
   * Sets the authorizations for which one is needed to the grant access to <b>get</b> resources
   * or a particular resource.
   * <p/>
   * A null or empty set indicates no authorization check needs to be performed.
   *
   * @param requiredGetAuthorizations a set of authorizations
   */
  public void setRequiredGetAuthorizations(Set<RoleAuthorization> requiredGetAuthorizations) {
    this.requiredGetAuthorizations = createUnmodifiableSet(requiredGetAuthorizations);
  }

  /**
   * Gets the authorizations for which one is needed to the grant access to <b>update</b> resources
   * or a particular resource.
   * <p/>
   * A null or empty set indicates no authorization check needs to be performed.
   *
   * @return a set of authorizations
   */
  public Set<RoleAuthorization> getRequiredUpdateAuthorizations() {
    return requiredUpdateAuthorizations;
  }

  /**
   * Sets the authorizations for which one is needed to the grant access to <b>update</b> resources
   * or a particular resource.
   * <p/>
   * A null or empty set indicates no authorization check needs to be performed.
   *
   * @param requiredUpdateAuthorizations a set of authorizations
   */
  public void setRequiredUpdateAuthorizations(Set<RoleAuthorization> requiredUpdateAuthorizations) {
    this.requiredUpdateAuthorizations = createUnmodifiableSet(requiredUpdateAuthorizations);
  }

  /**
   * Gets the authorizations for which one is needed to the grant access to <b>delete</b> resources
   * or a particular resource.
   * <p/>
   * A null or empty set indicates no authorization check needs to be performed.
   *
   * @return a set of authorizations
   */
  public Set<RoleAuthorization> getRequiredDeleteAuthorizations() {
    return requiredDeleteAuthorizations;
  }

  /**
   * Sets the authorizations for which one is needed to the grant access to <b>delete</b> resources
   * or a particular resource.
   * <p/>
   * A null or empty set indicates no authorization check needs to be performed.
   *
   * @param requiredDeleteAuthorizations a set of authorizations
   */
  public void setRequiredDeleteAuthorizations(Set<RoleAuthorization> requiredDeleteAuthorizations) {
    this.requiredDeleteAuthorizations = createUnmodifiableSet(requiredDeleteAuthorizations);
  }

  // ----- ResourceProvider --------------------------------------------------

  /**
   * Create the resources defined by the properties in the given request object.
   * <p/>
   * This implementation attempts to authorize the authenticated user before performing the requested
   * operation. If authorization fails, an AuthorizationException will be thrown.
   * <p/>
   * This method may be overwritten by implementing classes to avoid performing authorization checks
   * to create resources.
   *
   * @param request the request object which defines the set of properties
   *                for the resources to be created
   * @return the request status
   * @throws SystemException                an internal system exception occurred
   * @throws UnsupportedPropertyException   the request contains unsupported property ids
   * @throws ResourceAlreadyExistsException attempted to create a resource which already exists
   * @throws NoSuchParentResourceException  a parent resource of the resource to create doesn't exist
   * @throws AuthorizationException         if the authenticated user is not authorized to perform this operation
   */
  @Override
  public RequestStatus createResources(Request request)
      throws SystemException, UnsupportedPropertyException, ResourceAlreadyExistsException, NoSuchParentResourceException {
    Authentication authentication = AuthorizationHelper.getAuthentication();

    if (authentication == null || !authentication.isAuthenticated()) {
      throw new AuthorizationException("Authentication data is not available, authorization to perform the requested operation is not granted");
    } else if (!isAuthorizedToCreateResources(authentication, request)) {
      throw new AuthorizationException("The authenticated user does not have the appropriate authorizations to create the requested resource(s)");
    }

    return createResourcesAuthorized(request);
  }

  /**
   * Get a set of {@link Resource resources} based on the given request and predicate
   * information.
   * </p>
   * Note that it is not required for this resource provider to completely filter
   * the set of resources based on the given predicate.  It may not be possible
   * since some of the properties involved may be provided by another
   * {@link PropertyProvider provider}.  This partial filtering is allowed because
   * the predicate will always be applied by the calling cluster controller.  The
   * predicate is made available at this level so that some pre-filtering can be done
   * as an optimization.
   * </p>
   * A simple implementation of a resource provider may choose to just return all of
   * the resources of a given type and allow the calling cluster controller to filter
   * based on the predicate.
   * <p/>
   * This implementation attempts to authorize the authenticated user before performing the requested
   * operation. If authorization fails, an AuthorizationException will be thrown.
   * <p/>
   * This method may be overwritten by implementing classes to avoid performing authorization checks
   * to get resources.
   *
   * @param request   the request object which defines the desired set of properties
   * @param predicate the predicate object which can be used to filter which
   *                  resources are returned
   * @return a set of resources based on the given request and predicate information
   * @throws SystemException               an internal system exception occurred
   * @throws UnsupportedPropertyException  the request contains unsupported property ids
   * @throws NoSuchResourceException       the requested resource instance doesn't exist
   * @throws NoSuchParentResourceException a parent resource of the requested resource doesn't exist
   * @throws AuthorizationException        if the authenticated user is not authorized to perform this operation
   */
  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    Authentication authentication = AuthorizationHelper.getAuthentication();

    if (authentication == null || !authentication.isAuthenticated()) {
      throw new AuthorizationException("Authentication data is not available, authorization to perform the requested operation is not granted");
    } else if (!isAuthorizedToGetResources(authentication, request, predicate)) {
      throw new AuthorizationException("The authenticated user does not have the appropriate authorizations to get the requested resource(s)");
    }

    return getResourcesAuthorized(request, predicate);
  }

  /**
   * Update the resources selected by the given predicate with the properties
   * from the given request object.
   * <p/>
   * This implementation attempts to authorize the authenticated user before performing the requested
   * operation. If authorization fails, an AuthorizationException will be thrown.
   * <p/>
   * This method may be overwritten by implementing classes to avoid performing authorization checks
   * to update resources.
   *
   * @param request   the request object which defines the set of properties
   *                  for the resources to be updated
   * @param predicate the predicate object which can be used to filter which
   *                  resources are updated
   * @return the request status
   * @throws SystemException               an internal system exception occurred
   * @throws UnsupportedPropertyException  the request contains unsupported property ids
   * @throws NoSuchResourceException       the resource instance to be updated doesn't exist
   * @throws NoSuchParentResourceException a parent resource of the resource doesn't exist
   * @throws AuthorizationException        if the authenticated user is not authorized to perform this operation
   */
  @Override
  public RequestStatus updateResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    Authentication authentication = AuthorizationHelper.getAuthentication();

    if (authentication == null || !authentication.isAuthenticated()) {
      throw new AuthorizationException("Authentication data is not available, authorization to perform the requested operation is not granted");
    } else if (!isAuthorizedToUpdateResources(authentication, request, predicate)) {
      throw new AuthorizationException("The authenticated user does not have the appropriate authorizations to update the requested resource(s)");
    }

    return updateResourcesAuthorized(request, predicate);
  }

  /**
   * Delete the resources selected by the given predicate.
   * <p/>
   * This implementation attempts to authorize the authenticated user before performing the requested
   * operation. If authorization fails, an AuthorizationException will be thrown.
   * <p/>
   * This method may be overwritten by implementing classes to avoid performing authorization checks
   * to delete resources.
   *
   *
   * @param request
   * @param predicate the predicate object which can be used to filter which
   *                  resources are deleted
   * @return the request status
   * @throws SystemException               an internal system exception occurred
   * @throws UnsupportedPropertyException  the request contains unsupported property ids
   * @throws NoSuchResourceException       the resource instance to be deleted doesn't exist
   * @throws NoSuchParentResourceException a parent resource of the resource doesn't exist
   * @throws AuthorizationException        if the authenticated user is not authorized to perform this operation
   */
  @Override
  public RequestStatus deleteResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    Authentication authentication = AuthorizationHelper.getAuthentication();

    if (authentication == null || !authentication.isAuthenticated()) {
      throw new AuthorizationException("Authentication data is not available, authorization to perform the requested operation is not granted");
    } else if (!isAuthorizedToDeleteResources(authentication, predicate)) {
      throw new AuthorizationException("The authenticated user does not have the appropriate authorizations to delete the requested resource(s)");
    }

    return deleteResourcesAuthorized(request, predicate);
  }

  // ----- ResourceProvider (end) --------------------------------------------

  /**
   * Create the resources defined by the properties in the given request object if authorization was
   * granted to the authenticated user.
   * <p/>
   * This method must be overwritten if {@link #createResources(Request)} is not overwritten.
   *
   * @param request the request object which defines the set of properties for the resources to be created
   * @return the request status
   * @throws SystemException                an internal system exception occurred
   * @throws UnsupportedPropertyException   the request contains unsupported property ids
   * @throws ResourceAlreadyExistsException attempted to create a resource which already exists
   * @throws NoSuchParentResourceException  a parent resource of the resource to create doesn't exist
   * @throws AuthorizationException         if the authenticated user is not authorized to perform this operation
   * @see #createResources(Request)
   */
  protected RequestStatus createResourcesAuthorized(Request request)
      throws SystemException, UnsupportedPropertyException, ResourceAlreadyExistsException, NoSuchParentResourceException {
    throw new UnsupportedOperationException("If createResources is not overwritten, then createResourcesAuthorized must be overwritten");
  }

  /**
   * Tests authorization to create resources.
   * <p/>
   * Implementations should override this method to perform a more sophisticated authorization routine.
   *
   * @param authentication the authenticated user and associated access privileges
   * @param request        the request object which defines the set of properties for the resources to be created
   * @return true if authorized; otherwise false
   * @throws SystemException if an internal system exception occurred
   */
  protected boolean isAuthorizedToCreateResources(Authentication authentication, Request request)
      throws SystemException {
    return AuthorizationHelper.isAuthorized(authentication, getResourceType(request, null),
        getResourceId(request, null), requiredCreateAuthorizations);
  }

  /**
   * Get a set of {@link Resource resources} based on the given request and predicate
   * information if the authenticated user is authorized to do so.
   * <p/>
   * This method must be overwritten if {@link #getResources(Request, Predicate)} is not overwritten.
   *
   * @param request   the request object which defines the desired set of properties
   * @param predicate the predicate object which can be used to filter which resources are returned
   * @return a set of resources based on the given request and predicate information
   * @throws SystemException               an internal system exception occurred
   * @throws UnsupportedPropertyException  the request contains unsupported property ids
   * @throws NoSuchResourceException       the requested resource instance doesn't exist
   * @throws NoSuchParentResourceException a parent resource of the requested resource doesn't exist
   * @throws AuthorizationException        if the authenticated user is not authorized to perform this operation
   * @see #getResources(Request, Predicate)
   */
  protected Set<Resource> getResourcesAuthorized(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    throw new UnsupportedOperationException("If getResources is not overwritten, then getResourcesAuthorized must be overwritten");
  }

  /**
   * Tests authorization to get resources.
   * <p/>
   * Implementations should override this method to perform a more sophisticated authorization routine.
   *
   * @param authentication the authenticated user and associated access privileges
   * @param request        the request object which defines the desired set of properties
   * @param predicate      the predicate object which can be used to filter which resources are returned
   * @return true if authorized; otherwise false
   * @throws SystemException if an internal system exception occurred
   */
  protected boolean isAuthorizedToGetResources(Authentication authentication, Request request, Predicate predicate)
      throws SystemException {
    return AuthorizationHelper.isAuthorized(authentication, getResourceType(request, predicate),
        getResourceId(request, predicate), requiredGetAuthorizations);
  }

  /**
   * Update the resources selected by the given predicate with the properties from the given request
   * object if the authenticated user is authorized to do so.
   * <p/>
   * This method must be overwritten if {@link #updateResources(Request, Predicate)} is not overwritten.
   *
   * @param request   the request object which defines the set of properties for the resources to be updated
   * @param predicate the predicate object which can be used to filter which resources are updated
   * @return the request status
   * @throws SystemException               an internal system exception occurred
   * @throws UnsupportedPropertyException  the request contains unsupported property ids
   * @throws NoSuchResourceException       the resource instance to be updated doesn't exist
   * @throws NoSuchParentResourceException a parent resource of the resource doesn't exist
   * @throws AuthorizationException        if the authenticated user is not authorized to perform this operation
   * @see #updateResources(Request, Predicate)
   */
  protected RequestStatus updateResourcesAuthorized(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    throw new UnsupportedOperationException("If updateResources is not overwritten, then updateResourcesAuthorized must be overwritten");
  }

  /**
   * Tests authorization to update resources
   * <p/>
   * Implementations should override this method to perform a more sophisticated authorization routine.
   *
   * @param authentication the authenticated user and associated access privileges
   * @param request        the request object which defines the desired set of properties
   * @param predicate      the predicate object which can be used to filter which resources are returned
   * @return true if authorized; otherwise false
   * @throws SystemException if an internal system exception occurred
   */
  protected boolean isAuthorizedToUpdateResources(Authentication authentication, Request request, Predicate predicate)
      throws SystemException {
    return AuthorizationHelper.isAuthorized(authentication, getResourceType(request, predicate),
        getResourceId(request, predicate), requiredUpdateAuthorizations);
  }

  /**
   * Delete the resources selected by the given predicate if the authenticated user is authorized
   * to do so.
   * <p/>
   * This method must be overwritten if {@link ResourceProvider#deleteResources(Request, Predicate)} is not overwritten.
   *
   *
   * @param request
   * @param predicate the predicate object which can be used to filter which resources are deleted
   * @return the request status
   * @throws SystemException               an internal system exception occurred
   * @throws UnsupportedPropertyException  the request contains unsupported property ids
   * @throws NoSuchResourceException       the resource instance to be deleted doesn't exist
   * @throws NoSuchParentResourceException a parent resource of the resource doesn't exist
   * @throws AuthorizationException        if the authenticated user is not authorized to perform this operation
   * @see ResourceProvider#deleteResources(Request, Predicate)
   */
  protected RequestStatus deleteResourcesAuthorized(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    throw new UnsupportedOperationException("If deleteResources is not overwritten, then deleteResourcesAuthorized must be overwritten");
  }

  /**
   * Tests authorization to delete resources.
   * <p/>
   * Implementations should override this method to perform a more sophisticated authorization routine.
   *
   * @param authentication the authenticated user and associated access privileges
   * @param predicate      the predicate object which can be used to filter which resources are deleted
   * @return true if authorized; otherwise false
   * @throws SystemException if an internal system exception occurred
   */
  protected boolean isAuthorizedToDeleteResources(Authentication authentication, Predicate predicate)
      throws SystemException {
    return AuthorizationHelper.isAuthorized(authentication, getResourceType(null, predicate),
        getResourceId(null, predicate), requiredDeleteAuthorizations);
  }

  /**
   * Gets the ResourceType that is effected by the relevant request and predicate.
   * <p/>
   * Implementations should override this method return the proper ResourceType.
   *
   * @param request   the request object which defines the desired set of properties
   * @param predicate the predicate object which can be used to indicate the filter
   * @return a ResourceType
   */
  protected ResourceType getResourceType(Request request, Predicate predicate) {
    return ResourceType.CLUSTER;
  }

  /**
   * Gets the identifier, relative to the the effected ResourceType that is effected by the relevant
   * request and predicate.
   * <p/>
   * Implementations should override this method return the proper resource id.
   *
   * @param request   the request object which defines the desired set of properties
   * @param predicate the predicate object which can be used to indicate the filter
   * @return a resource id; or null to indicate any or all resources of the relevant type
   */
  protected Long getResourceId(Request request, Predicate predicate) {
    return null;
  }

  /**
   * Safely create an unmodifiable set of RoleAuthorizations
   *
   * @param set the set to copy
   * @return an unmodifiable set of RoleAuthorizations
   */
  private Set<RoleAuthorization> createUnmodifiableSet(Set<RoleAuthorization> set) {
    return (set == null)
        ? Collections.emptySet()
        : Collections.unmodifiableSet(new HashSet<>(set));
  }
}

