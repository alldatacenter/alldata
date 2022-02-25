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
package org.apache.ambari.server.utils;

import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.easymock.EasyMockSupport;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.util.MultiValueMap;

public class RequestUtilsTest extends EasyMockSupport {

  private static final String REMOTE_ADDRESS = "12.13.14.15";
  private static final String REMOTE_ADDRESS_MULTIPLE = "12.13.14.15,12.13.14.16";

  @Test
  public void testGetRemoteAddress() {
    // GIVEN
    HttpServletRequest mockedRequest = createMock(HttpServletRequest.class);
    expect(mockedRequest.getHeader("X-Forwarded-For")).andReturn(null);
    expect(mockedRequest.getHeader("Proxy-Client-IP")).andReturn("unknown");
    expect(mockedRequest.getHeader("WL-Proxy-Client-IP")).andReturn("");
    expect(mockedRequest.getHeader("HTTP_CLIENT_IP")).andReturn("unknown");
    expect(mockedRequest.getHeader("HTTP_X_FORWARDED_FOR")).andReturn(REMOTE_ADDRESS);
    replayAll();
    // WHEN
    String remoteAddress = RequestUtils.getRemoteAddress(mockedRequest);
    // THEN
    assertEquals(REMOTE_ADDRESS, remoteAddress);
    verifyAll();
  }

  @Test
  public void testGetMultipleRemoteAddress() {
    // GIVEN
    HttpServletRequest mockedRequest = createMock(HttpServletRequest.class);
    expect(mockedRequest.getHeader("X-Forwarded-For")).andReturn(null);
    expect(mockedRequest.getHeader("Proxy-Client-IP")).andReturn("unknown");
    expect(mockedRequest.getHeader("WL-Proxy-Client-IP")).andReturn("");
    expect(mockedRequest.getHeader("HTTP_CLIENT_IP")).andReturn("unknown");
    expect(mockedRequest.getHeader("HTTP_X_FORWARDED_FOR")).andReturn(REMOTE_ADDRESS_MULTIPLE);
    replayAll();
    // WHEN
    String remoteAddress = RequestUtils.getRemoteAddress(mockedRequest);
    // THEN
    assertEquals(REMOTE_ADDRESS, remoteAddress);
    verifyAll();
  }

  @Test
  public void testGetRemoteAddressFoundFirstHeader() {
    // GIVEN
    HttpServletRequest mockedRequest = createMock(HttpServletRequest.class);
    expect(mockedRequest.getHeader("X-Forwarded-For")).andReturn(REMOTE_ADDRESS);
    replayAll();
    // WHEN
    String remoteAddress = RequestUtils.getRemoteAddress(mockedRequest);
    // THEN
    assertEquals(REMOTE_ADDRESS, remoteAddress);
    verifyAll();
  }

  @Test
  public void testGetRemoteAddressWhenHeadersAreMissing() {
    // GIVEN
    HttpServletRequest mockedRequest = createMock(HttpServletRequest.class);
    expect(mockedRequest.getHeader(anyString())).andReturn(null).times(5);
    expect(mockedRequest.getRemoteAddr()).andReturn(REMOTE_ADDRESS);
    replayAll();
    // WHEN
    String remoteAddress = RequestUtils.getRemoteAddress(mockedRequest);
    // THEN
    assertEquals(REMOTE_ADDRESS, remoteAddress);
    verifyAll();
  }


  @Test
  public void testGetQueryStringParameters() {

    HttpServletRequest request = createMock(HttpServletRequest.class);
    // Null query string
    expect(request.getQueryString()).andReturn(null).once();
    // Empty query string
    expect(request.getQueryString()).andReturn("").once();
    // Single parameter query string
    expect(request.getQueryString()).andReturn("p1=1").once();
    // Multiple parameter query string
    expect(request.getQueryString()).andReturn("p1=1&p2=2").once();
    // Multiple parameter query string, one with multiple values
    expect(request.getQueryString()).andReturn("p1=1a&p2=2&p1=1b").once();

    replayAll();
    // Null query string
    Assert.assertNull(RequestUtils.getQueryStringParameters(request));

    // Empty query string
    Assert.assertNull(RequestUtils.getQueryStringParameters(request));

    MultiValueMap<String, String> parameters;

    // Single parameter query string
    parameters = RequestUtils.getQueryStringParameters(request);
    Assert.assertNotNull(parameters);
    Assert.assertEquals(1, parameters.size());
    Assert.assertNotNull(parameters.get("p1"));
    Assert.assertEquals(1, parameters.get("p1").size());
    Assert.assertEquals("1", parameters.get("p1").get(0));

    // Multiple  parameter query string
    parameters = RequestUtils.getQueryStringParameters(request);
    Assert.assertNotNull(parameters);
    Assert.assertEquals(2, parameters.size());
    Assert.assertNotNull(parameters.get("p1"));
    Assert.assertEquals(1, parameters.get("p1").size());
    Assert.assertEquals("1", parameters.get("p1").get(0));
    Assert.assertNotNull(parameters.get("p2"));
    Assert.assertEquals(1, parameters.get("p2").size());
    Assert.assertEquals("2", parameters.get("p2").get(0));

    // Multiple parameter query string, one with multiple values
    parameters = RequestUtils.getQueryStringParameters(request);
    Assert.assertNotNull(parameters);
    Assert.assertEquals(2, parameters.size());
    Assert.assertNotNull(parameters.get("p1"));
    Assert.assertEquals(2, parameters.get("p1").size());
    Assert.assertEquals("1a", parameters.get("p1").get(0));
    Assert.assertEquals("1b", parameters.get("p1").get(1));
    Assert.assertNotNull(parameters.get("p2"));
    Assert.assertEquals(1, parameters.get("p2").size());
    Assert.assertEquals("2", parameters.get("p2").get(0));

    verifyAll();
  }

  @Test
  public void testGetQueryStringParameterValues() {
    HttpServletRequest request = createMock(HttpServletRequest.class);
    // Null query string
    expect(request.getQueryString()).andReturn(null).once();
    // Empty query string
    expect(request.getQueryString()).andReturn("").once();
    // Single parameter query string
    expect(request.getQueryString()).andReturn("p1=1").once();
    // Multiple parameter query string
    expect(request.getQueryString()).andReturn("p1=1&p2=2").once();
    // Multiple parameter query string, one with multiple values
    expect(request.getQueryString()).andReturn("p1=1a&p2=2&p1=1b").once();

    replayAll();
    // Null query string
    Assert.assertNull(RequestUtils.getQueryStringParameterValues(request, "p1"));

    // Empty query string
    Assert.assertNull(RequestUtils.getQueryStringParameterValues(request, "p1"));

    List<String> parameterValues;

    // Single parameter query string
    parameterValues = RequestUtils.getQueryStringParameterValues(request, "p1");
    Assert.assertNotNull(parameterValues);
    Assert.assertEquals(1, parameterValues.size());
    Assert.assertEquals("1", parameterValues.get(0));

    // Multiple parameter query string
    parameterValues = RequestUtils.getQueryStringParameterValues(request, "p2");
    Assert.assertNotNull(parameterValues);
    Assert.assertEquals(1, parameterValues.size());
    Assert.assertEquals("2", parameterValues.get(0));

    // Multiple parameter query string, one with multiple values
    parameterValues = RequestUtils.getQueryStringParameterValues(request, "p1");
    Assert.assertNotNull(parameterValues);
    Assert.assertEquals(2, parameterValues.size());
    Assert.assertEquals("1a", parameterValues.get(0));
    Assert.assertEquals("1b", parameterValues.get(1));

    verifyAll();
  }

  @Test
  public void testGetQueryStringParameterValue() {
    HttpServletRequest request = createMock(HttpServletRequest.class);
    // Null query string
    expect(request.getQueryString()).andReturn(null).once();
    // Empty query string
    expect(request.getQueryString()).andReturn("").once();
    // Single parameter query string
    expect(request.getQueryString()).andReturn("p1=1").once();
    // Multiple parameter query string
    expect(request.getQueryString()).andReturn("p1=1&p2=2").once();
    // Multiple parameter query string, one with multiple values
    expect(request.getQueryString()).andReturn("p1=1a&p2=2&p1=1b").once();

    replayAll();
    // Null query string
    Assert.assertNull(RequestUtils.getQueryStringParameterValue(request, "p1"));

    // Empty query string
    Assert.assertNull(RequestUtils.getQueryStringParameterValue(request, "p1"));

    String parameterValue;

    // Single parameter query string
    parameterValue = RequestUtils.getQueryStringParameterValue(request, "p1");
    Assert.assertEquals("1", parameterValue);

    // Multiple parameter query string
    parameterValue = RequestUtils.getQueryStringParameterValue(request, "p2");
    Assert.assertEquals("2", parameterValue);

    // Multiple parameter query string, one with multiple values
    parameterValue = RequestUtils.getQueryStringParameterValue(request, "p1");
    Assert.assertEquals("1a", parameterValue);

    verifyAll();
  }
}
