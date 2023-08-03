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

package com.netease.arctic.io.reader;

import com.netease.arctic.io.ArcticFileIO;
import org.apache.iceberg.Accessor;
import org.apache.iceberg.DeleteFile;
import org.apache.iceberg.MetadataColumns;
import org.apache.iceberg.Schema;
import org.apache.iceberg.StructLike;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.data.parquet.GenericParquetReaders;
import org.apache.iceberg.io.CloseableIterable;
import org.apache.iceberg.io.InputFile;
import org.apache.iceberg.parquet.Parquet;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;

import java.util.List;

/**
 * Reader for positional delete files.
 */
public class BaseIcebergPosDeleteReader {

  private static final Schema POS_DELETE_SCHEMA = new Schema(
      MetadataColumns.DELETE_FILE_PATH,
      MetadataColumns.DELETE_FILE_POS);
  private static final Accessor<StructLike> FILENAME_ACCESSOR = POS_DELETE_SCHEMA
      .accessorForField(MetadataColumns.DELETE_FILE_PATH.fieldId());
  private static final Accessor<StructLike> POSITION_ACCESSOR = POS_DELETE_SCHEMA
      .accessorForField(MetadataColumns.DELETE_FILE_POS.fieldId());

  protected final ArcticFileIO fileIO;
  protected final List<DeleteFile> posDeleteFiles;

  public BaseIcebergPosDeleteReader(ArcticFileIO fileIO, List<DeleteFile> posDeleteFiles) {
    this.fileIO = fileIO;
    this.posDeleteFiles = posDeleteFiles;
  }

  public CloseableIterable<Record> readDeletes() {
    List<CloseableIterable<Record>> deletes = Lists.transform(posDeleteFiles, this::readDelete);
    return CloseableIterable.concat(deletes);
  }

  public String readPath(Record record) {
    return (String) FILENAME_ACCESSOR.get(record);
  }

  public Long readPos(Record record) {
    return (Long) POSITION_ACCESSOR.get(record);
  }

  private CloseableIterable<Record> readDelete(DeleteFile deleteFile) {
    InputFile input = fileIO.newInputFile(deleteFile.path().toString());
    switch (deleteFile.format()) {
      case PARQUET:
        Parquet.ReadBuilder builder = Parquet.read(input)
            .project(POS_DELETE_SCHEMA)
            .reuseContainers()
            .createReaderFunc(fileSchema -> GenericParquetReaders.buildReader(POS_DELETE_SCHEMA, fileSchema));

        return fileIO.doAs(builder::build);
      default:
        throw new UnsupportedOperationException(String.format(
            "Cannot read deletes, %s is not a supported format: %s", deleteFile.format().name(), deleteFile.path()));
    }
  }
}
