/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.audit.request.eventcreator;

import java.util.EnumSet;
import java.util.Set;

import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.api.services.ResultStatus;
import org.apache.ambari.server.audit.event.AuditEvent;
import org.apache.ambari.server.audit.event.request.DefaultRequestAuditEvent;
import org.apache.ambari.server.audit.request.RequestAuditLogger;
import org.apache.ambari.server.controller.spi.Resource;

import com.google.common.collect.ImmutableSet;

/**
 * Default creator for {@link RequestAuditLogger}
 */
public class DefaultEventCreator implements RequestAuditEventCreator {

  /**
   * Set of {@link org.apache.ambari.server.api.services.Request.Type}s that are handled by this plugin
   * In this case all {@link Request.Type}s are listed, except {@link Request.Type#GET}
   */
  private Set<Request.Type> requestTypes = ImmutableSet.<Request.Type>builder().addAll(EnumSet.complementOf(EnumSet.of(Request.Type.GET))).build();


  /** {@inheritDoc} */
  @Override
  public Set<Request.Type> getRequestTypes() {
    return requestTypes;
  }

  /** {@inheritDoc} */
  @Override
  public Set<Resource.Type> getResourceTypes() {
    // null makes this default
    return null;
  }

  /** {@inheritDoc} */
  @Override
  public Set<ResultStatus.STATUS> getResultStatuses() {
    // null makes this default
    return null;
  }

  /**
   * Creates a simple {@link AuditEvent} with the details of request and response
   * @param request HTTP request object
   * @param result HTTP result object
   * @return
   */
  @Override
  public AuditEvent createAuditEvent(final Request request, final Result result) {

    return DefaultRequestAuditEvent.builder()
      .withTimestamp(System.currentTimeMillis())
      .withRemoteIp(request.getRemoteAddress())
      .withRequestType(request.getRequestType())
      .withUrl(request.getURI())
      .withResultStatus(result.getStatus())
      .build();
  }

}
