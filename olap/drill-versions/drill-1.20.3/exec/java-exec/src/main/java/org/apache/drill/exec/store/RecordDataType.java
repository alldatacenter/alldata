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
package org.apache.drill.exec.store;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.List;

import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.sql.type.SqlTypeName;

/**
 * Defines names and data types of columns in a static drill table.
 */
public abstract class RecordDataType {

  /**
   * @return the {@link org.apache.calcite.sql.type.SqlTypeName} of columns in the table as a pair with its nullability
   */
  public abstract List<SimpleImmutableEntry<SqlTypeName, Boolean>> getFieldSqlTypeNames();

  /**
   * @return the column names in the table
   */
  public abstract List<String> getFieldNames();

  /**
   * This method constructs a {@link org.apache.calcite.rel.type.RelDataType} based on the
   * {@link org.apache.drill.exec.store.RecordDataType}'s field sql types and field names.
   *
   * @param factory helps construct a {@link org.apache.calcite.rel.type.RelDataType}
   * @return the constructed type
   */
  public final RelDataType getRowType(RelDataTypeFactory factory) {
    final List<SimpleImmutableEntry<SqlTypeName, Boolean>> types = getFieldSqlTypeNames();
    final List<String> names = getFieldNames();
    final List<RelDataType> fields = new ArrayList<>();
    for (SimpleImmutableEntry<SqlTypeName, Boolean> sqlTypePair : types) {
      final SqlTypeName typeName = sqlTypePair.getKey();
      final RelDataType tempDataType;
      switch (typeName) {
        case VARCHAR:
          tempDataType = factory.createSqlType(typeName, Integer.MAX_VALUE);
          break;
        default:
          tempDataType = factory.createSqlType(typeName);
      }
      //Add [Non]Nullable RelDataType
      fields.add(factory.createTypeWithNullability(tempDataType, sqlTypePair.getValue()));
    }
    return factory.createStructType(fields, names);
  }
}
