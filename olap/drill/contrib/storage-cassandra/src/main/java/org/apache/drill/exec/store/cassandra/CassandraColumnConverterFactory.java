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
package org.apache.drill.exec.store.cassandra;

import com.datastax.driver.core.Duration;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.record.ColumnConverter;
import org.apache.drill.exec.record.ColumnConverterFactory;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.vector.accessor.TupleWriter;
import org.apache.drill.exec.vector.accessor.ValueWriter;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.Inet4Address;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class CassandraColumnConverterFactory extends ColumnConverterFactory {

  private static final PeriodFormatter FORMATTER = new PeriodFormatterBuilder()
    .appendYears()
    .appendSuffix("Y")
    .appendMonths()
    .appendSuffix("M")
    .appendWeeks()
    .appendSuffix("W")
    .appendDays()
    .appendSuffix("D")
    .appendHours()
    .appendSuffix("H")
    .appendMinutes()
    .appendSuffix("M")
    .appendSecondsWithOptionalMillis()
    .appendSuffix("S")
    .toFormatter();

  public CassandraColumnConverterFactory(TupleMetadata providedSchema) {
    super(providedSchema);
  }

  @Override
  public ColumnConverter.ScalarColumnConverter buildScalar(ColumnMetadata readerSchema, ValueWriter writer) {
    switch (readerSchema.type()) {
      case INTERVAL:
        return new ColumnConverter.ScalarColumnConverter(value -> {
          Duration duration = (Duration) value;
          writer.setPeriod(Period.parse(duration.toString(), FORMATTER));
        });
      case BIGINT:
        return new ColumnConverter.ScalarColumnConverter(value -> {
          long longValue;
          if (value instanceof BigInteger) {
            longValue = ((BigInteger) value).longValue();
          } else {
            longValue = (Long) value;
          }
          writer.setLong(longValue);
        });
      case VARCHAR:
        return new ColumnConverter.ScalarColumnConverter(value -> writer.setString(value.toString()));
      case VARDECIMAL:
        return new ColumnConverter.ScalarColumnConverter(value -> writer.setDecimal((BigDecimal) value));
      case VARBINARY:
        return new ColumnConverter.ScalarColumnConverter(value -> {
          byte[] bytes;
          if (value instanceof Inet4Address) {
            bytes = ((Inet4Address) value).getAddress();
          } else if (value instanceof UUID) {
            UUID uuid = (UUID) value;
            bytes = ByteBuffer.wrap(new byte[16])
              .order(ByteOrder.BIG_ENDIAN)
              .putLong(uuid.getMostSignificantBits())
              .putLong(uuid.getLeastSignificantBits())
              .array();
          } else {
            bytes = (byte[]) value;
          }
          writer.setBytes(bytes, bytes.length);
        });
      case BIT:
        return new ColumnConverter.ScalarColumnConverter(value -> writer.setBoolean((Boolean) value));
      default:
        return super.buildScalar(readerSchema, writer);
    }
  }

  @Override
  protected ColumnConverter getMapConverter(TupleMetadata providedSchema,
    TupleMetadata readerSchema, TupleWriter tupleWriter) {
    Map<String, ColumnConverter> converters = StreamSupport.stream(readerSchema.spliterator(), false)
      .collect(Collectors.toMap(
        ColumnMetadata::name,
        columnMetadata ->
          getConverter(providedSchema, columnMetadata, tupleWriter.column(columnMetadata.name()))));

    return new CassandraMapColumnConverter(this, providedSchema, tupleWriter, converters);
  }

  private static class CassandraMapColumnConverter extends ColumnConverter.MapColumnConverter {

    public CassandraMapColumnConverter(ColumnConverterFactory factory, TupleMetadata providedSchema, TupleWriter tupleWriter, Map<String, ColumnConverter> converters) {
      super(factory, providedSchema, tupleWriter, converters);
    }

    @Override
    protected TypeProtos.MinorType getScalarMinorType(Class<?> clazz) {
      if (clazz == Duration.class) {
        return TypeProtos.MinorType.INTERVAL;
      } else if (clazz == Inet4Address.class
        || clazz == UUID.class) {
        return TypeProtos.MinorType.VARBINARY;
      } else if (clazz == java.math.BigInteger.class) {
        return TypeProtos.MinorType.BIGINT;
      } else if (clazz == org.apache.calcite.avatica.util.ByteString.class) {
        return TypeProtos.MinorType.VARCHAR;
      }
      return super.getScalarMinorType(clazz);
    }
  }
}
