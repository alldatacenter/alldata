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
package org.apache.drill.exec.planner.logical;

import org.apache.drill.common.util.GuavaUtils;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.rex.RexBuilder;
import org.apache.calcite.rex.RexFieldCollation;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.type.SqlTypeFactoryImpl;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.drill.categories.PlannerTest;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.planner.types.DrillRelDataTypeSystem;
import org.apache.drill.test.BaseTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.LinkedList;
import java.util.List;

@Category(PlannerTest.class)
public class DrillOptiqTest extends BaseTest {

  /* Method checks if we raise the appropriate error while dealing with RexNode that cannot be converted to
   * equivalent Drill expressions
   */
  @Test
  public void testUnsupportedRexNode() {
    try {
      // Create the data type factory.
      RelDataTypeFactory relFactory = new SqlTypeFactoryImpl(DrillRelDataTypeSystem.DRILL_REL_DATATYPE_SYSTEM);
      // Create the rex builder
      RexBuilder rex = new RexBuilder(relFactory);
      RelDataType anyType = relFactory.createSqlType(SqlTypeName.ANY);
      List<RexNode> emptyList = new LinkedList<>();
      ImmutableList<RexFieldCollation> e = ImmutableList.copyOf(new RexFieldCollation[0]);

      // create a dummy RexOver object.
      RexNode window = rex.makeOver(anyType, SqlStdOperatorTable.AVG, emptyList, emptyList, GuavaUtils.convertToUnshadedImmutableList(e),
          null, null, true, false, false, false);
      DrillOptiq.toDrill(null, (RelNode) null, window);
    } catch (UserException e) {
      if (e.getMessage().contains(DrillOptiq.UNSUPPORTED_REX_NODE_ERROR)) {
        // got expected error return
        return;
      }
      Assert.fail("Hit exception with unexpected error message");
    }

    Assert.fail("Failed to raise the expected exception");
  }
}
