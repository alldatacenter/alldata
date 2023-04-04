package com.netease.arctic.ams.server.optimize;

import com.netease.arctic.ams.server.service.impl.OrphanFilesCleanService;
import org.apache.iceberg.io.OutputFile;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static com.netease.arctic.ams.server.service.impl.OrphanFilesCleanService.DATA_FOLDER_NAME;

public class TestOrphanFileCleanSupportIceberg extends TestIcebergBase {
  @Test
  public void orphanDataFileClean() throws IOException {
    String orphanFilePath = icebergNoPartitionTable.asUnkeyedTable().location() +
        File.separator + DATA_FOLDER_NAME + File.separator + "orphan.parquet";
    OutputFile baseOrphanDataFile = icebergNoPartitionTable.io().newOutputFile(orphanFilePath);
    baseOrphanDataFile.createOrOverwrite().close();
    Assert.assertTrue(icebergNoPartitionTable.io().exists(orphanFilePath));
    OrphanFilesCleanService.cleanContentFiles(icebergNoPartitionTable, System.currentTimeMillis());
    Assert.assertFalse(icebergNoPartitionTable.io().exists(orphanFilePath));
  }

  @Test
  public void orphanMetadataFileClean() throws IOException {
    String orphanFilePath = icebergNoPartitionTable.asUnkeyedTable().location() + File.separator + "metadata" +
        File.separator + "orphan.avro";
    OutputFile baseOrphanDataFile = icebergNoPartitionTable.io().newOutputFile(orphanFilePath);
    baseOrphanDataFile.createOrOverwrite().close();
    Assert.assertTrue(icebergNoPartitionTable.io().exists(orphanFilePath));
    OrphanFilesCleanService.cleanMetadata(icebergNoPartitionTable, System.currentTimeMillis());
    Assert.assertFalse(icebergNoPartitionTable.io().exists(orphanFilePath));
  }
}
