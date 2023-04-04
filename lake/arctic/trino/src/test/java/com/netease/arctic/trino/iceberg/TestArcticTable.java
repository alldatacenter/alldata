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

package com.netease.arctic.trino.iceberg;

import com.netease.arctic.io.ArcticFileIO;
import com.netease.arctic.table.BasicUnkeyedTable;
import com.netease.arctic.table.TableIdentifier;
import org.apache.iceberg.AppendFiles;
import org.apache.iceberg.BaseTable;
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
import org.apache.iceberg.Rollback;
import org.apache.iceberg.RowDelta;
import org.apache.iceberg.Schema;
import org.apache.iceberg.Snapshot;
import org.apache.iceberg.SortOrder;
import org.apache.iceberg.TableOperations;
import org.apache.iceberg.TableScan;
import org.apache.iceberg.Transaction;
import org.apache.iceberg.UpdateLocation;
import org.apache.iceberg.UpdatePartitionSpec;
import org.apache.iceberg.UpdateProperties;
import org.apache.iceberg.UpdateSchema;
import org.apache.iceberg.encryption.EncryptionManager;
import org.apache.iceberg.io.LocationProvider;

import java.util.List;
import java.util.Map;

//extends BasicUnkeyedTable is for adapt IcebergMeta
public class TestArcticTable extends BasicUnkeyedTable {

    private BaseTable table;

    private TableIdentifier tableIdentifier;

    public TestArcticTable(BaseTable table, TableIdentifier tableIdentifier) {
        super(null, null, null, null, null);
        this.table = table;
        this.tableIdentifier = tableIdentifier;
    }

    @Override
    public TableIdentifier id() {
        return tableIdentifier;
    }

    @Override
    public void refresh() {
        table.refresh();
    }

    @Override
    public TableScan newScan() {
        return table.newScan();
    }

    @Override
    public Schema schema() {
        return table.schema();
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
    public Rollback rollback() {
        return table.rollback();
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
        return null;
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
    public TableOperations operations() {
        return table.operations();
    }
}
