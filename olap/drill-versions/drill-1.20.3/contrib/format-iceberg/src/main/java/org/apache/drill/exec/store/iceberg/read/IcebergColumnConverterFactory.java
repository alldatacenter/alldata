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
package org.apache.drill.exec.store.iceberg.read;

import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.record.ColumnConverter;
import org.apache.drill.exec.record.ColumnConverterFactory;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.DictColumnMetadata;
import org.apache.drill.exec.record.metadata.MetadataUtils;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.record.metadata.TupleSchema;
import org.apache.drill.exec.vector.accessor.TupleWriter;
import org.apache.drill.exec.vector.accessor.ValueWriter;
import org.apache.drill.exec.vector.complex.DictVector;
import org.apache.iceberg.types.Type;
import org.apache.iceberg.types.Types;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class IcebergColumnConverterFactory extends ColumnConverterFactory {

  public IcebergColumnConverterFactory(TupleMetadata providedSchema) {
    super(providedSchema);
  }

  @Override
  protected ColumnConverter getMapConverter(TupleMetadata providedSchema,
    TupleMetadata readerSchema, TupleWriter tupleWriter) {
    Map<String, ColumnConverter> converters = StreamSupport.stream(readerSchema.spliterator(), false)
      .collect(Collectors.toMap(
        ColumnMetadata::name,
        columnMetadata ->
          getConverter(providedSchema, columnMetadata, tupleWriter.column(columnMetadata.name()))));

    return new MapColumnConverter(this, providedSchema, tupleWriter, converters);
  }

  @Override
  public ColumnConverter.ScalarColumnConverter buildScalar(ColumnMetadata readerSchema, ValueWriter writer) {
    switch (readerSchema.type()) {
      case BIT:
        return new ColumnConverter.ScalarColumnConverter(value -> writer.setBoolean((Boolean) value));
      case TIMESTAMP:
        return new ColumnConverter.ScalarColumnConverter(value -> {
          Instant instant;
          if (value instanceof LocalDateTime) {
            LocalDateTime dateTime = (LocalDateTime) value;
            instant = dateTime.toInstant(ZoneOffset.UTC);
          } else {
            instant = Instant.ofEpochMilli((Long) value / 1000);
          }
          writer.setTimestamp(instant);
        });
      case VARDECIMAL:
        return new ColumnConverter.ScalarColumnConverter(value -> writer.setDecimal((BigDecimal) value));
      case VARBINARY:
        return new ColumnConverter.ScalarColumnConverter(value -> {
          byte[] bytes;
          if (value instanceof ByteBuffer) {
            ByteBuffer byteBuf = (ByteBuffer) value;
            bytes = byteBuf.array();
          } else {
            bytes = (byte[]) value;
          }
          writer.setBytes(bytes, bytes.length);
        });
      default:
        return super.buildScalar(readerSchema, writer);
    }
  }

  public static ColumnMetadata getColumnMetadata(Types.NestedField field) {
    Type type = field.type();
    String name = field.name();
    return getColumnMetadata(name, type, field.isOptional() ? TypeProtos.DataMode.OPTIONAL : TypeProtos.DataMode.REQUIRED);
  }

  private static ColumnMetadata getColumnMetadata(String name, Type type, TypeProtos.DataMode dataMode) {
    switch (type.typeId()) {
      case MAP:
        return getDictColumnMetadata(name, type, dataMode);
      case STRUCT:
        return MetadataUtils.newMap(name, dataMode, convertSchema(type.asStructType()));
      case LIST:
        Type elementType = type.asListType().elementType();
        switch (elementType.typeId()) {
          case MAP:
            return getDictColumnMetadata(name, elementType, TypeProtos.DataMode.REPEATED);
          case STRUCT:
            return MetadataUtils.newMapArray(name, convertSchema(elementType.asStructType()));
          case LIST:
            return MetadataUtils.newRepeatedList(name, getColumnMetadata(name, elementType, TypeProtos.DataMode.REPEATED));
          default:
            return getPrimitiveMetadata(name, elementType, TypeProtos.DataMode.REPEATED);
        }
      default:
        return getPrimitiveMetadata(name, type, dataMode);
    }
  }

  private static ColumnMetadata getPrimitiveMetadata(String name, Type type, TypeProtos.DataMode dataMode) {
    TypeProtos.MinorType minorType = getType(type);
    if (minorType == null) {
      throw new UnsupportedOperationException(String.format("Unsupported type: %s for column: %s", type, name));
    }
    TypeProtos.MajorType.Builder builder = TypeProtos.MajorType.newBuilder()
      .setMinorType(minorType)
      .setMode(dataMode);
    switch (type.typeId()) {
      case DECIMAL: {
        Types.DecimalType decimalType = (Types.DecimalType) type;
        builder.setScale(decimalType.scale())
          .setPrecision(decimalType.precision());
        break;
      }
      case FIXED: {
        Types.FixedType fixedType = (Types.FixedType) type;
        builder.setWidth(fixedType.length());
      }
    }
    MaterializedField materializedField = MaterializedField.create(name, builder.build());
    return MetadataUtils.fromField(materializedField);
  }

  private static DictColumnMetadata getDictColumnMetadata(String name, Type type, TypeProtos.DataMode dataMode) {
    MaterializedField dictField = SchemaBuilder.columnSchema(name, TypeProtos.MinorType.DICT, dataMode);
    TupleSchema dictSchema = new TupleSchema();
    dictSchema.add(getColumnMetadata(DictVector.FIELD_KEY_NAME, type.asMapType().keyType(), TypeProtos.DataMode.REQUIRED));
    dictSchema.add(getColumnMetadata(DictVector.FIELD_VALUE_NAME, type.asMapType().valueType(), TypeProtos.DataMode.REQUIRED));
    return MetadataUtils.newDict(dictField, dictSchema);
  }

  public static TupleSchema convertSchema(Types.StructType structType) {
    TupleSchema schema = new TupleSchema();
    for (Types.NestedField field : structType.fields()) {
      ColumnMetadata columnMetadata = getColumnMetadata(field);
      schema.add(columnMetadata);
    }
    return schema;
  }

  private static TypeProtos.MinorType getType(Type type) {
    switch (type.typeId()) {
      case BOOLEAN:
        return TypeProtos.MinorType.BIT;
      case INTEGER:
        return TypeProtos.MinorType.INT;
      case LONG:
        return TypeProtos.MinorType.BIGINT;
      case FLOAT:
        return TypeProtos.MinorType.FLOAT4;
      case DOUBLE:
        return TypeProtos.MinorType.FLOAT8;
      case DATE:
        return TypeProtos.MinorType.DATE;
      case TIME:
        return TypeProtos.MinorType.TIME;
      case TIMESTAMP:
        return TypeProtos.MinorType.TIMESTAMP;
      case STRING:
        return TypeProtos.MinorType.VARCHAR;
      case UUID:
      case FIXED:
      case BINARY:
        return TypeProtos.MinorType.VARBINARY;
      case DECIMAL:
        return TypeProtos.MinorType.VARDECIMAL;
    }
    return null;
  }
}
