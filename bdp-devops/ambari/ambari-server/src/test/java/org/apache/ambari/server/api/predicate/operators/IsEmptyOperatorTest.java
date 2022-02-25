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

import org.apache.ambari.server.api.predicate.InvalidQueryException;
import org.apache.ambari.server.controller.predicate.CategoryIsEmptyPredicate;
import org.apache.ambari.server.controller.spi.Predicate;
import org.junit.Test;

/**
 * IsEmpty operator test.
 */
public class IsEmptyOperatorTest {

  @Test
  public void testGetName() {
    assertEquals("IsEmptyOperator", new IsEmptyOperator().getName());
  }

  @Test
  public void testToPredicate() throws InvalidQueryException {
    String prop = "prop";
    Predicate p = new CategoryIsEmptyPredicate(prop);

    assertEquals(p, new IsEmptyOperator().toPredicate(prop, null));
  }

  @Test
  public void testGetType() {
    assertSame(Operator.TYPE.IS_EMPTY, new IsEmptyOperator().getType());
  }

  @Test
  public void testGetBasePrecedence() {
    assertEquals(-1, new IsEmptyOperator().getBasePrecedence());
  }

  @Test
  public void testGetPrecedence() {
    assertEquals(-1, new IsEmptyOperator().getPrecedence());
  }
}
