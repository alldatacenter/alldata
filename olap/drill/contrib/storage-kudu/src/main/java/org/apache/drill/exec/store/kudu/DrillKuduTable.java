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
package org.apache.drill.exec.store.kudu;

import java.util.List;

import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.drill.exec.planner.logical.DynamicDrillTable;
import org.apache.kudu.ColumnSchema;
import org.apache.kudu.Schema;
import org.apache.kudu.Type;

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;

public class DrillKuduTable extends DynamicDrillTable {

  private final Schema schema;

  public DrillKuduTable(String storageEngineName, KuduStoragePlugin plugin, Schema schema, KuduScanSpec scanSpec) {
    super(plugin, storageEngineName, scanSpec);
    this.schema = schema;
  }

  @Override
  public RelDataType getRowType(RelDataTypeFactory typeFactory) {

    List<String> names = Lists.newArrayList();
    List<RelDataType> types = Lists.newArrayList();
    for (ColumnSchema column : schema.getColumns()) {
      names.add(column.getName());
      RelDataType type = getSqlTypeFromKuduType(typeFactory, column.getType());
      type = typeFactory.createTypeWithNullability(type, column.isNullable());
      types.add(type);
    }

    return typeFactory.createStructType(types, names);
  }

  private RelDataType getSqlTypeFromKuduType(RelDataTypeFactory typeFactory, Type type) {
    switch (type) {
    case BOOL:
      return typeFactory.createSqlType(SqlTypeName.BOOLEAN);
    case DOUBLE:
      return typeFactory.createSqlType(SqlTypeName.DOUBLE);
    case FLOAT:
      return typeFactory.createSqlType(SqlTypeName.FLOAT);
    case INT16:
    case INT32:
    case INT64:
    case INT8:
      return typeFactory.createSqlType(SqlTypeName.INTEGER);
    case STRING:
      return typeFactory.createSqlType(SqlTypeName.VARCHAR);
    case UNIXTIME_MICROS:
      return typeFactory.createSqlType(SqlTypeName.TIMESTAMP);
    case BINARY:
      return typeFactory.createSqlType(SqlTypeName.VARBINARY, Integer.MAX_VALUE);
    default:
      throw new UnsupportedOperationException("Unsupported type.");
    }
  }
}
