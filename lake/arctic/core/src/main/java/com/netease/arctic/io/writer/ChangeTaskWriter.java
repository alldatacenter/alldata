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

package com.netease.arctic.io.writer;

import com.netease.arctic.data.ChangeAction;
import com.netease.arctic.data.DataFileType;
import com.netease.arctic.io.ArcticFileIO;
import com.netease.arctic.table.PrimaryKeySpec;
import org.apache.iceberg.FileFormat;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.io.FileAppenderFactory;

import java.io.IOException;

/**
 * Abstract implementation of writer for {@link com.netease.arctic.table.ChangeTable}.
 * @param <T> to indicate the record data type
 */
public abstract class ChangeTaskWriter<T> extends BaseTaskWriter<T> {

  private long fileOffset = 0L;

  protected ChangeTaskWriter(
      FileFormat format,
      FileAppenderFactory<T> appenderFactory,
      OutputFileFactory outputFileFactory,
      ArcticFileIO io,
      long targetFileSize,
      long mask,
      Schema schema,
      PartitionSpec spec,
      PrimaryKeySpec primaryKeySpec,
      boolean orderedWriter) {
    super(
        format, appenderFactory, outputFileFactory, io, targetFileSize,
        mask, schema, spec, primaryKeySpec, orderedWriter
    );
  }

  @Override
  protected DataWriterKey buildWriterKey(T row) {
    DataWriterKey key = super.buildWriterKey(row);
    ChangeAction action = action(row);
    switch (action) {
      case INSERT:
      case UPDATE_AFTER:
        return new DataWriterKey(key.getPartitionKey(), key.getTreeNode(), DataFileType.INSERT_FILE);
      case DELETE:
      case UPDATE_BEFORE:
        return new DataWriterKey(key.getPartitionKey(), key.getTreeNode(), DataFileType.EQ_DELETE_FILE);
      default:
        throw new IllegalArgumentException("Unknown action:" + action.name());
    }
  }

  @Override
  protected void write(TaskDataWriter<T> writer, T row) throws IOException {
    super.write(writer, appendFileOffset(row));
  }

  private T appendFileOffset(T row) {
    ChangeAction action = action(row);
    switch (action) {
      case INSERT:
      case DELETE:
      case UPDATE_BEFORE:
        return appendMetaColumns(row, ++fileOffset);
      case UPDATE_AFTER:
        return appendMetaColumns(row, fileOffset);
      default:
        throw new IllegalArgumentException("Unknown action:" + action.name());
    }
  }

  /**
   * Append row sequence to the end of data
   * @param data data to be appended
   * @param fileOffset file offset to append
   * @return data with row sequence appended
   */
  protected abstract T appendMetaColumns(T data, Long fileOffset);

  /**
   * Get the {@link ChangeAction} of data
   * @param data source data
   * @return the action of data
   */
  protected abstract ChangeAction action(T data);
}
