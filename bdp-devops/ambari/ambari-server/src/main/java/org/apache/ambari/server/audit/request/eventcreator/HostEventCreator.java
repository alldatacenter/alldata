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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.ambari.server.api.services.NamedPropertySet;
import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.api.services.ResultStatus;
import org.apache.ambari.server.audit.event.AuditEvent;
import org.apache.ambari.server.audit.event.request.AddComponentToHostRequestAuditEvent;
import org.apache.ambari.server.audit.event.request.AddHostRequestAuditEvent;
import org.apache.ambari.server.audit.event.request.DeleteHostRequestAuditEvent;
import org.apache.ambari.server.controller.internal.HostComponentResourceProvider;
import org.apache.ambari.server.controller.internal.HostResourceProvider;
import org.apache.ambari.server.controller.spi.Resource;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

/**
 * This creator handles host requests (add, delete, add component)
 * For resource type {@link Resource.Type#HostComponent}
 * and request types {@link Request.Type#POST}, {@link Request.Type#DELETE} and {@link Request.Type#QUERY_POST}
 */
public class HostEventCreator implements RequestAuditEventCreator {

  /**
   * Set of {@link Request.Type}s that are handled by this plugin
   */
  private Set<Request.Type> requestTypes = ImmutableSet.<Request.Type>builder().add(Request.Type.QUERY_POST, Request.Type.POST, Request.Type.DELETE).build();

  /**
   * Set of {@link Resource.Type}s that are handled by this plugin
   */
  private Set<Resource.Type> resourceTypes = ImmutableSet.<Resource.Type>builder().add(Resource.Type.Host).build();

  /**
   * Pattern to retrieve hostname from url
   */
  private static final Pattern HOSTNAME_PATTERN = Pattern.compile(".*" + HostResourceProvider.HOST_HOST_NAME_PROPERTY_ID + "\\s*=\\s*([^&\\s]+).*");

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

    switch (request.getRequestType()) {
      case DELETE:
        return DeleteHostRequestAuditEvent.builder()
          .withTimestamp(System.currentTimeMillis())
          .withRequestType(request.getRequestType())
          .withResultStatus(result.getStatus())
          .withUrl(request.getURI())
          .withRemoteIp(request.getRemoteAddress())
          .withHostName(request.getResource().getKeyValueMap().get(Resource.Type.Host))
          .build();
      case POST:
        return AddHostRequestAuditEvent.builder()
          .withTimestamp(System.currentTimeMillis())
          .withRequestType(request.getRequestType())
          .withResultStatus(result.getStatus())
          .withUrl(request.getURI())
          .withRemoteIp(request.getRemoteAddress())
          .withHostName(RequestAuditEventCreatorHelper.getNamedProperty(request, HostResourceProvider.HOST_HOST_NAME_PROPERTY_ID))
          .build();
      case QUERY_POST:
        return AddComponentToHostRequestAuditEvent.builder()
          .withTimestamp(System.currentTimeMillis())
          .withRequestType(request.getRequestType())
          .withResultStatus(result.getStatus())
          .withUrl(request.getURI())
          .withRemoteIp(request.getRemoteAddress())
          .withHostName(getHostNameFromQuery(request))
          .withComponents(getHostComponents(request))
          .build();
      default:
        return null;
    }
  }

  /**
   * Returns component name from the request
   * @param request
   * @return
   */
  private Set<String> getHostComponents(Request request) {
    Set<String> components = new HashSet<>();
    NamedPropertySet propertySet = Iterables.getFirst(request.getBody().getNamedPropertySets(), null);
    if (propertySet != null && propertySet.getProperties().get("host_components") instanceof Set) {
      Set<Map<String, String>> set = (Set<Map<String, String>>) propertySet.getProperties().get("host_components");
      if (set != null && !set.isEmpty()) {
        for(Map<String, String> element : set) {
          components.add(element.get(HostComponentResourceProvider.COMPONENT_NAME));
        }
      }
    }
    return components;
  }

  /**
   * Returns hostname from the query string of the request
   * @param request
   * @return
   */
  private String getHostNameFromQuery(Request request) {
    Matcher matcher = HOSTNAME_PATTERN.matcher(request.getURI());
    if(matcher.find()) {
      return matcher.group(1);
    }
    return null;
  }
}
