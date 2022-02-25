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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.api.services.ResultStatus;
import org.apache.ambari.server.audit.event.AuditEvent;
import org.apache.ambari.server.audit.event.request.AddRepositoryVersionRequestAuditEvent;
import org.apache.ambari.server.audit.event.request.ChangeRepositoryVersionRequestAuditEvent;
import org.apache.ambari.server.audit.event.request.DeleteRepositoryVersionRequestAuditEvent;
import org.apache.ambari.server.audit.request.eventcreator.RepositoryVersionEventCreator;
import org.apache.ambari.server.controller.internal.OperatingSystemResourceProvider;
import org.apache.ambari.server.controller.internal.RepositoryResourceProvider;
import org.apache.ambari.server.controller.internal.RepositoryVersionResourceProvider;
import org.apache.ambari.server.controller.spi.Resource;
import org.junit.Test;

import junit.framework.Assert;

public class RepositoryVersionEventCreatorTest extends AuditEventCreatorTestBase{

  @Test
  public void postTest() {
    RepositoryVersionEventCreator creator = new RepositoryVersionEventCreator();

    Map<String,Object> properties = new HashMap<>();
    properties.put(RepositoryVersionResourceProvider.REPOSITORY_VERSION_STACK_NAME_PROPERTY_ID, "StackName");
    properties.put(RepositoryVersionResourceProvider.REPOSITORY_VERSION_STACK_VERSION_PROPERTY_ID, "1.9");
    properties.put(RepositoryVersionResourceProvider.REPOSITORY_VERSION_DISPLAY_NAME_PROPERTY_ID, "MyStack");
    properties.put(RepositoryVersionResourceProvider.REPOSITORY_VERSION_REPOSITORY_VERSION_PROPERTY_ID, "1.2-56");

    properties.put("operating_systems", createOperatingSystems());

    Request request = AuditEventCreatorTestHelper.createRequest(Request.Type.POST, Resource.Type.RepositoryVersion, properties, null);
    Result result = AuditEventCreatorTestHelper.createResult(new ResultStatus(ResultStatus.STATUS.OK));

    AuditEvent event = AuditEventCreatorTestHelper.getEvent(creator, request, result);

    String actual = event.getAuditMessage();
    String expected = "User(" + userName + "), RemoteIp(1.2.3.4), Operation(Repository version addition), RequestType(POST), url(http://example.com:8080/api/v1/test), ResultStatus(200 OK), Stack(StackName), Stack version(1.9), Display name(MyStack), Repo version(1.2-56), Repositories(\n" +
      "Operating system: redhat6\n" +
      "    Repository ID(2), Repository name(MyRepo6), Base url(http://example6.com)\n" +
      "Operating system: redhat7\n" +
      "    Repository ID(1), Repository name(MyRepo), Base url(http://example.com)\n" +
      ")";

    Assert.assertTrue("Class mismatch", event instanceof AddRepositoryVersionRequestAuditEvent);
    Assert.assertEquals(expected, actual);
    Assert.assertTrue(actual.contains(userName));
  }

  @Test
  public void putTest() {
    RepositoryVersionEventCreator creator = new RepositoryVersionEventCreator();

    Map<String,Object> properties = new HashMap<>();
    properties.put(RepositoryVersionResourceProvider.REPOSITORY_VERSION_STACK_NAME_PROPERTY_ID, "StackName");
    properties.put(RepositoryVersionResourceProvider.REPOSITORY_VERSION_STACK_VERSION_PROPERTY_ID, "1.9");
    properties.put(RepositoryVersionResourceProvider.REPOSITORY_VERSION_DISPLAY_NAME_PROPERTY_ID, "MyStack");
    properties.put(RepositoryVersionResourceProvider.REPOSITORY_VERSION_REPOSITORY_VERSION_PROPERTY_ID, "1.2-56");

    properties.put("operating_systems", createOperatingSystems());

    Request request = AuditEventCreatorTestHelper.createRequest(Request.Type.PUT, Resource.Type.RepositoryVersion, properties, null);
    Result result = AuditEventCreatorTestHelper.createResult(new ResultStatus(ResultStatus.STATUS.OK));

    AuditEvent event = AuditEventCreatorTestHelper.getEvent(creator, request, result);

    String actual = event.getAuditMessage();
    String expected = "User(" + userName + "), RemoteIp(1.2.3.4), Operation(Repository version change), RequestType(PUT), url(http://example.com:8080/api/v1/test), ResultStatus(200 OK), Stack(StackName), Stack version(1.9), Display name(MyStack), Repo version(1.2-56), Repositories(\n" +
      "Operating system: redhat6\n" +
      "    Repository ID(2), Repository name(MyRepo6), Base url(http://example6.com)\n" +
      "Operating system: redhat7\n" +
      "    Repository ID(1), Repository name(MyRepo), Base url(http://example.com)\n" +
      ")";

    Assert.assertTrue("Class mismatch", event instanceof ChangeRepositoryVersionRequestAuditEvent);
    Assert.assertEquals(expected, actual);
    Assert.assertTrue(actual.contains(userName));
  }

  @Test
  public void deleteTest() {
    RepositoryVersionEventCreator creator = new RepositoryVersionEventCreator();

    Map<Resource.Type,String> resource = new HashMap<>();
    resource.put(Resource.Type.Stack, "HDP");
    resource.put(Resource.Type.StackVersion, "1.9");
    resource.put(Resource.Type.RepositoryVersion, "1.2-56");

    Request request = AuditEventCreatorTestHelper.createRequest(Request.Type.DELETE, Resource.Type.RepositoryVersion, null, resource);
    Result result = AuditEventCreatorTestHelper.createResult(new ResultStatus(ResultStatus.STATUS.OK));

    AuditEvent event = AuditEventCreatorTestHelper.getEvent(creator, request, result);

    String actual = event.getAuditMessage();
    String expected = "User(" + userName + "), RemoteIp(1.2.3.4), Operation(Repository version removal), RequestType(DELETE), url(http://example.com:8080/api/v1/test), ResultStatus(200 OK), Stack(HDP), Stack version(1.9), Repo version ID(1.2-56)";

    Assert.assertTrue("Class mismatch", event instanceof DeleteRepositoryVersionRequestAuditEvent);
    Assert.assertEquals(expected, actual);
    Assert.assertTrue(actual.contains(userName));
  }

  private Set<Map<String, Object>> createOperatingSystems() {
    Set<Map<String, Object>> operatingSystems = new HashSet<>();

    // ***
    Map<String, Object> operatingSystem = new HashMap<>();
    operatingSystem.put(OperatingSystemResourceProvider.OPERATING_SYSTEM_OS_TYPE_PROPERTY_ID, "redhat7");

    Set<Map<String,String>> repositories = new HashSet<>();

    Map<String, String> repository = new HashMap<>();
    repository.put(RepositoryResourceProvider.REPOSITORY_REPO_ID_PROPERTY_ID, "1");
    repository.put(RepositoryResourceProvider.REPOSITORY_REPO_NAME_PROPERTY_ID, "MyRepo");
    repository.put(RepositoryResourceProvider.REPOSITORY_BASE_URL_PROPERTY_ID, "http://example.com");
    repositories.add(repository);

    operatingSystem.put("repositories", repositories);
    // ***
    Map<String, Object> operatingSystem2 = new HashMap<>();
    operatingSystem2.put(OperatingSystemResourceProvider.OPERATING_SYSTEM_OS_TYPE_PROPERTY_ID, "redhat6");

    Set<Map<String,String>> repositories2 = new HashSet<>();

    Map<String, String> repository2 = new HashMap<>();
    repository2.put(RepositoryResourceProvider.REPOSITORY_REPO_ID_PROPERTY_ID, "2");
    repository2.put(RepositoryResourceProvider.REPOSITORY_REPO_NAME_PROPERTY_ID, "MyRepo6");
    repository2.put(RepositoryResourceProvider.REPOSITORY_BASE_URL_PROPERTY_ID, "http://example6.com");
    repositories2.add(repository2);

    operatingSystem2.put("repositories", repositories2);
    // ***

    operatingSystems.add(operatingSystem);
    operatingSystems.add(operatingSystem2);
    return operatingSystems;
  }


}
