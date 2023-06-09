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

import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.store.easy.json.parser.ArrayParser;
import org.apache.drill.exec.store.easy.json.parser.FullValueParser;
import org.apache.drill.exec.store.easy.json.parser.ObjectParser;
import org.apache.drill.exec.store.easy.json.parser.TokenIterator;
import org.apache.drill.exec.vector.accessor.VariantWriter;

import com.fasterxml.jackson.core.JsonToken;

/**
 * Parser which accepts all JSON values and converts them to actions on a
 * UNION vector writer. Scalar values are written to the writer directly.
 * Object and array values create extra layers of parser and listener.
 */
public class VariantParser extends FullValueParser {

  private final JsonLoaderImpl loader;
  private final VariantWriter writer;

  public VariantParser(JsonLoaderImpl loader, VariantWriter writer) {
    super(loader.parser());
    this.loader = loader;
    this.writer = writer;
  }

  @Override
  protected void onValue(JsonToken token, TokenIterator tokenizer) {
    switch (token) {
      case VALUE_NULL:
        writer.setNull();
        break;
      case VALUE_TRUE:
        writer.scalar(MinorType.BIT).setBoolean(true);
        break;
      case VALUE_FALSE:
        writer.scalar(MinorType.BIT).setBoolean(false);
        break;
      case VALUE_NUMBER_INT:
        writer.scalar(MinorType.BIGINT).setLong(tokenizer.longValue());
        break;
      case VALUE_NUMBER_FLOAT:
        writer.scalar(MinorType.FLOAT8).setDouble(tokenizer.doubleValue());
        break;
      case VALUE_STRING:
        writer.scalar(MinorType.VARCHAR).setString(tokenizer.stringValue());
        break;
      default:
        // Won't get here: the Jackson parser catches errors.
        throw tokenizer.invalidValue(token);
    }
  }

  @Override
  protected ObjectParser buildObjectParser(TokenIterator tokenizer) {
    return new VariantObjectParser(loader, writer);
  }

  @Override
  protected ArrayParser buildArrayParser(TokenIterator tokenizer) {
    // TODO Auto-generated method stub
    return null;
  }

  private static class VariantObjectParser extends TupleParser {

    private final VariantWriter writer;

    public VariantObjectParser(JsonLoaderImpl loader, VariantWriter writer) {
      super(loader, writer.tuple(), null);
      this.writer = writer;
    }

    @Override
    public void onStart() {
      writer.setType(MinorType.MAP);
    }
  }
}
