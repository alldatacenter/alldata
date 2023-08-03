package com.netease.arctic.hive.op;

import com.netease.arctic.TableTestHelper;
import com.netease.arctic.ams.api.TableFormat;
import com.netease.arctic.catalog.CatalogTestHelper;
import com.netease.arctic.catalog.TableTestBase;
import com.netease.arctic.hive.TestHMS;
import com.netease.arctic.hive.catalog.HiveCatalogTestHelper;
import com.netease.arctic.hive.catalog.HiveTableTestHelper;
import com.netease.arctic.hive.exceptions.CannotAlterHiveLocationException;
import com.netease.arctic.hive.io.HiveDataTestHelpers;
import com.netease.arctic.table.UnkeyedTable;
import com.netease.arctic.utils.ArcticTableUtil;
import org.apache.hadoop.hive.metastore.api.Table;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.OverwriteFiles;
import org.apache.iceberg.TableProperties;
import org.apache.iceberg.Transaction;
import org.apache.iceberg.UpdateProperties;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.expressions.Expressions;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.apache.thrift.TException;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.List;

import static com.netease.arctic.hive.op.UpdateHiveFiles.DELETE_UNTRACKED_HIVE_FILE;

@RunWith(Parameterized.class)
public class TestOverwriteFiles extends TableTestBase {

  @ClassRule
  public static TestHMS TEST_HMS = new TestHMS();

  public TestOverwriteFiles(CatalogTestHelper catalogTestHelper, TableTestHelper tableTestHelper) {
    super(catalogTestHelper, tableTestHelper);
  }

  @Parameterized.Parameters(name = "{0}, {1}")
  public static Object[] parameters() {
    return new Object[][] {{new HiveCatalogTestHelper(TableFormat.MIXED_HIVE, TEST_HMS.getHiveConf()),
                            new HiveTableTestHelper(true, true)},
                           {new HiveCatalogTestHelper(TableFormat.MIXED_HIVE, TEST_HMS.getHiveConf()),
                            new HiveTableTestHelper(true, false)},
                           {new HiveCatalogTestHelper(TableFormat.MIXED_HIVE, TEST_HMS.getHiveConf()),
                            new HiveTableTestHelper(false, true)},
                           {new HiveCatalogTestHelper(TableFormat.MIXED_HIVE, TEST_HMS.getHiveConf()),
                            new HiveTableTestHelper(false, false)}};
  }

  @Test
  public void testOverwriteWholeHiveTable() throws TException {
    List<Record> insertRecords = Lists.newArrayList();
    insertRecords.add(tableTestHelper().generateTestRecord(1, "john", 0, "2022-01-01T12:00:00"));
    insertRecords.add(tableTestHelper().generateTestRecord(2, "lily", 0, "2022-01-02T12:00:00"));
    List<DataFile> dataFiles = HiveDataTestHelpers.writeBaseStore(getArcticTable(), 1L, insertRecords, false, true);
    UnkeyedTable baseStore = ArcticTableUtil.baseStore(getArcticTable());
    OverwriteFiles overwriteFiles = baseStore.newOverwrite();
    dataFiles.forEach(overwriteFiles::addFile);
    overwriteFiles.commit();

    UpdateHiveFilesTestHelpers.validateHiveTableValues(TEST_HMS.getHiveClient(), getArcticTable(), dataFiles);

    // ================== test overwrite all table
    insertRecords.clear();
    insertRecords.add(tableTestHelper().generateTestRecord(2, "lily", 0, "2022-01-02T12:00:00"));
    insertRecords.add(tableTestHelper().generateTestRecord(3, "john", 0, "2022-01-03T12:00:00"));
    dataFiles = HiveDataTestHelpers.writeBaseStore(getArcticTable(), 1L, insertRecords, false, true);
    overwriteFiles = baseStore.newOverwrite();
    overwriteFiles.overwriteByRowFilter(Expressions.alwaysTrue());
    dataFiles.forEach(overwriteFiles::addFile);
    overwriteFiles.commit();

    UpdateHiveFilesTestHelpers.validateHiveTableValues(TEST_HMS.getHiveClient(), getArcticTable(), dataFiles);
  }

  @Test
  public void testOverwriteInTransaction() throws TException {
    List<Record> insertRecords = Lists.newArrayList();
    insertRecords.add(tableTestHelper().generateTestRecord(1, "john", 0, "2022-01-01T12:00:00"));
    insertRecords.add(tableTestHelper().generateTestRecord(2, "lily", 0, "2022-01-02T12:00:00"));
    List<DataFile> dataFiles = HiveDataTestHelpers.writeBaseStore(getArcticTable(), 1L, insertRecords, false, true);
    UnkeyedTable baseStore = ArcticTableUtil.baseStore(getArcticTable());
    Transaction transaction = baseStore.newTransaction();
    OverwriteFiles overwriteFiles = transaction.newOverwrite();
    dataFiles.forEach(overwriteFiles::addFile);
    overwriteFiles.commit();

    String key = "test-overwrite-transaction";
    UpdateProperties updateProperties = transaction.updateProperties();
    updateProperties.set(key, "true");
    updateProperties.commit();

    Assert.assertFalse(getArcticTable().properties().containsKey(key));
    UpdateHiveFilesTestHelpers.validateHiveTableValues(
        TEST_HMS.getHiveClient(),
        getArcticTable(),
        Lists.newArrayList());

    transaction.commitTransaction();
    Assert.assertTrue(getArcticTable().properties().containsKey(key));
    UpdateHiveFilesTestHelpers.validateHiveTableValues(TEST_HMS.getHiveClient(), getArcticTable(), dataFiles);
  }

  @Test
  public void testOverwriteByRowFilter() throws TException {
    List<Record> insertRecords = Lists.newArrayList();
    insertRecords.add(tableTestHelper().generateTestRecord(1, "john", 0, "2022-01-01T12:00:00"));
    insertRecords.add(tableTestHelper().generateTestRecord(2, "lily", 0, "2022-01-02T12:00:00"));
    List<DataFile> dataFiles = HiveDataTestHelpers.writeBaseStore(getArcticTable(), 1L, insertRecords, false, true);
    UnkeyedTable baseStore = ArcticTableUtil.baseStore(getArcticTable());
    OverwriteFiles overwriteFiles = baseStore.newOverwrite();
    dataFiles.forEach(overwriteFiles::addFile);
    overwriteFiles.commit();

    UpdateHiveFilesTestHelpers.validateHiveTableValues(TEST_HMS.getHiveClient(), getArcticTable(), dataFiles);

    // ================== test overwrite all table
    insertRecords.clear();
    insertRecords.add(tableTestHelper().generateTestRecord(2, "lily", 0, "2022-01-02T12:00:00"));
    insertRecords.add(tableTestHelper().generateTestRecord(3, "john", 0, "2022-01-03T12:00:00"));
    dataFiles = HiveDataTestHelpers.writeBaseStore(getArcticTable(), 1L, insertRecords, false, true);
    overwriteFiles = baseStore.newOverwrite();
    overwriteFiles.overwriteByRowFilter(Expressions.alwaysTrue());
    dataFiles.forEach(overwriteFiles::addFile);
    overwriteFiles.commit();

    UpdateHiveFilesTestHelpers.validateHiveTableValues(TEST_HMS.getHiveClient(), getArcticTable(), dataFiles);
  }

  /**
   * add file to exist partition, overwrite success without create partition
   */
  @Test
  public void testOverwriteByAddFiles() throws TException {
    String hiveLocation = "test_hive_location";
    List<Record> insertRecords = Lists.newArrayList();
    insertRecords.add(tableTestHelper().generateTestRecord(1, "john", 0, "2022-01-01T12:00:00"));
    insertRecords.add(tableTestHelper().generateTestRecord(2, "lily", 0, "2022-01-01T12:00:00"));
    List<DataFile> dataFiles =
        HiveDataTestHelpers.writeBaseStore(getArcticTable(), 1L, insertRecords, false, true, hiveLocation);
    UnkeyedTable baseStore = ArcticTableUtil.baseStore(getArcticTable());
    OverwriteFiles overwriteFiles = baseStore.newOverwrite();
    dataFiles.forEach(overwriteFiles::addFile);
    overwriteFiles.commit();

    UpdateHiveFilesTestHelpers.validateHiveTableValues(TEST_HMS.getHiveClient(), getArcticTable(), dataFiles);

    insertRecords.clear();
    insertRecords.add(tableTestHelper().generateTestRecord(2, "lily", 0, "2022-01-01T12:00:00"));
    insertRecords.add(tableTestHelper().generateTestRecord(3, "john", 0, "2022-01-01T12:00:00"));
    List<DataFile> newFiles = HiveDataTestHelpers.writeBaseStore(getArcticTable(), 2L, insertRecords, false, true,
        hiveLocation);
    overwriteFiles = baseStore.newOverwrite();
    newFiles.forEach(overwriteFiles::addFile);
    overwriteFiles.commit();

    List<DataFile> expectFiles = Lists.newArrayList(dataFiles);
    expectFiles.addAll(newFiles);
    UpdateHiveFilesTestHelpers.validateHiveTableValues(TEST_HMS.getHiveClient(), getArcticTable(), expectFiles);
  }

  @Test
  public void testOverwriteCommitHMSFailed() throws TException {
    List<Record> insertRecords = Lists.newArrayList();
    insertRecords.add(tableTestHelper().generateTestRecord(1, "john", 0, "2022-01-01T12:00:00"));
    insertRecords.add(tableTestHelper().generateTestRecord(2, "lily", 0, "2022-01-02T12:00:00"));
    List<DataFile> dataFiles = HiveDataTestHelpers.writeBaseStore(getArcticTable(), 1L, insertRecords, false, true);
    UnkeyedTable baseStore = ArcticTableUtil.baseStore(getArcticTable());
    OverwriteFiles overwriteFiles = baseStore.newOverwrite();
    dataFiles.forEach(overwriteFiles::addFile);

    // rename the hive table,
    Table hiveTable =
        TEST_HMS.getHiveClient().getTable(getArcticTable().id().getDatabase(), getArcticTable().id().getTableName());
    hiveTable.setTableName("new_table");
    TEST_HMS.getHiveClient()
        .alter_table(getArcticTable().id().getDatabase(), getArcticTable().id().getTableName(), hiveTable);

    // commit should success even though hive table is not existed.
    overwriteFiles.commit();

    hiveTable.setTableName(getArcticTable().id().getTableName());
    TEST_HMS.getHiveClient().alter_table(getArcticTable().id().getDatabase(), "new_table", hiveTable);
    String tableRootLocation = ArcticTableUtil.tableRootLocation(getArcticTable());
    String newTableLocation = tableRootLocation.replace(getArcticTable().id().getTableName(), "new_table");
    getArcticTable().io().asFileSystemIO().deletePrefix(newTableLocation);
  }

  @Test
  public void testOverwriteCleanUntrackedFiles() throws TException {
    String hiveLocation = "test_hive_location";
    List<Record> insertRecords = Lists.newArrayList();
    insertRecords.add(tableTestHelper().generateTestRecord(1, "john", 0, "2022-01-01T12:00:00"));
    insertRecords.add(tableTestHelper().generateTestRecord(2, "lily", 0, "2022-01-02T12:00:00"));
    HiveDataTestHelpers.writeBaseStore(getArcticTable(), 1L, insertRecords, false, true, hiveLocation);
    // rewrite data files
    List<DataFile> rewriteDataFiles = HiveDataTestHelpers.writeBaseStore(getArcticTable(), 2L, insertRecords, false,
        true, hiveLocation);
    UnkeyedTable baseStore = ArcticTableUtil.baseStore(getArcticTable());
    OverwriteFiles overwriteFiles = baseStore.newOverwrite();
    rewriteDataFiles.forEach(overwriteFiles::addFile);
    overwriteFiles.set(DELETE_UNTRACKED_HIVE_FILE, "true");
    overwriteFiles.commit();

    UpdateHiveFilesTestHelpers.validateHiveTableValues(TEST_HMS.getHiveClient(), getArcticTable(), rewriteDataFiles);
  }

  @Test
  public void testOverwritePartFiles() throws TException {
    //TODO should add cases for tables without partition spec
    Assume.assumeTrue(isPartitionedTable());
    getArcticTable().updateProperties().set(TableProperties.WRITE_TARGET_FILE_SIZE_BYTES, "1").commit();
    List<Record> insertRecords = Lists.newArrayList();
    insertRecords.add(tableTestHelper().generateTestRecord(1, "john", 0, "2022-01-01T12:00:00"));
    insertRecords.add(tableTestHelper().generateTestRecord(2, "lily", 0, "2022-01-01T12:00:00"));
    List<DataFile> dataFiles = HiveDataTestHelpers.writeBaseStore(getArcticTable(), 1L, insertRecords, false, true);
    Assert.assertEquals(2, dataFiles.size());
    DataFile deleteFile = dataFiles.get(0);
    UnkeyedTable baseStore = ArcticTableUtil.baseStore(getArcticTable());
    OverwriteFiles overwriteFiles = baseStore.newOverwrite();
    dataFiles.forEach(overwriteFiles::addFile);
    overwriteFiles.commit();

    UpdateHiveFilesTestHelpers.validateHiveTableValues(TEST_HMS.getHiveClient(), getArcticTable(), dataFiles);

    // ================== test overwrite part files
    insertRecords.clear();
    insertRecords.add(tableTestHelper().generateTestRecord(2, "lily", 0, "2022-01-01T12:00:00"));
    insertRecords.add(tableTestHelper().generateTestRecord(3, "john", 0, "2022-01-01T12:00:00"));
    dataFiles = HiveDataTestHelpers.writeBaseStore(getArcticTable(), 1L, insertRecords, false, true);
    overwriteFiles = baseStore.newOverwrite();
    dataFiles.forEach(overwriteFiles::addFile);
    overwriteFiles.deleteFile(deleteFile);
    Assert.assertThrows(CannotAlterHiveLocationException.class, overwriteFiles::commit);
  }

  @Test
  public void testOverwriteWithFilesUnderDifferentDir() {
    //TODO should add cases for tables without partition spec
    Assume.assumeTrue(isPartitionedTable());
    UnkeyedTable baseStore = ArcticTableUtil.baseStore(getArcticTable());
    OverwriteFiles overwriteFiles = baseStore.newOverwrite();

    List<Record> insertRecords = Lists.newArrayList();
    insertRecords.add(tableTestHelper().generateTestRecord(1, "john", 0, "2022-01-01T12:00:00"));
    insertRecords.add(tableTestHelper().generateTestRecord(2, "lily", 0, "2022-01-02T12:00:00"));
    List<DataFile> dataFiles = HiveDataTestHelpers.writeBaseStore(getArcticTable(), 1L, insertRecords, false, true);
    dataFiles.forEach(overwriteFiles::addFile);

    // write data files under another dir
    dataFiles = HiveDataTestHelpers.writeBaseStore(getArcticTable(), 1L, insertRecords, false, true);
    dataFiles.forEach(overwriteFiles::addFile);

    Assert.assertThrows(CannotAlterHiveLocationException.class, overwriteFiles::commit);
  }

  @Test
  public void testOverwriteByAddFilesInDifferentDir() throws TException {
    //TODO should add cases for tables without partition spec
    Assume.assumeTrue(isPartitionedTable());
    List<Record> insertRecords = Lists.newArrayList();
    insertRecords.add(tableTestHelper().generateTestRecord(1, "john", 0, "2022-01-01T12:00:00"));
    insertRecords.add(tableTestHelper().generateTestRecord(2, "lily", 0, "2022-01-02T12:00:00"));
    List<DataFile> dataFiles = HiveDataTestHelpers.writeBaseStore(getArcticTable(), 1L, insertRecords, false, true);
    UnkeyedTable baseStore = ArcticTableUtil.baseStore(getArcticTable());
    OverwriteFiles overwriteFiles = baseStore.newOverwrite();
    dataFiles.forEach(overwriteFiles::addFile);
    overwriteFiles.commit();

    UpdateHiveFilesTestHelpers.validateHiveTableValues(TEST_HMS.getHiveClient(), getArcticTable(), dataFiles);

    // ================== test add files only
    insertRecords.clear();
    insertRecords.add(tableTestHelper().generateTestRecord(2, "lily", 0, "2022-01-02T12:00:00"));
    insertRecords.add(tableTestHelper().generateTestRecord(3, "john", 0, "2022-01-03T12:00:00"));
    dataFiles = HiveDataTestHelpers.writeBaseStore(getArcticTable(), 1L, insertRecords, false, true);
    overwriteFiles = baseStore.newOverwrite();
    dataFiles.forEach(overwriteFiles::addFile);

    Assert.assertThrows(CannotAlterHiveLocationException.class, overwriteFiles::commit);
  }

  @Test
  public void testOverwriteWithSameLocation() throws TException {
    //TODO should add cases for tables without partition spec
    Assume.assumeTrue(isPartitionedTable());
    List<Record> insertRecords = Lists.newArrayList();
    insertRecords.add(tableTestHelper().generateTestRecord(1, "john", 0, "2022-01-01T12:00:00"));
    insertRecords.add(tableTestHelper().generateTestRecord(2, "lily", 0, "2022-01-02T12:00:00"));
    List<DataFile> dataFiles = HiveDataTestHelpers.writeBaseStore(getArcticTable(), 1L, insertRecords, false, true);
    UnkeyedTable baseStore = ArcticTableUtil.baseStore(getArcticTable());
    OverwriteFiles overwriteFiles = baseStore.newOverwrite();
    dataFiles.forEach(overwriteFiles::addFile);
    overwriteFiles.commit();

    UpdateHiveFilesTestHelpers.validateHiveTableValues(TEST_HMS.getHiveClient(), getArcticTable(), dataFiles);

    overwriteFiles = baseStore.newOverwrite();
    dataFiles.forEach(overwriteFiles::deleteFile);
    dataFiles.forEach(overwriteFiles::addFile);

    Assert.assertThrows(CannotAlterHiveLocationException.class, overwriteFiles::commit);
  }
}
