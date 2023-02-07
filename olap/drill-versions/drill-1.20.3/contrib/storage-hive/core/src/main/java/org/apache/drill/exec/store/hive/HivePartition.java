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
package org.apache.drill.exec.store.hive;

import org.apache.hadoop.hive.metastore.api.Partition;
import org.apache.hadoop.hive.metastore.api.StorageDescriptor;

import java.util.List;
import java.util.Map;

/**
 * This class is wrapper of {@link Partition} class and used for
 * storage of such additional information as index of list in column lists cache.
 */
public class HivePartition extends Partition {
  // index of partition column list in the table's column list cache
  private int columnListIndex;

  public HivePartition(
    List<String> values,
    String dbName,
    String tableName,
    int createTime,
    int lastAccessTime,
    StorageDescriptor sd,
    Map<String,String> parameters,
    int columnListIndex)
  {
    super(values, dbName, tableName, createTime, lastAccessTime, sd, parameters);
    this.columnListIndex = columnListIndex;
  }

  public HivePartition(Partition other, int columnListIndex) {
    super(other);
    this.columnListIndex = columnListIndex;
  }

  /**
   * To reduce physical plan for Hive tables, in partitions does not stored list of columns
   * but stored index of that list in the table's column list cache.
   *
   * @return index of partition column list in the table's column list cache
   */
  public int getColumnListIndex() {
    return columnListIndex;
  }
}
