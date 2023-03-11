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

package com.netease.arctic.io;

import com.netease.arctic.ams.api.properties.TableFormat;
import com.netease.arctic.catalog.TableTestBase;
import com.netease.arctic.io.writer.IcebergFanoutPosDeleteWriter;
import com.netease.arctic.utils.TableFileUtils;
import org.apache.hadoop.fs.Path;
import org.apache.iceberg.DeleteFile;
import org.apache.iceberg.FileFormat;
import org.apache.iceberg.MetadataColumns;
import org.apache.iceberg.MetricsModes;
import org.apache.iceberg.Schema;
import org.apache.iceberg.StructLike;
import org.apache.iceberg.TestHelpers;
import org.apache.iceberg.data.GenericAppenderFactory;
import org.apache.iceberg.data.GenericRecord;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.io.DeleteSchemaUtil;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RunWith(Parameterized.class)
public class IcebergFanoutPosDeleteWriterTest extends TableTestBase {

  private final FileFormat fileFormat;

  public IcebergFanoutPosDeleteWriterTest(boolean partitionedTable, FileFormat fileFormat) {
    super(TableFormat.ICEBERG, false, partitionedTable);
    this.fileFormat = fileFormat;
  }

  @Parameterized.Parameters(name = "partitionedTable = {0}, fileFormat = {1}")
  public static Object[][] parameters() {
    return new Object[][] {{true, FileFormat.PARQUET}, {false, FileFormat.PARQUET},
                           {true, FileFormat.AVRO}, {false, FileFormat.AVRO},
                           {true, FileFormat.ORC}, {false, FileFormat.ORC}};
  }

  private StructLike getPartitionData() {
    if (isPartitionedTable()) {
      return TestHelpers.Row.of(0);
    } else {
      return TestHelpers.Row.of();
    }
  }

  @Test
  public void testWritePosDelete() throws IOException {
    StructLike partitionData = getPartitionData();
    GenericAppenderFactory appenderFactory =
        new GenericAppenderFactory(getArcticTable().schema(), getArcticTable().spec());
    appenderFactory.setAll(getArcticTable().properties());
    appenderFactory.set(
        org.apache.iceberg.TableProperties.METRICS_MODE_COLUMN_CONF_PREFIX + MetadataColumns.DELETE_FILE_PATH.name(),
        MetricsModes.Full.get().toString());
    appenderFactory.set(
        org.apache.iceberg.TableProperties.METRICS_MODE_COLUMN_CONF_PREFIX + MetadataColumns.DELETE_FILE_POS.name(),
        MetricsModes.Full.get().toString());

    IcebergFanoutPosDeleteWriter<Record> icebergPosDeleteWriter = new IcebergFanoutPosDeleteWriter<>(
        appenderFactory, fileFormat, partitionData, getArcticTable().io(),
        getArcticTable().asUnkeyedTable().encryption(), "suffix");

    String dataDir = temp.newFolder("data").getPath();

    String dataFile1Path =
        new Path(TableFileUtils.getNewFilePath(dataDir, fileFormat.addExtension("data-1"))).toString();
    icebergPosDeleteWriter.delete(dataFile1Path, 0);
    icebergPosDeleteWriter.delete(dataFile1Path, 1);
    icebergPosDeleteWriter.delete(dataFile1Path, 3);

    String dataFile2Path =
        new Path(TableFileUtils.getNewFilePath(dataDir, fileFormat.addExtension("data-2"))).toString();
    icebergPosDeleteWriter.delete(dataFile2Path, 10);
    icebergPosDeleteWriter.delete(dataFile2Path, 9);
    icebergPosDeleteWriter.delete(dataFile2Path, 8);

    List<DeleteFile> deleteFiles = icebergPosDeleteWriter.complete();
    Assert.assertEquals(2, deleteFiles.size());
    Map<String, DeleteFile> deleteFileMap = deleteFiles.stream().collect(Collectors.toMap(
        f -> f.path().toString(),
        f -> f));
    DeleteFile deleteFile1 = deleteFileMap.get(
        new Path(TableFileUtils.getNewFilePath(dataDir, fileFormat.addExtension("data-1-delete-suffix"))).toString());
    Assert.assertNotNull(deleteFile1);
    Assert.assertEquals(3, deleteFile1.recordCount());
    // Check whether the path-pos pairs are sorted as expected.
    Schema pathPosSchema = DeleteSchemaUtil.pathPosSchema();
    Record record = GenericRecord.create(pathPosSchema);
    List<Record> expectedDeletes =
        Lists.newArrayList(
            record.copy("file_path", dataFile1Path, "pos", 0L),
            record.copy("file_path", dataFile1Path, "pos", 1L),
            record.copy("file_path", dataFile1Path, "pos", 3L));
    Assert.assertEquals(expectedDeletes, DataTestHelpers.readDataFile(fileFormat, pathPosSchema, deleteFile1.path()));

    DeleteFile deleteFile2 = deleteFileMap.get(
        new Path(TableFileUtils.getNewFilePath(dataDir, fileFormat.addExtension("data-2-delete-suffix"))).toString());
    Assert.assertNotNull(deleteFile2);
    Assert.assertEquals(
        new Path(TableFileUtils.getNewFilePath(dataDir, fileFormat.addExtension("data-2-delete-suffix"))).toString(),
        deleteFile2.path().toString());
    Assert.assertEquals(3, deleteFile2.recordCount());
    // Check whether the path-pos pairs are sorted as expected.
    expectedDeletes =
        Lists.newArrayList(
            record.copy("file_path", dataFile2Path, "pos", 8L),
            record.copy("file_path", dataFile2Path, "pos", 9L),
            record.copy("file_path", dataFile2Path, "pos", 10L));
    Assert.assertEquals(
        expectedDeletes,
        DataTestHelpers.readDataFile(fileFormat, pathPosSchema, deleteFile2.path()));
  }
}
