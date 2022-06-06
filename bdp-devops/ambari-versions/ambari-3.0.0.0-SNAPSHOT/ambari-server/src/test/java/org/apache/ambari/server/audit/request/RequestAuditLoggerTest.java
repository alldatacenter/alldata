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

import java.util.HashMap;

import org.apache.ambari.server.api.query.QueryImpl;
import org.apache.ambari.server.api.resources.BlueprintResourceDefinition;
import org.apache.ambari.server.api.resources.HostComponentResourceDefinition;
import org.apache.ambari.server.api.resources.ResourceDefinition;
import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.api.services.LocalUriInfo;
import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.services.RequestBody;
import org.apache.ambari.server.api.services.RequestFactory;
import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.api.services.ResultImpl;
import org.apache.ambari.server.api.services.ResultStatus;
import org.apache.ambari.server.audit.AuditLogger;
import org.apache.ambari.server.audit.event.AuditEvent;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;

import junit.framework.Assert;

public class RequestAuditLoggerTest {

  private static final String TEST_URI = "http://apache.org";
  private static RequestAuditLogger requestAuditLogger;
  private static AuditLogger mockAuditLogger;
  private RequestFactory requestFactory = new RequestFactory();

  @BeforeClass
  public static void beforeClass() throws Exception {
    Injector injector = Guice.createInjector(new RequestAuditLogModule());
    requestAuditLogger = injector.getInstance(RequestAuditLogger.class);
    mockAuditLogger = injector.getInstance(AuditLogger.class);
  }

  @Before
  public void before() {
    EasyMock.reset(mockAuditLogger);
  }

  @After
  public void after() {
    EasyMock.verify(mockAuditLogger);
  }

  @Test
  public void defaultEventCreatorPostTest() {
    testCreator(AllPostAndPutCreator.class, Request.Type.POST, new BlueprintResourceDefinition(), ResultStatus.STATUS.OK, null);
  }

  @Test
  public void customEventCreatorPutTest() {
    testCreator(PutHostComponentCreator.class, Request.Type.PUT, new HostComponentResourceDefinition(), ResultStatus.STATUS.OK, null);
  }

  @Test
  public void noCreatorForRequestTypeTest() {
    Request request = createRequest(new HostComponentResourceDefinition(), Request.Type.GET);
    Result result = new ResultImpl(new ResultStatus(ResultStatus.STATUS.OK));

    try {
      createCapture();
      requestAuditLogger.log(request, result);
      EasyMock.verify(mockAuditLogger);
      Assert.fail("Exception is excepted to be thrown");
    } catch (AssertionError ae) {
      EasyMock.reset(mockAuditLogger);
      EasyMock.replay(mockAuditLogger);
    }
  }

  @Test
  public void noRequestTypeTest() {
    Request request = createRequest(new BlueprintResourceDefinition(), Request.Type.DELETE);
    Result result = new ResultImpl(new ResultStatus(ResultStatus.STATUS.OK));

    try {
      createCapture();
      requestAuditLogger.log(request, result);
      EasyMock.verify(mockAuditLogger);
      Assert.fail("Exception is excepted to be thrown");
    } catch (AssertionError ae) {
      EasyMock.reset(mockAuditLogger);
      EasyMock.replay(mockAuditLogger);
    }
  }



  @Test
  public void noGetCreatorForResourceTypeTest__defaultGetCreatorUsed() {
    testCreator(AllGetCreator.class, Request.Type.GET, new HostComponentResourceDefinition(), ResultStatus.STATUS.ACCEPTED, null);
  }

  private void testCreator(Class<? extends AbstractBaseCreator> expectedCreatorClass, Request.Type requestType, ResourceDefinition resourceDefinition, ResultStatus.STATUS resultStatus, String resultStatusMessage) {
    Request request = createRequest(resourceDefinition, requestType);
    Result result = new ResultImpl(new ResultStatus(resultStatus, resultStatusMessage));

    Capture<AuditEvent> capture = createCapture();
    requestAuditLogger.log(request, result);

    String expectedMessage = createExpectedMessage(expectedCreatorClass, requestType, resultStatus, resultStatusMessage);

    Assert.assertEquals(expectedMessage, capture.getValue().getAuditMessage());
  }

  private Capture<AuditEvent> createCapture() {
    EasyMock.expect(mockAuditLogger.isEnabled()).andReturn(true).anyTimes();
    Capture<AuditEvent> capture = EasyMock.newCapture();
    mockAuditLogger.log(EasyMock.capture(capture));
    EasyMock.expectLastCall();
    EasyMock.replay(mockAuditLogger);
    return capture;
  }

  private Request createRequest(ResourceDefinition resourceDefinition, Request.Type requestType) {
    ResourceInstance resource = new QueryImpl(new HashMap<>(), resourceDefinition, null);
    return requestFactory.createRequest(null, new RequestBody(), new LocalUriInfo(TEST_URI), requestType, resource);
  }

  private String createExpectedMessage(Class<? extends AbstractBaseCreator> expectedCreatorClass, Request.Type requestType, ResultStatus.STATUS resultStatus, String resultStatusMessage) {
    return expectedCreatorClass.getName() + " " + String.format("%s %s %s %s %s", requestType, TEST_URI, resultStatus.getStatus(), resultStatus, resultStatusMessage);
  }

}
