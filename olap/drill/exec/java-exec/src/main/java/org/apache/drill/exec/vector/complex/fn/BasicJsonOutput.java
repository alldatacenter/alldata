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
package org.apache.drill.exec.vector.complex.fn;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

import org.apache.drill.exec.expr.fn.impl.DateUtility;
import org.apache.drill.exec.vector.complex.reader.FieldReader;
import org.joda.time.Period;
import org.joda.time.format.ISOPeriodFormat;

import com.fasterxml.jackson.core.JsonGenerator;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

/**
 * A JSON output class that generates standard JSON. By default, literals are
 * output such that they can be implicitly cast.
 */
public class BasicJsonOutput implements JsonOutput {

  protected final JsonGenerator gen;
  private final DateTimeFormatter dateFormatter;
  private final DateTimeFormatter timeFormatter;
  private final DateTimeFormatter timestampFormatter;

  public BasicJsonOutput(JsonGenerator gen) {
    this(gen, DateOutputFormat.SQL);
  }

  protected BasicJsonOutput(JsonGenerator gen, DateOutputFormat dateOutput) {
    Preconditions.checkNotNull(dateOutput);
    Preconditions.checkNotNull(gen);

    this.gen = gen;

    switch (dateOutput) {
      case SQL: {
        dateFormatter = DateUtility.formatDate;
        timeFormatter = DateUtility.formatTime;
        timestampFormatter = DateUtility.formatTimeStamp;
        break;
      }
      case ISO: {
        dateFormatter = DateUtility.isoFormatDate;
        timeFormatter = DateUtility.isoFormatTime;
        timestampFormatter = DateUtility.isoFormatTimeStamp;
        break;
      }

      default:
        throw new UnsupportedOperationException(String.format("Unable to support date output of type %s.", dateOutput));
    }
  }

  @Override
  public void flush() throws IOException {
    gen.flush();
  }

  @Override
  public void writeStartArray() throws IOException {
    gen.writeStartArray();
  }

  @Override
  public void writeEndArray() throws IOException {
    gen.writeEndArray();
  }

  @Override
  public void writeStartObject() throws IOException {
    gen.writeStartObject();
  }

  @Override
  public void writeEndObject() throws IOException {
    gen.writeEndObject();
  }

  @Override
  public void writeUntypedNull() throws IOException {
    gen.writeNull();
  }

  @Override
  public void writeFieldName(String name) throws IOException {
    gen.writeFieldName(name);
  }

  @Override
  public void writeDecimal(FieldReader reader) throws IOException {
    if (reader.isSet()) {
      writeDecimal(reader.readBigDecimal());
    } else {
      writeDecimalNull();
    }
  }

  @Override
  public void writeTinyInt(FieldReader reader) throws IOException {
    if (reader.isSet()) {
      writeTinyInt(reader.readByte());
    } else {
      writeTinyIntNull();
    }
  }

  @Override
  public void writeSmallInt(FieldReader reader) throws IOException {
    if (reader.isSet()) {
      writeSmallInt(reader.readShort());
    } else {
      writeSmallIntNull();
    }
  }

  @Override
  public void writeInt(FieldReader reader) throws IOException {
    if (reader.isSet()) {
      writeInt(reader.readInteger());
    } else {
      writeIntNull();
    }
  }

  @Override
  public void writeBigInt(FieldReader reader) throws IOException {
    if (reader.isSet()) {
      writeBigInt(reader.readLong());
    } else {
      writeBigIntNull();
    }
  }

  @Override
  public void writeFloat(FieldReader reader) throws IOException {
    if (reader.isSet()) {
      writeFloat(reader.readFloat());
    } else {
      writeFloatNull();
    }
  }

  @Override
  public void writeDouble(FieldReader reader) throws IOException {
    if (reader.isSet()) {
      writeDouble(reader.readDouble());
    } else {
      writeDoubleNull();
    }
  }

  @Override
  public void writeVarChar(FieldReader reader) throws IOException {
    if (reader.isSet()) {
      writeVarChar(reader.readText().toString());
    } else {
      writeVarcharNull();
    }
  }

  @Override
  public void writeVar16Char(FieldReader reader) throws IOException {
    if (reader.isSet()) {
      writeVar16Char(reader.readText().toString());
    } else {
      writeVar16charNull();
    }
  }

  @Override
  public void writeBinary(FieldReader reader) throws IOException {
    if (reader.isSet()) {
      writeBinary(reader.readByteArray());
    } else {
      writeBinaryNull();
    }
  }

  @Override
  public void writeBoolean(FieldReader reader) throws IOException {
    if (reader.isSet()) {
      writeBoolean(reader.readBoolean());
    } else {
      writeBooleanNull();
    }
  }

  @Override
  public void writeDate(FieldReader reader) throws IOException {
    if (reader.isSet()) {
      writeDate(reader.readLocalDate());
    } else {
      writeDateNull();
    }
  }

  @Override
  public void writeTime(FieldReader reader) throws IOException {
    if (reader.isSet()) {
      writeTime(reader.readLocalTime());
    } else {
      writeTimeNull();
    }
  }

  @Override
  public void writeTimestamp(FieldReader reader) throws IOException {
    if (reader.isSet()) {
      writeTimestamp(reader.readLocalDateTime());
    } else {
      writeTimeNull();
    }
  }

  @Override
  public void writeInterval(FieldReader reader) throws IOException {
    if (reader.isSet()) {
      writeInterval(reader.readPeriod());
    } else {
      writeIntervalNull();
    }
  }

  @Override
  public void writeTinyInt(int index, FieldReader reader) throws IOException {
    if (reader.isSet()) {
      writeTinyInt(reader.readByte(index));
    } else {
      writeTinyIntNull();
    }
  }

  @Override
  public void writeSmallInt(int index, FieldReader reader) throws IOException {
    if (reader.isSet()) {
      writeSmallInt(reader.readShort(index));
    } else {
      writeSmallIntNull();
    }
  }

  @Override
  public void writeInt(int index, FieldReader reader) throws IOException {
    if (reader.isSet()) {
      writeInt(reader.readInteger(index));
    } else {
      writeIntNull();
    }
  }

  @Override
  public void writeBigInt(int index, FieldReader reader) throws IOException {
    if (reader.isSet()) {
      writeBigInt(reader.readLong(index));
    } else {
      writeBigIntNull();
    }
  }

  @Override
  public void writeFloat(int index, FieldReader reader) throws IOException {
    if (reader.isSet()) {
      writeFloat(reader.readFloat(index));
    } else {
      writeFloatNull();
    }
  }

  @Override
  public void writeDouble(int index, FieldReader reader) throws IOException {
    if (reader.isSet()) {
      writeDouble(reader.readDouble(index));
    } else {
      writeDoubleNull();
    }
  }

  @Override
  public void writeVarChar(int index, FieldReader reader) throws IOException {
    if (reader.isSet()) {
      writeVarChar(reader.readText(index).toString());
    } else {
      writeVarcharNull();
    }
  }

  @Override
  public void writeVar16Char(int index, FieldReader reader) throws IOException {
    if (reader.isSet()) {
      writeVar16Char(reader.readString(index));
    } else {
      writeVar16charNull();
    }
  }

  @Override
  public void writeBinary(int index, FieldReader reader) throws IOException {
    if (reader.isSet()) {
      writeBinary(reader.readByteArray(index));
    } else {
      writeBinaryNull();
    }
  }

  @Override
  public void writeBoolean(int index, FieldReader reader) throws IOException {
    if (reader.isSet()) {
      writeBoolean(reader.readBoolean(index));
    } else {
      writeBooleanNull();
    }
  }

  @Override
  public void writeDate(int index, FieldReader reader) throws IOException {
    if (reader.isSet()) {
      writeDate(reader.readLocalDate(index));
    } else {
      writeDateNull();
    }
  }

  @Override
  public void writeTime(int index, FieldReader reader) throws IOException {
    if (reader.isSet()) {
      writeTime(reader.readLocalTime(index));
    } else {
      writeTimeNull();
    }
  }

  @Override
  public void writeTimestamp(int index, FieldReader reader) throws IOException {
    if (reader.isSet()) {
      writeTimestamp(reader.readLocalDateTime(index));
    } else {
      writeTimestampNull();
    }
  }

  @Override
  public void writeInterval(int index, FieldReader reader) throws IOException {
    if (reader.isSet()) {
      writeInterval(reader.readPeriod(index));
    } else {
      writeIntervalNull();
    }
  }

  @Override
  public void writeDecimal(int index, FieldReader reader) throws IOException {
    if (reader.isSet()) {
      writeDecimal(reader.readBigDecimal(index));
    } else {
      writeDecimalNull();
    }
  }

  @Override
  public void writeDecimal(BigDecimal value) throws IOException {
    gen.writeNumber(value);
  }

  @Override
  public void writeTinyInt(byte value) throws IOException {
    gen.writeNumber(value);
  }

  @Override
  public void writeSmallInt(short value) throws IOException {
    gen.writeNumber(value);
  }

  @Override
  public void writeInt(int value) throws IOException {
    gen.writeNumber(value);
  }

  @Override
  public void writeBigInt(long value) throws IOException {
    gen.writeNumber(value);
  }

  @Override
  public void writeFloat(float value) throws IOException {
    gen.writeNumber(value);
  }

  @Override
  public void writeDouble(double value) throws IOException {
    gen.writeNumber(value);
  }

  @Override
  public void writeVarChar(String value) throws IOException {
    gen.writeString(value);
  }

  @Override
  public void writeVar16Char(String value) throws IOException {
    gen.writeString(value);
  }

  @Override
  public void writeBinary(byte[] value) throws IOException {
    gen.writeBinary(value);
  }

  @Override
  public void writeBoolean(boolean value) throws IOException {
    gen.writeBoolean(value);
  }

  @Override
  public void writeDate(TemporalAccessor value) throws IOException {
    gen.writeString(dateFormatter.format(value));
  }

  @Override
  public void writeTime(TemporalAccessor value) throws IOException {
    gen.writeString(timeFormatter.format(value));
  }

  @Override
  public void writeTimestamp(TemporalAccessor value) throws IOException {
    gen.writeString(timestampFormatter.format(value));
  }

  @Override
  public void writeInterval(Period value) throws IOException {
    gen.writeString(value.toString(ISOPeriodFormat.standard()));
  }

  @Override
  public void writeDecimalNull() throws IOException {
    gen.writeNull();
  }

  @Override
  public void writeTinyIntNull() throws IOException {
    gen.writeNull();
  }

  @Override
  public void writeSmallIntNull() throws IOException {
    gen.writeNull();
  }

  @Override
  public void writeIntNull() throws IOException {
    gen.writeNull();
  }

  @Override
  public void writeBigIntNull() throws IOException {
    gen.writeNull();
  }

  @Override
  public void writeFloatNull() throws IOException {
    gen.writeNull();
  }

  @Override
  public void writeDoubleNull() throws IOException {
    gen.writeNull();
  }

  @Override
  public void writeVarcharNull() throws IOException {
    gen.writeNull();
  }

  @Override
  public void writeVar16charNull() throws IOException {
    gen.writeNull();
  }

  @Override
  public void writeBinaryNull() throws IOException {
    gen.writeNull();
  }

  @Override
  public void writeBooleanNull() throws IOException {
    gen.writeNull();
  }

  @Override
  public void writeDateNull() throws IOException {
    gen.writeNull();
  }

  @Override
  public void writeTimeNull() throws IOException {
    gen.writeNull();
  }

  @Override
  public void writeTimestampNull() throws IOException {
    gen.writeNull();
  }

  @Override
  public void writeIntervalNull() throws IOException {
    gen.writeNull();
  }
}
