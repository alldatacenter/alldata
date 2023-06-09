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

package com.netease.arctic.flink.write;

import com.netease.arctic.data.ChangeAction;
import com.netease.arctic.io.ArcticFileIO;
import com.netease.arctic.io.writer.ChangeTaskWriter;
import com.netease.arctic.io.writer.OutputFileFactory;
import com.netease.arctic.table.KeyedTable;
import com.netease.arctic.table.PrimaryKeySpec;
import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.data.utils.JoinedRowData;
import org.apache.flink.table.types.logical.RowType;
import org.apache.iceberg.FileFormat;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.StructLike;
import org.apache.iceberg.flink.RowDataWrapper;
import org.apache.iceberg.io.FileAppenderFactory;

/**
 * task writer for {@link KeyedTable#changeTable()} ()}.
 * Dev should make sure outputFileFactory write to change table's location
 */
public class FlinkChangeTaskWriter extends ChangeTaskWriter<RowData> {

  private final RowDataWrapper wrapper;

  public FlinkChangeTaskWriter(FileFormat format, FileAppenderFactory<RowData> appenderFactory,
                               OutputFileFactory outputFileFactory, ArcticFileIO io, long targetFileSize,
                               long mask, Schema schema, RowType flinkSchema, PartitionSpec spec,
                               PrimaryKeySpec primaryKeySpec) {
    super(format, appenderFactory, outputFileFactory, io, targetFileSize,
        mask, schema, spec, primaryKeySpec, false);
    this.wrapper = new RowDataWrapper(flinkSchema, schema.asStruct());
  }

  @Override
  protected StructLike asStructLike(RowData data) {
    return wrapper.wrap(data);
  }

  @Override
  protected RowData appendMetaColumns(RowData data, Long fileOffset) {
    return new JoinedRowData(data, GenericRowData.of(fileOffset));
  }

  @Override
  protected ChangeAction action(RowData data) {
    switch (data.getRowKind()) {
      case DELETE:
        return ChangeAction.DELETE;
      case INSERT:
        return ChangeAction.INSERT;
      case UPDATE_BEFORE:
        return ChangeAction.UPDATE_BEFORE;
      case UPDATE_AFTER:
        return ChangeAction.UPDATE_AFTER;
    }
    return ChangeAction.INSERT;
  }
}
