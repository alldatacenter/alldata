/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.audit.request.eventcreator;

import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.api.services.ResultStatus;
import org.apache.ambari.server.audit.event.AuditEvent;
import org.apache.ambari.server.audit.event.request.ActivateUserRequestAuditEvent;
import org.apache.ambari.server.audit.event.request.AdminUserRequestAuditEvent;
import org.apache.ambari.server.audit.event.request.CreateUserRequestAuditEvent;
import org.apache.ambari.server.audit.event.request.DeleteUserRequestAuditEvent;
import org.apache.ambari.server.audit.event.request.UserPasswordChangeRequestAuditEvent;
import org.apache.ambari.server.controller.internal.UserResourceProvider;
import org.apache.ambari.server.controller.spi.Resource;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

/**
 * This creator handles user requests
 * For resource type {@link Resource.Type#User}
 * and request types {@link Request.Type#POST}, {@link Request.Type#PUT} and {@link Request.Type#DELETE}
 */
public class UserEventCreator implements RequestAuditEventCreator {

  /**
   * Set of {@link Request.Type}s that are handled by this plugin
   */
  private Set<Request.Type> requestTypes = ImmutableSet.<Request.Type>builder().add(Request.Type.PUT, Request.Type.POST, Request.Type.DELETE).build();

  /**
   * Set of {@link Resource.Type}s that are handled by this plugin
   */
  private Set<Resource.Type> resourceTypes = ImmutableSet.<Resource.Type>builder().add(Resource.Type.User).build();

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Request.Type> getRequestTypes() {
    return requestTypes;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Resource.Type> getResourceTypes() {
    return resourceTypes;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<ResultStatus.STATUS> getResultStatuses() {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public AuditEvent createAuditEvent(Request request, Result result) {

    switch (request.getRequestType()) {
      case POST:
        return CreateUserRequestAuditEvent.builder()
          .withTimestamp(System.currentTimeMillis())
          .withRequestType(request.getRequestType())
          .withResultStatus(result.getStatus())
          .withUrl(request.getURI())
          .withRemoteIp(request.getRemoteAddress())
          .withCreatedUsername(getUsername(request))
          .withActive(isActive(request))
          .withAdmin(isAdmin(request))
          .build();
      case DELETE:
        return DeleteUserRequestAuditEvent.builder()
          .withTimestamp(System.currentTimeMillis())
          .withRequestType(request.getRequestType())
          .withResultStatus(result.getStatus())
          .withUrl(request.getURI())
          .withRemoteIp(request.getRemoteAddress())
          .withDeletedUsername(request.getResource().getKeyValueMap().get(Resource.Type.User))
          .build();
      case PUT:
        if (hasActive(request)) {
          return ActivateUserRequestAuditEvent.builder()
            .withTimestamp(System.currentTimeMillis())
            .withRequestType(request.getRequestType())
            .withResultStatus(result.getStatus())
            .withUrl(request.getURI())
            .withRemoteIp(request.getRemoteAddress())
              .withAffectedUsername(getUsername(request))
            .withActive(isActive(request))
            .build();
        }
        if (hasAdmin(request)) {
          return AdminUserRequestAuditEvent.builder()
            .withTimestamp(System.currentTimeMillis())
            .withRequestType(request.getRequestType())
            .withResultStatus(result.getStatus())
            .withUrl(request.getURI())
            .withRemoteIp(request.getRemoteAddress())
              .withAffectedUsername(getUsername(request))
            .withAdmin(isAdmin(request))
            .build();
        }
        if (hasOldPassword(request)) {
          return UserPasswordChangeRequestAuditEvent.builder()
            .withTimestamp(System.currentTimeMillis())
            .withRequestType(request.getRequestType())
            .withResultStatus(result.getStatus())
            .withUrl(request.getURI())
            .withRemoteIp(request.getRemoteAddress())
              .withAffectedUsername(getUsername(request))
            .build();
        }
        break;
      default:
        break;
    }
    return null;
  }


  /**
   * Returns fromt he request if the user has admin rights
   * @param request
   * @return
   */
  private boolean isAdmin(Request request) {
    return hasAdmin(request) && "true".equals(RequestAuditEventCreatorHelper.getProperty(request, UserResourceProvider.USER_ADMIN_PROPERTY_ID));
  }

  /**
   * Returns from the request if the user is active
   * @param request
   * @return
   */
  private boolean isActive(Request request) {
    return hasActive(request) && "true".equals(RequestAuditEventCreatorHelper.getProperty(request, UserResourceProvider.USER_ACTIVE_PROPERTY_ID));
  }

  /**
   * Returns if the request contains admin property
   * @param request
   * @return
   */
  private boolean hasAdmin(Request request) {
    Map<String, Object> first = Iterables.getFirst(request.getBody().getPropertySets(), null);
    return first != null && first.containsKey(UserResourceProvider.USER_ADMIN_PROPERTY_ID);
  }

  /**
   * Returns if the request contains active property
   * @param request
   * @return
   */
  private boolean hasActive(Request request) {
    Map<String, Object> first = Iterables.getFirst(request.getBody().getPropertySets(), null);
    return first != null && first.containsKey(UserResourceProvider.USER_ACTIVE_PROPERTY_ID);
  }

  /**
   * Returns if the request contains old password field
   * @param request
   * @return
   */
  private boolean hasOldPassword(Request request) {
    Map<String, Object> first = Iterables.getFirst(request.getBody().getPropertySets(), null);
    return first != null && first.containsKey(UserResourceProvider.USER_OLD_PASSWORD_PROPERTY_ID);
  }

  /**
   * Returns the username from the request
   * @param request
   * @return
   */
  private String getUsername(Request request) {
    Map<String, Object> first = Iterables.getFirst(request.getBody().getPropertySets(), null);
    if (first != null) {
      return String.valueOf(first.get(UserResourceProvider.USER_USERNAME_PROPERTY_ID));
    }
    return null;
  }

}
