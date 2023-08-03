package com.netease.arctic.catalog;

import com.netease.arctic.AmsClient;
import com.netease.arctic.ams.api.CatalogMeta;
import com.netease.arctic.ams.api.TableMeta;
import com.netease.arctic.ams.api.properties.CatalogMetaProperties;
import com.netease.arctic.ams.api.properties.MetaTableProperties;
import com.netease.arctic.io.ArcticFileIO;
import com.netease.arctic.io.ArcticFileIOs;
import com.netease.arctic.io.TableTrashManagers;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.BaseTable;
import com.netease.arctic.table.BasicKeyedTable;
import com.netease.arctic.table.BasicUnkeyedTable;
import com.netease.arctic.table.ChangeTable;
import com.netease.arctic.table.KeyedTable;
import com.netease.arctic.table.PrimaryKeySpec;
import com.netease.arctic.table.TableIdentifier;
import com.netease.arctic.table.TableMetaStore;
import com.netease.arctic.table.TableProperties;
import com.netease.arctic.table.UnkeyedTable;
import com.netease.arctic.utils.CatalogUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.Table;
import org.apache.iceberg.Tables;
import org.apache.iceberg.hadoop.HadoopTables;
import org.apache.iceberg.relocated.com.google.common.base.Preconditions;
import org.apache.iceberg.relocated.com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class MixedTables {

  private static final Logger LOG = LoggerFactory.getLogger(MixedTables.class);

  protected CatalogMeta catalogMeta;
  protected Tables tables;
  protected TableMetaStore tableMetaStore;

  /**
   * @deprecated since 0.5.0, will be removed in 0.6.0;
   */
  @Deprecated
  protected AmsClient amsClient;

  public MixedTables(CatalogMeta catalogMeta) {
    this(catalogMeta, null);
  }

  public MixedTables(CatalogMeta catalogMeta, AmsClient amsClient) {
    initialize(catalogMeta);
    this.amsClient = amsClient;
  }

  private void initialize(CatalogMeta meta) {
    this.catalogMeta = meta;
    if (meta.getStorageConfigs() != null &&
        CatalogMetaProperties.STORAGE_CONFIGS_VALUE_TYPE_HDFS.equalsIgnoreCase(
            meta.getStorageConfigs().get(CatalogMetaProperties.STORAGE_CONFIGS_KEY_TYPE))) {
      if (!meta.getStorageConfigs().containsKey(CatalogMetaProperties.STORAGE_CONFIGS_KEY_HDFS_SITE)) {
        throw new IllegalStateException("lack hdfs.site config");
      }
      if (!meta.getStorageConfigs().containsKey(CatalogMetaProperties.STORAGE_CONFIGS_KEY_CORE_SITE)) {
        throw new IllegalStateException("lack core.site config");
      }
    }
    this.tableMetaStore = CatalogUtil.buildMetaStore(meta);
    this.tables = new HadoopTables(tableMetaStore.getConfiguration());
  }

  public ArcticTable loadTableByMeta(TableMeta tableMeta) {
    if (tableMeta.getKeySpec() != null &&
        tableMeta.getKeySpec().getFields() != null &&
        tableMeta.getKeySpec().getFields().size() > 0) {
      return loadKeyedTable(tableMeta);
    } else {
      return loadUnKeyedTable(tableMeta);
    }
  }

  protected KeyedTable loadKeyedTable(TableMeta tableMeta) {
    TableIdentifier tableIdentifier = TableIdentifier.of(tableMeta.getTableIdentifier());
    String tableLocation = checkLocation(tableMeta, MetaTableProperties.LOCATION_KEY_TABLE);
    String baseLocation = checkLocation(tableMeta, MetaTableProperties.LOCATION_KEY_BASE);
    String changeLocation = checkLocation(tableMeta, MetaTableProperties.LOCATION_KEY_CHANGE);

    ArcticFileIO fileIO = ArcticFileIOs.buildRecoverableHadoopFileIO(
        tableIdentifier, tableLocation, tableMeta.getProperties(),
        tableMetaStore, catalogMeta.getCatalogProperties());
    Table baseIcebergTable = tableMetaStore.doAs(() -> tables.load(baseLocation));
    BaseTable baseTable = new BasicKeyedTable.BaseInternalTable(tableIdentifier,
        CatalogUtil.useArcticTableOperations(
            baseIcebergTable, baseLocation, fileIO, tableMetaStore.getConfiguration()),
        fileIO, amsClient, catalogMeta.getCatalogProperties());

    Table changeIcebergTable = tableMetaStore.doAs(() -> tables.load(changeLocation));
    ChangeTable changeTable = new BasicKeyedTable.ChangeInternalTable(tableIdentifier,
        CatalogUtil.useArcticTableOperations(changeIcebergTable, changeLocation, fileIO,
            tableMetaStore.getConfiguration()),
        fileIO, amsClient, catalogMeta.getCatalogProperties());
    return new BasicKeyedTable(tableMeta, tableLocation,
        buildPrimaryKeySpec(baseTable.schema(), tableMeta), amsClient, baseTable, changeTable);
  }

  protected String checkLocation(TableMeta meta, String locationKey) {
    String location = meta.getLocations().get(locationKey);
    Preconditions.checkArgument(StringUtils.isNotBlank(location), "table location can't found");
    return location;
  }

  protected PrimaryKeySpec buildPrimaryKeySpec(Schema schema, TableMeta tableMeta) {
    PrimaryKeySpec.Builder builder = PrimaryKeySpec.builderFor(schema);
    if (tableMeta.getKeySpec() != null &&
        tableMeta.getKeySpec().getFields() != null &&
        tableMeta.getKeySpec().getFields().size() > 0) {
      for (String field : tableMeta.getKeySpec().getFields()) {
        builder.addColumn(field);
      }
    }
    return builder.build();
  }

  protected UnkeyedTable loadUnKeyedTable(TableMeta tableMeta) {
    TableIdentifier tableIdentifier = TableIdentifier.of(tableMeta.getTableIdentifier());
    String tableLocation = checkLocation(tableMeta, MetaTableProperties.LOCATION_KEY_TABLE);
    String baseLocation = checkLocation(tableMeta, MetaTableProperties.LOCATION_KEY_BASE);
    Table table = tableMetaStore.doAs(() -> tables.load(baseLocation));

    ArcticFileIO fileIO = ArcticFileIOs.buildRecoverableHadoopFileIO(
        tableIdentifier, tableLocation, tableMeta.getProperties(),
        tableMetaStore, catalogMeta.getCatalogProperties());
    return new BasicUnkeyedTable(tableIdentifier, CatalogUtil.useArcticTableOperations(table, baseLocation,
        fileIO, tableMetaStore.getConfiguration()), fileIO, amsClient, catalogMeta.getCatalogProperties());
  }

  public ArcticTable createTableByMeta(TableMeta tableMeta, Schema schema, PrimaryKeySpec primaryKeySpec,
                                       PartitionSpec partitionSpec) {
    if (primaryKeySpec.primaryKeyExisted()) {
      return createKeyedTable(tableMeta, schema, primaryKeySpec, partitionSpec);
    } else {
      return createUnKeyedTable(tableMeta, schema, primaryKeySpec, partitionSpec);
    }
  }

  protected KeyedTable createKeyedTable(TableMeta tableMeta, Schema schema, PrimaryKeySpec primaryKeySpec,
                                        PartitionSpec partitionSpec) {
    TableIdentifier tableIdentifier = TableIdentifier.of(tableMeta.getTableIdentifier());
    String tableLocation = checkLocation(tableMeta, MetaTableProperties.LOCATION_KEY_TABLE);
    String baseLocation = checkLocation(tableMeta, MetaTableProperties.LOCATION_KEY_BASE);
    String changeLocation = checkLocation(tableMeta, MetaTableProperties.LOCATION_KEY_CHANGE);

    fillTableProperties(tableMeta);
    ArcticFileIO fileIO = ArcticFileIOs.buildRecoverableHadoopFileIO(
        tableIdentifier, tableLocation, tableMeta.getProperties(),
        tableMetaStore, catalogMeta.getCatalogProperties());
    Table baseIcebergTable = tableMetaStore.doAs(() -> {
      try {
        return tables.create(schema, partitionSpec, tableMeta.getProperties(), baseLocation);
      } catch (Exception e) {
        throw new IllegalStateException("create base table failed", e);
      }
    });

    BaseTable baseTable = new BasicKeyedTable.BaseInternalTable(tableIdentifier,
        CatalogUtil.useArcticTableOperations(baseIcebergTable, baseLocation, fileIO,
            tableMetaStore.getConfiguration()),
        fileIO, amsClient, catalogMeta.getCatalogProperties());

    Table changeIcebergTable = tableMetaStore.doAs(() -> {
      try {
        return tables.create(schema, partitionSpec, tableMeta.getProperties(), changeLocation);
      } catch (Exception e) {
        throw new IllegalStateException("create change table failed", e);
      }
    });
    ChangeTable changeTable = new BasicKeyedTable.ChangeInternalTable(tableIdentifier,
        CatalogUtil.useArcticTableOperations(changeIcebergTable, changeLocation, fileIO,
            tableMetaStore.getConfiguration()),
        fileIO, amsClient, catalogMeta.getCatalogProperties());
    return new BasicKeyedTable(tableMeta, tableLocation,
        primaryKeySpec, amsClient, baseTable, changeTable);
  }

  protected void fillTableProperties(TableMeta tableMeta) {
    tableMeta.putToProperties(TableProperties.TABLE_CREATE_TIME, String.valueOf(System.currentTimeMillis()));
    tableMeta.putToProperties(org.apache.iceberg.TableProperties.FORMAT_VERSION, "2");
    tableMeta.putToProperties(org.apache.iceberg.TableProperties.METADATA_DELETE_AFTER_COMMIT_ENABLED, "true");
    tableMeta.putToProperties("flink.max-continuous-empty-commits", String.valueOf(Integer.MAX_VALUE));
  }

  protected UnkeyedTable createUnKeyedTable(TableMeta tableMeta, Schema schema, PrimaryKeySpec primaryKeySpec,
                                            PartitionSpec partitionSpec) {
    TableIdentifier tableIdentifier = TableIdentifier.of(tableMeta.getTableIdentifier());
    String tableLocation = checkLocation(tableMeta, MetaTableProperties.LOCATION_KEY_TABLE);
    String baseLocation = checkLocation(tableMeta, MetaTableProperties.LOCATION_KEY_BASE);

    fillTableProperties(tableMeta);
    Table table = tableMetaStore.doAs(() -> {
      try {
        return tables.create(schema, partitionSpec, tableMeta.getProperties(), baseLocation);
      } catch (Exception e) {
        throw new IllegalStateException("create table failed", e);
      }
    });
    ArcticFileIO fileIO = ArcticFileIOs.buildRecoverableHadoopFileIO(
        tableIdentifier, tableLocation, tableMeta.getProperties(),
        tableMetaStore, catalogMeta.getCatalogProperties());
    return new BasicUnkeyedTable(tableIdentifier, CatalogUtil.useArcticTableOperations(table, baseLocation, fileIO,
        tableMetaStore.getConfiguration()), fileIO, amsClient, catalogMeta.getCatalogProperties());
  }

  public void dropTableByMeta(TableMeta tableMeta, boolean purge) {
    try {
      ArcticFileIO fileIO = ArcticFileIOs.buildHadoopFileIO(tableMetaStore);
      Map<String, String> tableProperties = Maps.newHashMap();
      try {
        ArcticTable arcticTable = loadTableByMeta(tableMeta);
        tableProperties.putAll(arcticTable.properties());
      } catch (Exception loadException) {
        LOG.warn("load table failed when dropping table", loadException);
      }

      // If purge is true, all manifest/data files must be located under the table directory.
      if (!purge) {
        String baseLocation = tableMeta.getLocations().get(MetaTableProperties.LOCATION_KEY_BASE);
        String changeLocation = tableMeta.getLocations().get(MetaTableProperties.LOCATION_KEY_CHANGE);
        try {
          if (StringUtils.isNotBlank(baseLocation)) {
            dropInternalTable(tableMetaStore, baseLocation, purge);
          }
          if (StringUtils.isNotBlank(changeLocation)) {
            dropInternalTable(tableMetaStore, changeLocation, purge);
          }
        } catch (Exception e) {
          LOG.warn("drop base/change iceberg table fail ", e);
        }
      } else {
        String tableLocation = tableMeta.getLocations().get(MetaTableProperties.LOCATION_KEY_TABLE);
        if (fileIO.exists(tableLocation)) {
          LOG.info("try to delete table directory location is " + tableLocation);
          fileIO.asPrefixFileIO().deletePrefix(tableLocation);
        }
      }

      // delete custom trash location
      String customTrashLocation = tableProperties.get(TableProperties.TABLE_TRASH_CUSTOM_ROOT_LOCATION);
      if (customTrashLocation != null) {
        TableIdentifier tableId = TableIdentifier.of(tableMeta.getTableIdentifier());
        String trashParentLocation = TableTrashManagers.getTrashParentLocation(tableId, customTrashLocation);
        if (fileIO.exists(trashParentLocation)) {
          fileIO.asPrefixFileIO().deletePrefix(trashParentLocation);
        }
      }
    } catch (Exception e) {
      LOG.warn("drop table directory fail ", e);
    }
  }

  private void dropInternalTable(TableMetaStore tableMetaStore, String internalTableLocation, boolean purge) {
    final HadoopTables internalTables = new HadoopTables(tableMetaStore.getConfiguration());
    tableMetaStore.doAs(() -> {
      internalTables.dropTable(internalTableLocation, purge);
      return null;
    });
  }

  public void refreshCatalogMeta(CatalogMeta meta) {
    initialize(meta);
  }

  protected TableMetaStore getTableMetaStore() {
    return tableMetaStore;
  }
}
