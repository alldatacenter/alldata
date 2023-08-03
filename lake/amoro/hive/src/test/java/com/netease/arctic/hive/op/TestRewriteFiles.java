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
import org.apache.iceberg.RewriteFiles;
import org.apache.iceberg.TableProperties;
import org.apache.iceberg.Transaction;
import org.apache.iceberg.UpdateProperties;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.apache.iceberg.relocated.com.google.common.collect.Sets;
import org.apache.thrift.TException;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.List;
import java.util.Set;

import static com.netease.arctic.hive.op.UpdateHiveFiles.DELETE_UNTRACKED_HIVE_FILE;

@RunWith(Parameterized.class)
public class TestRewriteFiles extends TableTestBase {

  @ClassRule
  public static TestHMS TEST_HMS = new TestHMS();

  private List<DataFile> initDataFiles;

  public TestRewriteFiles(CatalogTestHelper catalogTestHelper, TableTestHelper tableTestHelper) {
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

  private void initDataFiles() {
    List<Record> insertRecords = Lists.newArrayList();
    insertRecords.add(tableTestHelper().generateTestRecord(1, "john", 0, "2022-01-01T12:00:00"));
    insertRecords.add(tableTestHelper().generateTestRecord(2, "lily", 0, "2022-01-02T12:00:00"));
    initDataFiles = HiveDataTestHelpers.writeBaseStore(getArcticTable(), 1L, insertRecords, false, true);
    UnkeyedTable baseStore = ArcticTableUtil.baseStore(getArcticTable());
    OverwriteFiles overwriteFiles = baseStore.newOverwrite();
    initDataFiles.forEach(overwriteFiles::addFile);
    overwriteFiles.commit();
  }

  @Test
  public void testRewriteWholeHiveTable() throws TException {
    initDataFiles();
    List<Record> insertRecords = Lists.newArrayList();
    insertRecords.add(tableTestHelper().generateTestRecord(2, "lily", 0, "2022-01-02T12:00:00"));
    insertRecords.add(tableTestHelper().generateTestRecord(3, "john", 0, "2022-01-03T12:00:00"));
    List<DataFile> newFiles = HiveDataTestHelpers.writeBaseStore(getArcticTable(), 1L, insertRecords, false, true);
    UnkeyedTable baseStore = ArcticTableUtil.baseStore(getArcticTable());
    RewriteFiles rewriteFiles = baseStore.newRewrite();
    rewriteFiles.rewriteFiles(Sets.newHashSet(initDataFiles), Sets.newHashSet(newFiles));
    rewriteFiles.commit();

    UpdateHiveFilesTestHelpers.validateHiveTableValues(TEST_HMS.getHiveClient(), getArcticTable(), newFiles);
  }

  @Test
  public void testRewriteInTransaction() throws TException {
    initDataFiles();
    List<Record> insertRecords = Lists.newArrayList();
    insertRecords.add(tableTestHelper().generateTestRecord(2, "lily", 0, "2022-01-02T12:00:00"));
    insertRecords.add(tableTestHelper().generateTestRecord(3, "john", 0, "2022-01-03T12:00:00"));
    List<DataFile> newFiles = HiveDataTestHelpers.writeBaseStore(getArcticTable(), 1L, insertRecords, false, true);

    UnkeyedTable baseStore = ArcticTableUtil.baseStore(getArcticTable());
    Transaction transaction = baseStore.newTransaction();
    RewriteFiles rewriteFiles = transaction.newRewrite();
    rewriteFiles.rewriteFiles(Sets.newHashSet(initDataFiles), Sets.newHashSet(newFiles));
    rewriteFiles.commit();

    String key = "test-overwrite-transaction";
    UpdateProperties updateProperties = transaction.updateProperties();
    updateProperties.set(key, "true");
    updateProperties.commit();

    Assert.assertFalse(getArcticTable().properties().containsKey(key));
    UpdateHiveFilesTestHelpers.validateHiveTableValues(TEST_HMS.getHiveClient(), getArcticTable(), initDataFiles);

    transaction.commitTransaction();
    Assert.assertTrue(getArcticTable().properties().containsKey(key));
    UpdateHiveFilesTestHelpers.validateHiveTableValues(TEST_HMS.getHiveClient(), getArcticTable(), newFiles);
  }

  @Test
  public void testRewriteCommitHMSFailed() throws TException {
    initDataFiles();
    List<Record> insertRecords = Lists.newArrayList();
    insertRecords.clear();
    insertRecords.add(tableTestHelper().generateTestRecord(2, "lily", 0, "2022-01-02T12:00:00"));
    insertRecords.add(tableTestHelper().generateTestRecord(3, "john", 0, "2022-01-03T12:00:00"));
    List<DataFile> newFiles = HiveDataTestHelpers.writeBaseStore(getArcticTable(), 1L, insertRecords, false, true);
    UnkeyedTable baseStore = ArcticTableUtil.baseStore(getArcticTable());
    RewriteFiles rewriteFiles = baseStore.newRewrite();
    rewriteFiles.rewriteFiles(Sets.newHashSet(initDataFiles), Sets.newHashSet(newFiles));

    // rename the hive table,
    Table hiveTable =
        TEST_HMS.getHiveClient().getTable(getArcticTable().id().getDatabase(), getArcticTable().id().getTableName());
    hiveTable.setTableName("new_table");
    TEST_HMS.getHiveClient()
        .alter_table(getArcticTable().id().getDatabase(), getArcticTable().id().getTableName(), hiveTable);

    rewriteFiles.commit();

    hiveTable.setTableName(getArcticTable().id().getTableName());
    TEST_HMS.getHiveClient().alter_table(getArcticTable().id().getDatabase(), "new_table", hiveTable);
    String tableRootLocation = ArcticTableUtil.tableRootLocation(getArcticTable());
    String newTableLocation = tableRootLocation.replace(getArcticTable().id().getTableName(), "new_table");
    getArcticTable().io().asFileSystemIO().deletePrefix(newTableLocation);
  }

  @Test
  public void testRewriteCleanUntrackedFiles() throws TException {
    initDataFiles();
    List<Record> insertRecords = Lists.newArrayList();
    insertRecords.add(tableTestHelper().generateTestRecord(1, "john", 0, "2022-01-01T12:00:00"));
    insertRecords.add(tableTestHelper().generateTestRecord(2, "lily", 0, "2022-01-02T12:00:00"));

    String hiveLocation = "test_hive_location";
    HiveDataTestHelpers.writeBaseStore(getArcticTable(), 1L, insertRecords, false, true, hiveLocation);
    // rewrite data files
    List<DataFile> rewriteDataFiles = HiveDataTestHelpers.writeBaseStore(getArcticTable(), 2L, insertRecords, false,
        true, hiveLocation);
    UnkeyedTable baseStore = ArcticTableUtil.baseStore(getArcticTable());
    RewriteFiles rewriteFiles = baseStore.newRewrite();
    rewriteFiles.rewriteFiles(Sets.newHashSet(initDataFiles), Sets.newHashSet(rewriteDataFiles));
    rewriteFiles.set(DELETE_UNTRACKED_HIVE_FILE, "true");
    rewriteFiles.commit();

    UpdateHiveFilesTestHelpers.validateHiveTableValues(TEST_HMS.getHiveClient(), getArcticTable(), rewriteDataFiles);
  }

  @Test
  public void testRewritePartFiles() {
    //TODO should add cases for tables without partition spec
    Assume.assumeTrue(isPartitionedTable());
    getArcticTable().updateProperties().set(TableProperties.WRITE_TARGET_FILE_SIZE_BYTES, "1").commit();
    List<Record> insertRecords = Lists.newArrayList();
    insertRecords.add(tableTestHelper().generateTestRecord(1, "john", 0, "2022-01-01T12:00:00"));
    insertRecords.add(tableTestHelper().generateTestRecord(2, "lily", 0, "2022-01-01T12:00:00"));
    initDataFiles = HiveDataTestHelpers.writeBaseStore(getArcticTable(), 1L, insertRecords, false, true);
    UnkeyedTable baseStore = ArcticTableUtil.baseStore(getArcticTable());
    OverwriteFiles overwriteFiles = baseStore.newOverwrite();
    initDataFiles.forEach(overwriteFiles::addFile);
    overwriteFiles.commit();

    Assert.assertEquals(2, initDataFiles.size());
    DataFile deleteFile = initDataFiles.get(0);

    // ================== test rewrite part files
    insertRecords.clear();
    insertRecords.add(tableTestHelper().generateTestRecord(2, "lily", 0, "2022-01-01T12:00:00"));
    insertRecords.add(tableTestHelper().generateTestRecord(3, "john", 0, "2022-01-01T12:00:00"));
    List<DataFile> dataFiles = HiveDataTestHelpers.writeBaseStore(getArcticTable(), 1L, insertRecords, false, true);
    RewriteFiles rewriteFiles = baseStore.newRewrite();
    rewriteFiles.rewriteFiles(Sets.newHashSet(deleteFile), Sets.newHashSet(dataFiles));
    Assert.assertThrows(CannotAlterHiveLocationException.class, rewriteFiles::commit);
  }

  @Test
  public void testRewriteWithFilesUnderDifferentDir() {
    //TODO should add cases for tables without partition spec
    Assume.assumeTrue(isPartitionedTable());
    initDataFiles();
    List<Record> insertRecords = Lists.newArrayList();
    insertRecords.add(tableTestHelper().generateTestRecord(1, "john", 0, "2022-01-01T12:00:00"));
    insertRecords.add(tableTestHelper().generateTestRecord(2, "lily", 0, "2022-01-02T12:00:00"));
    Set<DataFile> addFiles = Sets.newHashSet();
    List<DataFile> dataFiles = HiveDataTestHelpers.writeBaseStore(getArcticTable(), 1L, insertRecords, false, true);
    addFiles.addAll(dataFiles);
    // write data files under another dir
    dataFiles = HiveDataTestHelpers.writeBaseStore(getArcticTable(), 1L, insertRecords, false, true);
    addFiles.addAll(dataFiles);

    UnkeyedTable baseStore = ArcticTableUtil.baseStore(getArcticTable());
    RewriteFiles rewriteFiles = baseStore.newRewrite();
    rewriteFiles.rewriteFiles(Sets.newHashSet(initDataFiles), addFiles);

    Assert.assertThrows(CannotAlterHiveLocationException.class, rewriteFiles::commit);
  }

  @Test
  public void testRewriteByAddFilesInDifferentDir() {
    //TODO should add cases for tables without partition spec
    Assume.assumeTrue(isPartitionedTable());
    initDataFiles();
    List<Record> insertRecords = Lists.newArrayList();
    insertRecords.add(tableTestHelper().generateTestRecord(2, "lily", 0, "2022-01-02T12:00:00"));
    insertRecords.add(tableTestHelper().generateTestRecord(3, "john", 0, "2022-01-03T12:00:00"));
    List<DataFile> dataFiles = HiveDataTestHelpers.writeBaseStore(getArcticTable(), 1L, insertRecords, false, true);
    Set<DataFile> addFiles = Sets.newHashSet(dataFiles);
    addFiles.addAll(initDataFiles);

    UnkeyedTable baseStore = ArcticTableUtil.baseStore(getArcticTable());
    RewriteFiles rewriteFiles = baseStore.newRewrite();
    rewriteFiles.rewriteFiles(Sets.newHashSet(initDataFiles), addFiles);

    Assert.assertThrows(CannotAlterHiveLocationException.class, rewriteFiles::commit);
  }

  @Test
  public void testRewriteWithSameLocation() {
    //TODO should add cases for tables without partition spec
    Assume.assumeTrue(isPartitionedTable());
    initDataFiles();
    UnkeyedTable baseStore = ArcticTableUtil.baseStore(getArcticTable());
    RewriteFiles rewriteFiles = baseStore.newRewrite();
    rewriteFiles.rewriteFiles(Sets.newHashSet(initDataFiles), Sets.newHashSet(initDataFiles));

    Assert.assertThrows(CannotAlterHiveLocationException.class, rewriteFiles::commit);
  }
}
