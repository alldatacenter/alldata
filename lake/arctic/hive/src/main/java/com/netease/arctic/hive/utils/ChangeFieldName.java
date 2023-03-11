/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.hive.utils;

import com.google.common.collect.Sets;
import org.apache.iceberg.Schema;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.apache.iceberg.types.Type;
import org.apache.iceberg.types.TypeUtil;
import org.apache.iceberg.types.Types;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;

class ChangeFieldName extends TypeUtil.CustomOrderSchemaVisitor<Type> {

  enum ChangeType {
    TO_UPPERCASE, TO_LOWERCASE
  }

  private final ChangeType changeType;
  private final Set<String> fieldNameSet = Sets.newHashSet();

  /**
   * Change the filed name of a schema, change to uppercase or lowercase.
   *
   * @param changeType TO_UPPERCASE or TO_LOWERCASE
   */
  ChangeFieldName(ChangeType changeType) {
    this.changeType = changeType;
  }

  private String changeName(String name) {
    switch (changeType) {
      case TO_UPPERCASE:
        return name.toUpperCase(Locale.ROOT);
      case TO_LOWERCASE:
        return name.toLowerCase(Locale.ROOT);
      default:
        throw new UnsupportedOperationException("Unsupported change type: " + changeType);
    }
  }

  @Override
  public Type schema(Schema schema, Supplier<Type> future) {
    return future.get();
  }

  @Override
  public Type struct(Types.StructType struct, Iterable<Type> futures) {
    List<Types.NestedField> fields = struct.fields();
    int length = struct.fields().size();

    List<Types.NestedField> newFields = Lists.newArrayListWithExpectedSize(length);
    Iterator<Type> types = futures.iterator();
    for (int i = 0; i < length; i += 1) {
      Types.NestedField field = fields.get(i);
      Type type = types.next();
      if (field.isOptional()) {
        newFields.add(Types.NestedField.optional(field.fieldId(), changeName(field.name()), type, field.doc()));
      } else {
        newFields.add(Types.NestedField.required(field.fieldId(), changeName(field.name()), type, field.doc()));
      }
      if (fieldNameSet.contains(newFields.get(i).name())) {
        throw new IllegalArgumentException("Multiple fields' name will be changed to " + newFields.get(i).name());
      }
      fieldNameSet.add(newFields.get(i).name());
    }

    return Types.StructType.of(newFields);
  }

  @Override
  public Type field(Types.NestedField field, Supplier<Type> future) {
    return future.get();
  }

  @Override
  public Type list(Types.ListType list, Supplier<Type> future) {
    return list;
  }

  @Override
  public Type map(Types.MapType map, Supplier<Type> keyFuture, Supplier<Type> valueFuture) {
    return map;
  }

  @Override
  public Type primitive(Type.PrimitiveType primitive) {
    return primitive;
  }
}
