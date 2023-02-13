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

package org.apache.inlong.manager.service.resource.sink.iceberg;

import org.apache.commons.lang3.StringUtils;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.Table;
import org.apache.iceberg.UpdatePartitionSpec;
import org.apache.iceberg.UpdateSchema;
import org.apache.iceberg.catalog.Namespace;
import org.apache.iceberg.catalog.TableIdentifier;
import org.apache.iceberg.expressions.Expressions;
import org.apache.iceberg.hive.HiveCatalog;
import org.apache.iceberg.types.Types;
import org.apache.iceberg.types.Types.NestedField;
import org.apache.inlong.manager.pojo.sink.iceberg.IcebergColumnInfo;
import org.apache.inlong.manager.pojo.sink.iceberg.IcebergPartition;
import org.apache.inlong.manager.pojo.sink.iceberg.IcebergTableInfo;
import org.apache.inlong.manager.pojo.sink.iceberg.IcebergType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utils for Iceberg Catalog
 */
public class IcebergCatalogUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(IcebergCatalogUtils.class);

    private static final String CATALOG_PROP_WAREHOUSE = "warehouse";
    private static final String CATALOG_PROP_URI = "uri";

    /**
     * Get Hive catalog for iceberg registry
     */
    public static HiveCatalog getCatalog(String metastoreUri, String warehouse) {
        HiveCatalog catalog = new HiveCatalog();
        Map<String, String> properties = new HashMap<>();
        properties.put(CATALOG_PROP_URI, metastoreUri);
        if (StringUtils.isNotEmpty(warehouse)) {
            properties.put(CATALOG_PROP_WAREHOUSE, warehouse);
        }
        catalog.initialize("hive", properties);
        return catalog;
    }

    /**
     * Get Hive catalog for iceberg registry
     */
    public static HiveCatalog getCatalog(String metastoreUri) {
        return getCatalog(metastoreUri, "");
    }

    /**
     * Create Iceberg namespace
     */
    public static void createDb(String metastoreUri, String warehouse, String dbName) {
        HiveCatalog catalog = getCatalog(metastoreUri, warehouse);
        Namespace ns = Namespace.of(dbName);
        if (catalog.namespaceExists(ns)) {
            LOGGER.info("db {} already exists", dbName);
            return;
        }
        catalog.createNamespace(ns);
    }

    /**
     * Create Iceberg table
     */
    public static void createTable(String metastoreUri, String warehouse, IcebergTableInfo tableInfo) {
        // prepare table scheme
        List<Types.NestedField> nestedFields = new ArrayList<>();
        int id = 1;
        for (IcebergColumnInfo column : tableInfo.getColumns()) {
            if (column.isRequired()) {
                nestedFields.add(Types.NestedField.required(id, column.getName(),
                        Types.fromPrimitiveString(icebergTypeDesc(column))));
            } else {
                nestedFields.add(Types.NestedField.optional(id, column.getName(),
                        Types.fromPrimitiveString(icebergTypeDesc(column))));
            }
            id += 1;
        }
        Schema schema = new Schema(nestedFields);

        // prepare partition spec
        PartitionSpec spec = createPartitionSpec(schema, tableInfo.getColumns());

        // create table
        HiveCatalog catalog = getCatalog(metastoreUri, warehouse);
        TableIdentifier name = TableIdentifier.of(tableInfo.getDbName(), tableInfo.getTableName());
        catalog.createTable(name, schema, spec);
    }

    /**
     * Transform to iceberg recognizable type description
     */
    private static String icebergTypeDesc(IcebergColumnInfo column) {
        switch (IcebergType.forType(column.getType())) {
            case DECIMAL:
                // note: the space is needed or iceberg won't recognize
                return String.format("decimal(%d, %d)", column.getPrecision(), column.getScale());
            case FIXED:
                return String.format("fixed(%d)", column.getLength());
            default:
                return column.getType();
        }
    }

    /**
     * Check Iceberg table already exists or not in metastore
     */
    public static boolean tableExists(String metastoreUri, String dbName, String tableName) {
        HiveCatalog catalog = getCatalog(metastoreUri);
        return catalog.tableExists(TableIdentifier.of(dbName, tableName));
    }

    /**
     * Query Iceberg columns
     */
    public static List<IcebergColumnInfo> getColumns(String metastoreUri, String dbName, String tableName) {
        List<IcebergColumnInfo> columnList = new ArrayList<>();
        HiveCatalog catalog = getCatalog(metastoreUri);
        Table table = catalog.loadTable(TableIdentifier.of(dbName, tableName));
        Schema schema = table.schema();
        for (NestedField column : schema.columns()) {
            IcebergColumnInfo info = new IcebergColumnInfo();
            info.setName(column.name());
            info.setRequired(column.isRequired());
            columnList.add(info);
        }
        return columnList;
    }

    /**
     * Add columns for Iceberg table
     */
    public static void addColumns(String metastoreUri, String dbName, String tableName,
            List<IcebergColumnInfo> columns) {
        HiveCatalog catalog = getCatalog(metastoreUri);
        Table table = catalog.loadTable(TableIdentifier.of(dbName, tableName));

        // update column
        UpdateSchema updateSchema = table.updateSchema();
        for (IcebergColumnInfo column : columns) {
            if (column.isRequired()) {
                updateSchema.addRequiredColumn(column.getName(), Types.fromPrimitiveString(icebergTypeDesc(column)),
                        column.getDesc());
            } else {
                updateSchema.addColumn(column.getName(), Types.fromPrimitiveString(icebergTypeDesc(column)),
                        column.getDesc());
            }
        }

        // commit schema update before partition spec update
        updateSchema.commit();

        // update partition spec
        UpdatePartitionSpec updateSpec = table.updateSpec();
        columns.forEach(c -> updateColumnSpec(c, updateSpec));
        updateSpec.commit();
    }

    /**
     * Create iceberg table partition meta data
     */
    private static PartitionSpec createPartitionSpec(Schema schema, List<IcebergColumnInfo> columns) {
        PartitionSpec.Builder spec = PartitionSpec.builderFor(schema);
        columns.forEach(c -> buildColumnSpec(c, spec));
        return spec.build();
    }

    /**
     * Build iceberg table column schema
     */
    private static void buildColumnSpec(IcebergColumnInfo column, PartitionSpec.Builder builder) {
        if (StringUtils.isEmpty(column.getPartitionStrategy())) {
            return;
        }
        switch (IcebergPartition.forName(column.getPartitionStrategy())) {
            case IDENTITY:
                builder.identity(column.getName());
                break;
            case BUCKET:
                builder.bucket(column.getName(), column.getBucketNum());
                break;
            case TRUNCATE:
                builder.truncate(column.getName(), column.getWidth());
                break;
            case YEAR:
                builder.year(column.getName());
                break;
            case MONTH:
                builder.month(column.getName());
                break;
            case DAY:
                builder.day(column.getName());
                break;
            case HOUR:
                builder.hour(column.getName());
                break;
            case NONE:
                break;
            default:
                throw new IllegalArgumentException(
                        "unknown iceberg partition strategy: " + column.getPartitionStrategy());
        }
    }

    /**
     * Update iceberg table column schema.
     * It's unfortunate that the updating api is different from the creating api so the partition type switch is
     * repeated here.
     */
    private static void updateColumnSpec(IcebergColumnInfo column, UpdatePartitionSpec builder) {
        if (StringUtils.isEmpty(column.getPartitionStrategy())) {
            return;
        }
        switch (IcebergPartition.forName(column.getPartitionStrategy())) {
            case IDENTITY:
                builder.addField(column.getName());
                break;
            case BUCKET:
                builder.addField(Expressions.bucket(column.getName(), column.getBucketNum()));
                break;
            case TRUNCATE:
                builder.addField(Expressions.truncate(column.getName(), column.getWidth()));
                break;
            case YEAR:
                builder.addField(Expressions.year(column.getName()));
                break;
            case MONTH:
                builder.addField(Expressions.month(column.getName()));
                break;
            case DAY:
                builder.addField(Expressions.day(column.getName()));
                break;
            case HOUR:
                builder.addField(Expressions.hour(column.getName()));
                break;
            case NONE:
                break;
            default:
                throw new IllegalArgumentException(
                        "unknown iceberg partition strategy: " + column.getPartitionStrategy());
        }
    }
}
