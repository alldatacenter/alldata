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
import java.io.OutputStream;

import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.vector.complex.reader.FieldReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;

public class JsonWriter {
  static final Logger logger = LoggerFactory.getLogger(JsonWriter.class);

  private final JsonFactory factory = new JsonFactory();
  private final JsonOutput gen;

  public JsonWriter(OutputStream out, boolean pretty, boolean useExtendedOutput) throws IOException {
    JsonGenerator writer = factory.configure(JsonGenerator.Feature.QUOTE_NON_NUMERIC_NUMBERS, false).createGenerator(out);
    if (pretty) {
      writer = writer.useDefaultPrettyPrinter();
    }
    if (useExtendedOutput) {
      gen = new ExtendedJsonOutput(writer);
    } else {
      gen = new BasicJsonOutput(writer);
    }
  }

  public JsonWriter(JsonOutput gen) {
    this.gen = gen;
  }

  public void write(FieldReader reader) throws JsonGenerationException, IOException{
    writeValue(reader);
    gen.flush();
  }

  private void writeValue(FieldReader reader) throws JsonGenerationException, IOException{
    final DataMode m = reader.getType().getMode();
    final MinorType mt = reader.getType().getMinorType();

    switch(m){
    case OPTIONAL:
    case REQUIRED:

      switch (mt) {
      case FLOAT4:
        gen.writeFloat(reader);
        break;
      case FLOAT8:
        gen.writeDouble(reader);
        break;
      case INT:
        gen.writeInt(reader);
        break;
      case SMALLINT:
        gen.writeSmallInt(reader);
        break;
      case TINYINT:
        gen.writeTinyInt(reader);
        break;
      case BIGINT:
        gen.writeBigInt(reader);
        break;
      case BIT:
        gen.writeBoolean(reader);
        break;

      case DATE:
        gen.writeDate(reader);
        break;
      case TIME:
        gen.writeTime(reader);
        break;
      case TIMESTAMP:
        gen.writeTimestamp(reader);
        break;
      case INTERVALYEAR:
      case INTERVALDAY:
      case INTERVAL:
        gen.writeInterval(reader);
        break;
      case DECIMAL28DENSE:
      case DECIMAL28SPARSE:
      case DECIMAL38DENSE:
      case DECIMAL38SPARSE:
      case DECIMAL9:
      case DECIMAL18:
      case VARDECIMAL:
        gen.writeDecimal(reader);
        break;

      case LIST:
        // this is a pseudo class, doesn't actually contain the real reader so we have to drop down.
        gen.writeStartArray();
        while (reader.next()) {
          writeValue(reader.reader());
        }
        gen.writeEndArray();
        break;
      case MAP:
        gen.writeStartObject();
        if (reader.isSet()) {
          for(String name : reader){
            FieldReader childReader = reader.reader(name);
            if(childReader.isSet()){
              gen.writeFieldName(name);
              writeValue(childReader);
            }
          }
        }
        gen.writeEndObject();
        break;
      case NULL:
      case LATE:
        gen.writeUntypedNull();
        break;

      case VAR16CHAR:
        gen.writeVar16Char(reader);
        break;
      case VARBINARY:
        gen.writeBinary(reader);
        break;
      case VARCHAR:
        gen.writeVarChar(reader);
        break;
      }
      break;

    case REPEATED:
      gen.writeStartArray();
      switch (mt) {
      case FLOAT4:
        for(int i = 0; i < reader.size(); i++){
          gen.writeFloat(i, reader);
        }
        break;
      case FLOAT8:
        for(int i = 0; i < reader.size(); i++){
          gen.writeDouble(i, reader);
        }
        break;
      case INT:
        for(int i = 0; i < reader.size(); i++){
          gen.writeInt(i, reader);
        }
        break;
      case SMALLINT:
        for(int i = 0; i < reader.size(); i++){
          gen.writeSmallInt(i, reader);
        }
        break;
      case TINYINT:
        for(int i = 0; i < reader.size(); i++){
          gen.writeTinyInt(i, reader);
        }
        break;
      case BIGINT:
        for(int i = 0; i < reader.size(); i++){
          gen.writeBigInt(i, reader);
        }
        break;
      case BIT:
        for(int i = 0; i < reader.size(); i++){
          gen.writeBoolean(i, reader);
        }
        break;

      case DATE:
        for(int i = 0; i < reader.size(); i++){
          gen.writeDate(i, reader);
        }
        break;
      case TIME:
        for(int i = 0; i < reader.size(); i++){
          gen.writeTime(i, reader);
        }
        break;
      case TIMESTAMP:
        for(int i = 0; i < reader.size(); i++){
          gen.writeTimestamp(i, reader);
        }
        break;
      case INTERVALYEAR:
      case INTERVALDAY:
      case INTERVAL:
        for(int i = 0; i < reader.size(); i++){
          gen.writeInterval(i, reader);
        }
        break;
      case DECIMAL28DENSE:
      case DECIMAL28SPARSE:
      case DECIMAL38DENSE:
      case DECIMAL38SPARSE:
      case DECIMAL9:
      case DECIMAL18:
      case VARDECIMAL:
        for(int i = 0; i < reader.size(); i++){
          gen.writeDecimal(i, reader);
        }
        break;

      case LIST:
        for(int i = 0; i < reader.size(); i++){
          while(reader.next()){
            writeValue(reader.reader());
          }
        }
        break;
      case MAP:
        while(reader.next()){
          gen.writeStartObject();
          for(String name : reader){
            FieldReader mapField = reader.reader(name);
            if(mapField.isSet()){
              gen.writeFieldName(name);
              writeValue(mapField);
            }
          }
          gen.writeEndObject();
        }
        break;
      case NULL:
        break;

      case VAR16CHAR:
        for(int i = 0; i < reader.size(); i++){
          gen.writeVar16Char(i, reader);
        }
        break;
      case VARBINARY:
        for(int i = 0; i < reader.size(); i++){
          gen.writeBinary(i, reader);
        }
        break;
      case VARCHAR:
        for(int i = 0; i < reader.size(); i++){
          gen.writeVarChar(i, reader);
        }
        break;

      default:
        throw new IllegalStateException(String.format("Unable to handle type %s.", mt));
      }
      gen.writeEndArray();
      break;
    }
  }
}
