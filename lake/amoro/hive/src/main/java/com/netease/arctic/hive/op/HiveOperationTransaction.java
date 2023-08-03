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

package com.netease.arctic.hive.op;

import com.google.common.collect.Lists;
import com.netease.arctic.hive.HMSClient;
import com.netease.arctic.hive.HMSClientPool;
import com.netease.arctic.hive.table.UnkeyedHiveTable;
import org.apache.iceberg.AppendFiles;
import org.apache.iceberg.DeleteFiles;
import org.apache.iceberg.ExpireSnapshots;
import org.apache.iceberg.HistoryEntry;
import org.apache.iceberg.ManageSnapshots;
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
import org.apache.iceberg.TableScan;
import org.apache.iceberg.Transaction;
import org.apache.iceberg.UpdateLocation;
import org.apache.iceberg.UpdatePartitionSpec;
import org.apache.iceberg.UpdateProperties;
import org.apache.iceberg.UpdateSchema;
import org.apache.iceberg.encryption.EncryptionManager;
import org.apache.iceberg.io.FileIO;
import org.apache.iceberg.io.LocationProvider;
import org.apache.thrift.TException;

import java.util.List;
import java.util.Map;

public class HiveOperationTransaction implements Transaction {

  private final UnkeyedHiveTable unkeyedHiveTable;
  private final Transaction wrapped;
  private final HMSClientPool client;
  private final TransactionalHMSClient transactionalClient;

  private final TransactionalTable transactionalTable;

  public HiveOperationTransaction(
      UnkeyedHiveTable unkeyedHiveTable,
      Transaction wrapped,
      HMSClientPool client) {
    this.unkeyedHiveTable = unkeyedHiveTable;
    this.wrapped = wrapped;
    this.client = client;
    this.transactionalTable = new TransactionalTable();
    this.transactionalClient = new TransactionalHMSClient();
  }

  @Override
  public Table table() {
    return transactionalTable;
  }

  @Override
  public UpdateSchema updateSchema() {
    return new HiveSchemaUpdate(unkeyedHiveTable, client, transactionalClient, wrapped.updateSchema());
  }

  @Override
  public UpdatePartitionSpec updateSpec() {
    return wrapped.updateSpec();
  }

  @Override
  public UpdateProperties updateProperties() {
    return wrapped.updateProperties();
  }

  @Override
  public ReplaceSortOrder replaceSortOrder() {
    return wrapped.replaceSortOrder();
  }

  @Override
  public UpdateLocation updateLocation() {
    return wrapped.updateLocation();
  }

  @Override
  public AppendFiles newAppend() {
    return wrapped.newAppend();
  }

  @Override
  public AppendFiles newFastAppend() {
    return wrapped.newFastAppend();
  }

  @Override
  public RewriteFiles newRewrite() {
    return new RewriteHiveFiles(wrapped, true, unkeyedHiveTable, client, transactionalClient);
  }

  @Override
  public RewriteManifests rewriteManifests() {
    return wrapped.rewriteManifests();
  }

  @Override
  public OverwriteFiles newOverwrite() {
    return new OverwriteHiveFiles(wrapped, true, unkeyedHiveTable, client, transactionalClient);
  }

  @Override
  public RowDelta newRowDelta() {
    return wrapped.newRowDelta();
  }

  @Override
  public ReplacePartitions newReplacePartitions() {
    return new ReplaceHivePartitions(wrapped, true, unkeyedHiveTable, client, transactionalClient);
  }

  @Override
  public DeleteFiles newDelete() {
    return wrapped.newDelete();
  }

  @Override
  public ExpireSnapshots expireSnapshots() {
    return wrapped.expireSnapshots();
  }

  @Override
  public void commitTransaction() {
    wrapped.commitTransaction();
    transactionalClient.commit();
  }

  private class TransactionalHMSClient implements HMSClientPool {
    List<Action<?, HMSClient, TException>> pendingActions = Lists.newArrayList();

    @Override
    public <R> R run(Action<R, HMSClient, TException> action) {
      pendingActions.add(action);
      return null;
    }

    @Override
    public <R> R run(Action<R, HMSClient, TException> action, boolean retry) throws TException, InterruptedException {
      pendingActions.add(action);
      return null;
    }

    public void commit() {
      for (Action<?, HMSClient, TException> action : pendingActions) {
        try {
          client.run(action);
        } catch (TException | InterruptedException e) {
          throw new RuntimeException("execute pending hive operation failed.", e);
        }
      }
    }
  }

  private class TransactionalTable implements Table {

    Table transactionTable;

    public TransactionalTable() {
      transactionTable = wrapped.table();
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
      return HiveOperationTransaction.this.updateSchema();
    }

    @Override
    public UpdatePartitionSpec updateSpec() {
      return HiveOperationTransaction.this.updateSpec();
    }

    @Override
    public UpdateProperties updateProperties() {
      return HiveOperationTransaction.this.updateProperties();
    }

    @Override
    public ReplaceSortOrder replaceSortOrder() {
      return HiveOperationTransaction.this.replaceSortOrder();
    }

    @Override
    public UpdateLocation updateLocation() {
      return HiveOperationTransaction.this.updateLocation();
    }

    @Override
    public AppendFiles newAppend() {
      return HiveOperationTransaction.this.newAppend();
    }

    @Override
    public AppendFiles newFastAppend() {
      return HiveOperationTransaction.this.newFastAppend();
    }

    @Override
    public RewriteFiles newRewrite() {
      return HiveOperationTransaction.this.newRewrite();
    }

    @Override
    public RewriteManifests rewriteManifests() {
      return HiveOperationTransaction.this.rewriteManifests();
    }

    @Override
    public OverwriteFiles newOverwrite() {
      return HiveOperationTransaction.this.newOverwrite();
    }

    @Override
    public RowDelta newRowDelta() {
      return HiveOperationTransaction.this.newRowDelta();
    }

    @Override
    public ReplacePartitions newReplacePartitions() {
      return HiveOperationTransaction.this.newReplacePartitions();
    }

    @Override
    public DeleteFiles newDelete() {
      return HiveOperationTransaction.this.newDelete();
    }

    @Override
    public ExpireSnapshots expireSnapshots() {
      return HiveOperationTransaction.this.expireSnapshots();
    }

    @Override
    public ManageSnapshots manageSnapshots() {
      throw new UnsupportedOperationException("Transaction tables do not support rollback");
    }

    @Override
    public Transaction newTransaction() {
      throw new UnsupportedOperationException("Transaction tables do not support rollback");
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
  }
}
