package com.netease.arctic.hive.op;

import com.netease.arctic.hive.HMSClientPool;
import com.netease.arctic.hive.table.UnkeyedHiveTable;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.DeleteFile;
import org.apache.iceberg.RewriteFiles;
import org.apache.iceberg.Snapshot;
import org.apache.iceberg.SnapshotUpdate;
import org.apache.iceberg.Transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static com.netease.arctic.op.OverwriteBaseFiles.PROPERTIES_TRANSACTION_ID;

public class RewriteHiveFiles extends UpdateHiveFiles<RewriteFiles> implements RewriteFiles {

  private final RewriteFiles delegate;

  public RewriteHiveFiles(Transaction transaction, boolean insideTransaction, UnkeyedHiveTable table,
                      HMSClientPool hmsClient, HMSClientPool transactionClient) {
    super(transaction, insideTransaction, table, hmsClient, transactionClient);
    this.delegate = transaction.newRewrite();
  }

  @Override
  SnapshotUpdate<?> getSnapshotUpdateDelegate() {
    return delegate;
  }

  @Override
  public RewriteFiles rewriteFiles(Set<DataFile> filesToDelete, Set<DataFile> filesToAdd) {
    delegate.rewriteFiles(filesToDelete, filesToAdd);
    markHiveFiles(filesToDelete, filesToAdd);
    return this;
  }

  @Override
  public RewriteFiles rewriteFiles(Set<DataFile> filesToDelete, Set<DataFile> filesToAdd, long sequenceNumber) {
    delegate.rewriteFiles(filesToDelete, filesToAdd, sequenceNumber);
    markHiveFiles(filesToDelete, filesToAdd);
    return this;
  }

  @Override
  public RewriteFiles rewriteFiles(Set<DataFile> dataFilesToReplace,
                                   Set<DeleteFile> deleteFilesToReplace,
                                   Set<DataFile> dataFilesToAdd,
                                   Set<DeleteFile> deleteFilesToAdd) {
    delegate.rewriteFiles(dataFilesToReplace, deleteFilesToReplace, dataFilesToAdd, deleteFilesToAdd);
    markHiveFiles(dataFilesToReplace, dataFilesToAdd);

    return this;
  }

  private void markHiveFiles(Set<DataFile> filesToDelete, Set<DataFile> filesToAdd) {
    String hiveLocationRoot = table.hiveLocation();
    // handle filesToAdd, only handle file in hive location
    this.addFiles.addAll(getDataFilesInHiveLocation(filesToAdd, hiveLocationRoot));

    // handle filesToDelete, only handle file in hive location
    this.deleteFiles.addAll(getDataFilesInHiveLocation(filesToDelete, hiveLocationRoot));
  }

  @Override
  public RewriteFiles validateFromSnapshot(long snapshotId) {
    delegate.validateFromSnapshot(snapshotId);
    return this;
  }

  @Override
  public RewriteFiles set(String property, String value) {
    if (PROPERTIES_TRANSACTION_ID.equals(property)) {
      this.txId = Long.parseLong(value);
    }

    if (PROPERTIES_VALIDATE_LOCATION.equals(property)) {
      this.validateLocation = Boolean.parseBoolean(value);
    }

    if (DELETE_UNTRACKED_HIVE_FILE.equals(property)) {
      this.checkOrphanFiles = Boolean.parseBoolean(value);
    }

    delegate.set(property, value);
    return this;
  }

  @Override
  public RewriteFiles deleteWith(Consumer<String> deleteFunc) {
    delegate.deleteWith(deleteFunc);
    return this;
  }

  @Override
  public RewriteFiles stageOnly() {
    delegate.stageOnly();
    return this;
  }

  @Override
  public Snapshot apply() {
    return delegate.apply();
  }

  @Override
  public Object updateEvent() {
    return delegate.updateEvent();
  }

  private List<DataFile> getDataFilesInHiveLocation(Set<DataFile> dataFiles, String hiveLocation) {
    List<DataFile> result = new ArrayList<>();
    for (DataFile dataFile : dataFiles) {
      String dataFileLocation = dataFile.path().toString();
      if (dataFileLocation.toLowerCase().contains(hiveLocation.toLowerCase())) {
        // only handle file in hive location
        result.add(dataFile);
      }
    }

    return result;
  }
}
