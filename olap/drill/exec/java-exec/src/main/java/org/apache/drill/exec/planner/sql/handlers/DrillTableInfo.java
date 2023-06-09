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

import org.apache.calcite.schema.Table;
import org.apache.calcite.schema.TranslatableTable;
import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlFunction;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlOperator;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.sql.validate.SqlUserDefinedTableMacro;
import org.apache.calcite.util.Util;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.planner.logical.DrillTable;
import org.apache.drill.exec.planner.logical.DrillTranslatableTable;
import org.apache.drill.exec.planner.sql.SchemaUtilites;
import org.apache.drill.exec.store.AbstractSchema;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Holder class for {@link DrillTable}, {@code tableName} and table {@code schemaPath} obtained from
 * {@code SqlNode tableRef}.
 */
public class DrillTableInfo {
  private static final Logger logger = LoggerFactory.getLogger(DrillTableInfo.class);

  private final DrillTable drillTable;
  private final String tableName;
  private final List<String> schemaPath;

  private DrillTableInfo(DrillTable drillTable, List<String> schemaPath, String tableName) {
    this.drillTable = drillTable;
    this.tableName = tableName;
    this.schemaPath = schemaPath;
  }

  public DrillTable drillTable() {
    return drillTable;
  }

  public String tableName() {
    return tableName;
  }

  public List<String> schemaPath() {
    return schemaPath;
  }

  /**
   * Returns {@link DrillTableInfo} instance which holds {@link DrillTable}, {@code drillTable},
   * {@code schemaPath} corresponding to specified {@code tableRef}.
   *
   * @param tableRef table ref
   * @param config   handler config
   * @return {@link DrillTableInfo} instance
   */
  public static DrillTableInfo getTableInfoHolder(SqlNode tableRef, SqlHandlerConfig config) {
    switch (tableRef.getKind()) {
      case COLLECTION_TABLE: {
        SqlCall call = (SqlCall) config.getConverter().validate(tableRef);
        assert call.getOperandList().size() == 1;
        SqlOperator operator = ((SqlCall) call.operand(0)).getOperator();
        assert operator instanceof SqlUserDefinedTableMacro;
        SqlUserDefinedTableMacro tableMacro = (SqlUserDefinedTableMacro) operator;
        SqlIdentifier tableIdentifier = tableMacro.getSqlIdentifier();

        AbstractSchema drillSchema = SchemaUtilites.resolveToDrillSchema(
            config.getConverter().getDefaultSchema(), SchemaUtilites.getSchemaPath(tableIdentifier));

        TranslatableTable translatableTable = tableMacro.getTable(config.getConverter().getTypeFactory(), prepareTableMacroOperands(call.operand(0)));
        DrillTable table = ((DrillTranslatableTable) translatableTable).getDrillTable();
        return new DrillTableInfo(table, drillSchema.getSchemaPath(), Util.last(tableIdentifier.names));
      }
      case IDENTIFIER: {
        SqlIdentifier tableIdentifier = (SqlIdentifier) tableRef;
        AbstractSchema drillSchema = SchemaUtilites.resolveToDrillSchema(
            config.getConverter().getDefaultSchema(), SchemaUtilites.getSchemaPath(tableIdentifier));
        String tableName = Util.last(tableIdentifier.names);
        DrillTable table = getDrillTable(drillSchema, tableName);
        return new DrillTableInfo(table, drillSchema.getSchemaPath(), tableName);
      }
      default:
        throw new UnsupportedOperationException("Unsupported table ref kind: " + tableRef.getKind());
    }
  }

  /**
   * Returns list with operands for table function, obtained from specified call in the order
   * suitable to be used in table function and default values for absent arguments.
   * For example, for the following call:
   * <pre>
   *   `dfs`.`corrupted_dates`(`type` => 'parquet', `autoCorrectCorruptDates` => FALSE, `enableStringsSignedMinMax` => FALSE)
   * </pre>
   * will be returned the following list:
   * <pre>
   *   ['parquet', FALSE, FALSE, DEFAULT]
   * </pre>
   * whose elements correspond to the following parameters:
   * <pre>
   *   [type, autoCorrectCorruptDates, enableStringsSignedMinMax, schema]
   * </pre>
   *
   * @param call sql call whose arguments should be prepared
   * @return list with operands for table function
   */
  private static List<SqlNode> prepareTableMacroOperands(SqlCall call) {
    Function<String, SqlNode> convertOperand = paramName -> call.getOperandList().stream()
        .map(sqlNode -> (SqlCall) sqlNode)
        .filter(sqlCall -> ((SqlIdentifier) sqlCall.operand(1)).getSimple().equals(paramName))
        .peek(sqlCall -> Preconditions.checkState(sqlCall.getKind() == SqlKind.ARGUMENT_ASSIGNMENT))
        .findFirst()
        .map(sqlCall -> (SqlNode) sqlCall.operand(0))
        .orElse(SqlStdOperatorTable.DEFAULT.createCall(SqlParserPos.ZERO));

    SqlFunction operator = (SqlFunction) call.getOperator();

    return operator.getParamNames().stream()
        .map(convertOperand)
        .collect(Collectors.toList());
  }

  private static DrillTable getDrillTable(AbstractSchema drillSchema, String tableName) {
    Table tableFromSchema = SqlHandlerUtil.getTableFromSchema(drillSchema, tableName);

    if (tableFromSchema == null) {
      throw UserException.validationError()
          .message("No table with given name [%s] exists in schema [%s]", tableName, drillSchema.getFullSchemaName())
          .build(logger);
    }

    switch (tableFromSchema.getJdbcTableType()) {
      case TABLE:
      case SYSTEM_TABLE:
        if (tableFromSchema instanceof DrillTable) {
          return (DrillTable) tableFromSchema;
        } else {
          throw UserException.validationError()
              .message("[%s] table kind is not supported", tableFromSchema.getClass().getSimpleName())
              .build(logger);
        }
      default:
        throw UserException.validationError()
            .message("[%s] object type is not supported", tableFromSchema.getJdbcTableType())
            .build(logger);
    }
  }
}
