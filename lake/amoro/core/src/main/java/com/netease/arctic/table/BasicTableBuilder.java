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
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.SortOrder;
import org.apache.iceberg.Transaction;
import org.apache.iceberg.relocated.com.google.common.base.Preconditions;
import org.apache.iceberg.relocated.com.google.common.collect.Maps;

import java.util.Map;


/**
 * A base class of interface {@link TableBuilder}
 * @param <ThisT> self class
 */
public abstract class BasicTableBuilder<ThisT extends TableBuilder> implements TableBuilder {
  protected PartitionSpec spec = PartitionSpec.unpartitioned();
  protected SortOrder sortOrder = SortOrder.unsorted();
  protected Map<String, String> properties = Maps.newHashMap();
  protected PrimaryKeySpec keySpec = PrimaryKeySpec.noPrimaryKey();

  protected final Schema schema;
  protected final TableIdentifier identifier;
  protected final TableFormat format;


  public BasicTableBuilder(Schema schema, TableFormat format, TableIdentifier identifier) {
    this.schema = schema;
    this.format = format;
    this.identifier = identifier;
  }


  @Override
  public TableBuilder withPartitionSpec(PartitionSpec partitionSpec) {
    this.spec = partitionSpec;
    return self();
  }

  @Override
  public TableBuilder withSortOrder(SortOrder sortOrder) {
    this.sortOrder = sortOrder;
    return self();
  }

  @Override
  public TableBuilder withProperties(Map<String, String> properties) {
    Preconditions.checkNotNull(properties, "table properties must not be null");
    this.properties = properties;
    return self();
  }

  @Override
  public TableBuilder withProperty(String key, String value) {
    this.properties.put(key, value);
    return self();
  }

  @Override
  public TableBuilder withPrimaryKeySpec(PrimaryKeySpec primaryKeySpec) {
    this.keySpec = primaryKeySpec;
    return self();
  }

  @Override
  public Transaction newCreateTableTransaction() {
    throw new UnsupportedOperationException("do not support create table transactional.");
  }

  protected abstract ThisT self();
}
