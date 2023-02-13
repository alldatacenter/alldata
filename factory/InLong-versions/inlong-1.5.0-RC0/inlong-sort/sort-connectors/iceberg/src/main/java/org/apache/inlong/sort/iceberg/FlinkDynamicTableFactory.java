/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sort.iceberg;

import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.configuration.ConfigOptions;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.catalog.CatalogBaseTable;
import org.apache.flink.table.catalog.CatalogDatabaseImpl;
import org.apache.flink.table.catalog.CatalogTable;
import org.apache.flink.table.catalog.ObjectIdentifier;
import org.apache.flink.table.catalog.ObjectPath;
import org.apache.flink.table.catalog.exceptions.DatabaseAlreadyExistException;
import org.apache.flink.table.catalog.exceptions.TableAlreadyExistException;
import org.apache.flink.table.connector.sink.DynamicTableSink;
import org.apache.flink.table.connector.source.DynamicTableSource;
import org.apache.flink.table.factories.DynamicTableSinkFactory;
import org.apache.flink.table.factories.DynamicTableSourceFactory;
import org.apache.flink.table.utils.TableSchemaUtils;
import org.apache.flink.util.Preconditions;
import org.apache.iceberg.actions.ActionsProvider;
import org.apache.iceberg.catalog.TableIdentifier;
import org.apache.iceberg.common.DynMethods;
import org.apache.iceberg.exceptions.AlreadyExistsException;
import org.apache.iceberg.flink.CatalogLoader;
import org.apache.iceberg.flink.IcebergTableSource;
import org.apache.iceberg.flink.TableLoader;
import org.apache.iceberg.relocated.com.google.common.collect.Maps;
import org.apache.iceberg.relocated.com.google.common.collect.Sets;
import org.apache.inlong.sort.base.dirty.DirtyOptions;
import org.apache.inlong.sort.base.dirty.sink.DirtySink;
import org.apache.inlong.sort.base.dirty.utils.DirtySinkFactoryUtils;

import java.util.Map;
import java.util.Set;

import static org.apache.inlong.sort.base.Constants.IGNORE_ALL_CHANGELOG;
import static org.apache.inlong.sort.base.Constants.INLONG_AUDIT;
import static org.apache.inlong.sort.base.Constants.INLONG_METRIC;
import static org.apache.inlong.sort.base.Constants.SINK_MULTIPLE_DATABASE_PATTERN;
import static org.apache.inlong.sort.base.Constants.SINK_MULTIPLE_ENABLE;
import static org.apache.inlong.sort.base.Constants.SINK_MULTIPLE_FORMAT;
import static org.apache.inlong.sort.base.Constants.SINK_MULTIPLE_PK_AUTO_GENERATED;
import static org.apache.inlong.sort.base.Constants.SINK_MULTIPLE_SCHEMA_UPDATE_POLICY;
import static org.apache.inlong.sort.base.Constants.SINK_MULTIPLE_TABLE_PATTERN;
import static org.apache.inlong.sort.base.Constants.SINK_MULTIPLE_TYPE_MAP_COMPATIBLE_WITH_SPARK;

/**
 * Copy from org.apache.iceberg.flink:iceberg-flink-runtime-1.13:0.13.2
 *
 * <p>
 * Factory for creating configured instances of {@link IcebergTableSource} and {@link
 * IcebergTableSink}.We modify IcebergDynamicTableSink to support append-mode .
 * </p>
 */
public class FlinkDynamicTableFactory implements DynamicTableSinkFactory, DynamicTableSourceFactory {

    static final String FACTORY_IDENTIFIER = "iceberg-inlong";

    private static final ConfigOption<String> CATALOG_NAME =
            ConfigOptions.key("catalog-name")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("Catalog name");

    private static final ConfigOption<String> CATALOG_TYPE =
            ConfigOptions.key(FlinkCatalogFactory.ICEBERG_CATALOG_TYPE)
                    .stringType()
                    .noDefaultValue()
                    .withDescription("Catalog type, the optional types are: custom, hadoop, hive.");

    private static final ConfigOption<String> CATALOG_DATABASE =
            ConfigOptions.key("catalog-database")
                    .stringType()
                    .defaultValue(FlinkCatalogFactory.DEFAULT_DATABASE_NAME)
                    .withDescription("Database name managed in the iceberg catalog.");

    private static final ConfigOption<String> CATALOG_TABLE =
            ConfigOptions.key("catalog-table")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("Table name managed in the underlying iceberg catalog and database.");

    private static final ConfigOption<String> ACTION_IMPL =
            ConfigOptions.key("action-impl")
                    .stringType()
                    .defaultValue("org.apache.inlong.sort.iceberg.FlinkActions$FlinkDefaultActions")
                    .withDescription("Iceberg action provider implement.");

    // Flink 1.13.x change the return type from CatalogTable interface to ResolvedCatalogTable which extends the
    // CatalogTable. Here we use the dynamic method loading approach to avoid adding explicit CatalogTable or
    // ResolvedCatalogTable class into the iceberg-flink-runtime jar for compatibility purpose.
    private static final DynMethods.UnboundMethod GET_CATALOG_TABLE = DynMethods.builder("getCatalogTable")
            .impl(Context.class, "getCatalogTable")
            .orNoop()
            .build();

    private final FlinkCatalog catalog;

    public FlinkDynamicTableFactory() {
        this.catalog = null;
    }

    public FlinkDynamicTableFactory(FlinkCatalog catalog) {
        this.catalog = catalog;
    }

    private static CatalogTable loadCatalogTable(Context context) {
        return GET_CATALOG_TABLE.invoke(context);
    }

    private static CatalogLoader createCatalogLoader(Map<String, String> tableProps) {
        Configuration flinkConf = new Configuration();
        tableProps.forEach(flinkConf::setString);

        String catalogName = flinkConf.getString(CATALOG_NAME);
        Preconditions.checkNotNull(catalogName, "Table property '%s' cannot be null", CATALOG_NAME.key());

        org.apache.hadoop.conf.Configuration hadoopConf = FlinkCatalogFactory.clusterHadoopConf();
        return FlinkCatalogFactory.createCatalogLoader(catalogName, tableProps, hadoopConf);
    }

    private static TableLoader createTableLoader(CatalogBaseTable catalogBaseTable,
            Map<String, String> tableProps,
            String databaseName,
            String tableName) {
        Configuration flinkConf = new Configuration();
        tableProps.forEach(flinkConf::setString);

        String catalogName = flinkConf.getString(CATALOG_NAME);
        Preconditions.checkNotNull(catalogName, "Table property '%s' cannot be null", CATALOG_NAME.key());

        String catalogDatabase = flinkConf.getString(CATALOG_DATABASE, databaseName);
        Preconditions.checkNotNull(catalogDatabase, "The iceberg database name cannot be null");

        String catalogTable = flinkConf.getString(CATALOG_TABLE, tableName);
        Preconditions.checkNotNull(catalogTable, "The iceberg table name cannot be null");

        org.apache.hadoop.conf.Configuration hadoopConf = FlinkCatalogFactory.clusterHadoopConf();
        FlinkCatalogFactory factory = new FlinkCatalogFactory();
        FlinkCatalog flinkCatalog = (FlinkCatalog) factory.createCatalog(catalogName, tableProps, hadoopConf);
        ObjectPath objectPath = new ObjectPath(catalogDatabase, catalogTable);

        // Create database if not exists in the external catalog.
        if (!flinkCatalog.databaseExists(catalogDatabase)) {
            try {
                flinkCatalog.createDatabase(catalogDatabase, new CatalogDatabaseImpl(Maps.newHashMap(), null), true);
            } catch (DatabaseAlreadyExistException e) {
                throw new AlreadyExistsException(e, "Database %s already exists in the iceberg catalog %s.",
                        catalogName,
                        catalogDatabase);
            }
        }

        // Create table if not exists in the external catalog.
        if (!flinkCatalog.tableExists(objectPath)) {
            try {
                flinkCatalog.createIcebergTable(objectPath, catalogBaseTable, true);
            } catch (TableAlreadyExistException e) {
                throw new AlreadyExistsException(e, "Table %s already exists in the database %s and catalog %s",
                        catalogTable, catalogDatabase, catalogName);
            }
        }

        return TableLoader.fromCatalog(flinkCatalog.getCatalogLoader(),
                TableIdentifier.of(catalogDatabase, catalogTable));
    }

    private static TableLoader createTableLoader(FlinkCatalog catalog, ObjectPath objectPath) {
        Preconditions.checkNotNull(catalog, "Flink catalog cannot be null");
        return TableLoader.fromCatalog(catalog.getCatalogLoader(), catalog.toIdentifier(objectPath));
    }

    private static ActionsProvider createActionLoader(ClassLoader cl, Map<String, String> tableProps) {
        String actionClass = tableProps.getOrDefault(ACTION_IMPL.key(), ACTION_IMPL.defaultValue());
        try {
            FlinkActions actionsProvider = (FlinkActions) cl.loadClass(actionClass).newInstance();
            actionsProvider.init(tableProps);
            return actionsProvider;
        } catch (ClassNotFoundException
                | IllegalAccessException
                | InstantiationException e) {
            throw new RuntimeException("Can not new instance for custom class from " + actionClass, e);
        }
    }

    @Override
    public DynamicTableSource createDynamicTableSource(Context context) {
        ObjectIdentifier objectIdentifier = context.getObjectIdentifier();
        CatalogTable catalogTable = loadCatalogTable(context);
        Map<String, String> tableProps = catalogTable.getOptions();
        TableSchema tableSchema = TableSchemaUtils.getPhysicalSchema(catalogTable.getSchema());

        TableLoader tableLoader;
        if (catalog != null) {
            tableLoader = createTableLoader(catalog, objectIdentifier.toObjectPath());
        } else {
            tableLoader = createTableLoader(catalogTable, tableProps, objectIdentifier.getDatabaseName(),
                    objectIdentifier.getObjectName());
        }

        return new IcebergTableSource(tableLoader, tableSchema, tableProps, context.getConfiguration());
    }

    @Override
    public DynamicTableSink createDynamicTableSink(Context context) {
        ObjectPath objectPath = context.getObjectIdentifier().toObjectPath();
        CatalogTable catalogTable = loadCatalogTable(context);
        Map<String, String> tableProps = catalogTable.getOptions();
        TableSchema tableSchema = TableSchemaUtils.getPhysicalSchema(catalogTable.getSchema());
        ActionsProvider actionsLoader = createActionLoader(context.getClassLoader(), tableProps);
        // Build the dirty data side-output
        final DirtyOptions dirtyOptions = DirtyOptions.fromConfig(Configuration.fromMap(tableProps));
        final DirtySink<Object> dirtySink = DirtySinkFactoryUtils.createDirtySink(context, dirtyOptions);
        boolean multipleSink = Boolean.parseBoolean(
                tableProps.getOrDefault(SINK_MULTIPLE_ENABLE.key(), SINK_MULTIPLE_ENABLE.defaultValue().toString()));
        if (multipleSink) {
            CatalogLoader catalogLoader = createCatalogLoader(tableProps);
            return new IcebergTableSink(null, tableSchema, catalogTable,
                    catalogLoader, actionsLoader, dirtyOptions, dirtySink);
        } else {
            TableLoader tableLoader;
            if (catalog != null) {
                tableLoader = createTableLoader(catalog, objectPath);
            } else {
                tableLoader = createTableLoader(catalogTable, tableProps, objectPath.getDatabaseName(),
                        objectPath.getObjectName());
            }
            return new IcebergTableSink(tableLoader, tableSchema, catalogTable,
                    null, actionsLoader, dirtyOptions, dirtySink);
        }
    }

    @Override
    public Set<ConfigOption<?>> requiredOptions() {
        Set<ConfigOption<?>> options = Sets.newHashSet();
        options.add(CATALOG_TYPE);
        options.add(CATALOG_NAME);
        return options;
    }

    @Override
    public Set<ConfigOption<?>> optionalOptions() {
        Set<ConfigOption<?>> options = Sets.newHashSet();
        options.add(CATALOG_DATABASE);
        options.add(CATALOG_TABLE);
        options.add(ACTION_IMPL);
        options.add(IGNORE_ALL_CHANGELOG);
        options.add(INLONG_METRIC);
        options.add(INLONG_AUDIT);

        options.add(SINK_MULTIPLE_ENABLE);
        options.add(SINK_MULTIPLE_FORMAT);
        options.add(SINK_MULTIPLE_DATABASE_PATTERN);
        options.add(SINK_MULTIPLE_TABLE_PATTERN);
        options.add(SINK_MULTIPLE_SCHEMA_UPDATE_POLICY);
        options.add(SINK_MULTIPLE_PK_AUTO_GENERATED);
        options.add(SINK_MULTIPLE_TYPE_MAP_COMPATIBLE_WITH_SPARK);
        return options;
    }

    @Override
    public String factoryIdentifier() {
        return FACTORY_IDENTIFIER;
    }
}
