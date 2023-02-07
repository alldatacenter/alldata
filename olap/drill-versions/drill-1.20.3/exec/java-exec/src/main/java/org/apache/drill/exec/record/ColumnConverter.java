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
package org.apache.drill.exec.record;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.MapColumnMetadata;
import org.apache.drill.exec.record.metadata.MetadataUtils;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.record.metadata.TupleSchema;
import org.apache.drill.exec.vector.accessor.ArrayWriter;
import org.apache.drill.exec.vector.accessor.DictWriter;
import org.apache.drill.exec.vector.accessor.TupleWriter;

/**
 * Converts and sets given value into the specific column writer.
 */
public interface ColumnConverter {

  void convert(Object value);

  /**
   * Does nothing, is used when column is not projected to avoid unnecessary
   * column values conversions and writes.
   */
  class DummyColumnConverter implements ColumnConverter {

    public static final DummyColumnConverter INSTANCE = new DummyColumnConverter();

    @Override
    public void convert(Object value) {
      // do nothing
    }
  }

  /**
   * Converts and writes scalar values using provided {@link #valueConverter}.
   * {@link #valueConverter} has different implementation depending
   * on the scalar value type.
   */
  class ScalarColumnConverter implements ColumnConverter {

    private final Consumer<Object> valueConverter;

    public ScalarColumnConverter(Consumer<Object> valueConverter) {
      this.valueConverter = valueConverter;
    }

    @Override
    public void convert(Object value) {
      if (value == null) {
        return;
      }

      valueConverter.accept(value);
    }
  }

  /**
   * Converts and writes array values using {@link #valueConverter}
   * into {@link #arrayWriter}.
   */
  class ArrayColumnConverter implements ColumnConverter {

    private final ArrayWriter arrayWriter;
    private final ColumnConverter valueConverter;

    public ArrayColumnConverter(ArrayWriter arrayWriter, ColumnConverter valueConverter) {
      this.arrayWriter = arrayWriter;
      this.valueConverter = valueConverter;
    }

    @Override
    public void convert(Object value) {
      if (value == null || !arrayWriter.isProjected()) {
        return;
      }

      Iterable<?> array;
      if (value instanceof Iterable) {
        array = (Iterable<?>) value;
      } else if (value.getClass().isArray()) {
        array = Arrays.asList(((Object[]) value));
      } else {
        throw new IllegalStateException("Invalid value type for list ArrayColumnConverter: " + value.getClass());
      }
      array.forEach(arrayValue -> {
        valueConverter.convert(arrayValue);
        arrayWriter.save();
      });
    }
  }

  /**
   * Converts and writes all map children using provided {@link #converters}.
   * If {@link #converters} are empty, generates their converters based on
   * specified schema.
   */
  class MapColumnConverter implements ColumnConverter {

    private final ColumnConverterFactory factory;
    private final TupleMetadata providedSchema;
    private final TupleWriter tupleWriter;
    private final Map<String, ColumnConverter> converters;

    public MapColumnConverter(ColumnConverterFactory factory,
        TupleMetadata providedSchema,
        TupleWriter tupleWriter, Map<String, ColumnConverter> converters) {
      this.factory = factory;
      this.providedSchema = providedSchema;
      this.tupleWriter = tupleWriter;
      this.converters = new HashMap<>(converters);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void convert(Object value) {
      if (value == null) {
        return;
      }

      Map<String, Object> record = (Map<String, Object>) value;

      record.forEach(this::processValue);
    }

    private void processValue(String name, Object columnValue) {
      ColumnConverter columnConverter = converters.computeIfAbsent(name,
          columnName -> getColumnConverter(columnValue, columnName));
      if (columnConverter != null) {
        columnConverter.convert(columnValue);
      }
    }

    private ColumnConverter getColumnConverter(Object columnValue, String columnName) {
      if (columnValue != null) {
        ColumnMetadata providedColumn = providedSchema != null
            ? providedSchema.metadata(columnName)
            : null;
        ColumnMetadata column = buildColumnMetadata(columnName, columnValue);
        if (column != null) {
          tupleWriter.addColumn(providedColumn != null ? providedColumn : column);
          return factory.getConverter(providedSchema, column, tupleWriter.column(columnName));
        }
      }
      return null;
    }

    @SuppressWarnings("unchecked")
    private ColumnMetadata buildColumnMetadata(String name, Object value) {
      Class<?> clazz = value.getClass();
      if (Map.class.isAssignableFrom(clazz)) {
        return buildMapColumnMetadata(name, (Map<String, Object>) value);
      } else if (List.class.isAssignableFrom(clazz)) {
        List<?> list = (List<?>) value;
        if (!list.isEmpty()) {
          Object innerValue = list.iterator().next();
          return buildListColumnMetadata(name, innerValue);
        } else {
          return null;
        }
      } else if (clazz.isArray()) {
        Object[] array = (Object[]) value;
        if (array.length > 0) {
          return buildListColumnMetadata(name, array[0]);
        } else {
          return null;
        }
      }
      return MetadataUtils.newScalar(name, getScalarMinorType(clazz), DataMode.OPTIONAL);
    }

    private MapColumnMetadata buildMapColumnMetadata(String name, Map<String, Object> map) {
      TupleMetadata schema = new TupleSchema();
      map.forEach((key, value) -> {
        if (value != null) {
          schema.addColumn(buildColumnMetadata(key, value));
        }
      });

      return MetadataUtils.newMap(name, schema);
    }

    private ColumnMetadata buildListColumnMetadata(String name, Object innerValue) {
      Class<?> clazz = innerValue.getClass();
      Class<?> componentType = clazz.getComponentType();
      if (List.class.isAssignableFrom(clazz) || componentType!= null && componentType.isArray()) {
        return MetadataUtils.newRepeatedList(name, buildColumnMetadata(name, innerValue));
      } else if (Map.class.isAssignableFrom(clazz)) {
        return MetadataUtils.newMapArray(name);
      } else {
        return MetadataUtils.newScalar(name, getScalarMinorType(clazz), DataMode.REPEATED);
      }
    }

    protected MinorType getScalarMinorType(Class<?> clazz) {
      if (clazz == byte.class || clazz == Byte.class) {
        return MinorType.TINYINT;
      } else if (clazz == short.class || clazz == Short.class) {
        return MinorType.SMALLINT;
      } else if (clazz == int.class || clazz == Integer.class) {
        return MinorType.INT;
      } else if (clazz == long.class || clazz == Long.class) {
        return MinorType.BIGINT;
      } else if (clazz == Date.class) {
        return MinorType.DATE;
      } else if (clazz == Time.class) {
        return MinorType.TIME;
      } else if (clazz == Timestamp.class) {
        return MinorType.TIMESTAMP;
      } else if (clazz == float.class || clazz == Float.class) {
        return MinorType.FLOAT4;
      } else if (clazz == double.class || clazz == Double.class) {
        return MinorType.FLOAT8;
      } else if (clazz == boolean.class || clazz == Boolean.class) {
        return MinorType.BIT;
      } else if (clazz == char.class || clazz == Character.class) {
        return MinorType.VARCHAR;
      } else if (clazz == String.class) {
        return MinorType.VARCHAR;
      } else if (clazz == byte[].class) {
        return MinorType.VARBINARY;
      } else if (clazz == BigDecimal.class) {
        return MinorType.VARDECIMAL;
      }
      throw new IllegalArgumentException("Cannot determine minor type for " + clazz.getName());
    }
  }

  /**
   * Converts and writes dict values using provided key / value converters.
   */
  class DictColumnConverter implements ColumnConverter {

    private final DictWriter dictWriter;
    private final ColumnConverter keyConverter;
    private final ColumnConverter valueConverter;

    public DictColumnConverter(DictWriter dictWriter, ColumnConverter keyConverter, ColumnConverter valueConverter) {
      this.dictWriter = dictWriter;
      this.keyConverter = keyConverter;
      this.valueConverter = valueConverter;
    }

    @Override
    public void convert(Object value) {
      if (value == null) {
        return;
      }

      @SuppressWarnings("unchecked") Map<Object, Object> map = (Map<Object, Object>) value;
      map.forEach((key, val) -> {
        keyConverter.convert(key);
        valueConverter.convert(val);
        dictWriter.save();
      });
    }
  }
}
