package com.netease.arctic.ams.server.optimize;

import com.google.common.collect.Maps;
import com.netease.arctic.TableTestBase;
import com.netease.arctic.ams.api.CatalogMeta;
import com.netease.arctic.ams.server.AmsTestBase;
import com.netease.arctic.ams.server.model.BasicOptimizeTask;
import com.netease.arctic.ams.server.model.TableOptimizeRuntime;
import com.netease.arctic.ams.server.service.ServiceContainer;
import com.netease.arctic.table.ArcticTable;
import org.apache.hadoop.conf.Configuration;
import org.apache.iceberg.AppendFiles;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.DeleteFile;
import org.apache.iceberg.FileFormat;
import org.apache.iceberg.MetadataColumns;
import org.apache.iceberg.MetricsModes;
import org.apache.iceberg.PartitionKey;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.RowDelta;
import org.apache.iceberg.Schema;
import org.apache.iceberg.StructLike;
import org.apache.iceberg.Table;
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
import org.apache.iceberg.util.PropertyUtil;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.netease.arctic.ams.server.AmsTestBase.AMS_TEST_ICEBERG_CATALOG_NAME;
import static org.apache.iceberg.CatalogUtil.ICEBERG_CATALOG_TYPE;
import static org.apache.iceberg.CatalogUtil.ICEBERG_CATALOG_TYPE_HADOOP;

public class TestIcebergBase {
  ArcticTable icebergNoPartitionTable;
  ArcticTable icebergPartitionTable;

  static final String DATABASE = "native_test_db";
  static final String NO_PARTITION_TABLE_NAME = "native_test_npt_tb";
  static final String PARTITION_TABLE_NAME = "native_test_pt_tb";
  TableIdentifier noPartitionTableIdentifier = TableIdentifier.of(DATABASE, NO_PARTITION_TABLE_NAME);
  TableIdentifier partitionTableIdentifier = TableIdentifier.of(DATABASE, PARTITION_TABLE_NAME);

  protected static final PartitionSpec SPEC = PartitionSpec.builderFor(TableTestBase.TABLE_SCHEMA)
      .identity("name").build();
  static final String DEFAULT_PARTITION = "name1";

  @BeforeClass
  public static void createDatabase() {
    AmsTestBase.icebergCatalog.createDatabase(DATABASE);
  }

  @AfterClass
  public static void clearCatalog() {
    AmsTestBase.icebergCatalog.dropDatabase(DATABASE);
  }

  @Before
  public void initTable() throws Exception {
    CatalogMeta catalogMeta = ServiceContainer.getCatalogMetadataService()
        .getCatalog(AMS_TEST_ICEBERG_CATALOG_NAME).get();
    Map<String, String> catalogProperties = Maps.newHashMap(catalogMeta.getCatalogProperties());
    catalogProperties.put(ICEBERG_CATALOG_TYPE, ICEBERG_CATALOG_TYPE_HADOOP);
    Catalog nativeIcebergCatalog = org.apache.iceberg.CatalogUtil.buildIcebergCatalog(AMS_TEST_ICEBERG_CATALOG_NAME,
        catalogProperties, new Configuration());
    Map<String, String> tableProperty = new HashMap<>();
    tableProperty.put(TableProperties.FORMAT_VERSION, "2");

    // init unPartitionTable
    nativeIcebergCatalog.createTable(noPartitionTableIdentifier, TableTestBase.TABLE_SCHEMA, PartitionSpec.unpartitioned(), tableProperty);
    icebergNoPartitionTable = AmsTestBase.icebergCatalog.loadTable(
        com.netease.arctic.table.TableIdentifier.of(AMS_TEST_ICEBERG_CATALOG_NAME, DATABASE, NO_PARTITION_TABLE_NAME));

    // init partitionTable
    nativeIcebergCatalog.createTable(partitionTableIdentifier, TableTestBase.TABLE_SCHEMA, SPEC, tableProperty);
    icebergPartitionTable = AmsTestBase.icebergCatalog.loadTable(
        com.netease.arctic.table.TableIdentifier.of(AMS_TEST_ICEBERG_CATALOG_NAME, DATABASE, PARTITION_TABLE_NAME));
  }

  @After
  public void clearTable() throws Exception {
    CatalogMeta catalogMeta = ServiceContainer.getCatalogMetadataService()
        .getCatalog(AMS_TEST_ICEBERG_CATALOG_NAME).get();
    Map<String, String> catalogProperties = Maps.newHashMap(catalogMeta.getCatalogProperties());
    catalogProperties.put(ICEBERG_CATALOG_TYPE, catalogMeta.getCatalogType());
    Catalog nativeIcebergCatalog = org.apache.iceberg.CatalogUtil.buildIcebergCatalog(AMS_TEST_ICEBERG_CATALOG_NAME,
        catalogProperties, new Configuration());
    nativeIcebergCatalog.dropTable(noPartitionTableIdentifier);
    nativeIcebergCatalog.dropTable(partitionTableIdentifier);
  }

  protected List<DataFile> insertDataFiles(Table arcticTable, String partition, int fileCnt, int recordCnt, int fromId)
      throws IOException {
    PartitionKey partitionKey = null;
    if (!arcticTable.spec().isUnpartitioned()) {
      partitionKey = new PartitionKey(arcticTable.spec(), arcticTable.schema());
      partitionKey.partition(baseRecords(0, 1, partition, arcticTable.schema()).get(0));
    }
    GenericAppenderFactory appenderFactory = new GenericAppenderFactory(arcticTable.schema(), arcticTable.spec());
    OutputFileFactory outputFileFactory =
        OutputFileFactory.builderFor(arcticTable, arcticTable.spec().specId(), 1)
            .build();

    List<DataFile> result = new ArrayList<>();
    
    for (int i = 0; i < fileCnt; i++) {
      EncryptedOutputFile newOutputFile;
      if (partitionKey != null) {
        newOutputFile = outputFileFactory.newOutputFile(partitionKey);
      } else {
        newOutputFile = outputFileFactory.newOutputFile();
      }
      DataWriter<Record> writer = appenderFactory
          .newDataWriter(newOutputFile, FileFormat.PARQUET, partitionKey);
      int currentFromId = recordCnt * i + fromId;
      for (Record record : baseRecords(currentFromId, recordCnt, partition, arcticTable.schema())) {
        writer.write(record);
      }
      writer.close();
      result.add(writer.toDataFile());
    }

    AppendFiles baseAppend = arcticTable.newAppend();
    result.forEach(baseAppend::appendFile);
    baseAppend.commit();

    return result;
  }

  protected List<DataFile> insertDataFiles(Table arcticTable, int fileCnt, int recordCnt) throws IOException {
    return insertDataFiles(arcticTable, DEFAULT_PARTITION, fileCnt, recordCnt, 1);
  }

  protected void insertEqDeleteFiles(Table arcticTable, int id) throws IOException {
    insertEqDeleteFiles(arcticTable, DEFAULT_PARTITION, id);
  }

  protected void insertEqDeleteFiles(Table arcticTable, String partition, int id) throws IOException {
    PartitionKey partitionKey = null;
    if (!arcticTable.spec().isUnpartitioned()) {
      partitionKey = new PartitionKey(arcticTable.spec(), arcticTable.schema());
      partitionKey.partition(baseRecords(id, 1, partition, arcticTable.schema()).get(0));
    }
    List<Integer> equalityFieldIds = Lists.newArrayList(arcticTable.schema().findField("id").fieldId());
    Schema eqDeleteRowSchema = arcticTable.schema().select("id");
    GenericAppenderFactory appenderFactory =
        new GenericAppenderFactory(arcticTable.schema(), arcticTable.spec(),
            ArrayUtil.toIntArray(equalityFieldIds), eqDeleteRowSchema, null);
    OutputFileFactory outputFileFactory =
        OutputFileFactory.builderFor(arcticTable, arcticTable.spec().specId(), 1)
            .build();
    EncryptedOutputFile outputFile;
    if (partitionKey != null) {
      outputFile = outputFileFactory.newOutputFile(partitionKey);
    } else {
      outputFile = outputFileFactory.newOutputFile();
    }

    List<DeleteFile> result = new ArrayList<>();
    EqualityDeleteWriter<Record> writer = appenderFactory
        .newEqDeleteWriter(outputFile, FileFormat.PARQUET, partitionKey);

    List<Record> records = baseRecords(id, 1, partition, arcticTable.schema());
    for (Record record : records) {
      writer.write(record);
    }
    writer.close();
    result.add(writer.toDeleteFile());

    RowDelta rowDelta = arcticTable.newRowDelta();
    result.forEach(rowDelta::addDeletes);
    rowDelta.commit();
  }

  protected List<DeleteFile> insertPosDeleteFiles(Table arcticTable, List<DataFile> dataFiles) throws IOException {
    StructLike partitionKey = dataFiles.get(0).partition();
    GenericAppenderFactory appenderFactory =
        new GenericAppenderFactory(arcticTable.schema(), arcticTable.spec());
    appenderFactory.set(
        org.apache.iceberg.TableProperties.METRICS_MODE_COLUMN_CONF_PREFIX + MetadataColumns.DELETE_FILE_PATH.name(),
        MetricsModes.Full.get().toString());
    appenderFactory.set(
        org.apache.iceberg.TableProperties.METRICS_MODE_COLUMN_CONF_PREFIX + MetadataColumns.DELETE_FILE_POS.name(),
        MetricsModes.Full.get().toString());
    OutputFileFactory outputFileFactory =
        OutputFileFactory.builderFor(arcticTable, arcticTable.spec().specId(), 1)
            .build();
    EncryptedOutputFile outputFile;
    if (partitionKey != null) {
      outputFile = outputFileFactory.newOutputFile(partitionKey);
    } else {
      outputFile = outputFileFactory.newOutputFile();
    }

    List<DeleteFile> result = new ArrayList<>();
    PositionDeleteWriter<Record> writer = appenderFactory
        .newPosDeleteWriter(outputFile, FileFormat.PARQUET, partitionKey);
    for (DataFile dataFile : dataFiles) {
      PositionDelete<Record> positionDelete = PositionDelete.create();
      positionDelete.set(dataFile.path().toString(), 0L, null);
      writer.write(positionDelete);
    }
    writer.close();
    result.add(writer.toDeleteFile());

    RowDelta rowDelta = arcticTable.newRowDelta();
    result.forEach(rowDelta::addDeletes);
    rowDelta.commit();

    return result;
  }

  public List<Record> baseRecords(int start, int length, String partition, Schema tableSchema) {
    GenericRecord record = GenericRecord.create(tableSchema);

    ImmutableList.Builder<Record> builder = ImmutableList.builder();
    for (int i = start; i < start + length; i++) {
      builder.add(record.copy(ImmutableMap.of("id", i, "name", partition,
          "op_time", LocalDateTime.of(2022, 1, 1, 12, 0, 0))));
    }

    return builder.build();
  }

  protected void assertPlanResult(OptimizePlanResult planResult, int affectPartitions, int tasksCnt, long snapshotId) {
    Assert.assertEquals(tasksCnt, planResult.getOptimizeTasks().size());
    Assert.assertEquals(affectPartitions, planResult.getAffectPartitions().size());
    Assert.assertEquals(snapshotId, planResult.getSnapshotId());
    Assert.assertEquals(TableOptimizeRuntime.INVALID_SNAPSHOT_ID, planResult.getChangeSnapshotId());
  }

  protected void assertTask(BasicOptimizeTask task, String partition, int baseCnt, int insertCnt, int deleteCnt,
                          int posDeleteCnt) {
    Assert.assertEquals(partition, task.getPartition());
    Assert.assertEquals(baseCnt, task.getBaseFileCnt());
    Assert.assertEquals(baseCnt, task.getBaseFiles().size());
    Assert.assertEquals(insertCnt, task.getInsertFileCnt());
    Assert.assertEquals(insertCnt, task.getInsertFiles().size());
    Assert.assertEquals(deleteCnt, task.getDeleteFileCnt());
    Assert.assertEquals(deleteCnt, task.getDeleteFiles().size());
    Assert.assertEquals(posDeleteCnt, task.getPosDeleteFileCnt());
    Assert.assertEquals(posDeleteCnt, task.getPosDeleteFiles().size());
    if (baseCnt > 0) {
      Assert.assertTrue(task.getBaseFileSize() > 0);
    } else {
      Assert.assertEquals(0, task.getBaseFileSize());
    }
    if (insertCnt > 0) {
      Assert.assertTrue(task.getInsertFileSize() > 0);
    } else {
      Assert.assertEquals(0, task.getInsertFileSize());
    }
    if (deleteCnt > 0) {
      Assert.assertTrue(task.getDeleteFileSize() > 0);
    } else {
      Assert.assertEquals(0, task.getDeleteFileSize());
    }
    if (posDeleteCnt > 0) {
      Assert.assertTrue(task.getPosDeleteFileSize() > 0);
    } else {
      Assert.assertEquals(0, task.getPosDeleteFileSize());
    }
  }
}
