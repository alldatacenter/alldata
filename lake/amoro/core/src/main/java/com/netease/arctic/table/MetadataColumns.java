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

import org.apache.iceberg.Schema;
import org.apache.iceberg.types.Types;
import org.apache.iceberg.types.Types.NestedField;

import java.util.ArrayList;
import java.util.List;

/**
 * Addition metadata columns for {@link ArcticTable}
 */
public class MetadataColumns {

  // IDs Integer.MAX_VALUE - (1001-9999) are used for arctic metadata columns
  public static final String TRANSACTION_ID_FILED_NAME = "_transaction_id";
  public static final int TRANSACTION_ID_FILED_ID = Integer.MAX_VALUE - 1001;
  public static final NestedField TRANSACTION_ID_FILED = NestedField
      .optional(TRANSACTION_ID_FILED_ID, TRANSACTION_ID_FILED_NAME, Types.LongType.get());

  public static final String FILE_OFFSET_FILED_NAME = "_file_offset";
  public static final int FILE_OFFSET_FILED_ID = Integer.MAX_VALUE - 1002;
  public static final NestedField FILE_OFFSET_FILED = NestedField
      .optional(FILE_OFFSET_FILED_ID, FILE_OFFSET_FILED_NAME, Types.LongType.get());

  public static final String CHANGE_ACTION_NAME = "_change_action";
  public static final int CHANGE_ACTION_ID = Integer.MAX_VALUE - 1003;
  public static final NestedField CHANGE_ACTION_FIELD = NestedField
      .optional(CHANGE_ACTION_ID, CHANGE_ACTION_NAME, Types.StringType.get());

  public static final String TREE_NODE_NAME = "_tree_node";
  public static final int TREE_NODE_ID = Integer.MAX_VALUE - 1004;
  public static final NestedField TREE_NODE_FIELD = NestedField
      .optional(TREE_NODE_ID, TREE_NODE_NAME, Types.LongType.get());

  private MetadataColumns() {
  }

  public static Schema appendChangeStoreMetadataColumns(Schema sourceSchema) {
    List<NestedField> columns = new ArrayList<>(sourceSchema.columns());
    columns.add(MetadataColumns.TRANSACTION_ID_FILED);
    columns.add(MetadataColumns.FILE_OFFSET_FILED);
    columns.add(MetadataColumns.CHANGE_ACTION_FIELD);
    return new Schema(columns);
  }
}
