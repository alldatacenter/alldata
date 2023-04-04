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

package com.netease.arctic.hive.catalog;

import com.netease.arctic.AmsClient;
import com.netease.arctic.ams.api.CatalogMeta;
import com.netease.arctic.ams.api.TableMeta;
import com.netease.arctic.ams.api.properties.MetaTableProperties;
import com.netease.arctic.catalog.BasicArcticCatalog;
import com.netease.arctic.hive.CachedHiveClientPool;
import com.netease.arctic.hive.HMSClient;
import com.netease.arctic.hive.HMSClientPool;
import com.netease.arctic.hive.HiveTableProperties;
import com.netease.arctic.hive.table.KeyedHiveTable;
import com.netease.arctic.hive.table.UnkeyedHiveTable;
import com.netease.arctic.hive.utils.HiveSchemaUtil;
import com.netease.arctic.hive.utils.HiveTableUtil;
import com.netease.arctic.io.ArcticFileIO;
import com.netease.arctic.io.ArcticFileIOs;
import com.netease.arctic.table.ChangeTable;
import com.netease.arctic.table.PrimaryKeySpec;
import com.netease.arctic.table.TableBuilder;
import com.netease.arctic.table.TableIdentifier;
import com.netease.arctic.table.TableProperties;
import com.netease.arctic.utils.CatalogUtil;
import org.apache.hadoop.hive.metastore.TableType;
import org.apache.hadoop.hive.metastore.api.AlreadyExistsException;
import org.apache.hadoop.hive.metastore.api.Database;
import org.apache.iceberg.FileFormat;
import org.apache.iceberg.IcebergSchemaUtil;
import org.apache.iceberg.PartitionField;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.SortOrder;
import org.apache.iceberg.Table;
import org.apache.iceberg.mapping.MappingUtil;
import org.apache.iceberg.mapping.NameMappingParser;
import org.apache.iceberg.relocated.com.google.common.base.Preconditions;
import org.apache.iceberg.util.PropertyUtil;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Implementation of {@link com.netease.arctic.catalog.ArcticCatalog} to support Hive table as base store.
 */
public class ArcticHiveCatalog extends BasicArcticCatalog {

  private static final Logger LOG = LoggerFactory.getLogger(ArcticHiveCatalog.class);

  private CachedHiveClientPool hiveClientPool;

  @Override
  public void initialize(
      AmsClient client, CatalogMeta meta, Map<String, String> properties) {
    super.initialize(client, meta, properties);
    this.hiveClientPool = new CachedHiveClientPool(tableMetaStore, properties);
  }

  @Override
  public List<String> listDatabases() {
    try {
      return hiveClientPool.run(HMSClient::getAllDatabases);
    } catch (TException | InterruptedException e) {
      throw new RuntimeException("Failed to list databases", e);
    }
  }

  @Override
  public void createDatabase(String databaseName) {
    try {
      hiveClientPool.run(client -> {
        Database database = new Database();
        database.setName(databaseName);
        client.createDatabase(database);
        return null;
      });
    } catch (AlreadyExistsException e) {
      throw new org.apache.iceberg.exceptions.AlreadyExistsException(
          e, "Database '%s' already exists!", databaseName);

    }  catch (TException | InterruptedException e) {
      throw new RuntimeException("Failed to create database:" + databaseName, e);
    }
  }

  /**
   * HMS is case-insensitive for table name and database
   */
  @Override
  protected TableMeta getArcticTableMeta(TableIdentifier identifier) {
    return super.getArcticTableMeta(identifier.toLowCaseIdentifier());
  }

  @Override
  public void dropDatabase(String databaseName) {
    try {
      hiveClientPool.run(client -> {
        client.dropDatabase(databaseName,
            false /* deleteData */,
            false /* ignoreUnknownDb */,
            false /* cascade */);
        return null;
      });
    } catch (TException | InterruptedException e) {
      throw new RuntimeException("Failed to drop database:" + databaseName, e);
    }
  }

  @Override
  protected void doDropTable(TableMeta meta, boolean purge) {
    // drop hive table operation will only delete hive table metadata
    // delete data files operation will use BasicArcticCatalog
    try {
      hiveClientPool.run(client -> {
        client.dropTable(meta.getTableIdentifier().getDatabase(),
            meta.getTableIdentifier().getTableName(),
            false /* deleteData */,
            false /* ignoreUnknownTab */);
        return null;
      });
    } catch (TException | InterruptedException e) {
      throw new RuntimeException("Failed to drop table:" + meta.getTableIdentifier(), e);
    }
    super.doDropTable(meta, purge);
  }

  public void dropTableButNotDropHiveTable(TableIdentifier tableIdentifier) {
    TableMeta meta = getArcticTableMeta(tableIdentifier);
    super.doDropTable(meta, false);
  }

  @Override
  public TableBuilder newTableBuilder(
      TableIdentifier identifier, Schema schema) {
    return new ArcticHiveTableBuilder(identifier, schema);
  }

  @Override
  protected KeyedHiveTable loadKeyedTable(TableMeta tableMeta) {
    TableIdentifier tableIdentifier = TableIdentifier.of(tableMeta.getTableIdentifier());
    String tableLocation = checkLocation(tableMeta, MetaTableProperties.LOCATION_KEY_TABLE);
    String baseLocation = checkLocation(tableMeta, MetaTableProperties.LOCATION_KEY_BASE);
    String changeLocation = checkLocation(tableMeta, MetaTableProperties.LOCATION_KEY_CHANGE);

    ArcticFileIO fileIO = ArcticFileIOs.buildTableFileIO(tableIdentifier, tableLocation, tableMeta.getProperties(),
        tableMetaStore, catalogMeta.getCatalogProperties());
    Table baseIcebergTable = tableMetaStore.doAs(() -> tables.load(baseLocation));
    UnkeyedHiveTable baseTable = new KeyedHiveTable.HiveBaseInternalTable(tableIdentifier,
        CatalogUtil.useArcticTableOperations(baseIcebergTable, baseLocation, fileIO, tableMetaStore.getConfiguration()),
        fileIO, tableLocation, client, hiveClientPool, catalogMeta.getCatalogProperties(), false);

    Table changeIcebergTable = tableMetaStore.doAs(() -> tables.load(changeLocation));
    ChangeTable changeTable = new KeyedHiveTable.HiveChangeInternalTable(tableIdentifier,
        CatalogUtil.useArcticTableOperations(changeIcebergTable, changeLocation, fileIO,
            tableMetaStore.getConfiguration()),
        fileIO, client, catalogMeta.getCatalogProperties());
    return new KeyedHiveTable(tableMeta, tableLocation,
        buildPrimaryKeySpec(baseTable.schema(), tableMeta), client, hiveClientPool, baseTable, changeTable);
  }

  @Override
  protected UnkeyedHiveTable loadUnKeyedTable(TableMeta tableMeta) {
    TableIdentifier tableIdentifier = TableIdentifier.of(tableMeta.getTableIdentifier());
    String baseLocation = checkLocation(tableMeta, MetaTableProperties.LOCATION_KEY_BASE);
    String tableLocation = checkLocation(tableMeta, MetaTableProperties.LOCATION_KEY_TABLE);
    Table table = tableMetaStore.doAs(() -> tables.load(baseLocation));

    ArcticFileIO fileIO = ArcticFileIOs.buildTableFileIO(tableIdentifier, tableLocation, tableMeta.getProperties(),
        tableMetaStore, catalogMeta.getCatalogProperties());
    return new UnkeyedHiveTable(tableIdentifier, CatalogUtil.useArcticTableOperations(table, baseLocation,
        fileIO, tableMetaStore.getConfiguration()), fileIO, tableLocation, client, hiveClientPool,
        catalogMeta.getCatalogProperties());
  }

  public HMSClientPool getHMSClient() {
    return hiveClientPool;
  }


  class ArcticHiveTableBuilder extends ArcticTableBuilder {

    public ArcticHiveTableBuilder(TableIdentifier identifier, Schema schema) {
      super(identifier.toLowCaseIdentifier(), HiveSchemaUtil.changeFieldNameToLowercase(schema));
    }

    boolean allowExistedHiveTable = false;

    @Override
    public TableBuilder withPartitionSpec(PartitionSpec partitionSpec) {
      return super.withPartitionSpec(IcebergSchemaUtil.copyPartitionSpec(partitionSpec, schema));
    }

    @Override
    public TableBuilder withSortOrder(SortOrder sortOrder) {
      return super.withSortOrder(IcebergSchemaUtil.copySortOrderSpec(sortOrder, schema));
    }

    @Override
    public TableBuilder withPrimaryKeySpec(PrimaryKeySpec primaryKeySpec) {
      PrimaryKeySpec.Builder builder = PrimaryKeySpec.builderFor(schema);
      primaryKeySpec.fields().forEach(primaryKeyField -> builder.addColumn(primaryKeyField.fieldName().toLowerCase(
          Locale.ROOT)));
      return super.withPrimaryKeySpec(builder.build());
    }

    @Override
    public TableBuilder withProperty(String key, String value) {
      if (key.equals(HiveTableProperties.ALLOW_HIVE_TABLE_EXISTED) && value.equals("true")) {
        allowExistedHiveTable = true;
      } else if (key.equals(TableProperties.TABLE_EVENT_TIME_FIELD)) {
        super.withProperty(key, value.toLowerCase(Locale.ROOT));
      } else {
        super.withProperty(key, value);
      }
      return this;
    }

    @Override
    public TableBuilder withProperties(Map<String, String> properties) {
      properties.forEach(this::withProperty);
      return this;
    }

    @Override
    protected void doCreateCheck() {

      super.doCreateCheck();
      try {
        if (allowExistedHiveTable) {
          LOG.info("No need to check hive table exist");
        } else {
          org.apache.hadoop.hive.metastore.api.Table hiveTable =
              hiveClientPool.run(client -> client.getTable(
                  identifier.getDatabase(),
                  identifier.getTableName()));
          if (hiveTable != null) {
            throw new IllegalArgumentException("Table is already existed in hive meta store:" + identifier);
          }
        }
      } catch (org.apache.hadoop.hive.metastore.api.NoSuchObjectException noSuchObjectException) {
        // ignore this exception
      } catch (TException | InterruptedException e) {
        throw new RuntimeException("Failed to check table exist:" + identifier, e);
      }
      if (!partitionSpec.isUnpartitioned()) {
        for (PartitionField partitionField : partitionSpec.fields()) {
          if (!partitionField.transform().isIdentity()) {
            throw new IllegalArgumentException("Unsupported partition transform:" +
                partitionField.transform().toString());
          }
          Preconditions.checkArgument(schema.columns().indexOf(schema.findField(partitionField.sourceId())) >=
              (schema.columns().size() - partitionSpec.fields().size()), "Partition field should be at last of " +
              "schema");
        }
      }
    }

    @Override
    protected KeyedHiveTable createKeyedTable(TableMeta meta) {
      TableIdentifier tableIdentifier = TableIdentifier.of(meta.getTableIdentifier());
      String baseLocation = checkLocation(meta, MetaTableProperties.LOCATION_KEY_BASE);
      String changeLocation = checkLocation(meta, MetaTableProperties.LOCATION_KEY_CHANGE);
      String tableLocation = checkLocation(meta, MetaTableProperties.LOCATION_KEY_TABLE);
      fillTableProperties(meta);
      String hiveLocation = meta.getProperties().get(HiveTableProperties.BASE_HIVE_LOCATION_ROOT);
      // default 1 day
      if (!meta.properties.containsKey(TableProperties.SELF_OPTIMIZING_FULL_TRIGGER_INTERVAL)) {
        meta.putToProperties(TableProperties.SELF_OPTIMIZING_FULL_TRIGGER_INTERVAL, "86400000");
      }

      ArcticFileIO fileIO = ArcticFileIOs.buildTableFileIO(tableIdentifier, tableLocation, meta.getProperties(),
          tableMetaStore, catalogMeta.getCatalogProperties());
      Table baseIcebergTable = tableMetaStore.doAs(() -> {
        try {
          Table createTable = tables.create(schema, partitionSpec, meta.getProperties(), baseLocation);
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
          fileIO, tableLocation, client, hiveClientPool, catalogMeta.getCatalogProperties(), false);

      Table changeIcebergTable = tableMetaStore.doAs(() -> {
        try {
          Table createTable = tables.create(schema, partitionSpec, meta.getProperties(), changeLocation);
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
          fileIO, client, catalogMeta.getCatalogProperties());

      Map<String, String> metaProperties = meta.properties;
      try {
        hiveClientPool.run(client -> {
          if (allowExistedHiveTable) {
            org.apache.hadoop.hive.metastore.api.Table hiveTable = client.getTable(tableIdentifier.getDatabase(),
                tableIdentifier.getTableName());
            Map<String, String> hiveParameters = hiveTable.getParameters();
            hiveParameters.putAll(constructProperties());
            hiveTable.setParameters(hiveParameters);
            client.alterTable(tableIdentifier.getDatabase(), tableIdentifier.getTableName(), hiveTable);
          } else {
            org.apache.hadoop.hive.metastore.api.Table hiveTable = newHiveTable(meta);
            hiveTable.setSd(HiveTableUtil.storageDescriptor(schema, partitionSpec, hiveLocation,
                FileFormat.valueOf(PropertyUtil.propertyAsString(metaProperties, TableProperties.DEFAULT_FILE_FORMAT,
                    TableProperties.DEFAULT_FILE_FORMAT_DEFAULT).toUpperCase(Locale.ENGLISH))));
            setProToHive(hiveTable);
            client.createTable(hiveTable);
          }
          return null;
        });
      } catch (TException | InterruptedException e) {
        throw new RuntimeException("Failed to create hive table:" + meta.getTableIdentifier(), e);
      }
      return new KeyedHiveTable(meta, tableLocation,
          primaryKeySpec, client, hiveClientPool, baseTable, changeTable);
    }

    @Override
    protected UnkeyedHiveTable createUnKeyedTable(TableMeta meta) {
      TableIdentifier tableIdentifier = TableIdentifier.of(meta.getTableIdentifier());
      String baseLocation = checkLocation(meta, MetaTableProperties.LOCATION_KEY_BASE);
      String tableLocation = checkLocation(meta, MetaTableProperties.LOCATION_KEY_TABLE);
      fillTableProperties(meta);
      String hiveLocation = meta.getProperties().get(HiveTableProperties.BASE_HIVE_LOCATION_ROOT);

      Table table = tableMetaStore.doAs(() -> {
        try {
          Table createTable = tables.create(schema, partitionSpec, meta.getProperties(), baseLocation);
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
            hiveParameters.putAll(constructProperties());
            hiveTable.setParameters(hiveParameters);
            client.alterTable(tableIdentifier.getDatabase(), tableIdentifier.getTableName(), hiveTable);
          } else {
            org.apache.hadoop.hive.metastore.api.Table hiveTable = newHiveTable(meta);
            hiveTable.setSd(HiveTableUtil.storageDescriptor(schema, partitionSpec, hiveLocation,
                FileFormat.valueOf(PropertyUtil.propertyAsString(properties, TableProperties.BASE_FILE_FORMAT,
                    TableProperties.BASE_FILE_FORMAT_DEFAULT).toUpperCase(Locale.ENGLISH))));
            setProToHive(hiveTable);
            client.createTable(hiveTable);
          }
          return null;
        });
      } catch (TException | InterruptedException e) {
        throw new RuntimeException("Failed to create hive table:" + meta.getTableIdentifier(), e);
      }

      ArcticFileIO fileIO = ArcticFileIOs.buildTableFileIO(tableIdentifier, tableLocation, meta.getProperties(),
          tableMetaStore, catalogMeta.getCatalogProperties());
      return new UnkeyedHiveTable(tableIdentifier, CatalogUtil.useArcticTableOperations(table, baseLocation, fileIO,
          tableMetaStore.getConfiguration()), fileIO, tableLocation, client, hiveClientPool,
          catalogMeta.getCatalogProperties());
    }

    private org.apache.hadoop.hive.metastore.api.Table newHiveTable(TableMeta meta) {
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

    @Override
    protected void fillTableProperties(TableMeta meta) {
      super.fillTableProperties(meta);
      String tableLocation = checkLocation(meta, MetaTableProperties.LOCATION_KEY_TABLE);
      String hiveLocation = HiveTableUtil.hiveRootLocation(tableLocation);
      meta.putToProperties(HiveTableProperties.BASE_HIVE_LOCATION_ROOT, hiveLocation);
    }

    @Override
    protected String getDatabaseLocation() {
      try {
        return hiveClientPool.run(client -> client.getDatabase(identifier.getDatabase()).getLocationUri());
      } catch (TException | InterruptedException e) {
        throw new RuntimeException("Failed to get database location:" + identifier.getDatabase(), e);
      }
    }

    @Override
    protected void doRollbackCreateTable(TableMeta meta) {
      super.doRollbackCreateTable(meta);
      if (allowExistedHiveTable) {
        LOG.info("No need to drop hive table");
        com.netease.arctic.ams.api.TableIdentifier tableIdentifier = meta.getTableIdentifier();
        try {
          hiveClientPool.run(client -> {
            org.apache.hadoop.hive.metastore.api.Table hiveTable = client.getTable(tableIdentifier.getDatabase(),
                tableIdentifier.getTableName());
            Map<String, String> hiveParameters = hiveTable.getParameters();
            hiveParameters.remove(HiveTableProperties.ARCTIC_TABLE_FLAG);
            client.alterTable(tableIdentifier.getDatabase(), tableIdentifier.getTableName(), hiveTable);
            return null;
          });
        } catch (TException | InterruptedException e) {
          LOG.warn("Failed to alter hive table while rolling back create table operation", e);
        }
      } else {
        try {
          hiveClientPool.run(client -> {
            client.dropTable(
                meta.getTableIdentifier().getDatabase(),
                meta.getTableIdentifier().getTableName(),
                true,
                true);
            return null;
          });
        } catch (TException | InterruptedException e) {
          LOG.warn("Failed to drop hive table while rolling back create table operation", e);
        }
      }
    }

    private void setProToHive(org.apache.hadoop.hive.metastore.api.Table hiveTable) {
      Map<String, String> parameters = constructProperties();
      hiveTable.setParameters(parameters);
    }

    private Map<String, String> constructProperties() {
      Map<String, String> parameters = new HashMap<>();
      parameters.put(HiveTableProperties.ARCTIC_TABLE_FLAG, "true");
      parameters.put(HiveTableProperties.ARCTIC_TABLE_PRIMARY_KEYS, primaryKeySpec.description());
      return parameters;
    }
  }
}
