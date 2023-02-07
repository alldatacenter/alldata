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
import org.apache.drill.exec.store.AbstractRecordWriter;

import java.lang.Override;
import java.lang.UnsupportedOperationException;

<@pp.dropOutputFile />
<@pp.changeOutputFile name="org/apache/drill/exec/store/StringOutputRecordWriter.java" />
<#include "/@includes/license.ftl" />

package org.apache.drill.exec.store;

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.exec.expr.TypeHelper;
import org.apache.drill.exec.expr.holders.*;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.record.VectorAccessible;
import org.apache.drill.exec.store.EventBasedRecordWriter.FieldConverter;
import org.apache.drill.exec.vector.*;
import org.apache.drill.exec.vector.complex.reader.FieldReader;

import java.io.IOException;
import java.lang.UnsupportedOperationException;
import java.util.List;
import java.util.Map;

/**
 * Abstract implementation of RecordWriter interface which exposes interface:
 *    {@link #startNewSchema(BatchSchema)}
 *    {@link #addField(int,String)}
 * to output the data in string format instead of implementing addField for each type holder.
 *
 * This is useful for text format writers such as CSV, TSV etc.
 *
 * NB: Source code generated using FreeMarker template ${.template_name}
 */
public abstract class StringOutputRecordWriter extends AbstractRecordWriter {

  private final BufferAllocator allocator;
  protected StringOutputRecordWriter(BufferAllocator allocator){
    this.allocator = allocator;
  }

  @Override
  public void updateSchema(VectorAccessible batch) throws IOException {
    startNewSchema(batch.getSchema());
  }

  @Override
  public FieldConverter getNewMapConverter(int fieldId, String fieldName, FieldReader reader) {
    throw new UnsupportedOperationException();
  }
  public FieldConverter getNewRepeatedMapConverter(int fieldId, String fieldName, FieldReader reader) {
    throw new UnsupportedOperationException();
  }
  public FieldConverter getNewRepeatedListConverter(int fieldId, String fieldName, FieldReader reader) {
    throw new UnsupportedOperationException();
  }

<#list vv.types as type>
  <#list type.minor as minor>
    <#list vv.modes as mode>
  @Override
  public FieldConverter getNew${mode.prefix}${minor.class}Converter(int fieldId, String fieldName, FieldReader reader) {
    return new ${mode.prefix}${minor.class}StringFieldConverter(fieldId, fieldName, reader);
  }

  public class ${mode.prefix}${minor.class}StringFieldConverter extends FieldConverter {
    <#if mode.prefix == "Repeated">
    private Repeated${minor.class}Holder holder = new Repeated${minor.class}Holder();
    <#else>
    private Nullable${minor.class}Holder holder = new Nullable${minor.class}Holder();
    </#if>

    public ${mode.prefix}${minor.class}StringFieldConverter(int fieldId, String fieldName, FieldReader reader) {
      super(fieldId, fieldName, reader);
    }

    @Override
    public void writeField() throws IOException {
  <#if mode.prefix == "Nullable" >
    if (!reader.isSet()) {
      addField(fieldId, null);
      return;
    }
  <#elseif mode.prefix == "Repeated" >
    throw new UnsupportedOperationException("Repeated types are not supported.");
    }
  }
    <#break>
  </#if>

    reader.read(holder);
  <#if  minor.class == "TinyInt" ||
        minor.class == "UInt1" ||
        minor.class == "UInt2" ||
        minor.class == "SmallInt" ||
        minor.class == "Int" ||
        minor.class == "UInt4" ||
        minor.class == "Float4" ||
        minor.class == "BigInt" ||
        minor.class == "UInt8" ||
        minor.class == "Float8">
    addField(fieldId, String.valueOf(holder.value));
  <#elseif minor.class == "Bit">
    addField(fieldId, holder.value == 0 ? "false" : "true");
  <#elseif
        minor.class == "Date" ||
        minor.class == "Time" ||
        minor.class == "TimeTZ" ||
        minor.class == "TimeStamp" ||
        minor.class == "IntervalYear" ||
        minor.class == "IntervalDay" ||
        minor.class == "Interval" ||
        minor.class == "Decimal9" ||
        minor.class == "Decimal18" ||
        minor.class == "Decimal28Dense" ||
        minor.class == "Decimal38Dense" ||
        minor.class == "Decimal28Sparse" ||
        minor.class == "Decimal38Sparse" ||
        minor.class == "VarChar" ||
        minor.class == "Var16Char" ||
        minor.class == "VarBinary" ||
        minor.class == "VarDecimal">
    // TODO: error check
    addField(fieldId, reader.readObject().toString());
  <#else>
    throw new UnsupportedOperationException(String.format("Unsupported field type: %s"),
      holder.getCanonicalClass());
   </#if>
    }
  }
    </#list>
  </#list>
</#list>

  public void cleanup() throws IOException {
  }

  public abstract void startNewSchema(BatchSchema schema) throws IOException;
  public abstract void addField(int fieldId, String value) throws IOException;
}
