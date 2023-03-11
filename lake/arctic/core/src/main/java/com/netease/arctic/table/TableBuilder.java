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

import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.SortOrder;
import org.apache.iceberg.Transaction;

import java.util.Map;

/**
 * A builder used to create valid {@link ArcticTable}.
 */
public interface TableBuilder {
  /**
   * Sets a partition spec for table.
   *
   * @param partitionSpec partition spec
   * @return this object for chaining call
   */
  TableBuilder withPartitionSpec(PartitionSpec partitionSpec);

  /**
   * Set a sort order for the table.
   *
   * @param sortOrder a sort order
   * @return this object for chaining call
   */
  TableBuilder withSortOrder(SortOrder sortOrder);

  /**
   * Adds key/value properties to the table.
   *
   * @param properties key/value properties
   * @return this object for chaining call
   */
  TableBuilder withProperties(Map<String, String> properties);

  /**
   * Adds a key/value property to the table.
   *
   * @param key   a key
   * @param value a value
   * @return this object for chaining call
   */
  TableBuilder withProperty(String key, String value);

  /**
   * Set a primary key for the table.
   *
   * @param primaryKeySpec a primary key spec
   * @return this object for chaining call
   */
  TableBuilder withPrimaryKeySpec(PrimaryKeySpec primaryKeySpec);

  /**
   * Creates the table.
   *
   * @return the created table
   */
  ArcticTable create();

  /**
   * Create a transaction for create table;
   */
  Transaction newCreateTableTransaction();
}
