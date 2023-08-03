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

package com.netease.arctic.table;

import com.netease.arctic.ams.api.TableFormat;
import com.netease.arctic.io.ArcticFileIO;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.UpdateProperties;
import org.apache.iceberg.UpdateSchema;

import java.io.Serializable;
import java.util.Map;

/**
 * Represents an arctic table.
 */
public interface ArcticTable extends Serializable {

  /**
   * Returns the {@link TableIdentifier} of this table
   */
  TableIdentifier id();

  /**
   * Returns the {@link TableFormat} of this table
   */
  TableFormat format();

  /**
   * Returns the {@link Schema} of this table
   */
  Schema schema();

  /**
   * Returns the name of this table
   */
  String name();

  /**
   * Returns the {@link PartitionSpec} of this table
   */
  PartitionSpec spec();

  /**
   * Returns a map of string properties of this table
   */
  Map<String, String> properties();

  /**
   * Returns the location of this table
   */
  String location();

  /**
   * Returns a {@link ArcticFileIO} to read and write files in this table
   */
  ArcticFileIO io();

  /**
   * Refresh the current table metadata.
   */
  void refresh();

  /**
   * Create a new {@link UpdateSchema} to alter the columns of this table and commit the change.
   *
   * @return a new {@link UpdateSchema}
   */
  UpdateSchema updateSchema();

  /**
   * Create a new {@link UpdateProperties} to update table properties and commit the changes.
   *
   * @return a new {@link UpdateProperties}
   */
  UpdateProperties updateProperties();

  /**
   * Returns true if this table is an {@link UnkeyedTable}
   */
  default boolean isUnkeyedTable() {
    return false;
  }

  /**
   * Returns true if this table is an {@link KeyedTable}
   */
  default boolean isKeyedTable() {
    return false;
  }

  /**
   * Returns this cast to {@link UnkeyedTable} if it is one
   *
   * @return this cast to {@link UnkeyedTable} if it is one
   * @throws IllegalStateException if this is not a {@link UnkeyedTable}
   */
  default UnkeyedTable asUnkeyedTable() {
    throw new IllegalStateException("Not an unkeyed table");
  }

  /**
   * Returns this cast to {@link KeyedTable} if it is one
   *
   * @return this cast to {@link KeyedTable} if it is one
   * @throws IllegalStateException if this is not a {@link KeyedTable}
   */
  default KeyedTable asKeyedTable() {
    throw new IllegalStateException("Not a keyed table");
  }
}
