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

package com.netease.arctic.utils;

import org.apache.iceberg.types.Types;

import static org.apache.iceberg.types.Types.NestedField.optional;
import static org.apache.iceberg.types.Types.NestedField.required;

public class ManifestEntryFields {
  public static final Types.NestedField STATUS = required(0, "status", Types.IntegerType.get());
  public static final Types.NestedField SNAPSHOT_ID = optional(1, "snapshot_id", Types.LongType.get());
  public static final Types.NestedField SEQUENCE_NUMBER = optional(3, "sequence_number", Types.LongType.get());
  public static final int DATA_FILE_ID = 3;
  public static final String DATA_FILE_FIELD_NAME = "data_file";

  public enum Status {
    EXISTING(0),
    ADDED(1),
    DELETED(2);

    private final int id;

    Status(int id) {
      this.id = id;
    }

    public int id() {
      return id;
    }

    public static Status of(int id) {
      for (Status status : Status.values()) {
        if (status.id() == id) {
          return status;
        }
      }
      throw new IllegalArgumentException("not support status id " + id);
    }
  }
}
