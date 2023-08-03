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
import com.netease.arctic.ams.api.TableFormat;
import com.netease.arctic.ams.api.TableMeta;
import com.netease.arctic.catalog.BasicArcticCatalog;
import com.netease.arctic.catalog.MixedTables;
import com.netease.arctic.hive.CachedHiveClientPool;
import com.netease.arctic.hive.HMSClient;
import com.netease.arctic.hive.HMSClientPool;
import com.netease.arctic.hive.HiveTableProperties;
import com.netease.arctic.hive.utils.HiveSchemaUtil;
import com.netease.arctic.table.PrimaryKeySpec;
import com.netease.arctic.table.TableBuilder;
import com.netease.arctic.table.TableIdentifier;
import com.netease.arctic.table.TableProperties;
import com.netease.arctic.utils.ConvertStructUtil;
import org.apache.hadoop.hive.metastore.api.AlreadyExistsException;
import org.apache.hadoop.hive.metastore.api.Database;
import org.apache.iceberg.IcebergSchemaUtil;
import org.apache.iceberg.PartitionField;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.SortOrder;
import org.apache.iceberg.relocated.com.google.common.base.Preconditions;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    this.hiveClientPool = ((MixedHiveTables)tables).getHiveClientPool();
  }

  @Override
  protected MixedTables newMixedTables(CatalogMeta catalogMeta) {
    return new MixedHiveTables(catalogMeta);
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

    } catch (TException | InterruptedException e) {
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

  public void dropTableButNotDropHiveTable(TableIdentifier tableIdentifier) {
    TableMeta meta = getArcticTableMeta(tableIdentifier);
    super.doDropTable(meta, false);
  }

  @Override
  public TableBuilder newTableBuilder(
      TableIdentifier identifier, Schema schema) {
    return new MixedHiveTableBuilder(identifier, schema);
  }

  public HMSClientPool getHMSClient() {
    return hiveClientPool;
  }


  class MixedHiveTableBuilder extends ArcticTableBuilder {

    public MixedHiveTableBuilder(TableIdentifier identifier, Schema schema) {
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
        super.withProperty(key, value);
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
            org.apache.hadoop.hive.metastore.api.Table hiveTable = client.getTable(
                tableIdentifier.getDatabase(),
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

    @Override
    protected ConvertStructUtil.TableMetaBuilder createTableMataBuilder() {
      ConvertStructUtil.TableMetaBuilder builder = super.createTableMataBuilder();
      return builder.withFormat(TableFormat.MIXED_HIVE);
    }
  }
}
