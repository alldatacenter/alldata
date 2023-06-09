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

package com.netease.arctic.spark.writer;

public enum WriteMode {
  OVERWRITE_BY_FILTER("overwrite-by-filter"),
  OVERWRITE_DYNAMIC("overwrite-dynamic"),
  APPEND("append"),
  UPSERT("upsert"),
  MERGE("merge");

  public static final String WRITE_MODE_KEY = "write-mode";

  public final String mode;
  WriteMode(String mode) {
    this.mode = mode;
  }

  public static WriteMode getWriteMode(String mode) {
    for (WriteMode m : values()) {
      if (m.mode.equalsIgnoreCase(mode)) {
        return m;
      }
    }
    throw new IllegalArgumentException("Invalid write mode: " + mode);
  }
}
