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
package org.apache.drill.exec.store.easy.json.values;

import org.apache.drill.exec.store.easy.json.loader.JsonLoaderImpl;
import org.apache.drill.exec.store.easy.json.parser.TokenIterator;
import org.apache.drill.exec.vector.accessor.ScalarWriter;

import com.fasterxml.jackson.core.JsonToken;

/**
 * Value listener for JSON string values. Allows conversion from
 * other scalar types using the Java {@code toString()} semantics.
 * Use the "text-mode" hint in a provided schema to get the literal
 * JSON value.
 */
public class VarCharListener extends ScalarListener {

  private final boolean classicArrayNulls;

  public VarCharListener(JsonLoaderImpl loader, ScalarWriter writer) {
    super(loader, writer);
    classicArrayNulls = isArray ? loader.options().classicArrayNulls : false;
  }

  @Override
  public void onValue(JsonToken token, TokenIterator tokenizer) {
    String value;
    switch (token) {
      case VALUE_NULL:
        setNull();
        return;
      case VALUE_TRUE:
        value = Boolean.TRUE.toString();
        break;
      case VALUE_FALSE:
        value = Boolean.FALSE.toString();
        break;
      case VALUE_NUMBER_INT:
        value = Long.toString(tokenizer.longValue());
        break;
      case VALUE_NUMBER_FLOAT:
        value = Double.toString(tokenizer.doubleValue());
        break;
      case VALUE_STRING:
        value = tokenizer.stringValue();
        break;
      default:
        throw tokenizer.invalidValue(token);
    }
    writer.setString(value);
  }

  @Override
  public void onText(String value) {
    if (value == null) {
      setNull();
    } else {
      writer.setString(value);
    }
  }

  @Override
  protected void setArrayNull() {
    writer.setString(classicArrayNulls ? "null" : "");
  }
}
