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
import java.io.InputStream;
import java.util.List;

import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.physical.base.GroupScan;
import org.apache.drill.exec.store.easy.json.reader.BaseJsonReader;
import org.apache.drill.exec.vector.complex.fn.VectorOutput.ListVectorOutput;
import org.apache.drill.exec.vector.complex.fn.VectorOutput.MapVectorOutput;
import org.apache.drill.exec.vector.complex.writer.BaseWriter.ComplexWriter;
import org.apache.drill.exec.vector.complex.writer.BaseWriter.ListWriter;
import org.apache.drill.exec.vector.complex.writer.BaseWriter.MapWriter;
import org.apache.drill.shaded.guava.com.google.common.base.Charsets;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;

import io.netty.buffer.DrillBuf;

public class JsonReader extends BaseJsonReader {
  private static final Logger logger =
      LoggerFactory.getLogger(JsonReader.class);
  public final static int MAX_RECORD_SIZE = 128 * 1024;

  private final WorkingBuffer workingBuffer;
  private final List<SchemaPath> columns;
  private final boolean allTextMode;
  private final MapVectorOutput mapOutput;
  private final ListVectorOutput listOutput;
  private final boolean extended = true;
  private final boolean readNumbersAsDouble;

  /**
   * Collection for tracking empty array writers during reading
   * and storing them for initializing empty arrays
   */
  private final List<ListWriter> emptyArrayWriters = Lists.newArrayList();

  private final FieldSelection selection;

  public JsonReader(Builder builder) {
    super(builder.managedBuf, builder.enableNanInf, builder.enableEscapeAnyChar, builder.skipOuterList);
    selection = FieldSelection.getFieldSelection(builder.columns);
    workingBuffer = builder.workingBuffer;
    allTextMode = builder.allTextMode;
    columns = builder.columns;
    mapOutput = builder.mapOutput;
    listOutput = builder.listOutput;
    currentFieldName = builder.currentFieldName;
    readNumbersAsDouble = builder.readNumbersAsDouble;
  }

  public static class Builder {
    private final DrillBuf managedBuf;
    private final WorkingBuffer workingBuffer;
    private List<SchemaPath> columns;
    private final MapVectorOutput mapOutput;
    private final ListVectorOutput listOutput;
    private final String currentFieldName = "<none>";
    private boolean readNumbersAsDouble;
    private boolean skipOuterList;
    private boolean allTextMode;
    private boolean enableNanInf;
    private boolean enableEscapeAnyChar;

    public Builder(DrillBuf managedBuf) {
      this.managedBuf = managedBuf;
      this.workingBuffer = new WorkingBuffer(managedBuf);
      this.mapOutput = new MapVectorOutput(workingBuffer);
      this.listOutput = new ListVectorOutput(workingBuffer);
      this.enableNanInf = true;
    }

    public Builder readNumbersAsDouble(boolean readNumbersAsDouble) {
      this.readNumbersAsDouble = readNumbersAsDouble;
      return this;
    }

    public Builder skipOuterList(boolean skipOuterList) {
      this.skipOuterList = skipOuterList;
      return this;
    }

    public Builder allTextMode(boolean allTextMode) {
      this.allTextMode = allTextMode;
      return this;
    }

    public Builder enableNanInf(boolean enableNanInf) {
      this.enableNanInf = enableNanInf;
      return this;
    }

    public Builder enableEscapeAnyChar(boolean enableEscapeAnyChar) {
      this.enableEscapeAnyChar = enableEscapeAnyChar;
      return this;
    }

    public Builder defaultSchemaPathColumns() {
      this.columns = GroupScan.ALL_COLUMNS;
      return this;
    }

    public Builder schemaPathColumns(List<SchemaPath> columns) {
      this.columns = columns;
      return this;
    }

    public JsonReader build() {
      if (columns == null) {
        throw new IllegalStateException("You need to set SchemaPath columns in order to build JsonReader");
      }
      assert Preconditions.checkNotNull(columns).size() > 0 : "JSON record reader requires at least one column";
      return new JsonReader(this);
    }
  }

  @Override
  public void ensureAtLeastOneField(ComplexWriter writer) {
    JsonReaderUtils.ensureAtLeastOneField(writer, columns, allTextMode, emptyArrayWriters);
  }

  public void setSource(int start, int end, DrillBuf buf) throws IOException {
    setSource(DrillBufInputStream.getStream(start, end, buf));
  }

  @Override
  public void setSource(InputStream is) throws IOException {
    super.setSource(is);
    mapOutput.setParser(parser);
    listOutput.setParser(parser);
  }

  @Override
  public void setSource(JsonNode node) {
    super.setSource(node);
    mapOutput.setParser(parser);
    listOutput.setParser(parser);
  }

  public void setSource(String data) throws IOException {
    setSource(data.getBytes(Charsets.UTF_8));
  }

  public void setSource(byte[] bytes) throws IOException {
    setSource(new SeekableBAIS(bytes));
  }

  @Override
  protected void writeDocument(ComplexWriter writer, JsonToken t) throws IOException {
    switch (t) {
      case START_OBJECT:
        writeDataSwitch(writer.rootAsMap());
        break;
      case START_ARRAY:
        writeDataSwitch(writer.rootAsList());
        break;
      default:
        throw createDocumentTopLevelException();
    }
  }

  private void writeDataSwitch(MapWriter w) throws IOException {
    if (this.allTextMode) {
      writeDataAllText(w, this.selection, true);
    } else {
      writeData(w, this.selection, true);
    }
  }

  private void writeDataSwitch(ListWriter w) throws IOException {
    if (this.allTextMode) {
      writeDataAllText(w);
    } else {
      writeData(w);
    }
  }

  private void consumeEntireNextValue() throws IOException {
    switch (parser.nextToken()) {
    case START_ARRAY:
    case START_OBJECT:
      parser.skipChildren();
      return;
    default:
      // hit a single value, do nothing as the token was already read
      // in the switch statement
      return;
    }
  }

  /**
   *
   * @param map
   * @param selection
   * @param moveForward
   *          Whether or not we should start with using the current token or the
   *          next token. If moveForward = true, we should start with the next
   *          token and ignore the current one.
   * @throws IOException
   */
  private void writeData(MapWriter map, FieldSelection selection,
      boolean moveForward) throws IOException {

    map.start();
    try {
      outside: while (true) {

        JsonToken t;
        if (moveForward) {
          t = parser.nextToken();
        } else {
          t = parser.getCurrentToken();
          moveForward = true;
        }
        if (t == JsonToken.NOT_AVAILABLE || t == JsonToken.END_OBJECT) {
          return;
        }

        assert t == JsonToken.FIELD_NAME : String.format(
            "Expected FIELD_NAME but got %s.", t.name());

        final String fieldName = parser.getText();
        this.currentFieldName = fieldName;
        FieldSelection childSelection = selection.getChild(fieldName);
        if (childSelection.isNeverValid()) {
          consumeEntireNextValue();
          continue outside;
        }

        switch (parser.nextToken()) {
        case START_ARRAY:
          writeData(map.list(fieldName));
          break;

        case START_OBJECT:
          if (!writeMapDataIfTyped(map, fieldName)) {
            writeData(map.map(fieldName), childSelection, false);
          }
          break;

        case END_OBJECT:
          break outside;

        case VALUE_FALSE:
          map.bit(fieldName).writeBit(0);
          break;

        case VALUE_TRUE:
          map.bit(fieldName).writeBit(1);
          break;

        case VALUE_NULL:
          // do nothing as we don't have a type.
          break;

        case VALUE_NUMBER_FLOAT:
          map.float8(fieldName).writeFloat8(parser.getDoubleValue());
          break;

        case VALUE_NUMBER_INT:
          if (this.readNumbersAsDouble) {
            map.float8(fieldName).writeFloat8(parser.getDoubleValue());
          } else {
            map.bigInt(fieldName).writeBigInt(parser.getLongValue());
          }
          break;

        case VALUE_STRING:
          handleString(parser, map, fieldName);
          break;

        default:
          throw getExceptionWithContext(UserException.dataReadError(), null)
              .message("Unexpected token %s", parser.getCurrentToken())
              .build(logger);
        }

      }
    } finally {
      map.end();
    }
  }

  private void writeDataAllText(MapWriter map, FieldSelection selection,
      boolean moveForward) throws IOException {

    map.start();
    outside: while (true) {

      JsonToken t;

      if (moveForward) {
        t = parser.nextToken();
      } else {
        t = parser.getCurrentToken();
        moveForward = true;
      }
      if (t == JsonToken.NOT_AVAILABLE || t == JsonToken.END_OBJECT) {
        return;
      }

      assert t == JsonToken.FIELD_NAME : String.format(
          "Expected FIELD_NAME but got %s.", t.name());

      final String fieldName = parser.getText();
      this.currentFieldName = fieldName;
      FieldSelection childSelection = selection.getChild(fieldName);
      if (childSelection.isNeverValid()) {
        consumeEntireNextValue();
        continue outside;
      }

      switch (parser.nextToken()) {
      case START_ARRAY:
        writeDataAllText(map.list(fieldName));
        break;

      case START_OBJECT:
        if (!writeMapDataIfTyped(map, fieldName)) {
          writeDataAllText(map.map(fieldName), childSelection, false);
        }
        break;

      case END_OBJECT:
        break outside;

      case VALUE_EMBEDDED_OBJECT:
      case VALUE_FALSE:
      case VALUE_TRUE:
      case VALUE_NUMBER_FLOAT:
      case VALUE_NUMBER_INT:
      case VALUE_STRING:
        handleString(parser, map, fieldName);
        break;

      case VALUE_NULL:
        // do nothing as we don't have a type.
        break;

      default:
        throw getExceptionWithContext(UserException.dataReadError(), null).message("Unexpected token %s",
            parser.getCurrentToken()).build(logger);
      }
    }
    map.end();
  }

  /**
   * Will attempt to take the current value and consume it as an extended value
   * (if extended mode is enabled). Whether extended is enable or disabled, will
   * consume the next token in the stream.
   * @param writer
   * @param fieldName
   * @return
   * @throws IOException
   */
  private boolean writeMapDataIfTyped(MapWriter writer, String fieldName)
      throws IOException {
    if (extended) {
      return mapOutput.run(writer, fieldName);
    } else {
      parser.nextToken();
      return false;
    }
  }

  /**
   * Will attempt to take the current value and consume it as an extended value
   * (if extended mode is enabled). Whether extended is enable or disabled, will
   * consume the next token in the stream.
   * @param writer
   * @return
   * @throws IOException
   */
  private boolean writeListDataIfTyped(ListWriter writer) throws IOException {
    if (extended) {
      return listOutput.run(writer);
    } else {
      parser.nextToken();
      return false;
    }
  }

  private void handleString(JsonParser parser, MapWriter writer, String fieldName) throws IOException {
    try {
      writer.varChar(fieldName)
          .writeVarChar(0, workingBuffer.prepareVarCharHolder(parser.getText()), workingBuffer.getBuf());
    } catch (IllegalArgumentException e) {
      if (parser.getText() == null || parser.getText().isEmpty()) {
        return;
      }
      throw e;
    }
  }

  private void handleString(JsonParser parser, ListWriter writer)
      throws IOException {
    writer.varChar().writeVarChar(0,
        workingBuffer.prepareVarCharHolder(parser.getText()),
        workingBuffer.getBuf());
  }

  private void writeData(ListWriter list) throws IOException {
    list.startList();
    outside: while (true) {
      try {
        switch (parser.nextToken()) {
        case START_ARRAY:
          writeData(list.list());
          break;

        case START_OBJECT:
          if (!writeListDataIfTyped(list)) {
            writeData(list.map(), FieldSelection.ALL_VALID, false);
          }
          break;

        case END_ARRAY:
          addIfNotInitialized(list);
        case END_OBJECT:
          break outside;

        case VALUE_EMBEDDED_OBJECT:
        case VALUE_FALSE:
          list.bit().writeBit(0);
          break;

        case VALUE_TRUE:
          list.bit().writeBit(1);
          break;

        case VALUE_NULL:
          throw UserException
              .unsupportedError()
              .message(
                  "Null values are not supported in lists by default. "
                      + "Please set `store.json.all_text_mode` to true to read lists containing nulls. "
                      + "Be advised that this will treat JSON null values as a string containing the word 'null'.")
              .build(logger);

        case VALUE_NUMBER_FLOAT:
          list.float8().writeFloat8(parser.getDoubleValue());
          break;

        case VALUE_NUMBER_INT:
          if (this.readNumbersAsDouble) {
            list.float8().writeFloat8(parser.getDoubleValue());
          } else {
            list.bigInt().writeBigInt(parser.getLongValue());
          }
          break;

        case VALUE_STRING:
          handleString(parser, list);
          break;

        default:
          throw UserException.dataReadError()
              .message("Unexpected token %s", parser.getCurrentToken())
              .build(logger);
        }
      } catch (Exception e) {
        throw getExceptionWithContext(e, null).build(logger);
      }
    }
    list.endList();
  }

  /**
   * Checks that list has not been initialized and adds it to the emptyArrayWriters collection.
   * @param list ListWriter that should be checked
   */
  private void addIfNotInitialized(ListWriter list) {
    if (list.getValueCapacity() == 0) {
      emptyArrayWriters.add(list);
    }
  }

  private void writeDataAllText(ListWriter list) throws IOException {
    list.startList();
    outside: while (true) {

      switch (parser.nextToken()) {
      case START_ARRAY:
        writeDataAllText(list.list());
        break;

      case START_OBJECT:
        if (!writeListDataIfTyped(list)) {
          writeDataAllText(list.map(), FieldSelection.ALL_VALID, false);
        }
        break;

      case END_ARRAY:
        addIfNotInitialized(list);
      case END_OBJECT:
        break outside;

      case VALUE_EMBEDDED_OBJECT:
      case VALUE_FALSE:
      case VALUE_TRUE:
      case VALUE_NULL:
      case VALUE_NUMBER_FLOAT:
      case VALUE_NUMBER_INT:
      case VALUE_STRING:
        handleString(parser, list);
        break;

      default:
        throw getExceptionWithContext(UserException.dataReadError(), null).message("Unexpected token %s",
            parser.getCurrentToken()).build(logger);
      }
    }
    list.endList();
  }

  public DrillBuf getWorkBuf() {
    return workingBuffer.getBuf();
  }
}
