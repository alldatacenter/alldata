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

package org.apache.ambari.server.collections;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.apache.ambari.server.collections.functors.AndPredicate;
import org.apache.ambari.server.collections.functors.ContainsPredicate;
import org.apache.ambari.server.collections.functors.ContextTransformer;
import org.apache.ambari.server.collections.functors.EqualsPredicate;
import org.apache.ambari.server.collections.functors.NotPredicate;
import org.apache.ambari.server.collections.functors.OrPredicate;
import org.junit.Test;

import junit.framework.Assert;

public class PredicateUtilsTest {
  @Test
  public void toMap() throws Exception {
    // Test null predicate safely returns null
    Assert.assertNull(PredicateUtils.toMap(null));

    Assert.assertEquals(createMap(), PredicateUtils.toMap(createPredicate()));
  }

  @Test
  public void fromMap() throws Exception {
    verifyPredicate(PredicateUtils.fromMap(createMap()));
  }

  @Test
  public void toJSON() throws Exception {
    // Test null predicate safely returns null
    Assert.assertNull(PredicateUtils.toJSON(null));

    Assert.assertEquals(createJSON(), PredicateUtils.toJSON(createPredicate()));
  }

  @Test
  public void fromJSON() throws Exception {
    verifyPredicate(PredicateUtils.fromJSON(createJSON()));
  }

  private Predicate createPredicate() {
    ContextTransformer transformer1 = new ContextTransformer("services");
    ContextTransformer transformer2 = new ContextTransformer("configurations/service-env/property1");
    ContextTransformer transformer3 = new ContextTransformer("configurations/cluster-env/property1");
    ContainsPredicate predicate1 = new ContainsPredicate(transformer1, "HDFS");
    EqualsPredicate predicate2 = new EqualsPredicate(transformer2, "true");
    EqualsPredicate predicate3 = new EqualsPredicate(transformer3, "false");

    AndPredicate andPredicate = new AndPredicate(predicate1, predicate2);
    OrPredicate orPredicate = new OrPredicate(predicate3, andPredicate);

    return new NotPredicate(orPredicate);
  }

  private Map<String, Object> createMap() {
    Map<String, Object> andMap =
        Collections.singletonMap(
            AndPredicate.NAME, Arrays.asList(
                Collections.<String, Object>singletonMap(ContainsPredicate.NAME, Arrays.asList("services", "HDFS")),
                Collections.<String, Object>singletonMap(EqualsPredicate.NAME, Arrays.asList("configurations/service-env/property1", "true"))
            )
        );

    Map<String, Object> orMap =
        Collections.singletonMap(OrPredicate.NAME,
            Arrays.asList(
                Collections.<String, Object>singletonMap(EqualsPredicate.NAME, Arrays.asList("configurations/cluster-env/property1", "false")),
                andMap
            )
        );

    return Collections.singletonMap(NotPredicate.NAME, orMap);
  }

  private String createJSON() {
    String andJSON = "{\"and\":[{\"contains\":[\"services\",\"HDFS\"]},{\"equals\":[\"configurations/service-env/property1\",\"true\"]}]}";
    String orJSON = "{\"or\":[{\"equals\":[\"configurations/cluster-env/property1\",\"false\"]}," + andJSON + "]}";
    return "{\"not\":" + orJSON + "}";
  }

  private void verifyPredicate(Predicate predicate) {
    Assert.assertNotNull(predicate);
    Assert.assertEquals(NotPredicate.NAME, predicate.getName());
    Assert.assertTrue(predicate instanceof NotPredicate);

    org.apache.commons.collections.Predicate[] predicates;

    predicates = ((NotPredicate) predicate).getPredicates();
    Assert.assertEquals(1, predicates.length);

    Assert.assertNotNull(predicates[0]);
    Assert.assertTrue(predicates[0] instanceof OrPredicate);
    Assert.assertEquals(OrPredicate.NAME, ((OrPredicate) predicates[0]).getName());

    predicates = ((OrPredicate) predicates[0]).getPredicates();
    Assert.assertEquals(2, predicates.length);

    Assert.assertNotNull(predicates[0]);
    Assert.assertTrue(predicates[0] instanceof EqualsPredicate);
    Assert.assertEquals(EqualsPredicate.NAME, ((EqualsPredicate) predicates[0]).getName());
    Assert.assertEquals("configurations/cluster-env/property1", ((EqualsPredicate) predicates[0]).getContextKey());
    Assert.assertEquals("false", ((EqualsPredicate) predicates[0]).getValue());

    Assert.assertNotNull(predicates[1]);
    Assert.assertTrue(predicates[1] instanceof AndPredicate);
    Assert.assertEquals(AndPredicate.NAME, ((AndPredicate) predicates[1]).getName());

    predicates = ((AndPredicate) predicates[1]).getPredicates();
    Assert.assertEquals(2, predicates.length);

    Assert.assertNotNull(predicates[0]);
    Assert.assertTrue(predicates[0] instanceof ContainsPredicate);
    Assert.assertEquals(ContainsPredicate.NAME, ((ContainsPredicate) predicates[0]).getName());
    Assert.assertEquals("services", ((ContainsPredicate) predicates[0]).getContextKey());
    Assert.assertEquals("HDFS", ((ContainsPredicate) predicates[0]).getValue());

    Assert.assertNotNull(predicates[1]);
    Assert.assertTrue(predicates[1] instanceof EqualsPredicate);
    Assert.assertEquals(EqualsPredicate.NAME, ((EqualsPredicate) predicates[1]).getName());
    Assert.assertEquals("configurations/service-env/property1", ((EqualsPredicate) predicates[1]).getContextKey());
    Assert.assertEquals("true", ((EqualsPredicate) predicates[1]).getValue());
  }
}