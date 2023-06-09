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
package org.apache.drill.exec.store.avro;

import org.apache.avro.LogicalType;
import org.apache.avro.LogicalTypes;
import org.apache.avro.Schema;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.DictBuilder;
import org.apache.drill.exec.record.metadata.MapBuilder;
import org.apache.drill.exec.record.metadata.MetadataUtils;
import org.apache.drill.exec.record.metadata.RepeatedListBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.record.metadata.TupleSchema;
import org.apache.drill.exec.vector.complex.DictVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility class that provides methods to interact with Avro schema.
 */
public class AvroSchemaUtil {

  private static final Logger logger = LoggerFactory.getLogger(AvroSchemaUtil.class);

  public static final String AVRO_LOGICAL_TYPE_PROPERTY = "avro_logical_type";

  public static final String DECIMAL_LOGICAL_TYPE = "decimal";
  public static final String TIMESTAMP_MICROS_LOGICAL_TYPE = "timestamp-micros";
  public static final String TIMESTAMP_MILLIS_LOGICAL_TYPE = "timestamp-millis";
  public static final String DATE_LOGICAL_TYPE = "date";
  public static final String TIME_MICROS_LOGICAL_TYPE = "time-micros";
  public static final String TIME_MILLIS_LOGICAL_TYPE = "time-millis";
  public static final String DURATION_LOGICAL_TYPE = "duration";

  /**
   * Converts Avro schema into Drill metadata description of the schema.
   *
   * @param schema Avro schema
   * @return metadata description of the schema
   * @throws UserException if schema contains unsupported types
   */
  public static TupleMetadata convert(Schema schema) {
    return SchemaConverter.INSTANCE.convert(schema);
  }

  /**
   * Avro represents nullable type as union of null and another schema: ["null", "some-type"].
   * This method extracts non-nullable schema for given union schema.
   *
   * @param schema Avro schema
   * @param columnName column name
   * @return non-nullable Avro schema
   * @throws UserException if given schema is not a union or represents complex union
   */
  public static Schema extractSchemaFromNullable(Schema schema, String columnName) {
    if (!schema.isUnion()) {
      throw UserException.validationError()
        .message("Expected union type, but received: %s", schema.getType())
        .addContext("Column", columnName)
        .build(logger);
    }
    List<Schema> unionSchemas = schema.getTypes();

    // exclude all schemas with null type
    List<Schema> nonNullSchemas = unionSchemas.stream()
      .filter(unionSchema -> !Schema.Type.NULL.equals(unionSchema.getType()))
      .collect(Collectors.toList());

    // if original schema has two elements and only one non-nullable schema, this is simple nullable type
    if (unionSchemas.size() == 2 && nonNullSchemas.size() == 1) {
      return nonNullSchemas.get(0);
    } else {
      return throwUnsupportedErrorForType("complex union", columnName);
    }
  }

  private static <T> T throwUnsupportedErrorForType(String type, String columnName) {
    throw UserException.unsupportedError()
      .message("'%s' type is not supported", type)
      .addContext("Column", columnName)
      .build(logger);
  }

  /**
   * Class is responsible for converting Avro schema into Drill metadata description of the schema.
   * It does not hold state and thus is thread-safe.
   */
  private static class SchemaConverter {

    private static final SchemaConverter INSTANCE = new SchemaConverter();

    TupleMetadata convert(Schema schema) {
      /*
        Avro allows to reference types by name, sometimes reference can be done to the type under construction.
        For example, a linked-list of 64-bit values:
        {
          "type": "record",
          "name": "LongList",
          "fields" : [
             {"name": "value", "type": "long"},             // each element has a long
             {"name": "next", "type": ["null", "LongList"]} // optional next element
           ]
        }

        Since we cannot build record type which is not constructed yet, when such situation is detected,
        record type is set to Drill Map without columns, columns will be detected when reading actual data.

        `typeNamesUnderConstruction` is a holder to store record type names under construction to detect
         reference to the types which are not yet constructed.
       */
      Set<String> typeNamesUnderConstruction = new HashSet<>();
      TupleSchema tupleSchema = new TupleSchema();

      // add current record type to the set of types under construction
      typeNamesUnderConstruction.add(schema.getFullName());

      List<Schema.Field> fields = schema.getFields();
      fields.stream()
        .map(field -> convert(field, typeNamesUnderConstruction))
        .forEach(tupleSchema::add);
      return tupleSchema;
    }

    private ColumnMetadata convert(Schema.Field field, Set<String> typeNamesUnderConstruction) {
      Schema fieldSchema = field.schema();
      return defineColumn(field.name(), fieldSchema, TypeProtos.DataMode.REQUIRED, typeNamesUnderConstruction);
    }

    private ColumnMetadata defineColumn(String name, Schema fieldSchema,
                                        TypeProtos.DataMode mode,
                                        Set<String> typeNamesUnderConstruction) {
      String logicalTypeName = getLogicalTypeName(fieldSchema);
      switch (fieldSchema.getType()) {
        case INT:
          switch (logicalTypeName) {
            case DATE_LOGICAL_TYPE:
              return initField(name, TypeProtos.MinorType.DATE, mode);
            case TIME_MILLIS_LOGICAL_TYPE:
              return initField(name, TypeProtos.MinorType.TIME, mode);
            default:
              return initField(name, TypeProtos.MinorType.INT, mode);
          }
        case LONG:
          switch (logicalTypeName) {
            case TIMESTAMP_MICROS_LOGICAL_TYPE:
            case TIMESTAMP_MILLIS_LOGICAL_TYPE:
              ColumnMetadata timestampColumn = initField(name, TypeProtos.MinorType.TIMESTAMP, mode);
              // add avro logical type property to know how to convert timestamp value
              timestampColumn.setProperty(AVRO_LOGICAL_TYPE_PROPERTY, logicalTypeName);
              return timestampColumn;
            case TIME_MICROS_LOGICAL_TYPE:
              return initField(name, TypeProtos.MinorType.TIME, mode);
            default:
              return initField(name, TypeProtos.MinorType.BIGINT, mode);
          }
        case FLOAT:
          return initField(name, TypeProtos.MinorType.FLOAT4, mode);
        case DOUBLE:
          return initField(name, TypeProtos.MinorType.FLOAT8, mode);
        case FIXED:
          if (DURATION_LOGICAL_TYPE.equals(logicalTypeName)) {
            return initField(name, TypeProtos.MinorType.INTERVAL, mode);
          }
          // fall through
        case BYTES:
          if (DECIMAL_LOGICAL_TYPE.equals(logicalTypeName)) {
            LogicalTypes.Decimal decimalLogicalType = (LogicalTypes.Decimal) fieldSchema.getLogicalType();
            TypeProtos.MajorType majorType = Types.withPrecisionAndScale(TypeProtos.MinorType.VARDECIMAL,
              mode, decimalLogicalType.getPrecision(), decimalLogicalType.getScale());
            return initField(name, majorType);
          } else {
            return initField(name, TypeProtos.MinorType.VARBINARY, mode);
          }
        case BOOLEAN:
          return initField(name, TypeProtos.MinorType.BIT, mode);
        case ENUM:
        case STRING:
          return initField(name, TypeProtos.MinorType.VARCHAR, mode);
        case NULL:
          return initField(name, TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL);
        case UNION:
          Schema schema = extractSchemaFromNullable(fieldSchema, name);
          TypeProtos.DataMode nullableMode =
            TypeProtos.DataMode.REPEATED == mode ? TypeProtos.DataMode.REPEATED : TypeProtos.DataMode.OPTIONAL;
          return defineColumn(name, schema, nullableMode, typeNamesUnderConstruction);
        case RECORD:
          MapBuilder recordBuilder = new MapBuilder(null, name, mode);
          String typeName = fieldSchema.getFullName();
          // if type name is not under construction, proceed adding columns
          // otherwise leave type as Drill Map without columns
          if (typeNamesUnderConstruction.add(typeName)) {
            fieldSchema.getFields().stream()
              .map(field -> convert(field, typeNamesUnderConstruction))
              .forEach(recordBuilder::addColumn);
            // remove record type name since it was constructed
            typeNamesUnderConstruction.remove(typeName);
          }
          return recordBuilder.buildColumn();
        case ARRAY:
          Schema elementSchema = fieldSchema.getElementType();
          boolean hasNestedArray = elementSchema.isUnion()
            ? Schema.Type.ARRAY == extractSchemaFromNullable(elementSchema, name).getType()
            : Schema.Type.ARRAY == elementSchema.getType();

          if (hasNestedArray) {
            RepeatedListBuilder builder = new RepeatedListBuilder(null, name);
            builder.addColumn(defineColumn(name, elementSchema, TypeProtos.DataMode.REQUIRED, typeNamesUnderConstruction));
            return builder.buildColumn();
          }
          return defineColumn(name, elementSchema, TypeProtos.DataMode.REPEATED, typeNamesUnderConstruction);
        case MAP:
          DictBuilder dictBuilder = new DictBuilder(null, name, mode);
          // per Avro specification Map key is always of varchar type
          dictBuilder.key(TypeProtos.MinorType.VARCHAR);
          Schema valueSchema = fieldSchema.getValueType();
          ColumnMetadata valueColumn = defineColumn(DictVector.FIELD_VALUE_NAME, valueSchema,
            TypeProtos.DataMode.REQUIRED, typeNamesUnderConstruction);
          dictBuilder.addColumn(valueColumn);
          return dictBuilder.buildColumn();
        default:
          return throwUnsupportedErrorForType(fieldSchema.getType().getName(), name);
      }
    }

    /**
     * Identifies logical type name for the column if present.
     * Column schema can have logical type set as object or through property.
     *
     * @param schema Avro column schema
     * @return logical type name if present, empty string otherwise
     */
    private String getLogicalTypeName(Schema schema) {
      String name = schema.getLogicalType() != null
        ? schema.getLogicalType().getName()
        : schema.getProp(LogicalType.LOGICAL_TYPE_PROP);
      return name == null ? "" : name;
    }

    private ColumnMetadata initField(String name, TypeProtos.MinorType minorType, TypeProtos.DataMode mode) {
      TypeProtos.MajorType majorType = Types.withMode(minorType, mode);
      return initField(name, majorType);
    }

    private ColumnMetadata initField(String name, TypeProtos.MajorType majorType) {
      MaterializedField materializedField = MaterializedField.create(name, majorType);
      return MetadataUtils.fromField(materializedField);
    }
  }
}
