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

import org.apache.ambari.server.audit.AuditLogger;
import org.apache.ambari.server.audit.AuditLoggerDefaultImpl;
import org.apache.ambari.server.audit.request.eventcreator.RequestAuditEventCreator;
import org.easymock.EasyMock;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

public class RequestAuditLogModule extends AbstractModule {

  @Override
  protected void configure() {
    Multibinder<RequestAuditEventCreator> auditLogEventCreatorBinder = Multibinder.newSetBinder(binder(), RequestAuditEventCreator.class);
    auditLogEventCreatorBinder.addBinding().to(AllPostAndPutCreator.class);
    auditLogEventCreatorBinder.addBinding().to(AllGetCreator.class);
    auditLogEventCreatorBinder.addBinding().to(PutHostComponentCreator.class);

    bind(AuditLogger.class).toInstance(EasyMock.createStrictMock(AuditLoggerDefaultImpl.class));
    bind(RequestAuditLogger.class).to(RequestAuditLoggerImpl.class);
  }
}
