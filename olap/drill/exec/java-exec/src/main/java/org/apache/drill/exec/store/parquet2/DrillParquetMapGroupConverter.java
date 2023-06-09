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
package org.apache.drill.exec.store.parquet2;

import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.exec.physical.impl.OutputMutator;
import org.apache.drill.exec.server.options.OptionManager;
import org.apache.drill.exec.store.parquet.ParquetReaderUtility;
import org.apache.drill.exec.vector.complex.DictVector;
import org.apache.drill.exec.vector.complex.writer.BaseWriter;
import org.apache.drill.exec.vector.complex.writer.BaseWriter.DictWriter;
import org.apache.parquet.io.api.Converter;
import org.apache.parquet.schema.GroupType;
import org.apache.parquet.schema.Type;

import java.util.Collections;

class DrillParquetMapGroupConverter extends DrillParquetGroupConverter {

  private final DictWriter writer;

  DrillParquetMapGroupConverter(OutputMutator mutator, DictWriter mapWriter, GroupType schema, OptionManager options,
                                ParquetReaderUtility.DateCorruptionStatus containsCorruptedDates) {
    super(mutator, mapWriter, options, containsCorruptedDates);
    writer = mapWriter;

    GroupType type = schema.getType(0).asGroupType();
    Converter innerConverter = new KeyValueGroupConverter(mutator, type, options, containsCorruptedDates);
    converters.add(innerConverter);
  }

  @Override
  public void start() {
    writer.start();
  }

  @Override
  public void end() {
    writer.end();
  }

  private class KeyValueGroupConverter extends DrillParquetGroupConverter {

    private static final int INDEX_KEY = 0;
    private static final int INDEX_VALUE = 1;

    KeyValueGroupConverter(OutputMutator mutator, GroupType schema, OptionManager options,
                           ParquetReaderUtility.DateCorruptionStatus containsCorruptedDates) {
      super(mutator, writer, options, containsCorruptedDates);

      converters.add(getKeyConverter(schema));
      converters.add(getValueConverter(schema, mutator, options, containsCorruptedDates));
    }

    private Converter getKeyConverter(GroupType schema) {
      Type keyType = schema.getType(INDEX_KEY);
      if (!keyType.isPrimitive()) {
        throw new DrillRuntimeException("Dict supports primitive key only. Found: " + keyType);
      } else {
        return getConverterForType(DictVector.FIELD_KEY_NAME, keyType.asPrimitiveType());
      }
    }

    private Converter getValueConverter(GroupType schema, OutputMutator mutator, OptionManager options,
                                        ParquetReaderUtility.DateCorruptionStatus containsCorruptedDates) {
      Type valueType = schema.getType(INDEX_VALUE);
      Converter valueConverter;
      if (!valueType.isPrimitive()) {
        GroupType groupType = valueType.asGroupType();
        if (ParquetReaderUtility.isLogicalMapType(groupType)) {
          DictWriter valueWriter = writer.dict(DictVector.FIELD_VALUE_NAME);
          valueConverter =
              new DrillParquetMapGroupConverter(mutator, valueWriter, groupType, options, containsCorruptedDates);
        } else {
          boolean isListType = ParquetReaderUtility.isLogicalListType(groupType);
          BaseWriter valueWriter = isListType
              ? writer.list(DictVector.FIELD_VALUE_NAME)
              : writer.map(DictVector.FIELD_VALUE_NAME);
          valueConverter = new DrillParquetGroupConverter(mutator, valueWriter, groupType, Collections.emptyList(), options,
              containsCorruptedDates, isListType, "KeyValueGroupConverter");
        }
      } else {
        valueConverter = getConverterForType(DictVector.FIELD_VALUE_NAME, valueType.asPrimitiveType());
      }
      return valueConverter;
    }

    @Override
    public void start() {
      writer.startKeyValuePair();
    }

    @Override
    public void end() {
      writer.endKeyValuePair();
    }

    @Override
    boolean isMapWriter() {
      return true;
    }
  }
}
