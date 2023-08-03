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

package com.netease.arctic.trino;

import com.netease.arctic.AmsClient;
import com.netease.arctic.ams.api.CatalogMeta;
import com.netease.arctic.ams.api.TableFormat;
import com.netease.arctic.catalog.ArcticCatalog;
import com.netease.arctic.catalog.BasicArcticCatalog;
import com.netease.arctic.io.ArcticFileIO;
import com.netease.arctic.op.UpdatePartitionProperties;
import com.netease.arctic.scan.ChangeTableIncrementalScan;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.BasicUnkeyedTable;
import com.netease.arctic.table.ChangeTable;
import com.netease.arctic.table.KeyedTable;
import com.netease.arctic.table.MetadataColumns;
import com.netease.arctic.table.TableBuilder;
import com.netease.arctic.table.TableIdentifier;
import com.netease.arctic.table.TableMetaStore;
import com.netease.arctic.table.blocker.TableBlockerManager;
import org.apache.iceberg.AppendFiles;
import org.apache.iceberg.DeleteFiles;
import org.apache.iceberg.ExpireSnapshots;
import org.apache.iceberg.HasTableOperations;
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
import org.apache.iceberg.TableOperations;
import org.apache.iceberg.Transaction;
import org.apache.iceberg.UpdateLocation;
import org.apache.iceberg.UpdatePartitionSpec;
import org.apache.iceberg.UpdateProperties;
import org.apache.iceberg.UpdateSchema;
import org.apache.iceberg.encryption.EncryptionManager;
import org.apache.iceberg.io.LocationProvider;
import org.apache.iceberg.types.Types;
import org.apache.iceberg.util.StructLikeMap;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A wrapper of {@link ArcticCatalog} to resolve sub table, such as "tableName#change","tableName#base"
 */
public class ArcticCatalogSupportTableSuffix implements ArcticCatalog {

  private ArcticCatalog arcticCatalog;

  public ArcticCatalogSupportTableSuffix(ArcticCatalog arcticCatalog) {
    this.arcticCatalog = arcticCatalog;
  }

  @Override
  public String name() {
    return arcticCatalog.name();
  }

  @Override
  public void initialize(
      AmsClient client, CatalogMeta meta, Map<String, String> properties) {
    arcticCatalog.initialize(client, meta, properties);
  }

  @Override
  public List<String> listDatabases() {
    return arcticCatalog.listDatabases();
  }

  @Override
  public void createDatabase(String databaseName) {
    arcticCatalog.createDatabase(databaseName);
  }

  @Override
  public void dropDatabase(String databaseName) {
    arcticCatalog.dropDatabase(databaseName);
  }

  @Override
  public List<TableIdentifier> listTables(String database) {
    return arcticCatalog.listTables(database);
  }

  @Override
  public ArcticTable loadTable(TableIdentifier tableIdentifier) {
    TableNameResolve tableNameResolve = new TableNameResolve(tableIdentifier.getTableName());
    if (tableNameResolve.withSuffix()) {
      TableIdentifier newTableIdentifier = TableIdentifier.of(tableIdentifier.getCatalog(),
          tableIdentifier.getDatabase(), tableNameResolve.getTableName());
      ArcticTable arcticTable = arcticCatalog.loadTable(newTableIdentifier);
      if (arcticTable.isUnkeyedTable()) {
        throw new IllegalArgumentException("table " + newTableIdentifier + " is not keyed table can not use " +
            "change or base suffix");
      }
      KeyedTable keyedTable = arcticTable.asKeyedTable();
      if (tableNameResolve.isBase()) {
        return keyedTable.baseTable();
      } else {
        return new ChangeTableWithExternalSchemas((BasicUnkeyedTable) keyedTable.changeTable());
      }
    }
    return arcticCatalog.loadTable(tableIdentifier);
  }

  @Override
  public void renameTable(TableIdentifier from, String newTableName) {
    arcticCatalog.renameTable(from, newTableName);
  }

  @Override
  public boolean dropTable(TableIdentifier tableIdentifier, boolean purge) {
    return arcticCatalog.dropTable(tableIdentifier, purge);
  }

  @Override
  public TableBuilder newTableBuilder(
      TableIdentifier identifier, Schema schema) {
    return arcticCatalog.newTableBuilder(identifier, schema);
  }

  @Override
  public void refresh() {
    arcticCatalog.refresh();
  }

  @Override
  public TableBlockerManager getTableBlockerManager(TableIdentifier tableIdentifier) {
    return arcticCatalog.getTableBlockerManager(tableIdentifier);
  }

  @Override
  public Map<String, String> properties() {
    return arcticCatalog.properties();
  }

  public TableMetaStore getTableMetaStore() {
    return ((BasicArcticCatalog) arcticCatalog).getTableMetaStore();
  }

  private static class ChangeTableWithExternalSchemas implements ChangeTable, HasTableOperations {

    private final BasicUnkeyedTable table;

    public ChangeTableWithExternalSchemas(BasicUnkeyedTable table) {
      this.table = table;
    }

    @Override
    public void refresh() {
      table.refresh();
    }

    @Override
    public TableIdentifier id() {
      return table.id();
    }

    @Override
    public TableFormat format() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Schema schema() {
      Schema schema = table.schema();
      List<Types.NestedField> columns = schema.columns().stream().collect(Collectors.toList());
      columns.add(MetadataColumns.TRANSACTION_ID_FILED);
      columns.add(MetadataColumns.FILE_OFFSET_FILED);
      columns.add(MetadataColumns.CHANGE_ACTION_FIELD);
      return new Schema(columns);
    }

    @Override
    public Map<Integer, Schema> schemas() {
      return table.schemas();
    }

    @Override
    public PartitionSpec spec() {
      return table.spec();
    }

    @Override
    public Map<Integer, PartitionSpec> specs() {
      return table.specs();
    }

    @Override
    public SortOrder sortOrder() {
      return table.sortOrder();
    }

    @Override
    public Map<Integer, SortOrder> sortOrders() {
      return table.sortOrders();
    }

    @Override
    public Map<String, String> properties() {
      return table.properties();
    }

    @Override
    public String location() {
      return table.location();
    }

    @Override
    public Snapshot currentSnapshot() {
      return table.currentSnapshot();
    }

    @Override
    public Snapshot snapshot(long snapshotId) {
      return table.snapshot(snapshotId);
    }

    @Override
    public Iterable<Snapshot> snapshots() {
      return table.snapshots();
    }

    @Override
    public List<HistoryEntry> history() {
      return table.history();
    }

    @Override
    public UpdateSchema updateSchema() {
      return table.updateSchema();
    }

    @Override
    public UpdatePartitionSpec updateSpec() {
      return table.updateSpec();
    }

    @Override
    public UpdateProperties updateProperties() {
      return table.updateProperties();
    }

    @Override
    public ReplaceSortOrder replaceSortOrder() {
      return table.replaceSortOrder();
    }

    @Override
    public UpdateLocation updateLocation() {
      return table.updateLocation();
    }

    @Override
    public AppendFiles newAppend() {
      return table.newAppend();
    }

    @Override
    public RewriteFiles newRewrite() {
      return table.newRewrite();
    }

    @Override
    public RewriteManifests rewriteManifests() {
      return table.rewriteManifests();
    }

    @Override
    public OverwriteFiles newOverwrite() {
      return table.newOverwrite();
    }

    @Override
    public RowDelta newRowDelta() {
      return table.newRowDelta();
    }

    @Override
    public ReplacePartitions newReplacePartitions() {
      return table.newReplacePartitions();
    }

    @Override
    public DeleteFiles newDelete() {
      return table.newDelete();
    }

    @Override
    public ExpireSnapshots expireSnapshots() {
      return table.expireSnapshots();
    }

    @Override
    public ManageSnapshots manageSnapshots() {
      return table.manageSnapshots();
    }

    @Override
    public Transaction newTransaction() {
      return table.newTransaction();
    }

    @Override
    public ArcticFileIO io() {
      return table.io();
    }

    @Override
    public EncryptionManager encryption() {
      return table.encryption();
    }

    @Override
    public LocationProvider locationProvider() {
      return table.locationProvider();
    }

    @Override
    public List<StatisticsFile> statisticsFiles() {
      return table.statisticsFiles();
    }

    @Override
    public Map<String, SnapshotRef> refs() {
      return table.refs();
    }

    @Override
    public TableOperations operations() {
      return table.operations();
    }

    @Override
    public StructLikeMap<Map<String, String>> partitionProperty() {
      return table.partitionProperty();
    }

    @Override
    public UpdatePartitionProperties updatePartitionProperties(Transaction transaction) {
      return table.updatePartitionProperties(transaction);
    }

    @Override
    public ChangeTableIncrementalScan newScan() {
      if (table instanceof ChangeTable) {
        return ((ChangeTable) table).newScan();
      } else {
        throw new UnsupportedOperationException();
      }
    }
  }
}
