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


package org.apache.ambari.server.api.query;


import static org.easymock.EasyMock.anyBoolean;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.ambari.server.api.query.render.DefaultRenderer;
import org.apache.ambari.server.api.query.render.Renderer;
import org.apache.ambari.server.api.resources.ClusterResourceDefinition;
import org.apache.ambari.server.api.resources.HostResourceDefinition;
import org.apache.ambari.server.api.resources.ResourceDefinition;
import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.api.resources.StackResourceDefinition;
import org.apache.ambari.server.api.resources.StackVersionResourceDefinition;
import org.apache.ambari.server.api.resources.SubResourceDefinition;
import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.api.util.TreeNode;
import org.apache.ambari.server.api.util.TreeNodeImpl;
import org.apache.ambari.server.controller.internal.ClusterControllerImpl;
import org.apache.ambari.server.controller.internal.ClusterControllerImplTest;
import org.apache.ambari.server.controller.internal.PageRequestImpl;
import org.apache.ambari.server.controller.spi.ClusterController;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.PageRequest;
import org.apache.ambari.server.controller.spi.PageResponse;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.QueryResponse;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.Schema;
import org.apache.ambari.server.controller.spi.SchemaFactory;
import org.apache.ambari.server.controller.spi.SortRequest;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Assert;
import org.junit.Test;


/**
 * QueryImpl unit tests.
 */
public class QueryImplTest {

  @Test
  public void testIsCollection__True() {
    ResourceDefinition resourceDefinition = createNiceMock(ResourceDefinition.class);

    Map<Resource.Type, String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.Cluster, "cluster");
    mapIds.put(Resource.Type.Service, null);

    // expectations
    expect(resourceDefinition.getType()).andReturn(Resource.Type.Service).anyTimes();
    expect(resourceDefinition.getSubResourceDefinitions()).andReturn(Collections.emptySet()).anyTimes();

    replay(resourceDefinition);

    //test
    ResourceInstance instance = new TestQuery(mapIds, resourceDefinition);
    assertTrue(instance.isCollectionResource());

    verify(resourceDefinition);
  }

  @Test
  public void testIsCollection__False() {
    ResourceDefinition resourceDefinition = createNiceMock(ResourceDefinition.class);

    Map<Resource.Type, String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.Cluster, "cluster");
    mapIds.put(Resource.Type.Service, "service");

    // expectations
    expect(resourceDefinition.getType()).andReturn(Resource.Type.Service).anyTimes();
    expect(resourceDefinition.getSubResourceDefinitions()).andReturn(Collections.emptySet()).anyTimes();

    replay(resourceDefinition);

    //test
    ResourceInstance instance = new TestQuery(mapIds, resourceDefinition);
    assertFalse(instance.isCollectionResource());

    verify(resourceDefinition);
  }

  @Test
  public void testExecute__Cluster_instance_noSpecifiedProps() throws Exception {
    ResourceDefinition resourceDefinition = createNiceMock(ResourceDefinition.class);

    Map<Resource.Type, String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.Cluster, "cluster");

    // expectations
    expect(resourceDefinition.getType()).andReturn(Resource.Type.Cluster).anyTimes();
    Set<SubResourceDefinition> setChildren = new HashSet<>();
    setChildren.add(new SubResourceDefinition(Resource.Type.Host));

    expect(resourceDefinition.getSubResourceDefinitions()).andReturn(setChildren).anyTimes();

    replay(resourceDefinition);

    //test
    QueryImpl instance = new TestQuery(mapIds, resourceDefinition);
    Result result = instance.execute();

    verify(resourceDefinition);

    TreeNode<Resource> tree = result.getResultTree();

    Assert.assertEquals(1, tree.getChildren().size());
    TreeNode<Resource> clusterNode = tree.getChild("Cluster:1");
    Assert.assertEquals("Cluster:1", clusterNode.getName());
    Assert.assertEquals(Resource.Type.Cluster, clusterNode.getObject().getType());
    Assert.assertEquals(1, clusterNode.getChildren().size());
    TreeNode<Resource> hostNode = clusterNode.getChild("hosts");
    Assert.assertEquals(4, hostNode.getChildren().size());
  }

  @Test
  public void testExecute__Stack_instance_noSpecifiedProps() throws Exception {
    ResourceDefinition resourceDefinition = new StackResourceDefinition();

    Map<Resource.Type, String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.Stack, "HDP");

    //test
    QueryImpl instance = new TestQuery(mapIds, resourceDefinition);

    Result result = instance.execute();


    TreeNode<Resource> tree = result.getResultTree();

    Assert.assertEquals(1, tree.getChildren().size());
    TreeNode<Resource> stackNode = tree.getChild("Stack:1");
    Assert.assertEquals("Stack:1", stackNode.getName());
    Assert.assertEquals(Resource.Type.Stack, stackNode.getObject().getType());
    Assert.assertEquals(1, stackNode.getChildren().size());
    TreeNode<Resource> versionsNode = stackNode.getChild("versions");
    Assert.assertEquals(3, versionsNode.getChildren().size());
  }

  @Test
  public void testGetJoinedResourceProperties() throws Exception {
    ResourceDefinition resourceDefinition = new StackResourceDefinition();

    Map<Resource.Type, String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.Stack, "HDP");

    //test
    QueryImpl instance = new TestQuery(mapIds, resourceDefinition);

    instance.addProperty("versions/*", null);
    instance.addProperty("versions/operating_systems/*", null);
    instance.addProperty("versions/operating_systems/repositories/*", null);

    instance.execute();

    Set<String> propertyIds = new HashSet<>();
    propertyIds.add("versions/operating_systems/repositories/Repositories/repo_id");
    propertyIds.add("versions/operating_systems/OperatingSystems/os_type");

    Map<Resource, Set<Map<String, Object>>> resourcePropertiesMap = instance.getJoinedResourceProperties(propertyIds, null, null);

    Set<Map<String, Object>> propertyMaps = null;

    for (Map.Entry<Resource, Set<Map<String, Object>>> resourceSetEntry : resourcePropertiesMap.entrySet()) {
      Assert.assertEquals(Resource.Type.Stack, resourceSetEntry.getKey().getType());
      propertyMaps = resourceSetEntry.getValue();
    }
    if (propertyMaps == null) {
      fail("No property maps found!");
    }

    Assert.assertEquals(6, propertyMaps.size());

    for (Map<String, Object> map : propertyMaps) {
      Assert.assertEquals(2, map.size());
      Assert.assertTrue(map.containsKey("versions/operating_systems/OperatingSystems/os_type"));
      Assert.assertTrue(map.containsKey("versions/operating_systems/repositories/Repositories/repo_id"));
    }
  }

  @Test
  public void testExecute_subResourcePredicate() throws Exception {
    ResourceDefinition resourceDefinition = new StackResourceDefinition();

    Map<Resource.Type, String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.Stack, "HDP");

    //test
    QueryImpl instance = new TestQuery(mapIds, resourceDefinition);

    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate = pb.property("versions/operating_systems/OperatingSystems/os_type").equals("centos5").toPredicate();

    instance.setUserPredicate(predicate);

    Result result = instance.execute();

    TreeNode<Resource> tree = result.getResultTree();

    Assert.assertEquals(1, tree.getChildren().size());
    TreeNode<Resource> stackNode = tree.getChild("Stack:1");
    Assert.assertEquals("Stack:1", stackNode.getName());
    Assert.assertEquals(Resource.Type.Stack, stackNode.getObject().getType());
    Assert.assertEquals(1, stackNode.getChildren().size());
    TreeNode<Resource> versionsNode = stackNode.getChild("versions");
    Assert.assertEquals(3, versionsNode.getChildren().size());

    TreeNode<Resource> versionNode = versionsNode.getChild("StackVersion:1");
    Assert.assertEquals("StackVersion:1", versionNode.getName());
    Assert.assertEquals(Resource.Type.StackVersion, versionNode.getObject().getType());

    Assert.assertEquals(1, versionNode.getChildren().size());
    TreeNode<Resource> opSystemsNode = versionNode.getChild("operating_systems");
    Assert.assertEquals(1, opSystemsNode.getChildren().size());

    TreeNode<Resource> opSystemNode = opSystemsNode.getChild("OperatingSystem:1");
    Assert.assertEquals("OperatingSystem:1", opSystemNode.getName());
    Resource osResource = opSystemNode.getObject();
    Assert.assertEquals(Resource.Type.OperatingSystem, opSystemNode.getObject().getType());

    Assert.assertEquals("centos5", osResource.getPropertyValue("OperatingSystems/os_type"));
  }

  @Test
  public void testExecute_NoSuchResourceException() throws Exception {
    ResourceDefinition resourceDefinition = new ClusterResourceDefinition();

    Map<Resource.Type, String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.Cluster, "c1");

    ClusterController clusterController = createNiceMock(ClusterController.class);
    QueryResponse queryResponse = createNiceMock(QueryResponse.class);
    Schema schema = createNiceMock(Schema.class);
    Renderer renderer = createNiceMock(Renderer.class);

    expect(clusterController.getSchema(Resource.Type.Cluster)).andReturn(schema).anyTimes();

    expect(clusterController.getResources(eq(Resource.Type.Cluster),
        anyObject(org.apache.ambari.server.controller.spi.Request.class), anyObject(Predicate.class))).
        andReturn(queryResponse);

    // Always return an empty set of resources.
    expect(queryResponse.getResources()).andReturn(Collections.emptySet()).anyTimes();

    expect(schema.getKeyPropertyId(Resource.Type.Cluster)).andReturn("Clusters/cluster_name").anyTimes();

    TreeNode<Set<String>> treeNode = new TreeNodeImpl<>(null, Collections.<String>emptySet(), null);
    expect(renderer.finalizeProperties(EasyMock.anyObject(), anyBoolean())).andReturn(treeNode).anyTimes();

    replay(clusterController, queryResponse, schema, renderer);

    //test
    QueryImpl query = new TestQuery(mapIds, resourceDefinition, clusterController);
    query.setRenderer(renderer);

    try {
      query.execute();
      fail("Expected NoSuchResourceException!");

    } catch (NoSuchResourceException e) {
      //expected
    }
    verify(clusterController, queryResponse, schema, renderer);
  }

  @Test
  public void testExecute_SubResourcePropertyPredicate() throws Exception {
    ResourceDefinition resourceDefinition = new ClusterResourceDefinition();

    Map<Resource.Type, String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.Cluster, "c1");
    mapIds.put(Resource.Type.Host, "h1");

    ClusterController clusterController = createNiceMock(ClusterController.class);
    QueryResponse clusterResponse = createNiceMock(QueryResponse.class);
    QueryResponse hostResponse = createNiceMock(QueryResponse.class);
    Schema clusterSchema = createNiceMock("ClusterSchema", Schema.class);
    Schema hostSchema = createNiceMock("HostSchema", Schema.class);
    Renderer renderer = createNiceMock(Renderer.class);
    Resource clusterResource = createMock("ClusterResource", Resource.class);
    Resource hostResource = createMock("HostResource", Resource.class);
    Set<Resource> clusterResources = Collections.singleton(clusterResource);
    Set<Resource> hostResources = Collections.singleton(hostResource);
    Iterable<Resource> iterable = createNiceMock(Iterable.class);
    Iterator<Resource> iterator = createNiceMock(Iterator.class);

    expect(clusterController.getSchema(Resource.Type.Cluster)).andReturn(clusterSchema).anyTimes();
    expect(clusterController.getSchema(Resource.Type.Host)).andReturn(hostSchema).anyTimes();

    expect(clusterController.getResources(eq(Resource.Type.Cluster),
      anyObject(org.apache.ambari.server.controller.spi.Request.class), anyObject(Predicate.class))).
      andReturn(clusterResponse);

    // Expect this call with a predicate passed down
    expect(clusterController.getResources(eq(Resource.Type.Host),
      anyObject(org.apache.ambari.server.controller.spi.Request.class), anyObject(Predicate.class))).
      andReturn(hostResponse);

    expect(iterable.iterator()).andReturn(iterator).anyTimes();
    expect(iterator.hasNext()).andReturn(false).anyTimes();

    expect(clusterResponse.getResources()).andReturn(clusterResources).anyTimes();
    expect(hostResponse.getResources()).andReturn(hostResources).anyTimes();
    expect(clusterResource.getType()).andReturn(Resource.Type.Cluster).anyTimes();
    expect(hostResource.getType()).andReturn(Resource.Type.Host).anyTimes();

    expect(clusterSchema.getKeyPropertyId(Resource.Type.Cluster)).andReturn("Clusters/cluster_name").anyTimes();
    expect(clusterSchema.getKeyTypes()).andReturn(Collections.singleton(Resource.Type.Cluster)).anyTimes();
    expect(hostSchema.getKeyPropertyId(Resource.Type.Cluster)).andReturn(null).anyTimes();
    expect(hostSchema.getKeyPropertyId(Resource.Type.Host)).andReturn("Hosts/host_name").anyTimes();
    expect(hostSchema.getKeyTypes()).andReturn(Collections.singleton(Resource.Type.Host)).anyTimes();

    expect(clusterResource.getPropertyValue("Clusters/cluster_name")).andReturn("c1").anyTimes();

    TreeNode<Set<String>> treeNode = new TreeNodeImpl<>(null, Collections.<String>emptySet(), null);
    expect(renderer.finalizeProperties(EasyMock.anyObject(), anyBoolean())).andReturn(treeNode).anyTimes();

    expect(clusterController.getIterable(eq(Resource.Type.Cluster), anyObject(QueryResponse.class),
      anyObject(org.apache.ambari.server.controller.spi.Request.class), anyObject(Predicate.class),
      anyObject(PageRequest.class), anyObject(SortRequest.class))).andReturn(iterable).anyTimes();

    replay(clusterController, clusterResponse, clusterSchema, renderer,
      hostSchema, clusterResource, hostResource, hostResponse, iterable, iterator);

    //test
    QueryImpl query = new TestQuery(mapIds, resourceDefinition, clusterController);
    query.setUserPredicate(new PredicateBuilder()
      .property("hosts/Hosts/host_name").equals("h1").and()
      .property("metrics/boottime").equals("value").toPredicate());
    query.setRenderer(renderer);

    query.execute();

    verify(clusterController, clusterResponse, clusterSchema, renderer,
      hostSchema, clusterResource, hostResource, hostResponse, iterable, iterator);
  }

  @Test
  public void testExecute_collection_NoSuchResourceException() throws Exception {
    ResourceDefinition resourceDefinition = new ClusterResourceDefinition();

    ClusterController clusterController = createNiceMock(ClusterController.class);
    QueryResponse queryResponse = createNiceMock(QueryResponse.class);
    Schema schema = createNiceMock(Schema.class);
    Renderer renderer = createNiceMock(Renderer.class);
    Iterable<Resource> iterable = createNiceMock(Iterable.class);
    Iterator<Resource> iterator = createNiceMock(Iterator.class);

    expect(clusterController.getSchema(Resource.Type.Cluster)).andReturn(schema).anyTimes();

    expect(clusterController.getResources(eq(Resource.Type.Cluster),
        anyObject(org.apache.ambari.server.controller.spi.Request.class), anyObject(Predicate.class))).
        andReturn(queryResponse);

    expect(clusterController.getIterable(eq(Resource.Type.Cluster), anyObject(QueryResponse.class),
        anyObject(org.apache.ambari.server.controller.spi.Request.class), anyObject(Predicate.class),
        anyObject(PageRequest.class), anyObject(SortRequest.class))).andReturn(iterable).anyTimes();

    expect(iterable.iterator()).andReturn(iterator).anyTimes();

    expect(iterator.hasNext()).andReturn(false).anyTimes();

    // Always return an empty set of resources.
    expect(queryResponse.getResources()).andReturn(Collections.emptySet()).anyTimes();

    expect(schema.getKeyPropertyId(Resource.Type.Cluster)).andReturn("Clusters/cluster_name").anyTimes();

    TreeNode<Set<String>> treeNode = new TreeNodeImpl<>(null, Collections.<String>emptySet(), null);
    expect(renderer.finalizeProperties(EasyMock.anyObject(), anyBoolean())).andReturn(treeNode).anyTimes();

    Capture<Result> resultCapture = EasyMock.newCapture();

    expect(renderer.finalizeResult(capture(resultCapture))).andReturn(null);

    replay(clusterController, queryResponse, schema, renderer, iterable, iterator);

    //test
    QueryImpl query = new TestQuery(new HashMap<>(), resourceDefinition, clusterController);
    query.setRenderer(renderer);

    query.execute();

    TreeNode<Resource> tree = resultCapture.getValue().getResultTree();
    Assert.assertEquals(0, tree.getChildren().size());

    verify(clusterController, queryResponse, schema, renderer, iterable, iterator);
  }

  @Test
  public void testExecute__Stack_instance_specifiedSubResources() throws Exception {
    ResourceDefinition resourceDefinition = new StackResourceDefinition();

    Map<Resource.Type, String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.Stack, "HDP");

    //test
    QueryImpl instance = new TestQuery(mapIds, resourceDefinition);

    instance.addProperty("versions/*", null);
    instance.addProperty("versions/operating_systems/*", null);
    instance.addProperty("versions/operating_systems/repositories/*", null);

    Result result = instance.execute();

    TreeNode<Resource> tree = result.getResultTree();

    Assert.assertEquals(1, tree.getChildren().size());
    TreeNode<Resource> stackNode = tree.getChild("Stack:1");
    Assert.assertEquals("Stack:1", stackNode.getName());
    Assert.assertEquals(Resource.Type.Stack, stackNode.getObject().getType());
    Assert.assertEquals(1, stackNode.getChildren().size());
    TreeNode<Resource> versionsNode = stackNode.getChild("versions");
    Assert.assertEquals(3, versionsNode.getChildren().size());

    TreeNode<Resource> versionNode = versionsNode.getChild("StackVersion:1");
    Assert.assertEquals("StackVersion:1", versionNode.getName());
    Assert.assertEquals(Resource.Type.StackVersion, versionNode.getObject().getType());

    Assert.assertEquals(7, versionNode.getChildren().size());

    TreeNode<Resource> opSystemsNode = versionNode.getChild("operating_systems");
    Assert.assertEquals(3, opSystemsNode.getChildren().size());

    TreeNode<Resource> opSystemNode = opSystemsNode.getChild("OperatingSystem:1");
    Assert.assertEquals("OperatingSystem:1", opSystemNode.getName());
    Assert.assertEquals(Resource.Type.OperatingSystem, opSystemNode.getObject().getType());

    Assert.assertEquals(1, opSystemNode.getChildren().size());
    TreeNode<Resource> repositoriesNode = opSystemNode.getChild("repositories");
    Assert.assertEquals(2, repositoriesNode.getChildren().size());

    TreeNode<Resource> repositoryNode = repositoriesNode.getChild("Repository:1");
    Assert.assertEquals("Repository:1", repositoryNode.getName());
    Resource repositoryResource = repositoryNode.getObject();
    Assert.assertEquals(Resource.Type.Repository, repositoryResource.getType());

    Assert.assertEquals("repo1", repositoryResource.getPropertyValue("Repositories/repo_id"));
    Assert.assertEquals("centos5", repositoryResource.getPropertyValue("Repositories/os_type"));
    Assert.assertEquals("1.2.1", repositoryResource.getPropertyValue("Repositories/stack_version"));
    Assert.assertEquals("HDP", repositoryResource.getPropertyValue("Repositories/stack_name"));

    TreeNode<Resource> artifactsNode = versionNode.getChild("artifacts");
    Assert.assertEquals(1, artifactsNode.getChildren().size());

    TreeNode<Resource> artifactNode = artifactsNode.getChild("StackArtifact:1");
    Assert.assertEquals("StackArtifact:1", artifactNode.getName());
    Assert.assertEquals(Resource.Type.StackArtifact, artifactNode.getObject().getType());
  }

  @Test
  public void testExecute_StackVersionPageResourcePredicate()
    throws NoSuchParentResourceException, UnsupportedPropertyException,
    NoSuchResourceException, SystemException {

    ResourceDefinition resourceDefinition = new StackVersionResourceDefinition();

    Map<Resource.Type, String> mapIds = new HashMap<>();

    //test
    QueryImpl instance = new TestQuery(mapIds, resourceDefinition);

    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate = pb.property("Versions/stack_version").equals("1.2.1").or()
        .property("Versions/stack_version").equals("1.2.2").toPredicate();

    instance.setUserPredicate(predicate);
    //First page
    instance.setPageRequest(new PageRequestImpl(PageRequest.StartingPoint.Beginning, 1, 0, null, null));

    Result result = instance.execute();

    TreeNode<Resource> tree = result.getResultTree();

    Assert.assertEquals(1, tree.getChildren().size());
    TreeNode<Resource> stackVersionNode = tree.getChild("StackVersion:1");
    Assert.assertEquals("StackVersion:1", stackVersionNode.getName());
    Assert.assertEquals(Resource.Type.StackVersion, stackVersionNode.getObject().getType());
    Assert.assertEquals("1.2.1", stackVersionNode.getObject().getPropertyValue("Versions/stack_version"));

    //Second page
    instance.setPageRequest(new PageRequestImpl(PageRequest.StartingPoint.OffsetStart, 1, 1, null, null));
    result = instance.execute();
    tree = result.getResultTree();

    Assert.assertEquals(1, tree.getChildren().size());
    stackVersionNode = tree.getChild("StackVersion:1");
    Assert.assertEquals("StackVersion:1", stackVersionNode.getName());
    Assert.assertEquals(Resource.Type.StackVersion, stackVersionNode.getObject().getType());
    Assert.assertEquals("1.2.2", stackVersionNode.getObject().getPropertyValue("Versions/stack_version"));

  }

  @Test
  public void testExecute_StackVersionPageSubResourcePredicate()
      throws NoSuchParentResourceException, UnsupportedPropertyException,
    NoSuchResourceException, SystemException {

    ResourceDefinition resourceDefinition = new StackVersionResourceDefinition();

    Map<Resource.Type, String> mapIds = new HashMap<>();

    QueryImpl instance = new TestQuery(mapIds, resourceDefinition);
    instance.addProperty("operating_systems/*", null);

    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate = pb.property("operating_systems/OperatingSystems/os_type").equals("centos5").toPredicate();

    instance.setUserPredicate(predicate);
    instance.setPageRequest(new PageRequestImpl(PageRequest.StartingPoint.Beginning, 1, 0, null, null));

    Result result = instance.execute();

    TreeNode<Resource> tree = result.getResultTree();
    Assert.assertEquals(1, tree.getChildren().size());
    TreeNode<Resource> stackVersionNode = tree.getChild("StackVersion:1");
    Assert.assertEquals("StackVersion:1", stackVersionNode.getName());

    Assert.assertEquals(Resource.Type.StackVersion, stackVersionNode.getObject().getType());
    Assert.assertEquals("1.2.1", stackVersionNode.getObject().getPropertyValue("Versions/stack_version"));

    QueryImpl instance2 = new TestQuery(mapIds, resourceDefinition);
    instance2.addProperty("operating_systems/*", null);
    instance2.setUserPredicate(predicate);
    instance2.setPageRequest(new PageRequestImpl(PageRequest.StartingPoint.OffsetStart, 1, 1, null, null));

    Result result2 = instance2.execute();

    TreeNode<Resource> tree2 = result2.getResultTree();
    Assert.assertEquals(1, tree2.getChildren().size());
    TreeNode<Resource> stackVersionNode2 = tree2.getChild("StackVersion:1");
    Assert.assertEquals("StackVersion:1", stackVersionNode2.getName());

    Assert.assertEquals(Resource.Type.StackVersion, stackVersionNode2.getObject().getType());
    Assert.assertEquals("1.2.2", stackVersionNode2.getObject().getPropertyValue("Versions/stack_version"));

    QueryImpl instance3 = new TestQuery(mapIds, resourceDefinition);

    instance3.addProperty("operating_systems/*", null);

    instance3.setUserPredicate(predicate);
    //page_size = 2, offset = 1
    instance3.setPageRequest(new PageRequestImpl(PageRequest.StartingPoint.OffsetStart, 2, 1, null, null));

    Result result3 = instance3.execute();

    TreeNode<Resource> tree3 = result3.getResultTree();
    Assert.assertEquals(2, tree3.getChildren().size());
    TreeNode<Resource> stackVersionNode3 = tree3.getChild("StackVersion:1");
    Assert.assertEquals("StackVersion:1", stackVersionNode3.getName());

    Assert.assertEquals(Resource.Type.StackVersion, stackVersionNode3.getObject().getType());
    Assert.assertEquals("1.2.2", stackVersionNode3.getObject().getPropertyValue("Versions/stack_version"));

    stackVersionNode3 = tree3.getChild("StackVersion:2");
    Assert.assertEquals("StackVersion:2", stackVersionNode3.getName());

    Assert.assertEquals(Resource.Type.StackVersion, stackVersionNode3.getObject().getType());
    Assert.assertEquals("2.0.1", stackVersionNode3.getObject().getPropertyValue("Versions/stack_version"));

  }

  @Test
  public void testExecute__Host_collection_noSpecifiedProps() throws Exception {
    ResourceDefinition resourceDefinition = createNiceMock(ResourceDefinition.class);

    Map<Resource.Type, String> mapIds = new HashMap<>();

    // expectations
    expect(resourceDefinition.getType()).andReturn(Resource.Type.Host).anyTimes();
    Set<SubResourceDefinition> setChildren = new HashSet<>();

    expect(resourceDefinition.getSubResourceDefinitions()).andReturn(setChildren).anyTimes();

    replay(resourceDefinition);

    //test
    QueryImpl instance = new TestQuery(mapIds, resourceDefinition);

    Result result = instance.execute();

    verify(resourceDefinition);

    TreeNode<Resource> tree = result.getResultTree();

    Assert.assertEquals(4, tree.getChildren().size());
    TreeNode<Resource> hostNode = tree.getChild("Host:1");
    Assert.assertEquals("Host:1", hostNode.getName());
    Assert.assertEquals(Resource.Type.Host, hostNode.getObject().getType());

    hostNode = tree.getChild("Host:2");
    Assert.assertEquals("Host:2", hostNode.getName());
    Assert.assertEquals(Resource.Type.Host, hostNode.getObject().getType());

    hostNode = tree.getChild("Host:3");
    Assert.assertEquals("Host:3", hostNode.getName());
    Assert.assertEquals(Resource.Type.Host, hostNode.getObject().getType());

    hostNode = tree.getChild("Host:4");
    Assert.assertEquals("Host:4", hostNode.getName());
    Assert.assertEquals(Resource.Type.Host, hostNode.getObject().getType());
  }

  @Test
  public void testExecute__Host_collection_AlertsSummary() throws Exception {
    ResourceDefinition resourceDefinition = new HostResourceDefinition();
    Map<Resource.Type, String> mapIds = new HashMap<>();
    final AtomicInteger pageCallCount = new AtomicInteger(0);
    ClusterControllerImpl clusterControllerImpl = new ClusterControllerImpl(new ClusterControllerImplTest.TestProviderModule()) {
      @Override
      public PageResponse getPage(Resource.Type type, QueryResponse queryResponse, Request request, Predicate predicate, PageRequest pageRequest,
          SortRequest sortRequest) throws UnsupportedPropertyException, SystemException, NoSuchResourceException, NoSuchParentResourceException {
        pageCallCount.incrementAndGet();
        return super.getPage(type, queryResponse, request, predicate, pageRequest, sortRequest);
      }
    };
    QueryImpl instance = new TestQuery(mapIds, resourceDefinition, clusterControllerImpl);

    //test 1: No predicate or paging request
    pageCallCount.set(0);
    Result result = instance.execute();
    TreeNode<Resource> tree = result.getResultTree();
    Assert.assertEquals(4, tree.getChildren().size());
    Assert.assertEquals(1, pageCallCount.get());

    //test 2: Predicate = (alerts_summary/CRITICAL > 0)
    pageCallCount.set(0);
    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate = pb.property("alerts_summary/CRITICAL").greaterThan(0).toPredicate();
    instance.setUserPredicate(predicate);
    result = instance.execute();
    tree = result.getResultTree();
    Assert.assertEquals(2, tree.getChildren().size());
    Assert.assertEquals(2, pageCallCount.get());
    TreeNode<Resource> hostNode = tree.getChild("Host:1");
    Assert.assertEquals("Host:1", hostNode.getName());
    Assert.assertEquals("host:0", hostNode.getObject().getPropertyValue(PropertyHelper.getPropertyId("Hosts", "host_name")));
    Assert.assertEquals(Resource.Type.Host, hostNode.getObject().getType());
    Assert.assertEquals("1", hostNode.getObject().getPropertyValue(PropertyHelper.getPropertyId("alerts_summary", "CRITICAL")));
    hostNode = tree.getChild("Host:2");
    Assert.assertEquals("Host:2", hostNode.getName());
    Assert.assertEquals("host:2", hostNode.getObject().getPropertyValue(PropertyHelper.getPropertyId("Hosts", "host_name")));
    Assert.assertEquals(Resource.Type.Host, hostNode.getObject().getType());
    Assert.assertEquals("1", hostNode.getObject().getPropertyValue(PropertyHelper.getPropertyId("alerts_summary", "CRITICAL")));

    // test 3: Predicate = (alerts_summary/WARNING > 0) AND Page = (size=1)
    pageCallCount.set(0);
    pb = new PredicateBuilder();
    predicate = pb.property("alerts_summary/WARNING").greaterThan(0).toPredicate();
    instance.setUserPredicate(predicate);
    instance.setPageRequest(new PageRequestImpl(PageRequest.StartingPoint.Beginning, 1, 0, null, null));
    result = instance.execute();
    tree = result.getResultTree();
    Assert.assertEquals(1, tree.getChildren().size());
    Assert.assertEquals(2, pageCallCount.get());
    hostNode = tree.getChild("Host:1");
    Assert.assertEquals("Host:1", hostNode.getName());
    Assert.assertEquals("host:1", hostNode.getObject().getPropertyValue(PropertyHelper.getPropertyId("Hosts", "host_name")));
    Assert.assertEquals(Resource.Type.Host, hostNode.getObject().getType());
    Assert.assertEquals("1", hostNode.getObject().getPropertyValue(PropertyHelper.getPropertyId("alerts_summary", "WARNING")));

    // test 4: SubResource Predicate = (alerts_summary/WARNING > 0 | host_components/HostRoles/component_name = DATANODE)
    pageCallCount.set(0);
    pb = new PredicateBuilder();
    predicate = pb.property("alerts_summary/WARNING").greaterThan(0).or().property("host_components/HostRoles/component_name").equals("DATANODE").toPredicate();
    instance.setUserPredicate(predicate);
    instance.setPageRequest(null);
    result = instance.execute();
    tree = result.getResultTree();
    Assert.assertEquals(0, tree.getChildren().size());
    Assert.assertEquals(6, pageCallCount.get());

  }

  @Test
  public void testExecute__collection_nullInternalPredicate_nullUserPredicate() throws Exception {
    ResourceDefinition resourceDefinition = createNiceMock(ResourceDefinition.class);

    Map<Resource.Type, String> mapIds = new HashMap<>();

    // expectations
    expect(resourceDefinition.getType()).andReturn(Resource.Type.Cluster).anyTimes();
    Set<SubResourceDefinition> setChildren = new HashSet<>();
    setChildren.add(new SubResourceDefinition(Resource.Type.Host));

    expect(resourceDefinition.getSubResourceDefinitions()).andReturn(setChildren).anyTimes();

    replay(resourceDefinition);

    //test
    QueryImpl instance = new TestQuery(mapIds, resourceDefinition);

    Result result = instance.execute();

    verify(resourceDefinition);

    TreeNode<Resource> tree = result.getResultTree();

    Assert.assertEquals(1, tree.getChildren().size());
    TreeNode<Resource> clusterNode = tree.getChild("Cluster:1");
    Assert.assertEquals("Cluster:1", clusterNode.getName());
    Assert.assertEquals(Resource.Type.Cluster, clusterNode.getObject().getType());
    Assert.assertEquals(0, clusterNode.getChildren().size());
  }

  @Test
  public void testExecute__collection_nullInternalPredicate_nonNullUserPredicate() throws Exception {
    ResourceDefinition resourceDefinition = createNiceMock(ResourceDefinition.class);

    Map<Resource.Type, String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.Cluster, "cluster");

    // expectations
    expect(resourceDefinition.getType()).andReturn(Resource.Type.Host).anyTimes();
    Set<SubResourceDefinition> setChildren = new HashSet<>();

    expect(resourceDefinition.getSubResourceDefinitions()).andReturn(setChildren).anyTimes();

    replay(resourceDefinition);

    //test
    QueryImpl instance = new TestQuery(mapIds, resourceDefinition);

    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate = pb.property("Hosts/host_name").equals("host:2").toPredicate();

    instance.setUserPredicate(predicate);

    Result result = instance.execute();

    verify(resourceDefinition);

    TreeNode<Resource> tree = result.getResultTree();

    Assert.assertEquals(1, tree.getChildren().size());
    TreeNode<Resource> hostNode = tree.getChild("Host:1");
    Assert.assertEquals("Host:1", hostNode.getName());
    Assert.assertEquals(Resource.Type.Host, hostNode.getObject().getType());
    Assert.assertEquals("host:2", hostNode.getObject().getPropertyValue("Hosts/host_name"));
  }

  @Test
  public void testExecute__collection_nonNullInternalPredicate_nonNullUserPredicate() throws Exception {
    ResourceDefinition resourceDefinition = createNiceMock(ResourceDefinition.class);

    Map<Resource.Type, String> mapIds = new HashMap<>();

    // expectations
    expect(resourceDefinition.getType()).andReturn(Resource.Type.Host).anyTimes();
    Set<SubResourceDefinition> setChildren = new HashSet<>();

    expect(resourceDefinition.getSubResourceDefinitions()).andReturn(setChildren).anyTimes();

    replay(resourceDefinition);

    //test
    QueryImpl instance = new TestQuery(mapIds, resourceDefinition);

    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate = pb.property("Hosts/host_name").equals("host:2").toPredicate();

    instance.setUserPredicate(predicate);

    Result result = instance.execute();

    verify(resourceDefinition);

    TreeNode<Resource> tree = result.getResultTree();

    Assert.assertEquals(1, tree.getChildren().size());
    TreeNode<Resource> hostNode = tree.getChild("Host:1");
    Assert.assertEquals("Host:1", hostNode.getName());
    Assert.assertEquals(Resource.Type.Host, hostNode.getObject().getType());
    Assert.assertEquals("host:2", hostNode.getObject().getPropertyValue("Hosts/host_name"));
  }

  @Test
  public void testAddProperty__localProperty() throws Exception {
    ResourceDefinition resourceDefinition = createNiceMock(ResourceDefinition.class);

    Map<Resource.Type, String> mapIds = new HashMap<>();

    // expectations
    expect(resourceDefinition.getType()).andReturn(Resource.Type.Host).anyTimes();
    Set<SubResourceDefinition> setChildren = new HashSet<>();

    expect(resourceDefinition.getSubResourceDefinitions()).andReturn(setChildren).anyTimes();

    replay(resourceDefinition);

    //test
    QueryImpl instance = new TestQuery(mapIds, resourceDefinition);

    instance.addLocalProperty("c1/p1");

    Result result = instance.execute();

    verify(resourceDefinition);

    TreeNode<Resource> tree = result.getResultTree();

    Assert.assertEquals(4, tree.getChildren().size());
    TreeNode<Resource> hostNode = tree.getChild("Host:1");
    Assert.assertEquals("Host:1", hostNode.getName());
    Assert.assertEquals(Resource.Type.Host, hostNode.getObject().getType());
    Assert.assertNotNull(hostNode.getObject().getPropertyValue("c1/p1"));

    hostNode = tree.getChild("Host:2");
    Assert.assertEquals("Host:2", hostNode.getName());
    Assert.assertEquals(Resource.Type.Host, hostNode.getObject().getType());
    Assert.assertNotNull(hostNode.getObject().getPropertyValue("c1/p1"));

    hostNode = tree.getChild("Host:3");
    Assert.assertEquals("Host:3", hostNode.getName());
    Assert.assertEquals(Resource.Type.Host, hostNode.getObject().getType());
    Assert.assertNotNull(hostNode.getObject().getPropertyValue("c1/p1"));

    hostNode = tree.getChild("Host:4");
    Assert.assertEquals("Host:4", hostNode.getName());
    Assert.assertEquals(Resource.Type.Host, hostNode.getObject().getType());
    Assert.assertNotNull(hostNode.getObject().getPropertyValue("c1/p1"));
  }

  @Test
  public void testAddProperty__allCategoryProperties() throws Exception {
    ResourceDefinition resourceDefinition = createNiceMock(ResourceDefinition.class);

    Map<Resource.Type, String> mapIds = new HashMap<>();

    // expectations
    expect(resourceDefinition.getType()).andReturn(Resource.Type.Host).anyTimes();
    Set<SubResourceDefinition> setChildren = new HashSet<>();

    expect(resourceDefinition.getSubResourceDefinitions()).andReturn(setChildren).anyTimes();

    replay(resourceDefinition);

    //test
    QueryImpl instance = new TestQuery(mapIds, resourceDefinition);

    instance.addLocalProperty("c1");

    Result result = instance.execute();

    verify(resourceDefinition);

    TreeNode<Resource> tree = result.getResultTree();

    Assert.assertEquals(4, tree.getChildren().size());
    TreeNode<Resource> hostNode = tree.getChild("Host:1");
    Assert.assertEquals("Host:1", hostNode.getName());
    Assert.assertEquals(Resource.Type.Host, hostNode.getObject().getType());
    Assert.assertNotNull(hostNode.getObject().getPropertyValue("c1/p1"));
    Assert.assertNotNull(hostNode.getObject().getPropertyValue("c1/p2"));
    Assert.assertNotNull(hostNode.getObject().getPropertyValue("c1/p3"));

    hostNode = tree.getChild("Host:2");
    Assert.assertEquals("Host:2", hostNode.getName());
    Assert.assertEquals(Resource.Type.Host, hostNode.getObject().getType());
    Assert.assertNotNull(hostNode.getObject().getPropertyValue("c1/p1"));
    Assert.assertNotNull(hostNode.getObject().getPropertyValue("c1/p2"));
    Assert.assertNotNull(hostNode.getObject().getPropertyValue("c1/p3"));

    hostNode = tree.getChild("Host:3");
    Assert.assertEquals("Host:3", hostNode.getName());
    Assert.assertEquals(Resource.Type.Host, hostNode.getObject().getType());
    Assert.assertNotNull(hostNode.getObject().getPropertyValue("c1/p1"));
    Assert.assertNotNull(hostNode.getObject().getPropertyValue("c1/p2"));
    Assert.assertNotNull(hostNode.getObject().getPropertyValue("c1/p3"));

    hostNode = tree.getChild("Host:4");
    Assert.assertEquals("Host:4", hostNode.getName());
    Assert.assertEquals(Resource.Type.Host, hostNode.getObject().getType());
    Assert.assertNotNull(hostNode.getObject().getPropertyValue("c1/p1"));
    Assert.assertNotNull(hostNode.getObject().getPropertyValue("c1/p2"));
    Assert.assertNotNull(hostNode.getObject().getPropertyValue("c1/p3"));
  }

  @Test
  public void testExecute_RendererDoesNotRequirePropertyProviderInput() throws Exception {
    EasyMockSupport mockSupport = new EasyMockSupport();

    ResourceDefinition mockResourceDefinition =
      mockSupport.createMock(ResourceDefinition.class);

    SubResourceDefinition mockSubResourceDefinition =
      mockSupport.createMock(SubResourceDefinition.class);

    // need to use a default mock here, to verify
    // that certain methods are called or not called
    ClusterController mockClusterController =
      mockSupport.createMock(ClusterController.class);

    Renderer mockRenderer =
      mockSupport.createMock(Renderer.class);

    QueryResponse mockQueryResponse =
      mockSupport.createMock(QueryResponse.class);

    QueryResponse mockSubQueryResponse =
      mockSupport.createMock(QueryResponse.class);

    Resource mockResource =
      mockSupport.createMock(Resource.class);

    Schema mockSchema =
      mockSupport.createMock(Schema.class);

    expect(mockResourceDefinition.getType()).andReturn(Resource.Type.Host).atLeastOnce();
    expect(mockResourceDefinition.getSubResourceDefinitions()).andReturn(Collections.singleton(mockSubResourceDefinition)).atLeastOnce();

    expect(mockSubResourceDefinition.getType()).andReturn(Resource.Type.Configuration).atLeastOnce();
    expect(mockSubResourceDefinition.isCollection()).andReturn(false).atLeastOnce();

    expect(mockSchema.getKeyPropertyId(isA(Resource.Type.class))).andReturn("test-value").anyTimes();
    expect(mockSchema.getKeyTypes()).andReturn(Collections.emptySet()).anyTimes();

    mockRenderer.init(isA(SchemaFactory.class));
    // the mock renderer should return false for requiresPropertyProviderInput, to
    // simulate the case of a renderer that does not need the property providers to execute
    // this method should be called twice: once each resource (1 resource, 1 sub-resource in this test)
    expect(mockRenderer.requiresPropertyProviderInput()).andReturn(false).times(2);
    expect(mockRenderer.finalizeProperties(isA(TreeNode.class), eq(true))).andReturn(new TreeNodeImpl<>(null, Collections.<String>emptySet(), "test-node"));
    expect(mockRenderer.finalizeResult(isA(Result.class))).andReturn(null);

    // note: the expectations for the ClusterController mock are significant to this test.
    //       the ClusterController.populateResources() method should not be called, and so
    //       is not expected here.
    expect(mockClusterController.getSchema(Resource.Type.Host)).andReturn(mockSchema).anyTimes();
    expect(mockClusterController.getSchema(Resource.Type.Configuration)).andReturn(mockSchema).anyTimes();
    expect(mockClusterController.getResources(eq(Resource.Type.Host), isA(Request.class), (Predicate)eq(null))).andReturn(mockQueryResponse).atLeastOnce();
    expect(mockClusterController.getResources(eq(Resource.Type.Configuration), isA(Request.class), (Predicate)eq(null))).andReturn(mockSubQueryResponse).atLeastOnce();
    expect(mockClusterController.getIterable(eq(Resource.Type.Host), isA(QueryResponse.class), isA(Request.class),(Predicate)eq(null), (PageRequest)eq(null), (SortRequest)eq(null))).andReturn(Collections.singleton(mockResource)).atLeastOnce();
    expect(mockClusterController.getIterable(eq(Resource.Type.Configuration), isA(QueryResponse.class), isA(Request.class),(Predicate)eq(null), (PageRequest)eq(null), (SortRequest)eq(null))).andReturn(Collections.singleton(mockResource)).atLeastOnce();

    expect(mockQueryResponse.getResources()).andReturn(Collections.singleton(mockResource)).atLeastOnce();
    expect(mockSubQueryResponse.getResources()).andReturn(Collections.singleton(mockResource)).atLeastOnce();

    expect(mockResource.getType()).andReturn(Resource.Type.Host).atLeastOnce();

    Map<Resource.Type, String> mapIds = new HashMap<>();

    mockSupport.replayAll();

    QueryImpl instance = new QueryImpl(mapIds, mockResourceDefinition, mockClusterController);
    instance.setRenderer(mockRenderer);

    // call these methods to setup sub resources
    instance.ensureSubResources();
    instance.addProperty("*", null);

    instance.execute();

    mockSupport.verifyAll();

  }

  @Test
  public void testExecute_RendererRequiresPropertyProviderInput() throws Exception {
    EasyMockSupport mockSupport = new EasyMockSupport();

    ResourceDefinition mockResourceDefinition =
      mockSupport.createMock(ResourceDefinition.class);

    SubResourceDefinition mockSubResourceDefinition =
      mockSupport.createMock(SubResourceDefinition.class);

    // need to use a default mock here, to verify
    // that certain methods are called or not called
    ClusterController mockClusterController =
      mockSupport.createMock(ClusterController.class);

    Renderer mockRenderer =
      mockSupport.createMock(Renderer.class);

    QueryResponse mockQueryResponse =
      mockSupport.createMock(QueryResponse.class);

    QueryResponse mockSubQueryResponse =
      mockSupport.createMock(QueryResponse.class);

    Resource mockResource =
      mockSupport.createMock(Resource.class);

    Schema mockSchema =
      mockSupport.createMock(Schema.class);

    expect(mockResourceDefinition.getType()).andReturn(Resource.Type.Host).atLeastOnce();
    expect(mockResourceDefinition.getSubResourceDefinitions()).andReturn(Collections.singleton(mockSubResourceDefinition)).atLeastOnce();

    expect(mockSubResourceDefinition.getType()).andReturn(Resource.Type.Configuration).atLeastOnce();
    expect(mockSubResourceDefinition.isCollection()).andReturn(false).atLeastOnce();

    expect(mockSchema.getKeyPropertyId(isA(Resource.Type.class))).andReturn("test-value").anyTimes();
    expect(mockSchema.getKeyTypes()).andReturn(Collections.emptySet()).anyTimes();

    mockRenderer.init(isA(SchemaFactory.class));
    // simulate the case of a renderer that requires the property providers to execute
    // this method should be called twice: once for each resource (1 resource, 1 sub-resource in this test)
    expect(mockRenderer.requiresPropertyProviderInput()).andReturn(true).times(2);
    expect(mockRenderer.finalizeProperties(isA(TreeNode.class), eq(true))).andReturn(new TreeNodeImpl<>(null, Collections.<String>emptySet(), "test-node"));
    expect(mockRenderer.finalizeResult(isA(Result.class))).andReturn(null);

    // note: the expectations for the ClusterController mock are significant to this test.
    expect(mockClusterController.getSchema(Resource.Type.Host)).andReturn(mockSchema).anyTimes();
    expect(mockClusterController.getSchema(Resource.Type.Configuration)).andReturn(mockSchema).anyTimes();
    expect(mockClusterController.getResources(eq(Resource.Type.Host), isA(Request.class), (Predicate)eq(null))).andReturn(mockQueryResponse).atLeastOnce();
    expect(mockClusterController.getResources(eq(Resource.Type.Configuration), isA(Request.class), (Predicate)eq(null))).andReturn(mockSubQueryResponse).atLeastOnce();
    expect(mockClusterController.getIterable(eq(Resource.Type.Host), isA(QueryResponse.class), isA(Request.class),(Predicate)eq(null), (PageRequest)eq(null), (SortRequest)eq(null))).andReturn(Collections.singleton(mockResource)).atLeastOnce();
    expect(mockClusterController.getIterable(eq(Resource.Type.Configuration), isA(QueryResponse.class), isA(Request.class),(Predicate)eq(null), (PageRequest)eq(null), (SortRequest)eq(null))).andReturn(Collections.singleton(mockResource)).atLeastOnce();
    // expect call to activate property providers for Host resource
    expect(mockClusterController.populateResources(eq(Resource.Type.Host), eq(Collections.singleton(mockResource)), isA(Request.class), (Predicate)eq(null))).andReturn(Collections.emptySet()).times(1);
    // expect call to activate property providers for Configuration sub-resource
    expect(mockClusterController.populateResources(eq(Resource.Type.Configuration), eq(Collections.singleton(mockResource)), isA(Request.class), (Predicate)eq(null))).andReturn(Collections.emptySet()).times(1);

    expect(mockQueryResponse.getResources()).andReturn(Collections.singleton(mockResource)).atLeastOnce();
    expect(mockSubQueryResponse.getResources()).andReturn(Collections.singleton(mockResource)).atLeastOnce();

    expect(mockResource.getType()).andReturn(Resource.Type.Host).atLeastOnce();

    Map<Resource.Type, String> mapIds = new HashMap<>();

    mockSupport.replayAll();

    QueryImpl instance = new QueryImpl(mapIds, mockResourceDefinition, mockClusterController);
    instance.setRenderer(mockRenderer);

    // call these methods to setup sub resources
    instance.ensureSubResources();
    instance.addProperty("*", null);

    instance.execute();

    mockSupport.verifyAll();

  }

  public static class TestQuery extends QueryImpl {
    public TestQuery(Map<Resource.Type, String> mapIds, ResourceDefinition resourceDefinition) {
      super(mapIds, resourceDefinition, new ClusterControllerImpl(new ClusterControllerImplTest.TestProviderModule()));
      setRenderer(new DefaultRenderer());
    }

    public TestQuery(Map<Resource.Type, String> mapIds, ResourceDefinition resourceDefinition, ClusterController clusterController) {
      super(mapIds, resourceDefinition, clusterController);
      setRenderer(new DefaultRenderer());
    }
  }
}

