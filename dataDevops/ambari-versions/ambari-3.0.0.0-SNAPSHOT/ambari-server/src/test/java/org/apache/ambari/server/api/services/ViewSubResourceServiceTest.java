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
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.api.services.parsers.RequestBodyParser;
import org.apache.ambari.server.api.services.serializers.ResultSerializer;
import org.apache.ambari.server.api.services.views.ViewSubResourceService;
import org.apache.ambari.server.api.util.TreeNode;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.orm.entities.ViewInstanceEntity;
import org.apache.ambari.server.orm.entities.ViewInstanceEntityTest;
import org.junit.Test;

/**
 * ViewSubResourceService tests
 */
public class ViewSubResourceServiceTest extends BaseServiceTest {

  @Override
  public List<ServiceTestInvocation> getTestInvocations() throws Exception {
    List<ServiceTestInvocation> listInvocations = new ArrayList<>();

    ViewInstanceEntity viewInstanceEntity = ViewInstanceEntityTest.getViewInstanceEntity();

    Resource.Type type = new Resource.Type("subResource");

    // get resource
    ViewSubResourceService service = new TestViewSubResourceService(type, viewInstanceEntity);
    Method m = service.getClass().getMethod("getSubResource1", HttpHeaders.class, UriInfo.class, String.class);
    Object[] args = new Object[] {getHttpHeaders(), getUriInfo(), "id"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.GET, service, m, args, null));

    // get resource
    service = new TestViewSubResourceService(type, viewInstanceEntity);
    m = service.getClass().getMethod("getSubResource2", HttpHeaders.class, UriInfo.class, String.class);
    args = new Object[] {getHttpHeaders(), getUriInfo(), "id"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.GET, service, m, args, null));

    // create resource
    service = new TestViewSubResourceService(type, viewInstanceEntity);
    m = service.getClass().getMethod("postSubResource", HttpHeaders.class, UriInfo.class, String.class);
    args = new Object[] {getHttpHeaders(), getUriInfo(), "id"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.POST, service, m, args, null));

    // update resource
    service = new TestViewSubResourceService(type, viewInstanceEntity);
    m = service.getClass().getMethod("putSubResource", HttpHeaders.class, UriInfo.class, String.class);
    args = new Object[] {getHttpHeaders(), getUriInfo(), "id"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.PUT, service, m, args, null));

    // delete resource
    service = new TestViewSubResourceService(type, viewInstanceEntity);
    m = service.getClass().getMethod("deleteSubResource", HttpHeaders.class, UriInfo.class, String.class);
    args = new Object[] {getHttpHeaders(), getUriInfo(), "id"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.DELETE, service, m, args, null));

    return listInvocations;
  }

  @Test
  public void testGetResultSerializer_Text() throws Exception {
    UriInfo uriInfo = createMock(UriInfo.class);
    Resource resource = createMock(Resource.class);

    Result result = new ResultImpl(true);
    result.setResultStatus(new ResultStatus(ResultStatus.STATUS.OK));
    TreeNode<Resource> tree = result.getResultTree();
    TreeNode<Resource> child = tree.addChild(resource, "resource1");
    child.setProperty("href", "this is an href");

    // resource properties
    Map<String, Object> mapRootProps = new LinkedHashMap<>();
    mapRootProps.put("prop2", "value2");
    mapRootProps.put("prop1", "value1");

    Map<String, Object> mapCategoryProps = new LinkedHashMap<>();
    mapCategoryProps.put("catProp1", "catValue1");
    mapCategoryProps.put("catProp2", "catValue2");

    Map<String, Map<String, Object>> propertyMap = new LinkedHashMap<>();

    propertyMap.put(null, mapRootProps);
    propertyMap.put("category", mapCategoryProps);

    //expectations
    expect(resource.getPropertiesMap()).andReturn(propertyMap).anyTimes();
    expect(resource.getType()).andReturn(Resource.Type.Cluster).anyTimes();

    replay(uriInfo, resource);

    //execute test
    ViewInstanceEntity viewInstanceEntity = ViewInstanceEntityTest.getViewInstanceEntity();

    Resource.Type type = new Resource.Type("subResource");

    // get resource
    ViewSubResourceService service = new ViewSubResourceService(type, viewInstanceEntity);

    ResultSerializer serializer = service.getResultSerializer(MediaType.TEXT_PLAIN_TYPE);

    Object o = serializer.serialize(result);

    String expected = "{\n" +
        "  \"href\" : \"this is an href\",\n" +
        "  \"prop2\" : \"value2\",\n" +
        "  \"prop1\" : \"value1\",\n" +
        "  \"category\" : {\n" +
        "    \"catProp1\" : \"catValue1\",\n" +
        "    \"catProp2\" : \"catValue2\"\n" +
        "  }\n" +
        "}";

    assertEquals(expected, o.toString().replace("\r", ""));

    verify(uriInfo, resource);
  }

  @Test
  public void testGetResultSerializer_Json() throws Exception {
    UriInfo uriInfo = createMock(UriInfo.class);
    Resource resource = createMock(Resource.class);

    Result result = new ResultImpl(true);
    result.setResultStatus(new ResultStatus(ResultStatus.STATUS.OK));
    TreeNode<Resource> tree = result.getResultTree();
    TreeNode<Resource> child = tree.addChild(resource, "resource1");
    child.setProperty("href", "this is an href");

    // resource properties
    HashMap<String, Object> mapRootProps = new HashMap<>();
    mapRootProps.put("prop1", "value1");
    mapRootProps.put("prop2", "value2");

    HashMap<String, Object> mapCategoryProps = new HashMap<>();
    mapCategoryProps.put("catProp1", "catValue1");
    mapCategoryProps.put("catProp2", "catValue2");

    Map<String, Map<String, Object>> propertyMap = new HashMap<>();

    propertyMap.put(null, mapRootProps);
    propertyMap.put("category", mapCategoryProps);

    //expectations
    expect(resource.getPropertiesMap()).andReturn(propertyMap).anyTimes();
    expect(resource.getType()).andReturn(Resource.Type.Cluster).anyTimes();

    replay(uriInfo, resource);

    //execute test
    ViewInstanceEntity viewInstanceEntity = ViewInstanceEntityTest.getViewInstanceEntity();

    Resource.Type type = new Resource.Type("subResource");

    // get resource
    ViewSubResourceService service = new ViewSubResourceService(type, viewInstanceEntity);

    ResultSerializer serializer = service.getResultSerializer(MediaType.APPLICATION_JSON_TYPE);

    Object o = serializer.serialize(result);

    assertTrue(o instanceof Map);
    Map map = (Map) o;
    assertEquals(4, map.size());
    assertEquals("value1", map.get("prop1"));
    assertEquals("value2", map.get("prop2"));
    assertEquals("this is an href", map.get("href"));
    Object o2 = map.get("category");
    assertNotNull(o2);
    assertTrue(o2 instanceof Map);
    Map subMap = (Map) o2;
    assertEquals(2, subMap.size());
    assertEquals("catValue1", subMap.get("catProp1"));
    assertEquals("catValue2", subMap.get("catProp2"));

    verify(uriInfo, resource);
  }

  private class TestViewSubResourceService extends ViewSubResourceService {

    /**
     * Construct a view sub-resource service.
     */
    public TestViewSubResourceService(Resource.Type type, ViewInstanceEntity viewInstanceDefinition) {
      super(type, viewInstanceDefinition);
    }

    public Response getSubResource1(@Context HttpHeaders headers, @Context UriInfo ui,
                             @PathParam("resourceId") String resourceId) {

      return handleRequest(headers, ui, resourceId);
    }

    public Response getSubResource2(@Context HttpHeaders headers, @Context UriInfo ui,
                                   @PathParam("resourceId") String resourceId) {

      return handleRequest(headers, ui, RequestType.GET, MediaType.TEXT_PLAIN, resourceId);
    }

    public Response postSubResource(@Context HttpHeaders headers, @Context UriInfo ui,
                                   @PathParam("resourceId") String resourceId) {

      return handleRequest(headers, ui, RequestType.POST, MediaType.TEXT_PLAIN, resourceId);
    }

    public Response putSubResource(@Context HttpHeaders headers, @Context UriInfo ui,
                                    @PathParam("resourceId") String resourceId) {

      return handleRequest(headers, ui, RequestType.PUT, MediaType.TEXT_PLAIN, resourceId);
    }

    public Response deleteSubResource(@Context HttpHeaders headers, @Context UriInfo ui,
                                    @PathParam("resourceId") String resourceId) {

      return handleRequest(headers, ui, RequestType.DELETE, MediaType.TEXT_PLAIN, resourceId);
    }

    @Override
    protected ResourceInstance createResource(String resourceId) {
      return getTestResource();
    }

    @Override
    RequestFactory getRequestFactory() {
      return getTestRequestFactory();
    }

    @Override
    protected RequestBodyParser getBodyParser() {
      return getTestBodyParser();
    }

    @Override
    protected ResultSerializer getResultSerializer() {
      return getTestResultSerializer();
    }

    @Override
    protected ResultSerializer getResultSerializer(javax.ws.rs.core.MediaType mediaType) {
      return getTestResultSerializer();
    }
  }
}


