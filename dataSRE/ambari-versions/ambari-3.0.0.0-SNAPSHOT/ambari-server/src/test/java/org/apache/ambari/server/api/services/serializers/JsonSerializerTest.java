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

package org.apache.ambari.server.api.services.serializers;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.ws.rs.core.UriInfo;

import org.apache.ambari.server.api.services.DeleteResultMetadata;
import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.api.services.ResultImpl;
import org.apache.ambari.server.api.services.ResultStatus;
import org.apache.ambari.server.api.util.TreeNode;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.junit.Test;

/**
 * JSONSerializer unit tests
 */
public class JsonSerializerTest {

  @Test
  public void testSerialize() throws Exception {
    UriInfo uriInfo = createMock(UriInfo.class);
    Resource resource = createMock(Resource.class);
    //Resource resource2 = createMock(Resource.class);

    Result result = new ResultImpl(true);
    result.setResultStatus(new ResultStatus(ResultStatus.STATUS.OK));
    TreeNode<Resource> tree = result.getResultTree();
    //tree.setName("items");
    TreeNode<Resource> child = tree.addChild(resource, "resource1");
    child.setProperty("href", "this is an href");
    //child.addChild(resource2, "sub-resource");

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

    replay(uriInfo, resource/*, resource2*/);

    //execute test
    Object o = new JsonSerializer().serialize(result).toString().replace("\r", "");

    String expected = "{\n" +
        "  \"href\" : \"this is an href\",\n" +
        "  \"prop2\" : \"value2\",\n" +
        "  \"prop1\" : \"value1\",\n" +
        "  \"category\" : {\n" +
        "    \"catProp1\" : \"catValue1\",\n" +
        "    \"catProp2\" : \"catValue2\"\n" +
        "  }\n" +
        "}";

    assertEquals(expected, o);

    verify(uriInfo, resource/*, resource2*/);
  }

  @Test
  public void testSerializeResources() throws Exception {
    UriInfo uriInfo = createMock(UriInfo.class);
    Resource resource = createMock(Resource.class);
    //Resource resource2 = createMock(Resource.class);

    Result result = new ResultImpl(true);
    result.setResultStatus(new ResultStatus(ResultStatus.STATUS.OK));
    TreeNode<Resource> tree = result.getResultTree();


    TreeNode<Resource> resourcesNode = tree.addChild(null, "resources");
    

    resourcesNode.addChild(resource, "resource1");

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
    Object o = new JsonSerializer().serialize(result).toString().replace("\r", "");

    String expected = "{\n" +
        "  \"resources\" : [\n" +
        "    {\n" +
        "      \"prop2\" : \"value2\",\n" +
        "      \"prop1\" : \"value1\",\n" +
        "      \"category\" : {\n" +
        "        \"catProp1\" : \"catValue1\",\n" +
        "        \"catProp2\" : \"catValue2\"\n" +
        "      }\n" +
        "    }\n" +
        "  ]\n" +
        "}";

    assertEquals(expected, o);

    verify(uriInfo, resource);
  }
  
  @Test
  public void testSerializeResourcesAsArray() throws Exception {
    UriInfo uriInfo = createMock(UriInfo.class);
    Resource resource = createMock(Resource.class);
    //Resource resource2 = createMock(Resource.class);

    Result result = new ResultImpl(true);
    result.setResultStatus(new ResultStatus(ResultStatus.STATUS.OK));
    TreeNode<Resource> tree = result.getResultTree();
    //tree.setName("items");
    TreeNode<Resource> child = tree.addChild(resource, "resource1");
    child.setProperty("href", "this is an href");
    tree.addChild(resource, "resource2");
    //child.addChild(resource2, "sub-resource");

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

    replay(uriInfo, resource/*, resource2*/);

    //execute test
    Object o = new JsonSerializer().serialize(result).toString().replace("\r", "");
    String expected = "[\n" +
    "  {\n" +
    "    \"href\" : \"this is an href\",\n" +
    "    \"prop2\" : \"value2\",\n" +
    "    \"prop1\" : \"value1\",\n" +
    "    \"category\" : {\n" +
    "      \"catProp1\" : \"catValue1\",\n" +
    "      \"catProp2\" : \"catValue2\"\n" +
    "    }\n" +
    "  },\n" +
    "  {\n" +
    "    \"prop2\" : \"value2\",\n" +
    "    \"prop1\" : \"value1\",\n" +
    "    \"category\" : {\n" +
    "      \"catProp1\" : \"catValue1\",\n" +
    "      \"catProp2\" : \"catValue2\"\n" +
    "    }\n" +
    "  }\n" +
    "]";

    assertEquals(expected, o);

    verify(uriInfo, resource/*, resource2*/);
  }

  @Test
  public void testDeleteResultMetadata() throws Exception {
    Result result = new ResultImpl(true);
    result.setResultStatus(new ResultStatus(ResultStatus.STATUS.OK));
    DeleteResultMetadata metadata = new DeleteResultMetadata();
    metadata.addDeletedKey("key1");
    metadata.addException("key2", new AuthorizationException());
    result.setResultMetadata(metadata);

    String expected =
        "{\n" +
        "  \"deleteResult\" : [\n"+
        "    {\n" +
        "      \"deleted\" : {\n" +
        "        \"key\" : \"key1\"\n" +
        "      }\n" +
        "    },\n" +
        "    {\n" +
        "      \"error\" : {\n" +
        "        \"key\" : \"key2\",\n" +
        "        \"code\" : 403,\n" +
        "        \"message\" : \"org.apache.ambari.server.security.authorization.AuthorizationException:"+
                              " The authenticated user is not authorized to perform the requested operation\"\n" +
        "      }\n" +
        "    }\n" +
        "  ]\n" +
        "}";
    String  json = new JsonSerializer().serialize(result).toString().replace("\r", "");
    assertEquals(expected, json);
  }
  
}
