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

package org.apache.ambari.server.api.predicate.operators;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import org.apache.ambari.server.controller.predicate.EqualsPredicate;
import org.apache.ambari.server.controller.predicate.OrPredicate;
import org.junit.Test;


/**
 * OR operator test.
 */
public class OrOperatorTest {

  @Test
  public void testGetName() {
    assertEquals("OrOperator", new OrOperator(1).getName());
  }

  @Test
  public void testToPredicate() {
    EqualsPredicate p1 = new EqualsPredicate<>("p1", "one");
    EqualsPredicate p2 = new EqualsPredicate<>("p2", "two");
    OrPredicate orPRedicate = new OrPredicate(p1, p2);

    assertEquals(orPRedicate, new OrOperator(1).toPredicate(p1, p2));
  }

  @Test
  public void testGetType() {
    assertSame(Operator.TYPE.OR, new OrOperator(1).getType());
  }

  @Test
  public void testGetBasePrecedence() {
    assertEquals(1, new OrOperator(1).getBasePrecedence());
  }

  @Test
  public void testGetPrecedence() {
    assertEquals(3, new OrOperator(2).getPrecedence());
  }
}
