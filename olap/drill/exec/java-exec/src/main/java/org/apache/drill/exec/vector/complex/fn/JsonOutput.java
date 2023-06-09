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
import java.time.temporal.TemporalAccessor;

import org.apache.drill.exec.vector.complex.reader.FieldReader;
import org.joda.time.Period;

/**
 * Interface through which UDFs, RecordWriters and other systems can write out a
 * JSON output. Generally used to control how non-JSON types are mapped to a
 * JSON output stream.
 */
public interface JsonOutput {

  // basic json tools.
  void flush() throws IOException;
  void writeStartArray() throws IOException;
  void writeEndArray() throws IOException;
  void writeStartObject() throws IOException;
  void writeEndObject() throws IOException;
  void writeUntypedNull() throws IOException;
  void writeFieldName(String name) throws IOException;

  // literals
  void writeDecimal(BigDecimal value) throws IOException;
  void writeTinyInt(byte value) throws IOException;
  void writeSmallInt(short value) throws IOException;
  void writeInt(int value) throws IOException;
  void writeBigInt(long value) throws IOException;
  void writeFloat(float value) throws IOException;
  void writeDouble(double value) throws IOException;
  void writeVarChar(String value) throws IOException;
  void writeVar16Char(String value) throws IOException;
  void writeBinary(byte[] value) throws IOException;
  void writeBoolean(boolean value) throws IOException;
  void writeDate(TemporalAccessor value) throws IOException;
  void writeTime(TemporalAccessor value) throws IOException;
  void writeTimestamp(TemporalAccessor value) throws IOException;
  void writeInterval(Period value) throws IOException;
  void writeDecimalNull() throws IOException;
  void writeTinyIntNull() throws IOException;
  void writeSmallIntNull() throws IOException;
  void writeIntNull() throws IOException;
  void writeBigIntNull() throws IOException;
  void writeFloatNull() throws IOException;
  void writeDoubleNull() throws IOException;
  void writeVarcharNull() throws IOException;
  void writeVar16charNull() throws IOException;
  void writeBinaryNull() throws IOException;
  void writeBooleanNull() throws IOException;
  void writeDateNull() throws IOException;
  void writeTimeNull() throws IOException;
  void writeTimestampNull() throws IOException;
  void writeIntervalNull() throws IOException;

  // scalars reader
  void writeDecimal(FieldReader reader) throws IOException;
  void writeTinyInt(FieldReader reader) throws IOException;
  void writeSmallInt(FieldReader reader) throws IOException;
  void writeInt(FieldReader reader) throws IOException;
  void writeBigInt(FieldReader reader) throws IOException;
  void writeFloat(FieldReader reader) throws IOException;
  void writeDouble(FieldReader reader) throws IOException;
  void writeVarChar(FieldReader reader) throws IOException;
  void writeVar16Char(FieldReader reader) throws IOException;
  void writeBinary(FieldReader reader) throws IOException;
  void writeBoolean(FieldReader reader) throws IOException;
  void writeDate(FieldReader reader) throws IOException;
  void writeTime(FieldReader reader) throws IOException;
  void writeTimestamp(FieldReader reader) throws IOException;
  void writeInterval(FieldReader reader) throws IOException;

  // index positioned scalars
  void writeDecimal(int index, FieldReader reader) throws IOException;
  void writeTinyInt(int index, FieldReader reader) throws IOException;
  void writeSmallInt(int index, FieldReader reader) throws IOException;
  void writeInt(int index, FieldReader reader) throws IOException;
  void writeBigInt(int index, FieldReader reader) throws IOException;
  void writeFloat(int index, FieldReader reader) throws IOException;
  void writeDouble(int index, FieldReader reader) throws IOException;
  void writeVarChar(int index, FieldReader reader) throws IOException;
  void writeVar16Char(int index, FieldReader reader) throws IOException;
  void writeBinary(int index, FieldReader reader) throws IOException;
  void writeBoolean(int index, FieldReader reader) throws IOException;
  void writeDate(int index, FieldReader reader) throws IOException;
  void writeTime(int index, FieldReader reader) throws IOException;
  void writeTimestamp(int index, FieldReader reader) throws IOException;
  void writeInterval(int index, FieldReader reader) throws IOException;
}
