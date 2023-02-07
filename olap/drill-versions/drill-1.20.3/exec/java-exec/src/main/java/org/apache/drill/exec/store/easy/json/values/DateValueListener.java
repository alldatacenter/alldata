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
import java.time.LocalDate;

import org.apache.drill.exec.store.easy.json.loader.JsonLoaderImpl;
import org.apache.drill.exec.store.easy.json.parser.TokenIterator;
import org.apache.drill.exec.vector.accessor.ScalarWriter;

import com.fasterxml.jackson.core.JsonToken;

/**
 * Parse local time dates. Stored internally in a local epoch
 * offset from the local epoch, in ms. Does no time zone conversions,
 * simply asserts that the date is in in the same time zone as the
 * Drillbit.
 */
public class DateValueListener extends ScalarListener {

  public DateValueListener(JsonLoaderImpl loader, ScalarWriter writer) {
    super(loader, writer);
  }

  @Override
  public void onValue(JsonToken token, TokenIterator tokenizer) {
    switch (token) {
      case VALUE_NULL:
        setNull();
        break;
      case VALUE_NUMBER_INT:
        writer.setLong(tokenizer.longValue());
        break;
      case VALUE_STRING:
        try {

          // A Drill date is ms since the epoch, local time. Our input
          // is in UTC. We DO NOT want to convert from the date, midnight, UTC
          // to local time since that will change the date. Instead, we just
          // want to copy the offset since the epoch from UTC to our local
          // time, so that we retain the date, even if the span of the date
          // is different locally than UTC. A mess.
          LocalDate localDate = LocalDate.parse(tokenizer.stringValue());
          writer.setLong(Duration.between(TimestampValueListener.LOCAL_EPOCH,
              localDate.atStartOfDay()).toMillis());
        } catch (Exception e) {
          throw loader.dataConversionError(schema(), "date", tokenizer.stringValue());
        }
        break;
      default:
        throw tokenizer.invalidValue(token);
    }
  }
}
