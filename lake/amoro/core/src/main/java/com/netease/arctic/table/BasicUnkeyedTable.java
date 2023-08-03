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

package com.netease.arctic.table;

import com.netease.arctic.AmsClient;
import com.netease.arctic.ams.api.TableFormat;
import com.netease.arctic.io.ArcticFileIO;
import com.netease.arctic.op.PartitionPropertiesUpdate;
import com.netease.arctic.op.UpdatePartitionProperties;
import com.netease.arctic.trace.AmsTableTracer;
import com.netease.arctic.trace.ArcticAppendFiles;
import com.netease.arctic.trace.ArcticDeleteFiles;
import com.netease.arctic.trace.ArcticOverwriteFiles;
import com.netease.arctic.trace.ArcticReplacePartitions;
import com.netease.arctic.trace.ArcticRewriteFiles;
import com.netease.arctic.trace.ArcticRowDelta;
import com.netease.arctic.trace.ArcticTransaction;
import com.netease.arctic.trace.TraceOperations;
import com.netease.arctic.trace.TracedSchemaUpdate;
import com.netease.arctic.trace.TracedUpdateProperties;
import com.netease.arctic.utils.CatalogUtil;
import com.netease.arctic.utils.TablePropertyUtil;
import org.apache.iceberg.AppendFiles;
import org.apache.iceberg.DeleteFiles;
import org.apache.iceberg.ExpireSnapshots;
import org.apache.iceberg.HasTableOperations;
import org.apache.iceberg.HistoryEntry;
import org.apache.iceberg.IncrementalAppendScan;
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
import org.apache.iceberg.TableOperations;
import org.apache.iceberg.TableScan;
import org.apache.iceberg.Transaction;
import org.apache.iceberg.UpdateLocation;
import org.apache.iceberg.UpdatePartitionSpec;
import org.apache.iceberg.UpdateProperties;
import org.apache.iceberg.UpdateSchema;
import org.apache.iceberg.encryption.EncryptionManager;
import org.apache.iceberg.io.LocationProvider;
import org.apache.iceberg.util.StructLikeMap;

import java.util.List;
import java.util.Map;

/**
 * Basic implementation of {@link UnkeyedTable}, wrapping a {@link Table}.
 */
public class BasicUnkeyedTable implements UnkeyedTable, HasTableOperations {

  private final Map<String, String> catalogProperties;
  private final TableIdentifier tableIdentifier;
  protected final Table icebergTable;
  protected final ArcticFileIO arcticFileIO;

  /**
   * @deprecated since 0.5.0, will be removed in 0.6.0;
   */
  @Deprecated
  private final AmsClient client;

  public BasicUnkeyedTable(
      TableIdentifier tableIdentifier, Table icebergTable, ArcticFileIO arcticFileIO,
      AmsClient client, Map<String, String> catalogProperties) {
    this.tableIdentifier = tableIdentifier;
    this.icebergTable = icebergTable;
    this.arcticFileIO = arcticFileIO;
    this.client = client;
    this.catalogProperties = catalogProperties;
  }

  @Override
  public void refresh() {
    icebergTable.refresh();
  }

  @Override
  public TableScan newScan() {
    return icebergTable.newScan();
  }

  @Override
  public IncrementalAppendScan newIncrementalAppendScan() {
    return icebergTable.newIncrementalAppendScan();
  }

  @Override
  public TableIdentifier id() {
    return tableIdentifier;
  }

  @Override
  public TableFormat format() {
    return TableFormat.MIXED_ICEBERG;
  }

  @Override
  public Schema schema() {
    return icebergTable.schema();
  }

  @Override
  public Map<Integer, Schema> schemas() {
    return icebergTable.schemas();
  }

  @Override
  public PartitionSpec spec() {
    return icebergTable.spec();
  }

  @Override
  public Map<Integer, PartitionSpec> specs() {
    return icebergTable.specs();
  }

  @Override
  public SortOrder sortOrder() {
    return icebergTable.sortOrder();
  }

  @Override
  public Map<Integer, SortOrder> sortOrders() {
    return icebergTable.sortOrders();
  }

  @Override
  public Map<String, String> properties() {
    if (catalogProperties == null) {
      return icebergTable.properties();
    } else {
      return CatalogUtil.mergeCatalogPropertiesToTable(icebergTable.properties(), catalogProperties);
    }
  }

  @Override
  public String location() {
    return icebergTable.location();
  }

  @Override
  public Snapshot currentSnapshot() {
    return icebergTable.currentSnapshot();
  }

  @Override
  public Snapshot snapshot(long snapshotId) {
    return icebergTable.snapshot(snapshotId);
  }

  @Override
  public Iterable<Snapshot> snapshots() {
    return icebergTable.snapshots();
  }

  @Override
  public List<HistoryEntry> history() {
    return icebergTable.history();
  }

  @Override
  public UpdateSchema updateSchema() {
    if (client != null) {
      return new TracedSchemaUpdate(icebergTable.updateSchema(),
          new AmsTableTracer(this, TraceOperations.UPDATE_SCHEMA, client, false));
    } else {
      return icebergTable.updateSchema();
    }
  }

  @Override
  public UpdatePartitionSpec updateSpec() {
    return icebergTable.updateSpec();
  }

  @Override
  public UpdateProperties updateProperties() {
    UpdateProperties updateProperties = icebergTable.updateProperties();
    if (client != null) {
      AmsTableTracer tracer = new AmsTableTracer(this, TraceOperations.UPDATE_PROPERTIES, client, false);
      return new TracedUpdateProperties(updateProperties, tracer);
    } else {
      return updateProperties;
    }
  }

  @Override
  public ReplaceSortOrder replaceSortOrder() {
    return icebergTable.replaceSortOrder();
  }

  @Override
  public UpdateLocation updateLocation() {
    return icebergTable.updateLocation();
  }

  @Override
  public AppendFiles newAppend() {
    return ArcticAppendFiles.buildFor(this, false)
        .traceTable(client, this)
        .onTableStore(icebergTable)
        .build();
  }

  @Override
  public AppendFiles newFastAppend() {
    return ArcticAppendFiles.buildFor(this, true)
        .traceTable(client, this)
        .onTableStore(icebergTable)
        .build();
  }

  @Override
  public RewriteFiles newRewrite() {
    return ArcticRewriteFiles.buildFor(this)
        .traceTable(client, this)
        .onTableStore(icebergTable)
        .build();
  }

  @Override
  public RewriteManifests rewriteManifests() {
    return icebergTable.rewriteManifests();
  }

  @Override
  public OverwriteFiles newOverwrite() {
    return ArcticOverwriteFiles.buildFor(this)
        .traceTable(client, this).onTableStore(icebergTable).build();
  }

  @Override
  public RowDelta newRowDelta() {
    return ArcticRowDelta.buildFor(this)
        .traceTable(client, this).onTableStore(icebergTable).build();
  }

  @Override
  public ReplacePartitions newReplacePartitions() {
    return ArcticReplacePartitions.buildFor(this)
        .traceTable(client, this).onTableStore(icebergTable).build();
  }

  @Override
  public DeleteFiles newDelete() {
    return ArcticDeleteFiles.buildFor(this)
        .traceTable(client, this)
        .onTableStore(icebergTable)
        .build();
  }

  @Override
  public ExpireSnapshots expireSnapshots() {
    return icebergTable.expireSnapshots();
  }

  @Override
  public ManageSnapshots manageSnapshots() {
    return icebergTable.manageSnapshots();
  }

  @Override
  public Transaction newTransaction() {
    Transaction transaction = icebergTable.newTransaction();
    AmsTableTracer tableTracer = null;
    if (client != null) {
      tableTracer = new AmsTableTracer(this, client, false);
    }
    return new ArcticTransaction(this, transaction, tableTracer);
  }

  @Override
  public ArcticFileIO io() {
    return arcticFileIO;
  }

  @Override
  public EncryptionManager encryption() {
    return icebergTable.encryption();
  }

  @Override
  public LocationProvider locationProvider() {
    return icebergTable.locationProvider();
  }

  @Override
  public List<StatisticsFile> statisticsFiles() {
    return icebergTable.statisticsFiles();
  }

  @Override
  public Map<String, SnapshotRef> refs() {
    return icebergTable.refs();
  }

  @Override
  public TableOperations operations() {
    if (icebergTable instanceof HasTableOperations) {
      return ((HasTableOperations) icebergTable).operations();
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public StructLikeMap<Map<String, String>> partitionProperty() {
    String s = icebergTable.properties().get(TableProperties.TABLE_PARTITION_PROPERTIES);
    if (s != null) {
      return TablePropertyUtil.decodePartitionProperties(spec(), s);
    } else {
      return StructLikeMap.create(spec().partitionType());
    }
  }

  @Override
  public UpdatePartitionProperties updatePartitionProperties(Transaction transaction) {
    return new PartitionPropertiesUpdate(this, transaction);
  }
}
