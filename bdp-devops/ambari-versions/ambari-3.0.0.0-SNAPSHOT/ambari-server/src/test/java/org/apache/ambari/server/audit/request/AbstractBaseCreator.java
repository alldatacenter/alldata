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

package org.apache.ambari.server.audit.request;

import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.audit.event.AuditEvent;
import org.apache.ambari.server.audit.request.eventcreator.RequestAuditEventCreator;

public abstract class AbstractBaseCreator implements RequestAuditEventCreator {

  public String getPrefix() {
    return this.getClass().getName();
  }

  @Override
  public AuditEvent createAuditEvent(final Request request, final Result result) {
    return new AuditEvent() {
      @Override
      public Long getTimestamp() {
        return System.currentTimeMillis();
      }

      @Override
      public String getAuditMessage() {
        return getPrefix() + " " + String.format("%s %s %s %s %s", request.getRequestType(), request.getURI(), result.getStatus().getStatusCode(), result.getStatus().getStatus(), result.getStatus().getMessage());
      }
    };
  }
}
