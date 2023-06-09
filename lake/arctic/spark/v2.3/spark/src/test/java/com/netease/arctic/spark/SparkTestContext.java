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

package com.netease.arctic.spark;

import com.google.common.collect.Sets;
import com.netease.arctic.ams.api.CatalogMeta;
import com.netease.arctic.ams.api.MockArcticMetastoreServer;
import com.netease.arctic.ams.api.NoSuchObjectException;
import com.netease.arctic.ams.api.TableMeta;
import com.netease.arctic.ams.api.client.AmsClientPools;
import com.netease.arctic.catalog.ArcticCatalog;
import com.netease.arctic.catalog.CatalogLoader;
import com.netease.arctic.data.ChangeAction;
import com.netease.arctic.data.DataTreeNode;
import com.netease.arctic.data.file.FileNameGenerator;
import com.netease.arctic.hive.HMSMockServer;
import com.netease.arctic.hive.io.writer.AdaptHiveGenericTaskWriterBuilder;
import com.netease.arctic.io.writer.GenericTaskWriters;
import com.netease.arctic.io.writer.SortedPosDeleteWriter;
import com.netease.arctic.op.OverwriteBaseFiles;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.KeyedTable;
import com.netease.arctic.table.LocationKind;
import com.netease.arctic.table.TableIdentifier;
import com.netease.arctic.table.UnkeyedTable;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.iceberg.AppendFiles;
import org.apache.iceberg.ContentFile;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.DeleteFile;
import org.apache.iceberg.FileScanTask;
import org.apache.iceberg.OverwriteFiles;
import org.apache.iceberg.RowDelta;
import org.apache.iceberg.Schema;
import org.apache.iceberg.StructLike;
import org.apache.iceberg.data.GenericRecord;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.io.CloseableIterable;
import org.apache.iceberg.io.TaskWriter;
import org.apache.iceberg.relocated.com.google.common.collect.ImmutableList;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.apache.iceberg.relocated.com.google.common.collect.Maps;
import org.apache.iceberg.util.StructLikeMap;
import org.apache.spark.SparkConf;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.internal.SQLConf;
import org.apache.thrift.TException;
import org.junit.Assert;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * test context for all spark tests.
 */
public class SparkTestContext extends ExternalResource {
  protected static final Object ANY = new Object();
  final static ConcurrentHashMap<String, ArcticCatalog> catalogs = new ConcurrentHashMap<>();
  private static final Logger LOG = LoggerFactory.getLogger(SparkTestBase.class);

  protected static File testBaseDir = new File("unit_test_base_tmp");
  protected static File testSparkDir = new File(testBaseDir, "spark-warehouse");
  protected static File testArcticDir = new File(testBaseDir, "arctic");
  protected static SparkSession spark = null;
  protected static MockArcticMetastoreServer ams = new MockArcticMetastoreServer();
  protected static String amsUrl;
  protected static String catalogName;
  protected List<Object[]> rows;
  static final File hmsDir = new File(testBaseDir, "hive");
  protected static final HMSMockServer hms = new HMSMockServer(hmsDir);

  protected static final AtomicInteger arcticSetupCount = new AtomicInteger(0);
  protected static final AtomicInteger hiveSetupCount = new AtomicInteger(0);
  protected static final AtomicInteger sparkSetupCount = new AtomicInteger(0);

  public static Map<String, String> setUpTestDirAndArctic() throws IOException {
    Map<String, String> configs = Maps.newHashMap();

    if (arcticSetupCount.incrementAndGet() > 1) {
      return configs;
    }

    System.out.println("======================== start AMS  ========================= ");
    org.apache.commons.io.FileUtils.deleteQuietly(testBaseDir);
    testBaseDir.mkdirs();

    AmsClientPools.cleanAll();
    if (!ams.isStarted()) {
      ams.start();
    }
    amsUrl = "thrift://127.0.0.1:" + ams.port();
    return configs;
  }

  public static Map<String, String> setUpHMS() throws IOException, ClassNotFoundException {
    Map<String, String> configs = Maps.newHashMap();
    String hiveVersion = SparkTestContext.class.getClassLoader()
        .loadClass("org.apache.hadoop.hive.metastore.HiveMetaStoreClient")
        .getPackage()
        .getImplementationVersion();


    configs.put("spark.sql.catalogImplementation", "hive");
    configs.put("hive.metastore.uris", "thrift://127.0.0.1:" + hms.getMetastorePort());
    configs.put("spark.sql.arctic.delegate.enabled", "true");
    //hive.metastore.client.capability.check
    configs.put("hive.metastore.client.capability.check", "false");

    if (hiveSetupCount.incrementAndGet() > 1) {
      return configs;
    }

    System.out.println("======================== start hive metastore ========================= ");
    hms.hiveConf().set("datanucleus.autoCreateTables", "true");
    hms.start();
    HiveConf entries = hms.hiveConf();
    CatalogMeta arctic_hive = ArcticCatalogMetaTestUtil.createArcticCatalog(testArcticDir, entries);
    catalogName = arctic_hive.getCatalogName();
    ams.handler().createCatalog(arctic_hive);

    configs.put("spark.sql.arctic.catalog." + catalogName, ArcticCatalog.class.getName());
    configs.put("spark.sql.arctic.catalog.type", "hive");
    configs.put("spark.sql.arctic.catalog.url" , amsUrl + "/" + catalogName);
    return configs;
  }

  public static void setUpSparkSession(Map<String, String> configs) {
    if (sparkSetupCount.incrementAndGet() > 1) {
      return;
    }

    System.out.println("======================== set up spark session  ========================= ");
    Map<String, String> sparkConfigs = Maps.newHashMap();

    sparkConfigs.put(SQLConf.PARTITION_OVERWRITE_MODE().key(), "DYNAMIC");
    sparkConfigs.put("spark.executor.heartbeatInterval", "300s");
    sparkConfigs.put("spark.network.timeout", "500s");
    sparkConfigs.put("spark.sql.warehouse.dir", testSparkDir.getAbsolutePath());
    sparkConfigs.put("spark.sql.extensions", ArcticSparkExtensions.class.getName());
    sparkConfigs.put("spark.testing.memory", "471859200");
    sparkConfigs.put("spark.cores.max", "6");
    sparkConfigs.put("spark.executor.cores", "2");
    sparkConfigs.put("spark.default.parallelism", "12");

    sparkConfigs.putAll(configs);
    sparkConfigs.forEach(((k, v) -> System.out.println("--" + k + "=" + v)));

    SparkConf sparkconf = new SparkConf()
        .setAppName("test")
        .setMaster("local");

    sparkConfigs.forEach(sparkconf::set);

    spark = SparkSession
        .builder()
        .config(sparkconf)
        .getOrCreate();
    spark.sparkContext().setLogLevel("WARN");
  }

  public static void cleanUpAms() {
    if (arcticSetupCount.decrementAndGet() > 0) {
      return;
    }
    System.out.println("======================== clean up AMS  ========================= ");
    ams.handler().cleanUp();
    AmsClientPools.cleanAll();
  }

  public static void cleanUpHive() {
    if (hiveSetupCount.decrementAndGet() > 0) {
      return;
    }
    System.out.println("======================== stop hive metastore ========================= ");
    hms.stop();
  }

  public static void cleanUpSparkSession() {
    if (sparkSetupCount.decrementAndGet() > 0) {
      return;
    }
    System.out.println("======================== clean up spark session  ========================= ");
    spark.stop();
    spark.close();
    spark = null;
  }

  public static ArcticCatalog catalog(String name) {
    return catalogs.computeIfAbsent(name, n -> CatalogLoader.load(amsUrl + "/" + n));
  }

  public static ArcticTable loadTable(String catalog, String database, String table) {
    return SparkTestContext.loadTable(TableIdentifier.of(catalog, database, table));
  }

  public static ArcticTable loadTable(TableIdentifier identifier) {
    ArcticCatalog catalog = CatalogLoader.load(amsUrl + "/" + identifier.getCatalog());
    return catalog.loadTable(identifier);
  }

  public static Map<String, String> loadTablePropertiesFromAms(TableIdentifier identifier) {
    try {
      TableMeta meta = ams.handler().getTable(identifier.buildTableIdentifier());
      return meta.getProperties();
    } catch (TException e) {
      throw new IllegalStateException(e);
    }
  }

  public static List<DataFile> writeBase(TableIdentifier identifier, List<Object[]> records) {
    KeyedTable table = SparkTestContext.catalog(identifier.getCatalog()).loadTable(identifier).asKeyedTable();
    long txId = table.beginTransaction(System.currentTimeMillis() + "");
    List<DataFile> baseDataFiles = new ArrayList<>();
    try (TaskWriter<Record> writer = GenericTaskWriters.builderFor(table)
        .withTransactionId(txId)
        .buildBaseWriter()) {
      records.forEach(row -> {
        try {
          Record r = SparkTestContext.newRecord(table, row);
          writer.write(r);
        } catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      });
      baseDataFiles.addAll(Arrays.asList(writer.complete().dataFiles()));
      AppendFiles appendFiles = table.baseTable().newAppend();
      baseDataFiles.forEach(appendFiles::appendFile);
      appendFiles.commit();
      return baseDataFiles;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static GenericRecord newRecord(KeyedTable table, Object... val) {
    GenericRecord writeRecord = GenericRecord.create(table.schema());
    for (int i = 0; i < val.length; i++) {
      writeRecord.set(i, val[i]);
    }
    return writeRecord;
  }

  public static GenericRecord newRecord(Schema schema, Object... val) {
    GenericRecord writeRecord = GenericRecord.create(schema);
    for (int i = 0; i < val.length; i++) {
      writeRecord.set(i, val[i]);
    }
    return writeRecord;
  }

  public static void writeChange(TableIdentifier identifier, ChangeAction action, List<Record> rows) {
    KeyedTable table = SparkTestContext.catalog(identifier.getCatalog()).loadTable(identifier).asKeyedTable();
    try (TaskWriter<Record> writer = GenericTaskWriters.builderFor(table)
        .withChangeAction(action)
        .buildChangeWriter()) {
      rows.forEach(row -> {
        try {
          writer.write(row);
        } catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      });
      AppendFiles appendFiles = table.changeTable().newAppend();
      Arrays.stream(writer.complete().dataFiles())
          .forEach(appendFiles::appendFile);
      appendFiles.commit();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static OffsetDateTime ofDateWithZone(int year, int mon, int day, int hour) {
    LocalDateTime dateTime = LocalDateTime.of(year, mon, day, hour, 0);
    return OffsetDateTime.of(dateTime, ZoneOffset.ofHours(0));
  }

  public static OffsetDateTime quickDateWithZone(int day) {
    return SparkTestContext.ofDateWithZone(2022, 1, day, 0);
  }

  public static void assertContainIdSet(List<Object[]> rows, int idIndex, Object... idList) {
    Set<Object> idSet = Sets.newHashSet();
    rows.forEach(r -> idSet.add(r[idIndex]));
    for (Object id : idList) {
      if (!idSet.contains(id)) {
        throw new AssertionError("assert id contain " + id + ", but not found");
      }
    }
  }

  public static StructLikeMap<List<DataFile>> partitionFiles(UnkeyedTable table) {
    // List<DataFile> dataFiles = Lists.newArrayList();
    StructLikeMap<List<DataFile>> map = StructLikeMap.create(table.spec().partitionType());
    try (CloseableIterable<FileScanTask> tasks = table.newScan().planFiles()) {
      tasks.forEach(t -> {
        DataFile d = t.file();
        StructLike pd = d.partition();
        if (!map.containsKey(pd)) {
          map.put(pd, Lists.newArrayList());
        }
        map.get(pd).add(d);
      });
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
    return map;
  }

  protected List<Object[]> sql(String query, Object... args) throws RuntimeException {
    MessageFormat format = new MessageFormat(query);
    String sql = format.format(args);
    if (args.length == 0) {
      sql = query;
    }
    LOG.info("execute sql: " + sql);
    Dataset<Row> result = spark.sql(sql);
    List<Row> rows = result.collectAsList();
    if (rows.size() < 1) {
      LOG.info("empty result");
      this.rows = new ArrayList<>();
      return ImmutableList.of();
    }
    result.show(false);
    this.rows = rows.stream()
        .map(row -> IntStream.range(0, row.size())
            .mapToObj(pos -> row.isNullAt(pos) ? null : row.get(pos))
            .toArray(Object[]::new)
        ).collect(Collectors.toList());
    return this.rows;
  }

  protected void assertEquals(String context, List<Object[]> expectedRows, List<Object[]> actualRows) {
    Assert.assertEquals(context + ": number of results should match", expectedRows.size(), actualRows.size());
    for (int row = 0; row < expectedRows.size(); row += 1) {
      Object[] expected = expectedRows.get(row);
      Object[] actual = actualRows.get(row);
      Assert.assertEquals("Number of columns should match", expected.length, actual.length);
      for (int col = 0; col < actualRows.get(row).length; col += 1) {
        if (expected[col] != ANY) {
          Assert.assertEquals(context + ": row " + row + " col " + col + " contents should match",
              expected[col], actual[col]);
        }
      }
    }
  }

  protected void assertTableExist(TableIdentifier ident) {
    ArcticCatalog catalog = catalog(ident.getCatalog());
    boolean exists = catalog.tableExists(ident);
    Assert.assertTrue("table should exist", exists);
  }

  protected void assertTableNotExist(TableIdentifier identifier) {
    Assert.assertThrows(NoSuchObjectException.class, () -> ams.handler().getTable(identifier.buildTableIdentifier()));
  }

  protected static Timestamp quickTs(int day) {
    return Timestamp.valueOf(quickDateWithZone(day).toLocalDateTime());
  }

  public void assertDescResult(List<Object[]> rows, List<String> primaryKeys) {
    boolean primaryKeysBlock = false;
    List<String> descPrimaryKeys = Lists.newArrayList();
    for (Object[] row : rows) {
      if (StringUtils.equalsIgnoreCase("# Primary keys", row[0].toString())) {
        primaryKeysBlock = true;
      } else if (StringUtils.startsWith(row[0].toString(), "# ") && primaryKeysBlock) {
        primaryKeysBlock = false;
      } else if (primaryKeysBlock) {
        descPrimaryKeys.add(row[0].toString());
      }
    }

    Assert.assertEquals(primaryKeys.size(), descPrimaryKeys.size());
    Assert.assertArrayEquals(
        primaryKeys.stream().sorted().distinct().toArray(),
        descPrimaryKeys.stream().sorted().distinct().toArray());
  }

  public void assertPartitionResult(List<Object[]> rows, List<String> partitionKey) {
    boolean primaryKeysBlock = false;
    List<String> descPrimaryKeys = Lists.newArrayList();
    for (Object[] row : rows) {
      if (StringUtils.equalsIgnoreCase("# Partitioning", row[0].toString())) {
        primaryKeysBlock = true;
      } else if (StringUtils.startsWith(row[0].toString(), "Part ") && primaryKeysBlock) {
        descPrimaryKeys.add(row[1].toString());
      }
    }

    Assert.assertEquals(partitionKey.size(), descPrimaryKeys.size());
    Assert.assertArrayEquals(
        partitionKey.stream().sorted().distinct().toArray(),
        descPrimaryKeys.stream().sorted().distinct().toArray());
  }

  public void assertHiveDesc(List<Object[]> rows, List<String> cols, List<String> partitionKey) {
    boolean colsBlock = true;
    boolean partitionBlock = false;
    List<String> descCols = Lists.newArrayList();
    List<String> descPartitionKey = Lists.newArrayList();
    for (Object[] row : rows) {
      if (StringUtils.startsWith(row[0].toString(), "# Partition Infor")) {
        partitionBlock = true;
        colsBlock = false;
      } else if (colsBlock) {
        descCols.add(row[0].toString());
      } else if (partitionBlock && !StringUtils.startsWith(row[0].toString(), "#")) {
        descPartitionKey.add(row[0].toString());
      }
    }
    Assert.assertArrayEquals(
        cols.stream().sorted().distinct().toArray(),
        descCols.stream().sorted().distinct().toArray());
    Assert.assertArrayEquals(
        partitionKey.stream().sorted().distinct().toArray(),
        descPartitionKey.stream().sorted().distinct().toArray());
  }

  public List<DataFile> writeHive(ArcticTable table, LocationKind locationKind, List<Record> records)
      throws IOException {
    Long txId = null;
    if (table.isKeyedTable()) {
      txId = table.asKeyedTable().beginTransaction(System.currentTimeMillis() + "");
    }
    AdaptHiveGenericTaskWriterBuilder builder = AdaptHiveGenericTaskWriterBuilder
        .builderFor(table)
        .withTransactionId(txId);

    TaskWriter<Record> changeWrite = builder.buildWriter(locationKind);
    for (Record record : records) {
      changeWrite.write(record);
    }
    DataFile[] dataFiles = changeWrite.complete().dataFiles();
    if (table.isKeyedTable()) {
      KeyedTable keyedTable = table.asKeyedTable();
      OverwriteBaseFiles overwriteBaseFiles = keyedTable.newOverwriteBaseFiles();
      Arrays.stream(dataFiles).forEach(overwriteBaseFiles::addFile);
      overwriteBaseFiles.updateOptimizedSequenceDynamically(txId);
      overwriteBaseFiles.commit();
    } else if (table.isUnkeyedTable()) {
      UnkeyedTable unkeyedTable = table.asUnkeyedTable();
      OverwriteFiles overwriteFiles = unkeyedTable.newOverwrite();
      Arrays.stream(dataFiles).forEach(overwriteFiles::addFile);
      overwriteFiles.commit();
    } else {
      throw new IllegalStateException("Table is neither keyed nor unkeyed");
    }
    return Arrays.asList(dataFiles);
  }

  public void adaptHiveInsertPosDeleteFiles(Long transactionId, List<DataFile> dataFiles, ArcticTable table)
      throws IOException {
    Map<StructLike, List<DataFile>> dataFilesPartitionMap =
        new HashMap<>(dataFiles.stream().collect(Collectors.groupingBy(ContentFile::partition)));
    List<DeleteFile> deleteFiles = new ArrayList<>();
    for (Map.Entry<StructLike, List<DataFile>> dataFilePartitionMap : dataFilesPartitionMap.entrySet()) {
      StructLike partition = dataFilePartitionMap.getKey();
      List<DataFile> partitionFiles = dataFilePartitionMap.getValue();
      Map<DataTreeNode, List<DataFile>> nodeFilesPartitionMap = new HashMap<>(partitionFiles.stream()
          .collect(Collectors.groupingBy(dataFile ->
              FileNameGenerator.parseFileNodeFromFileName(dataFile.path().toString()))));
      for (Map.Entry<DataTreeNode, List<DataFile>> nodeFilePartitionMap : nodeFilesPartitionMap.entrySet()) {
        DataTreeNode key = nodeFilePartitionMap.getKey();
        List<DataFile> nodeFiles = nodeFilePartitionMap.getValue();

        // write pos delete
        SortedPosDeleteWriter<Record> writer = AdaptHiveGenericTaskWriterBuilder
            .builderFor(table)
            .withTransactionId(transactionId).buildBasePosDeleteWriter(key.getMask(), key.getIndex(), partition);
        for (DataFile nodeFile : nodeFiles) {
          writer.delete(nodeFile.path(), 0);
        }
        deleteFiles.addAll(writer.complete());
      }
    }
    if (table.isKeyedTable()) {
      RowDelta rowDelta = table.asKeyedTable().baseTable().newRowDelta();
      deleteFiles.forEach(rowDelta::addDeletes);
      rowDelta.commit();
    } else if (table.isUnkeyedTable()) {
      RowDelta rowDelta = table.asUnkeyedTable().newRowDelta();
      deleteFiles.forEach(rowDelta::addDeletes);
      rowDelta.commit();
    } else {
      throw new IllegalStateException("Table is neither keyed nor unkeyed");
    }
  }
}
