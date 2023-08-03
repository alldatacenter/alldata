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

package com.netease.arctic.flink.shuffle;

import com.netease.arctic.data.PrimaryKeyData;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.KeyedTable;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.types.logical.RowType;
import org.apache.flink.util.CollectionUtil;
import org.apache.iceberg.PartitionKey;
import org.apache.iceberg.Schema;
import org.apache.iceberg.flink.RowDataWrapper;
import org.apache.iceberg.types.Types;

import java.io.Serializable;

import static org.apache.iceberg.IcebergSchemaUtil.projectPartition;

/**
 * This helper operates to one arctic table and the data of the table.
 */
public class ShuffleHelper implements Serializable {
  private static final long serialVersionUID = 1L;

  private boolean primaryKeyExist = false;
  private PrimaryKeyData primaryKeyData;
  private PartitionKey partitionKey;
  private RowType rowType;
  private Types.StructType struct;
  private transient RowDataWrapper rowDataWrapper;

  public static ShuffleHelper EMPTY = new ShuffleHelper();

  public static ShuffleHelper build(ArcticTable table, Schema schema, RowType rowType) {
    PartitionKey partitionKey = null;

    if (table.spec() != null && !CollectionUtil.isNullOrEmpty(table.spec().fields())) {
      partitionKey = new PartitionKey(projectPartition(table.spec(), schema), schema);
    }
    schema = addFieldsNotInArctic(schema, rowType);

    if (table.isUnkeyedTable()) {
      return new ShuffleHelper(rowType, schema.asStruct(), partitionKey);
    }

    KeyedTable keyedTable = table.asKeyedTable();
    PrimaryKeyData primaryKeyData = new PrimaryKeyData(keyedTable.primaryKeySpec(), schema);
    return new ShuffleHelper(keyedTable.primaryKeySpec().primaryKeyExisted(),
        primaryKeyData, partitionKey, rowType, schema.asStruct());
  }

  /**
   * If using arctic table as build table, there will be an additional implicit field, valuing process time.
   *
   * @param schema  The physical schema in Arctic table.
   * @param rowType Flink RowData type.
   * @return the Arctic Schema with additional implicit field.
   */
  public static Schema addFieldsNotInArctic(Schema schema, RowType rowType) {
    Types.NestedField[] nestedFields = new Types.NestedField[rowType.getFieldCount()];

    for (int i = 0; i < nestedFields.length; i++) {
      RowType.RowField field = rowType.getFields().get(i);
      Types.NestedField nestedField;
      if ((nestedField = schema.findField(field.getName())) != null) {
        nestedFields[i] = nestedField;
      } else {
        // for now, there is only one case that virtual watermark exist in RowData, but not in Arctic table schema.
        nestedFields[i] = Types.NestedField.optional(-1, field.getName(), Types.TimestampType.withoutZone());
      }
    }
    return new Schema(nestedFields);
  }

  /**
   * Should open firstly to initial RowDataWrapper, because it cannot be serialized.
   */
  public void open() {
    if (rowDataWrapper != null) {
      return;
    }
    if (rowType != null && struct != null) {
      rowDataWrapper = new RowDataWrapper(rowType, struct);
    }
  }

  public ShuffleHelper() {
  }

  public ShuffleHelper(RowType rowType, Types.StructType structType,
                       PartitionKey partitionKey) {
    this(false, null, partitionKey, rowType, structType);
  }

  public ShuffleHelper(boolean primaryKeyExist, PrimaryKeyData primaryKeyData,
                       PartitionKey partitionKey, RowType rowType, Types.StructType structType) {
    this(primaryKeyExist, primaryKeyData, null, partitionKey, rowType, structType);
  }

  public ShuffleHelper(boolean primaryKeyExist,
                       PrimaryKeyData primaryKeyData,
                       RowDataWrapper rowDataWrapper,
                       PartitionKey partitionKey,
                       RowType rowType,
                       Types.StructType structType) {
    this.primaryKeyExist = primaryKeyExist;
    this.primaryKeyData = primaryKeyData;
    this.rowDataWrapper = rowDataWrapper;
    this.partitionKey = partitionKey;
    this.rowType = rowType;
    this.struct = structType;
  }

  public boolean isPrimaryKeyExist() {
    return primaryKeyExist;
  }

  public boolean isPartitionKeyExist() {
    return partitionKey != null && partitionKey.size() > 0;
  }

  public int hashPartitionValue(RowData rowData) {
    partitionKey.partition(rowDataWrapper.wrap(rowData));
    int hashcode = Math.abs(partitionKey.hashCode());
    return hashcode == Integer.MIN_VALUE ? Integer.MAX_VALUE : hashcode;
  }

  public int hashKeyValue(RowData rowData) {
    primaryKeyData.primaryKey(rowDataWrapper.wrap(rowData));
    return primaryKeyData.hashCode();
  }
}
