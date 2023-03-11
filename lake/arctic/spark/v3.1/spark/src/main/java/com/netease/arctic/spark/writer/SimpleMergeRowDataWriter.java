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

public class SimpleMergeRowDataWriter implements RowLevelWriter<InternalRow> {
  final TaskWriter<InternalRow> writer;
  
  final StructType schema;
  final boolean isKeyedTable;

  public SimpleMergeRowDataWriter(TaskWriter<InternalRow> writer, StructType schema, boolean isKeyedTable) {
    this.writer = writer;
    this.schema = schema;
    this.isKeyedTable = isKeyedTable;
  }

  @Override
  public void delete(InternalRow row) throws IOException {
    writer.write(new SparkInternalRowCastWrapper(row, ChangeAction.DELETE));
  }

  @Override
  public void update(InternalRow updateBefore, InternalRow updateAfter) throws IOException {
    SparkInternalRowCastWrapper delete;
    SparkInternalRowCastWrapper insert;
    if (isKeyedTable) {
      delete = new SparkInternalRowCastWrapper(updateBefore, ChangeAction.UPDATE_BEFORE);
      insert = new SparkInternalRowCastWrapper(updateAfter, ChangeAction.UPDATE_AFTER);
    } else {
      delete = new SparkInternalRowCastWrapper(updateBefore, ChangeAction.DELETE);
      insert = new SparkInternalRowCastWrapper(updateAfter, ChangeAction.INSERT);
    }
    writer.write(delete);
    writer.write(insert);

  }

  @Override
  public void insert(InternalRow row) throws IOException {
    writer.write(new SparkInternalRowCastWrapper(row, ChangeAction.INSERT));

  }

  @Override
  public WriterCommitMessage commit() throws IOException {
    WriteResult result = writer.complete();
    return new WriteTaskCommit(result.dataFiles(), result.deleteFiles());
  }

  @Override
  public void abort() throws IOException {
    if (this.writer != null) {
      this.writer.abort();
    }
  }

  @Override
  public void close() throws IOException {
    if (this.writer != null) {
      writer.close();
    }
  }
}
