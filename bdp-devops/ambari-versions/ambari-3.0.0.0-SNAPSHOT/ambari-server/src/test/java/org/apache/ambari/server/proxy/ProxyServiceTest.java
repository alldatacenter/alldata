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

package org.apache.ambari.server.proxy;

import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED_TYPE;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertSame;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.apache.ambari.server.api.services.BaseServiceTest;
import org.apache.ambari.server.controller.internal.URLStreamProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.gson.Gson;
import com.sun.jersey.core.spi.factory.ResponseBuilderImpl;
import com.sun.jersey.core.spi.factory.ResponseImpl;
import com.sun.jersey.core.util.MultivaluedMapImpl;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ ProxyServiceTest.class, ProxyService.class, URLStreamProvider.class, Response.class,
        ResponseBuilderImpl.class, URI.class })
public class ProxyServiceTest extends BaseServiceTest {

  @Test
  public void testProxyGetRequest() throws Exception {
    ProxyService ps = new ProxyService();
    URLStreamProvider streamProviderMock = PowerMock.createNiceMock(URLStreamProvider.class);
    HttpURLConnection urlConnectionMock = createMock(HttpURLConnection.class);
    URI uriMock = PowerMock.createMock(URI.class);
    MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
    MultivaluedMap<String, String> headerParams = new MultivaluedMapImpl();
    Map<String, List<String>> headerParamsToForward = new HashMap<>();
    Response.ResponseBuilder responseBuilderMock = PowerMock.createMock(ResponseBuilderImpl.class);
    Response responseMock = createMock(ResponseImpl.class);
    headerParams.add("AmbariProxy-User-Remote","testuser");
    headerParams.add("Content-Type","testtype");
    List<String> userRemoteParams = new LinkedList<>();
    userRemoteParams.add("testuser");
    headerParamsToForward.put("User-Remote", userRemoteParams);
    InputStream is = new ByteArrayInputStream("test".getBytes());
    PowerMock.mockStatic(Response.class);
    expect(getHttpHeaders().getRequestHeaders()).andReturn(headerParams);
    expect(getHttpHeaders().getRequestHeader("AmbariProxy-User-Remote")).andReturn(userRemoteParams);
    expect(getUriInfo().getRequestUri()).andReturn(uriMock);
    expect(getUriInfo().getQueryParameters()).andReturn(queryParams);
    expect(uriMock.getQuery()).andReturn("url=testurl");
    expect(streamProviderMock.processURL("testurl", "GET", (InputStream) null, headerParamsToForward)).andReturn(urlConnectionMock);
    expect(urlConnectionMock.getResponseCode()).andReturn(200);
    expect(urlConnectionMock.getContentType()).andReturn("text/plain");
    expect(urlConnectionMock.getInputStream()).andReturn(is);
    PowerMock.expectNew(URLStreamProvider.class, 20000, 15000, null, null, null).andReturn(streamProviderMock);
    expect(Response.status(200)).andReturn(responseBuilderMock);
    expect(responseBuilderMock.entity(is)).andReturn(responseBuilderMock);
    expect(responseBuilderMock.type("text/plain")).andReturn(responseBuilderMock);
    expect(responseBuilderMock.build()).andReturn(responseMock);
    PowerMock.replay(streamProviderMock, URLStreamProvider.class, Response.class, responseBuilderMock, uriMock, URI.class);
    replay(getUriInfo(), urlConnectionMock, getHttpHeaders());
    Response resultForGetRequest = ps.processGetRequestForwarding(getHttpHeaders(),getUriInfo());
    assertSame(resultForGetRequest, responseMock);
  }

  @Test
  public void testProxyPostRequest() throws Exception {
    ProxyService ps = new ProxyService();
    URLStreamProvider streamProviderMock = PowerMock.createNiceMock(URLStreamProvider.class);
    HttpURLConnection urlConnectionMock = createMock(HttpURLConnection.class);
    URI uriMock = PowerMock.createMock(URI.class);
    MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
    MultivaluedMap<String, String> headerParams = new MultivaluedMapImpl();
    Map<String, List<String>> headerParamsToForward = new HashMap<>();
    Response.ResponseBuilder responseBuilderMock = PowerMock.createMock(ResponseBuilderImpl.class);
    Response responseMock = createMock(ResponseImpl.class);
    headerParams.add("AmbariProxy-User-Remote","testuser");
    headerParams.add("Content-Type","testtype");
    List<String> userRemoteParams = new LinkedList<>();
    userRemoteParams.add("testuser");
    headerParamsToForward.put("User-Remote", userRemoteParams);
    InputStream is = new ByteArrayInputStream("test".getBytes());
    PowerMock.mockStatic(Response.class);
    expect(getHttpHeaders().getRequestHeaders()).andReturn(headerParams);
    expect(getHttpHeaders().getRequestHeader("AmbariProxy-User-Remote")).andReturn(userRemoteParams);
    expect(getUriInfo().getRequestUri()).andReturn(uriMock);
    expect(getUriInfo().getQueryParameters()).andReturn(queryParams);
    expect(uriMock.getQuery()).andReturn("url=testurl");
    expect(getHttpHeaders().getMediaType()).andReturn(APPLICATION_FORM_URLENCODED_TYPE);
    expect(streamProviderMock.processURL("testurl", "POST", is, headerParamsToForward)).andReturn(urlConnectionMock);
    expect(urlConnectionMock.getResponseCode()).andReturn(200);
    expect(urlConnectionMock.getContentType()).andReturn("text/plain");
    expect(urlConnectionMock.getInputStream()).andReturn(is);
    PowerMock.expectNew(URLStreamProvider.class, 20000, 15000, null, null, null).andReturn(streamProviderMock);
    expect(Response.status(200)).andReturn(responseBuilderMock);
    expect(responseBuilderMock.entity(is)).andReturn(responseBuilderMock);
    expect(responseBuilderMock.type("text/plain")).andReturn(responseBuilderMock);
    expect(responseBuilderMock.build()).andReturn(responseMock);
    PowerMock.replay(streamProviderMock, URLStreamProvider.class, Response.class, responseBuilderMock, uriMock, URI.class);
    replay(getUriInfo(), urlConnectionMock, getHttpHeaders());
    Response resultForPostRequest = ps.processPostRequestForwarding(is, getHttpHeaders(), getUriInfo());
    assertSame(resultForPostRequest, responseMock);
  }

  @Test
  public void testProxyPutRequest() throws Exception {
    ProxyService ps = new ProxyService();
    URLStreamProvider streamProviderMock = PowerMock.createNiceMock(URLStreamProvider.class);
    HttpURLConnection urlConnectionMock = createMock(HttpURLConnection.class);
    URI uriMock = PowerMock.createMock(URI.class);
    MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
    MultivaluedMap<String, String> headerParams = new MultivaluedMapImpl();
    Map<String, List<String>> headerParamsToForward = new HashMap<>();
    Response.ResponseBuilder responseBuilderMock = PowerMock.createMock(ResponseBuilderImpl.class);
    Response responseMock = createMock(ResponseImpl.class);
    headerParams.add("AmbariProxy-User-Remote","testuser");
    headerParams.add("Content-Type","testtype");
    List<String> userRemoteParams = new LinkedList<>();
    userRemoteParams.add("testuser");
    headerParamsToForward.put("User-Remote", userRemoteParams);
    InputStream is = new ByteArrayInputStream("test".getBytes());
    PowerMock.mockStatic(Response.class);
    expect(getHttpHeaders().getRequestHeaders()).andReturn(headerParams);
    expect(getHttpHeaders().getRequestHeader("AmbariProxy-User-Remote")).andReturn(userRemoteParams);
    expect(getUriInfo().getRequestUri()).andReturn(uriMock);
    expect(getUriInfo().getQueryParameters()).andReturn(queryParams);
    expect(uriMock.getQuery()).andReturn("url=testurl");
    expect(getHttpHeaders().getMediaType()).andReturn(APPLICATION_FORM_URLENCODED_TYPE);
    expect(streamProviderMock.processURL("testurl", "PUT", is, headerParamsToForward)).andReturn(urlConnectionMock);
    expect(urlConnectionMock.getResponseCode()).andReturn(200);
    expect(urlConnectionMock.getContentType()).andReturn("text/plain");
    expect(urlConnectionMock.getInputStream()).andReturn(is);
    PowerMock.expectNew(URLStreamProvider.class, 20000, 15000, null, null, null).andReturn(streamProviderMock);
    expect(Response.status(200)).andReturn(responseBuilderMock);
    expect(responseBuilderMock.entity(is)).andReturn(responseBuilderMock);
    expect(responseBuilderMock.type("text/plain")).andReturn(responseBuilderMock);
    expect(responseBuilderMock.build()).andReturn(responseMock);
    PowerMock.replay(streamProviderMock, URLStreamProvider.class, Response.class, responseBuilderMock, uriMock, URI.class);
    replay(getUriInfo(), urlConnectionMock, getHttpHeaders());
    Response resultForPutRequest = ps.processPutRequestForwarding(is, getHttpHeaders(), getUriInfo());
    assertSame(resultForPutRequest, responseMock);
  }

  @Test
  public void testProxyDeleteRequest() throws Exception {
    ProxyService ps = new ProxyService();
    URLStreamProvider streamProviderMock = PowerMock.createNiceMock(URLStreamProvider.class);
    HttpURLConnection urlConnectionMock = createMock(HttpURLConnection.class);
    URI uriMock = PowerMock.createMock(URI.class);
    MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
    MultivaluedMap<String, String> headerParams = new MultivaluedMapImpl();
    Map<String, List<String>> headerParamsToForward = new HashMap<>();
    Response.ResponseBuilder responseBuilderMock = PowerMock.createMock(ResponseBuilderImpl.class);
    Response responseMock = createMock(ResponseImpl.class);
    headerParams.add("AmbariProxy-User-Remote","testuser");
    headerParams.add("Content-Type","testtype");
    List<String> userRemoteParams = new LinkedList<>();
    userRemoteParams.add("testuser");
    headerParamsToForward.put("User-Remote", userRemoteParams);
    InputStream is = new ByteArrayInputStream("test".getBytes());
    PowerMock.mockStatic(Response.class);
    expect(getHttpHeaders().getRequestHeaders()).andReturn(headerParams);
    expect(getHttpHeaders().getRequestHeader("AmbariProxy-User-Remote")).andReturn(userRemoteParams);
    expect(getUriInfo().getRequestUri()).andReturn(uriMock);
    expect(getUriInfo().getQueryParameters()).andReturn(queryParams);
    expect(uriMock.getQuery()).andReturn("url=testurl");
    expect(streamProviderMock.processURL("testurl", "DELETE", (InputStream) null, headerParamsToForward)).andReturn(urlConnectionMock);
    expect(urlConnectionMock.getResponseCode()).andReturn(200);
    expect(urlConnectionMock.getContentType()).andReturn("text/plain");
    expect(urlConnectionMock.getInputStream()).andReturn(is);
    PowerMock.expectNew(URLStreamProvider.class, 20000, 15000, null, null, null).andReturn(streamProviderMock);
    expect(Response.status(200)).andReturn(responseBuilderMock);
    expect(responseBuilderMock.entity(is)).andReturn(responseBuilderMock);
    expect(responseBuilderMock.type("text/plain")).andReturn(responseBuilderMock);
    expect(responseBuilderMock.build()).andReturn(responseMock);
    PowerMock.replay(streamProviderMock, URLStreamProvider.class, Response.class, responseBuilderMock, uriMock, URI.class);
    replay(getUriInfo(), urlConnectionMock, getHttpHeaders());
    Response resultForDeleteRequest = ps.processDeleteRequestForwarding(getHttpHeaders(), getUriInfo());
    assertSame(resultForDeleteRequest, responseMock);
  }

  @Test
  public void testResponseWithError() throws Exception {
    ProxyService ps = new ProxyService();
    URLStreamProvider streamProviderMock = PowerMock.createNiceMock(URLStreamProvider.class);
    HttpURLConnection urlConnectionMock = createMock(HttpURLConnection.class);
    Response.ResponseBuilder responseBuilderMock = PowerMock.createMock(ResponseBuilderImpl.class);
    URI uriMock = PowerMock.createMock(URI.class);
    Response responseMock = createMock(ResponseImpl.class);
    InputStream es = new ByteArrayInputStream("error".getBytes());
    MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
    MultivaluedMap<String, String> headerParams = new MultivaluedMapImpl();
    Map<String, List<String>> headerParamsToForward = new HashMap<>();
    headerParams.add("AmbariProxy-User-Remote","testuser");
    headerParams.add("Content-Type","testtype");
    List<String> userRemoteParams = new LinkedList<>();
    userRemoteParams.add("testuser");
    headerParamsToForward.put("User-Remote", userRemoteParams);
    PowerMock.mockStatic(Response.class);
    expect(getHttpHeaders().getRequestHeaders()).andReturn(headerParams);
    expect(getHttpHeaders().getRequestHeader("AmbariProxy-User-Remote")).andReturn(userRemoteParams);
    expect(getUriInfo().getRequestUri()).andReturn(uriMock);
    expect(getUriInfo().getQueryParameters()).andReturn(queryParams);
    expect(uriMock.getQuery()).andReturn("url=testurl");
    expect(streamProviderMock.processURL("testurl", "GET", (InputStream) null, headerParamsToForward)).andReturn(urlConnectionMock);
    expect(urlConnectionMock.getResponseCode()).andReturn(400).times(2);
    expect(urlConnectionMock.getContentType()).andReturn("text/plain");
    expect(urlConnectionMock.getErrorStream()).andReturn(es);
    expect(Response.status(400)).andReturn(responseBuilderMock);
    expect(responseBuilderMock.entity(es)).andReturn(responseBuilderMock);
    expect(responseBuilderMock.type("text/plain")).andReturn(responseBuilderMock);
    expect(responseBuilderMock.build()).andReturn(responseMock);
    PowerMock.expectNew(URLStreamProvider.class, 20000, 15000, null, null, null).andReturn(streamProviderMock);
    PowerMock.replay(streamProviderMock, URLStreamProvider.class, uriMock, URI.class, Response.class, responseBuilderMock);
    replay(getUriInfo(), urlConnectionMock, getHttpHeaders());
    Response resultForErrorRequest = ps.processGetRequestForwarding(getHttpHeaders(),getUriInfo());
    assertSame(resultForErrorRequest, responseMock);
  }

  @Test
  public void testProxyWithJSONResponse() throws Exception {
    ProxyService ps = new ProxyService();
    URLStreamProvider streamProviderMock = PowerMock.createNiceMock(URLStreamProvider.class);
    HttpURLConnection urlConnectionMock = createMock(HttpURLConnection.class);
    URI uriMock = PowerMock.createMock(URI.class);
    MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
    MultivaluedMap<String, String> headerParams = new MultivaluedMapImpl();
    Map<String, List<String>> headerParamsToForward = new HashMap<>();
    Response.ResponseBuilder responseBuilderMock = PowerMock.createMock(ResponseBuilderImpl.class);
    Response responseMock = createMock(ResponseImpl.class);
    headerParams.add("AmbariProxy-User-Remote","testuser");
    headerParams.add("Content-Type","testtype");
    List<String> userRemoteParams = new LinkedList<>();
    userRemoteParams.add("testuser");
    headerParamsToForward.put("User-Remote", userRemoteParams);
    Map map = new Gson().fromJson(new InputStreamReader(new ByteArrayInputStream("{ \"test\":\"test\" }".getBytes())), Map.class);
    PowerMock.mockStatic(Response.class);
    expect(getHttpHeaders().getRequestHeaders()).andReturn(headerParams);
    expect(getHttpHeaders().getRequestHeader("AmbariProxy-User-Remote")).andReturn(userRemoteParams);
    expect(getUriInfo().getRequestUri()).andReturn(uriMock);
    expect(getUriInfo().getQueryParameters()).andReturn(queryParams);
    expect(uriMock.getQuery()).andReturn("url=testurl");
    expect(streamProviderMock.processURL("testurl", "GET", (InputStream) null, headerParamsToForward)).andReturn(urlConnectionMock);
    expect(urlConnectionMock.getResponseCode()).andReturn(200);
    expect(urlConnectionMock.getContentType()).andReturn("application/json");
    expect(urlConnectionMock.getInputStream()).andReturn(new ByteArrayInputStream("{ \"test\":\"test\" }".getBytes()));
    PowerMock.expectNew(URLStreamProvider.class, 20000, 15000, null, null, null).andReturn(streamProviderMock);
    expect(Response.status(200)).andReturn(responseBuilderMock);
    expect(responseBuilderMock.entity(map)).andReturn(responseBuilderMock);
    expect(responseBuilderMock.type("application/json")).andReturn(responseBuilderMock);
    expect(responseBuilderMock.build()).andReturn(responseMock);
    PowerMock.replay(streamProviderMock, URLStreamProvider.class, Response.class, responseBuilderMock, uriMock, URI.class);
    replay(getUriInfo(), urlConnectionMock, getHttpHeaders());
    Response resultForGetRequest = ps.processGetRequestForwarding(getHttpHeaders(),getUriInfo());
    assertSame(resultForGetRequest, responseMock);
  }

  @Test
  public void testEscapedURL() throws Exception {
    ProxyService ps = new ProxyService();
    URLStreamProvider streamProviderMock = PowerMock.createNiceMock(URLStreamProvider.class);
    MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
    MultivaluedMap<String, String> headerParams = new MultivaluedMapImpl();
    HttpURLConnection urlConnectionMock = createMock(HttpURLConnection.class);
    URI uri = UriBuilder.fromUri("http://dev01.hortonworks.com:8080/proxy?url=http%3a%2f%2fserver%3a8188%2fws%2fv1%2f" +
     "timeline%2fHIVE_QUERY_ID%3ffields=events%2cprimaryfilters%26limit=10%26primaryFilter=user%3ahiveuser1").build();
    Map<String, List<String>> headerParamsToForward = new HashMap<>();
    InputStream is = new ByteArrayInputStream("test".getBytes());
    List<String> userRemoteParams = new LinkedList<>();
    userRemoteParams.add("testuser");
    headerParams.add("AmbariProxy-User-Remote","testuser");
    headerParams.add("Content-Type","testtype");
    headerParamsToForward.put("User-Remote", userRemoteParams);
    expect(getHttpHeaders().getRequestHeaders()).andReturn(headerParams);
    expect(getHttpHeaders().getRequestHeader("AmbariProxy-User-Remote")).andReturn(userRemoteParams);
    expect(getUriInfo().getRequestUri()).andReturn(uri);
    expect(getUriInfo().getQueryParameters()).andReturn(queryParams);
    expect(urlConnectionMock.getResponseCode()).andReturn(200);
    expect(urlConnectionMock.getContentType()).andReturn("text/plain");
    expect(urlConnectionMock.getInputStream()).andReturn(is);
    PowerMock.expectNew(URLStreamProvider.class, 20000, 15000, null, null, null).andReturn(streamProviderMock);
    expect(streamProviderMock.processURL("http://server:8188/ws/v1/timeline/HIVE_QUERY_ID?fields=events,primary" +
     "filters&limit=10&primaryFilter=user:hiveuser1", "GET", (InputStream) null, headerParamsToForward)).andReturn(urlConnectionMock);
    PowerMock.replay(streamProviderMock, URLStreamProvider.class);
    replay(getUriInfo(), urlConnectionMock, getHttpHeaders());
    ps.processGetRequestForwarding(getHttpHeaders(),getUriInfo());

  }

  @Override
  public List<ServiceTestInvocation> getTestInvocations() throws Exception {
    return Collections.emptyList();
  }

}
