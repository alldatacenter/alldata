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
import org.apache.ambari.server.audit.event.request.DeleteServiceRequestAuditEvent;
import org.apache.ambari.server.audit.event.request.StartOperationRequestAuditEvent;
import org.apache.ambari.server.controller.internal.RequestOperationLevel;
import org.apache.ambari.server.controller.internal.ServiceResourceProvider;
import org.apache.ambari.server.controller.spi.Resource;

import com.google.common.collect.ImmutableSet;

/**
 * This creator handles operation requests (start, stop, install, etc)
 * For resource type {@link Resource.Type#Service}
 * and request types {@link Request.Type#POST}, {@link Request.Type#PUT} and {@link Request.Type#DELETE}
 */
public class ServiceEventCreator implements RequestAuditEventCreator {

  /**
   * Set of {@link Request.Type}s that are handled by this plugin
   */
  private Set<Request.Type> requestTypes = ImmutableSet.<Request.Type>builder().add(Request.Type.PUT, Request.Type.POST, Request.Type.DELETE).build();

  /**
   * Set of {@link Resource.Type}s that are handled by this plugin
   */
  private Set<Resource.Type> resourceTypes = ImmutableSet.<Resource.Type>builder().add(Resource.Type.Service).build();

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
    // null makes this default
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public AuditEvent createAuditEvent(Request request, Result result) {

    if (request.getRequestType() == Request.Type.DELETE) {
      return DeleteServiceRequestAuditEvent.builder()
        .withTimestamp(System.currentTimeMillis())
        .withRequestType(request.getRequestType())
        .withResultStatus(result.getStatus())
        .withUrl(request.getURI())
        .withRemoteIp(request.getRemoteAddress())
          .withService(request.getResource().getKeyValueMap().get(Resource.Type.Service))
        .build();
    }

    String operation = getOperation(request);

    Long requestId = null;
    if (containsRequestId(result)) {
      requestId = getRequestId(result);
    }

    StartOperationRequestAuditEvent.StartOperationAuditEventBuilder auditEventBuilder = StartOperationRequestAuditEvent.builder()
      .withOperation(operation)
      .withRemoteIp(request.getRemoteAddress())
      .withTimestamp(System.currentTimeMillis())
      .withRequestId(String.valueOf(requestId));

    if (result.getStatus().isErrorState()) {
      auditEventBuilder.withReasonOfFailure(result.getStatus().getMessage());
    }

    return auditEventBuilder.build();
  }

  /**
   * Generates operation name based on the request. It checks the operation level, the host name, the service name, the status
   * and whether this is a maintenance mode switch change.
   * @param request
   * @return
   */
  private String getOperation(Request request) {
    if (request.getBody().getRequestInfoProperties() != null && request.getBody().getRequestInfoProperties().containsKey(RequestOperationLevel.OPERATION_LEVEL_ID)) {
      String operation = "";
      if ("CLUSTER".equals(request.getBody().getRequestInfoProperties().get(RequestOperationLevel.OPERATION_LEVEL_ID))) {
        for (Map<String, Object> map : request.getBody().getPropertySets()) {
          if (map.containsKey(ServiceResourceProvider.SERVICE_SERVICE_STATE_PROPERTY_ID)) {
            operation = String.valueOf(map.get(ServiceResourceProvider.SERVICE_SERVICE_STATE_PROPERTY_ID)) + ": all services"
              + " (" + request.getBody().getRequestInfoProperties().get(RequestOperationLevel.OPERATION_CLUSTER_ID) + ")";
            break;
          }
        }
      }
      if ("SERVICE".equals(request.getBody().getRequestInfoProperties().get(RequestOperationLevel.OPERATION_LEVEL_ID))) {
        for (Map<String, Object> map : request.getBody().getPropertySets()) {
          if (map.containsKey(ServiceResourceProvider.SERVICE_SERVICE_STATE_PROPERTY_ID)) {
            operation = String.valueOf(map.get(ServiceResourceProvider.SERVICE_SERVICE_STATE_PROPERTY_ID)) + ": " + map.get(ServiceResourceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID)
              + " (" + request.getBody().getRequestInfoProperties().get(RequestOperationLevel.OPERATION_CLUSTER_ID) + ")";
            break;
          }
        }
      }
      return operation;
    }

    for (Map<String, Object> map : request.getBody().getPropertySets()) {
      if (map.containsKey(ServiceResourceProvider.SERVICE_MAINTENANCE_STATE_PROPERTY_ID)) {
        return "Turn " + map.get(ServiceResourceProvider.SERVICE_MAINTENANCE_STATE_PROPERTY_ID) + " Maintenance Mode for " + map.get(ServiceResourceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID);
      }
    }
    return null;
  }

  /**
   * Returns request id from the result
   * @param result
   * @return
   */
  private Long getRequestId(Result result) {
    return (Long) result.getResultTree().getChild("request").getObject().getPropertiesMap().get("Requests").get("id");
  }

  /**
   * Checks if request id can be found in the result
   * @param result
   * @return
   */
  private boolean containsRequestId(Result result) {
    return result.getResultTree().getChild("request") != null
      && result.getResultTree().getChild("request").getObject() != null
      && result.getResultTree().getChild("request").getObject().getPropertiesMap().get("Requests") != null
      && result.getResultTree().getChild("request").getObject().getPropertiesMap().get("Requests").get("id") != null;
  }
}
