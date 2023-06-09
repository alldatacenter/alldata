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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.avro.generic.GenericFixed;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.util.Utf8;
import org.apache.drill.exec.physical.impl.scan.v3.FixedReceiver;
import org.apache.drill.exec.physical.resultSet.RowSetLoader;
import org.apache.drill.exec.record.ColumnConverter;
import org.apache.drill.exec.record.ColumnConverterFactory;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.vector.accessor.TupleWriter;
import org.apache.drill.exec.vector.accessor.ValueWriter;
import org.apache.drill.shaded.guava.com.google.common.base.Charsets;
import org.joda.time.DateTimeConstants;
import org.joda.time.Period;

public class AvroColumnConverterFactory extends ColumnConverterFactory {
  public AvroColumnConverterFactory(TupleMetadata providedSchema) {
    super(providedSchema);
  }

  /**
   * Based on given converted Avro schema and current row writer generates list of
   * column converters based on column type.
   *
   * @param readerSchema converted Avro schema
   * @param rowWriter current row writer
   * @return list of column converters
   */
  public List<ColumnConverter> initConverters(TupleMetadata providedSchema,
      TupleMetadata readerSchema, RowSetLoader rowWriter) {
    return IntStream.range(0, readerSchema.size())
        .mapToObj(i -> getConverter(providedSchema, readerSchema.metadata(i), rowWriter.column(i)))
        .collect(Collectors.toList());
  }

  @Override
  public ColumnConverter.ScalarColumnConverter buildScalar(ColumnMetadata readerSchema, ValueWriter writer) {
    switch (readerSchema.type()) {
      case VARCHAR:
        return new ColumnConverter.ScalarColumnConverter(value -> {
          byte[] binary;
          int length;
          if (value instanceof Utf8) {
            Utf8 utf8 = (Utf8) value;
            binary = utf8.getBytes();
            length = utf8.getByteLength();
          } else {
            binary = value.toString().getBytes(Charsets.UTF_8);
            length = binary.length;
          }
          writer.setBytes(binary, length);
        });
      case VARBINARY:
        return new ColumnConverter.ScalarColumnConverter(value -> {
          if (value instanceof ByteBuffer) {
            ByteBuffer buf = (ByteBuffer) value;
            writer.setBytes(buf.array(), buf.remaining());
          } else {
            byte[] bytes = ((GenericFixed) value).bytes();
            writer.setBytes(bytes, bytes.length);
          }
        });
      case VARDECIMAL:
        return new ColumnConverter.ScalarColumnConverter(value -> {
          byte[] bytes;
          if (value instanceof ByteBuffer) {
            ByteBuffer decBuf = (ByteBuffer) value;
            bytes = decBuf.array();
          } else {
            GenericFixed genericFixed = (GenericFixed) value;
            bytes = genericFixed.bytes();
          }
          BigInteger bigInteger = bytes.length == 0 ? BigInteger.ZERO : new BigInteger(bytes);
          BigDecimal decimalValue = new BigDecimal(bigInteger, readerSchema.scale());
          writer.setDecimal(decimalValue);
        });
      case TIMESTAMP:
        return new ColumnConverter.ScalarColumnConverter(value -> {
          String avroLogicalType = readerSchema.property(AvroSchemaUtil.AVRO_LOGICAL_TYPE_PROPERTY);
          if (AvroSchemaUtil.TIMESTAMP_MILLIS_LOGICAL_TYPE.equals(avroLogicalType)) {
            writer.setLong((long) value);
          } else {
            writer.setLong((long) value / 1000);
          }
        });
      case DATE:
        return new ColumnConverter.ScalarColumnConverter(value -> writer.setLong((int) value * (long) DateTimeConstants.MILLIS_PER_DAY));
      case TIME:
        return new ColumnConverter.ScalarColumnConverter(value -> {
          if (value instanceof Long) {
            writer.setInt((int) ((long) value / 1000));
          } else {
            writer.setInt((int) value);
          }
        });
      case INTERVAL:
        return new ColumnConverter.ScalarColumnConverter(value -> {
          GenericFixed genericFixed = (GenericFixed) value;
          IntBuffer intBuf = ByteBuffer.wrap(genericFixed.bytes())
              .order(ByteOrder.LITTLE_ENDIAN)
              .asIntBuffer();

          Period period = Period.months(intBuf.get(0))
              .withDays(intBuf.get(1)
              ).withMillis(intBuf.get(2));

          writer.setPeriod(period);
        });
      case FLOAT4:
        return new ColumnConverter.ScalarColumnConverter(value -> writer.setDouble((Float) value));
      case BIT:
        return new ColumnConverter.ScalarColumnConverter(value -> writer.setBoolean((Boolean) value));
      default:
        return super.buildScalar(readerSchema, writer);
    }
  }

  /**
   * Based on provided schema, given converted Avro schema and current row writer
   * generates list of column converters based on column type for {@link MapColumnConverter} and returns it.
   *
   * @param providedSchema provided schema
   * @param readerSchema   converted Avro schema
   * @param tupleWriter    current row writer
   * @return {@link MapColumnConverter} with column converters
   */
  @Override
  protected ColumnConverter getMapConverter(TupleMetadata providedSchema,
      TupleMetadata readerSchema, TupleWriter tupleWriter) {
    List<ColumnConverter> converters = IntStream.range(0, readerSchema.size())
        .mapToObj(i -> getConverter(providedSchema, readerSchema.metadata(i), tupleWriter.column(i)))
        .collect(Collectors.toList());

    return new MapColumnConverter(this, providedSchema, tupleWriter, converters);
  }

  public void buildMapMembers(GenericRecord genericRecord, TupleMetadata providedSchema,
      TupleWriter tupleWriter, List<ColumnConverter> converters) {
    // fill in tuple schema for cases when it contains recursive named record types
    TupleMetadata readerSchema = AvroSchemaUtil.convert(genericRecord.getSchema());
    TupleMetadata tableSchema = FixedReceiver.Builder.mergeSchemas(providedSchema, readerSchema);
    tableSchema.toMetadataList().forEach(tupleWriter::addColumn);

    IntStream.range(0, tableSchema.size())
        .mapToObj(i -> getConverter(providedSchema,
            readerSchema.metadata(i), tupleWriter.column(i)))
        .forEach(converters::add);
  }

  /**
   * Converts and writes all map children using provided {@link #converters}.
   * If {@link #converters} are empty, generates their converters based on
   * {@link GenericRecord} schema.
   */
  public static class MapColumnConverter implements ColumnConverter {

    private final AvroColumnConverterFactory factory;
    private final TupleMetadata providedSchema;
    private final TupleWriter tupleWriter;
    private final List<ColumnConverter> converters;

    public MapColumnConverter(AvroColumnConverterFactory factory,
        TupleMetadata providedSchema,
        TupleWriter tupleWriter, List<ColumnConverter> converters) {
      this.factory = factory;
      this.providedSchema = providedSchema;
      this.tupleWriter = tupleWriter;
      this.converters = new ArrayList<>(converters);
    }

    @Override
    public void convert(Object value) {
      if (value == null) {
        return;
      }

      GenericRecord genericRecord = (GenericRecord) value;

      if (converters.isEmpty()) {
        factory.buildMapMembers(genericRecord, providedSchema, tupleWriter, converters);
      }

      IntStream.range(0, converters.size())
          .forEach(i -> converters.get(i).convert(genericRecord.get(i)));
    }
  }
}
