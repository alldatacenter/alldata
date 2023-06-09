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
package org.apache.drill.exec.store.easy.json.parser;

import java.util.function.Function;

import org.apache.drill.exec.store.easy.json.parser.ArrayValueParser.LenientArrayValueParser;
import org.apache.drill.exec.store.easy.json.parser.ScalarValueParser.SimpleValueParser;
import org.apache.drill.exec.store.easy.json.parser.ScalarValueParser.TextValueParser;
import org.apache.drill.exec.store.easy.json.parser.ValueDef.JsonType;


/**
 * Creates a field parser given a field description and an optional field
 * listener.
 * <p>
 * Parse position: <code>{ ... field : ^ ?</code> for a newly-seen field.
 * Constructs a value parser and its listeners by looking ahead
 * some number of tokens to "sniff" the type of the value. For
 * example:
 * <ul>
 * <li>{@code foo: <value>} - Field value</li>
 * <li>{@code foo: [ <value> ]} - 1D array value</li>
 * <li>{@code foo: [ [<value> ] ]} - 2D array value</li>
 * <li>Etc.</li>
 * </ul>
 * <p>
 * There are two cases in which no type estimation is possible:
 * <ul>
 * <li>The value is {@code null}, indicated by
 * {@link JsonType#NULL}.</code>
 * <li>The value is an array, and the array is empty, indicated
 * by {@link JsonType#EMPTY}.</li>
 * </ul>
 * {@link ValueDefFactory} handles syntactic type inference. The associated
 * listener enforces semantic rules. For example, if a schema is
 * available, and we know that field "x" must be an Integer, but
 * this class reports that it is an object, then the listener should
 * raise an exception.
 * <p>
 * Also, the parser cannot enforce type consistency. This method
 * looks only at the first appearance of a value: a sample size of
 * one. JSON allows anything.
 * The listener must enforce semantic rules that say whether a different
 * type is allowed for later values.
 */
public class FieldParserFactory {

  private final JsonStructureParser structParser;
  private final Function<JsonStructureParser, ObjectParser> parserFactory;

  public FieldParserFactory(JsonStructureParser structParser,
      Function<JsonStructureParser, ObjectParser> parserFactory) {
    this.structParser = structParser;
    this.parserFactory = parserFactory;
  }

  public ObjectParser rootParser() {
    return parserFactory.apply(structParser);
  }

  public ElementParser ignoredFieldParser() {
    return DummyValueParser.INSTANCE;
  }

  public ValueParser jsonTextParser(ValueListener fieldListener) {
    return new JsonValueParser(structParser, fieldListener);
  }

  public ValueParser simpleValueParser(ValueListener fieldListener) {
    return new SimpleValueParser(structParser, fieldListener);
  }

  public ValueParser textValueParser(ValueListener fieldListener) {
    return new TextValueParser(structParser, fieldListener);
  }

  public ElementParser scalarArrayValueParser(ArrayListener arrayListener, ElementParser elementParser) {
    return new LenientArrayValueParser(
        new ArrayParser(structParser, arrayListener, elementParser));
  }

  public ElementParser arrayValueParser(ArrayListener arrayListener, ElementParser elementParser) {
    return new ArrayValueParser(
        new ArrayParser(structParser, arrayListener, elementParser));
  }

  public ElementParser objectValueParser(ObjectParser objParser) {
    return new ObjectValueParser(objParser);
  }
}
