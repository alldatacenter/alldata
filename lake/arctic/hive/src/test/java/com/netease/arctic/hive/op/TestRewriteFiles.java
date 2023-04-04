package com.netease.arctic.hive.op;

import com.netease.arctic.hive.HiveTableProperties;
import com.netease.arctic.hive.HiveTableTestBase;
import com.netease.arctic.hive.MockDataFileBuilder;
import com.netease.arctic.hive.exceptions.CannotAlterHiveLocationException;
import com.netease.arctic.hive.table.UnkeyedHiveTable;
import com.netease.arctic.op.OverwriteBaseFiles;
import com.netease.arctic.table.KeyedTable;
import com.netease.arctic.table.TableIdentifier;
import com.netease.arctic.table.UnkeyedTable;
import com.netease.arctic.utils.TableFileUtils;
import com.netease.arctic.utils.TablePropertyUtil;
import org.apache.hadoop.hive.metastore.api.Table;
import org.apache.iceberg.AppendFiles;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.OverwriteFiles;
import org.apache.iceberg.RewriteFiles;
import org.apache.iceberg.Transaction;
import org.apache.iceberg.UpdateProperties;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.apache.iceberg.relocated.com.google.common.collect.Maps;
import org.apache.iceberg.relocated.com.google.common.collect.Sets;
import org.apache.thrift.TException;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.netease.arctic.hive.op.UpdateHiveFiles.DELETE_UNTRACKED_HIVE_FILE;

public class TestRewriteFiles extends HiveTableTestBase {

  @Test
  public void testRewriteUnkeyedPartitionTable() throws TException {
    UnkeyedTable table = testHiveTable;
    Map<String, String> partitionAndLocations = Maps.newHashMap();

    // ================== init partitions files
    List<Map.Entry<String, String>> initFiles = Lists.newArrayList(
        Maps.immutableEntry("name=aaa", "/test_path/partition1/data-a1.parquet"),
        Maps.immutableEntry("name=aaa", "/test_path/partition1/data-a2.parquet"),
        Maps.immutableEntry("name=bbb", "/test_path/partition2/data-a2.parquet")
    );
    MockDataFileBuilder dataFileBuilder = new MockDataFileBuilder(table, hms.getClient());
    Set<DataFile> initDataFiles = new HashSet<>(dataFileBuilder.buildList(initFiles));

    OverwriteFiles overwriteFiles = table.newOverwrite();
    initDataFiles.forEach(overwriteFiles::addFile);
    overwriteFiles.commit();

    applyUpdateHiveFiles(partitionAndLocations, s -> false, initFiles);
    assertHivePartitionLocations(partitionAndLocations, table);

    // ================== test rewrite all partition
    List<Map.Entry<String, String>> newFiles = Lists.newArrayList(
        Maps.immutableEntry("name=aaa", "/test_path/partition3/data-a3.parquet"),
        Maps.immutableEntry("name=ccc", "/test_path/partition4/data-c.parquet")
    );
    Set<DataFile> newDataFiles = new HashSet<>(dataFileBuilder.buildList(newFiles));
    RewriteFiles rewriteFiles = table.newRewrite();
    rewriteFiles.set(DELETE_UNTRACKED_HIVE_FILE, "true");
    rewriteFiles.rewriteFiles(initDataFiles, newDataFiles);
    rewriteFiles.commit();

    partitionAndLocations.clear();
    applyUpdateHiveFiles(partitionAndLocations, s -> false, newFiles);
    assertHivePartitionLocations(partitionAndLocations, table);
  }

  @Test
  public void testRewriteKeyedPartitionTable() throws TException {
    KeyedTable table = testKeyedHiveTable;
    long txId = testKeyedHiveTable.beginTransaction(System.currentTimeMillis() + "");
    Map<String, String> partitionAndLocations = Maps.newHashMap();

    // ================== init partitions files
    List<Map.Entry<String, String>> initFiles = Lists.newArrayList(
        Maps.immutableEntry("name=aaa", "/test_path/partition1/data-a1.parquet"),
        Maps.immutableEntry("name=aaa", "/test_path/partition1/data-a2.parquet"),
        Maps.immutableEntry("name=bbb", "/test_path/partition2/data-a2.parquet")
    );
    MockDataFileBuilder dataFileBuilder = new MockDataFileBuilder(table, hms.getClient());
    Set<DataFile> initDataFiles = new HashSet<>(dataFileBuilder.buildList(initFiles));

    OverwriteBaseFiles overwriteBaseFiles = table.newOverwriteBaseFiles();
    initDataFiles.forEach(overwriteBaseFiles::addFile);
    overwriteBaseFiles.updateOptimizedSequenceDynamically(txId);
    overwriteBaseFiles.commit();

    applyUpdateHiveFiles(partitionAndLocations, s -> false, initFiles);
    assertHivePartitionLocations(partitionAndLocations, table);

    // ================== test rewrite all partition
    List<Map.Entry<String, String>> newFiles = Lists.newArrayList(
        Maps.immutableEntry("name=aaa", "/test_path/partition3/data-a3.parquet"),
        Maps.immutableEntry("name=ccc", "/test_path/partition4/data-c.parquet")
    );
    Set<DataFile> newDataFiles = new HashSet<>(dataFileBuilder.buildList(newFiles));

    RewriteFiles rewriteFiles = table.asKeyedTable().baseTable().newRewrite();
    rewriteFiles.rewriteFiles(initDataFiles, newDataFiles);
    rewriteFiles.commit();

    partitionAndLocations.clear();
    applyUpdateHiveFiles(partitionAndLocations, s -> false, newFiles);
    assertHivePartitionLocations(partitionAndLocations, table);
  }

  @Test
  public void testRewriteOperationTransaction() throws TException {
    UnkeyedTable table = testHiveTable;
    Map<String, String> partitionAndLocations = Maps.newHashMap();

    // ================== init partitions files
    List<Map.Entry<String, String>> initFiles = Lists.newArrayList(
        Maps.immutableEntry("name=aaa", "/test_path/partition1/data-a1.parquet"),
        Maps.immutableEntry("name=aaa", "/test_path/partition1/data-a2.parquet"),
        Maps.immutableEntry("name=bbb", "/test_path/partition2/data-a2.parquet")
    );
    MockDataFileBuilder dataFileBuilder = new MockDataFileBuilder(table, hms.getClient());
    Set<DataFile> initDataFiles = new HashSet<>(dataFileBuilder.buildList(initFiles));

    OverwriteFiles overwriteFiles = table.newOverwrite();
    initDataFiles.forEach(overwriteFiles::addFile);
    overwriteFiles.commit();

    applyUpdateHiveFiles(partitionAndLocations, s -> false, initFiles);
    assertHivePartitionLocations(partitionAndLocations, table);

    // ================== rewrite transaction
    Transaction tx = table.newTransaction();

    List<Map.Entry<String, String>> newFiles = Lists.newArrayList(
        Maps.immutableEntry("name=aaa", "/test_path/partition3/data-a3.parquet"),
        Maps.immutableEntry("name=ccc", "/test_path/partition4/data-c.parquet")
    );
    Set<DataFile> newDataFiles = new HashSet<>(dataFileBuilder.buildList(newFiles));

    RewriteFiles rewriteFiles = tx.newRewrite();
    rewriteFiles.rewriteFiles(initDataFiles, newDataFiles);
    rewriteFiles.commit();

    String key = "test-rewrite-transaction";
    UpdateProperties updateProperties = tx.updateProperties();
    updateProperties.set(key, "true");
    updateProperties.commit();

    table.refresh();
    Assert.assertFalse("table properties should not update", table.properties().containsKey(key));
    assertHivePartitionLocations(partitionAndLocations, table);

    tx.commitTransaction();
    table.refresh();

    Assert.assertTrue("table properties should update", table.properties().containsKey(key));
    Assert.assertEquals("true", table.properties().get(key));

    partitionAndLocations.clear();
    applyUpdateHiveFiles(partitionAndLocations, s -> false, newFiles);
    assertHivePartitionLocations(partitionAndLocations, table);
  }

  @Test
  public void testNonPartitionTable() throws TException {
    TableIdentifier identifier = TableIdentifier.of(HIVE_CATALOG_NAME, HIVE_DB_NAME, "test_un_partition");
    UnkeyedHiveTable table = (UnkeyedHiveTable) hiveCatalog.newTableBuilder(identifier, TABLE_SCHEMA)
        .create();

    Map<String, String> partitionAndLocations = Maps.newHashMap();

    // ================== init table files
    List<Map.Entry<String, String>> initFiles = Lists.newArrayList(
        Maps.immutableEntry(null, "/test_path/hive_data_location/data-a1.parquet"),
        Maps.immutableEntry(null, "/test_path/hive_data_location/data-a2.parquet"),
        Maps.immutableEntry(null, "/test_path/hive_data_location/data-a2.parquet")
    );
    MockDataFileBuilder dataFileBuilder = new MockDataFileBuilder(table, hms.getClient());
    Set<DataFile> initDataFiles = new HashSet<>(dataFileBuilder.buildList(initFiles));

    OverwriteFiles overwriteFiles = table.newOverwrite();
    initDataFiles.forEach(overwriteFiles::addFile);
    overwriteFiles.commit();

    // assert no hive partition
    assertHivePartitionLocations(partitionAndLocations, table);
    Table hiveTable = hms.getClient().getTable(table.id().getDatabase(), table.name());
    Assert.assertTrue(
        "table location to new path",
        hiveTable.getSd().getLocation().endsWith("/test_path/hive_data_location"));
    Assert.assertEquals("table partition properties hive location is error",
        hiveTable.getSd().getLocation(),
        table.partitionProperty().get(TablePropertyUtil.EMPTY_STRUCT)
            .get(HiveTableProperties.PARTITION_PROPERTIES_KEY_HIVE_LOCATION));
    Assert.assertEquals("table partition properties transient_lastDdlTime is error",
        hiveTable.getParameters().get("transient_lastDdlTime"),
        table.partitionProperty().get(TablePropertyUtil.EMPTY_STRUCT)
            .get(HiveTableProperties.PARTITION_PROPERTIES_KEY_TRANSIENT_TIME));

    // ================== test rewrite table
    List<Map.Entry<String, String>> newFiles = Lists.newArrayList(
        Maps.immutableEntry(null, "/test_path/hive_data_location_new/data-a3.parquet"),
        Maps.immutableEntry(null, "/test_path/hive_data_location_new/data-c.parquet")
    );
    Set<DataFile> newDataFiles = new HashSet<>(dataFileBuilder.buildList(newFiles));

    RewriteFiles rewriteFiles = table.asUnkeyedTable().newRewrite();
    rewriteFiles.rewriteFiles(initDataFiles, newDataFiles);
    rewriteFiles.commit();

    assertHivePartitionLocations(partitionAndLocations, table);
    hiveTable = hms.getClient().getTable(table.id().getDatabase(), table.name());
    Assert.assertFalse(
        "table location to new path",
        hiveTable.getSd().getLocation().endsWith("/test_path/hive_data_location"));

    String hiveLocation = hiveTable.getSd().getLocation();
    Assert.assertTrue(
        "table location change to hive location",
        hiveLocation.startsWith(table.hiveLocation()));
    Assert.assertEquals("table partition property hive location is error",
        hiveTable.getSd().getLocation(),
        table.partitionProperty().get(TablePropertyUtil.EMPTY_STRUCT)
            .get(HiveTableProperties.PARTITION_PROPERTIES_KEY_HIVE_LOCATION));
    Assert.assertEquals("table partition property transient_lastDdlTime is error",
        hiveTable.getParameters().get("transient_lastDdlTime"),
        table.partitionProperty().get(TablePropertyUtil.EMPTY_STRUCT)
            .get(HiveTableProperties.PARTITION_PROPERTIES_KEY_TRANSIENT_TIME));

    hiveCatalog.dropTable(identifier, true);
    AMS.handler().getTableCommitMetas().remove(identifier.buildTableIdentifier());
  }

  /**
   * failed when not delete all files in partition
   */
  @Test
  public void testExceptionNotDeleteAllPartitionFiles() throws TException {
    UnkeyedTable table = testHiveTable;
    Map<String, String> partitionAndLocations = Maps.newHashMap();

    // ================== init partitions files
    List<Map.Entry<String, String>> initFiles = Lists.newArrayList(
        Maps.immutableEntry("name=aaa", "/test_path/partition1/data-a1.parquet"),
        Maps.immutableEntry("name=aaa", "/test_path/partition1/data-a2.parquet"),
        Maps.immutableEntry("name=bbb", "/test_path/partition2/data-a2.parquet")
    );
    MockDataFileBuilder dataFileBuilder = new MockDataFileBuilder(table, hms.getClient());
    Set<DataFile> initDataFiles = new HashSet<>(dataFileBuilder.buildList(initFiles));

    OverwriteFiles overwriteFiles = table.newOverwrite();
    initDataFiles.forEach(overwriteFiles::addFile);
    overwriteFiles.commit();

    applyUpdateHiveFiles(partitionAndLocations, s -> false, initFiles);
    assertHivePartitionLocations(partitionAndLocations, table);

    List<Map.Entry<String, String>> newFiles = Lists.newArrayList(
        Maps.immutableEntry("name=aaa", "/test_path/partition3/data-a3.parquet"),
        Maps.immutableEntry("name=ccc", "/test_path/partition4/data-c.parquet")
    );
    Set<DataFile> newDataFiles = new HashSet<>(dataFileBuilder.buildList(newFiles));
    RewriteFiles rewriteFiles = table.newRewrite();
    // delete one file under partition[name=aaa]
    rewriteFiles.rewriteFiles(Sets.newHashSet(initDataFiles.iterator().next()), newDataFiles);

    Assert.assertThrows(CannotAlterHiveLocationException.class, rewriteFiles::commit);
  }

  /**
   * failed when add two file in different location but with same partition key
   */
  @Test
  public void testExceptionAddFileInDifferentLocation() throws TException {
    UnkeyedTable table = testHiveTable;
    Map<String, String> partitionAndLocations = Maps.newHashMap();

    // ================== init partitions files
    List<Map.Entry<String, String>> initFiles = Lists.newArrayList(
        Maps.immutableEntry("name=aaa", "/test_path/partition1/data-a1.parquet"),
        Maps.immutableEntry("name=aaa", "/test_path/partition1/data-a2.parquet"),
        Maps.immutableEntry("name=bbb", "/test_path/partition2/data-a2.parquet")
    );
    MockDataFileBuilder dataFileBuilder = new MockDataFileBuilder(table, hms.getClient());
    Set<DataFile> initDataFiles = new HashSet<>(dataFileBuilder.buildList(initFiles));

    OverwriteFiles overwriteFiles = table.newOverwrite();
    initDataFiles.forEach(overwriteFiles::addFile);
    overwriteFiles.commit();

    applyUpdateHiveFiles(partitionAndLocations, s -> false, initFiles);
    assertHivePartitionLocations(partitionAndLocations, table);

    List<Map.Entry<String, String>> newFiles = Lists.newArrayList(
        Maps.immutableEntry("name=aaa", "/test_path/partition3/data-a3.parquet"),
        Maps.immutableEntry("name=aaa", "/test_path/partition4/data-c.parquet")
    );
    Set<DataFile> newDataFiles = new HashSet<>(dataFileBuilder.buildList(newFiles));
    RewriteFiles rewriteFiles = table.newRewrite();
    rewriteFiles.rewriteFiles(initDataFiles, newDataFiles);

    Assert.assertThrows(CannotAlterHiveLocationException.class, rewriteFiles::commit);
  }

  /**
   * failed when add partition exist with different location
   */
  @Test
  public void testExceptionAddFileWithExistPartition() throws TException {
    UnkeyedTable table = testHiveTable;
    Map<String, String> partitionAndLocations = Maps.newHashMap();

    // ================== init partitions files
    List<Map.Entry<String, String>> initFiles = Lists.newArrayList(
        Maps.immutableEntry("name=aaa", "/test_path/partition1/data-a1.parquet"),
        Maps.immutableEntry("name=aaa", "/test_path/partition1/data-a2.parquet"),
        Maps.immutableEntry("name=bbb", "/test_path/partition2/data-a2.parquet")
    );
    MockDataFileBuilder dataFileBuilder = new MockDataFileBuilder(table, hms.getClient());
    Set<DataFile> initDataFiles = new HashSet<>(dataFileBuilder.buildList(initFiles));

    OverwriteFiles overwriteFiles = table.newOverwrite();
    initDataFiles.forEach(overwriteFiles::addFile);
    overwriteFiles.commit();

    applyUpdateHiveFiles(partitionAndLocations, s -> false, initFiles);
    assertHivePartitionLocations(partitionAndLocations, table);

    List<Map.Entry<String, String>> newFiles = Lists.newArrayList(
        Maps.immutableEntry("name=aaa", "/test_path/partition3/data-a3.parquet"),
        Maps.immutableEntry("name=bbb", "/test_path/partition4/data-c.parquet")
    );
    Set<DataFile> newDataFiles = new HashSet<>(dataFileBuilder.buildList(newFiles));
    RewriteFiles rewriteFiles = table.newRewrite();
    // delete one file under partition[name=aaa]
    rewriteFiles.rewriteFiles(initDataFiles.stream()
        .filter(dataFile -> dataFile.path().toString().contains("partition1")).collect(Collectors.toSet()), newDataFiles);

    Assert.assertThrows(CannotAlterHiveLocationException.class, rewriteFiles::commit);
  }

  /**
   * failed when delete and add partition with same location
   */
  @Test
  public void testExceptionDeleteCreateSamePartition() throws TException {
    UnkeyedTable table = testHiveTable;
    Map<String, String> partitionAndLocations = Maps.newHashMap();

    // ================== init partitions files
    List<Map.Entry<String, String>> initFiles = Lists.newArrayList(
        Maps.immutableEntry("name=aaa", "/test_path/partition1/data-a1.parquet"),
        Maps.immutableEntry("name=aaa", "/test_path/partition1/data-a2.parquet"),
        Maps.immutableEntry("name=bbb", "/test_path/partition2/data-a2.parquet")
    );
    MockDataFileBuilder dataFileBuilder = new MockDataFileBuilder(table, hms.getClient());
    Set<DataFile> initDataFiles = new HashSet<>(dataFileBuilder.buildList(initFiles));

    OverwriteFiles overwriteFiles = table.newOverwrite();
    initDataFiles.forEach(overwriteFiles::addFile);
    overwriteFiles.commit();

    applyUpdateHiveFiles(partitionAndLocations, s -> false, initFiles);
    assertHivePartitionLocations(partitionAndLocations, table);

    List<Map.Entry<String, String>> newFiles = Lists.newArrayList(
        Maps.immutableEntry("name=aaa", "/test_path/partition1/data-a3.parquet"),
        Maps.immutableEntry("name=ccc", "/test_path/partition4/data-c.parquet")
    );
    Set<DataFile> newDataFiles = new HashSet<>(dataFileBuilder.buildList(newFiles));
    RewriteFiles rewriteFiles = table.newRewrite();
    rewriteFiles.rewriteFiles(initDataFiles, newDataFiles);

    Assert.assertThrows(CannotAlterHiveLocationException.class, rewriteFiles::commit);
  }

  private void applyUpdateHiveFiles(
      Map<String, String> partitionAndLocations,
      Predicate<String> deleteFunc,
      List<Map.Entry<String, String>> addFiles) {
    Set<String> deleteLocations = partitionAndLocations.keySet()
        .stream().filter(deleteFunc).collect(Collectors.toSet());

    deleteLocations.forEach(partitionAndLocations::remove);

    addFiles.forEach(kv -> {
      String partLocation = TableFileUtils.getFileDir(kv.getValue());
      partitionAndLocations.put(
          kv.getKey(),
          partLocation
      );
    });
  }
}
