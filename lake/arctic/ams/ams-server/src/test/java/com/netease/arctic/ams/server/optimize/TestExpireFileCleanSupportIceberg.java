package com.netease.arctic.ams.server.optimize;

import com.netease.arctic.ams.server.service.impl.TableExpireService;
import com.netease.arctic.table.UnkeyedTable;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.DeleteFiles;
import org.apache.iceberg.relocated.com.google.common.collect.Iterables;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;

public class TestExpireFileCleanSupportIceberg extends TestIcebergBase {
  @Test
  public void testExpireTableFiles() throws Exception {
    UnkeyedTable table = icebergNoPartitionTable.asUnkeyedTable();
    List<DataFile> dataFiles = insertDataFiles(table, 1, 1);

    DeleteFiles deleteFiles = table.newDelete();
    for (DataFile dataFile : dataFiles) {
      Assert.assertTrue(icebergNoPartitionTable.io().exists(dataFile.path().toString()));
      deleteFiles.deleteFile(dataFile);
    }
    deleteFiles.commit();

    List<DataFile> newDataFiles = insertDataFiles(table, 1, 1);
    TableExpireService.expireSnapshots(table, System.currentTimeMillis(),
        new HashSet<>());
    Assert.assertEquals(1, Iterables.size(table.snapshots()));

    for (DataFile dataFile : dataFiles) {
      Assert.assertFalse(table.io().exists(dataFile.path().toString()));
    }
    for (DataFile dataFile : newDataFiles) {
      Assert.assertTrue(table.io().exists(dataFile.path().toString()));
    }
  }
}
