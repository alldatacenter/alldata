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

import java.time.Duration;
import java.time.LocalDateTime;

import org.apache.drill.exec.store.easy.json.loader.JsonLoaderImpl;
import org.apache.drill.exec.store.easy.json.parser.TokenIterator;
import org.apache.drill.exec.vector.accessor.ScalarWriter;

import com.fasterxml.jackson.core.JsonToken;

/**
 * Drill-flavored version of a timestamp parser. Assumes the date time is in
 * a local (unspecified) time zone, interpreted to be the default time zone
 * of the Drillbit machine. Does no time zone conversions.
 */
public class TimestampValueListener extends ScalarListener {

  public static final LocalDateTime LOCAL_EPOCH = LocalDateTime.of(1970, 1, 1, 0, 0, 0);

  public TimestampValueListener(JsonLoaderImpl loader, ScalarWriter writer) {
    super(loader, writer);
  }

  @Override
  public void onValue(JsonToken token, TokenIterator tokenizer) {
    switch (token) {
      case VALUE_NULL:
        setNull();
        return;
      case VALUE_NUMBER_INT:
        writer.setLong(tokenizer.longValue());
        break;
      case VALUE_STRING:
        try {
          LocalDateTime localDT = LocalDateTime.parse(tokenizer.stringValue());
          writer.setLong(Duration.between(LOCAL_EPOCH, localDT).toMillis());
        } catch (Exception e) {
          throw loader.dataConversionError(schema(), "date", tokenizer.stringValue());
        }
        break;
      default:
        throw tokenizer.invalidValue(token);
    }
  }
}
