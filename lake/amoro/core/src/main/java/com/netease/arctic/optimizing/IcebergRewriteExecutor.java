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

package com.netease.arctic.optimizing;

import com.netease.arctic.data.IcebergDataFile;
import com.netease.arctic.io.reader.GenericCombinedIcebergDataReader;
import com.netease.arctic.io.writer.IcebergFanoutPosDeleteWriter;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.utils.map.StructLikeCollections;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.StructLike;
import org.apache.iceberg.TableProperties;
import org.apache.iceberg.data.GenericAppenderFactory;
import org.apache.iceberg.data.IdentityPartitionConverters;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.deletes.EqualityDeleteWriter;
import org.apache.iceberg.deletes.PositionDelete;
import org.apache.iceberg.deletes.PositionDeleteWriter;
import org.apache.iceberg.encryption.EncryptedOutputFile;
import org.apache.iceberg.io.DataWriteResult;
import org.apache.iceberg.io.DataWriter;
import org.apache.iceberg.io.DeleteWriteResult;
import org.apache.iceberg.io.FileWriter;
import org.apache.iceberg.io.FileWriterFactory;
import org.apache.iceberg.io.OutputFileFactory;
import org.apache.iceberg.io.RollingDataWriter;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class IcebergRewriteExecutor extends AbstractRewriteFilesExecutor {

  public IcebergRewriteExecutor(
      RewriteFilesInput input,
      ArcticTable table,
      StructLikeCollections structLikeCollections) {
    super(input, table, structLikeCollections);
  }

  @Override
  protected OptimizingDataReader dataReader() {
    Set<String> set = new HashSet<>();
    if (input.rewrittenDataFiles() != null) {
      for (IcebergDataFile icebergContentFile : input.rewrittenDataFiles()) {
        set.add(icebergContentFile.path().toString());
      }
    }

    if (input.rePosDeletedDataFiles() != null) {
      for (IcebergDataFile icebergContentFile : input.rePosDeletedDataFiles()) {
        set.add(icebergContentFile.path().toString());
      }
    }
    return new GenericCombinedIcebergDataReader(
        io,
        table.schema(),
        table.spec(),
        table.properties().get(TableProperties.DEFAULT_NAME_MAPPING),
        false,
        IdentityPartitionConverters::convertConstant,
        false,
        structLikeCollections,
        input
    );
  }

  @Override
  protected FileWriter<PositionDelete<Record>, DeleteWriteResult> posWriter() {
    return new IcebergFanoutPosDeleteWriter<>(
        fullMetricAppenderFactory(), deleteFileFormat(), partition(), table.io(), table.asUnkeyedTable().encryption(),
        UUID.randomUUID().toString());
  }

  @Override
  protected FileWriter<Record, DataWriteResult> dataWriter() {
    OutputFileFactory outputFileFactory = OutputFileFactory
        .builderFor(table.asUnkeyedTable(), table.spec().specId(), 0).build();

    GenericAppenderFactory appenderFactory = new GenericAppenderFactory(table.schema(), table.spec());
    appenderFactory.setAll(table.properties());
    return new RollingDataWriter<>(
        new FileWriterFactory<Record>() {

          @Override
          public DataWriter newDataWriter(EncryptedOutputFile file, PartitionSpec spec, StructLike partition) {
            return appenderFactory.newDataWriter(file, dataFileFormat(), partition);
          }

          @Override
          public EqualityDeleteWriter newEqualityDeleteWriter(
              EncryptedOutputFile file,
              PartitionSpec spec,
              StructLike partition) {
            return null;
          }

          @Override
          public PositionDeleteWriter newPositionDeleteWriter(
              EncryptedOutputFile file,
              PartitionSpec spec,
              StructLike partition) {
            return null;
          }
        },
        outputFileFactory,
        io,
        targetSize(),
        table.spec(),
        partition()
    );
  }
}
