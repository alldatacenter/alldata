package com.netease.arctic.hive.op;

import com.netease.arctic.hive.MockDataFileBuilder;
import com.netease.arctic.hive.catalog.HiveTableTestBase;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.UnkeyedTable;
import org.apache.iceberg.AppendFiles;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.OverwriteFiles;
import org.apache.iceberg.ReplacePartitions;
import org.apache.iceberg.RewriteFiles;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.apache.iceberg.relocated.com.google.common.collect.Maps;
import org.apache.thrift.TException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.netease.arctic.hive.op.UpdateHiveFiles.DELETE_UNTRACKED_HIVE_FILE;

@RunWith(Parameterized.class)
public class TestCleanOrphanFiles extends HiveTableTestBase {

  private String valuePath = null;

  private UnkeyedTable operationTable;

  public TestCleanOrphanFiles(
      boolean keyedTable,
      boolean partitionedTable) {
    super(keyedTable, partitionedTable);
    if (isPartitionedTable()) {
      this.valuePath = "name=aaa";
    }
  }

  @Parameterized.Parameters(name = "keyedTable = {0}, partitionedTable = {1}")
  public static Object[][] parameters() {
    return new Object[][] {{false, true}, {false, false}, {true, true}, {true, false}};
  }

  private UnkeyedTable getOperationTable() {
    if (operationTable == null) {
      ArcticTable arcticTable = getArcticTable();
      if (isKeyedTable()) {
        operationTable = arcticTable.asKeyedTable().baseTable();
      } else {
        operationTable = arcticTable.asUnkeyedTable();
      }
    }
    return operationTable;
  }

  @Test
  public void testCleanOrphanFileWhenOverwrite() throws TException {
    List<Map.Entry<String, String>> orphanFiles = Lists.newArrayList(
        Maps.immutableEntry(valuePath, "/test_path/partition/orphan-a1.parquet"),
        Maps.immutableEntry(valuePath, "/test_path/partition/orphan-a2.parquet"),
        Maps.immutableEntry(valuePath, "/test_path/partition/orphan-a3.parquet")
    );
    UnkeyedTable table = getOperationTable();
    table.updateProperties().set(DELETE_UNTRACKED_HIVE_FILE, "true").commit();
    AppendFiles appendFiles = table.newAppend();
    MockDataFileBuilder dataFileBuilder = new MockDataFileBuilder(table, TEST_HMS.getHiveClient());
    List<DataFile> orphanDataFiles = dataFileBuilder.buildList(orphanFiles);
    orphanDataFiles.forEach(appendFiles::appendFile);
    appendFiles.commit();

    List<Map.Entry<String, String>> files = Lists.newArrayList(
        Maps.immutableEntry(valuePath, "/test_path/partition/data-a1.parquet"),
        Maps.immutableEntry(valuePath, "/test_path/partition/data-a2.parquet"),
        Maps.immutableEntry(valuePath, "/test_path/partition/data-a3.parquet")
    );
    List<DataFile> dataFiles = dataFileBuilder.buildList(files);

    OverwriteFiles overwriteFiles = table.newOverwrite();
    overwriteFiles.set(DELETE_UNTRACKED_HIVE_FILE, "true");
    dataFiles.forEach(overwriteFiles::addFile);
    overwriteFiles.commit();

    List<String> exceptedFiles = new ArrayList<>();
    exceptedFiles.add("data-a1.parquet");
    exceptedFiles.add("data-a2.parquet");
    exceptedFiles.add("data-a3.parquet");
    asserFilesName(exceptedFiles, table);
  }

  @Test
  public void testCleanOrphanFileWhenRewrite() throws TException {
    List<Map.Entry<String, String>> orphanFiles = Lists.newArrayList(
        Maps.immutableEntry(valuePath, "/test_path/partition/data-a1.parquet"),
        Maps.immutableEntry(valuePath, "/test_path/partition/data-a2.parquet"),
        Maps.immutableEntry(valuePath, "/test_path/partition/data-a3.parquet"),
        Maps.immutableEntry(valuePath, "/test_path/partition/orphan-a1.parquet"),
        Maps.immutableEntry(valuePath, "/test_path/partition/orphan-a2.parquet"),
        Maps.immutableEntry(valuePath, "/test_path/partition/orphan-a3.parquet")
    );
    UnkeyedTable table = getOperationTable();
    table.updateProperties().set(DELETE_UNTRACKED_HIVE_FILE, "true").commit();
    AppendFiles appendFiles = table.newAppend();
    MockDataFileBuilder dataFileBuilder = new MockDataFileBuilder(table, TEST_HMS.getHiveClient());
    List<DataFile> orphanDataFiles = dataFileBuilder.buildList(orphanFiles);
    orphanDataFiles.forEach(appendFiles::appendFile);
    appendFiles.commit();

    List<Map.Entry<String, String>> files = Lists.newArrayList(
        Maps.immutableEntry(valuePath, "/test_path/partition/data-a1.parquet")
    );
    Set<DataFile> initDataFiles = new HashSet<>(dataFileBuilder.buildList(files));

    List<Map.Entry<String, String>> newFiles = Lists.newArrayList(
        Maps.immutableEntry(valuePath, "/test_path/partition/data-a1.parquet"),
        Maps.immutableEntry(valuePath, "/test_path/partition/data-a3.parquet")
    );
    Set<DataFile> newDataFiles = new HashSet<>(dataFileBuilder.buildList(newFiles));

    RewriteFiles rewriteFiles = table.newRewrite();
    rewriteFiles.rewriteFiles(initDataFiles, newDataFiles);
    rewriteFiles.set(DELETE_UNTRACKED_HIVE_FILE, "true");
    rewriteFiles.commit();

    List<String> exceptedFiles = new ArrayList<>();
    exceptedFiles.add("data-a1.parquet");
    exceptedFiles.add("data-a3.parquet");
    asserFilesName(exceptedFiles, table);
  }

  @Test
  public void testCleanOrphanFileWhenRewritePartition() throws TException {
    List<Map.Entry<String, String>> orphanFiles = Lists.newArrayList(
        Maps.immutableEntry(valuePath, "/test_path/partition/orphan-a1.parquet"),
        Maps.immutableEntry(valuePath, "/test_path/partition/orphan-a2.parquet"),
        Maps.immutableEntry(valuePath, "/test_path/partition/orphan-a3.parquet")
    );
    UnkeyedTable table = getOperationTable();
    AppendFiles appendFiles = table.newAppend();
    MockDataFileBuilder dataFileBuilder = new MockDataFileBuilder(table, TEST_HMS.getHiveClient());
    List<DataFile> orphanDataFiles = dataFileBuilder.buildList(orphanFiles);
    orphanDataFiles.forEach(appendFiles::appendFile);
    appendFiles.commit();

    List<Map.Entry<String, String>> files = Lists.newArrayList(
        Maps.immutableEntry(valuePath, "/test_path/partition/data-a1.parquet"),
        Maps.immutableEntry(valuePath, "/test_path/partition/data-a2.parquet"),
        Maps.immutableEntry(valuePath, "/test_path/partition/data-a3.parquet")
    );
    List<DataFile> dataFiles = dataFileBuilder.buildList(files);

    ReplacePartitions replacePartitions = table.newReplacePartitions();
    dataFiles.forEach(replacePartitions::addFile);
    replacePartitions.commit();

    List<String> exceptedFiles = new ArrayList<>();
    exceptedFiles.add("data-a1.parquet");
    exceptedFiles.add("data-a2.parquet");
    exceptedFiles.add("data-a3.parquet");
    asserFilesName(exceptedFiles, table);
  }
}
