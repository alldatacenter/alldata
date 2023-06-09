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

<@pp.dropOutputFile />
<@pp.changeOutputFile name="org/apache/drill/exec/store/EventBasedRecordWriter.java" />
<#include "/@includes/license.ftl" />

package org.apache.drill.exec.store;

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.shaded.guava.com.google.common.collect.Maps;

import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.record.VectorAccessible;
import org.apache.drill.exec.record.VectorWrapper;
import org.apache.drill.exec.vector.complex.impl.UnionReader;
import org.apache.drill.exec.vector.complex.reader.FieldReader;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/*
 * This class is generated using freemarker and the ${.template_name} template.
 */

/** Reads records from the RecordValueAccessor and writes into RecordWriter. */
public class EventBasedRecordWriter {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(EventBasedRecordWriter.class);

  private VectorAccessible batch;
  private RecordWriter recordWriter;
  private List<FieldConverter> fieldConverters;

  public EventBasedRecordWriter(VectorAccessible batch, RecordWriter recordWriter)
          throws IOException {
    this.batch = batch;
    this.recordWriter = recordWriter;

    initFieldWriters();
  }

  public int write(int recordCount) throws IOException {
    int counter = 0;

    for (; counter < recordCount; counter++) {
      recordWriter.checkForNewPartition(counter);
      recordWriter.startRecord();
      // write the current record
      for (FieldConverter converter : fieldConverters) {
        converter.setPosition(counter);
        converter.startField();
        converter.writeField();
        converter.endField();
      }
      recordWriter.endRecord();
    }

    return counter;
  }

  private void initFieldWriters() throws IOException {
    fieldConverters = Lists.newArrayList();
    try {
      int fieldId = 0;
      for (VectorWrapper w : batch) {
        if (!recordWriter.supportsField(w.getField())) {
          continue;
        }
        FieldReader reader = w.getValueVector().getReader();
        FieldConverter converter = getConverter(recordWriter, fieldId++, w.getField().getName(), reader);
        fieldConverters.add(converter);
      }
    } catch(Exception e) {
      logger.error("Failed to create FieldWriter.", e);
      throw new IOException("Failed to initialize FieldWriters.", e);
    }
  }

  public static abstract class FieldConverter {
    protected int fieldId;
    protected String fieldName;
    protected FieldReader reader;

    public FieldConverter(int fieldId, String fieldName, FieldReader reader) {
      this.fieldId = fieldId;
      this.fieldName = fieldName;
      this.reader = reader;
    }

    public void setPosition(int index) {
      reader.setPosition(index);
    }

    public void startField() throws IOException {
      // no op
    }

    public void endField() throws IOException {
      // no op
    }

    public abstract void writeField() throws IOException;

    /**
     * Used by repeated converters for writing Parquet logical lists.
     *
     * @throws IOException may be thrown by subsequent invocation of {{@link #writeField()}}
     *         in overriden methods
     * @see <a href="https://github.com/apache/parquet-format/blob/master/LogicalTypes.md#lists">Lists</a>
     */
    public void writeListField() throws IOException {
      throw new UnsupportedOperationException(String.format(
          "Converter '%s' doesn't support writing list fields.",
          getClass().getSimpleName()));
    }
  }

  public static FieldConverter getConverter(RecordWriter recordWriter, int fieldId, String fieldName, FieldReader reader) {
    switch (reader.getType().getMinorType()) {
      case UNION:
        return recordWriter.getNewUnionConverter(fieldId, fieldName, reader);
      case MAP:
        switch (reader.getType().getMode()) {
          case REQUIRED:
          case OPTIONAL:
            return recordWriter.getNewMapConverter(fieldId, fieldName, reader);
          case REPEATED:
            return recordWriter.getNewRepeatedMapConverter(fieldId, fieldName, reader);
        }

      case DICT:
        switch (reader.getType().getMode()) {
          case REQUIRED:
          case OPTIONAL:
            return recordWriter.getNewDictConverter(fieldId, fieldName, reader);
          case REPEATED:
            return recordWriter.getNewRepeatedDictConverter(fieldId, fieldName, reader);
        }

      case LIST:
        return recordWriter.getNewRepeatedListConverter(fieldId, fieldName, reader);

      case NULL:
        return recordWriter.getNewNullableIntConverter(fieldId, fieldName, reader);

        <#list vv.types as type>
        <#list type.minor as minor>
      case ${minor.class?upper_case}:
      switch (reader.getType().getMode()) {
        case REQUIRED:
          return recordWriter.getNew${minor.class}Converter(fieldId, fieldName, reader);
        case OPTIONAL:
          return recordWriter.getNewNullable${minor.class}Converter(fieldId, fieldName, reader);
        case REPEATED:
          return recordWriter.getNewRepeated${minor.class}Converter(fieldId, fieldName, reader);
      }
      </#list>
      </#list>

    }
    throw new UnsupportedOperationException();
  }
}
