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
package org.apache.drill.exec.planner.sql.handlers;

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.shaded.guava.com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.apache.calcite.rex.RexBuilder;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.schema.Table;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.SqlWriter;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.tools.RelConversionException;
import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.planner.common.DrillRelOptUtil;
import org.apache.drill.exec.planner.logical.DrillRelFactories;
import org.apache.drill.exec.store.AbstractSchema;

import org.apache.calcite.tools.ValidationException;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.type.RelDataType;

import java.io.IOException;
import java.util.AbstractList;
import java.util.HashSet;
import java.util.List;

public class SqlHandlerUtil {
  private static final Logger logger = LoggerFactory.getLogger(SqlHandlerUtil.class);

  /**
   * Resolve final RelNode of the new table (or view) for given table field list and new table definition.
   *
   * @param isNewTableView Is the new table created a view? This doesn't affect the functionality, but it helps format
   *                       better error messages.
   * @param tableFieldNames List of fields specified in new table/view field list. These are the fields given just after
   *                        new table name.
   *                        Ex. CREATE TABLE newTblName(col1, medianOfCol2, avgOfCol3) AS
   *                        SELECT col1, median(col2), avg(col3) FROM sourcetbl GROUP BY col1;
   * @throws ValidationException If table's fields list and field list specified in table definition are not valid.
   * @throws RelConversionException If failed to convert the table definition into a RelNode.
   */
  public static RelNode resolveNewTableRel(boolean isNewTableView, List<String> tableFieldNames,
      RelDataType validatedRowtype, RelNode queryRelNode) throws ValidationException, RelConversionException {


//    TypedSqlNode validatedSqlNodeWithType = planner.validateAndGetType(newTableQueryDef);

    // Get the row type of view definition query.
    // Reason for getting the row type from validated SqlNode than RelNode is because SqlNode -> RelNode involves
    // renaming duplicate fields which is not desired when creating a view or table.
    // For ex: SELECT region_id, region_id FROM cp.`region.json` LIMIT 1 returns
    //  +------------+------------+
    //  | region_id  | region_id0 |
    //  +------------+------------+
    //  | 0          | 0          |
    //  +------------+------------+
    // which is not desired when creating new views or tables.
//    final RelDataType queryRowType = validatedRowtype;

    if (tableFieldNames.size() > 0) {
      // Field count should match.
      if (tableFieldNames.size() != validatedRowtype.getFieldCount()) {
        final String tblType = isNewTableView ? "view" : "table";
        throw UserException.validationError()
            .message("%s's field list and the %s's query field list have different counts.", tblType, tblType)
            .build(logger);
      }

      // CTAS's query field list shouldn't have "**" when table's field list is specified.
      for (String field : validatedRowtype.getFieldNames()) {
        if (SchemaPath.DYNAMIC_STAR.equals(field)) {
          final String tblType = isNewTableView ? "view" : "table";
          throw UserException.validationError()
              .message("%s's query field list has a '*', which is invalid when %s's field list is specified.",
                  tblType, tblType)
              .build(logger);
        }
      }

      // validate the given field names to make sure there are no duplicates
      ensureNoDuplicateColumnNames(tableFieldNames);

      // CTAS statement has table field list (ex. below), add a project rel to rename the query fields.
      // Ex. CREATE TABLE tblname(col1, medianOfCol2, avgOfCol3) AS
      //        SELECT col1, median(col2), avg(col3) FROM sourcetbl GROUP BY col1 ;
      // Similary for CREATE VIEW.

      return DrillRelOptUtil.createRename(queryRelNode, tableFieldNames);
    }

    // As the column names of the view are derived from SELECT query, make sure the query has no duplicate column names
    ensureNoDuplicateColumnNames(validatedRowtype.getFieldNames());

    return queryRelNode;
  }

  private static void ensureNoDuplicateColumnNames(List<String> fieldNames) throws ValidationException {
    final HashSet<String> fieldHashSet = Sets.newHashSetWithExpectedSize(fieldNames.size());
    for(String field : fieldNames) {
      if (fieldHashSet.contains(field.toLowerCase())) {
        throw new ValidationException(String.format("Duplicate column name [%s]", field));
      }
      fieldHashSet.add(field.toLowerCase());
    }
  }

  /**
   *  Resolve the partition columns specified in "PARTITION BY" clause of CTAS statement.
   *
   *  A partition column is resolved, either (1) the same column appear in the select list of CTAS
   *  or (2) CTAS has a * in select list.
   *
   *  In the second case, a PROJECT with ITEM expression would be created and returned.
   *  Throw validation error if a partition column is not resolved correctly.
   *
   * @param input : the RelNode represents the select statement in CTAS.
   * @param partitionColumns : the list of partition columns.
   * @return : 1) the original RelNode input, if all partition columns are in select list of CTAS
   *           2) a New Project, if a partition column is resolved to * column in select list
   *           3) validation error, if partition column is not resolved.
   */
  public static RelNode qualifyPartitionCol(RelNode input, List<String> partitionColumns) {

    final RelDataType inputRowType = input.getRowType();

    final List<RexNode> colRefStarExprs = Lists.newArrayList();
    final List<String> colRefStarNames = Lists.newArrayList();
    final RexBuilder builder = input.getCluster().getRexBuilder();
    final int originalFieldSize = inputRowType.getFieldCount();

    for (final String col : partitionColumns) {
      final RelDataTypeField field = inputRowType.getField(col, false, false);

      if (field == null) {
        throw UserException.validationError()
            .message("Partition column %s is not in the SELECT list of CTAS!", col)
            .build(logger);
      } else {
        if (SchemaPath.DYNAMIC_STAR.equals(field.getName())) {
          colRefStarNames.add(col);

          final List<RexNode> operands = Lists.newArrayList();
          operands.add(new RexInputRef(field.getIndex(), field.getType()));
          operands.add(builder.makeLiteral(col));
          final RexNode item = builder.makeCall(SqlStdOperatorTable.ITEM, operands);
          colRefStarExprs.add(item);
        }
      }
    }

    if (colRefStarExprs.isEmpty()) {
      return input;
    } else {
      final List<String> names =
          new AbstractList<String>() {
            @Override
            public String get(int index) {
              if (index < originalFieldSize) {
                return inputRowType.getFieldNames().get(index);
              } else {
                return colRefStarNames.get(index - originalFieldSize);
              }
            }

            @Override
            public int size() {
              return originalFieldSize + colRefStarExprs.size();
            }
          };

      final List<RexNode> refs =
          new AbstractList<RexNode>() {
            @Override
            public int size() {
              return originalFieldSize + colRefStarExprs.size();
            }

            @Override
            public RexNode get(int index) {
              if (index < originalFieldSize) {
                return RexInputRef.of(index, inputRowType.getFieldList());
              } else {
                return colRefStarExprs.get(index - originalFieldSize);
              }
            }
          };

      return DrillRelFactories.LOGICAL_BUILDER
          .create(input.getCluster(), null)
          .push(input)
          .projectNamed(refs, names, true)
          .build();
    }
  }

  public static Table getTableFromSchema(AbstractSchema drillSchema, String tblName) {
    try {
      return drillSchema.getTable(tblName);
    } catch (Exception e) {
      // TODO: Move to better exception types.
      throw new DrillRuntimeException(
          String.format("Failure while trying to check if a table or view with given name [%s] already exists " +
              "in schema [%s]: %s", tblName, drillSchema.getFullSchemaName(), e.getMessage()), e);
    }
  }

  public static void unparseSqlNodeList(SqlWriter writer, int leftPrec, int rightPrec, SqlNodeList fieldList) {
    writer.keyword("(");
    fieldList.get(0).unparse(writer, leftPrec, rightPrec);
    for (int i = 1; i<fieldList.size(); i++) {
      writer.keyword(",");
      fieldList.get(i).unparse(writer, leftPrec, rightPrec);
    }
    writer.keyword(")");
  }

  /**
   * Drops table from schema.
   * If drop has failed makes concurrency check: checks if table still exists.
   * If table exists, throws {@link org.apache.drill.common.exceptions.UserException} since drop was unsuccessful,
   * otherwise assumes that other user had dropped the view and exists without error.
   *
   * @param drillSchema drill schema
   * @param tableName table name
   */
  public static void dropTableFromSchema(AbstractSchema drillSchema, String tableName) {
    try {
      drillSchema.dropTable(tableName);
    } catch (Exception e) {
      if (SqlHandlerUtil.getTableFromSchema(drillSchema, tableName) != null) {
        throw e;
      }
    }
  }

  /**
   * Drops view from schema.
   * If drop has failed makes concurrency check: checks if view still exists.
   * If view exists, throws {@link org.apache.drill.common.exceptions.UserException} since drop was unsuccessful,
   * otherwise assumes that other user had dropped the view and exists without error.
   *
   * @param drillSchema drill schema
   * @param viewName view name
   */
  public static void dropViewFromSchema(AbstractSchema drillSchema, String viewName) throws IOException {
    try {
      drillSchema.dropView(viewName);
    } catch (Exception e) {
      if (SqlHandlerUtil.getTableFromSchema(drillSchema, viewName) != null) {
        throw e;
      }
    }
  }

  /**
   * Unparses given {@link SqlNodeList} into key / values pairs: (k1 = v1, k2 = v2).
   *
   * @param writer sql writer
   * @param leftPrec left precedence
   * @param rightPrec right precedence
   * @param list sql node list
   */
  public static void unparseKeyValuePairs(SqlWriter writer, int leftPrec, int rightPrec, SqlNodeList list) {
    writer.keyword("(");

    for (int i = 1; i < list.size(); i += 2) {
      if (i != 1) {
        writer.keyword(",");
      }
      list.get(i - 1).unparse(writer, leftPrec, rightPrec);
      writer.keyword("=");
      list.get(i).unparse(writer, leftPrec, rightPrec);
    }

    writer.keyword(")");
  }
}
