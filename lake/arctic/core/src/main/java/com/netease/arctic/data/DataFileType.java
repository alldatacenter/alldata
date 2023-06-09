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

package com.netease.arctic.data;

/**
 * Data file type, one of:
 * <ul>
 *   <li>BASE_FILE: store data record in {@link com.netease.arctic.table.BaseTable}</li>
 *   <li>INSERT_LINE: store data record in {@link com.netease.arctic.table.ChangeTable}</li>
 *   <li>EQ_DELETE_FILE: store equality delete record in {@link com.netease.arctic.table.ChangeTable}</li>
 *   <li>POS_DELETE_FILE: store positional delete record in {@link com.netease.arctic.table.BaseTable}</li>
 *   <li>ICEBERG_EQ_DELETE_FILE: store equality delete record in native iceberg table</li>
 * </ul>
 */
public enum DataFileType {
  BASE_FILE(0, "B"),
  INSERT_FILE(1, "I"),
  EQ_DELETE_FILE(2, "ED"),
  POS_DELETE_FILE(3, "PD"),
  ICEBERG_EQ_DELETE_FILE(4, "IED");

  private final int id;

  private final String shortName;

  DataFileType(int id, String shortName) {
    this.id = id;
    this.shortName = shortName;
  }

  public int id() {
    return id;
  }

  public String shortName() {
    return shortName;
  }

  public static DataFileType ofId(int id) {
    for (DataFileType type : DataFileType.values()) {
      if (type.id() == id) {
        return type;
      }
    }
    throw new IllegalArgumentException("Unknown file type id:" + id);
  }

  public static DataFileType ofShortName(String shortName) {
    for (DataFileType type : DataFileType.values()) {
      if (type.shortName().equals(shortName)) {
        return type;
      }
    }
    throw new IllegalArgumentException("Unknown file type short name:" + shortName);
  }

}
