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

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import org.apache.ambari.server.api.resources.ResourceDefinition;
import org.apache.ambari.server.api.resources.ResourceInstance;
import org.junit.Test;

/**
 * RequestFactory unit tests.
 */
public class RequestFactoryTest {

  @Test
  public void testCreate_Post__NoQueryParams() {
    HttpHeaders headers = createNiceMock(HttpHeaders.class);
    UriInfo uriInfo = createNiceMock(UriInfo.class);
    RequestBody body = createStrictMock(RequestBody.class);
    ResourceInstance resource = createNiceMock(ResourceInstance.class);
    @SuppressWarnings("unchecked")
    MultivaluedMap<String, String> mapQueryParams = createMock(MultivaluedMap.class);

    //expectations
    expect(uriInfo.getQueryParameters()).andReturn(mapQueryParams).anyTimes();
    expect(mapQueryParams.entrySet()).andReturn(Collections.emptySet()).anyTimes();
    expect(body.getQueryString()).andReturn(null);

    replay(headers, uriInfo, body, resource, mapQueryParams);

    //test
    RequestFactory factory = new RequestFactory();
    Request request = factory.createRequest(headers, body, uriInfo, Request.Type.POST, resource);

    assertEquals(resource, request.getResource());
    assertEquals(body, request.getBody());
    assertEquals(Request.Type.POST, request.getRequestType());

    verify(headers, uriInfo, body, resource, mapQueryParams);
  }

  @Test
  // query post : uri contains query string
  public void testCreate_Post__UriQueryParams() {
    HttpHeaders headers = createNiceMock(HttpHeaders.class);
    UriInfo uriInfo = createNiceMock(UriInfo.class);
    RequestBody body = createNiceMock(RequestBody.class);
    ResourceInstance resource = createNiceMock(ResourceInstance.class);
    ResourceDefinition resourceDefinition = createNiceMock(ResourceDefinition.class);
    @SuppressWarnings("unchecked")
    MultivaluedMap<String, String> mapQueryParams = createMock(MultivaluedMap.class);
    Map<String, List<String>> mapProps = new HashMap<>();
    mapProps.put("foo", Collections.singletonList("bar"));


    //expectations
    expect(uriInfo.getQueryParameters()).andReturn(mapQueryParams).anyTimes();
    expect(mapQueryParams.entrySet()).andReturn(mapProps.entrySet()).anyTimes();
    expect(resource.getResourceDefinition()).andReturn(resourceDefinition).anyTimes();
    expect(resourceDefinition.getCreateDirectives()).andReturn(Collections.emptySet());
    expect(body.getQueryString()).andReturn(null);

    replay(headers, uriInfo, body, resource, mapQueryParams, resourceDefinition);

    //test
    RequestFactory factory = new RequestFactory();
    Request request = factory.createRequest(headers, body, uriInfo, Request.Type.POST, resource);

    assertEquals(resource, request.getResource());
    assertEquals(body, request.getBody());
    assertEquals(Request.Type.QUERY_POST, request.getRequestType());

    verify(headers, uriInfo, body, resource, mapQueryParams, resourceDefinition);
  }

  @Test
  // post with create directive in URI
  public void testCreate_Post__WithUriDirective() {
    HttpHeaders headers = createNiceMock(HttpHeaders.class);
    UriInfo uriInfo = createNiceMock(UriInfo.class);
    RequestBody body = createNiceMock(RequestBody.class);
    ResourceInstance resource = createNiceMock(ResourceInstance.class);
    ResourceDefinition resourceDefinition = createNiceMock(ResourceDefinition.class);

    @SuppressWarnings("unchecked")
    MultivaluedMap<String, String> mapQueryParams = createMock(MultivaluedMap.class);
    Map<String, List<String>> mapProps = new HashMap<>();
    mapProps.put("foo", Collections.singletonList("bar"));

    Map<String, String> requestInfoMap = new HashMap<>();

    //expectations
    expect(uriInfo.getQueryParameters()).andReturn(mapQueryParams).anyTimes();
    expect(mapQueryParams.entrySet()).andReturn(mapProps.entrySet()).anyTimes();
    expect(resource.getResourceDefinition()).andReturn(resourceDefinition).anyTimes();
    expect(resourceDefinition.getCreateDirectives()).andReturn(Collections.singleton("foo"));
    expect(body.getQueryString()).andReturn(null);
    expect(body.getRequestInfoProperties()).andReturn(requestInfoMap).anyTimes();

    replay(headers, uriInfo, body, resource, mapQueryParams, resourceDefinition);

    //test
    RequestFactory factory = new RequestFactory();
    Request request = factory.createRequest(headers, body, uriInfo, Request.Type.POST, resource);

    assertEquals(resource, request.getResource());
    assertEquals(body, request.getBody());
    assertEquals(Request.Type.POST, request.getRequestType());
    assertEquals("bar", requestInfoMap.get("foo"));

    verify(headers, uriInfo, body, resource, mapQueryParams, resourceDefinition);
  }

  @Test
  // put with update directive in URI
  public void testCreate_Put__WithUriDirective() {
    HttpHeaders headers = createNiceMock(HttpHeaders.class);
    UriInfo uriInfo = createNiceMock(UriInfo.class);
    RequestBody body = createNiceMock(RequestBody.class);
    ResourceInstance resource = createNiceMock(ResourceInstance.class);
    ResourceDefinition resourceDefinition = createNiceMock(ResourceDefinition.class);

    @SuppressWarnings("unchecked")
    MultivaluedMap<String, String> mapQueryParams = createMock(MultivaluedMap.class);
    Map<String, List<String>> mapProps = new HashMap<>();
    mapProps.put("foo", Collections.singletonList("bar"));

    Map<String, String> requestInfoMap = new HashMap<>();

    //expectations
    expect(uriInfo.getQueryParameters()).andReturn(mapQueryParams).anyTimes();
    expect(mapQueryParams.entrySet()).andReturn(mapProps.entrySet()).anyTimes();
    expect(resource.getResourceDefinition()).andReturn(resourceDefinition).anyTimes();
    expect(resourceDefinition.getUpdateDirectives()).andReturn(Collections.singleton("foo"));
    expect(body.getQueryString()).andReturn(null);
    expect(body.getRequestInfoProperties()).andReturn(requestInfoMap).anyTimes();

    replay(headers, uriInfo, body, resource, mapQueryParams, resourceDefinition);

    //test
    RequestFactory factory = new RequestFactory();
    Request request = factory.createRequest(headers, body, uriInfo, Request.Type.PUT, resource);

    assertEquals(resource, request.getResource());
    assertEquals(body, request.getBody());
    assertEquals(Request.Type.PUT, request.getRequestType());
    assertEquals("bar", requestInfoMap.get("foo"));

    verify(headers, uriInfo, body, resource, mapQueryParams, resourceDefinition);
  }

  @Test
  // delete with delete directive in URI
  public void testCreate_Delete__WithUriDirective() {
    HttpHeaders headers = createNiceMock(HttpHeaders.class);
    UriInfo uriInfo = createNiceMock(UriInfo.class);
    RequestBody body = createNiceMock(RequestBody.class);
    ResourceInstance resource = createNiceMock(ResourceInstance.class);
    ResourceDefinition resourceDefinition = createNiceMock(ResourceDefinition.class);

    @SuppressWarnings("unchecked")
    MultivaluedMap<String, String> mapQueryParams = createMock(MultivaluedMap.class);
    Map<String, List<String>> mapProps = new HashMap<>();
    mapProps.put("foo", Collections.singletonList("bar"));

    Map<String, String> requestInfoMap = new HashMap<>();

    //expectations
    expect(uriInfo.getQueryParameters()).andReturn(mapQueryParams).anyTimes();
    expect(mapQueryParams.entrySet()).andReturn(mapProps.entrySet()).anyTimes();
    expect(resource.getResourceDefinition()).andReturn(resourceDefinition).anyTimes();
    expect(resourceDefinition.getDeleteDirectives()).andReturn(Collections.singleton("foo"));
    expect(body.getQueryString()).andReturn(null);
    expect(body.getRequestInfoProperties()).andReturn(requestInfoMap).anyTimes();

    replay(headers, uriInfo, body, resource, mapQueryParams, resourceDefinition);

    //test
    RequestFactory factory = new RequestFactory();
    Request request = factory.createRequest(headers, body, uriInfo, Request.Type.DELETE, resource);

    assertEquals(resource, request.getResource());
    assertEquals(body, request.getBody());
    assertEquals(Request.Type.DELETE, request.getRequestType());
    assertEquals("bar", requestInfoMap.get("foo"));

    verify(headers, uriInfo, body, resource, mapQueryParams, resourceDefinition);
  }

  @Test
  // delete w/o delete directive in URI
  public void testCreate_Delete__WithoutUriDirective() {
    HttpHeaders headers = createNiceMock(HttpHeaders.class);
    UriInfo uriInfo = createNiceMock(UriInfo.class);
    RequestBody body = createNiceMock(RequestBody.class);
    ResourceInstance resource = createNiceMock(ResourceInstance.class);
    ResourceDefinition resourceDefinition = createNiceMock(ResourceDefinition.class);

    @SuppressWarnings("unchecked")
    MultivaluedMap<String, String> mapQueryParams = createMock(MultivaluedMap.class);
    Map<String, List<String>> mapProps = new HashMap<>();
    mapProps.put("foo", Collections.singletonList("bar"));

    Map<String, String> requestInfoMap = new HashMap<>();

    //expectations
    expect(uriInfo.getQueryParameters()).andReturn(mapQueryParams).anyTimes();
    expect(mapQueryParams.entrySet()).andReturn(mapProps.entrySet()).anyTimes();
    expect(resource.getResourceDefinition()).andReturn(resourceDefinition).anyTimes();
    expect(resourceDefinition.getDeleteDirectives()).andReturn(Collections.emptySet());
    expect(body.getQueryString()).andReturn(null);
    expect(body.getRequestInfoProperties()).andReturn(requestInfoMap).anyTimes();

    replay(headers, uriInfo, body, resource, mapQueryParams, resourceDefinition);

    //test
    RequestFactory factory = new RequestFactory();
    Request request = factory.createRequest(headers, body, uriInfo, Request.Type.DELETE, resource);

    assertEquals(resource, request.getResource());
    assertEquals(body, request.getBody());
    assertEquals(Request.Type.DELETE, request.getRequestType());
    assertEquals(null, requestInfoMap.get("foo"));
    verify(headers, uriInfo, body, resource, mapQueryParams, resourceDefinition);
  }

  @Test
  // query post : body contains query string
  public void testCreate_Post__BodyQueryParams() {
    HttpHeaders headers = createNiceMock(HttpHeaders.class);
    UriInfo uriInfo = createNiceMock(UriInfo.class);
    RequestBody body = createNiceMock(RequestBody.class);
    ResourceInstance resource = createNiceMock(ResourceInstance.class);
    ResourceDefinition resourceDefinition = createNiceMock(ResourceDefinition.class);
    @SuppressWarnings("unchecked")
    MultivaluedMap<String, String> mapQueryParams = createMock(MultivaluedMap.class);

    //expectations
    expect(uriInfo.getQueryParameters()).andReturn(mapQueryParams).anyTimes();
    expect(mapQueryParams.entrySet()).andReturn(Collections.emptySet()).anyTimes();
    expect(resource.getResourceDefinition()).andReturn(resourceDefinition).anyTimes();
    expect(resourceDefinition.getCreateDirectives()).andReturn(Collections.emptySet());
    expect(body.getQueryString()).andReturn("foo=bar");

    replay(headers, uriInfo, body, resource, mapQueryParams, resourceDefinition);

    //test
    RequestFactory factory = new RequestFactory();
    Request request = factory.createRequest(headers, body, uriInfo, Request.Type.POST, resource);

    assertEquals(resource, request.getResource());
    assertEquals(body, request.getBody());
    assertEquals(Request.Type.QUERY_POST, request.getRequestType());

    verify(headers, uriInfo, body, resource, mapQueryParams, resourceDefinition);
  }

  @Test
  // post with create directive in body
  public void testCreate_Post__WithBodyDirective() {
    HttpHeaders headers = createNiceMock(HttpHeaders.class);
    UriInfo uriInfo = createNiceMock(UriInfo.class);
    RequestBody body = createNiceMock(RequestBody.class);
    ResourceInstance resource = createNiceMock(ResourceInstance.class);
    ResourceDefinition resourceDefinition = createNiceMock(ResourceDefinition.class);

    @SuppressWarnings("unchecked")
    MultivaluedMap<String, String> mapQueryParams = createMock(MultivaluedMap.class);
    Map<String, List<String>> mapProps = new HashMap<>();
    mapProps.put("foo", Collections.singletonList("bar"));

    Map<String, String> requestInfoMap = new HashMap<>();

    //expectations
    expect(uriInfo.getQueryParameters()).andReturn(mapQueryParams).anyTimes();
    expect(mapQueryParams.entrySet()).andReturn(Collections.emptySet()).anyTimes();
    expect(resource.getResourceDefinition()).andReturn(resourceDefinition).anyTimes();
    expect(resourceDefinition.getCreateDirectives()).andReturn(Collections.singleton("foo"));
    expect(body.getQueryString()).andReturn("foo=bar");
    expect(body.getRequestInfoProperties()).andReturn(requestInfoMap).anyTimes();

    replay(headers, uriInfo, body, resource, mapQueryParams, resourceDefinition);

    //test
    RequestFactory factory = new RequestFactory();
    Request request = factory.createRequest(headers, body, uriInfo, Request.Type.POST, resource);

    assertEquals(resource, request.getResource());
    assertEquals(body, request.getBody());
    assertEquals(Request.Type.POST, request.getRequestType());
    assertEquals("bar", requestInfoMap.get("foo"));

    verify(headers, uriInfo, body, resource, mapQueryParams, resourceDefinition);
  }

  @Test
  // get with create directive in URI
  public void testCreate_Get__WithUriDirective() {
    HttpHeaders headers = createMock(HttpHeaders.class);
    UriInfo uriInfo = createMock(UriInfo.class);
    RequestBody body = createMock(RequestBody.class);
    ResourceInstance resource = createMock(ResourceInstance.class);
    ResourceDefinition resourceDefinition = createMock(ResourceDefinition.class);

    @SuppressWarnings("unchecked")
    MultivaluedMap<String, String> mapQueryParams = createMock(MultivaluedMap.class);
    Map<String, List<String>> mapProps = new HashMap<>();
    mapProps.put("foo", Collections.singletonList("bar"));

    Map<String, String> requestInfoMap = new HashMap<>();

    //expectations
    expect(uriInfo.getQueryParameters()).andReturn(mapQueryParams).anyTimes();
    expect(mapQueryParams.entrySet()).andReturn(mapProps.entrySet()).anyTimes();
    expect(resource.getResourceDefinition()).andReturn(resourceDefinition).anyTimes();
    expect(resourceDefinition.getReadDirectives()).andReturn(Collections.singleton("foo"));
    expect(body.getQueryString()).andReturn(null);
    expect(body.getRequestInfoProperties()).andReturn(requestInfoMap).anyTimes();

    replay(headers, uriInfo, body, resource, mapQueryParams, resourceDefinition);

    //test
    RequestFactory factory = new RequestFactory();
    Request request = factory.createRequest(headers, body, uriInfo, Request.Type.GET, resource);

    assertEquals(resource, request.getResource());
    assertEquals(body, request.getBody());
    assertEquals(Request.Type.GET, request.getRequestType());
    assertEquals("bar", requestInfoMap.get("foo"));

    verify(headers, uriInfo, body, resource, mapQueryParams, resourceDefinition);
  }
}
