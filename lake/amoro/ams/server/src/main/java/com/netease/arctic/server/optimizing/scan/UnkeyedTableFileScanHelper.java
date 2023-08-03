package com.netease.arctic.server.optimizing.scan;

import com.netease.arctic.data.DefaultKeyedFile;
import com.netease.arctic.data.IcebergContentFile;
import com.netease.arctic.data.IcebergDataFile;
import com.netease.arctic.data.IcebergDeleteFile;
import com.netease.arctic.server.ArcticServiceConstants;
import com.netease.arctic.table.UnkeyedTable;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.DeleteFile;
import org.apache.iceberg.FileContent;
import org.apache.iceberg.FileScanTask;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.StructLike;
import org.apache.iceberg.io.CloseableIterable;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.stream.Collectors;

public class UnkeyedTableFileScanHelper implements TableFileScanHelper {
  private static final Logger LOG = LoggerFactory.getLogger(UnkeyedTableFileScanHelper.class);

  private final UnkeyedTable table;
  private PartitionFilter partitionFilter;

  private final long snapshotId;

  public UnkeyedTableFileScanHelper(UnkeyedTable table, long snapshotId) {
    this.table = table;
    this.snapshotId = snapshotId;
  }

  @Override
  public List<FileScanResult> scan() {
    List<FileScanResult> results = Lists.newArrayList();
    LOG.info("{} start scan files with snapshotId = {}", table.id(), snapshotId);
    if (snapshotId == ArcticServiceConstants.INVALID_SNAPSHOT_ID) {
      return results;
    }

    long startTime = System.currentTimeMillis();
    PartitionSpec partitionSpec = table.spec();
    try (CloseableIterable<FileScanTask> filesIterable =
             table.newScan().useSnapshot(snapshotId).planFiles()) {
      for (FileScanTask task : filesIterable) {
        if (partitionFilter != null) {
          StructLike partition = task.file().partition();
          String partitionPath = partitionSpec.partitionToPath(partition);
          if (!partitionFilter.test(partitionPath)) {
            continue;
          }
        }
        IcebergDataFile dataFile = wrapBaseFile(task.file());
        List<IcebergContentFile<?>> deleteFiles =
            task.deletes().stream().map(this::wrapDeleteFile).collect(Collectors.toList());
        results.add(new FileScanResult(dataFile, deleteFiles));
      }
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to close table scan of " + table.id(), e);
    }
    long endTime = System.currentTimeMillis();
    LOG.info("{} finish scan files, cost {} ms, get {} files", table.id(), endTime - startTime, results.size());
    return results;
  }

  @Override
  public TableFileScanHelper withPartitionFilter(PartitionFilter partitionFilter) {
    this.partitionFilter = partitionFilter;
    return this;
  }

  private IcebergDataFile wrapBaseFile(DataFile dataFile) {
    DefaultKeyedFile defaultKeyedFile = DefaultKeyedFile.parseBase(dataFile);
    // the sequence of base file is useless for unkeyed table, since equality-delete files are not supported now
    return new IcebergDataFile(defaultKeyedFile, 0);
  }

  private IcebergContentFile<?> wrapDeleteFile(DeleteFile deleteFile) {
    if (deleteFile.content() == FileContent.EQUALITY_DELETES) {
      throw new UnsupportedOperationException("optimizing unkeyed table not support equality-delete");
    }
    // the sequence of pos-delete file is useless for unkeyed table, since equality-delete files are not supported now
    return new IcebergDeleteFile(deleteFile, 0);
  }

}
