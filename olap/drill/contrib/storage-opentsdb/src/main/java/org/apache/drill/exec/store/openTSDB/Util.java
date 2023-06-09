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
package org.apache.drill.exec.store.openTSDB;

import org.apache.drill.shaded.guava.com.google.common.base.Splitter;
import org.apache.drill.common.exceptions.UserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class Util {

  private static final Logger log = LoggerFactory.getLogger(Util.class);

  /**
   * Parse FROM parameters to Map representation
   *
   * @param rowData with this syntax (metric=warp.speed.test)
   * @return Map with params key: metric, value: warp.speed.test
   */
  public static Map<String, String> fromRowData(String rowData) {
    try {
      String fromRowData = rowData.replaceAll("[()]", "");
      return Splitter.on(",").trimResults().omitEmptyStrings().withKeyValueSeparator("=").split(fromRowData);
    } catch (IllegalArgumentException e) {
      throw UserException.validationError()
              .message(String.format("Syntax error in the query %s", rowData))
              .build(log);
    }
  }

  /**
   * @param name Metric name
   * @return Valid metric name
   */
  public static String getValidTableName(String name) {
    if (!isTableNameValid(name)) {
      name = fromRowData(name).get("metric");
    }
    return name;
  }

  /**
   * @param name Metric name
   * @return true if name is valid
   */
  public static boolean isTableNameValid(String name) {
    return !name.contains("=");
  }
}
