/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.planner.logical;

import java.io.IOException;
import java.util.Objects;

import org.apache.calcite.config.CalciteConnectionConfig;
import org.apache.calcite.plan.RelOptTable;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.schema.Schema.TableType;
import org.apache.calcite.schema.Statistic;
import org.apache.calcite.schema.Statistics;
import org.apache.calcite.schema.Table;
import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlNode;
import org.apache.drill.common.JSONOptions;
import org.apache.drill.common.logical.StoragePluginConfig;
import org.apache.drill.exec.metastore.store.FileSystemMetadataProviderManager;
import org.apache.drill.exec.metastore.MetadataProviderManager;
import org.apache.drill.metastore.metadata.TableMetadataProvider;
import org.apache.drill.exec.physical.base.SchemalessScan;
import org.apache.drill.exec.physical.base.GroupScan;
import org.apache.drill.exec.server.options.SessionOptionManager;
import org.apache.drill.exec.store.StoragePlugin;
import org.apache.drill.exec.store.dfs.FileSelection;
import org.apache.drill.exec.util.ImpersonationUtil;

public abstract class DrillTable implements Table {

  private final String storageEngineName;
  private final StoragePluginConfig storageEngineConfig;
  private final TableType tableType;
  private final DrillTableSelection selection;
  private final StoragePlugin plugin;
  private final String userName;
  private GroupScan scan;
  private SessionOptionManager options;
  private MetadataProviderManager metadataProviderManager;

  /**
   * Creates a DrillTable instance for a @{code TableType#Table} table.
   * @param storageEngineName StorageEngine name.
   * @param plugin Reference to StoragePlugin.
   * @param userName Whom to impersonate while reading the contents of the table.
   * @param selection Table contents (type and contents depend on type of StoragePlugin).
   */
  public DrillTable(String storageEngineName, StoragePlugin plugin, String userName, DrillTableSelection selection) {
    this(storageEngineName, plugin, TableType.TABLE, userName, selection);
  }

  /**
   * Creates a DrillTable instance.
   * @param storageEngineName StorageEngine name.
   * @param plugin Reference to StoragePlugin.
   * @param tableType the JDBC table type
   * @param userName Whom to impersonate while reading the contents of the table.
   * @param selection Table contents (type and contents depend on type of StoragePlugin).
   */
  public DrillTable(String storageEngineName, StoragePlugin plugin, TableType tableType, String userName, DrillTableSelection selection) {
    this(storageEngineName, plugin, tableType, userName, selection, null);
  }

  public DrillTable(String storageEngineName, StoragePlugin plugin, TableType tableType,
                    String userName, DrillTableSelection selection, MetadataProviderManager metadataProviderManager) {
    this.selection = selection;
    this.plugin = plugin;

    this.tableType = tableType;

    this.storageEngineConfig = plugin.getConfig();
    this.storageEngineName = storageEngineName;
    this.userName = userName;
    this.metadataProviderManager = metadataProviderManager;
  }

  /**
   * TODO: Same purpose as other constructor except the impersonation user is the user who is running the Drillbit
   * process. Once we add impersonation to non-FileSystem storage plugins such as Hive, HBase etc,
   * we can remove this constructor.
   */
  public DrillTable(String storageEngineName, StoragePlugin plugin, DrillTableSelection selection) {
    this(storageEngineName, plugin, ImpersonationUtil.getProcessUserName(), selection);
  }

  public void setOptions(SessionOptionManager options) {
    this.options = options;
  }

  public void setGroupScan(GroupScan scan) {
    this.scan = scan;
  }

  public void setTableMetadataProviderManager(MetadataProviderManager metadataProviderManager) {
    this.metadataProviderManager = metadataProviderManager;
  }

  public GroupScan getGroupScan() throws IOException {
    if (scan == null) {
      if (selection instanceof FileSelection && ((FileSelection) selection).isEmptyDirectory()) {
        this.scan = new SchemalessScan(userName, ((FileSelection) selection).getSelectionRoot());
      } else {
        this.scan = plugin.getPhysicalScan(userName, new JSONOptions(selection), options, metadataProviderManager);
      }
    }
    return scan;
  }

  /**
   * Returns manager for {@link TableMetadataProvider} which may provide null for the case when scan wasn't created.
   * This method should be used only for the case when it is possible to obtain {@link TableMetadataProvider} when supplier returns null
   * or {@link TableMetadataProvider} usage may be omitted.
   *
   * @return supplier for {@link TableMetadataProvider}
   */
  public MetadataProviderManager getMetadataProviderManager() {
    if (metadataProviderManager == null) {
      // for the case when scan wasn't initialized, return null to avoid reading data which may be pruned in future
      metadataProviderManager = FileSystemMetadataProviderManager.init();
      if (scan != null) {
        metadataProviderManager.setTableMetadataProvider(scan.getMetadataProvider());
      }
    }
    return metadataProviderManager;
  }

  public StoragePluginConfig getStorageEngineConfig() {
    return storageEngineConfig;
  }

  public StoragePlugin getPlugin() {
    return plugin;
  }

  public Object getSelection() {
    return selection;
  }

  public String getStorageEngineName() {
    return storageEngineName;
  }

  public String getUserName() {
    return userName;
  }

  @Override
  public Statistic getStatistic() {
    return Statistics.UNKNOWN;
  }

  public RelNode toRel(RelOptTable.ToRelContext context, RelOptTable table) {
    // Returns non-drill table scan to allow directory-based partition pruning
    // before table group scan is created.
    return SelectionBasedTableScan.create(context.getCluster(), table, selection.digest());
  }

  @Override
  public TableType getJdbcTableType() {
    return tableType;
  }

  @Override
  public boolean rolledUpColumnValidInsideAgg(String column,
      SqlCall call, SqlNode parent, CalciteConnectionConfig config) {
    return true;
  }

  @Override
  public boolean isRolledUp(String column) {
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(selection, storageEngineConfig, storageEngineName, userName);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    DrillTable other = (DrillTable) obj;
    return Objects.equals(selection, other.selection) &&
           Objects.equals(storageEngineConfig, other.storageEngineConfig) &&
           Objects.equals(storageEngineName, other.storageEngineName) &&
           Objects.equals(userName, other.userName);
  }
}
