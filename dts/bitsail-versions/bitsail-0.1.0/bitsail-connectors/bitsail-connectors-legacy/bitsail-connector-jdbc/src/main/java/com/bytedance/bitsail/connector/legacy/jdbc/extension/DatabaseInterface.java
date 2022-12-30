/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytedance.bitsail.connector.legacy.jdbc.extension;

import com.google.common.base.Strings;

public interface DatabaseInterface {
  /**
   * @return driver name
   */
  String getDriverName();

  String getFieldQuote();

  /**
   * @return value quota
   */
  String getValueQuote();

  default String getQuoteValue(final String value) {
    return getValueQuote().concat(value).concat(getValueQuote());
  }

  default String getQuoteColumn(final String column) {
    return getFieldQuote().concat(column).concat(getFieldQuote());
  }

  default String getQuoteTable(final String table) {
    if (Strings.isNullOrEmpty(table)) {
      return table;
    }
    String[] parts = table.split("\\.");
    StringBuilder stringBuilder = new StringBuilder();
    for (int i = 0; i < parts.length; ++i) {
      if (i != 0) {
        stringBuilder.append(".");
      }
      stringBuilder.append(getFieldQuote()).append(parts[i]).append(getFieldQuote());
    }
    return stringBuilder.toString();
  }
}
