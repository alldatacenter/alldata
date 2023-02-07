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
package org.apache.drill.exec.record.metadata.schema.parser;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.TupleMetadata;

import java.io.IOException;

public class SchemaExprParser {

  /**
   * Parses string definition of the schema and converts it
   * into {@link TupleMetadata} instance.
   *
   * @param schema schema definition
   * @return metadata description of the schema
   * @throws IOException when unable to parse the schema
   */
  public static TupleMetadata parseSchema(String schema) throws IOException {
    SchemaVisitor visitor = new SchemaVisitor();
    try {
      return visitor.visit(initParser(schema).schema());
    } catch (SchemaParsingException e) {
      throw new IOException(String.format("Unable to parse schema [%s]: %s", schema, e.getMessage()), e);
    }
  }

  /**
   * Parses given column name, type and mode into {@link ColumnMetadata} instance.
   *
   * @param name column name
   * @param type column type
   * @param mode column mode
   * @return column metadata
   * @throws IOException when unable to parse the column
   */
  public static ColumnMetadata parseColumn(String name, String type, TypeProtos.DataMode mode) throws IOException {
    return parseColumn(String.format("`%s` %s %s",
      name.replaceAll("(\\\\)|(`)", "\\\\$0"),
      type,
      TypeProtos.DataMode.REQUIRED == mode ? "not null" : ""));
  }

  /**
   * Parses string definition of the column and converts it
   * into {@link ColumnMetadata} instance.
   *
   * @param column column definition
   * @return metadata description of the column
   * @throws IOException when unable to parse the column
   */
  public static ColumnMetadata parseColumn(String column) throws IOException {
    SchemaVisitor.ColumnVisitor visitor = new SchemaVisitor.ColumnVisitor();
    try {
      return visitor.visit(initParser(column).column());
    } catch (SchemaParsingException e) {
      throw new IOException(String.format("Unable to parse column [%s]: %s", column, e.getMessage()), e);
    }
  }

  private static SchemaParser initParser(String value) {
    CodePointCharStream stream = CharStreams.fromString(value);
    UpperCaseCharStream upperCaseStream = new UpperCaseCharStream(stream);

    SchemaLexer lexer = new SchemaLexer(upperCaseStream);
    lexer.removeErrorListeners();
    lexer.addErrorListener(ErrorListener.INSTANCE);

    CommonTokenStream tokens = new CommonTokenStream(lexer);

    SchemaParser parser = new SchemaParser(tokens);
    parser.removeErrorListeners();
    parser.addErrorListener(ErrorListener.INSTANCE);

    return parser;
  }

  /**
   * Custom error listener that converts all syntax errors into {@link SchemaParsingException}.
   */
  private static class ErrorListener extends BaseErrorListener {

    static final ErrorListener INSTANCE = new ErrorListener();

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line,
                            int charPositionInLine, String msg, RecognitionException e) {
      StringBuilder builder = new StringBuilder();
      builder.append("Line [").append(line).append("]");
      builder.append(", position [").append(charPositionInLine).append("]");
      if (offendingSymbol != null) {
        builder.append(", offending symbol ").append(offendingSymbol);
      }
      if (msg != null) {
        builder.append(": ").append(msg);
      }
      throw new SchemaParsingException(builder.toString());
    }
  }
}
