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
import org.apache.ambari.server.audit.event.request.StartOperationRequestAuditEvent;
import org.apache.ambari.server.controller.internal.HostComponentResourceProvider;
import org.apache.ambari.server.controller.internal.RequestOperationLevel;
import org.apache.ambari.server.controller.spi.Resource;

import com.google.common.collect.ImmutableSet;

/**
 * This creator handles operation requests (start, stop, install, etc)
 * For resource type {@link org.apache.ambari.server.controller.spi.Resource.Type#HostComponent}
 * and request types {@link org.apache.ambari.server.api.services.Request.Type#POST}, {@link org.apache.ambari.server.api.services.Request.Type#PUT} and {@link org.apache.ambari.server.api.services.Request.Type#DELETE}
 */
public class ComponentEventCreator implements RequestAuditEventCreator {

  /**
   * Set of {@link Request.Type}s that are handled by this plugin
   */
  private Set<Request.Type> requestTypes = ImmutableSet.<Request.Type>builder().add(Request.Type.PUT, Request.Type.POST, Request.Type.DELETE).build();

  /**
   * Set of {@link Resource.Type}s that are handled by this plugin
   */
  private Set<Resource.Type> resourceTypes = ImmutableSet.<Resource.Type>builder().add(Resource.Type.HostComponent).build();

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

    String operation = getOperation(request);

    Long requestId = null;
    if (containsRequestId(result)) {
      requestId = getRequestId(result);
    }

    StartOperationRequestAuditEvent.StartOperationAuditEventBuilder auditEventBuilder = StartOperationRequestAuditEvent.builder()
      .withOperation(operation)
      .withRemoteIp(request.getRemoteAddress())
      .withTimestamp(System.currentTimeMillis())
      .withHostname(RequestAuditEventCreatorHelper.getProperty(request, HostComponentResourceProvider.HOST_NAME))
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
    if (request.getRequestType() == Request.Type.DELETE) {
      return "Delete component " + request.getResource().getKeyValueMap().get(Resource.Type.HostComponent);
    }

    if (request.getBody().getRequestInfoProperties() != null && request.getBody().getRequestInfoProperties().containsKey(RequestOperationLevel.OPERATION_LEVEL_ID)) {
      String operation = "";
      switch (request.getBody().getRequestInfoProperties().get(RequestOperationLevel.OPERATION_LEVEL_ID)) {
        case "CLUSTER":
          for (Map<String, Object> map : request.getBody().getPropertySets()) {
            if (map.containsKey(HostComponentResourceProvider.CLUSTER_NAME)) {
              operation = String.valueOf(map.get(HostComponentResourceProvider.STATE)) + ": all services"
                + " on all hosts"
                + (request.getBody().getQueryString() != null && request.getBody().getQueryString().length() > 0 ? " that matches " + request.getBody().getQueryString() : "")
                + " (" + request.getBody().getRequestInfoProperties().get(RequestOperationLevel.OPERATION_CLUSTER_ID) + ")";
              break;
            }
          }
          break;
        case "HOST":
          for (Map<String, Object> map : request.getBody().getPropertySets()) {
            if (map.containsKey(HostComponentResourceProvider.CLUSTER_NAME)) {
              String query = request.getBody().getRequestInfoProperties().get("query");
              operation = String.valueOf(map.get(HostComponentResourceProvider.STATE)) + ": " + query.substring(query.indexOf("(") + 1, query.length() - 1)
                + " on " + request.getBody().getRequestInfoProperties().get(RequestOperationLevel.OPERATION_HOST_NAME)
                + " (" + request.getBody().getRequestInfoProperties().get(RequestOperationLevel.OPERATION_CLUSTER_ID) + ")";
              break;
            }
          }
          break;
        case "HOST_COMPONENT":
          for (Map<String, Object> map : request.getBody().getPropertySets()) {
            if (map.containsKey(HostComponentResourceProvider.COMPONENT_NAME)) {
              operation = String.valueOf(map.get(HostComponentResourceProvider.STATE)) + ": " + String.valueOf(map.get(HostComponentResourceProvider.COMPONENT_NAME))
                + "/" + request.getBody().getRequestInfoProperties().get(RequestOperationLevel.OPERATION_SERVICE_ID)
                + " on " + request.getBody().getRequestInfoProperties().get(RequestOperationLevel.OPERATION_HOST_NAME)
                + " (" + request.getBody().getRequestInfoProperties().get(RequestOperationLevel.OPERATION_CLUSTER_ID) + ")";
              break;
            }
          }
          break;
      }
      return operation;
    }

    for (Map<String, Object> map : request.getBody().getPropertySets()) {
      if (map.containsKey(HostComponentResourceProvider.MAINTENANCE_STATE)) {
        return "Turn " + map.get(HostComponentResourceProvider.MAINTENANCE_STATE) + " Maintenance Mode for " + map.get(HostComponentResourceProvider.COMPONENT_NAME);
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
