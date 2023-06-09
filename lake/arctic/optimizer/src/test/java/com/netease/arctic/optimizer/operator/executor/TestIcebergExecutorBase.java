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

package com.netease.arctic.optimizer.operator.executor;

import com.google.common.collect.Maps;
import com.netease.arctic.TableTestBase;
import com.netease.arctic.ams.api.CatalogMeta;
import com.netease.arctic.ams.api.MockArcticMetastoreServer;
import com.netease.arctic.ams.api.OptimizeTaskId;
import com.netease.arctic.ams.api.OptimizeType;
import com.netease.arctic.ams.api.properties.CatalogMetaProperties;
import com.netease.arctic.catalog.ArcticCatalog;
import com.netease.arctic.catalog.CatalogLoader;
import com.netease.arctic.data.file.ContentFileWithSequence;
import com.netease.arctic.table.ArcticTable;
import org.apache.hadoop.conf.Configuration;
import org.apache.iceberg.CatalogProperties;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.DeleteFile;
import org.apache.iceberg.FileFormat;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.TableProperties;
import org.apache.iceberg.catalog.Catalog;
import org.apache.iceberg.catalog.TableIdentifier;
import org.apache.iceberg.data.GenericAppenderFactory;
import org.apache.iceberg.data.GenericRecord;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.deletes.EqualityDeleteWriter;
import org.apache.iceberg.deletes.PositionDelete;
import org.apache.iceberg.deletes.PositionDeleteWriter;
import org.apache.iceberg.encryption.EncryptedOutputFile;
import org.apache.iceberg.io.DataWriter;
import org.apache.iceberg.io.OutputFileFactory;
import org.apache.iceberg.relocated.com.google.common.collect.ImmutableList;
import org.apache.iceberg.relocated.com.google.common.collect.ImmutableMap;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.apache.iceberg.util.ArrayUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static com.netease.arctic.ams.api.properties.CatalogMetaProperties.CATALOG_TYPE_HADOOP;
import static org.apache.iceberg.CatalogUtil.ICEBERG_CATALOG_TYPE;
import static org.apache.iceberg.CatalogUtil.ICEBERG_CATALOG_TYPE_HADOOP;

public class TestIcebergExecutorBase {

  @ClassRule
  public static final TemporaryFolder tempFolder = new TemporaryFolder();
  protected static final String ICEBERG_HADOOP_CATALOG_NAME = "iceberg_hadoop";

  protected ArcticCatalog icebergCatalog;
  protected ArcticTable icebergTable;

  protected static final String DATABASE = "native_test_db";
  protected static final String TABLE_NAME = "native_test_tb";
  protected TableIdentifier tableIdentifier = TableIdentifier.of(DATABASE, TABLE_NAME);

  @BeforeClass
  public static void createIcebergCatalog() throws IOException {
    Map<String, String> storageConfig = new HashMap<>();
    storageConfig.put(
        CatalogMetaProperties.STORAGE_CONFIGS_KEY_TYPE,
        CatalogMetaProperties.STORAGE_CONFIGS_VALUE_TYPE_HDFS);
    storageConfig.put(CatalogMetaProperties.STORAGE_CONFIGS_KEY_CORE_SITE, MockArcticMetastoreServer.getHadoopSite());
    storageConfig.put(CatalogMetaProperties.STORAGE_CONFIGS_KEY_HDFS_SITE, MockArcticMetastoreServer.getHadoopSite());

    Map<String, String> authConfig = new HashMap<>();
    authConfig.put(CatalogMetaProperties.AUTH_CONFIGS_KEY_TYPE,
        CatalogMetaProperties.AUTH_CONFIGS_VALUE_TYPE_SIMPLE);
    authConfig.put(CatalogMetaProperties.AUTH_CONFIGS_KEY_HADOOP_USERNAME,
        System.getProperty("user.name"));

    tempFolder.create();
    Map<String, String> catalogProperties = new HashMap<>();
    catalogProperties.put(CatalogProperties.WAREHOUSE_LOCATION, tempFolder.newFolder().getPath());
    catalogProperties.put(CatalogMetaProperties.TABLE_FORMATS, "iceberg");

    CatalogMeta catalogMeta = new CatalogMeta(ICEBERG_HADOOP_CATALOG_NAME, CATALOG_TYPE_HADOOP,
        storageConfig, authConfig, catalogProperties);
    MockArcticMetastoreServer.getInstance().createCatalogIfAbsent(catalogMeta);
  }

  @Before
  public void initTable() throws Exception {
    icebergCatalog =
        CatalogLoader.load(MockArcticMetastoreServer.getInstance().getUrl(ICEBERG_HADOOP_CATALOG_NAME));
    icebergCatalog.createDatabase(DATABASE);
    CatalogMeta catalogMeta = MockArcticMetastoreServer.getInstance().handler().getCatalog(ICEBERG_HADOOP_CATALOG_NAME);
    Map<String, String> catalogProperties = Maps.newHashMap(catalogMeta.getCatalogProperties());
    catalogProperties.put(ICEBERG_CATALOG_TYPE, ICEBERG_CATALOG_TYPE_HADOOP);
    Catalog nativeIcebergCatalog = org.apache.iceberg.CatalogUtil.buildIcebergCatalog(ICEBERG_HADOOP_CATALOG_NAME,
        catalogProperties, new Configuration());
    Map<String, String> tableProperty = new HashMap<>();
    tableProperty.put(TableProperties.FORMAT_VERSION, "2");
    nativeIcebergCatalog.createTable(tableIdentifier, TableTestBase.TABLE_SCHEMA,
        PartitionSpec.unpartitioned(), tableProperty);
    icebergTable = icebergCatalog.loadTable(
        com.netease.arctic.table.TableIdentifier.of(ICEBERG_HADOOP_CATALOG_NAME, DATABASE, TABLE_NAME));
  }

  @After
  public void clear() throws Exception {
    CatalogMeta catalogMeta = MockArcticMetastoreServer.getInstance().handler().getCatalog(ICEBERG_HADOOP_CATALOG_NAME);
    Map<String, String> catalogProperties = Maps.newHashMap(catalogMeta.getCatalogProperties());
    catalogProperties.put(ICEBERG_CATALOG_TYPE, catalogMeta.getCatalogType());
    Catalog nativeIcebergCatalog = org.apache.iceberg.CatalogUtil.buildIcebergCatalog(ICEBERG_HADOOP_CATALOG_NAME,
        catalogProperties, new Configuration());
    nativeIcebergCatalog.dropTable(tableIdentifier);
    icebergCatalog.dropDatabase(DATABASE);
    tempFolder.delete();
  }

  protected DataFile insertDataFiles( int recordNumber, int startId) throws IOException {
    GenericAppenderFactory appenderFactory = new GenericAppenderFactory(icebergTable.schema(), icebergTable.spec());
    OutputFileFactory outputFileFactory =
        OutputFileFactory.builderFor(icebergTable.asUnkeyedTable(), icebergTable.spec().specId(), 1)
            .build();
    EncryptedOutputFile outputFile = outputFileFactory.newOutputFile();

    DataWriter<Record> writer = appenderFactory
        .newDataWriter(outputFile, FileFormat.PARQUET, null);

    for (Record record : baseRecords(startId, recordNumber, icebergTable.schema())) {
      writer.write(record);
    }
    writer.close();
    return writer.toDataFile();
  }

  protected DeleteFile insertEqDeleteFiles(Integer... deleteIds) throws IOException {
    List<Integer> equalityFieldIds = Lists.newArrayList(icebergTable.schema().findField("id").fieldId());
    Schema eqDeleteRowSchema = icebergTable.schema().select("id");
    GenericAppenderFactory appenderFactory =
        new GenericAppenderFactory(icebergTable.schema(), icebergTable.spec(),
            ArrayUtil.toIntArray(equalityFieldIds), eqDeleteRowSchema, null);
    OutputFileFactory outputFileFactory =
        OutputFileFactory.builderFor(icebergTable.asUnkeyedTable(), icebergTable.spec().specId(), 1)
            .build();
    EncryptedOutputFile outputFile = outputFileFactory.newOutputFile();

    EqualityDeleteWriter<Record> writer = appenderFactory
        .newEqDeleteWriter(outputFile, FileFormat.PARQUET, null);

    GenericRecord record = GenericRecord.create(eqDeleteRowSchema);
    for (Integer deleteId : deleteIds) {
      writer.write(record.copy("id", deleteId));
    }
    writer.close();
    return writer.toDeleteFile();
  }

  protected DeleteFile insertPosDeleteFiles(DataFile dataFile, Integer... positions) throws IOException {
    GenericAppenderFactory appenderFactory =
        new GenericAppenderFactory(icebergTable.schema(), icebergTable.spec());
    OutputFileFactory outputFileFactory =
        OutputFileFactory.builderFor(icebergTable.asUnkeyedTable(), icebergTable.spec().specId(), 1)
            .build();
    EncryptedOutputFile outputFile = outputFileFactory.newOutputFile();

    PositionDeleteWriter<Record> writer = appenderFactory
        .newPosDeleteWriter(outputFile, FileFormat.PARQUET, null);
    for (Integer position : positions) {
      PositionDelete<Record> positionDelete = PositionDelete.create();
      positionDelete.set(dataFile.path().toString(), position, null);
      writer.write(positionDelete);
    }
    writer.close();
    return writer.toDeleteFile();
  }

  protected List<Record> baseRecords(int start, int length, Schema tableSchema) {
    GenericRecord record = GenericRecord.create(tableSchema);

    ImmutableList.Builder<Record> builder = ImmutableList.builder();
    for (int i = start; i < start + length; i++) {
      builder.add(record.copy(ImmutableMap.of("id", i, "name", "name",
          "op_time", LocalDateTime.of(2022, 1, 1, 12, 0, 0))));
    }

    return builder.build();
  }

  protected NodeTask constructNodeTask(List<ContentFileWithSequence<?>> dataFiles,
      List<ContentFileWithSequence<?>> smallDataFiles,
      List<ContentFileWithSequence<?>> posDeleteFiles, List<ContentFileWithSequence<?>> equDeleteFiles, OptimizeType optimizeType) {
    NodeTask nodeTask = new NodeTask(dataFiles, smallDataFiles, equDeleteFiles, posDeleteFiles, false);
    nodeTask.setTableIdentifier(icebergTable.id());
    nodeTask.setTaskId(new OptimizeTaskId(optimizeType, UUID.randomUUID().toString()));
    nodeTask.setAttemptId(Math.abs(ThreadLocalRandom.current().nextInt()));
    return nodeTask;
  }

}
