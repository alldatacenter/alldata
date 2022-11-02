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
import org.apache.ambari.server.controller.spi.Resource;

/**
 * This interface must be implemented by the plugins for the request audit logger
 * in order to make custom {@link AuditEvent}s based on {@link org.apache.ambari.server.api.services.Request.Type}s
 * and {@link org.apache.ambari.server.controller.spi.Resource.Type}
 */
public interface RequestAuditEventCreator {

  /**
   * @return the set of {@link org.apache.ambari.server.api.services.Request.Type}s that are handled by this creator
   */
  Set<Request.Type> getRequestTypes();

  /**
   * @return the {@link org.apache.ambari.server.controller.spi.Resource.Type}s that is handled by this creator
   */
  Set<Resource.Type> getResourceTypes();

  /**
   * @return the {@link ResultStatus}es that is handled by this creator
   */
  Set<ResultStatus.STATUS> getResultStatuses();

  /**
   * Creates and {@link AuditEvent}
   * @param request HTTP request object
   * @param result HTTP result object
   * @return an {@link AuditEvent}
   */
  AuditEvent createAuditEvent(Request request, Result result);

}
