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

package org.apache.ambari.server.audit.request.creator;

import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.api.services.ResultStatus;
import org.apache.ambari.server.audit.event.AuditEvent;
import org.apache.ambari.server.audit.event.request.AddRepositoryRequestAuditEvent;
import org.apache.ambari.server.audit.event.request.UpdateRepositoryRequestAuditEvent;
import org.apache.ambari.server.audit.request.eventcreator.RepositoryEventCreator;
import org.apache.ambari.server.controller.internal.RepositoryResourceProvider;
import org.apache.ambari.server.controller.spi.Resource;
import org.junit.Test;

import junit.framework.Assert;

public class RepositoryEventCreatorTest extends AuditEventCreatorTestBase{

  @Test
  public void postTest() {
    RepositoryEventCreator creator = new RepositoryEventCreator();

    Map<String,Object> properties = new HashMap<>();
    properties.put(RepositoryResourceProvider.REPOSITORY_REPO_ID_PROPERTY_ID, "Repo1");
    properties.put(RepositoryResourceProvider.REPOSITORY_STACK_NAME_PROPERTY_ID, "StackName");
    properties.put(RepositoryResourceProvider.REPOSITORY_STACK_VERSION_PROPERTY_ID, "1.2-56");
    properties.put(RepositoryResourceProvider.REPOSITORY_OS_TYPE_PROPERTY_ID, "redhat7");
    properties.put(RepositoryResourceProvider.REPOSITORY_BASE_URL_PROPERTY_ID, "http://example.com");

    Request request = AuditEventCreatorTestHelper.createRequest(Request.Type.POST, Resource.Type.Repository, properties, null);
    Result result = AuditEventCreatorTestHelper.createResult(new ResultStatus(ResultStatus.STATUS.OK));

    AuditEvent event = AuditEventCreatorTestHelper.getEvent(creator, request, result);

    String actual = event.getAuditMessage();
    String expected = "User(" + userName + "), RemoteIp(1.2.3.4), Operation(Repository addition), RequestType(POST), url(http://example.com:8080/api/v1/test), ResultStatus(200 OK), Stack(StackName), Stack version(1.2-56), OS(redhat7), Repo id(Repo1), Base URL(http://example.com)";

    Assert.assertTrue("Class mismatch", event instanceof AddRepositoryRequestAuditEvent);
    Assert.assertEquals(expected, actual);
    Assert.assertTrue(actual.contains(userName));
  }

  @Test
  public void putTest() {
    RepositoryEventCreator creator = new RepositoryEventCreator();

    Map<String,Object> properties = new HashMap<>();
    properties.put(RepositoryResourceProvider.REPOSITORY_REPO_ID_PROPERTY_ID, "Repo1");
    properties.put(RepositoryResourceProvider.REPOSITORY_STACK_NAME_PROPERTY_ID, "StackName");
    properties.put(RepositoryResourceProvider.REPOSITORY_STACK_VERSION_PROPERTY_ID, "1.2-56");
    properties.put(RepositoryResourceProvider.REPOSITORY_OS_TYPE_PROPERTY_ID, "redhat7");
    properties.put(RepositoryResourceProvider.REPOSITORY_BASE_URL_PROPERTY_ID, "http://example.com");

    Request request = AuditEventCreatorTestHelper.createRequest(Request.Type.PUT, Resource.Type.Repository, properties, null);
    Result result = AuditEventCreatorTestHelper.createResult(new ResultStatus(ResultStatus.STATUS.OK));

    AuditEvent event = AuditEventCreatorTestHelper.getEvent(creator, request, result);

    String actual = event.getAuditMessage();
    String expected = "User(" + userName + "), RemoteIp(1.2.3.4), Operation(Repository update), RequestType(PUT), url(http://example.com:8080/api/v1/test), ResultStatus(200 OK), Stack(StackName), Stack version(1.2-56), OS(redhat7), Repo id(Repo1), Base URL(http://example.com)";

    Assert.assertTrue("Class mismatch", event instanceof UpdateRepositoryRequestAuditEvent);
    Assert.assertEquals(expected, actual);
    Assert.assertTrue(actual.contains(userName));
  }


}
