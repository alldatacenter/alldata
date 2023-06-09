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

import org.apache.drill.exec.planner.physical.WriterPrel;

<@pp.dropOutputFile />
<@pp.changeOutputFile name="org/apache/drill/exec/store/StatisticsRecordWriterImpl.java" />
<#include "/@includes/license.ftl" />

package org.apache.drill.exec.store;

import com.google.common.collect.Lists;

import org.apache.drill.exec.planner.physical.WriterPrel;
import org.apache.drill.exec.record.VectorAccessible;
import org.apache.drill.exec.record.VectorWrapper;
import org.apache.drill.exec.store.EventBasedRecordWriter.FieldConverter;
import org.apache.drill.exec.vector.complex.reader.FieldReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/*
 * This class is generated using freemarker and the ${.template_name} template.
 */

/**
 * Reads records from the RecordValueAccessor and writes into StatisticsRecordCollector.
 */
public class StatisticsRecordWriterImpl {
  private static final Logger logger = LoggerFactory.getLogger(StatisticsRecordWriterImpl.class);

  private VectorAccessible batch;
  private StatisticsRecordWriter recordWriter;
  private List<FieldConverter> fieldConverters;

  public StatisticsRecordWriterImpl(VectorAccessible batch, StatisticsRecordWriter recordWriter)
      throws IOException {
    this.batch = batch;
    this.recordWriter = recordWriter;
    initFieldWriters();
  }

  public int writeStatistics(int recordCount) throws IOException {
    int counter = 0;

    for (; counter < recordCount; counter++) {
      recordWriter.checkForNewPartition(counter);
      recordWriter.startStatisticsRecord();
      // write the current record
      for (FieldConverter converter : fieldConverters) {
        converter.setPosition(counter);
        converter.startField();
        converter.writeField();
        converter.endField();
      }
      recordWriter.endStatisticsRecord();
    }

    return counter;
  }

  public void flushBlockingWriter() throws IOException {
    if (recordWriter.isBlockingWriter()) {
      recordWriter.flushBlockingWriter();
    }
  }

  private void initFieldWriters() {
    fieldConverters = Lists.newArrayList();
    int fieldId = 0;
    for (VectorWrapper<?> w : batch) {
      if (w.getField().getName().equalsIgnoreCase(WriterPrel.PARTITION_COMPARATOR_FIELD)) {
        continue;
      }
      FieldReader reader = w.getValueVector().getReader();
      FieldConverter converter = getConverter(recordWriter, fieldId++, w.getField().getName(), reader);
      fieldConverters.add(converter);
    }
  }

  public static FieldConverter getConverter(StatisticsRecordCollector recordWriter, int fieldId, String fieldName,
      FieldReader reader) {
    switch (reader.getType().getMinorType()) {
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
        default:
          throw new UnsupportedOperationException();
      }
      </#list>
      </#list>
      default:
        throw new UnsupportedOperationException();
    }
  }
}