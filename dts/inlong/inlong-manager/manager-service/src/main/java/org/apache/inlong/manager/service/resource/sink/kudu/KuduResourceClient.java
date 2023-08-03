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

package org.apache.inlong.manager.service.resource.sink.kudu;

import org.apache.inlong.manager.pojo.sink.kudu.KuduColumnInfo;
import org.apache.inlong.manager.pojo.sink.kudu.KuduTableInfo;
import org.apache.inlong.manager.pojo.sink.kudu.KuduType;

import org.apache.kudu.ColumnSchema;
import org.apache.kudu.ColumnSchema.ColumnSchemaBuilder;
import org.apache.kudu.Schema;
import org.apache.kudu.Type;
import org.apache.kudu.client.AlterTableOptions;
import org.apache.kudu.client.CreateTableOptions;
import org.apache.kudu.client.KuduClient;
import org.apache.kudu.client.KuduException;
import org.apache.kudu.client.KuduTable;
import org.apache.kudu.client.ListTablesResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The resourceClient for Kudu.
 */
public class KuduResourceClient implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(KuduResourceClient.class);

    private static final String PARTITION_STRATEGY_HASH = "HASH";
    private static final String PARTITION_STRATEGY_PRIMARY_KEY = "PrimaryKey";
    private static final int DEFAULT_BUCKETS = 6;

    private final KuduClient client;

    public KuduResourceClient(String kuduMaster) {
        this.client = new KuduClient.KuduClientBuilder(kuduMaster).build();
    }

    public boolean tableExist(String tableName) throws KuduException {
        return client.tableExists(tableName);
    }

    /**
     * Create table with given tableName and tableInfo.
     */
    public void createTable(String tableName, KuduTableInfo tableInfo) throws KuduException {
        // Parse column from tableInfo.
        List<KuduColumnInfo> columns = tableInfo.getColumns();
        List<ColumnSchema> kuduColumns = columns.stream()
                .sorted(Comparator.comparing(KuduColumnInfo::getPartitionStrategy))
                .map(this::buildColumnSchema)
                .collect(Collectors.toList());

        // Build schema.
        Schema schema = new Schema(kuduColumns);
        // Create table options: like partitions
        CreateTableOptions options = new CreateTableOptions();
        List<String> parCols = columns.stream()
                .filter(column -> PARTITION_STRATEGY_HASH.equalsIgnoreCase(column.getPartitionStrategy()))
                .map(KuduColumnInfo::getFieldName).collect(Collectors.toList());
        if (!parCols.isEmpty()) {
            Integer partitionNum = tableInfo.getBuckets();
            int buckets = partitionNum == null ? DEFAULT_BUCKETS : partitionNum;
            options.addHashPartitions(parCols, buckets);
        }

        // Create table by KuduClient.
        client.createTable(tableName, schema, options);
    }

    private ColumnSchema buildColumnSchema(KuduColumnInfo columnInfo) {
        String name = columnInfo.getFieldName();
        String type = columnInfo.getFieldType();
        String desc = columnInfo.getFieldComment();

        String kuduType = KuduType.forType(type).kuduType();
        Type typeForName = Type.getTypeForName(kuduType);

        String partitionStrategy = columnInfo.getPartitionStrategy();

        ColumnSchemaBuilder builder = new ColumnSchemaBuilder(name, typeForName)
                .comment(desc);

        // Build schema: the hash key and partition key must be primary key.
        if (PARTITION_STRATEGY_HASH.equalsIgnoreCase(partitionStrategy)
                || PARTITION_STRATEGY_PRIMARY_KEY.equalsIgnoreCase(partitionStrategy)) {
            builder.key(true);
        } else {
            // The primary key must not null.
            builder.nullable(true);
        }
        return builder.build();
    }

    private ColumnSchema buildColumnSchema(
            String name,
            String desc,
            Type typeForName) {

        ColumnSchemaBuilder builder = new ColumnSchemaBuilder(name, typeForName)
                .comment(desc)
                .nullable(true);
        return builder.build();
    }

    public List<KuduColumnInfo> getColumns(String tableName) throws KuduException {
        // Open table and get column information.
        KuduTable kuduTable = client.openTable(tableName);
        List<ColumnSchema> columns = kuduTable.getSchema().getColumns();
        return columns
                .stream()
                .map(columnSchema -> {
                    String comment = columnSchema.getComment();
                    Type type = columnSchema.getType();
                    String name = columnSchema.getName();

                    String javaType = KuduType.forKuduType(type.getName()).getType();

                    KuduColumnInfo columnInfo = new KuduColumnInfo();
                    columnInfo.setFieldName(name);
                    columnInfo.setFieldType(javaType);
                    columnInfo.setFieldComment(comment);

                    return columnInfo;
                })
                .collect(Collectors.toList());
    }

    public void addColumns(String tableName,
            List<KuduColumnInfo> needAddColumns)
            throws KuduException {
        // Create Kudu client and open table
        KuduTable table = client.openTable(tableName);

        // Add new column to table
        AlterTableOptions alterOptions = new AlterTableOptions();
        for (KuduColumnInfo columnInfo : needAddColumns) {
            String name = columnInfo.getFieldName();
            String type = columnInfo.getFieldType();
            String desc = columnInfo.getFieldComment();

            String kuduType = KuduType.forType(type).kuduType();
            Type typeForName = Type.getTypeForName(kuduType);

            ColumnSchema columnSchema = buildColumnSchema(name, desc, typeForName);

            alterOptions.addColumn(columnSchema);
        }
        client.alterTable(table.getName(), alterOptions);
    }

    public List<String> getTablesList() throws KuduException {
        ListTablesResponse tablesList = client.getTablesList();
        return tablesList.getTablesList();
    }

    /**
     * Close KuduClient bundled this instance.
     */
    public void close() {
        try {
            client.close();
        } catch (KuduException e) {
            LOG.error("Can not properly close kuduClient.", e);
        }
    }

}
