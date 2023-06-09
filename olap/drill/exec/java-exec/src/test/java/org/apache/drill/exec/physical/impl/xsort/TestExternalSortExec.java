/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.physical.impl.xsort;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.calcite.rel.RelFieldCollation.Direction;
import org.apache.calcite.rel.RelFieldCollation.NullDirection;
import org.apache.drill.categories.OperatorTest;
import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.common.expression.FieldReference;
import org.apache.drill.common.logical.data.Order.Ordering;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.physical.base.AbstractBase;
import org.apache.drill.exec.physical.config.ExternalSort;
import org.apache.drill.exec.record.BatchSchema.SelectionVectorMode;
import org.apache.drill.test.DrillTest;
import org.junit.Test;

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.junit.experimental.categories.Category;

@Category(OperatorTest.class)
public class TestExternalSortExec extends DrillTest {

  @Test
  public void testFieldReference() {
    // Misnomer: the reference must be unquoted.
    FieldReference expr = FieldReference.getWithQuotedRef("foo");
    assertEquals(Types.LATE_BIND_TYPE, expr.getMajorType());
    assertTrue(expr.isSimplePath());
    assertEquals("foo", expr.getRootSegment().getPath());
    assertEquals("`foo`", expr.toExpr());
  }

  @Test
  public void testOrdering() {
    assertEquals(Direction.ASCENDING, Ordering.getOrderingSpecFromString(null));
    assertEquals(Direction.ASCENDING, Ordering.getOrderingSpecFromString(Ordering.ORDER_ASC));
    assertEquals(Direction.DESCENDING, Ordering.getOrderingSpecFromString(Ordering.ORDER_DESC));
    assertEquals(Direction.ASCENDING, Ordering.getOrderingSpecFromString(Ordering.ORDER_ASCENDING));
    assertEquals(Direction.DESCENDING, Ordering.getOrderingSpecFromString(Ordering.ORDER_DESCENDING));
    assertEquals(Direction.ASCENDING, Ordering.getOrderingSpecFromString(Ordering.ORDER_ASC.toLowerCase()));
    assertEquals(Direction.DESCENDING, Ordering.getOrderingSpecFromString(Ordering.ORDER_DESC.toLowerCase()));
    assertEquals(Direction.ASCENDING, Ordering.getOrderingSpecFromString(Ordering.ORDER_ASCENDING.toLowerCase()));
    assertEquals(Direction.DESCENDING, Ordering.getOrderingSpecFromString(Ordering.ORDER_DESCENDING.toLowerCase()));
    try {
      Ordering.getOrderingSpecFromString("");
      fail();
    } catch(DrillRuntimeException e) { }
    try {
      Ordering.getOrderingSpecFromString("foo");
      fail();
    } catch(DrillRuntimeException e) { }

    assertEquals(NullDirection.UNSPECIFIED, Ordering.getNullOrderingFromString(null));
    assertEquals(NullDirection.FIRST, Ordering.getNullOrderingFromString(Ordering.NULLS_FIRST));
    assertEquals(NullDirection.LAST, Ordering.getNullOrderingFromString(Ordering.NULLS_LAST));
    assertEquals(NullDirection.UNSPECIFIED, Ordering.getNullOrderingFromString(Ordering.NULLS_UNSPECIFIED));
    assertEquals(NullDirection.FIRST, Ordering.getNullOrderingFromString(Ordering.NULLS_FIRST.toLowerCase()));
    assertEquals(NullDirection.LAST, Ordering.getNullOrderingFromString(Ordering.NULLS_LAST.toLowerCase()));
    assertEquals(NullDirection.UNSPECIFIED, Ordering.getNullOrderingFromString(Ordering.NULLS_UNSPECIFIED.toLowerCase()));
    try {
      Ordering.getNullOrderingFromString("");
      fail();
    } catch(DrillRuntimeException e) { }
    try {
      Ordering.getNullOrderingFromString("foo");
      fail();
    } catch(DrillRuntimeException e) { }

    FieldReference expr = FieldReference.getWithQuotedRef("foo");

    // Test all getters

    Ordering ordering = new Ordering((String) null, expr, (String) null);
    assertEquals(Direction.ASCENDING, ordering.getDirection());
    assertEquals(NullDirection.UNSPECIFIED, ordering.getNullDirection());
    assertSame(expr, ordering.getExpr());
    assertTrue(ordering.nullsSortHigh());

    // Test all ordering strings

    ordering = new Ordering((String) Ordering.ORDER_ASC, expr, (String) null);
    assertEquals(Direction.ASCENDING, ordering.getDirection());
    assertEquals(NullDirection.UNSPECIFIED, ordering.getNullDirection());

    ordering = new Ordering((String) Ordering.ORDER_ASC.toLowerCase(), expr, (String) null);
    assertEquals(Direction.ASCENDING, ordering.getDirection());
    assertEquals(NullDirection.UNSPECIFIED, ordering.getNullDirection());

    ordering = new Ordering((String) Ordering.ORDER_ASCENDING, expr, (String) null);
    assertEquals(Direction.ASCENDING, ordering.getDirection());
    assertEquals(NullDirection.UNSPECIFIED, ordering.getNullDirection());

    ordering = new Ordering((String) Ordering.ORDER_DESC, expr, (String) null);
    assertEquals(Direction.DESCENDING, ordering.getDirection());
    assertEquals(NullDirection.UNSPECIFIED, ordering.getNullDirection());

    ordering = new Ordering((String) Ordering.ORDER_DESCENDING, expr, (String) null);
    assertEquals(Direction.DESCENDING, ordering.getDirection());
    assertEquals(NullDirection.UNSPECIFIED, ordering.getNullDirection());

    // Test all null ordering strings

    ordering = new Ordering((String) null, expr, Ordering.NULLS_FIRST);
    assertEquals(Direction.ASCENDING, ordering.getDirection());
    assertEquals(NullDirection.FIRST, ordering.getNullDirection());
    assertFalse(ordering.nullsSortHigh());

    ordering = new Ordering((String) null, expr, Ordering.NULLS_FIRST);
    assertEquals(Direction.ASCENDING, ordering.getDirection());
    assertEquals(NullDirection.FIRST, ordering.getNullDirection());
    assertFalse(ordering.nullsSortHigh());

    ordering = new Ordering((String) null, expr, Ordering.NULLS_LAST);
    assertEquals(Direction.ASCENDING, ordering.getDirection());
    assertEquals(NullDirection.LAST, ordering.getNullDirection());
    assertTrue(ordering.nullsSortHigh());

    ordering = new Ordering((String) null, expr, Ordering.NULLS_UNSPECIFIED);
    assertEquals(Direction.ASCENDING, ordering.getDirection());
    assertEquals(NullDirection.UNSPECIFIED, ordering.getNullDirection());
    assertTrue(ordering.nullsSortHigh());

    // Unspecified order is always nulls high

    ordering = new Ordering(Ordering.ORDER_DESC, expr, Ordering.NULLS_UNSPECIFIED);
    assertEquals(Direction.DESCENDING, ordering.getDirection());
    assertEquals(NullDirection.UNSPECIFIED, ordering.getNullDirection());
    assertTrue(ordering.nullsSortHigh());

    // Null sort direction reverses with a Desc sort.

    ordering = new Ordering(Ordering.ORDER_DESC, expr, Ordering.NULLS_FIRST);
    assertEquals(Direction.DESCENDING, ordering.getDirection());
    assertEquals(NullDirection.FIRST, ordering.getNullDirection());
    assertTrue(ordering.nullsSortHigh());

    ordering = new Ordering(Ordering.ORDER_DESC, expr, Ordering.NULLS_LAST);
    assertEquals(Direction.DESCENDING, ordering.getDirection());
    assertEquals(NullDirection.LAST, ordering.getNullDirection());
    assertFalse(ordering.nullsSortHigh());
  }

  @Test
  public void testSortSpec() {
    FieldReference expr = FieldReference.getWithQuotedRef("foo");
    Ordering ordering = new Ordering(Ordering.ORDER_ASC, expr, Ordering.NULLS_FIRST);

    // Basics

    ExternalSort popConfig = new ExternalSort(null, Lists.newArrayList(ordering), false);
    assertSame(ordering, popConfig.getOrderings().get(0));
    assertFalse(popConfig.getReverse());
    assertEquals(SelectionVectorMode.FOUR_BYTE, popConfig.getSVMode());
    assertEquals(ExternalSort.OPERATOR_TYPE, popConfig.getOperatorType());
    assertEquals(ExternalSort.DEFAULT_SORT_ALLOCATION, popConfig.getInitialAllocation());
    assertEquals(AbstractBase.MAX_ALLOCATION, popConfig.getMaxAllocation());
    assertTrue(popConfig.isExecutable());

    // Non-default settings

    popConfig = new ExternalSort(null, Lists.newArrayList(ordering), true);
    assertTrue(popConfig.getReverse());
    long maxAlloc = 50_000_000;
    popConfig.setMaxAllocation(maxAlloc);
    assertEquals(ExternalSort.DEFAULT_SORT_ALLOCATION, popConfig.getInitialAllocation());
    assertEquals(maxAlloc, popConfig.getMaxAllocation());
  }
}
