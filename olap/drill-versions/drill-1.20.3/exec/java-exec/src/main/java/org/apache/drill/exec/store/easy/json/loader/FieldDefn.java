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
package org.apache.drill.exec.store.easy.json.loader;

import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.MetadataUtils;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.store.easy.json.parser.JsonStructureParser;
import org.apache.drill.exec.store.easy.json.parser.TokenIterator;
import org.apache.drill.exec.store.easy.json.parser.ValueDef;
import org.apache.drill.exec.store.easy.json.parser.ValueDefFactory;
import org.apache.drill.exec.vector.accessor.ObjectWriter;
import org.apache.drill.exec.vector.accessor.ScalarWriter;
import org.apache.drill.exec.vector.accessor.TupleWriter;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

/**
 * Describes a new field within an object. Allows the listener to control
 * how to handle the field: as unprojected, parsed as a typed field, as
 * text, as JSON, or as a custom parser.
 */
public class FieldDefn {

  private final TupleParser tupleParser;
  private final String key;
  private final TokenIterator tokenizer;
  private ValueDef valueDef;
  private ColumnMetadata providedColumn;

  public FieldDefn(TupleParser tupleParser, final String key, TokenIterator tokenizer) {
    this(tupleParser, key, tokenizer, false);
  }

  public FieldDefn(TupleParser tupleParser, final String key,
      TokenIterator tokenizer, boolean isArray) {
    this.tupleParser = tupleParser;
    this.key = key;
    this.tokenizer = tokenizer;
    if (isArray) {
      valueDef = ValueDefFactory.lookAhead(tokenizer);
      valueDef = new ValueDef(valueDef.type(), valueDef.dimensions() + 1);
    }
  }

  /**
   * Returns the field name.
   */
  public String key() { return key; }

  public TupleParser tupleParser() { return tupleParser; }

  /**
   * Token stream which allows a custom parser to look ahead
   * as needed. The caller must "unget" all tokens to leave the
   * tokenizer at the present location. Note that the underlying
   * Jackson parser will return text for the last token consumed,
   * even if tokens are unwound using the token iterator, so do not
   * look ahead past the first field name or value; on look ahead
   * over "static" tokens such as object and array start characters.
   */
  public TokenIterator tokenizer() { return tokenizer; }

  /**
   * Returns the parent parser which is needed to construct standard
   * parsers.
   */
  public JsonStructureParser parser() { return tupleParser.structParser(); }

  /**
   * Looks ahead to guess the field type based on JSON tokens.
   * While this is helpful, it really only works if the JSON
   * is structured like a list of tuples, if the initial value is not {@code null},
   * and if initial arrays are not empty. The structure parser cannot see
   * into the future beyond the first field value; the value listener for each
   * field must handle "type-deferral" if needed to handle missing or null
   * values. That is, type-consistency is a semantic task handled by the listener,
   * not a syntax task handled by the parser.
   */
  public ValueDef lookahead() {
    Preconditions.checkState(tokenizer != null);
    if (valueDef == null) {
      valueDef = ValueDefFactory.lookAhead(tokenizer);
    }
    return valueDef;
  }

  public TupleWriter writer() { return tupleParser.writer(); }

  public ColumnMetadata providedColumn() {
    if (providedColumn == null) {
      TupleMetadata tupleSchema = tupleParser.providedSchema();
      providedColumn = tupleSchema == null ? null : tupleSchema.metadata(key);
    }
    return providedColumn;
  }

  public ColumnMetadata schemaFor(MinorType type, boolean isArray) {
    return MetadataUtils.newScalar(key, type, mode(isArray));
  }

  public DataMode mode(boolean isArray) {
    return isArray ? DataMode.REPEATED : DataMode.OPTIONAL;
  }

  public ScalarWriter scalarWriterFor(MinorType type, boolean isArray) {
    return scalarWriterFor(schemaFor(type, isArray));
  }

  public ScalarWriter scalarWriterFor(ColumnMetadata colSchema) {
    ObjectWriter writer = fieldWriterFor(colSchema);
    return colSchema.isArray() ? writer.array().scalar() : writer.scalar();
  }

  public ObjectWriter fieldWriterFor(ColumnMetadata colSchema) {
    final int index = writer().addColumn(colSchema);
    return writer().column(index);
  }
}
