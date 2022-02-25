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

package org.apache.ambari.server.audit.request;


import java.util.Set;

import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.api.services.ResultStatus;
import org.apache.ambari.server.audit.AuditLogger;
import org.apache.ambari.server.audit.event.AuditEvent;
import org.apache.ambari.server.audit.request.eventcreator.RequestAuditEventCreator;
import org.apache.ambari.server.controller.spi.Resource;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * The purpose of this class is to create audit log entries for the HTTP requests
 */
@Singleton
public class RequestAuditLoggerImpl implements RequestAuditLogger {

  /**
   * Priorities for searching the proper creator
   */
  private static final int REQUEST_TYPE_PRIORITY = 1;
  private static final int RESULT_STATUS_PRIORITY = 2;
  private static final int RESOURCE_TYPE_PRIORITY = 4;

  /**
   * Container for the {@link RequestAuditEventCreator}
   */
  private Set<RequestAuditEventCreator> creators;

  /**
   * Audit logger that receives {@link AuditEvent}s and does the actual logging
   */
  private AuditLogger auditLogger;

  /**
   * Injecting dependencies through the constructor
   * @param auditLogger Audit Logger
   * @param creatorSet Set of plugins that are registered for requests
   */
  @Inject
  public RequestAuditLoggerImpl(AuditLogger auditLogger, Set<RequestAuditEventCreator> creatorSet) {
    this.auditLogger = auditLogger;
    this.creators = creatorSet;
  }

  /**
   * Finds the proper creator, then creates and logs and {@link AuditEvent}
   * @param request
   * @param result
   */
  @Override
  public void log(Request request, Result result) {
    if(!auditLogger.isEnabled()) {
      return;
    }

    Resource.Type resourceType = request.getResource().getResourceDefinition().getType();
    Request.Type requestType = request.getRequestType();
    ResultStatus resultStatus = result.getStatus();

    RequestAuditEventCreator creator = selectCreator(resourceType, resultStatus, requestType);
    if (creator != null) {
      AuditEvent ae = creator.createAuditEvent(request, result);
      if (ae != null) {
        auditLogger.log(ae);
      }
    }
  }

  /**
   * Select the proper creator. Priority order: resourceType > resultStatus > requestType
   * The most matching creator is returned
   * If there is no creator found, then null is returned.
   * @param resourceType
   * @param requestType
   * @param resultStatus
   * @return
   */
  private RequestAuditEventCreator selectCreator(Resource.Type resourceType, ResultStatus resultStatus, Request.Type requestType) {

    RequestAuditEventCreator selected = null;
    Integer priority = -1;

    for (RequestAuditEventCreator creator : creators) {
      Integer creatorPriority = getPriority(creator, resourceType, resultStatus, requestType);
      if (creatorPriority != null && priority < creatorPriority) {
        priority = creatorPriority;
        selected = creator;
      }
    }
    return selected;
  }

  /**
   * Calculates the creator priority for the actual resouce type, result status and request type
   * @param creator
   * @param resourceType
   * @param resultStatus
   * @param requestType
   * @return
   */
  private Integer getPriority(RequestAuditEventCreator creator, Resource.Type resourceType, ResultStatus resultStatus, Request.Type requestType) {
    Integer priority = 0;

    if (isIncompatible(creator, resourceType, resultStatus, requestType)) {
      return null;
    }

    priority += creator.getRequestTypes() != null && creator.getRequestTypes().contains(requestType) ? REQUEST_TYPE_PRIORITY : 0;
    priority += creator.getResultStatuses() != null && creator.getResultStatuses().contains(resultStatus.getStatus()) ? RESULT_STATUS_PRIORITY : 0;
    priority += creator.getResourceTypes() != null && creator.getResourceTypes().contains(resourceType) ? RESOURCE_TYPE_PRIORITY : 0;
    return priority;
  }

  /**
   * Checks if the creator is a possible candidate for creating audit log event for the request
   * @param creator
   * @param resourceType
   * @param resultStatus
   * @param requestType
   * @return
   */
  private boolean isIncompatible(RequestAuditEventCreator creator, Resource.Type resourceType, ResultStatus resultStatus, Request.Type requestType) {
    return creator.getRequestTypes() != null && !creator.getRequestTypes().contains(requestType) ||
      creator.getResultStatuses() != null && !creator.getResultStatuses().contains(resultStatus.getStatus()) ||
      creator.getResourceTypes() != null && !creator.getResourceTypes().contains(resourceType);
  }
}
