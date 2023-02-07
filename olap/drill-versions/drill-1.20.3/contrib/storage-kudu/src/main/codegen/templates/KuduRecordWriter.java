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
<@pp.changeOutputFile name="org/apache/drill/exec/store/kudu/KuduRecordWriter.java" />

package org.apache.drill.exec.store.kudu;

import org.apache.drill.exec.store.EventBasedRecordWriter.FieldConverter;
import org.apache.drill.exec.vector.complex.reader.FieldReader;
import org.apache.drill.exec.vector.complex.fn.JsonOutput;
import java.io.IOException;
import java.lang.UnsupportedOperationException;
import java.util.List;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.expr.TypeHelper;
import org.apache.drill.exec.expr.holders.*;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.store.EventBasedRecordWriter.FieldConverter;
import org.apache.drill.exec.vector.*;
import org.apache.drill.exec.util.DecimalUtility;
import org.apache.drill.exec.vector.complex.reader.FieldReader;
import org.apache.parquet.io.api.RecordConsumer;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.io.api.Binary;
import io.netty.buffer.DrillBuf;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.common.types.TypeProtos;
import org.joda.time.DateTimeUtils;
import java.io.IOException;
import java.lang.UnsupportedOperationException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.expr.TypeHelper;
import org.apache.drill.exec.expr.holders.*;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.store.EventBasedRecordWriter.FieldConverter;
import org.apache.drill.exec.vector.*;
import org.apache.drill.exec.util.DecimalUtility;
import org.apache.drill.exec.vector.complex.reader.FieldReader;
import org.apache.parquet.io.api.RecordConsumer;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.io.api.Binary;
import io.netty.buffer.DrillBuf;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.common.types.TypeProtos;
import org.joda.time.DateTimeUtils;
import java.io.IOException;
import java.lang.UnsupportedOperationException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.kudu.client.*;
import org.apache.drill.exec.store.*;

public abstract class KuduRecordWriter extends AbstractRecordWriter implements RecordWriter {

    private PartialRow row;

    public void setUp(PartialRow row) {
      this.row = row;
    }

  <#list vv.types as type>
    <#list type.minor as minor>
      <#list vv.modes as mode>

        <#if mode.prefix == "Repeated" ||
        minor.class == "TinyInt" ||
        minor.class == "UInt1" ||
        minor.class == "UInt2" ||
        minor.class == "SmallInt" ||
        minor.class == "Time" ||
        minor.class == "Decimal9" ||
        minor.class == "Decimal18" ||
        minor.class == "Date" ||
        minor.class == "UInt4" ||
        minor.class == "Decimal28Sparse" ||
        minor.class == "Decimal38Sparse" ||
        minor.class?contains("Interval")
        >

        <#else>
          @Override
          public FieldConverter getNew${mode.prefix}${minor.class}Converter(int fieldId, String fieldName, FieldReader reader) {
            return new ${mode.prefix}${minor.class}KuduConverter(fieldId, fieldName, reader);
          }

          public class ${mode.prefix}${minor.class}KuduConverter extends FieldConverter {
            private Nullable${minor.class}Holder holder = new Nullable${minor.class}Holder();

            public ${mode.prefix}${minor.class}KuduConverter(int fieldId, String fieldName, FieldReader reader) {
              super(fieldId, fieldName, reader);
            }

            @Override
            public void writeField() throws IOException {

          <#if mode.prefix == "Nullable" >
            if (!reader.isSet()) {
              return;
            }
          </#if>

            reader.read(holder);

            <#if minor.class == "Float4">
              row.addFloat(fieldId, holder.value);
            <#elseif minor.class == "TimeStamp">
              row.addLong(fieldId, holder.value*1000);
            <#elseif minor.class == "Int">
              row.addInt(fieldId, holder.value);
            <#elseif minor.class == "BigInt">
              row.addLong(fieldId, holder.value);
            <#elseif minor.class == "Float8">
              row.addDouble(fieldId, holder.value);
            <#elseif minor.class == "Bit">
              row.addBoolean(fieldId, holder.value == 1);
            <#elseif minor.class == "VarChar" >
              byte[] bytes = new byte[holder.end - holder.start];
              holder.buffer.getBytes(holder.start, bytes);
              row.addString(fieldId, new String(bytes));
            <#elseif minor.class == "VarBinary">
              byte[] bytes = new byte[holder.end - holder.start];
              holder.buffer.getBytes(holder.start, bytes);
              row.addBinary(fieldId, bytes);
              reader.read(holder);
            <#else>
              throw new UnsupportedOperationException();
            </#if>
            }
          }
          </#if>
      </#list>
    </#list>
  </#list>
  }
