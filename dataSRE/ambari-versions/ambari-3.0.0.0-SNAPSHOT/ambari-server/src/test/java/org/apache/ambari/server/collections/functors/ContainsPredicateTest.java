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

package org.apache.ambari.server.collections.functors;

import static org.easymock.EasyMock.expect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Assert;
import org.junit.Test;

public class ContainsPredicateTest extends EasyMockSupport {

  @Test
  public void testEvaluate() {
    Set<String> data1 = new HashSet<>(Arrays.asList("ONE", "TWO", "THREE"));
    Set<String> data2 = new HashSet<>(Arrays.asList("TWO", "THREE"));

    ContextTransformer transformer = createStrictMock(ContextTransformer.class);
    expect(transformer.transform(EasyMock.<Map<?, ?>>anyObject())).andReturn(data1).times(1);
    expect(transformer.transform(EasyMock.<Map<?, ?>>anyObject())).andReturn(data2).times(1);

    replayAll();

    ContainsPredicate predicate = new ContainsPredicate(transformer, "ONE");

    Assert.assertTrue(predicate.evaluate(Collections.singletonMap("data", data1)));
    Assert.assertFalse(predicate.evaluate(Collections.singletonMap("data", data2)));

    verifyAll();
  }

  @Test
  public void testToMap() {
    ContextTransformer transformer = createStrictMock(ContextTransformer.class);
    expect(transformer.getKey()).andReturn("data").times(1);

    replayAll();

    ContainsPredicate predicate = new ContainsPredicate(transformer, "ONE");
    Map<String, Object> actualMap = predicate.toMap();

    verifyAll();

    Map<String, Object> expectedMap = new HashMap<>();
    expectedMap.put("contains", new ArrayList<>(Arrays.asList("data", "ONE")));

    Assert.assertEquals(expectedMap, actualMap);
  }

  @Test
  public void testToJSON() {
    ContextTransformer transformer = createStrictMock(ContextTransformer.class);
    expect(transformer.getKey()).andReturn("data").times(1);

    replayAll();

    ContainsPredicate predicate = new ContainsPredicate(transformer, "ONE");
    String actualJSON = predicate.toJSON();

    verifyAll();

    String expectedJSON = "{\"contains\":[\"data\",\"ONE\"]}";

    Assert.assertEquals(expectedJSON, actualJSON);
  }
}