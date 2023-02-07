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
package org.apache.drill.exec.physical.resultSet.util;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.drill.exec.vector.accessor.ArrayReader;
import org.apache.drill.exec.vector.accessor.ObjectReader;
import org.apache.drill.exec.vector.accessor.ScalarReader;
import org.apache.drill.exec.vector.accessor.TupleReader;
import org.apache.drill.exec.vector.complex.fn.BasicJsonOutput;
import org.apache.drill.exec.vector.complex.fn.ExtendedJsonOutput;
import org.apache.drill.exec.vector.complex.fn.JsonOutput;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.MinimalPrettyPrinter;

public class JsonWriter {
  private final JsonOutput gen;

  public JsonWriter(OutputStream out, boolean pretty, boolean useExtendedOutput) throws IOException {
    JsonFactory factory = new JsonFactory();
    JsonGenerator writer = factory.disable(Feature.FLUSH_PASSED_TO_STREAM).createGenerator(out);

    // Need at least a minimal pretty printer to get line-separated objects
    writer.setPrettyPrinter(pretty ?
        new DefaultPrettyPrinter("\n") :
        new MinimalPrettyPrinter("\n"));
    if (useExtendedOutput) {
      gen = new ExtendedJsonOutput(writer);
    } else {
      gen = new BasicJsonOutput(writer);
    }
  }

  public JsonWriter(JsonOutput gen) {
    this.gen = gen;
  }

  public JsonOutput jsonOutput() { return gen; }

  public void writeRow(TupleReader reader) throws IOException {
    writeObject(reader);
    gen.flush();
  }

  private void writeObject(TupleReader reader) throws IOException {
    gen.writeStartObject();
    for (int i = 0; i < reader.tupleSchema().size(); i++) {
      writeColumn(reader.column(i));
    }
    gen.writeEndObject();
  }

  private void writeColumn(ObjectReader reader) throws IOException {
    gen.writeFieldName(reader.schema().name());
    writeValue(reader);
  }

  private void writeValue(ObjectReader reader) throws IOException {
    if (reader.isNull()) {
      gen.writeUntypedNull();
      return;
    }
    switch (reader.type()) {
    case ARRAY:
      writeArray(reader.array());
      break;
    case SCALAR:
      writeScalar(reader.scalar());
      break;
    case TUPLE:
      writeObject(reader.tuple());
      break;
    case VARIANT:
      writeColumn(reader.variant().member());
      break;
    default:
      throw new IllegalStateException(reader.type().name());
    }
  }

  private void writeArray(ArrayReader reader) throws IOException {
    gen.writeStartArray();
    ObjectReader entryWriter = reader.entry();
    while (reader.next()) {
      writeValue(entryWriter);
    }
    gen.writeEndArray();
  }

  private void writeScalar(ScalarReader reader) throws IOException {
    switch (reader.valueType()) {
    case BOOLEAN:
      gen.writeBoolean(reader.getBoolean());
      break;
    case BYTES:
      gen.writeBinary(reader.getBytes());
      break;
    case DATE:
      gen.writeDate(reader.getDate());
      break;
    case DECIMAL:
      gen.writeDecimal(reader.getDecimal());
      break;
    case FLOAT:
      gen.writeDouble(reader.getFloat());
      break;
    case DOUBLE:
      gen.writeDouble(reader.getDouble());
      break;
    case INTEGER:
      gen.writeInt(reader.getInt());
      break;
    case LONG:
      gen.writeBigInt(reader.getLong());
      break;
    case NULL:
      gen.writeUntypedNull();
      break;
    case PERIOD:
      gen.writeInterval(reader.getPeriod());
      break;
    case STRING:
      gen.writeVarChar(reader.getString());
      break;
    case TIME:
      gen.writeTime(reader.getTime());
      break;
    case TIMESTAMP:
      gen.writeTimestamp(reader.getTimestamp());
      break;
    default:
      throw new IllegalStateException(reader.valueType().name());
    }
  }
}
