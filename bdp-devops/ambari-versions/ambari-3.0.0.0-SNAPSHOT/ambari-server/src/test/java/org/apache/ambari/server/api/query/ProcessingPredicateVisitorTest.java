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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.api.resources.HostResourceDefinition;
import org.apache.ambari.server.api.resources.ResourceDefinition;
import org.apache.ambari.server.api.resources.StackResourceDefinition;
import org.apache.ambari.server.controller.predicate.AndPredicate;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PredicateHelper;
import org.junit.Assert;
import org.junit.Test;

/**
 * ProcessingPredicateVisitor tests.
 */
public class ProcessingPredicateVisitorTest {
  @Test
  public void testGetProcessedPredicate() throws Exception {
    ResourceDefinition resourceDefinition = new StackResourceDefinition();

    Map<Resource.Type, String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.Stack, "HDP");

    //test
    QueryImpl instance = new QueryImplTest.TestQuery(mapIds, resourceDefinition);

    Predicate predicate = new PredicateBuilder().property("Stacks/stack_name").equals("HDP").and().
        property("versions/stackServices/StackServices/service_name").equals("HBASE").and().
        property("versions/operatingSystems/OperatingSystems/os_type").equals("centos5").toPredicate();

    ProcessingPredicateVisitor visitor = new ProcessingPredicateVisitor(instance);
    PredicateHelper.visit(predicate, visitor);

    Predicate processedPredicate = visitor.getProcessedPredicate();

    Predicate expectedPredicate = new PredicateBuilder().property("Stacks/stack_name").equals("HDP").toPredicate();

    Assert.assertEquals(expectedPredicate, processedPredicate);
  }

  @Test
  public void testGetSubResourceForNotPredicate() throws Exception {
    ResourceDefinition resourceDefinition = new HostResourceDefinition();

    Map<Resource.Type, String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.Host, null);

    //test
    QueryImpl instance = new QueryImplTest.TestQuery(mapIds, resourceDefinition);

    Predicate notPredicate1 = new PredicateBuilder().not().property("host_components/HostRoles/component_name").
            equals("ZOOKEEPER_SERVER").toPredicate();
    Predicate notPredicate2 = new PredicateBuilder().not().property("host_components/HostRoles/component_name").
            equals("HBASE_MASTER").toPredicate();
    Predicate andPredicate = new AndPredicate(notPredicate1,notPredicate2);

    ProcessingPredicateVisitor visitor = new ProcessingPredicateVisitor(instance);
    PredicateHelper.visit(andPredicate, visitor);

    Set<String> categories = visitor.getSubResourceCategories();

    Assert.assertEquals(categories.iterator().next(), "host_components");
  }


  @Test
  public void testGetSubResourceCategories() throws Exception {
    ResourceDefinition resourceDefinition = new StackResourceDefinition();

    Map<Resource.Type, String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.Stack, "HDP");

    //test
    QueryImpl instance = new QueryImplTest.TestQuery(mapIds, resourceDefinition);

    Predicate predicate = new PredicateBuilder().property("Stacks/stack_name").equals("HDP").and().
        property("versions/stackServices/StackServices/service_name").equals("HBASE").and().
        property("versions/operatingSystems/OperatingSystems/os_type").equals("centos5").toPredicate();

    ProcessingPredicateVisitor visitor = new ProcessingPredicateVisitor(instance);
    PredicateHelper.visit(predicate, visitor);

    Set<String> categories = visitor.getSubResourceCategories();

    Set<String> expected = new HashSet<>();
    expected.add("versions");

    Assert.assertEquals(expected, categories);
  }

  @Test
  public void testGetSubResourceProperties() throws Exception {
    ResourceDefinition resourceDefinition = new StackResourceDefinition();

    Map<Resource.Type, String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.Stack, "HDP");

    //test
    QueryImpl instance = new QueryImplTest.TestQuery(mapIds, resourceDefinition);

    Predicate predicate = new PredicateBuilder().property("Stacks/stack_name").equals("HDP").and().
        property("versions/stackServices/StackServices/service_name").equals("HBASE").and().
        property("versions/operatingSystems/OperatingSystems/os_type").equals("centos5").toPredicate();

    ProcessingPredicateVisitor visitor = new ProcessingPredicateVisitor(instance);
    PredicateHelper.visit(predicate, visitor);

    Set<String> properties = visitor.getSubResourceProperties();

    Set<String> expected = new HashSet<>();
    expected.add("versions/stackServices/StackServices/service_name");
    expected.add("versions/operatingSystems/OperatingSystems/os_type");

    Assert.assertEquals(expected, properties);
  }
}
