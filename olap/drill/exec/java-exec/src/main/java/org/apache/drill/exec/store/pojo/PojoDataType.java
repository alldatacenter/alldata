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
package org.apache.drill.exec.store.pojo;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.List;

import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.drill.exec.store.RecordDataType;

/**
 * This class uses reflection of a Java class to construct a {@link org.apache.drill.exec.store.RecordDataType}.
 */
public class PojoDataType extends RecordDataType {
//  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PojoDataType.class);

  private final List<SimpleImmutableEntry<SqlTypeName, Boolean>> types = new ArrayList<>();
  private final List<String> names = new ArrayList<>();
  private final Class<?> pojoClass;

  public PojoDataType(Class<?> pojoClass) {
    this.pojoClass = pojoClass;
    for (Field f : pojoClass.getDeclaredFields()) {
      if (Modifier.isStatic(f.getModifiers())) {
        continue;
      }

      Class<?> type = f.getType();
      names.add(f.getName());

      //Absence of annotation @NonNullable => (isNullable=true)
      final boolean isNullable = !(f.isAnnotationPresent(NonNullable.class));

      if (type == int.class || type == Integer.class) {
        types.add(new SimpleImmutableEntry<SqlTypeName, Boolean>(SqlTypeName.INTEGER, isNullable));
      } else if(type == boolean.class || type == Boolean.class) {
        types.add(new SimpleImmutableEntry<SqlTypeName, Boolean>(SqlTypeName.BOOLEAN, isNullable));
      } else if(type == long.class || type == Long.class) {
        types.add(new SimpleImmutableEntry<SqlTypeName, Boolean>(SqlTypeName.BIGINT, isNullable));
      } else if(type == double.class || type == Double.class) {
        types.add(new SimpleImmutableEntry<SqlTypeName, Boolean>(SqlTypeName.DOUBLE, isNullable));
      } else if(type == BigDecimal.class) {
        types.add(new SimpleImmutableEntry<SqlTypeName, Boolean>(SqlTypeName.DECIMAL, isNullable));
      } else if(type == String.class) {
        types.add(new SimpleImmutableEntry<SqlTypeName, Boolean>(SqlTypeName.VARCHAR, isNullable));
      } else if(type.isEnum()) {
        types.add(new SimpleImmutableEntry<SqlTypeName, Boolean>(SqlTypeName.VARCHAR, isNullable));
      } else if (type == Timestamp.class) {
        types.add(new SimpleImmutableEntry<SqlTypeName, Boolean>(SqlTypeName.TIMESTAMP, isNullable));
      } else {
        throw new RuntimeException(String.format("PojoDataType doesn't yet support conversions from type [%s].", type));
      }
    }
  }

  public Class<?> getPojoClass() {
    return pojoClass;
  }

  @Override
  public List<SimpleImmutableEntry<SqlTypeName, Boolean>> getFieldSqlTypeNames() {
    return types;
  }

  @Override
  public List<String> getFieldNames() {
    return names;
  }
}
