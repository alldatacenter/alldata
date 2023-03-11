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

package com.netease.arctic.spark.table;

import com.netease.arctic.table.UnkeyedTable;
import org.apache.iceberg.relocated.com.google.common.collect.Maps;
import org.apache.iceberg.spark.source.SparkTable;

import java.util.Map;

public class ArcticIcebergSparkTable extends SparkTable {
  private final UnkeyedTable unkeyedTable;

  public ArcticIcebergSparkTable(UnkeyedTable unkeyedTable, boolean refreshEagerly) {
    super(unkeyedTable, refreshEagerly);
    this.unkeyedTable = unkeyedTable;
  }

  @Override
  public UnkeyedTable table() {
    return unkeyedTable;
  }

  @Override
  public Map<String, String> properties() {
    Map<String, String> properties = Maps.newHashMap();
    properties.putAll(super.properties());
    properties.put("provider", "arctic");
    return properties;
  }
}
