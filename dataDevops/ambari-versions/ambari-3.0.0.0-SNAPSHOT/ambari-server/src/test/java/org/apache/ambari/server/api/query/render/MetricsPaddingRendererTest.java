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
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.apache.ambari.server.api.query.QueryInfo;
import org.apache.ambari.server.api.resources.ServiceResourceDefinition;
import org.apache.ambari.server.api.util.TreeNode;
import org.apache.ambari.server.api.util.TreeNodeImpl;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.Schema;
import org.apache.ambari.server.controller.spi.SchemaFactory;
import org.junit.Test;

public class MetricsPaddingRendererTest {
  @Test
  public void testFinalizeProperties__NullPadding_property() {
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

    MetricsPaddingRenderer renderer = new MetricsPaddingRenderer("null_padding");
    renderer.init(schemaFactory);
    TreeNode<Set<String>> propertyTree = renderer.finalizeProperties(queryTree, false);

    assertEquals(4, propertyTree.getObject().size());
    assertTrue(propertyTree.getObject().contains("ServiceInfo/service_name"));
    assertTrue(propertyTree.getObject().contains("ServiceInfo/cluster_name"));
    assertTrue(propertyTree.getObject().contains("foo/bar"));
    assertTrue(propertyTree.getObject().contains("params/padding/NULLS"));
    assertEquals(0, propertyTree.getChildren().size());


    verify(schemaFactory, schema);
  }
}
