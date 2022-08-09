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
import java.util.Map;

import org.apache.ambari.server.collections.Predicate;
import org.easymock.EasyMockSupport;
import org.junit.Assert;
import org.junit.Test;

public class OrPredicateTest extends EasyMockSupport {

  @Test
  public void testEvaluate() {
    Predicate mockPredicate1 = createStrictMock(Predicate.class);
    expect(mockPredicate1.evaluate("context")).andReturn(true).times(1);
    expect(mockPredicate1.evaluate("context")).andReturn(false).times(1);
    expect(mockPredicate1.evaluate("context")).andReturn(true).times(1);

    Predicate mockPredicate2 = createStrictMock(Predicate.class);
    expect(mockPredicate2.evaluate("context")).andReturn(true).times(1);

    replayAll();

    OrPredicate predicate = new OrPredicate(mockPredicate1, mockPredicate2);

    // Try with true and ???? (short circuited)
    predicate.evaluate("context");

    // Try with false and true
    predicate.evaluate("context");

    // Try with true and ???? (short circuited)
    predicate.evaluate("context");

    verifyAll();

    Assert.assertArrayEquals(new Predicate[]{mockPredicate1, mockPredicate2}, predicate.getPredicates());
  }

  @Test
  public void testToMap() {
    Predicate mockPredicate1 = createStrictMock(Predicate.class);
    expect(mockPredicate1.toMap()).andReturn(Collections.singletonMap("nop", "foo")).times(1);

    Predicate mockPredicate2 = createStrictMock(Predicate.class);
    expect(mockPredicate2.toMap()).andReturn(Collections.singletonMap("nop", "baz")).times(1);

    replayAll();

    OrPredicate predicate = new OrPredicate(mockPredicate1, mockPredicate2);
    Map<String, Object> actualMap = predicate.toMap();

    verifyAll();

    Map<String, Object> expectedMap = new HashMap<>();
    expectedMap.put("or", new ArrayList<>(
      Arrays.asList(Collections.<String, Object>singletonMap("nop", "foo"),
        Collections.<String, Object>singletonMap("nop", "baz"))
    ));

    Assert.assertEquals(expectedMap, actualMap);
  }

  @Test
  public void testToJSON() {
    Predicate mockPredicate1 = createStrictMock(Predicate.class);
    expect(mockPredicate1.toMap()).andReturn(Collections.singletonMap("nop", "foo")).times(1);

    Predicate mockPredicate2 = createStrictMock(Predicate.class);
    expect(mockPredicate2.toMap()).andReturn(Collections.singletonMap("nop", "baz")).times(1);

    replayAll();

    OrPredicate predicate = new OrPredicate(mockPredicate1, mockPredicate2);
    String actualJSON = predicate.toJSON();

    verifyAll();

    String expectedJSON = "{\"or\":[{\"nop\":\"foo\"},{\"nop\":\"baz\"}]}";

    Assert.assertEquals(expectedJSON, actualJSON);
  }
}