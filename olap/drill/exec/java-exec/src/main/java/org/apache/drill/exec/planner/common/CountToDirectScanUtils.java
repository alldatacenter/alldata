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
package org.apache.drill.exec.planner.common;

import org.apache.calcite.rel.core.AggregateCall;
import org.apache.calcite.rel.core.Aggregate;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.apache.calcite.rel.type.RelDataTypeFieldImpl;
import org.apache.calcite.rel.type.RelRecordType;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.sql.type.SqlTypeName;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;

/**
 * A utility class that contains helper functions used by rules that convert COUNT(*) and COUNT(col)
 * aggregates (no group-by) to DirectScan
 */
public class CountToDirectScanUtils {

  /**
   * Checks if aggregate call contains star or non-null expression:
   * <pre>
   * count(*)  == >  empty arg  ==>  rowCount
   * count(Not-null-input) ==> rowCount
   * </pre>
   *
   * @param aggregateCall aggregate call
   * @param aggregate aggregate relation expression
   * @return true of aggregate call contains star or non-null expression
   */
  public static boolean containsStarOrNotNullInput(AggregateCall aggregateCall, Aggregate aggregate) {
    return aggregateCall.getArgList().isEmpty() ||
        (aggregateCall.getArgList().size() == 1 &&
            !aggregate.getInput().getRowType().getFieldList().get(aggregateCall.getArgList().get(0)).getType().isNullable());
  }

  /**
   * For each aggregate call creates field based on its name with bigint type.
   * Constructs record type for created fields.
   *
   * @param aggregateRel aggregate relation expression
   * @param fieldNames field names
   * @return record type
   */
  public static RelDataType constructDataType(Aggregate aggregateRel, Collection<String> fieldNames) {
    List<RelDataTypeField> fields = new ArrayList<>();
    int fieldIndex = 0;
    for (String name : fieldNames) {
      RelDataTypeField field = new RelDataTypeFieldImpl(
          name,
          fieldIndex++,
          aggregateRel.getCluster().getTypeFactory().createSqlType(SqlTypeName.BIGINT));
      fields.add(field);
    }
    return new RelRecordType(fields);
  }

  /**
   * Builds schema based on given field names.
   * Type for each schema is set to long.class.
   *
   * @param fieldNames field names
   * @return schema
   */
  public static LinkedHashMap<String, Class<?>> buildSchema(List<String> fieldNames) {
    LinkedHashMap<String, Class<?>> schema = new LinkedHashMap<>();
    for (String fieldName: fieldNames) {
      schema.put(fieldName, long.class);
    }
    return schema;
  }

  /**
   * For each field creates row expression.
   *
   * @param rowType row type
   * @return list of row expressions
   */
  public static List<RexNode> prepareFieldExpressions(RelDataType rowType) {
    List<RexNode> expressions = new ArrayList<>();
    for (int i = 0; i < rowType.getFieldCount(); i++) {
      expressions.add(RexInputRef.of(i, rowType));
    }
    return expressions;
  }

}
