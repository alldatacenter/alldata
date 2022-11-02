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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.api.query.QueryInfo;
import org.apache.ambari.server.api.resources.ClusterResourceDefinition;
import org.apache.ambari.server.api.resources.ComponentResourceDefinition;
import org.apache.ambari.server.api.resources.HostResourceDefinition;
import org.apache.ambari.server.api.resources.ServiceResourceDefinition;
import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.api.services.ResultImpl;
import org.apache.ambari.server.api.util.TreeNode;
import org.apache.ambari.server.api.util.TreeNodeImpl;
import org.apache.ambari.server.controller.internal.HostComponentResourceProvider;
import org.apache.ambari.server.controller.internal.HostResourceProvider;
import org.apache.ambari.server.controller.internal.ResourceImpl;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.Schema;
import org.apache.ambari.server.controller.spi.SchemaFactory;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.junit.Test;

/**
 * MinimalRenderer unit tests.
 */
public class MinimalRendererTest {
  @Test
  public void testFinalizeProperties__instance_noProperties() {
    SchemaFactory schemaFactory = createNiceMock(SchemaFactory.class);
    Schema schema = createNiceMock(Schema.class);

    // schema expectations
    expect(schemaFactory.getSchema(Resource.Type.Component)).andReturn(schema).anyTimes();
    expect(schemaFactory.getSchema(Resource.Type.Alert)).andReturn(schema).anyTimes();
    expect(schemaFactory.getSchema(Resource.Type.Artifact)).andReturn(schema).anyTimes();
    expect(schema.getKeyPropertyId(Resource.Type.Component)).andReturn("ServiceComponentInfo/component_name").anyTimes();

    replay(schemaFactory, schema);

    QueryInfo rootQuery = new QueryInfo(new ServiceResourceDefinition(), new HashSet<>());
    TreeNode<QueryInfo> queryTree = new TreeNodeImpl<>(null, rootQuery, "Service");

    MinimalRenderer renderer = new MinimalRenderer();
    renderer.init(schemaFactory);
    TreeNode<Set<String>> propertyTree = renderer.finalizeProperties(queryTree, false);
    // no properties should have been added
    assertTrue(propertyTree.getObject().isEmpty());
    assertEquals(3, propertyTree.getChildren().size());

    TreeNode<Set<String>> componentNode = propertyTree.getChild("Component");
    assertEquals(1, componentNode.getObject().size());
    assertTrue(componentNode.getObject().contains("ServiceComponentInfo/component_name"));

    verify(schemaFactory, schema);
  }

  @Test
  public void testFinalizeProperties__instance_properties() {
    SchemaFactory schemaFactory = createNiceMock(SchemaFactory.class);
    Schema schema = createNiceMock(Schema.class);

    // schema expectations
    expect(schemaFactory.getSchema(Resource.Type.Service)).andReturn(schema).anyTimes();
    expect(schema.getKeyPropertyId(Resource.Type.Service)).andReturn("ServiceInfo/service_name").anyTimes();

    replay(schemaFactory, schema);

    HashSet<String> serviceProperties = new HashSet<>();
    serviceProperties.add("foo/bar");
    QueryInfo rootQuery = new QueryInfo(new ServiceResourceDefinition(), serviceProperties);
    TreeNode<QueryInfo> queryTree = new TreeNodeImpl<>(null, rootQuery, "Service");

    MinimalRenderer renderer = new MinimalRenderer();
    renderer.init(schemaFactory);
    TreeNode<Set<String>> propertyTree = renderer.finalizeProperties(queryTree, false);

    assertEquals(2, propertyTree.getObject().size());
    assertTrue(propertyTree.getObject().contains("ServiceInfo/service_name"));
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

    replay(schemaFactory, schema);

    HashSet<String> serviceProperties = new HashSet<>();
    QueryInfo rootQuery = new QueryInfo(new ServiceResourceDefinition(), serviceProperties);
    TreeNode<QueryInfo> queryTree = new TreeNodeImpl<>(null, rootQuery, "Service");

    MinimalRenderer renderer = new MinimalRenderer();
    renderer.init(schemaFactory);
    TreeNode<Set<String>> propertyTree = renderer.finalizeProperties(queryTree, true);

    assertEquals(1, propertyTree.getObject().size());
    assertTrue(propertyTree.getObject().contains("ServiceInfo/service_name"));

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

    replay(schemaFactory, schema);

    HashSet<String> serviceProperties = new HashSet<>();
    serviceProperties.add("foo/bar");
    QueryInfo rootQuery = new QueryInfo(new ServiceResourceDefinition(), serviceProperties);
    TreeNode<QueryInfo> queryTree = new TreeNodeImpl<>(null, rootQuery, "Service");

    MinimalRenderer renderer = new MinimalRenderer();
    renderer.init(schemaFactory);
    TreeNode<Set<String>> propertyTree = renderer.finalizeProperties(queryTree, true);

    assertEquals(2, propertyTree.getObject().size());
    assertTrue(propertyTree.getObject().contains("ServiceInfo/service_name"));
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
    expect(schemaFactory.getSchema(Resource.Type.Component)).andReturn(componentSchema).anyTimes();
    expect(componentSchema.getKeyPropertyId(Resource.Type.Component)).andReturn("ServiceComponentInfo/component_name").anyTimes();

    replay(schemaFactory, serviceSchema, componentSchema);

    HashSet<String> serviceProperties = new HashSet<>();
    QueryInfo rootQuery = new QueryInfo(new ServiceResourceDefinition(), serviceProperties);
    TreeNode<QueryInfo> queryTree = new TreeNodeImpl<>(null, rootQuery, "Service");
    queryTree.addChild(new QueryInfo(new ComponentResourceDefinition(), new HashSet<>()), "Component");

    MinimalRenderer renderer = new MinimalRenderer();
    renderer.init(schemaFactory);
    TreeNode<Set<String>> propertyTree = renderer.finalizeProperties(queryTree, false);

    assertEquals(1, propertyTree.getChildren().size());
    assertEquals(1, propertyTree.getObject().size());
    assertTrue(propertyTree.getObject().contains("ServiceInfo/service_name"));


    TreeNode<Set<String>> componentNode = propertyTree.getChild("Component");
    assertEquals(0, componentNode.getChildren().size());
    assertEquals(1, componentNode.getObject().size());
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
    expect(schemaFactory.getSchema(Resource.Type.Component)).andReturn(componentSchema).anyTimes();
    expect(componentSchema.getKeyPropertyId(Resource.Type.Component)).andReturn("ServiceComponentInfo/component_name").anyTimes();

    replay(schemaFactory, serviceSchema, componentSchema);

    HashSet<String> serviceProperties = new HashSet<>();
    serviceProperties.add("foo/bar");
    QueryInfo rootQuery = new QueryInfo(new ServiceResourceDefinition(), serviceProperties);
    TreeNode<QueryInfo> queryTree = new TreeNodeImpl<>(null, rootQuery, "Service");
    HashSet<String> componentProperties = new HashSet<>();
    componentProperties.add("goo/car");
    queryTree.addChild(new QueryInfo(new ComponentResourceDefinition(), componentProperties), "Component");

    MinimalRenderer renderer = new MinimalRenderer();
    renderer.init(schemaFactory);
    TreeNode<Set<String>> propertyTree = renderer.finalizeProperties(queryTree, false);

    assertEquals(1, propertyTree.getChildren().size());
    assertEquals(2, propertyTree.getObject().size());
    assertTrue(propertyTree.getObject().contains("ServiceInfo/service_name"));
    assertTrue(propertyTree.getObject().contains("foo/bar"));

    TreeNode<Set<String>> componentNode = propertyTree.getChild("Component");
    assertEquals(0, componentNode.getChildren().size());
    assertEquals(2, componentNode.getObject().size());
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
    expect(schemaFactory.getSchema(Resource.Type.Component)).andReturn(componentSchema).anyTimes();
    expect(componentSchema.getKeyPropertyId(Resource.Type.Component)).andReturn("ServiceComponentInfo/component_name").anyTimes();

    replay(schemaFactory, serviceSchema, componentSchema);

    HashSet<String> serviceProperties = new HashSet<>();
    QueryInfo rootQuery = new QueryInfo(new ServiceResourceDefinition(), serviceProperties);
    TreeNode<QueryInfo> queryTree = new TreeNodeImpl<>(null, rootQuery, "Service");
    queryTree.addChild(new QueryInfo(new ComponentResourceDefinition(), new HashSet<>()), "Component");

    MinimalRenderer renderer = new MinimalRenderer();
    renderer.init(schemaFactory);
    TreeNode<Set<String>> propertyTree = renderer.finalizeProperties(queryTree, true);

    assertEquals(1, propertyTree.getChildren().size());
    assertEquals(1, propertyTree.getObject().size());
    assertTrue(propertyTree.getObject().contains("ServiceInfo/service_name"));


    TreeNode<Set<String>> componentNode = propertyTree.getChild("Component");
    assertEquals(0, componentNode.getChildren().size());
    assertEquals(1, componentNode.getObject().size());
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
    expect(schemaFactory.getSchema(Resource.Type.Component)).andReturn(componentSchema).anyTimes();
    expect(componentSchema.getKeyPropertyId(Resource.Type.Component)).andReturn("ServiceComponentInfo/component_name").anyTimes();

    replay(schemaFactory, serviceSchema, componentSchema);

    HashSet<String> serviceProperties = new HashSet<>();
    serviceProperties.add("foo/bar");
    QueryInfo rootQuery = new QueryInfo(new ServiceResourceDefinition(), serviceProperties);
    TreeNode<QueryInfo> queryTree = new TreeNodeImpl<>(null, rootQuery, "Service");
    queryTree.addChild(new QueryInfo(new ComponentResourceDefinition(), new HashSet<>()), "Component");

    MinimalRenderer renderer = new MinimalRenderer();
    renderer.init(schemaFactory);
    TreeNode<Set<String>> propertyTree = renderer.finalizeProperties(queryTree, true);

    assertEquals(1, propertyTree.getChildren().size());
    assertEquals(2, propertyTree.getObject().size());
    assertTrue(propertyTree.getObject().contains("ServiceInfo/service_name"));
    assertTrue(propertyTree.getObject().contains("foo/bar"));


    TreeNode<Set<String>> componentNode = propertyTree.getChild("Component");
    assertEquals(0, componentNode.getChildren().size());
    assertEquals(1, componentNode.getObject().size());
    assertTrue(componentNode.getObject().contains("ServiceComponentInfo/component_name"));

    verify(schemaFactory, serviceSchema, componentSchema);
  }

  @Test
  public void testFinalizeResult() throws Exception {
    SchemaFactory schemaFactory = createNiceMock(SchemaFactory.class);
    Schema clusterSchema = createNiceMock(Schema.class);
    Schema hostSchema = createNiceMock(Schema.class);
    Schema hostComponentSchema = createNiceMock(Schema.class);

    // mock expectations
    expect(schemaFactory.getSchema(Resource.Type.Cluster)).andReturn(clusterSchema).anyTimes();
    expect(schemaFactory.getSchema(Resource.Type.Host)).andReturn(hostSchema).anyTimes();
    expect(schemaFactory.getSchema(Resource.Type.HostComponent)).andReturn(hostComponentSchema).anyTimes();

    expect(clusterSchema.getKeyPropertyId(Resource.Type.Cluster)).andReturn("Clusters/cluster_name").anyTimes();

    expect(hostSchema.getKeyPropertyId(Resource.Type.Cluster)).andReturn("Hosts/cluster_name").anyTimes();
    expect(hostSchema.getKeyPropertyId(Resource.Type.Host)).andReturn("Hosts/host_name").anyTimes();

    expect(hostComponentSchema.getKeyPropertyId(Resource.Type.Cluster)).andReturn("HostRoles/cluster_name").anyTimes();
    expect(hostComponentSchema.getKeyPropertyId(Resource.Type.Host)).andReturn("HostRoles/host_name").anyTimes();
    expect(hostComponentSchema.getKeyPropertyId(Resource.Type.HostComponent)).andReturn("HostRoles/component_name").anyTimes();

    replay(schemaFactory, clusterSchema, hostSchema, hostComponentSchema);

    Result result = new ResultImpl(true);
    createResultTree(result.getResultTree());

    MinimalRenderer renderer = new MinimalRenderer();
    renderer.init(schemaFactory);
    //call finalizeProperties so that renderer know which properties are requested by user
    renderer.finalizeProperties(createPropertyTree(), false);

    TreeNode<Resource> resultTree = renderer.finalizeResult(result).getResultTree();
    assertNull(resultTree.getStringProperty("isCollection"));
    assertEquals(1, resultTree.getChildren().size());

    TreeNode<Resource> clusterNode = resultTree.getChildren().iterator().next();
    Resource clusterResource = clusterNode.getObject();
    Map<String, Map<String, Object>> clusterProperties = clusterResource.getPropertiesMap();
    assertEquals(2, clusterProperties.size());

    assertEquals(3, clusterProperties.get("Clusters").size());
    assertEquals("testCluster", clusterProperties.get("Clusters").get("cluster_name"));
    assertEquals("HDP-1.3.3", clusterProperties.get("Clusters").get("version"));
    assertEquals("value1", clusterProperties.get("Clusters").get("prop1"));

    assertEquals(1, clusterProperties.get("").size());
    assertEquals("bar", clusterProperties.get("").get("foo"));

    TreeNode<Resource> hosts = clusterNode.getChildren().iterator().next();
    for (TreeNode<Resource> hostNode : hosts.getChildren()){
      Resource hostResource = hostNode.getObject();
      Map<String, Map<String, Object>> hostProperties = hostResource.getPropertiesMap();
      assertEquals(1, hostProperties.size());
      assertEquals(1, hostProperties.get("Hosts").size());
      assertTrue(hostProperties.get("Hosts").containsKey("host_name"));

      for (TreeNode<Resource> componentNode : hostNode.getChildren().iterator().next().getChildren()) {
        Resource componentResource = componentNode.getObject();
        Map<String, Map<String, Object>> componentProperties = componentResource.getPropertiesMap();
        assertEquals(1, componentProperties.size());
        assertEquals(1, componentProperties.get("HostRoles").size());
        assertTrue(componentProperties.get("HostRoles").containsKey("component_name"));
      }
    }
  }

  @Test
  public void testFinalizeResult_propsSetOnSubResource() throws Exception {
    SchemaFactory schemaFactory = createNiceMock(SchemaFactory.class);
    Schema clusterSchema = createNiceMock(Schema.class);
    Schema hostSchema = createNiceMock(Schema.class);
    Schema hostComponentSchema = createNiceMock(Schema.class);

    // mock expectations
    expect(schemaFactory.getSchema(Resource.Type.Cluster)).andReturn(clusterSchema).anyTimes();
    expect(schemaFactory.getSchema(Resource.Type.Host)).andReturn(hostSchema).anyTimes();
    expect(schemaFactory.getSchema(Resource.Type.HostComponent)).andReturn(hostComponentSchema).anyTimes();

    expect(clusterSchema.getKeyPropertyId(Resource.Type.Cluster)).andReturn("Clusters/cluster_name").anyTimes();

    expect(hostSchema.getKeyPropertyId(Resource.Type.Cluster)).andReturn("Hosts/cluster_name").anyTimes();
    expect(hostSchema.getKeyPropertyId(Resource.Type.Host)).andReturn("Hosts/host_name").anyTimes();

    expect(hostComponentSchema.getKeyPropertyId(Resource.Type.Cluster)).andReturn("HostRoles/cluster_name").anyTimes();
    expect(hostComponentSchema.getKeyPropertyId(Resource.Type.Host)).andReturn("HostRoles/host_name").anyTimes();
    expect(hostComponentSchema.getKeyPropertyId(Resource.Type.HostComponent)).andReturn("HostRoles/component_name").anyTimes();

    replay(schemaFactory, clusterSchema, hostSchema, hostComponentSchema);

    Result result = new ResultImpl(true);
    createResultTree(result.getResultTree());

    MinimalRenderer renderer = new MinimalRenderer();
    renderer.init(schemaFactory);
    //call finalizeProperties so that renderer know which properties are requested by user
    renderer.finalizeProperties(createPropertyTreeWithSubProps(), false);

    TreeNode<Resource> resultTree = renderer.finalizeResult(result).getResultTree();
    assertNull(resultTree.getStringProperty("isCollection"));
    assertEquals(1, resultTree.getChildren().size());

    TreeNode<Resource> clusterNode = resultTree.getChildren().iterator().next();
    Resource clusterResource = clusterNode.getObject();
    Map<String, Map<String, Object>> clusterProperties = clusterResource.getPropertiesMap();
    assertEquals(2, clusterProperties.size());

    assertEquals(3, clusterProperties.get("Clusters").size());
    assertEquals("testCluster", clusterProperties.get("Clusters").get("cluster_name"));
    assertEquals("HDP-1.3.3", clusterProperties.get("Clusters").get("version"));
    assertEquals("value1", clusterProperties.get("Clusters").get("prop1"));

    assertEquals(1, clusterProperties.get("").size());
    assertEquals("bar", clusterProperties.get("").get("foo"));

    TreeNode<Resource> hosts = clusterNode.getChildren().iterator().next();
    for (TreeNode<Resource> hostNode : hosts.getChildren()){
      Resource hostResource = hostNode.getObject();
      Map<String, Map<String, Object>> hostProperties = hostResource.getPropertiesMap();
      assertEquals(2, hostProperties.size());
      assertEquals(1, hostProperties.get("Hosts").size());
      assertTrue(hostProperties.get("Hosts").containsKey("host_name"));
      assertEquals(1, hostProperties.get("").size());
      assertEquals("bar", hostProperties.get("").get("foo"));

      for (TreeNode<Resource> componentNode : hostNode.getChildren().iterator().next().getChildren()) {
        Resource componentResource = componentNode.getObject();
        Map<String, Map<String, Object>> componentProperties = componentResource.getPropertiesMap();
        assertEquals(1, componentProperties.size());
        assertEquals(1, componentProperties.get("HostRoles").size());
        assertTrue(componentProperties.get("HostRoles").containsKey("component_name"));
      }
    }
  }

  //todo: test post processing to ensure href removal
  //todo: Need to do some refactoring to do this.
  //todo: BaseResourceDefinition.BaseHrefPostProcessor calls static ClusterControllerHelper.getClusterController().


  private TreeNode<QueryInfo> createPropertyTree() {
    TreeNode<QueryInfo> propertyTree = new TreeNodeImpl<>(null, new QueryInfo(
      new ClusterResourceDefinition(), new HashSet<>()), "Cluster");
    Set<String> clusterProperties = propertyTree.getObject().getProperties();
    clusterProperties.add("Clusters/cluster_name");
    clusterProperties.add("Clusters/version");
    clusterProperties.add("Clusters/prop1");
    clusterProperties.add("foo");

    return propertyTree;
  }

  private TreeNode<QueryInfo> createPropertyTreeWithSubProps() {
    TreeNode<QueryInfo> propertyTree = new TreeNodeImpl<>(null, new QueryInfo(
      new ClusterResourceDefinition(), new HashSet<>()), "Cluster");
    Set<String> clusterProperties = propertyTree.getObject().getProperties();
    clusterProperties.add("Clusters/cluster_name");
    clusterProperties.add("Clusters/version");
    clusterProperties.add("Clusters/prop1");
    clusterProperties.add("foo");

    propertyTree.addChild(new QueryInfo(new HostResourceDefinition(), new HashSet<>()), "Host");
    propertyTree.getChild("Host").getObject().getProperties().add("foo");

    return propertyTree;
  }

  private void createResultTree(TreeNode<Resource> resultTree) throws Exception{
    Resource clusterResource = new ResourceImpl(Resource.Type.Cluster);
    clusterResource.setProperty("Clusters/cluster_name", "testCluster");
    clusterResource.setProperty("Clusters/version", "HDP-1.3.3");
    clusterResource.setProperty("Clusters/prop1", "value1");
    clusterResource.setProperty("foo", "bar");

    TreeNode<Resource> clusterTree = resultTree.addChild(clusterResource, "Cluster:1");

    TreeNode<Resource> hostsTree = clusterTree.addChild(null, "hosts");
    hostsTree.setProperty("isCollection", "true");

    // host 1 : ambari host
    Resource hostResource = new ResourceImpl(Resource.Type.Host);

    PropertyHelper.setKeyPropertyIds(Resource.Type.Host,HostResourceProvider.keyPropertyIds);
    PropertyHelper.setKeyPropertyIds(Resource.Type.HostComponent, HostComponentResourceProvider.keyPropertyIds);
    hostResource.setProperty("Hosts/host_name", "testHost");
    hostResource.setProperty("Hosts/cluster_name", "testCluster");
    hostResource.setProperty("foo", "bar");
    TreeNode<Resource> hostTree = hostsTree.addChild(hostResource, "Host:1");

    TreeNode<Resource> hostComponentsTree = hostTree.addChild(null, "host_components");
    hostComponentsTree.setProperty("isCollection", "true");

    // host 1 components
    Resource nnComponentResource = new ResourceImpl(Resource.Type.HostComponent);
    nnComponentResource.setProperty("HostRoles/component_name", "NAMENODE");
    nnComponentResource.setProperty("HostRoles/host_name", "testHost");
    nnComponentResource.setProperty("HostRoles/cluster_name", "testCluster");

    Resource dnComponentResource = new ResourceImpl(Resource.Type.HostComponent);
    dnComponentResource.setProperty("HostRoles/component_name", "DATANODE");
    dnComponentResource.setProperty("HostRoles/host_name", "testHost");
    dnComponentResource.setProperty("HostRoles/cluster_name", "testCluster");

    Resource jtComponentResource = new ResourceImpl(Resource.Type.HostComponent);
    jtComponentResource.setProperty("HostRoles/component_name", "JOBTRACKER");
    jtComponentResource.setProperty("HostRoles/host_name", "testHost");
    jtComponentResource.setProperty("HostRoles/cluster_name", "testCluster");

    Resource ttComponentResource = new ResourceImpl(Resource.Type.HostComponent);
    ttComponentResource.setProperty("HostRoles/component_name", "TASKTRACKER");
    jtComponentResource.setProperty("HostRoles/host_name", "testHost");
    jtComponentResource.setProperty("HostRoles/cluster_name", "testCluster");

    hostComponentsTree.addChild(nnComponentResource, "HostComponent:1");
    hostComponentsTree.addChild(dnComponentResource, "HostComponent:2");
    hostComponentsTree.addChild(jtComponentResource, "HostComponent:3");
    hostComponentsTree.addChild(ttComponentResource, "HostComponent:4");

    // host 2
    Resource host2Resource = new ResourceImpl(Resource.Type.Host);
    host2Resource.setProperty("Hosts/host_name", "testHost2");
    host2Resource.setProperty("Hosts/cluster_name", "testCluster");
    host2Resource.setProperty("foo", "bar");
    TreeNode<Resource> host2Tree = hostsTree.addChild(host2Resource, "Host:2");

    TreeNode<Resource> host2ComponentsTree = host2Tree.addChild(null, "host_components");
    host2ComponentsTree.setProperty("isCollection", "true");

    // host 2 components
    host2ComponentsTree.addChild(dnComponentResource, "HostComponent:1");
    host2ComponentsTree.addChild(ttComponentResource, "HostComponent:2");

    // host 3 : same topology as host 2
    Resource host3Resource = new ResourceImpl(Resource.Type.Host);
    host3Resource.setProperty("Hosts/host_name", "testHost3");
    host3Resource.setProperty("Hosts/host_name", "testHost2");
    host3Resource.setProperty("foo", "bar");
    TreeNode<Resource> host3Tree = hostsTree.addChild(host3Resource, "Host:3");

    TreeNode<Resource> host3ComponentsTree = host3Tree.addChild(null, "host_components");
    host3ComponentsTree.setProperty("isCollection", "true");

    // host 3 components
    host3ComponentsTree.addChild(dnComponentResource, "HostComponent:1");
    host3ComponentsTree.addChild(ttComponentResource, "HostComponent:2");
  }
}
