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
import com.netease.arctic.iceberg.optimize.InternalRecordWrapper;
import com.netease.arctic.io.ArcticFileIO;
import com.netease.arctic.table.PrimaryKeySpec;
import com.netease.arctic.utils.SchemaUtil;
import org.apache.iceberg.FileFormat;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.StructLike;
import org.apache.iceberg.data.GenericRecord;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.io.FileAppenderFactory;

/**
 * Implementation of {@link ChangeTaskWriter} to write {@link Record}.
 */
public class GenericChangeTaskWriter extends ChangeTaskWriter<Record> {

  private final Schema joinSchema;
  private final InternalRecordWrapper wrapper;
  private final ChangeAction writeAction;

  public GenericChangeTaskWriter(
      FileFormat format,
      FileAppenderFactory<Record> appenderFactory,
      OutputFileFactory outputFileFactory,
      ArcticFileIO io,
      long targetFileSize,
      long mask,
      Schema schema,
      PartitionSpec spec,
      PrimaryKeySpec primaryKeySpec,
      ChangeAction writeAction,
      boolean orderedWriter) {
    super(
        format, appenderFactory, outputFileFactory, io,
        targetFileSize, mask, schema, spec, primaryKeySpec,orderedWriter
    );
    this.joinSchema = SchemaUtil.changeWriteSchema(schema);
    this.wrapper = new InternalRecordWrapper(schema.asStruct());
    this.writeAction = writeAction;
  }

  @Override
  protected StructLike asStructLike(Record data) {
    return wrapper.wrap(data);
  }

  @Override
  protected Record appendMetaColumns(Record data, Long fileOffset) {
    GenericRecord joinRecord = GenericRecord.create(joinSchema);
    int i = 0;
    for (; i < data.size(); i++) {
      joinRecord.set(i, data.get(i));
    }
    joinRecord.set(i, fileOffset);
    return joinRecord;
  }

  @Override
  protected ChangeAction action(Record data) {
    return writeAction;
  }
}
