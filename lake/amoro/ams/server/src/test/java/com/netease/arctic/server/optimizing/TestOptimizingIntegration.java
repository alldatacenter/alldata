package com.netease.arctic.server.optimizing;

import com.netease.arctic.server.AmsEnvironment;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.KeyedTable;
import com.netease.arctic.table.PrimaryKeySpec;
import com.netease.arctic.table.TableBuilder;
import com.netease.arctic.table.TableIdentifier;
import com.netease.arctic.table.TableProperties;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.types.Types;
import org.apache.thrift.TException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;

@Disabled
public class TestOptimizingIntegration {

  private static AmsEnvironment amsEnv = AmsEnvironment.getIntegrationInstances();


  private static final String DATABASE = "optimizing_integration_test_db";

  private static final TableIdentifier MIXED_ICEBERG_TB_1 =
      TableIdentifier.of(AmsEnvironment.MIXED_ICEBERG_CATALOG, DATABASE, "mix_iceberg_table1");
  private static final TableIdentifier MIXED_ICEBERG_TB_2 =
      TableIdentifier.of(AmsEnvironment.MIXED_ICEBERG_CATALOG, DATABASE, "mix_iceberg_table2");
  private static final TableIdentifier MIXED_ICEBERG_TB_3 =
      TableIdentifier.of(AmsEnvironment.MIXED_ICEBERG_CATALOG, DATABASE, "mix_iceberg_table3");
  private static final TableIdentifier MIXED_ICEBERG_TB_4 =
      TableIdentifier.of(AmsEnvironment.MIXED_ICEBERG_CATALOG, DATABASE, "mix_iceberg_table4");
  private static final TableIdentifier MIXED_ICEBERG_TB_5 =
      TableIdentifier.of(AmsEnvironment.MIXED_ICEBERG_CATALOG, DATABASE, "mix_iceberg_table5");
  private static final TableIdentifier MIXED_ICEBERG_TB_6 =
      TableIdentifier.of(AmsEnvironment.MIXED_ICEBERG_CATALOG, DATABASE, "mix_iceberg_table6");
  private static final TableIdentifier MIXED_ICEBERG_TB_7 =
      TableIdentifier.of(AmsEnvironment.MIXED_ICEBERG_CATALOG, DATABASE, "mix_iceberg_table7");
  private static final TableIdentifier MIXED_HIVE_TB_1 =
      TableIdentifier.of(AmsEnvironment.MIXED_HIVE_CATALOG, DATABASE, "mix_hive_table1");
  private static final TableIdentifier MIXED_HIVE_TB_2 = TableIdentifier.of(AmsEnvironment.MIXED_HIVE_CATALOG,
      DATABASE, "mix_hive_table2");
  private static final Schema SCHEMA = new Schema(
      Types.NestedField.required(1, "id", Types.IntegerType.get()),
      Types.NestedField.required(2, "name", Types.StringType.get()),
      Types.NestedField.required(3, "op_time", Types.TimestampType.withZone())
  );
  private static final PartitionSpec SPEC = PartitionSpec.builderFor(SCHEMA)
      .day("op_time").build();
  private static final PrimaryKeySpec PRIMARY_KEY = PrimaryKeySpec.builderFor(SCHEMA)
      .addColumn("id").build();

  @BeforeAll
  public static void before() throws Exception {
    amsEnv.start();
    amsEnv.startOptimizer();
    amsEnv.createDatabaseIfNotExists(AmsEnvironment.ICEBERG_CATALOG, DATABASE);
    amsEnv.createDatabaseIfNotExists(AmsEnvironment.MIXED_ICEBERG_CATALOG, DATABASE);
    amsEnv.createDatabaseIfNotExists(AmsEnvironment.MIXED_HIVE_CATALOG, DATABASE);
  }

  @AfterAll
  public static void after() throws IOException {
    amsEnv.stop();
  }


  @Test
  public void testPkTableOptimizing() {
    ArcticTable arcticTable = createArcticTable(MIXED_ICEBERG_TB_1, PRIMARY_KEY, PartitionSpec.unpartitioned());
    assertTableExist(MIXED_ICEBERG_TB_1);
    TestMixedIcebergOptimizing testCase = new TestMixedIcebergOptimizing(arcticTable);
    testCase.testKeyedTableContinueOptimizing();
  }

  @Test
  public void testPkPartitionTableOptimizing() {
    ArcticTable arcticTable = createArcticTable(MIXED_ICEBERG_TB_2, PRIMARY_KEY, SPEC);
    assertTableExist(MIXED_ICEBERG_TB_2);
    TestMixedIcebergOptimizing testCase = new TestMixedIcebergOptimizing(arcticTable);
    testCase.testKeyedTableContinueOptimizing();
  }

  @Test
  public void testPkTableMajorOptimizeLeftPosDelete() {
    ArcticTable arcticTable = createArcticTable(MIXED_ICEBERG_TB_3, PRIMARY_KEY, PartitionSpec.unpartitioned());
    assertTableExist(MIXED_ICEBERG_TB_3);
    TestMixedIcebergOptimizing testCase = new TestMixedIcebergOptimizing(arcticTable);
    testCase.testPkTableMajorOptimizeLeftPosDelete();
  }

  @Test
  public void testNoPkTableOptimizing() {
    ArcticTable arcticTable = createArcticTable(MIXED_ICEBERG_TB_4, PrimaryKeySpec.noPrimaryKey(),
        PartitionSpec.unpartitioned());
    assertTableExist(MIXED_ICEBERG_TB_4);
    TestMixedIcebergOptimizing testCase = new TestMixedIcebergOptimizing(arcticTable);
    testCase.testNoPkTableOptimizing();
  }

  @Test
  public void testNoPkPartitionTableOptimizing() {
    ArcticTable arcticTable = createArcticTable(MIXED_ICEBERG_TB_5, PrimaryKeySpec.noPrimaryKey(), SPEC);
    assertTableExist(MIXED_ICEBERG_TB_5);
    TestMixedIcebergOptimizing testCase = new TestMixedIcebergOptimizing(arcticTable);
    testCase.testNoPkPartitionTableOptimizing();
  }

  @Test
  public void testKeyedTableTxIdNotInOrder() {
    ArcticTable arcticTable = createArcticTable(MIXED_ICEBERG_TB_6, PRIMARY_KEY, PartitionSpec.unpartitioned());
    assertTableExist(MIXED_ICEBERG_TB_6);
    TestMixedIcebergOptimizing testCase = new TestMixedIcebergOptimizing(arcticTable);
    testCase.testKeyedTableTxIdNotInOrder();
  }

  @Test
  public void testHiveKeyedTableMajorOptimizeNotMove() throws TException, IOException {
    createHiveArcticTable(MIXED_HIVE_TB_1, PRIMARY_KEY, PartitionSpec.unpartitioned());
    assertTableExist(MIXED_HIVE_TB_1);
    KeyedTable table = amsEnv.catalog(AmsEnvironment.MIXED_HIVE_CATALOG).loadTable(MIXED_HIVE_TB_1).asKeyedTable();
    TestMixedHiveOptimizing testCase =
        new TestMixedHiveOptimizing(table, amsEnv.getTestHMS().getClient());
    testCase.testHiveKeyedTableMajorOptimizeNotMove();
  }

  @Test
  public void testHiveKeyedTableMajorOptimizeAndMove() throws TException, IOException {
    createHiveArcticTable(MIXED_HIVE_TB_2, PRIMARY_KEY, PartitionSpec.unpartitioned());
    assertTableExist(MIXED_HIVE_TB_2);
    KeyedTable table = amsEnv.catalog(AmsEnvironment.MIXED_HIVE_CATALOG).loadTable(MIXED_HIVE_TB_2).asKeyedTable();
    TestMixedHiveOptimizing testCase =
        new TestMixedHiveOptimizing(table, amsEnv.getTestHMS().getClient());
    testCase.testHiveKeyedTableMajorOptimizeAndMove();
  }

  private ArcticTable createArcticTable(
      TableIdentifier tableIdentifier, PrimaryKeySpec primaryKeySpec,
      PartitionSpec partitionSpec) {

    TableBuilder tableBuilder =
        amsEnv.catalog(AmsEnvironment.MIXED_ICEBERG_CATALOG).newTableBuilder(tableIdentifier, SCHEMA)
            .withPrimaryKeySpec(primaryKeySpec)
            .withPartitionSpec(partitionSpec)
            .withProperty(TableProperties.SELF_OPTIMIZING_MINOR_TRIGGER_INTERVAL, "1000");

    return tableBuilder.create();
  }

  private void createHiveArcticTable(
      TableIdentifier tableIdentifier, PrimaryKeySpec primaryKeySpec,
      PartitionSpec partitionSpec) {
    TableBuilder tableBuilder =
        amsEnv.catalog(AmsEnvironment.MIXED_HIVE_CATALOG).newTableBuilder(tableIdentifier, SCHEMA)
            .withPrimaryKeySpec(primaryKeySpec)
            .withPartitionSpec(partitionSpec)
            .withProperty(TableProperties.SELF_OPTIMIZING_MINOR_TRIGGER_INTERVAL, "1000");

    tableBuilder.create();
  }

  private void assertTableExist(TableIdentifier tableIdentifier) {
    Assertions.assertTrue(amsEnv.tableExist(tableIdentifier));
  }
}
