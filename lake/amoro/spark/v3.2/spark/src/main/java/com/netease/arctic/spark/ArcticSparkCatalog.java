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

package com.netease.arctic.spark;

import com.netease.arctic.catalog.ArcticCatalog;
import com.netease.arctic.catalog.CatalogLoader;
import com.netease.arctic.hive.utils.CatalogUtil;
import com.netease.arctic.spark.table.ArcticSparkChangeTable;
import com.netease.arctic.spark.table.ArcticSparkTable;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.BasicUnkeyedTable;
import com.netease.arctic.table.KeyedTable;
import com.netease.arctic.table.PrimaryKeySpec;
import com.netease.arctic.table.TableBuilder;
import com.netease.arctic.table.TableIdentifier;
import com.netease.arctic.table.UnkeyedTable;
import org.apache.commons.lang3.StringUtils;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.Transaction;
import org.apache.iceberg.exceptions.AlreadyExistsException;
import org.apache.iceberg.relocated.com.google.common.base.Joiner;
import org.apache.iceberg.relocated.com.google.common.base.Preconditions;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.apache.iceberg.relocated.com.google.common.collect.Maps;
import org.apache.iceberg.spark.Spark3Util;
import org.apache.iceberg.spark.SparkSchemaUtil;
import org.apache.iceberg.types.Types;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.catalyst.analysis.NoSuchNamespaceException;
import org.apache.spark.sql.catalyst.analysis.NoSuchTableException;
import org.apache.spark.sql.catalyst.analysis.TableAlreadyExistsException;
import org.apache.spark.sql.connector.catalog.Identifier;
import org.apache.spark.sql.connector.catalog.NamespaceChange;
import org.apache.spark.sql.connector.catalog.SupportsNamespaces;
import org.apache.spark.sql.connector.catalog.Table;
import org.apache.spark.sql.connector.catalog.TableCatalog;
import org.apache.spark.sql.connector.catalog.TableChange;
import org.apache.spark.sql.connector.catalog.TableChange.ColumnChange;
import org.apache.spark.sql.connector.catalog.TableChange.RemoveProperty;
import org.apache.spark.sql.connector.catalog.TableChange.SetProperty;
import org.apache.spark.sql.connector.expressions.Transform;
import org.apache.spark.sql.types.StructType;
import org.apache.spark.sql.util.CaseInsensitiveStringMap;
import scala.Option;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.netease.arctic.spark.SparkSQLProperties.REFRESH_CATALOG_BEFORE_USAGE;
import static com.netease.arctic.spark.SparkSQLProperties.REFRESH_CATALOG_BEFORE_USAGE_DEFAULT;
import static com.netease.arctic.spark.SparkSQLProperties.USE_TIMESTAMP_WITHOUT_TIME_ZONE_IN_NEW_TABLES;
import static com.netease.arctic.spark.SparkSQLProperties.USE_TIMESTAMP_WITHOUT_TIME_ZONE_IN_NEW_TABLES_DEFAULT;
import static org.apache.iceberg.spark.SparkSQLProperties.HANDLE_TIMESTAMP_WITHOUT_TIMEZONE;

public class ArcticSparkCatalog implements TableCatalog, SupportsNamespaces {
  // private static final Logger LOG = LoggerFactory.getLogger(ArcticSparkCatalog.class);
  private String catalogName = null;

  private ArcticCatalog catalog;

  /**
   * Build an Arctic {@link com.netease.arctic.table.TableIdentifier} for the given Spark identifier.
   *
   * @param identifier Spark's identifier
   * @return an Arctic identifier
   */
  protected TableIdentifier buildIdentifier(Identifier identifier) {
    if (identifier.namespace() == null || identifier.namespace().length == 0) {
      throw new IllegalArgumentException(
          "database is not specific, table identifier: " + identifier.name()
      );
    }

    if (identifier.namespace().length > 1) {
      throw new IllegalArgumentException(
          "arctic does not support multi-level namespace: " +
              Joiner.on(".").join(identifier.namespace()));
    }

    return TableIdentifier.of(
        catalog.name(),
        identifier.namespace()[0].split("\\.")[0],
        identifier.name());
  }

  protected TableIdentifier buildInnerTableIdentifier(Identifier identifier) {
    if (identifier.namespace() == null || identifier.namespace().length == 0) {
      throw new IllegalArgumentException(
          "database is not specific, table identifier: " + identifier.name()
      );
    }

    if (identifier.namespace().length < 2) {
      throw new IllegalArgumentException(
          "arctic does not support multi-level namespace: " +
              Joiner.on(".").join(identifier.namespace()));
    }

    return TableIdentifier.of(
        catalog.name(),
        identifier.namespace()[0],
        identifier.namespace()[1]
    );
  }

  @Override
  public Table loadTable(Identifier ident) throws NoSuchTableException {
    checkAndRefreshCatalogMeta(catalog);
    TableIdentifier identifier;
    ArcticTable table;
    try {
      if (isInnerTableIdentifier(ident)) {
        ArcticTableStoreType type = ArcticTableStoreType.from(ident.name());
        identifier = buildInnerTableIdentifier(ident);
        table = catalog.loadTable(identifier);
        return loadInnerTable(table, type);
      } else {
        identifier = buildIdentifier(ident);
        table = catalog.loadTable(identifier);
      }
    } catch (org.apache.iceberg.exceptions.NoSuchTableException e) {
      throw new NoSuchTableException(ident);
    }
    return ArcticSparkTable.ofArcticTable(table, catalog);
  }

  private Table loadInnerTable(ArcticTable table, ArcticTableStoreType type) {
    if (type != null) {
      switch (type) {
        case CHANGE:
          return new ArcticSparkChangeTable((BasicUnkeyedTable) table.asKeyedTable().changeTable(),
              false);
        default:
          throw new IllegalArgumentException("Unknown inner table type: " + type);
      }
    } else {
      throw new IllegalArgumentException("Table does not exist: " + table);
    }
  }

  private boolean isInnerTableIdentifier(Identifier identifier) {
    if (identifier.namespace().length != 2) {
      return false;
    }
    return ArcticTableStoreType.from(identifier.name()) != null;
  }

  @Override
  public Table createTable(
      Identifier ident, StructType schema, Transform[] transforms,
      Map<String, String> properties) throws TableAlreadyExistsException {
    checkAndRefreshCatalogMeta(catalog);
    properties = Maps.newHashMap(properties);
    Schema finalSchema = checkAndConvertSchema(schema, properties);
    TableIdentifier identifier = buildIdentifier(ident);
    TableBuilder builder = catalog.newTableBuilder(identifier, finalSchema);
    PartitionSpec spec = Spark3Util.toPartitionSpec(finalSchema, transforms);
    if (properties.containsKey(TableCatalog.PROP_LOCATION) &&
        isIdentifierLocation(properties.get(TableCatalog.PROP_LOCATION), ident)) {
      properties.remove(TableCatalog.PROP_LOCATION);
    }
    try {
      if (properties.containsKey("primary.keys")) {
        PrimaryKeySpec primaryKeySpec = PrimaryKeySpec.builderFor(finalSchema)
            .addDescription(properties.get("primary.keys"))
            .build();
        properties.remove("primary.keys");
        builder.withPartitionSpec(spec)
            .withProperties(properties)
            .withPrimaryKeySpec(primaryKeySpec);
      } else {
        builder.withPartitionSpec(spec)
            .withProperties(properties);
      }
      ArcticTable table = builder.create();
      return ArcticSparkTable.ofArcticTable(table, catalog);
    } catch (AlreadyExistsException e) {
      throw new TableAlreadyExistsException("Table " + ident + " already exists", Option.apply(e));
    }
  }

  private void checkAndRefreshCatalogMeta(ArcticCatalog catalog) {
    SparkSession sparkSession = SparkSession.active();
    if (Boolean.parseBoolean(sparkSession.conf().get(REFRESH_CATALOG_BEFORE_USAGE,
        REFRESH_CATALOG_BEFORE_USAGE_DEFAULT))) {
      catalog.refresh();
    }
  }

  private Schema checkAndConvertSchema(StructType schema, Map<String, String> properties) {
    Schema convertSchema;
    boolean useTimestampWithoutZoneInNewTables;
    SparkSession sparkSession = SparkSession.active();
    if (CatalogUtil.isHiveCatalog(catalog)) {
      useTimestampWithoutZoneInNewTables = true;
    } else {
      useTimestampWithoutZoneInNewTables = Boolean.parseBoolean(
          sparkSession.conf().get(USE_TIMESTAMP_WITHOUT_TIME_ZONE_IN_NEW_TABLES,
              USE_TIMESTAMP_WITHOUT_TIME_ZONE_IN_NEW_TABLES_DEFAULT));
    }
    if (useTimestampWithoutZoneInNewTables) {
      sparkSession.conf().set(HANDLE_TIMESTAMP_WITHOUT_TIMEZONE, true);
      convertSchema = SparkSchemaUtil.convert(schema, true);
    } else {
      convertSchema = SparkSchemaUtil.convert(schema, false);
    }

    // schema add primary keys
    if (properties.containsKey("primary.keys")) {
      PrimaryKeySpec primaryKeySpec = PrimaryKeySpec.builderFor(convertSchema)
          .addDescription(properties.get("primary.keys"))
          .build();
      List<String> primaryKeys = primaryKeySpec.fieldNames();
      Set<String> pkSet = new HashSet<>(primaryKeys);
      Set<Integer> identifierFieldIds = new HashSet<>();
      List<Types.NestedField> columnsWithPk = new ArrayList<>();
      convertSchema.columns().forEach(nestedField -> {
        if (pkSet.contains(nestedField.name())) {
          columnsWithPk.add(nestedField.asRequired());
          identifierFieldIds.add(nestedField.fieldId());
        } else {
          columnsWithPk.add(nestedField);
        }
      });
      return new Schema(columnsWithPk, identifierFieldIds);
    }
    return convertSchema;
  }

  private boolean isIdentifierLocation(String location, Identifier identifier) {
    List<String> nameParts = Lists.newArrayList();
    nameParts.add(name());
    nameParts.addAll(Arrays.asList(identifier.namespace()));
    nameParts.add(identifier.name());
    String ident = Joiner.on('.').join(nameParts);
    return ident.equalsIgnoreCase(location);
  }

  @Override
  public Table alterTable(Identifier ident, TableChange... changes) throws NoSuchTableException {
    TableIdentifier identifier = buildIdentifier(ident);
    ArcticTable table;
    try {
      table = catalog.loadTable(identifier);
    } catch (org.apache.iceberg.exceptions.NoSuchTableException e) {
      throw new NoSuchTableException(ident);
    }
    if (table.isUnkeyedTable()) {
      alterUnKeyedTable(table.asUnkeyedTable(), changes);
      return ArcticSparkTable.ofArcticTable(table, catalog);
    } else if (table.isKeyedTable()) {
      alterKeyedTable(table.asKeyedTable(), changes);
      return ArcticSparkTable.ofArcticTable(table, catalog);
    }
    throw new UnsupportedOperationException("Unsupported alter table");
  }

  private void alterKeyedTable(KeyedTable table, TableChange... changes) {
    List<TableChange> schemaChanges = new ArrayList<>();
    List<TableChange> propertyChanges = new ArrayList<>();
    for (TableChange change : changes) {
      if (change instanceof ColumnChange) {
        schemaChanges.add(change);
      } else if (change instanceof SetProperty) {
        propertyChanges.add(change);
      } else if (change instanceof RemoveProperty) {
        propertyChanges.add(change);
      } else {
        throw new UnsupportedOperationException("Cannot apply unknown table change: " + change);
      }
    }
    commitKeyedChanges(table, schemaChanges, propertyChanges);
  }

  private void commitKeyedChanges(
      KeyedTable table, List<TableChange> schemaChanges, List<TableChange> propertyChanges) {
    if (!schemaChanges.isEmpty()) {
      Spark3Util.applySchemaChanges(table.updateSchema(), schemaChanges).commit();
    }

    if (!propertyChanges.isEmpty()) {
      Spark3Util.applyPropertyChanges(table.updateProperties(), propertyChanges).commit();
    }
  }

  private void alterUnKeyedTable(UnkeyedTable table, TableChange... changes) {
    SetProperty setLocation = null;
    SetProperty setSnapshotId = null;
    SetProperty pickSnapshotId = null;
    List<TableChange> propertyChanges = new ArrayList<>();
    List<TableChange> schemaChanges = new ArrayList<>();

    for (TableChange change : changes) {
      if (change instanceof SetProperty) {
        SetProperty set = (SetProperty) change;
        if (TableCatalog.PROP_LOCATION.equalsIgnoreCase(set.property())) {
          setLocation = set;
        } else if ("current-snapshot-id".equalsIgnoreCase(set.property())) {
          setSnapshotId = set;
        } else if ("cherry-pick-snapshot-id".equalsIgnoreCase(set.property())) {
          pickSnapshotId = set;
        } else if ("sort-order".equalsIgnoreCase(set.property())) {
          throw new UnsupportedOperationException("Cannot specify the 'sort-order' because it's a reserved table " +
              "property. Please use the command 'ALTER TABLE ... WRITE ORDERED BY' to specify write sort-orders.");
        } else {
          propertyChanges.add(set);
        }
      } else if (change instanceof RemoveProperty) {
        propertyChanges.add(change);
      } else if (change instanceof ColumnChange) {
        schemaChanges.add(change);
      } else {
        throw new UnsupportedOperationException("Cannot apply unknown table change: " + change);
      }
    }

    commitUnKeyedChanges(table, setLocation, setSnapshotId, pickSnapshotId, propertyChanges, schemaChanges);
  }

  protected void commitUnKeyedChanges(
      UnkeyedTable table, SetProperty setLocation, SetProperty setSnapshotId,
      SetProperty pickSnapshotId, List<TableChange> propertyChanges,
      List<TableChange> schemaChanges
  ) {
    // don't allow setting the snapshot and picking a commit at the same time because order is ambiguous and choosing
    // one order leads to different results
    Preconditions.checkArgument(
        setSnapshotId == null || pickSnapshotId == null,
        "Cannot set the current the current snapshot ID and cherry-pick snapshot changes");

    if (setSnapshotId != null) {
      long newSnapshotId = Long.parseLong(setSnapshotId.value());
      table.manageSnapshots().setCurrentSnapshot(newSnapshotId).commit();
    }

    // if updating the table snapshot, perform that update first in case it fails
    if (pickSnapshotId != null) {
      long newSnapshotId = Long.parseLong(pickSnapshotId.value());
      table.manageSnapshots().cherrypick(newSnapshotId).commit();
    }

    Transaction transaction = table.newTransaction();

    if (setLocation != null) {
      transaction.updateLocation()
          .setLocation(setLocation.value())
          .commit();
    }

    if (!propertyChanges.isEmpty()) {
      Spark3Util.applyPropertyChanges(transaction.updateProperties(), propertyChanges).commit();
    }

    if (!schemaChanges.isEmpty()) {
      Spark3Util.applySchemaChanges(transaction.updateSchema(), schemaChanges).commit();
    }

    transaction.commitTransaction();
  }

  @Override
  public boolean dropTable(Identifier ident) {
    TableIdentifier identifier = buildIdentifier(ident);
    return catalog.dropTable(identifier, true);
  }

  @Override
  public void renameTable(Identifier from, Identifier to) {
    throw new UnsupportedOperationException("Unsupported renameTable.");
  }

  @Override
  public Identifier[] listTables(String[] namespace) {
    List<String> database;
    if (namespace == null || namespace.length == 0) {
      database = catalog.listDatabases();
    } else {
      database = new ArrayList<>();
      database.add(namespace[0]);
    }

    List<TableIdentifier> tableIdentifiers = database.stream()
        .map(d -> catalog.listTables(d))
        .flatMap(Collection::stream)
        .collect(Collectors.toList());

    return tableIdentifiers.stream()
        .map(i -> Identifier.of(new String[]{i.getDatabase()}, i.getTableName()))
        .toArray(Identifier[]::new);
  }

  @Override
  public final void initialize(String name, CaseInsensitiveStringMap options) {
    this.catalogName = name;
    String catalogUrl = options.get("url");
    if (StringUtils.isBlank(catalogUrl)) {
      throw new IllegalArgumentException("lack required properties: url");
    }
    catalog = CatalogLoader.load(catalogUrl, Maps.newHashMap());
  }

  @Override
  public String name() {
    return catalogName;
  }

  @Override
  public String[][] listNamespaces() {
    return catalog.listDatabases().stream()
        .map(d -> new String[]{d})
        .toArray(String[][]::new);
  }

  // ns
  @Override
  public String[][] listNamespaces(String[] namespace) {
    return new String[0][];
  }

  @Override
  public Map<String, String> loadNamespaceMetadata(String[] namespace) throws NoSuchNamespaceException {
    String database = namespace[0];
    return catalog.listDatabases().stream()
        .filter(d -> StringUtils.equals(d, database))
        .map(d -> new HashMap<String, String>())
        .findFirst().orElseThrow(() -> new NoSuchNamespaceException(namespace));
  }

  @Override
  public void createNamespace(String[] namespace, Map<String, String> metadata) {
    if (namespace.length > 1) {
      throw new UnsupportedOperationException("arctic does not support multi-level namespace.");
    }
    String database = namespace[0];
    catalog.createDatabase(database);
  }

  @Override
  public void alterNamespace(String[] namespace, NamespaceChange... changes) {
    throw new UnsupportedOperationException("Alter  namespace is not supported by catalog: " + catalogName);
  }

  @Override
  public boolean dropNamespace(String[] namespace) throws NoSuchNamespaceException {
    String database = namespace[0];
    catalog.dropDatabase(database);
    return true;
  }
}
