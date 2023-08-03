/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.trace;

import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.KeyedTable;
import com.netease.arctic.table.UnkeyedTable;
import org.apache.iceberg.AppendFiles;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.DeleteFile;
import org.apache.iceberg.DeleteFiles;
import org.apache.iceberg.ExpireSnapshots;
import org.apache.iceberg.HasTableOperations;
import org.apache.iceberg.HistoryEntry;
import org.apache.iceberg.ManageSnapshots;
import org.apache.iceberg.ManifestFile;
import org.apache.iceberg.OverwriteFiles;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.ReplacePartitions;
import org.apache.iceberg.ReplaceSortOrder;
import org.apache.iceberg.RewriteFiles;
import org.apache.iceberg.RewriteManifests;
import org.apache.iceberg.RowDelta;
import org.apache.iceberg.Schema;
import org.apache.iceberg.Snapshot;
import org.apache.iceberg.SnapshotRef;
import org.apache.iceberg.SortOrder;
import org.apache.iceberg.StatisticsFile;
import org.apache.iceberg.Table;
import org.apache.iceberg.TableOperations;
import org.apache.iceberg.TableScan;
import org.apache.iceberg.Transaction;
import org.apache.iceberg.UpdateLocation;
import org.apache.iceberg.UpdatePartitionSpec;
import org.apache.iceberg.UpdateProperties;
import org.apache.iceberg.UpdateSchema;
import org.apache.iceberg.encryption.EncryptionManager;
import org.apache.iceberg.io.FileIO;
import org.apache.iceberg.io.LocationProvider;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Supplier;


public class CreateTableTransaction implements Transaction {

  private final Transaction fakeTransaction;

  private final TransactionTracker transactionTracer;
  private final Table transactionTable;
  private final Supplier<ArcticTable> createTable;
  private final Runnable rollback;

  public CreateTableTransaction(
      Transaction fakeTransaction,
      Supplier<ArcticTable> createTable,
      Runnable rollback) {
    this.fakeTransaction = fakeTransaction;
    this.transactionTracer = new TransactionTracker();
    this.transactionTable = new TransactionTable();
    this.createTable = createTable;
    this.rollback = rollback;
  }

  @Override
  public Table table() {
    return transactionTable;
  }

  @Override
  public UpdateSchema updateSchema() {
    throw new UnsupportedOperationException("create table transaction unsupported updateSchema");
  }

  @Override
  public UpdatePartitionSpec updateSpec() {
    throw new UnsupportedOperationException("create table transaction unsupported updateSpec");
  }

  @Override
  public UpdateProperties updateProperties() {
    throw new UnsupportedOperationException("create table transaction unsupported updateProperties");
  }

  @Override
  public ReplaceSortOrder replaceSortOrder() {
    throw new UnsupportedOperationException("create table transaction unsupported updateProperties");
  }

  @Override
  public UpdateLocation updateLocation() {
    throw new UnsupportedOperationException("create table transaction unsupported updateProperties");
  }

  @Override
  public AppendFiles newAppend() {
    return new AppendFiles() {
      @Override
      public AppendFiles appendFile(DataFile file) {
        transactionTracer.addDataFile(file);
        return this;
      }

      @Override
      public AppendFiles appendManifest(ManifestFile file) {
        throw new UnsupportedOperationException("create table transaction AppendFiles unsupported ManifestFile");
      }

      @Override
      public AppendFiles set(String property, String value) {
        throw new UnsupportedOperationException("create table transaction AppendFiles unsupported set");
      }

      @Override
      public AppendFiles deleteWith(Consumer<String> deleteFunc) {
        throw new UnsupportedOperationException("create table transaction AppendFiles unsupported deleteWith");
      }

      @Override
      public AppendFiles stageOnly() {
        throw new UnsupportedOperationException("create table transaction AppendFiles unsupported stageOnly");
      }

      @Override
      public AppendFiles scanManifestsWith(ExecutorService executorService) {
        throw new UnsupportedOperationException("create table transaction AppendFiles unsupported scanManifestsWith");
      }

      @Override
      public Snapshot apply() {
        throw new UnsupportedOperationException("create table transaction AppendFiles unsupported apply");
      }

      @Override
      public void commit() {
        transactionTracer.commit();
      }
    };
  }

  @Override
  public AppendFiles newFastAppend() {
    throw new UnsupportedOperationException("create table transaction unsupported updateProperties");
  }

  @Override
  public RewriteFiles newRewrite() {
    throw new UnsupportedOperationException("create table transaction unsupported newRewrite");
  }

  @Override
  public RewriteManifests rewriteManifests() {
    throw new UnsupportedOperationException("create table transaction unsupported rewriteManifests");
  }

  @Override
  public OverwriteFiles newOverwrite() {
    throw new UnsupportedOperationException("create table transaction unsupported newOverwrite");
  }

  @Override
  public RowDelta newRowDelta() {
    throw new UnsupportedOperationException("create table transaction unsupported newRowDelta");
  }

  @Override
  public ReplacePartitions newReplacePartitions() {
    throw new UnsupportedOperationException("create table transaction unsupported newReplacePartitions");
  }

  @Override
  public DeleteFiles newDelete() {
    throw new UnsupportedOperationException("create table transaction unsupported newReplacePartitions");
  }

  @Override
  public ExpireSnapshots expireSnapshots() {
    throw new UnsupportedOperationException("create table transaction unsupported newReplacePartitions");
  }

  @Override
  public void commitTransaction() {
    try {
      ArcticTable arcticTable = createTable.get();
      if (!transactionTracer.add.isEmpty()) {
        if (!transactionTracer.isCommit) {
          throw new IllegalStateException("last operation has not committed");
        }
        Transaction transaction;
        if (arcticTable.isUnkeyedTable()) {
          UnkeyedTable table = arcticTable.asUnkeyedTable();
          transaction = table.newTransaction();
        } else {
          KeyedTable keyedTable = arcticTable.asKeyedTable();
          transaction = keyedTable.baseTable().newTransaction();
        }
        AppendFiles appendFiles = transaction.newAppend();
        for (DataFile dataFile : transactionTracer.add) {
          appendFiles.appendFile(dataFile);
        }
        appendFiles.commit();
        transaction.commitTransaction();
      }
    } catch (Throwable t) {
      rollback.run();
      throw t;
    }
  }

  static class TransactionTracker implements TableTracer {

    private final List<DataFile> add = new ArrayList<>();

    private boolean isCommit;

    @Override
    public void addDataFile(DataFile dataFile) {
      add.add(dataFile);
    }

    @Override
    public void deleteDataFile(DataFile dataFile) {
    }

    @Override
    public void addDeleteFile(DeleteFile deleteFile) {
    }

    @Override
    public void deleteDeleteFile(DeleteFile deleteFile) {
    }

    @Override
    public void commit() {
      isCommit = true;
    }

    @Override
    public void replaceProperties(Map<String, String> newProperties) {
    }

    @Override
    public void setSnapshotSummary(String key, String value) {
    }

    @Override
    public void updateColumn(UpdateColumn updateColumn) {

    }
  }

  class TransactionTable implements Table, HasTableOperations, Serializable {

    Table transactionTable;

    public TransactionTable() {
      transactionTable = fakeTransaction.table();
    }

    @Override
    public TableOperations operations() {
      if (transactionTable instanceof HasTableOperations) {
        return ((HasTableOperations) transactionTable).operations();
      }
      throw new IllegalStateException("table does not support operations");
    }

    @Override
    public String name() {
      return transactionTable.name();
    }

    @Override
    public void refresh() {
      transactionTable.refresh();
    }

    @Override
    public TableScan newScan() {
      return transactionTable.newScan();
    }

    @Override
    public Schema schema() {
      return transactionTable.schema();
    }

    @Override
    public Map<Integer, Schema> schemas() {
      return transactionTable.schemas();
    }

    @Override
    public PartitionSpec spec() {
      return transactionTable.spec();
    }

    @Override
    public Map<Integer, PartitionSpec> specs() {
      return transactionTable.specs();
    }

    @Override
    public SortOrder sortOrder() {
      return transactionTable.sortOrder();
    }

    @Override
    public Map<Integer, SortOrder> sortOrders() {
      return transactionTable.sortOrders();
    }

    @Override
    public Map<String, String> properties() {
      return transactionTable.properties();
    }

    @Override
    public String location() {
      return transactionTable.location();
    }

    @Override
    public Snapshot currentSnapshot() {
      return transactionTable.currentSnapshot();
    }

    @Override
    public Snapshot snapshot(long snapshotId) {
      return transactionTable.snapshot(snapshotId);
    }

    @Override
    public Iterable<Snapshot> snapshots() {
      return transactionTable.snapshots();
    }

    @Override
    public List<HistoryEntry> history() {
      return transactionTable.history();
    }

    @Override
    public UpdateSchema updateSchema() {
      return transactionTable.updateSchema();
    }

    @Override
    public UpdatePartitionSpec updateSpec() {
      return transactionTable.updateSpec();
    }

    @Override
    public UpdateProperties updateProperties() {
      return transactionTable.updateProperties();
    }

    @Override
    public ReplaceSortOrder replaceSortOrder() {
      return transactionTable.replaceSortOrder();
    }

    @Override
    public UpdateLocation updateLocation() {
      return transactionTable.updateLocation();
    }

    @Override
    public AppendFiles newAppend() {
      return transactionTable.newAppend();
    }

    @Override
    public AppendFiles newFastAppend() {
      return transactionTable.newFastAppend();
    }

    @Override
    public RewriteFiles newRewrite() {
      return transactionTable.newRewrite();
    }

    @Override
    public RewriteManifests rewriteManifests() {
      return transactionTable.rewriteManifests();
    }

    @Override
    public OverwriteFiles newOverwrite() {
      return transactionTable.newOverwrite();
    }

    @Override
    public RowDelta newRowDelta() {
      return transactionTable.newRowDelta();
    }

    @Override
    public ReplacePartitions newReplacePartitions() {
      return transactionTable.newReplacePartitions();
    }

    @Override
    public DeleteFiles newDelete() {
      return transactionTable.newDelete();
    }

    @Override
    public ExpireSnapshots expireSnapshots() {
      return transactionTable.expireSnapshots();
    }


    @Override
    public ManageSnapshots manageSnapshots() {
      return transactionTable.manageSnapshots();
    }

    @Override
    public Transaction newTransaction() {
      return transactionTable.newTransaction();
    }

    @Override
    public FileIO io() {
      return transactionTable.io();
    }

    @Override
    public EncryptionManager encryption() {
      return transactionTable.encryption();
    }

    @Override
    public LocationProvider locationProvider() {
      return transactionTable.locationProvider();
    }

    @Override
    public List<StatisticsFile> statisticsFiles() {
      return transactionTable.statisticsFiles();
    }

    @Override
    public Map<String, SnapshotRef> refs() {
      return transactionTable.refs();
    }

    @Override
    public String toString() {
      return transactionTable.toString();
    }
  }
}




