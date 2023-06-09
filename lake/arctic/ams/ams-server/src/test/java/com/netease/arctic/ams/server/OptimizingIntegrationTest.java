package com.netease.arctic.ams.server;

import com.netease.arctic.ams.server.optimize.IcebergHadoopOptimizingTest;
import com.netease.arctic.ams.server.optimize.MixedHiveOptimizingTest;
import com.netease.arctic.ams.server.optimize.MixedIcebergOptimizingTest;
import com.netease.arctic.ams.server.service.ServiceContainer;
import com.netease.arctic.catalog.ArcticCatalog;
import com.netease.arctic.catalog.CatalogLoader;
import com.netease.arctic.hive.TestHMS;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.KeyedTable;
import com.netease.arctic.table.PrimaryKeySpec;
import com.netease.arctic.table.TableBuilder;
import com.netease.arctic.table.TableIdentifier;
import com.netease.arctic.table.TableProperties;
import org.apache.hadoop.conf.Configuration;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.Table;
import org.apache.iceberg.Tables;
import org.apache.iceberg.hadoop.HadoopTables;
import org.apache.iceberg.relocated.com.google.common.collect.Maps;
import org.apache.iceberg.types.Types;
import org.apache.thrift.TException;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class OptimizingIntegrationTest {
  private static final Logger LOG = LoggerFactory.getLogger(OptimizingIntegrationTest.class);
  private static AmsEnvironment amsEnvironment;

  private static final String CATALOG = "local_catalog1";
  private static final String ICEBERG_CATALOG = "iceberg_catalog";
  private static final String HIVE_CATALOG = "hive_catalog";
  private static final String DATABASE = "test_db";
  private static String ICEBERG_CATALOG_DIR;
  private static final TableIdentifier TB_1 = TableIdentifier.of(CATALOG, DATABASE, "test_table1");
  private static final TableIdentifier TB_2 = TableIdentifier.of(CATALOG, DATABASE, "test_table2");
  private static final TableIdentifier TB_3 = TableIdentifier.of(CATALOG, DATABASE, "test_table3");
  private static final TableIdentifier TB_4 = TableIdentifier.of(CATALOG, DATABASE, "test_table4");
  private static final TableIdentifier TB_5 = TableIdentifier.of(ICEBERG_CATALOG, DATABASE, "iceberg_table5");
  private static final TableIdentifier TB_6 = TableIdentifier.of(ICEBERG_CATALOG, DATABASE, "iceberg_table6");
  private static final TableIdentifier TB_7 = TableIdentifier.of(ICEBERG_CATALOG, DATABASE, "iceberg_table7");
  private static final TableIdentifier TB_8 = TableIdentifier.of(ICEBERG_CATALOG, DATABASE, "iceberg_table8");
  private static final TableIdentifier TB_9 = TableIdentifier.of(HIVE_CATALOG, DATABASE, "hive_table9");
  private static final TableIdentifier TB_10 = TableIdentifier.of(HIVE_CATALOG, DATABASE, "hive_table10");
  private static final TableIdentifier TB_11 = TableIdentifier.of(CATALOG, DATABASE, "test_table11");
  private static final TableIdentifier TB_12 = TableIdentifier.of(ICEBERG_CATALOG, DATABASE, "iceberg_table12");

  private static final ConcurrentHashMap<String, ArcticCatalog> catalogsCache = new ConcurrentHashMap<>();

  private static final Schema SCHEMA = new Schema(
      Types.NestedField.required(1, "id", Types.IntegerType.get()),
      Types.NestedField.required(2, "name", Types.StringType.get()),
      Types.NestedField.required(3, "op_time", Types.TimestampType.withZone())
  );

  private static final PartitionSpec SPEC = PartitionSpec.builderFor(SCHEMA)
      .day("op_time").build();

  private static final PrimaryKeySpec PRIMARY_KEY = PrimaryKeySpec.builderFor(SCHEMA)
      .addColumn("id").build();

  @ClassRule
  public static TestHMS TEST_HMS = new TestHMS();

  @ClassRule
  public static TemporaryFolder TEMP_DIR = new TemporaryFolder();

  @BeforeClass
  public static void beforeClass() {
    String rootPath = TEMP_DIR.getRoot().getAbsolutePath();
    String CATALOG_DIR = rootPath + "/arctic/warehouse";
    ICEBERG_CATALOG_DIR = rootPath + "/iceberg/warehouse";
    LOG.info("TEMP folder {}", rootPath);
    catalogsCache.clear();
    amsEnvironment = new AmsEnvironment(rootPath);
    amsEnvironment.start();
    amsEnvironment.createMixedIcebergCatalog(CATALOG, CATALOG_DIR);
    amsEnvironment.createIcebergHadoopCatalog(ICEBERG_CATALOG, ICEBERG_CATALOG_DIR);
    amsEnvironment.createMixedHiveCatalog(HIVE_CATALOG, TEST_HMS.getHiveConf());
    catalog(CATALOG).createDatabase(DATABASE);
    catalog(ICEBERG_CATALOG).createDatabase(DATABASE);
    catalog(HIVE_CATALOG).createDatabase(DATABASE);
  }

  @AfterClass
  public static void afterClass() {
    amsEnvironment.stop();
    catalogsCache.clear();
  }

  @Test
  public void testPkTableOptimizing() {
    ArcticTable arcticTable = createArcticTable(TB_2, PRIMARY_KEY, PartitionSpec.unpartitioned());
    assertTableExist(TB_2);
    MixedIcebergOptimizingTest testCase = new MixedIcebergOptimizingTest(arcticTable, getOptimizeHistoryStartId());
    testCase.testKeyedTableContinueOptimizing();
  }

  @Test
  public void testPkPartitionTableOptimizing() {
    ArcticTable arcticTable = createArcticTable(TB_1, PRIMARY_KEY, SPEC);
    assertTableExist(TB_1);
    MixedIcebergOptimizingTest testCase = new MixedIcebergOptimizingTest(arcticTable, getOptimizeHistoryStartId());
    testCase.testKeyedTableContinueOptimizing();
  }

  @Test
  public void testNoPkTableOptimizing() {
    ArcticTable arcticTable = createArcticTable(TB_3, PrimaryKeySpec.noPrimaryKey(), PartitionSpec.unpartitioned());
    assertTableExist(TB_3);
    MixedIcebergOptimizingTest testCase = new MixedIcebergOptimizingTest(arcticTable, getOptimizeHistoryStartId());
    testCase.testNoPkTableOptimizing();
  }

  @Test
  public void testNoPkPartitionTableOptimizing() {
    ArcticTable arcticTable = createArcticTable(TB_4, PrimaryKeySpec.noPrimaryKey(), SPEC);
    assertTableExist(TB_4);
    MixedIcebergOptimizingTest testCase = new MixedIcebergOptimizingTest(arcticTable, getOptimizeHistoryStartId());
    testCase.testNoPkPartitionTableOptimizing();
  }

  @Test
  public void testKeyedTableTxIdNotInOrder() {
    ArcticTable arcticTable = createArcticTable(TB_11, PRIMARY_KEY, PartitionSpec.unpartitioned());
    assertTableExist(TB_11);
    MixedIcebergOptimizingTest testCase = new MixedIcebergOptimizingTest(arcticTable, getOptimizeHistoryStartId());
    testCase.testKeyedTableTxIdNotInOrder();
  }

  @Test
  public void testIcebergTableFullOptimize() throws IOException {
    Table table = createIcebergTable(TB_5, PartitionSpec.unpartitioned());
    assertTableExist(TB_5);
    IcebergHadoopOptimizingTest testCase = new IcebergHadoopOptimizingTest(TB_5, table, getOptimizeHistoryStartId());
    testCase.testIcebergTableFullOptimize();
  }

  @Test
  public void testIcebergTableOptimizing() throws IOException {
    Table table = createIcebergTable(TB_6, PartitionSpec.unpartitioned());
    assertTableExist(TB_6);
    IcebergHadoopOptimizingTest testCase = new IcebergHadoopOptimizingTest(TB_6, table, getOptimizeHistoryStartId());
    testCase.testIcebergTableOptimizing();
  }

  @Test
  public void testPartitionIcebergTableOptimizing() throws IOException {
    Table table = createIcebergTable(TB_7, SPEC);
    assertTableExist(TB_7);
    IcebergHadoopOptimizingTest testCase = new IcebergHadoopOptimizingTest(TB_7, table, getOptimizeHistoryStartId());
    testCase.testPartitionIcebergTableOptimizing();
  }

  @Test
  public void testPartitionIcebergTablePartialOptimizing() throws IOException {
    Table table = createIcebergTable(TB_12, SPEC);
    assertTableExist(TB_12);
    IcebergHadoopOptimizingTest testCase = new IcebergHadoopOptimizingTest(TB_12, table, getOptimizeHistoryStartId());
    testCase.testPartitionIcebergTablePartialOptimizing();
  }

  @Test
  public void testV1IcebergTableOptimizing() throws IOException {
    Table table = createIcebergV1Table(TB_8, PartitionSpec.unpartitioned());
    assertTableExist(TB_8);
    IcebergHadoopOptimizingTest testCase = new IcebergHadoopOptimizingTest(TB_8, table, getOptimizeHistoryStartId());
    testCase.testV1IcebergTableOptimizing();
  }

  @Test
  public void testHiveKeyedTableMajorOptimizeNotMove() throws TException, IOException {
    createHiveArcticTable(TB_9, PRIMARY_KEY, PartitionSpec.unpartitioned());
    assertTableExist(TB_9);
    KeyedTable table = catalog(HIVE_CATALOG).loadTable(TB_9).asKeyedTable();
    MixedHiveOptimizingTest testCase =
        new MixedHiveOptimizingTest(table, TEST_HMS.getHiveClient(), getOptimizeHistoryStartId());
    testCase.testHiveKeyedTableMajorOptimizeNotMove();
  }

  @Test
  public void testHiveKeyedTableMajorOptimizeAndMove() throws TException, IOException {
    createHiveArcticTable(TB_10, PRIMARY_KEY, PartitionSpec.unpartitioned());
    assertTableExist(TB_10);
    KeyedTable table = catalog(HIVE_CATALOG).loadTable(TB_10).asKeyedTable();
    MixedHiveOptimizingTest testCase =
        new MixedHiveOptimizingTest(table, TEST_HMS.getHiveClient(), getOptimizeHistoryStartId());
    testCase.testHiveKeyedTableMajorOptimizeAndMove();
  }

  private static long getOptimizeHistoryStartId() {
    return ServiceContainer.getOptimizeService().maxOptimizeHistoryId();
  }

  private static ArcticCatalog catalog(String name) {
    return catalogsCache.computeIfAbsent(name, n -> CatalogLoader.load(amsEnvironment.getAmsUrl() + "/" + n));
  }

  private ArcticTable createArcticTable(TableIdentifier tableIdentifier, PrimaryKeySpec primaryKeySpec,
                                        PartitionSpec partitionSpec) {

    TableBuilder tableBuilder = catalog(CATALOG).newTableBuilder(tableIdentifier, SCHEMA)
        .withPrimaryKeySpec(primaryKeySpec)
        .withPartitionSpec(partitionSpec)
        .withProperty(TableProperties.SELF_OPTIMIZING_MINOR_TRIGGER_INTERVAL, "1000")
        .withProperty(TableProperties.SELF_OPTIMIZING_MAJOR_TRIGGER_INTERVAL, "1000");

    return tableBuilder.create();
  }

  private Table createIcebergTable(TableIdentifier tableIdentifier, PartitionSpec partitionSpec) {
    Tables hadoopTables = new HadoopTables(new Configuration());
    Map<String, String> tableProperties = Maps.newHashMap();
    tableProperties.put(org.apache.iceberg.TableProperties.FORMAT_VERSION, "2");
    tableProperties.put(TableProperties.SELF_OPTIMIZING_MINOR_TRIGGER_FILE_CNT, "2");

    return hadoopTables.create(SCHEMA, partitionSpec, tableProperties,
        ICEBERG_CATALOG_DIR + "/" + tableIdentifier.getDatabase() + "/" + tableIdentifier.getTableName());
  }

  private void createHiveArcticTable(TableIdentifier tableIdentifier, PrimaryKeySpec primaryKeySpec,
                                 PartitionSpec partitionSpec) {

    TableBuilder tableBuilder = catalog(HIVE_CATALOG).newTableBuilder(tableIdentifier, SCHEMA)
        .withPrimaryKeySpec(primaryKeySpec)
        .withPartitionSpec(partitionSpec)
        .withProperty(TableProperties.SELF_OPTIMIZING_MINOR_TRIGGER_INTERVAL, "1000")
        .withProperty(TableProperties.SELF_OPTIMIZING_MAJOR_TRIGGER_INTERVAL, "1000");

    tableBuilder.create();
  }

  private Table createIcebergV1Table(TableIdentifier tableIdentifier, PartitionSpec partitionSpec) {
    Tables hadoopTables = new HadoopTables(new Configuration());
    Map<String, String> tableProperties = Maps.newHashMap();
    tableProperties.put(org.apache.iceberg.TableProperties.FORMAT_VERSION, "1");
    tableProperties.put(TableProperties.SELF_OPTIMIZING_MINOR_TRIGGER_FILE_CNT, "2");

    return hadoopTables.create(SCHEMA, partitionSpec, tableProperties,
        ICEBERG_CATALOG_DIR + "/" + tableIdentifier.getDatabase() + "/" + tableIdentifier.getTableName());
  }

  private void assertTableExist(TableIdentifier tableIdentifier) {
    List<TableIdentifier> tableIdentifiers = amsEnvironment.refreshTables();
    Assert.assertTrue(tableIdentifiers.contains(tableIdentifier));
  }
}
