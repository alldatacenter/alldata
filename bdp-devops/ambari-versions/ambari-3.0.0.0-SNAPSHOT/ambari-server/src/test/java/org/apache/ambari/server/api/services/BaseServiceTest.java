/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.api.services;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.api.services.parsers.BodyParseException;
import org.apache.ambari.server.api.services.parsers.RequestBodyParser;
import org.apache.ambari.server.api.services.serializers.ResultSerializer;
import org.apache.ambari.server.audit.request.RequestAuditLogger;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.EasyMockRunner;
import org.easymock.Mock;
import org.easymock.MockType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Base class for service unit tests.
 */
@RunWith(EasyMockRunner.class)
public abstract class BaseServiceTest {

  protected ResourceInstance resourceInstance = createNiceMock(ResourceInstance.class);
  protected RequestFactory requestFactory = createStrictMock(RequestFactory.class);
  protected Request request = createNiceMock(Request.class);
  protected HttpHeaders httpHeaders = createNiceMock(HttpHeaders.class);
  protected UriInfo uriInfo = createNiceMock(UriInfo.class);
  protected Result result = createMock(Result.class);
  protected RequestBody requestBody = createNiceMock(RequestBody.class);
  protected RequestBodyParser bodyParser = createStrictMock(RequestBodyParser.class);
  protected ResultStatus status = createNiceMock(ResultStatus.class);
  protected ResultSerializer serializer = createStrictMock(ResultSerializer.class);
  protected Object serializedResult = new Object();

  public ResourceInstance getTestResource() {
    return resourceInstance;
  }

  public RequestFactory getTestRequestFactory() {
    return requestFactory;
  }

  public Request getRequest() {
    return request;
  }


  public HttpHeaders getHttpHeaders() {
    return httpHeaders;
  }

  public UriInfo getUriInfo() {
    return uriInfo;
  }

  public RequestBodyParser getTestBodyParser() {
    return bodyParser;
  }

  public ResultSerializer getTestResultSerializer() {
    return serializer;
  }

  @Mock(type = MockType.NICE)
  public RequestAuditLogger requestAuditLogger;

  @Before
  public void before() throws Exception {
    BaseService.init(requestAuditLogger);
  }

  @Test
  public void testService() throws Exception {
    List<ServiceTestInvocation> listTestInvocations = getTestInvocations();
    for (ServiceTestInvocation testInvocation : listTestInvocations) {
      testMethod(testInvocation);
      testMethod_bodyParseException(testInvocation);
      testMethod_resultInErrorState(testInvocation);
    }
  }

  private void testMethod(ServiceTestInvocation testMethod) throws InvocationTargetException, IllegalAccessException {
    try {
      expect(bodyParser.parse(testMethod.getBody())).andReturn(Collections.singleton(requestBody));
    } catch (BodyParseException e) {
      // needed for compiler
    }

    assertCreateRequest(testMethod);
    expect(request.process()).andReturn(result);
    expect(result.getStatus()).andReturn(status).atLeastOnce();
    expect(status.getStatusCode()).andReturn(testMethod.getStatusCode()).atLeastOnce();
    expect(serializer.serialize(result)).andReturn(serializedResult);

    replayMocks();

    Response r = testMethod.invoke();

    assertEquals(serializedResult, r.getEntity());
    assertEquals(testMethod.getStatusCode(), r.getStatus());
    verifyAndResetMocks();
  }

  protected void assertCreateRequest(ServiceTestInvocation testMethod) {
    addExpectForInitialRequest(testMethod);
    expect(requestFactory.createRequest(httpHeaders, requestBody, uriInfo,
        testMethod.getRequestType(), resourceInstance)).andReturn(request);
  }



  private void testMethod_bodyParseException(ServiceTestInvocation testMethod) throws Exception {
    addExpectForInitialRequest(testMethod);

    Capture<Result> resultCapture = EasyMock.newCapture();
    BodyParseException e = new BodyParseException("TEST MSG");
    expect(bodyParser.parse(testMethod.getBody())).andThrow(e);
    expect(serializer.serialize(capture(resultCapture))).andReturn(serializedResult);

    replayMocks();

    Response r = testMethod.invoke();

    assertEquals(serializedResult, r.getEntity());
    assertEquals(400, r.getStatus());
    //todo: assert resource state
    verifyAndResetMocks();
  }

  private void testMethod_resultInErrorState(ServiceTestInvocation testMethod) throws Exception {
    try {
      expect(bodyParser.parse(testMethod.getBody())).andReturn(Collections.singleton(requestBody));
    } catch (BodyParseException e) {
      // needed for compiler
    }
    assertCreateRequest(testMethod);
    expect(request.process()).andReturn(result);
    expect(result.getStatus()).andReturn(status).atLeastOnce();
    expect(status.getStatusCode()).andReturn(400).atLeastOnce();
    expect(serializer.serialize(result)).andReturn(serializedResult);

    replayMocks();

    Response r = testMethod.invoke();

    assertEquals(serializedResult, r.getEntity());
    assertEquals(400, r.getStatus());
    verifyAndResetMocks();
  }

  private void replayMocks() {
    replay(resourceInstance, requestFactory, request, result, requestBody, bodyParser, status, serializer);
  }

  private void verifyAndResetMocks() {
    verify(resourceInstance, requestFactory, request, result, requestBody, bodyParser, status, serializer);
    reset(resourceInstance, requestFactory, request, result, requestBody, bodyParser, status, serializer);
  }

  private void addExpectForInitialRequest(ServiceTestInvocation testMethod) {
    RequestBody rb = new RequestBody();
    rb.setBody(testMethod.getBody());
    expect(requestFactory.createRequest(EasyMock.eq(httpHeaders), EasyMock.anyObject(RequestBody.class), EasyMock.eq(uriInfo),
      EasyMock.eq(testMethod.getRequestType()), EasyMock.eq(resourceInstance))).andReturn(request);
  }

  public static class ServiceTestInvocation {
    private Request.Type m_type;
    private BaseService m_instance;
    private Method m_method;
    private Object[] m_args;
    private String m_body;

    private static final Map<Request.Type, Integer> mapStatusCodes = new HashMap<>();

    static {
      mapStatusCodes.put(Request.Type.GET, 200);
      mapStatusCodes.put(Request.Type.POST, 201);
      mapStatusCodes.put(Request.Type.PUT, 200);
      mapStatusCodes.put(Request.Type.DELETE, 200);
      mapStatusCodes.put(Request.Type.QUERY_POST, 201);
    }

    public ServiceTestInvocation(Request.Type requestType, BaseService instance, Method method, Object[] args, String body) {
      m_type = requestType;
      m_instance = instance;
      m_method = method;
      m_args = args;
      m_body = body;
    }

    public int getStatusCode() {
      return mapStatusCodes.get(m_type);
    }

    public Request.Type getRequestType() {
      return m_type;
    }

    public String getBody() {
      return m_body;
    }

    public Response invoke() throws InvocationTargetException, IllegalAccessException {
      return (Response) m_method.invoke(m_instance, m_args);
    }
  }

  public abstract List<ServiceTestInvocation> getTestInvocations() throws Exception;
}
