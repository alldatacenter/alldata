/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.trino;

import com.netease.arctic.ams.api.Constants;

/**
 * To resolve sub table name, such as "tableName#base", "tableName#change"
 */
public class TableNameResolve {

  private static final String SPLIT = "#";
  private static final String DOT_SPIT = ".";
  private static final String REGEX_DOT_SPLIT = "\\.";

  private String original;
  private String tableName;
  private Boolean isBase;

  public TableNameResolve(String original) {
    this.original = original;
    if (original.contains(SPLIT)) {
      //use actual db name
      if (original.contains(DOT_SPIT)) {
        String[] tableString = original.split(REGEX_DOT_SPLIT);
        if (tableString.length == 2) {
          original = tableString[1];
        } else if (tableString.length == 3) {
          original = tableString[2];
        }
      }

      String[] sts = original.split(SPLIT);

      this.tableName = sts[0];
      if (Constants.INNER_TABLE_BASE.equalsIgnoreCase(sts[1])) {
        isBase = true;
      } else if (Constants.INNER_TABLE_CHANGE.equalsIgnoreCase(sts[1])) {
        isBase = false;
      } else {
        throw new IllegalArgumentException("table name " + tableName + " is illegal");
      }
    }
  }

  public String getTableName() {
    return tableName;
  }

  public boolean withSuffix() {
    return isBase != null;
  }

  public boolean isBase() {
    return isBase;
  }
}
