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
import org.apache.ambari.server.audit.event.AccessUnauthorizedAuditEvent;
import org.apache.ambari.server.audit.event.AuditEvent;
import org.apache.ambari.server.controller.spi.Resource;

import com.google.common.collect.ImmutableSet;

/**
 * This creator handles unauthorized actions
 * For result status {@link ResultStatus.STATUS#UNAUTHORIZED} and {@link ResultStatus.STATUS#FORBIDDEN}
 */
public class UnauthorizedEventCreator implements RequestAuditEventCreator {

  /**
   * Set of {@link Request.Type}s that are handled by this plugin
   */
  private Set<ResultStatus.STATUS> statuses = ImmutableSet.<ResultStatus.STATUS>builder().add(ResultStatus.STATUS.UNAUTHORIZED, ResultStatus.STATUS.FORBIDDEN).build();

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Request.Type> getRequestTypes() {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Resource.Type> getResourceTypes() {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<ResultStatus.STATUS> getResultStatuses() {
    return statuses;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public AuditEvent createAuditEvent(Request request, Result result) {

    AccessUnauthorizedAuditEvent ae = AccessUnauthorizedAuditEvent.builder()
      .withRemoteIp(request.getRemoteAddress())
      .withResourcePath(request.getURI())
      .withTimestamp(System.currentTimeMillis())
      .build();

    return ae;
  }
}
