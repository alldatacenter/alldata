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

package org.apache.inlong.sort.iceberg.schema;

import org.apache.inlong.sort.base.dirty.DirtySinkHelper;
import org.apache.inlong.sort.base.dirty.DirtyType;
import org.apache.inlong.sort.base.format.JsonDynamicSchemaFormat;
import org.apache.inlong.sort.base.metric.sub.SinkTableMetricData;
import org.apache.inlong.sort.base.schema.SchemaChangeHandleException;
import org.apache.inlong.sort.base.schema.SchemaChangeHelper;
import org.apache.inlong.sort.base.sink.SchemaUpdateExceptionPolicy;
import org.apache.inlong.sort.iceberg.sink.multiple.IcebergSchemaChangeUtils;
import org.apache.inlong.sort.protocol.ddl.enums.PositionType;
import org.apache.inlong.sort.protocol.ddl.expressions.AlterColumn;
import org.apache.inlong.sort.protocol.ddl.expressions.Column;
import org.apache.inlong.sort.protocol.ddl.operations.CreateTableOperation;
import org.apache.inlong.sort.protocol.enums.SchemaChangePolicy;
import org.apache.inlong.sort.protocol.enums.SchemaChangeType;
import org.apache.inlong.sort.schema.TableChange;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.table.types.logical.LogicalType;
import org.apache.flink.table.types.logical.RowType;
import org.apache.iceberg.Schema;
import org.apache.iceberg.Table;
import org.apache.iceberg.Transaction;
import org.apache.iceberg.catalog.Catalog;
import org.apache.iceberg.catalog.SupportsNamespaces;
import org.apache.iceberg.catalog.TableIdentifier;
import org.apache.iceberg.flink.FlinkSchemaUtil;
import org.apache.iceberg.types.Types;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Iceberg schema change helper
 * */
public class IcebergSchemaChangeHelper extends SchemaChangeHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(IcebergSchemaChangeHelper.class);
    private transient Catalog catalog;

    private transient SupportsNamespaces asNamespaceCatalog;

    public IcebergSchemaChangeHelper(JsonDynamicSchemaFormat dynamicSchemaFormat, boolean schemaChange,
            Map<SchemaChangeType, SchemaChangePolicy> policyMap, String databasePattern, String tablePattern,
            SchemaUpdateExceptionPolicy exceptionPolicy,
            SinkTableMetricData metricData, DirtySinkHelper<Object> dirtySinkHelper,
            Catalog catalog,
            SupportsNamespaces asNamespaceCatalog) {
        super(dynamicSchemaFormat, schemaChange, policyMap, databasePattern,
                tablePattern, exceptionPolicy, metricData, dirtySinkHelper);
        this.catalog = catalog;
        this.asNamespaceCatalog = asNamespaceCatalog;
    }
    @Override
    public void doAlterOperation(String database, String table, byte[] originData, String originSchema, JsonNode data,
            Map<SchemaChangeType, List<AlterColumn>> typeMap) {
        for (Map.Entry<SchemaChangeType, List<AlterColumn>> kv : typeMap.entrySet()) {
            try {
                switch (kv.getKey()) {
                    case ADD_COLUMN:
                        doAddColumn(kv.getValue(), TableIdentifier.of(database, table));
                        break;
                    case DROP_COLUMN:
                        doDropColumn(kv.getKey(), originSchema);
                        break;
                    case RENAME_COLUMN:
                        doRenameColumn(kv.getKey(), originSchema);
                        break;
                    case CHANGE_COLUMN_TYPE:
                        doChangeColumnType(kv.getKey(), originSchema);
                        break;
                    default:
                }
            } catch (Exception e) {
                if (exceptionPolicy == SchemaUpdateExceptionPolicy.THROW_WITH_STOP) {
                    throw new SchemaChangeHandleException(
                            String.format("Apply alter column failed, origin schema: %s", originSchema), e);
                }
                LOGGER.warn("Apply alter column failed, origin schema: {}", originSchema, e);
                handleDirtyData(data, originData, database, table, DirtyType.HANDLE_ALTER_TABLE_ERROR, e);
            }
        }
    }

    @Override
    public void doCreateTable(byte[] originData, String database, String table, SchemaChangeType type,
            String originSchema, JsonNode data, CreateTableOperation operation) {
        try {
            TableIdentifier tableId = TableIdentifier.of(database, table);
            List<String> pkListStr = dynamicSchemaFormat.extractPrimaryKeyNames(data);
            RowType rowType = dynamicSchemaFormat.extractSchema(data, pkListStr);
            Schema schema = FlinkSchemaUtil.convert(FlinkSchemaUtil.toSchema(rowType));
            IcebergSchemaChangeUtils.createTable(catalog, tableId, asNamespaceCatalog, schema);
            return;
        } catch (Exception e) {
            if (exceptionPolicy == SchemaUpdateExceptionPolicy.THROW_WITH_STOP) {
                throw new SchemaChangeHandleException(
                        String.format("Drop column failed, origin schema: %s", originSchema), e);
            }
            handleDirtyData(data, originData, database, table, DirtyType.CREATE_TABLE_ERROR, e);
            return;
        }
    }

    private void doAddColumn(List<AlterColumn> alterColumns, TableIdentifier tableId) {
        List<TableChange> tableChanges = new ArrayList<>();
        Table table = catalog.loadTable(tableId);
        Transaction transaction = table.newTransaction();

        alterColumns.forEach(alterColumn -> {
            Column column = alterColumn.getNewColumn();
            LogicalType dataType = dynamicSchemaFormat.sqlType2FlinkType(column.getJdbcType());
            TableChange.ColumnPosition position;
            if (column.getPosition() != null && column.getPosition().getColumnName() != null) {
                position =
                        column.getPosition().getPositionType() == PositionType.FIRST
                                ? TableChange.ColumnPosition.first()
                                : TableChange.ColumnPosition.after(column.getPosition().getColumnName());
            } else {
                List<Types.NestedField> columns = table.schema().columns();
                Types.NestedField lastField = columns.get(columns.size() - 1);
                position = TableChange.ColumnPosition.after(lastField.name());
            }
            TableChange.AddColumn addColumn = new TableChange.AddColumn(new String[]{column.getName()},
                    dataType, column.isNullable(), column.getComment(), position);
            tableChanges.add(addColumn);
        });
        IcebergSchemaChangeUtils.applySchemaChanges(transaction.updateSchema(), tableChanges);
        transaction.commitTransaction();
    }
}
