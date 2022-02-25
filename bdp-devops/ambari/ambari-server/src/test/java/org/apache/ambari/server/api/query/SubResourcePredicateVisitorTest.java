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

import org.apache.ambari.server.controller.predicate.AndPredicate;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PredicateHelper;
import org.junit.Assert;
import org.junit.Test;

/**
 * SubResourcePredicateVisitor tests.
 */
public class SubResourcePredicateVisitorTest {
  @Test
  public void testGetSubResourcePredicate() throws Exception {

    Predicate predicate = new PredicateBuilder().property("ServiceInfo/service_name").equals("HBASE").and().
        property("components/ServiceComponentInfo/category").equals("SLAVE").and().
        property("components/host_components/metrics/cpu/cpu_num").greaterThanEqualTo(1).toPredicate();

    SubResourcePredicateVisitor visitor = new SubResourcePredicateVisitor("components");
    PredicateHelper.visit(predicate, visitor);

    Predicate subResourcePredicate = visitor.getSubResourcePredicate();

    Predicate expectedPredicate = new PredicateBuilder().property("ServiceComponentInfo/category").equals("SLAVE").and().
        property("host_components/metrics/cpu/cpu_num").greaterThanEqualTo(1).toPredicate();

    Assert.assertEquals(expectedPredicate, subResourcePredicate);


    predicate = new PredicateBuilder().property("ServiceInfo/service_name").equals("HBASE").and().
        property("ServiceInfo/component_name").equals("HBASE_MASTER").toPredicate();

    visitor = new SubResourcePredicateVisitor("components");
    PredicateHelper.visit(predicate, visitor);

    subResourcePredicate = visitor.getSubResourcePredicate();

    Assert.assertEquals(new AndPredicate(), subResourcePredicate);


    Predicate notPredicate = new PredicateBuilder().not().property("host_components/HostRoles/component_name").
            equals("ZOOKEEPER_SERVER").toPredicate();

    Predicate expectedNotPredicate = new PredicateBuilder().not().property("HostRoles/component_name").
            equals("ZOOKEEPER_SERVER").toPredicate();

    visitor = new SubResourcePredicateVisitor("host_components");
    PredicateHelper.visit(notPredicate, visitor);

    subResourcePredicate = visitor.getSubResourcePredicate();

    Assert.assertEquals(expectedNotPredicate, subResourcePredicate);
  }
}
