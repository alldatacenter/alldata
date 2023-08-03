/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.server.optimizing.flow.view;

import com.google.common.collect.Lists;
import com.netease.arctic.ams.api.TableFormat;
import com.netease.arctic.data.ChangeAction;
import com.netease.arctic.io.writer.GenericBaseTaskWriter;
import com.netease.arctic.io.writer.GenericChangeTaskWriter;
import com.netease.arctic.io.writer.GenericTaskWriters;
import com.netease.arctic.io.writer.RecordWithAction;
import com.netease.arctic.table.ArcticTable;
import org.apache.iceberg.FileFormat;
import org.apache.iceberg.PartitionKey;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.StructLike;
import org.apache.iceberg.Table;
import org.apache.iceberg.data.GenericAppenderFactory;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.io.BaseTaskWriter;
import org.apache.iceberg.io.FileAppenderFactory;
import org.apache.iceberg.io.FileIO;
import org.apache.iceberg.io.OutputFileFactory;
import org.apache.iceberg.io.WriteResult;
import org.apache.iceberg.types.Types;
import org.apache.iceberg.util.ArrayUtil;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public abstract class AbstractTableDataView implements TableDataView {

  protected ArcticTable arcticTable;

  protected Schema primary;

  protected Schema schema;

  protected long targetFileSize;

  public AbstractTableDataView(ArcticTable arcticTable, Schema primary, long targetFileSize) {
    this.arcticTable = arcticTable;
    this.primary = primary;
    this.schema = arcticTable.schema();
    this.targetFileSize = targetFileSize;
  }

  protected WriteResult writeFile(List<RecordWithAction> records) throws IOException {
    if (arcticTable.format() == TableFormat.ICEBERG) {
      return writeIceberg(records);
    } else if (arcticTable.isKeyedTable()) {
      return writeKeyedTable(records);
    } else {
      return writeUnKeyedTable(records);
    }
  }

  private WriteResult writeKeyedTable(List<RecordWithAction> records) throws IOException {
    GenericChangeTaskWriter writer = GenericTaskWriters.builderFor(arcticTable.asKeyedTable())
        .withTransactionId(0L)
        .buildChangeWriter();
    try {
      for (Record record : records) {
        writer.write(record);
      }
    } finally {
      writer.close();
    }
    return writer.complete();
  }

  private WriteResult writeUnKeyedTable(List<RecordWithAction> records) throws IOException {
    GenericBaseTaskWriter writer = GenericTaskWriters.builderFor(arcticTable)
        .buildBaseWriter();
    try {
      for (Record record : records) {
        writer.write(record);
      }
    } finally {
      writer.close();
    }
    return writer.complete();
  }

  private WriteResult writeIceberg(List<RecordWithAction> records) throws IOException {
    Schema eqDeleteSchema = primary == null ? schema : primary;
    GenericTaskDeltaWriter deltaWriter = createTaskWriter(
        eqDeleteSchema
                .columns()
                .stream()
                .map(Types.NestedField::fieldId).collect(Collectors.toList()),
        schema,
        arcticTable.asUnkeyedTable(),
        FileFormat.PARQUET,
        OutputFileFactory.builderFor(
            arcticTable.asUnkeyedTable(),
            1,
            1).format(FileFormat.PARQUET).build()
    );
    for (RecordWithAction record : records) {
      if (record.getAction() == ChangeAction.DELETE || record.getAction() == ChangeAction.UPDATE_BEFORE) {
        deltaWriter.delete(record);
      } else {
        deltaWriter.write(record);
      }
    }
    return deltaWriter.complete();
  }

  private GenericTaskDeltaWriter createTaskWriter(
      List<Integer> equalityFieldIds,
      Schema eqDeleteRowSchema,
      Table table,
      FileFormat format,
      OutputFileFactory fileFactory) {
    FileAppenderFactory<Record> appenderFactory =
        new GenericAppenderFactory(
            table.schema(),
            table.spec(),
            ArrayUtil.toIntArray(equalityFieldIds),
            eqDeleteRowSchema,
            null);

    List<String> columns = Lists.newArrayList();
    for (Integer fieldId : equalityFieldIds) {
      columns.add(table.schema().findField(fieldId).name());
    }
    Schema deleteSchema = table.schema().select(columns);

    PartitionKey partitionKey = new PartitionKey(table.spec(), schema);

    return new GenericTaskDeltaWriter(
        table.schema(),
        deleteSchema,
        table.spec(),
        format,
        appenderFactory,
        fileFactory,
        table.io(),
        partitionKey,
        targetFileSize);
  }

  private static class GenericTaskDeltaWriter extends BaseTaskWriter<Record> {
    private final GenericEqualityDeltaWriter deltaWriter;

    private GenericTaskDeltaWriter(
        Schema schema,
        Schema deleteSchema,
        PartitionSpec spec,
        FileFormat format,
        FileAppenderFactory<Record> appenderFactory,
        OutputFileFactory fileFactory,
        FileIO io,
        PartitionKey partitionKey,
        long targetFileSize) {
      super(spec, format, appenderFactory, fileFactory, io, targetFileSize);
      this.deltaWriter = new GenericEqualityDeltaWriter(partitionKey, schema, deleteSchema);
    }

    @Override
    public void write(Record row) throws IOException {
      deltaWriter.write(row);
    }

    public void delete(Record row) throws IOException {
      deltaWriter.delete(row);
    }

    // The caller of this function is responsible for passing in a record with only the key fields
    public void deleteKey(Record key) throws IOException {
      deltaWriter.deleteKey(key);
    }

    @Override
    public void close() throws IOException {
      deltaWriter.close();
    }

    private class GenericEqualityDeltaWriter extends BaseEqualityDeltaWriter {
      private GenericEqualityDeltaWriter(
          PartitionKey partition, Schema schema, Schema eqDeleteSchema) {
        super(partition, schema, eqDeleteSchema);
      }

      @Override
      protected StructLike asStructLike(Record row) {
        return row;
      }

      @Override
      protected StructLike asStructLikeKey(Record data) {
        return data;
      }
    }
  }
}
