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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import org.apache.ambari.server.api.handlers.RequestHandler;
import org.apache.ambari.server.api.predicate.InvalidQueryException;
import org.apache.ambari.server.api.predicate.PredicateCompiler;
import org.apache.ambari.server.api.predicate.QueryLexer;
import org.apache.ambari.server.api.query.render.DefaultRenderer;
import org.apache.ambari.server.api.query.render.MinimalRenderer;
import org.apache.ambari.server.api.query.render.Renderer;
import org.apache.ambari.server.api.resources.ResourceDefinition;
import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.controller.internal.TemporalInfoImpl;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.TemporalInfo;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.junit.Test;

import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * Base tests for service requests.
 */
public abstract class BaseRequestTest {

  @Test
  public void testGetBody() {
    RequestBody body = createNiceMock(RequestBody.class);
    Request request = getTestRequest(null, body, null, null, null, null, null);

    assertSame(body, request.getBody());
  }

  @Test
  public void testGetResource() {
    ResourceInstance resource = createNiceMock(ResourceInstance.class);
    Request request = getTestRequest(null, null, null, null, null, null, resource);

    assertSame(resource, request.getResource());
  }

  @Test
  public void testGetApiVersion() {
    Request request = getTestRequest(null, null, null, null, null, null, null);
    assertEquals(1, request.getAPIVersion());
  }

  @Test
  public void testGetHttpHeaders() {
    HttpHeaders headers = createNiceMock(HttpHeaders.class);
    MultivaluedMap<String, String> mapHeaders = new MultivaluedMapImpl();
    Request request = getTestRequest(headers, null, null, null, null, null, null);

    expect(headers.getRequestHeaders()).andReturn(mapHeaders);
    replay(headers);

    assertSame(mapHeaders, request.getHttpHeaders());
    verify(headers);
  }

  @Test
  public void testProcess_noBody() throws Exception {
    String uriString = "http://localhost.com:8080/api/v1/clusters/c1";
    URI uri = new URI(URLEncoder.encode(uriString, "UTF-8"));
    PredicateCompiler compiler = createStrictMock(PredicateCompiler.class);
    UriInfo uriInfo = createMock(UriInfo.class);
    @SuppressWarnings("unchecked")
    MultivaluedMap<String, String> queryParams = createMock(MultivaluedMap.class);
    RequestHandler handler = createStrictMock(RequestHandler.class);
    Result result = createMock(Result.class);
    ResultStatus resultStatus = createMock(ResultStatus.class);
    ResultPostProcessor processor = createStrictMock(ResultPostProcessor.class);
    RequestBody body = createNiceMock(RequestBody.class);
    ResourceInstance resource = createNiceMock(ResourceInstance.class);
    ResourceDefinition resourceDefinition = createNiceMock(ResourceDefinition.class);
    Renderer renderer = new DefaultRenderer();

    Request request = getTestRequest(null, body, uriInfo, compiler, handler, processor, resource);

    //expectations
    expect(uriInfo.getQueryParameters()).andReturn(queryParams).anyTimes();
    expect(queryParams.getFirst(QueryLexer.QUERY_MINIMAL)).andReturn(null);
    expect(queryParams.getFirst(QueryLexer.QUERY_FORMAT)).andReturn(null);
    expect(resource.getResourceDefinition()).andReturn(resourceDefinition);
    expect(resourceDefinition.getRenderer(null)).andReturn(renderer);
    expect(uriInfo.getRequestUri()).andReturn(uri).anyTimes();
    expect(handler.handleRequest(request)).andReturn(result);
    expect(result.getStatus()).andReturn(resultStatus).anyTimes();
    expect(resultStatus.isErrorState()).andReturn(false).anyTimes();
    processor.process(result);

    replay(compiler, uriInfo, handler, queryParams, resource, resourceDefinition, result, resultStatus, processor, body);

    Result processResult = request.process();

    verify(compiler, uriInfo, handler, queryParams, resource, resourceDefinition, result, resultStatus, processor, body);
    assertSame(result, processResult);
    assertNull(request.getQueryPredicate());
  }

  @Test
  public void testProcess_withDirectives() throws Exception {
    HttpHeaders headers = createNiceMock(HttpHeaders.class);
    String path = URLEncoder.encode("http://localhost.com:8080/api/v1/clusters/c1", "UTF-8");
    String query = URLEncoder.encode("foo=foo-value&bar=bar-value", "UTF-8");
    URI uri = new URI(path + "?" + query);
    PredicateCompiler compiler = createStrictMock(PredicateCompiler.class);
    Predicate predicate = createNiceMock(Predicate.class);
    UriInfo uriInfo = createMock(UriInfo.class);
    @SuppressWarnings("unchecked")
    MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
    RequestHandler handler = createStrictMock(RequestHandler.class);
    Result result = createMock(Result.class);
    ResultStatus resultStatus = createMock(ResultStatus.class);
    ResultPostProcessor processor = createStrictMock(ResultPostProcessor.class);
    RequestBody body = createNiceMock(RequestBody.class);
    ResourceInstance resource = createNiceMock(ResourceInstance.class);
    ResourceDefinition resourceDefinition = createNiceMock(ResourceDefinition.class);
    Set<String> directives = Collections.singleton("my_directive");
    Renderer renderer = new DefaultRenderer();

    Request request = getTestRequest(headers, body, uriInfo, compiler, handler, processor, resource);

    //expectations
    expect(uriInfo.getQueryParameters()).andReturn(queryParams).anyTimes();
    expect(resource.getResourceDefinition()).andReturn(resourceDefinition).anyTimes();
    expect(resourceDefinition.getUpdateDirectives()).andReturn(directives).anyTimes(); // for PUT implementation
    expect(resourceDefinition.getCreateDirectives()).andReturn(directives).anyTimes(); // for POST implementation
    expect(resourceDefinition.getDeleteDirectives()).andReturn(directives).anyTimes(); // for DELETE implementation
    expect(resourceDefinition.getRenderer(null)).andReturn(renderer);
    expect(uriInfo.getRequestUri()).andReturn(uri).anyTimes();
    expect(body.getQueryString()).andReturn(null);
    if (request.getRequestType() == Request.Type.POST || request.getRequestType() == Request.Type.PUT
            || request.getRequestType() == Request.Type.DELETE)
    {
      expect(compiler.compile("foo=foo-value&bar=bar-value", directives)).andReturn(predicate);
    }
    else
    {
      expect(compiler.compile("foo=foo-value&bar=bar-value")).andReturn(predicate); // default case
    }
    expect(handler.handleRequest(request)).andReturn(result);
    expect(result.getStatus()).andReturn(resultStatus).anyTimes();
    expect(resultStatus.isErrorState()).andReturn(false).anyTimes();

    processor.process(result);

    replay(headers, compiler, uriInfo, handler, resource,
      resourceDefinition, result, resultStatus, processor, predicate, body);

    Result processResult = request.process();

    verify(headers, compiler, uriInfo, handler, resource,
      resourceDefinition, result, resultStatus, processor, predicate, body);

    assertSame(processResult, result);
    assertSame(predicate, request.getQueryPredicate());
  }

  @Test
  public void testProcess_WithBody() throws Exception {
    String uriString = "http://localhost.com:8080/api/v1/clusters/c1";
    URI uri = new URI(URLEncoder.encode(uriString, "UTF-8"));
    PredicateCompiler compiler = createStrictMock(PredicateCompiler.class);
    UriInfo uriInfo = createMock(UriInfo.class);
    @SuppressWarnings("unchecked")
    MultivaluedMap<String, String> queryParams = createMock(MultivaluedMap.class);
    RequestHandler handler = createStrictMock(RequestHandler.class);
    Result result = createMock(Result.class);
    ResultStatus resultStatus = createMock(ResultStatus.class);
    ResultPostProcessor processor = createStrictMock(ResultPostProcessor.class);
    RequestBody body = createNiceMock(RequestBody.class);
    ResourceInstance resource = createNiceMock(ResourceInstance.class);
    ResourceDefinition resourceDefinition = createNiceMock(ResourceDefinition.class);
    Renderer renderer = new DefaultRenderer();

    Request request = getTestRequest(null, body, uriInfo, compiler, handler, processor, resource);

    //expectations
    expect(uriInfo.getQueryParameters()).andReturn(queryParams).anyTimes();
    expect(queryParams.getFirst(QueryLexer.QUERY_MINIMAL)).andReturn(null);
    expect(queryParams.getFirst(QueryLexer.QUERY_FORMAT)).andReturn(null);
    expect(resource.getResourceDefinition()).andReturn(resourceDefinition);
    expect(resourceDefinition.getRenderer(null)).andReturn(renderer);
    expect(uriInfo.getRequestUri()).andReturn(uri).anyTimes();
    expect(handler.handleRequest(request)).andReturn(result);
    expect(result.getStatus()).andReturn(resultStatus).anyTimes();
    expect(resultStatus.isErrorState()).andReturn(false).anyTimes();
    processor.process(result);
    expect(body.getQueryString()).andReturn(null);

    replay(compiler, uriInfo, handler, queryParams, resource,
        resourceDefinition, result, resultStatus, processor, body);

    Result processResult = request.process();

    verify(compiler, uriInfo, handler, queryParams, resource,
        resourceDefinition, result, resultStatus, processor, body);
    assertSame(result, processResult);
    assertNull(request.getQueryPredicate());
  }


  @Test
  public void testProcess_QueryInURI() throws Exception {
    HttpHeaders headers = createNiceMock(HttpHeaders.class);
    String path = URLEncoder.encode("http://localhost.com:8080/api/v1/clusters/c1", "UTF-8");
    String query = URLEncoder.encode("foo=foo-value&bar=bar-value", "UTF-8");
    URI uri = new URI(path + "?" + query);
    PredicateCompiler compiler = createStrictMock(PredicateCompiler.class);
    Predicate predicate = createNiceMock(Predicate.class);
    UriInfo uriInfo = createMock(UriInfo.class);
    @SuppressWarnings("unchecked")
    MultivaluedMap<String, String> queryParams = createMock(MultivaluedMap.class);
    RequestHandler handler = createStrictMock(RequestHandler.class);
    Result result = createMock(Result.class);
    ResultStatus resultStatus = createMock(ResultStatus.class);
    ResultPostProcessor processor = createStrictMock(ResultPostProcessor.class);
    RequestBody body = createNiceMock(RequestBody.class);
    ResourceInstance resource = createNiceMock(ResourceInstance.class);
    ResourceDefinition resourceDefinition = createNiceMock(ResourceDefinition.class);
    Renderer renderer = new DefaultRenderer();

    Request request = getTestRequest(headers, body, uriInfo, compiler, handler, processor, resource);

    //expectations
    expect(uriInfo.getQueryParameters()).andReturn(queryParams).anyTimes();
    expect(queryParams.getFirst(QueryLexer.QUERY_MINIMAL)).andReturn(null);
    expect(queryParams.getFirst(QueryLexer.QUERY_FORMAT)).andReturn(null);
    expect(resource.getResourceDefinition()).andReturn(resourceDefinition).anyTimes();
    expect(resourceDefinition.getRenderer(null)).andReturn(renderer);
    expect(uriInfo.getRequestUri()).andReturn(uri).anyTimes();
    expect(body.getQueryString()).andReturn(null);
    expect(compiler.compile("foo=foo-value&bar=bar-value")).andReturn(predicate);
    expect(handler.handleRequest(request)).andReturn(result);
    expect(result.getStatus()).andReturn(resultStatus).anyTimes();
    expect(resultStatus.isErrorState()).andReturn(false).anyTimes();
    processor.process(result);

    replay(headers, compiler, uriInfo, handler, queryParams, resource,
        resourceDefinition, result, resultStatus, processor, predicate, body);

    Result processResult = request.process();

    verify(headers, compiler, uriInfo, handler, queryParams, resource,
        resourceDefinition, result, resultStatus, processor, predicate, body);

    assertSame(processResult, result);
    assertSame(predicate, request.getQueryPredicate());
  }

  @Test
  public void testProcess_QueryInBody() throws Exception {
    HttpHeaders headers = createNiceMock(HttpHeaders.class);
    String uriString = "http://localhost.com:8080/api/v1/clusters/c1";
    URI uri = new URI(URLEncoder.encode(uriString, "UTF-8"));
    @SuppressWarnings("unchecked")
    MultivaluedMap<String, String> queryParams = createMock(MultivaluedMap.class);
    PredicateCompiler compiler = createStrictMock(PredicateCompiler.class);
    Predicate predicate = createNiceMock(Predicate.class);
    UriInfo uriInfo = createMock(UriInfo.class);
    RequestHandler handler = createStrictMock(RequestHandler.class);
    Result result = createMock(Result.class);
    ResultStatus resultStatus = createMock(ResultStatus.class);
    ResultPostProcessor processor = createStrictMock(ResultPostProcessor.class);
    RequestBody body = createNiceMock(RequestBody.class);
    ResourceInstance resource = createNiceMock(ResourceInstance.class);
    ResourceDefinition resourceDefinition = createNiceMock(ResourceDefinition.class);
    Renderer renderer = new DefaultRenderer();

    Request request = getTestRequest(headers, body, uriInfo, compiler, handler, processor, resource);

    //expectations
    expect(uriInfo.getQueryParameters()).andReturn(queryParams).anyTimes();
    expect(queryParams.getFirst(QueryLexer.QUERY_MINIMAL)).andReturn(null);
    expect(queryParams.getFirst(QueryLexer.QUERY_FORMAT)).andReturn(null);
    expect(resource.getResourceDefinition()).andReturn(resourceDefinition).anyTimes();
    expect(resourceDefinition.getRenderer(null)).andReturn(renderer);
    expect(uriInfo.getRequestUri()).andReturn(uri).anyTimes();
    expect(body.getQueryString()).andReturn("foo=bar");
    expect(compiler.compile("foo=bar")).andReturn(predicate);
    expect(handler.handleRequest(request)).andReturn(result);
    expect(result.getStatus()).andReturn(resultStatus).anyTimes();
    expect(resultStatus.isErrorState()).andReturn(false).anyTimes();
    processor.process(result);

    replay(headers, compiler, uriInfo, handler, queryParams, resource,
        resourceDefinition, result, resultStatus, processor, predicate, body);

    Result processResult = request.process();

    verify(headers, compiler, uriInfo, handler, queryParams, resource,
        resourceDefinition, result, resultStatus, processor, predicate, body);

    assertSame(processResult, result);
    assertSame(predicate, request.getQueryPredicate());
  }

  @Test
  public void testProcess_QueryInBodyAndURI() throws Exception {
    HttpHeaders headers = createNiceMock(HttpHeaders.class);
    String uriString = "http://localhost.com:8080/api/v1/clusters/c1?bar=value";
    URI uri = new URI(URLEncoder.encode(uriString, "UTF-8"));
    PredicateCompiler compiler = createStrictMock(PredicateCompiler.class);
    Predicate predicate = createNiceMock(Predicate.class);
    UriInfo uriInfo = createMock(UriInfo.class);
    @SuppressWarnings("unchecked")
    MultivaluedMap<String, String> queryParams = createMock(MultivaluedMap.class);
    RequestHandler handler = createStrictMock(RequestHandler.class);
    Result result = createMock(Result.class);
    ResultStatus resultStatus = createMock(ResultStatus.class);
    ResultPostProcessor processor = createStrictMock(ResultPostProcessor.class);
    RequestBody body = createNiceMock(RequestBody.class);
    ResourceInstance resource = createNiceMock(ResourceInstance.class);
    ResourceDefinition resourceDefinition = createNiceMock(ResourceDefinition.class);
    Renderer renderer = new DefaultRenderer();

    Request request = getTestRequest(headers, body, uriInfo, compiler, handler, processor, resource);

    //expectations
    expect(uriInfo.getQueryParameters()).andReturn(queryParams).anyTimes();
    expect(queryParams.getFirst(QueryLexer.QUERY_MINIMAL)).andReturn(null);
    expect(queryParams.getFirst(QueryLexer.QUERY_FORMAT)).andReturn(null);
    expect(resource.getResourceDefinition()).andReturn(resourceDefinition).anyTimes();
    expect(resourceDefinition.getRenderer(null)).andReturn(renderer);
    expect(uriInfo.getRequestUri()).andReturn(uri).anyTimes();
    expect(body.getQueryString()).andReturn("foo=bar");
    expect(compiler.compile("foo=bar")).andReturn(predicate);
    expect(handler.handleRequest(request)).andReturn(result);
    expect(result.getStatus()).andReturn(resultStatus).anyTimes();
    expect(resultStatus.isErrorState()).andReturn(false).anyTimes();
    processor.process(result);

    replay(headers, compiler, uriInfo, handler, queryParams, resource,
        resourceDefinition, result, resultStatus, processor, predicate, body);

    Result processResult = request.process();

    verify(headers, compiler, uriInfo, handler, queryParams, resource,
        resourceDefinition, result, resultStatus, processor, predicate, body);

    assertSame(processResult, result);
    assertSame(predicate, request.getQueryPredicate());
  }

  @Test
  public void testProcess_WithBody_InvalidQuery() throws Exception {
    UriInfo uriInfo = createMock(UriInfo.class);
    String uriString = "http://localhost.com:8080/api/v1/clusters/c1";
    @SuppressWarnings("unchecked")
    MultivaluedMap<String, String> queryParams = createMock(MultivaluedMap.class);
    URI uri = new URI(URLEncoder.encode(uriString, "UTF-8"));
    PredicateCompiler compiler = createStrictMock(PredicateCompiler.class);
    RequestBody body = createNiceMock(RequestBody.class);
    Exception exception = new InvalidQueryException("test");
    ResourceInstance resource = createNiceMock(ResourceInstance.class);
    ResourceDefinition resourceDefinition = createNiceMock(ResourceDefinition.class);
    Renderer renderer = new DefaultRenderer();

    Request request = getTestRequest(null, body, uriInfo, compiler, null, null, resource);

    //expectations
    expect(uriInfo.getQueryParameters()).andReturn(queryParams).anyTimes();
    expect(queryParams.getFirst(QueryLexer.QUERY_MINIMAL)).andReturn(null);
    expect(queryParams.getFirst(QueryLexer.QUERY_FORMAT)).andReturn(null);
    expect(resource.getResourceDefinition()).andReturn(resourceDefinition).anyTimes();
    expect(resourceDefinition.getRenderer(null)).andReturn(renderer);
    expect(uriInfo.getRequestUri()).andReturn(uri).anyTimes();
    expect(body.getQueryString()).andReturn("blahblahblah");
    expect(compiler.compile("blahblahblah")).andThrow(exception);

    replay(compiler, uriInfo, queryParams, resource,
        resourceDefinition, body);

    Result processResult = request.process();

    verify(compiler, uriInfo, queryParams, resource,
        resourceDefinition, body);

    assertEquals(400, processResult.getStatus().getStatusCode());
    assertTrue(processResult.getStatus().isErrorState());
    assertEquals("Unable to compile query predicate: test", processResult.getStatus().getMessage());
  }

  @Test
  public void testProcess_noBody_ErrorStateResult() throws Exception {
    String uriString = "http://localhost.com:8080/api/v1/clusters/c1";
    URI uri = new URI(URLEncoder.encode(uriString, "UTF-8"));
    PredicateCompiler compiler = createStrictMock(PredicateCompiler.class);
    UriInfo uriInfo = createMock(UriInfo.class);
    @SuppressWarnings("unchecked")
    MultivaluedMap<String, String> queryParams = createMock(MultivaluedMap.class);
    RequestHandler handler = createStrictMock(RequestHandler.class);
    Result result = createMock(Result.class);
    ResultStatus resultStatus = createMock(ResultStatus.class);
    ResultPostProcessor processor = createStrictMock(ResultPostProcessor.class);
    RequestBody body = createNiceMock(RequestBody.class);
    ResourceInstance resource = createNiceMock(ResourceInstance.class);
    ResourceDefinition resourceDefinition = createNiceMock(ResourceDefinition.class);
    Renderer renderer = new DefaultRenderer();

    Request request = getTestRequest(null, body, uriInfo, compiler, handler, processor, resource);

    //expectations
    expect(uriInfo.getQueryParameters()).andReturn(queryParams).anyTimes();
    expect(queryParams.getFirst(QueryLexer.QUERY_MINIMAL)).andReturn(null);
    expect(queryParams.getFirst(QueryLexer.QUERY_FORMAT)).andReturn(null);
    expect(resource.getResourceDefinition()).andReturn(resourceDefinition).anyTimes();
    expect(resourceDefinition.getRenderer(null)).andReturn(renderer);
    expect(uriInfo.getRequestUri()).andReturn(uri).anyTimes();
    expect(handler.handleRequest(request)).andReturn(result);
    expect(result.getStatus()).andReturn(resultStatus).anyTimes();
    expect(resultStatus.isErrorState()).andReturn(true).anyTimes();

    replay(compiler, uriInfo, handler, queryParams, resource,
        resourceDefinition, result, resultStatus, processor, body);

    Result processResult = request.process();

    verify(compiler, uriInfo, handler, queryParams, resource,
        resourceDefinition, result, resultStatus, processor, body);
    assertSame(result, processResult);
    assertNull(request.getQueryPredicate());
  }

  @Test
  public void testGetFields() throws Exception {
    String fields = "prop,category/prop1,category2/category3/prop2[1,2,3],prop3[4,5,6],category4[7,8,9],sub-resource/*[10,11,12],finalProp";
    UriInfo uriInfo = createMock(UriInfo.class);

    @SuppressWarnings("unchecked")
    MultivaluedMap<String, String> mapQueryParams = createMock(MultivaluedMap.class);

    expect(uriInfo.getQueryParameters()).andReturn(mapQueryParams);
    expect(mapQueryParams.getFirst("fields")).andReturn(fields);

    replay(uriInfo, mapQueryParams);

    Request request = getTestRequest(null, null, uriInfo, null, null, null, null);
    Map<String, TemporalInfo> mapFields = request.getFields();

    assertEquals(7, mapFields.size());

    String prop = "prop";
    assertTrue(mapFields.containsKey(prop));
    assertNull(mapFields.get(prop));

    String prop1 = PropertyHelper.getPropertyId("category", "prop1");
    assertTrue(mapFields.containsKey(prop1));
    assertNull(mapFields.get(prop1));

    String prop2 = PropertyHelper.getPropertyId("category2/category3", "prop2");
    assertTrue(mapFields.containsKey(prop2));
    assertEquals(new TemporalInfoImpl(1, 2, 3), mapFields.get(prop2));

    String prop3 = "prop3";
    assertTrue(mapFields.containsKey(prop3));
    assertEquals(new TemporalInfoImpl(4, 5, 6), mapFields.get(prop3));

    String category4 = "category4";
    assertTrue(mapFields.containsKey(category4));
    assertEquals(new TemporalInfoImpl(7, 8, 9), mapFields.get(category4));

    String subResource = PropertyHelper.getPropertyId("sub-resource", "*");
    assertTrue(mapFields.containsKey(subResource));
    assertEquals(new TemporalInfoImpl(10, 11, 12), mapFields.get(subResource));

    String finalProp = "finalProp";
    assertTrue(mapFields.containsKey(finalProp));
    assertNull(mapFields.get(finalProp));

    verify(uriInfo, mapQueryParams);
  }

  @Test
  public void testParseRenderer_minimalResponse() throws Exception {

    String uriString = "http://localhost.com:8080/api/v1/clusters/c1";
    URI uri = new URI(URLEncoder.encode(uriString, "UTF-8"));
    PredicateCompiler compiler = createStrictMock(PredicateCompiler.class);
    UriInfo uriInfo = createMock(UriInfo.class);
    @SuppressWarnings("unchecked")
    MultivaluedMap<String, String> queryParams = createMock(MultivaluedMap.class);

    RequestHandler handler = createStrictMock(RequestHandler.class);
    Result result = createMock(Result.class);
    ResultStatus resultStatus = createMock(ResultStatus.class);
    ResultPostProcessor processor = createStrictMock(ResultPostProcessor.class);
    RequestBody body = createNiceMock(RequestBody.class);
    ResourceInstance resource = createNiceMock(ResourceInstance.class);
    ResourceDefinition resourceDefinition = createNiceMock(ResourceDefinition.class);
    Renderer renderer = new MinimalRenderer();

    Request request = getTestRequest(null, body, uriInfo, compiler, handler, processor, resource);

    //expectations
    expect(uriInfo.getQueryParameters()).andReturn(queryParams).anyTimes();
    expect(queryParams.getFirst(QueryLexer.QUERY_MINIMAL)).andReturn("true");
    expect(resource.getResourceDefinition()).andReturn(resourceDefinition).anyTimes();
    expect(resourceDefinition.getRenderer("minimal")).andReturn(renderer);
    expect(uriInfo.getRequestUri()).andReturn(uri).anyTimes();
    expect(handler.handleRequest(request)).andReturn(result);
    expect(result.getStatus()).andReturn(resultStatus).anyTimes();
    expect(resultStatus.isErrorState()).andReturn(false).anyTimes();
    processor.process(result);

    replay(compiler, uriInfo, handler, queryParams, resource, resourceDefinition, result, resultStatus, processor, body);

    request.process();

    verify(compiler, uriInfo, handler, queryParams, resource, resourceDefinition, result, resultStatus, processor, body);

    assertSame(renderer, request.getRenderer());
  }

  @Test
  public void testParseRenderer_formatSpecified() throws Exception {

    String uriString = "http://localhost.com:8080/api/v1/clusters/c1";
    URI uri = new URI(URLEncoder.encode(uriString, "UTF-8"));
    PredicateCompiler compiler = createStrictMock(PredicateCompiler.class);
    UriInfo uriInfo = createMock(UriInfo.class);
    @SuppressWarnings("unchecked")
    MultivaluedMap<String, String> queryParams = createMock(MultivaluedMap.class);

    RequestHandler handler = createStrictMock(RequestHandler.class);
    Result result = createMock(Result.class);
    ResultStatus resultStatus = createMock(ResultStatus.class);
    ResultPostProcessor processor = createStrictMock(ResultPostProcessor.class);
    RequestBody body = createNiceMock(RequestBody.class);
    ResourceInstance resource = createNiceMock(ResourceInstance.class);
    ResourceDefinition resourceDefinition = createNiceMock(ResourceDefinition.class);
    Renderer renderer = new DefaultRenderer();

    Request request = getTestRequest(null, body, uriInfo, compiler, handler, processor, resource);

    //expectations
    expect(uriInfo.getQueryParameters()).andReturn(queryParams).anyTimes();
    expect(queryParams.getFirst(QueryLexer.QUERY_MINIMAL)).andReturn(null);
    expect(queryParams.getFirst(QueryLexer.QUERY_FORMAT)).andReturn("default");
    expect(resource.getResourceDefinition()).andReturn(resourceDefinition).anyTimes();
    expect(resourceDefinition.getRenderer("default")).andReturn(renderer);
    expect(uriInfo.getRequestUri()).andReturn(uri).anyTimes();
    expect(handler.handleRequest(request)).andReturn(result);
    expect(result.getStatus()).andReturn(resultStatus).anyTimes();
    expect(resultStatus.isErrorState()).andReturn(false).anyTimes();
    processor.process(result);

    replay(compiler, uriInfo, handler, queryParams, resource, resourceDefinition, result, resultStatus, processor, body);

    request.process();

    verify(compiler, uriInfo, handler, queryParams, resource, resourceDefinition, result, resultStatus, processor, body);

    assertSame(renderer, request.getRenderer());
  }

   protected abstract Request getTestRequest(HttpHeaders headers, RequestBody body, UriInfo uriInfo, PredicateCompiler compiler,
                                             RequestHandler handler, ResultPostProcessor processor, ResourceInstance resource);

}
