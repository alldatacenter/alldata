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
package org.apache.drill.exec.store.openTSDB;

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.planner.logical.DynamicDrillTable;
import org.apache.drill.exec.store.openTSDB.client.OpenTSDBTypes;
import org.apache.drill.exec.store.openTSDB.client.Schema;
import org.apache.drill.exec.store.openTSDB.dto.ColumnDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.apache.drill.exec.store.openTSDB.client.OpenTSDBTypes.DOUBLE;
import static org.apache.drill.exec.store.openTSDB.client.OpenTSDBTypes.STRING;
import static org.apache.drill.exec.store.openTSDB.client.OpenTSDBTypes.TIMESTAMP;

public class DrillOpenTSDBTable extends DynamicDrillTable {

  private static final Logger log =
          LoggerFactory.getLogger(DrillOpenTSDBTable.class);

  private final Schema schema;

  public DrillOpenTSDBTable(String storageEngineName, OpenTSDBStoragePlugin plugin, Schema schema, OpenTSDBScanSpec scanSpec) {
    super(plugin, storageEngineName, scanSpec);
    this.schema = schema;
  }

  @Override
  public RelDataType getRowType(final RelDataTypeFactory typeFactory) {
    List<String> names = Lists.newArrayList();
    List<RelDataType> types = Lists.newArrayList();
    convertToRelDataType(typeFactory, names, types);
    return typeFactory.createStructType(types, names);
  }

  private void convertToRelDataType(RelDataTypeFactory typeFactory, List<String> names, List<RelDataType> types) {
    for (ColumnDTO column : schema.getColumns()) {
      names.add(column.getColumnName());
      RelDataType type = getSqlTypeFromOpenTSDBType(typeFactory, column.getColumnType());
      type = typeFactory.createTypeWithNullability(type, column.isNullable());
      types.add(type);
    }
  }

  private RelDataType getSqlTypeFromOpenTSDBType(RelDataTypeFactory typeFactory, OpenTSDBTypes type) {
    switch (type) {
      case STRING:
        return typeFactory.createSqlType(SqlTypeName.VARCHAR, Integer.MAX_VALUE);
      case DOUBLE:
        return typeFactory.createSqlType(SqlTypeName.DOUBLE);
      case TIMESTAMP:
        return typeFactory.createSqlType(SqlTypeName.TIMESTAMP);
      default:
        throw UserException.unsupportedError()
                .message(String.format("%s is unsupported now. Currently supported types is %s, %s, %s", type, STRING, DOUBLE, TIMESTAMP))
                .build(log);
    }
  }
}
