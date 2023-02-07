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
package org.apache.drill.common.logical.data;

import static org.hamcrest.MatcherAssert.assertThat;

import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.logical.data.Order.Ordering;
import org.apache.calcite.rel.RelFieldCollation;
import org.apache.calcite.rel.RelFieldCollation.Direction;
import org.apache.calcite.rel.RelFieldCollation.NullDirection;
import org.apache.drill.test.BaseTest;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;

public class OrderTest extends BaseTest {

  //////////
  // Order.Ordering tests:

  // "Round trip" tests that strings from output work as input:

  @Test
  public void test_Ordering_roundTripAscAndNullsFirst() {
    Ordering src = new Ordering( Direction.ASCENDING, null, NullDirection.FIRST);
    Ordering reconstituted =
        new Ordering( src.getDirection(), (LogicalExpression) null, src.getNullDirection() );
    assertThat( reconstituted.getDirection(), equalTo( RelFieldCollation.Direction.ASCENDING  ) );
    assertThat( reconstituted.getNullDirection(), equalTo( NullDirection.FIRST  ) );
  }

  @Test
  public void test_Ordering_roundTripDescAndNullsLast() {
    Ordering src = new Ordering( Direction.DESCENDING, null, NullDirection.LAST);
    Ordering reconstituted =
        new Ordering( src.getDirection(), (LogicalExpression) null, src.getNullDirection() );
    assertThat( reconstituted.getDirection(), equalTo( RelFieldCollation.Direction.DESCENDING  ) );
    assertThat( reconstituted.getNullDirection(), equalTo( NullDirection.LAST  ) );
  }

  @Test
  public void test_Ordering_roundTripDescAndNullsUnspecified() {
    Ordering src = new Ordering( Direction.DESCENDING, null, NullDirection.UNSPECIFIED);
    Ordering reconstituted =
        new Ordering( src.getDirection(), (LogicalExpression) null, src.getNullDirection() );
    assertThat( reconstituted.getDirection(), equalTo( RelFieldCollation.Direction.DESCENDING  ) );
    assertThat( reconstituted.getNullDirection(), equalTo( NullDirection.UNSPECIFIED  ) );
  }

  // Basic input validation:
  @Test( expected = DrillRuntimeException.class )  // (Currently.)
  public void test_Ordering_garbageOrderRejected() {
    new Ordering( "AS CE ND IN G", null, null );
  }

  @Test( expected = DrillRuntimeException.class )  // (Currently.)
  public void test_Ordering_garbageNullOrderingRejected() {
    new Ordering( null, null, "HIGH" );
  }


  // Defaults-value/null-strings test:

  @Test
  public void testOrdering_nullStrings() {
    Ordering ordering = new Ordering( (String) null, (LogicalExpression) null, null );
    assertThat( ordering.getDirection(), equalTo( RelFieldCollation.Direction.ASCENDING ) );
    assertThat( ordering.getNullDirection(), equalTo( RelFieldCollation.NullDirection.UNSPECIFIED ) );
    assertThat( ordering.getOrder(), equalTo( "ASC" ) );
  }


}
