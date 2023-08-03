package com.netease.arctic.hive.catalog;

import com.netease.arctic.AmsClient;
import com.netease.arctic.ams.api.CatalogMeta;
import com.netease.arctic.ams.api.TableMeta;
import com.netease.arctic.ams.api.properties.MetaTableProperties;
import com.netease.arctic.catalog.MixedTables;
import com.netease.arctic.hive.CachedHiveClientPool;
import com.netease.arctic.hive.HiveTableProperties;
import com.netease.arctic.hive.table.KeyedHiveTable;
import com.netease.arctic.hive.table.UnkeyedHiveTable;
import com.netease.arctic.hive.utils.HiveSchemaUtil;
import com.netease.arctic.hive.utils.HiveTableUtil;
import com.netease.arctic.io.ArcticFileIO;
import com.netease.arctic.io.ArcticFileIOs;
import com.netease.arctic.io.ArcticHadoopFileIO;
import com.netease.arctic.table.ChangeTable;
import com.netease.arctic.table.KeyedTable;
import com.netease.arctic.table.PrimaryKeySpec;
import com.netease.arctic.table.TableIdentifier;
import com.netease.arctic.table.TableProperties;
import com.netease.arctic.utils.CatalogUtil;
import org.apache.hadoop.hive.metastore.TableType;
import org.apache.iceberg.FileFormat;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.Table;
import org.apache.iceberg.exceptions.NoSuchTableException;
import org.apache.iceberg.mapping.MappingUtil;
import org.apache.iceberg.mapping.NameMappingParser;
import org.apache.iceberg.util.PropertyUtil;
import org.apache.thrift.TException;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MixedHiveTables extends MixedTables {

  private volatile CachedHiveClientPool hiveClientPool;

  public MixedHiveTables(CatalogMeta catalogMeta) {
    this(catalogMeta, null);
  }

  public MixedHiveTables(CatalogMeta catalogMeta, AmsClient amsClient) {
    super(catalogMeta, amsClient);
    this.hiveClientPool = new CachedHiveClientPool(getTableMetaStore(), catalogMeta.getCatalogProperties());
  }

  public CachedHiveClientPool getHiveClientPool() {
    return hiveClientPool;
  }

  @Override
  protected KeyedHiveTable loadKeyedTable(TableMeta tableMeta) {
    TableIdentifier tableIdentifier = TableIdentifier.of(tableMeta.getTableIdentifier());
    String tableLocation = checkLocation(tableMeta, MetaTableProperties.LOCATION_KEY_TABLE);
    String baseLocation = checkLocation(tableMeta, MetaTableProperties.LOCATION_KEY_BASE);
    String changeLocation = checkLocation(tableMeta, MetaTableProperties.LOCATION_KEY_CHANGE);

    ArcticHadoopFileIO fileIO = ArcticFileIOs.buildRecoverableHadoopFileIO(
        tableIdentifier, tableLocation, tableMeta.getProperties(),
        tableMetaStore, catalogMeta.getCatalogProperties());
    checkPrivilege(fileIO, baseLocation);
    Table baseIcebergTable = tableMetaStore.doAs(() -> tables.load(baseLocation));
    UnkeyedHiveTable baseTable = new KeyedHiveTable.HiveBaseInternalTable(tableIdentifier,
        CatalogUtil.useArcticTableOperations(baseIcebergTable, baseLocation, fileIO, tableMetaStore.getConfiguration()),
        fileIO, tableLocation, amsClient, hiveClientPool, catalogMeta.getCatalogProperties(), false);

    Table changeIcebergTable = tableMetaStore.doAs(() -> tables.load(changeLocation));
    ChangeTable changeTable = new KeyedHiveTable.HiveChangeInternalTable(tableIdentifier,
        CatalogUtil.useArcticTableOperations(changeIcebergTable, changeLocation, fileIO,
            tableMetaStore.getConfiguration()),
        fileIO, amsClient, catalogMeta.getCatalogProperties());
    return new KeyedHiveTable(tableMeta, tableLocation,
        buildPrimaryKeySpec(baseTable.schema(), tableMeta), amsClient, hiveClientPool, baseTable, changeTable);
  }

  /**
   * we check the privilege by calling existing method, the method will throw the UncheckedIOException Exception
   */
  private void checkPrivilege(ArcticFileIO fileIO, String fileLocation) {
    if (!fileIO.exists(fileLocation)) {
      throw new NoSuchTableException("Table's base location %s does not exist ", fileLocation);
    }
  }

  @Override
  protected UnkeyedHiveTable loadUnKeyedTable(TableMeta tableMeta) {
    TableIdentifier tableIdentifier = TableIdentifier.of(tableMeta.getTableIdentifier());
    String baseLocation = checkLocation(tableMeta, MetaTableProperties.LOCATION_KEY_BASE);
    String tableLocation = checkLocation(tableMeta, MetaTableProperties.LOCATION_KEY_TABLE);
    ArcticHadoopFileIO fileIO = ArcticFileIOs.buildRecoverableHadoopFileIO(
        tableIdentifier, tableLocation, tableMeta.getProperties(),
        tableMetaStore, catalogMeta.getCatalogProperties());
    checkPrivilege(fileIO, baseLocation);
    Table table = tableMetaStore.doAs(() -> tables.load(baseLocation));
    return new UnkeyedHiveTable(tableIdentifier, CatalogUtil.useArcticTableOperations(table, baseLocation,
        fileIO, tableMetaStore.getConfiguration()), fileIO, tableLocation, amsClient, hiveClientPool,
        catalogMeta.getCatalogProperties());
  }

  @Override
  protected KeyedTable createKeyedTable(TableMeta tableMeta, Schema schema, PrimaryKeySpec primaryKeySpec,
      PartitionSpec partitionSpec) {
    boolean allowExistedHiveTable = allowExistedHiveTable(tableMeta);
    TableIdentifier tableIdentifier = TableIdentifier.of(tableMeta.getTableIdentifier());
    String baseLocation = checkLocation(tableMeta, MetaTableProperties.LOCATION_KEY_BASE);
    String changeLocation = checkLocation(tableMeta, MetaTableProperties.LOCATION_KEY_CHANGE);
    String tableLocation = checkLocation(tableMeta, MetaTableProperties.LOCATION_KEY_TABLE);
    fillTableProperties(tableMeta);
    String hiveLocation = tableMeta.getProperties().get(HiveTableProperties.BASE_HIVE_LOCATION_ROOT);
    // default 1 day
    if (!tableMeta.properties.containsKey(TableProperties.SELF_OPTIMIZING_FULL_TRIGGER_INTERVAL)) {
      tableMeta.putToProperties(TableProperties.SELF_OPTIMIZING_FULL_TRIGGER_INTERVAL, "86400000");
    }

    ArcticHadoopFileIO fileIO = ArcticFileIOs.buildRecoverableHadoopFileIO(
        tableIdentifier, tableLocation, tableMeta.getProperties(),
        tableMetaStore, catalogMeta.getCatalogProperties());
    Table baseIcebergTable = tableMetaStore.doAs(() -> {
      try {
        Table createTable = tables.create(schema, partitionSpec, tableMeta.getProperties(), baseLocation);
        createTable.updateProperties().set(org.apache.iceberg.TableProperties.DEFAULT_NAME_MAPPING,
            NameMappingParser.toJson(MappingUtil.create(createTable.schema()))).commit();
        return createTable;
      } catch (Exception e) {
        throw new IllegalStateException("create base table failed", e);
      }
    });
    UnkeyedHiveTable baseTable = new KeyedHiveTable.HiveBaseInternalTable(tableIdentifier,
        CatalogUtil.useArcticTableOperations(baseIcebergTable, baseLocation, fileIO,
            tableMetaStore.getConfiguration()),
        fileIO, tableLocation, amsClient, hiveClientPool, catalogMeta.getCatalogProperties(), false);

    Table changeIcebergTable = tableMetaStore.doAs(() -> {
      try {
        Table createTable = tables.create(schema, partitionSpec, tableMeta.getProperties(), changeLocation);
        createTable.updateProperties().set(org.apache.iceberg.TableProperties.DEFAULT_NAME_MAPPING,
            NameMappingParser.toJson(MappingUtil.create(createTable.schema()))).commit();
        return createTable;
      } catch (Exception e) {
        throw new IllegalStateException("create change table failed", e);
      }
    });
    ChangeTable changeTable = new KeyedHiveTable.HiveChangeInternalTable(tableIdentifier,
        CatalogUtil.useArcticTableOperations(changeIcebergTable, changeLocation, fileIO,
            tableMetaStore.getConfiguration()),
        fileIO, amsClient, catalogMeta.getCatalogProperties());

    Map<String, String> metaProperties = tableMeta.getProperties();
    try {
      hiveClientPool.run(client -> {
        if (allowExistedHiveTable) {
          org.apache.hadoop.hive.metastore.api.Table hiveTable = client.getTable(tableIdentifier.getDatabase(),
              tableIdentifier.getTableName());
          Map<String, String> hiveParameters = hiveTable.getParameters();
          hiveParameters.putAll(constructProperties(primaryKeySpec, tableMeta));
          hiveTable.setParameters(hiveParameters);
          client.alterTable(tableIdentifier.getDatabase(), tableIdentifier.getTableName(), hiveTable);
        } else {
          org.apache.hadoop.hive.metastore.api.Table hiveTable = newHiveTable(tableMeta, schema, partitionSpec);
          hiveTable.setSd(HiveTableUtil.storageDescriptor(schema, partitionSpec, hiveLocation,
              FileFormat.valueOf(PropertyUtil.propertyAsString(metaProperties, TableProperties.DEFAULT_FILE_FORMAT,
                  TableProperties.DEFAULT_FILE_FORMAT_DEFAULT).toUpperCase(Locale.ENGLISH))));
          setProToHive(hiveTable, primaryKeySpec, tableMeta);
          client.createTable(hiveTable);
        }
        return null;
      });
    } catch (TException | InterruptedException e) {
      throw new RuntimeException("Failed to create hive table:" + tableMeta.getTableIdentifier(), e);
    }
    return new KeyedHiveTable(tableMeta, tableLocation,
        primaryKeySpec, amsClient, hiveClientPool, baseTable, changeTable);
  }

  @Override
  protected UnkeyedHiveTable createUnKeyedTable(TableMeta tableMeta, Schema schema, PrimaryKeySpec primaryKeySpec,
      PartitionSpec partitionSpec) {
    boolean allowExistedHiveTable = allowExistedHiveTable(tableMeta);
    TableIdentifier tableIdentifier = TableIdentifier.of(tableMeta.getTableIdentifier());
    String baseLocation = checkLocation(tableMeta, MetaTableProperties.LOCATION_KEY_BASE);
    String tableLocation = checkLocation(tableMeta, MetaTableProperties.LOCATION_KEY_TABLE);
    fillTableProperties(tableMeta);
    String hiveLocation = tableMeta.getProperties().get(HiveTableProperties.BASE_HIVE_LOCATION_ROOT);

    Table table = tableMetaStore.doAs(() -> {
      try {
        Table createTable = tables.create(schema, partitionSpec, tableMeta.getProperties(), baseLocation);
        // set name mapping using true schema
        createTable.updateProperties().set(org.apache.iceberg.TableProperties.DEFAULT_NAME_MAPPING,
            NameMappingParser.toJson(MappingUtil.create(createTable.schema()))).commit();
        return createTable;
      } catch (Exception e) {
        throw new IllegalStateException("create table failed", e);
      }
    });
    try {
      hiveClientPool.run(client -> {
        if (allowExistedHiveTable) {
          org.apache.hadoop.hive.metastore.api.Table hiveTable = client.getTable(tableIdentifier.getDatabase(),
              tableIdentifier.getTableName());
          Map<String, String> hiveParameters = hiveTable.getParameters();
          hiveParameters.putAll(constructProperties(primaryKeySpec, tableMeta));
          hiveTable.setParameters(hiveParameters);
          client.alterTable(tableIdentifier.getDatabase(), tableIdentifier.getTableName(), hiveTable);
        } else {
          org.apache.hadoop.hive.metastore.api.Table hiveTable = newHiveTable(tableMeta, schema, partitionSpec);
          hiveTable.setSd(HiveTableUtil.storageDescriptor(schema, partitionSpec, hiveLocation,
              FileFormat.valueOf(PropertyUtil.propertyAsString(tableMeta.getProperties(),
                  TableProperties.BASE_FILE_FORMAT,
                  TableProperties.BASE_FILE_FORMAT_DEFAULT).toUpperCase(Locale.ENGLISH))));
          setProToHive(hiveTable, primaryKeySpec, tableMeta);
          client.createTable(hiveTable);
        }
        return null;
      });
    } catch (TException | InterruptedException e) {
      throw new RuntimeException("Failed to create hive table:" + tableMeta.getTableIdentifier(), e);
    }

    ArcticHadoopFileIO fileIO = ArcticFileIOs.buildRecoverableHadoopFileIO(
        tableIdentifier, tableLocation, tableMeta.getProperties(),
        tableMetaStore, catalogMeta.getCatalogProperties());
    return new UnkeyedHiveTable(tableIdentifier, CatalogUtil.useArcticTableOperations(table, baseLocation, fileIO,
        tableMetaStore.getConfiguration()), fileIO, tableLocation, amsClient, hiveClientPool,
        catalogMeta.getCatalogProperties());
  }

  @Override
  public void dropTableByMeta(TableMeta tableMeta, boolean purge) {
    super.dropTableByMeta(tableMeta, purge);
    // drop hive table operation will only delete hive table metadata
    // delete data files operation will use BasicArcticCatalog
    try {
      hiveClientPool.run(client -> {
        client.dropTable(tableMeta.getTableIdentifier().getDatabase(),
            tableMeta.getTableIdentifier().getTableName(),
            false /* deleteData */,
            true /* ignoreUnknownTab */);
        return null;
      });
    } catch (TException | InterruptedException e) {
      throw new RuntimeException("Failed to drop table:" + tableMeta.getTableIdentifier(), e);
    }
  }

  protected void fillTableProperties(TableMeta meta) {
    super.fillTableProperties(meta);
    String tableLocation = checkLocation(meta, MetaTableProperties.LOCATION_KEY_TABLE);
    String hiveLocation = HiveTableUtil.hiveRootLocation(tableLocation);
    meta.putToProperties(HiveTableProperties.BASE_HIVE_LOCATION_ROOT, hiveLocation);
  }

  private Map<String, String> constructProperties(PrimaryKeySpec primaryKeySpec, TableMeta meta) {
    Map<String, String> parameters = new HashMap<>();
    parameters.put(HiveTableProperties.ARCTIC_TABLE_FLAG, "true");
    parameters.put(HiveTableProperties.ARCTIC_TABLE_PRIMARY_KEYS, primaryKeySpec.description());
    parameters.put(HiveTableProperties.ARCTIC_TABLE_ROOT_LOCATION,
            meta.getLocations().get(MetaTableProperties.LOCATION_KEY_TABLE));
    return parameters;
  }

  private org.apache.hadoop.hive.metastore.api.Table newHiveTable(TableMeta meta, Schema schema,
      PartitionSpec partitionSpec) {
    final long currentTimeMillis = System.currentTimeMillis();

    org.apache.hadoop.hive.metastore.api.Table newTable = new org.apache.hadoop.hive.metastore.api.Table(
        meta.getTableIdentifier().getTableName(),
        meta.getTableIdentifier().getDatabase(),
        meta.getProperties().getOrDefault(TableProperties.OWNER, System.getProperty("user.name")),
        (int) currentTimeMillis / 1000,
        (int) currentTimeMillis / 1000,
        Integer.MAX_VALUE,
        null,
        HiveSchemaUtil.hivePartitionFields(schema, partitionSpec),
        new HashMap<>(),
        null,
        null,
        TableType.EXTERNAL_TABLE.toString());

    newTable.getParameters().put("EXTERNAL", "TRUE"); // using the external table type also requires this
    return newTable;
  }

  private void setProToHive(org.apache.hadoop.hive.metastore.api.Table hiveTable, PrimaryKeySpec primaryKeySpec,
                            TableMeta meta) {
    Map<String, String> parameters = constructProperties(primaryKeySpec, meta);
    hiveTable.setParameters(parameters);
  }

  private boolean allowExistedHiveTable(TableMeta tableMeta) {
    String allowStringValue = tableMeta.getProperties().remove(HiveTableProperties.ALLOW_HIVE_TABLE_EXISTED);
    return Boolean.parseBoolean(allowStringValue);
  }

  @Override
  public void refreshCatalogMeta(CatalogMeta meta) {
    super.refreshCatalogMeta(meta);
    this.hiveClientPool = new CachedHiveClientPool(getTableMetaStore(), catalogMeta.getCatalogProperties());
  }
}
