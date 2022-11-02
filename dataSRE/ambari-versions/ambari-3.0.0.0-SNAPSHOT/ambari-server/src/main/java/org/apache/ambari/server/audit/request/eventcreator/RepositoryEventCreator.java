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

import java.util.Set;

import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.api.services.ResultStatus;
import org.apache.ambari.server.audit.event.AuditEvent;
import org.apache.ambari.server.audit.event.request.AddRepositoryRequestAuditEvent;
import org.apache.ambari.server.audit.event.request.UpdateRepositoryRequestAuditEvent;
import org.apache.ambari.server.controller.internal.RepositoryResourceProvider;
import org.apache.ambari.server.controller.spi.Resource;

import com.google.common.collect.ImmutableSet;

/**
 * This creator handles privilege requests
 * For resource type {@link Resource.Type#Repository}
 * and request types {@link Request.Type#POST} and {@link Request.Type#PUT}
 */
public class RepositoryEventCreator implements RequestAuditEventCreator {

  /**
   * Set of {@link Request.Type}s that are handled by this plugin
   */
  private Set<Request.Type> requestTypes = ImmutableSet.<Request.Type>builder().add(Request.Type.PUT, Request.Type.POST).build();

  /**
   * Set of {@link Resource.Type}s that are handled by this plugin
   */
  private Set<Resource.Type> resourceTypes = ImmutableSet.<Resource.Type>builder().add(Resource.Type.Repository).build();

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
        return AddRepositoryRequestAuditEvent.builder()
          .withTimestamp(System.currentTimeMillis())
          .withRequestType(request.getRequestType())
          .withResultStatus(result.getStatus())
          .withUrl(request.getURI())
          .withRemoteIp(request.getRemoteAddress())
          .withRepo(RequestAuditEventCreatorHelper.getProperty(request, RepositoryResourceProvider.REPOSITORY_REPO_ID_PROPERTY_ID))
          .withStackName(RequestAuditEventCreatorHelper.getProperty(request, RepositoryResourceProvider.REPOSITORY_STACK_NAME_PROPERTY_ID))
          .withStackVersion(RequestAuditEventCreatorHelper.getProperty(request, RepositoryResourceProvider.REPOSITORY_STACK_VERSION_PROPERTY_ID))
          .withOsType(RequestAuditEventCreatorHelper.getProperty(request, RepositoryResourceProvider.REPOSITORY_OS_TYPE_PROPERTY_ID))
          .withBaseUrl(RequestAuditEventCreatorHelper.getProperty(request, RepositoryResourceProvider.REPOSITORY_BASE_URL_PROPERTY_ID))
          .build();
      case PUT:
        return UpdateRepositoryRequestAuditEvent.builder()
          .withTimestamp(System.currentTimeMillis())
          .withRequestType(request.getRequestType())
          .withResultStatus(result.getStatus())
          .withUrl(request.getURI())
          .withRemoteIp(request.getRemoteAddress())
          .withRepo(RequestAuditEventCreatorHelper.getProperty(request, RepositoryResourceProvider.REPOSITORY_REPO_ID_PROPERTY_ID))
          .withStackName(RequestAuditEventCreatorHelper.getProperty(request, RepositoryResourceProvider.REPOSITORY_STACK_NAME_PROPERTY_ID))
          .withStackVersion(RequestAuditEventCreatorHelper.getProperty(request, RepositoryResourceProvider.REPOSITORY_STACK_VERSION_PROPERTY_ID))
          .withOsType(RequestAuditEventCreatorHelper.getProperty(request, RepositoryResourceProvider.REPOSITORY_OS_TYPE_PROPERTY_ID))
          .withBaseUrl(RequestAuditEventCreatorHelper.getProperty(request, RepositoryResourceProvider.REPOSITORY_BASE_URL_PROPERTY_ID))
          .build();
      default:
        return null;
    }
  }
}
