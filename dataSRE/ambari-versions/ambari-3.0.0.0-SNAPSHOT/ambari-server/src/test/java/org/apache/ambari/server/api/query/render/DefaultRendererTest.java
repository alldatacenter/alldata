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

package org.apache.ambari.server.api.query.render;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.apache.ambari.server.api.query.QueryInfo;
import org.apache.ambari.server.api.resources.ComponentResourceDefinition;
import org.apache.ambari.server.api.resources.ServiceResourceDefinition;
import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.api.util.TreeNode;
import org.apache.ambari.server.api.util.TreeNodeImpl;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.Schema;
import org.apache.ambari.server.controller.spi.SchemaFactory;
import org.junit.Test;


/**
 * DefaultRenderer unit tests.
 */
public class DefaultRendererTest {

  @Test
  public void testFinalizeProperties__instance_noProperties() {
    SchemaFactory schemaFactory = createNiceMock(SchemaFactory.class);
    Schema schema = createNiceMock(Schema.class);

    // schema expectations
    expect(schemaFactory.getSchema(Resource.Type.Component)).andReturn(schema).anyTimes();
    expect(schemaFactory.getSchema(Resource.Type.Alert)).andReturn(schema).anyTimes();
    expect(schemaFactory.getSchema(Resource.Type.Artifact)).andReturn(schema).anyTimes();
    expect(schema.getKeyPropertyId(Resource.Type.Component)).andReturn("ServiceComponentInfo/component_name").anyTimes();
    expect(schema.getKeyPropertyId(Resource.Type.Service)).andReturn("ServiceComponentInfo/service_name").anyTimes();

    replay(schemaFactory, schema);

    QueryInfo rootQuery = new QueryInfo(new ServiceResourceDefinition(), new HashSet<>());
    TreeNode<QueryInfo> queryTree = new TreeNodeImpl<>(null, rootQuery, "Service");

    DefaultRenderer renderer = new DefaultRenderer();
    renderer.init(schemaFactory);
    TreeNode<Set<String>> propertyTree = renderer.finalizeProperties(queryTree, false);
    // no properties should have been added
    assertTrue(propertyTree.getObject().isEmpty());
    assertEquals(3, propertyTree.getChildren().size());

    TreeNode<Set<String>> componentNode = propertyTree.getChild("Component");
    assertEquals(2, componentNode.getObject().size());
    assertTrue(componentNode.getObject().contains("ServiceComponentInfo/component_name"));
    assertTrue(componentNode.getObject().contains("ServiceComponentInfo/service_name"));

    verify(schemaFactory, schema);
  }

  @Test
  public void testFinalizeProperties__instance_properties() {
    SchemaFactory schemaFactory = createNiceMock(SchemaFactory.class);
    Schema schema = createNiceMock(Schema.class);

    // schema expectations
    expect(schemaFactory.getSchema(Resource.Type.Service)).andReturn(schema).anyTimes();
    expect(schema.getKeyPropertyId(Resource.Type.Service)).andReturn("ServiceInfo/service_name").anyTimes();
    expect(schema.getKeyPropertyId(Resource.Type.Cluster)).andReturn("ServiceInfo/cluster_name").anyTimes();

    replay(schemaFactory, schema);

    HashSet<String> serviceProperties = new HashSet<>();
    serviceProperties.add("foo/bar");
    QueryInfo rootQuery = new QueryInfo(new ServiceResourceDefinition(), serviceProperties);
    TreeNode<QueryInfo> queryTree = new TreeNodeImpl<>(null, rootQuery, "Service");

    DefaultRenderer renderer = new DefaultRenderer();
    renderer.init(schemaFactory);
    TreeNode<Set<String>> propertyTree = renderer.finalizeProperties(queryTree, false);

    assertEquals(3, propertyTree.getObject().size());
    assertTrue(propertyTree.getObject().contains("ServiceInfo/service_name"));
    assertTrue(propertyTree.getObject().contains("ServiceInfo/cluster_name"));
    assertTrue(propertyTree.getObject().contains("foo/bar"));

    assertEquals(0, propertyTree.getChildren().size());

    verify(schemaFactory, schema);
  }

  @Test
  public void testFinalizeProperties__collection_noProperties() {
    SchemaFactory schemaFactory = createNiceMock(SchemaFactory.class);
    Schema schema = createNiceMock(Schema.class);

    // schema expectations
    expect(schemaFactory.getSchema(Resource.Type.Service)).andReturn(schema).anyTimes();
    expect(schema.getKeyPropertyId(Resource.Type.Service)).andReturn("ServiceInfo/service_name").anyTimes();
    expect(schema.getKeyPropertyId(Resource.Type.Cluster)).andReturn("ServiceInfo/cluster_name").anyTimes();

    replay(schemaFactory, schema);

    HashSet<String> serviceProperties = new HashSet<>();
    QueryInfo rootQuery = new QueryInfo(new ServiceResourceDefinition(), serviceProperties);
    TreeNode<QueryInfo> queryTree = new TreeNodeImpl<>(null, rootQuery, "Service");

    DefaultRenderer renderer = new DefaultRenderer();
    renderer.init(schemaFactory);
    TreeNode<Set<String>> propertyTree = renderer.finalizeProperties(queryTree, true);

    assertEquals(2, propertyTree.getObject().size());
    assertTrue(propertyTree.getObject().contains("ServiceInfo/service_name"));
    assertTrue(propertyTree.getObject().contains("ServiceInfo/cluster_name"));

    assertEquals(0, propertyTree.getChildren().size());

    verify(schemaFactory, schema);
  }

  @Test
  public void testFinalizeProperties__collection_properties() {
    SchemaFactory schemaFactory = createNiceMock(SchemaFactory.class);
    Schema schema = createNiceMock(Schema.class);

    // schema expectations
    expect(schemaFactory.getSchema(Resource.Type.Service)).andReturn(schema).anyTimes();
    expect(schema.getKeyPropertyId(Resource.Type.Service)).andReturn("ServiceInfo/service_name").anyTimes();
    expect(schema.getKeyPropertyId(Resource.Type.Cluster)).andReturn("ServiceInfo/cluster_name").anyTimes();

    replay(schemaFactory, schema);

    HashSet<String> serviceProperties = new HashSet<>();
    serviceProperties.add("foo/bar");
    QueryInfo rootQuery = new QueryInfo(new ServiceResourceDefinition(), serviceProperties);
    TreeNode<QueryInfo> queryTree = new TreeNodeImpl<>(null, rootQuery, "Service");

    DefaultRenderer renderer = new DefaultRenderer();
    renderer.init(schemaFactory);
    TreeNode<Set<String>> propertyTree = renderer.finalizeProperties(queryTree, true);

    assertEquals(3, propertyTree.getObject().size());
    assertTrue(propertyTree.getObject().contains("ServiceInfo/service_name"));
    assertTrue(propertyTree.getObject().contains("ServiceInfo/cluster_name"));
    assertTrue(propertyTree.getObject().contains("foo/bar"));

    assertEquals(0, propertyTree.getChildren().size());

    verify(schemaFactory, schema);
  }

  @Test
  public void testFinalizeProperties__instance_subResource_noProperties() {
    SchemaFactory schemaFactory = createNiceMock(SchemaFactory.class);
    Schema serviceSchema = createNiceMock(Schema.class);
    Schema componentSchema = createNiceMock(Schema.class);

    // schema expectations
    expect(schemaFactory.getSchema(Resource.Type.Service)).andReturn(serviceSchema).anyTimes();
    expect(serviceSchema.getKeyPropertyId(Resource.Type.Service)).andReturn("ServiceInfo/service_name").anyTimes();
    expect(serviceSchema.getKeyPropertyId(Resource.Type.Cluster)).andReturn("ServiceInfo/cluster_name").anyTimes();
    expect(schemaFactory.getSchema(Resource.Type.Component)).andReturn(componentSchema).anyTimes();
    expect(componentSchema.getKeyPropertyId(Resource.Type.Service)).andReturn("ServiceComponentInfo/service_name").anyTimes();
    expect(componentSchema.getKeyPropertyId(Resource.Type.Component)).andReturn("ServiceComponentInfo/component_name").anyTimes();

    replay(schemaFactory, serviceSchema, componentSchema);

    HashSet<String> serviceProperties = new HashSet<>();
    QueryInfo rootQuery = new QueryInfo(new ServiceResourceDefinition(), serviceProperties);
    TreeNode<QueryInfo> queryTree = new TreeNodeImpl<>(null, rootQuery, "Service");
    queryTree.addChild(new QueryInfo(new ComponentResourceDefinition(), new HashSet<>()), "Component");

    DefaultRenderer renderer = new DefaultRenderer();
    renderer.init(schemaFactory);
    TreeNode<Set<String>> propertyTree = renderer.finalizeProperties(queryTree, false);

    assertEquals(1, propertyTree.getChildren().size());
    assertEquals(2, propertyTree.getObject().size());
    assertTrue(propertyTree.getObject().contains("ServiceInfo/service_name"));
    assertTrue(propertyTree.getObject().contains("ServiceInfo/cluster_name"));


    TreeNode<Set<String>> componentNode = propertyTree.getChild("Component");
    assertEquals(0, componentNode.getChildren().size());
    assertEquals(2, componentNode.getObject().size());
    assertTrue(componentNode.getObject().contains("ServiceComponentInfo/service_name"));
    assertTrue(componentNode.getObject().contains("ServiceComponentInfo/component_name"));

    verify(schemaFactory, serviceSchema, componentSchema);
  }

  @Test
  public void testFinalizeProperties__instance_subResource_properties() {
    SchemaFactory schemaFactory = createNiceMock(SchemaFactory.class);
    Schema serviceSchema = createNiceMock(Schema.class);
    Schema componentSchema = createNiceMock(Schema.class);

    // schema expectations
    expect(schemaFactory.getSchema(Resource.Type.Service)).andReturn(serviceSchema).anyTimes();
    expect(serviceSchema.getKeyPropertyId(Resource.Type.Service)).andReturn("ServiceInfo/service_name").anyTimes();
    expect(serviceSchema.getKeyPropertyId(Resource.Type.Cluster)).andReturn("ServiceInfo/cluster_name").anyTimes();
    expect(schemaFactory.getSchema(Resource.Type.Component)).andReturn(componentSchema).anyTimes();
    expect(componentSchema.getKeyPropertyId(Resource.Type.Service)).andReturn("ServiceComponentInfo/service_name").anyTimes();
    expect(componentSchema.getKeyPropertyId(Resource.Type.Component)).andReturn("ServiceComponentInfo/component_name").anyTimes();

    replay(schemaFactory, serviceSchema, componentSchema);

    HashSet<String> serviceProperties = new HashSet<>();
    serviceProperties.add("foo/bar");
    QueryInfo rootQuery = new QueryInfo(new ServiceResourceDefinition(), serviceProperties);
    TreeNode<QueryInfo> queryTree = new TreeNodeImpl<>(null, rootQuery, "Service");
    HashSet<String> componentProperties = new HashSet<>();
    componentProperties.add("goo/car");
    queryTree.addChild(new QueryInfo(new ComponentResourceDefinition(), componentProperties), "Component");

    DefaultRenderer renderer = new DefaultRenderer();
    renderer.init(schemaFactory);
    TreeNode<Set<String>> propertyTree = renderer.finalizeProperties(queryTree, false);

    assertEquals(1, propertyTree.getChildren().size());
    assertEquals(3, propertyTree.getObject().size());
    assertTrue(propertyTree.getObject().contains("ServiceInfo/service_name"));
    assertTrue(propertyTree.getObject().contains("ServiceInfo/cluster_name"));
    assertTrue(propertyTree.getObject().contains("foo/bar"));


    TreeNode<Set<String>> componentNode = propertyTree.getChild("Component");
    assertEquals(0, componentNode.getChildren().size());
    assertEquals(3, componentNode.getObject().size());
    assertTrue(componentNode.getObject().contains("ServiceComponentInfo/service_name"));
    assertTrue(componentNode.getObject().contains("ServiceComponentInfo/component_name"));
    assertTrue(componentNode.getObject().contains("goo/car"));

    verify(schemaFactory, serviceSchema, componentSchema);
  }

  @Test
  public void testFinalizeProperties__collection_subResource_noProperties() {
    SchemaFactory schemaFactory = createNiceMock(SchemaFactory.class);
    Schema serviceSchema = createNiceMock(Schema.class);
    Schema componentSchema = createNiceMock(Schema.class);

    // schema expectations
    expect(schemaFactory.getSchema(Resource.Type.Service)).andReturn(serviceSchema).anyTimes();
    expect(serviceSchema.getKeyPropertyId(Resource.Type.Service)).andReturn("ServiceInfo/service_name").anyTimes();
    expect(serviceSchema.getKeyPropertyId(Resource.Type.Cluster)).andReturn("ServiceInfo/cluster_name").anyTimes();
    expect(schemaFactory.getSchema(Resource.Type.Component)).andReturn(componentSchema).anyTimes();
    expect(componentSchema.getKeyPropertyId(Resource.Type.Service)).andReturn("ServiceComponentInfo/service_name").anyTimes();
    expect(componentSchema.getKeyPropertyId(Resource.Type.Component)).andReturn("ServiceComponentInfo/component_name").anyTimes();

    replay(schemaFactory, serviceSchema, componentSchema);

    HashSet<String> serviceProperties = new HashSet<>();
    QueryInfo rootQuery = new QueryInfo(new ServiceResourceDefinition(), serviceProperties);
    TreeNode<QueryInfo> queryTree = new TreeNodeImpl<>(null, rootQuery, "Service");
    queryTree.addChild(new QueryInfo(new ComponentResourceDefinition(), new HashSet<>()), "Component");

    DefaultRenderer renderer = new DefaultRenderer();
    renderer.init(schemaFactory);
    TreeNode<Set<String>> propertyTree = renderer.finalizeProperties(queryTree, true);

    assertEquals(1, propertyTree.getChildren().size());
    assertEquals(2, propertyTree.getObject().size());
    assertTrue(propertyTree.getObject().contains("ServiceInfo/service_name"));
    assertTrue(propertyTree.getObject().contains("ServiceInfo/cluster_name"));


    TreeNode<Set<String>> componentNode = propertyTree.getChild("Component");
    assertEquals(0, componentNode.getChildren().size());
    assertEquals(2, componentNode.getObject().size());
    assertTrue(componentNode.getObject().contains("ServiceComponentInfo/service_name"));
    assertTrue(componentNode.getObject().contains("ServiceComponentInfo/component_name"));

    verify(schemaFactory, serviceSchema, componentSchema);
  }

  @Test
  public void testFinalizeProperties__collection_subResource_propertiesTopLevelOnly() {
    SchemaFactory schemaFactory = createNiceMock(SchemaFactory.class);
    Schema serviceSchema = createNiceMock(Schema.class);
    Schema componentSchema = createNiceMock(Schema.class);

    // schema expectations
    expect(schemaFactory.getSchema(Resource.Type.Service)).andReturn(serviceSchema).anyTimes();
    expect(serviceSchema.getKeyPropertyId(Resource.Type.Service)).andReturn("ServiceInfo/service_name").anyTimes();
    expect(serviceSchema.getKeyPropertyId(Resource.Type.Cluster)).andReturn("ServiceInfo/cluster_name").anyTimes();
    expect(schemaFactory.getSchema(Resource.Type.Component)).andReturn(componentSchema).anyTimes();
    expect(componentSchema.getKeyPropertyId(Resource.Type.Service)).andReturn("ServiceComponentInfo/service_name").anyTimes();
    expect(componentSchema.getKeyPropertyId(Resource.Type.Component)).andReturn("ServiceComponentInfo/component_name").anyTimes();

    replay(schemaFactory, serviceSchema, componentSchema);

    HashSet<String> serviceProperties = new HashSet<>();
    serviceProperties.add("foo/bar");
    QueryInfo rootQuery = new QueryInfo(new ServiceResourceDefinition(), serviceProperties);
    TreeNode<QueryInfo> queryTree = new TreeNodeImpl<>(null, rootQuery, "Service");
    queryTree.addChild(new QueryInfo(new ComponentResourceDefinition(), new HashSet<>()), "Component");

    DefaultRenderer renderer = new DefaultRenderer();
    renderer.init(schemaFactory);
    TreeNode<Set<String>> propertyTree = renderer.finalizeProperties(queryTree, true);

    assertEquals(1, propertyTree.getChildren().size());
    assertEquals(3, propertyTree.getObject().size());
    assertTrue(propertyTree.getObject().contains("ServiceInfo/service_name"));
    assertTrue(propertyTree.getObject().contains("ServiceInfo/cluster_name"));
    assertTrue(propertyTree.getObject().contains("foo/bar"));


    TreeNode<Set<String>> componentNode = propertyTree.getChild("Component");
    assertEquals(0, componentNode.getChildren().size());
    assertEquals(2, componentNode.getObject().size());
    assertTrue(componentNode.getObject().contains("ServiceComponentInfo/service_name"));
    assertTrue(componentNode.getObject().contains("ServiceComponentInfo/component_name"));

    verify(schemaFactory, serviceSchema, componentSchema);
  }


  @Test
  public void testFinalizeResult() {
    Result result = createNiceMock(Result.class);
    DefaultRenderer renderer = new DefaultRenderer();

    assertSame(result, renderer.finalizeResult(result));
  }

  @Test
  public void testRequiresInputDefault() throws Exception {
    Renderer defaultRenderer =
      new DefaultRenderer();

    assertTrue("Default renderer for cluster resources must require property provider input",
      defaultRenderer.requiresPropertyProviderInput());
  }
}
