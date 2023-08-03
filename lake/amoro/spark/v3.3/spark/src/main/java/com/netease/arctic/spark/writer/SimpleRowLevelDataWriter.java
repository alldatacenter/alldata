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

package com.netease.arctic.spark.writer;

import com.netease.arctic.data.ChangeAction;
import com.netease.arctic.spark.SparkInternalRowCastWrapper;
import org.apache.iceberg.io.TaskWriter;
import org.apache.iceberg.io.WriteResult;
import org.apache.spark.sql.catalyst.InternalRow;
import org.apache.spark.sql.connector.write.WriterCommitMessage;
import org.apache.spark.sql.types.StructType;

import java.io.IOException;

public class SimpleRowLevelDataWriter implements RowLevelWriter<InternalRow> {
  private final TaskWriter<InternalRow> insertWriter;
  private final TaskWriter<InternalRow> deleteWrite;

  private final StructType schema;
  private final boolean isKeyedTable;

  public SimpleRowLevelDataWriter(TaskWriter<InternalRow> insertWriter,
                                  TaskWriter<InternalRow> deleteWrite,
                                  StructType schema, boolean isKeyedTable) {
    this.insertWriter = insertWriter;
    this.deleteWrite = deleteWrite;
    this.schema = schema;
    this.isKeyedTable = isKeyedTable;
  }

  @Override
  public void delete(InternalRow row) throws IOException {
    deleteWrite.write(new SparkInternalRowCastWrapper(row, ChangeAction.DELETE, schema));
  }

  @Override
  public void update(InternalRow updateBefore, InternalRow updateAfter) throws IOException {
    SparkInternalRowCastWrapper delete;
    SparkInternalRowCastWrapper insert;
    if (isKeyedTable) {
      delete = new SparkInternalRowCastWrapper(updateBefore, ChangeAction.UPDATE_BEFORE, schema);
      insert = new SparkInternalRowCastWrapper(updateAfter, ChangeAction.UPDATE_AFTER, schema);
    } else {
      delete = new SparkInternalRowCastWrapper(updateBefore, ChangeAction.DELETE, schema);
      insert = new SparkInternalRowCastWrapper(updateAfter, ChangeAction.INSERT, schema);
    }
    if (!rowIsAllNull(delete)) {
      insertWriter.write(delete);
    }
    insertWriter.write(insert);
  }

  private boolean rowIsAllNull(SparkInternalRowCastWrapper row) {
    boolean isAllNull = true;
    for (int i = 0; i < row.getSchema().size(); i++) {
      if (!row.getRow().isNullAt(i)) {
        isAllNull = false;
      }
    }
    return isAllNull;
  }

  @Override
  public void insert(InternalRow row) throws IOException {
    insertWriter.write(new SparkInternalRowCastWrapper(row, ChangeAction.INSERT, schema));
  }

  @Override
  public WriterCommitMessage commit() throws IOException {
    WriteResult insert = this.insertWriter.complete();
    WriteResult delete = this.deleteWrite.complete();
    return new WriteTaskCommit.Builder()
        .addDataFiles(insert.dataFiles())
        .addDataFiles(delete.dataFiles())
        .addDeleteFiles(insert.deleteFiles())
        .addDeleteFiles(delete.deleteFiles())
        .build();
  }

  @Override
  public void abort() throws IOException {
    if (this.insertWriter != null) {
      this.insertWriter.abort();
    }
    if (this.deleteWrite != null) {
      this.deleteWrite.abort();
    }
  }

  @Override
  public void close() throws IOException {
    if (this.insertWriter != null) {
      this.insertWriter.close();
    }
    if (this.deleteWrite != null) {
      this.deleteWrite.close();
    }
  }
}
