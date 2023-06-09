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

package com.netease.arctic.spark.io;

import com.netease.arctic.io.ArcticFileIO;
import com.netease.arctic.io.writer.BaseTaskWriter;
import com.netease.arctic.io.writer.OutputFileFactory;
import com.netease.arctic.spark.SparkInternalRowWrapper;
import com.netease.arctic.table.PrimaryKeySpec;
import org.apache.iceberg.FileFormat;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.StructLike;
import org.apache.iceberg.io.FileAppenderFactory;
import org.apache.iceberg.spark.SparkSchemaUtil;
import org.apache.spark.sql.catalyst.InternalRow;
import org.apache.spark.sql.types.StructType;

public class ArcticSparkBaseTaskWriter extends BaseTaskWriter<InternalRow> {

  private final StructType structType;

  protected ArcticSparkBaseTaskWriter(
      FileFormat format,
      FileAppenderFactory<InternalRow> appenderFactory,
      OutputFileFactory outputFileFactory,
      ArcticFileIO io,
      long targetFileSize,
      long mask,
      Schema schema,
      PartitionSpec spec,
      PrimaryKeySpec primaryKeySpec) {
    super(format, appenderFactory, outputFileFactory, io, targetFileSize,
        mask, schema, spec, primaryKeySpec, false);
    this.structType = SparkSchemaUtil.convert(schema);
  }

  @Override
  protected StructLike asStructLike(InternalRow data) {
    return new SparkInternalRowWrapper(structType).wrap(data);
  }
}
