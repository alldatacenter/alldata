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
package org.apache.drill.exec.store.druid.common;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public enum DruidCompareOp {
  EQUAL("$eq"),
  NOT_EQUAL("$ne"),
  GREATER_OR_EQUAL("$gte"),
  GREATER("$gt"),
  LESS_OR_EQUAL("$lte"),
  LESS("$lt"),
  IN("$in"),
  AND("and"),
  OR("or"),
  NOT("not"),
  OPTIONS("$options"),
  PROJECT("$project"),
  COND("$cond"),
  IFNULL("$ifNull"),
  IFNOTNULL("$ifNotNull"),
  SUM("$sum"),
  GROUP_BY("$group"),
  EXISTS("$exists"),
  TYPE_SELECTOR("selector"),
  TYPE_IN("in"),
  TYPE_REGEX("regex"),
  TYPE_SEARCH("search"),
  TYPE_SEARCH_CONTAINS("contains"),
  TYPE_SEARCH_CASESENSITIVE("caseSensitive"),
  TYPE_BOUND("bound");
  private final String compareOp;

  private static final Map<String, DruidCompareOp> ENUM_MAP;

  static {
    Map<String, DruidCompareOp> map = new ConcurrentHashMap<>();
    for (DruidCompareOp instance : DruidCompareOp.values()) {
      map.put(instance.getCompareOp(),instance);
    }
    ENUM_MAP = Collections.unmodifiableMap(map);
  }

  DruidCompareOp(String compareOp) {
    this.compareOp = compareOp;
  }

  public String getCompareOp() {
    return compareOp;
  }

  public static DruidCompareOp get (String name) {
    return ENUM_MAP.get(name);
  }
}
