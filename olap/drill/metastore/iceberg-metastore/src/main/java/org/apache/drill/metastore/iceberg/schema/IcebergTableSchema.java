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
package org.apache.drill.metastore.iceberg.schema;

import org.apache.drill.metastore.MetastoreColumn;
import org.apache.drill.metastore.MetastoreFieldDefinition;
import org.apache.drill.metastore.iceberg.exceptions.IcebergMetastoreException;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableMap;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.types.Types;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Provides Iceberg table schema and its partition specification for specific component.
 */
public class IcebergTableSchema {

  private static final Logger logger = LoggerFactory.getLogger(IcebergTableSchema.class);

  public static final int STARTING_SCHEMA_INDEX = 1;
  public static final int STARTING_COMPLEX_TYPES_INDEX = 10_000;

  private static final Map<String, org.apache.iceberg.types.Type> JAVA_TO_ICEBERG_TYPE_MAP =
    ImmutableMap.<String, org.apache.iceberg.types.Type>builder()
      .put("string", Types.StringType.get())
      .put("int", Types.IntegerType.get())
      .put("integer", Types.IntegerType.get())
      .put("long", Types.LongType.get())
      .put("float", Types.FloatType.get())
      .put("double", Types.DoubleType.get())
      .put("boolean", Types.BooleanType.get())
      .build();

  private final Schema tableSchema;
  private final PartitionSpec partitionSpec;

  public IcebergTableSchema(Schema tableSchema, PartitionSpec partitionSpec) {
    this.tableSchema = tableSchema;
    this.partitionSpec = partitionSpec;
  }

  /**
   * Based on given class fields annotated with {@link MetastoreFieldDefinition}
   * generates Iceberg table schema and its partition specification.
   *
   * @param clazz base class for Iceberg schema
   * @param partitionKeys list of partition keys
   * @return instance of Iceberg table schema
   */
  public static IcebergTableSchema of(Class<?> clazz, List<MetastoreColumn> partitionKeys) {
    List<Types.NestedField> tableSchemaFields = new ArrayList<>();
    Types.NestedField[] partitionSpecSchemaFields = new Types.NestedField[partitionKeys.size()];

    int schemaIndex = STARTING_SCHEMA_INDEX;
    int complexTypesIndex = STARTING_COMPLEX_TYPES_INDEX;

    for (Field field : clazz.getDeclaredFields()) {
      MetastoreFieldDefinition definition = field.getAnnotation(MetastoreFieldDefinition.class);
      if (definition == null) {
        continue;
      }

      MetastoreColumn column = definition.column();

      String typeSimpleName = field.getType().getSimpleName().toLowerCase();
      org.apache.iceberg.types.Type icebergType = JAVA_TO_ICEBERG_TYPE_MAP.get(typeSimpleName);

      if (icebergType == null && field.getAnnotatedType().getType() instanceof ParameterizedType) {
        Type[] actualTypeArguments = ((ParameterizedType) field.getAnnotatedType().getType()).getActualTypeArguments();
        switch (typeSimpleName) {
          case "list":
            org.apache.iceberg.types.Type listIcebergType = getGenericsType(actualTypeArguments[0]);
            icebergType = Types.ListType.ofOptional(complexTypesIndex++, listIcebergType);
            break;
          case "map":
            org.apache.iceberg.types.Type keyIcebergType = getGenericsType(actualTypeArguments[0]);
            org.apache.iceberg.types.Type valueIcebergType = getGenericsType(actualTypeArguments[1]);
            icebergType = Types.MapType.ofOptional(complexTypesIndex++, complexTypesIndex++, keyIcebergType, valueIcebergType);
            break;
          default:
            throw new IcebergMetastoreException(String.format(
              "Unexpected parametrized type for class [%s]: %s", clazz.getCanonicalName(), typeSimpleName));
        }
      }

      if (icebergType == null) {
        throw new IcebergMetastoreException(String.format(
          "Unexpected type for class [%s]: %s", clazz.getCanonicalName(), typeSimpleName));
      }

      Types.NestedField icebergField = Types.NestedField.optional(schemaIndex++, column.columnName(), icebergType);

      tableSchemaFields.add(icebergField);

      int partitionIndex = partitionKeys.indexOf(column);
      if (partitionIndex != -1) {
        partitionSpecSchemaFields[partitionIndex] = icebergField;
      }
    }

    if (Stream.of(partitionSpecSchemaFields).anyMatch(Objects::isNull)) {
      throw new IcebergMetastoreException(String.format(
        "Some of partition fields are missing in the class [%s]. Partition keys: %s. Partition values: %s.",
        clazz.getCanonicalName(), partitionKeys, Arrays.asList(partitionSpecSchemaFields)));
    }

    Schema tableSchema = new Schema(tableSchemaFields);
    PartitionSpec partitionSpec = buildPartitionSpec(partitionSpecSchemaFields);
    logger.debug("Constructed Iceberg table schema for class [{}]. Table schema : {}. Partition spec: {}.",
      clazz.getCanonicalName(), tableSchema, partitionSpec);
    return new IcebergTableSchema(tableSchema, partitionSpec);
  }

  public Schema tableSchema() {
    return tableSchema;
  }

  public PartitionSpec partitionSpec() {
    return partitionSpec;
  }

  private static org.apache.iceberg.types.Type getGenericsType(Type type) {
    if (!(type instanceof Class)) {
      throw new IcebergMetastoreException("Unexpected generics type: " + type.getTypeName());
    }
    Class<?> typeArgument = (Class<?>) type;
    String typeSimpleName = typeArgument.getSimpleName().toLowerCase();
    org.apache.iceberg.types.Type icebergType = JAVA_TO_ICEBERG_TYPE_MAP.get(typeSimpleName);
    if (icebergType == null) {
      throw new IcebergMetastoreException("Unexpected type: " + typeSimpleName);
    }
    return icebergType;
  }

  private static PartitionSpec buildPartitionSpec(Types.NestedField[] partitionFields) {
    Schema schema = new Schema(partitionFields);
    if (schema.columns().isEmpty()) {
      return PartitionSpec.unpartitioned();
    }
    PartitionSpec.Builder builder = PartitionSpec.builderFor(schema);
    schema.columns()
      .forEach(column -> builder.identity(column.name()));
    return builder.build();
  }
}
