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

package com.netease.arctic.ams.server.utils;

import com.netease.arctic.ams.api.TableIdentifier;

public class TableMetadataUtil {

  public static String getTableAllIdentifyName(TableIdentifier tableIdentifier) {
    return tableIdentifier.getCatalog() + "." + tableIdentifier.getDatabase() + "." + tableIdentifier.getTableName();
  }

  public static TableIdentifier getTableAllIdentify(String name) {
    String[] names = name.split("\\.");
    if (names.length != 3) {
      return null;
    }
    TableIdentifier tableIdentifier = new TableIdentifier();
    tableIdentifier.setCatalog(names[0]);
    tableIdentifier.setDatabase(names[1]);
    tableIdentifier.setTableName(names[2]);
    return tableIdentifier;
  }
}