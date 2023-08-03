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

package org.apache.inlong.sort.iceberg.sink.multiple;

import org.apache.inlong.sort.iceberg.FlinkTypeToType;
import org.apache.inlong.sort.schema.TableChange;
import org.apache.inlong.sort.util.SchemaChangeUtils;

import org.apache.flink.table.types.logical.RowType;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.UpdateSchema;
import org.apache.iceberg.catalog.Catalog;
import org.apache.iceberg.catalog.SupportsNamespaces;
import org.apache.iceberg.catalog.TableIdentifier;
import org.apache.iceberg.exceptions.AlreadyExistsException;
import org.apache.iceberg.relocated.com.google.common.base.Joiner;
import org.apache.iceberg.relocated.com.google.common.base.Preconditions;
import org.apache.iceberg.relocated.com.google.common.collect.ImmutableMap;
import org.apache.iceberg.types.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

public class IcebergSchemaChangeUtils extends SchemaChangeUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(IcebergSchemaChangeUtils.class);

    private static final Joiner DOT = Joiner.on(".");

    public static void createTable(Catalog catalog, TableIdentifier tableId, SupportsNamespaces asNamespaceCatalog,
            Schema schema) {
        if (!catalog.tableExists(tableId)) {
            if (asNamespaceCatalog != null && !asNamespaceCatalog.namespaceExists(tableId.namespace())) {
                try {
                    asNamespaceCatalog.createNamespace(tableId.namespace());
                    LOGGER.info("Auto create Database({}) in Catalog({}).", tableId.namespace(), catalog.name());
                } catch (AlreadyExistsException e) {
                    LOGGER.warn("Database({}) already exist in Catalog({})!", tableId.namespace(), catalog.name());
                }
            }
            ImmutableMap.Builder<String, String> properties = ImmutableMap.builder();
            properties.put("format-version", "2");
            properties.put("write.upsert.enabled", "true");
            properties.put("write.metadata.metrics.default", "full");
            // for hive visible
            properties.put("engine.hive.enabled", "true");
            try {
                catalog.createTable(tableId, schema, PartitionSpec.unpartitioned(), properties.build());
                LOGGER.info("Auto create Table({}) in Database({}) in Catalog({})!",
                        tableId.name(), tableId.namespace(), catalog.name());
            } catch (AlreadyExistsException e) {
                LOGGER.warn("Table({}) already exist in Database({}) in Catalog({})!",
                        tableId.name(), tableId.namespace(), catalog.name());
            }
        }
    }

    public static void applySchemaChanges(UpdateSchema pendingUpdate, List<TableChange> tableChanges) {
        for (TableChange change : tableChanges) {
            if (change instanceof TableChange.AddColumn) {
                applyAddColumn(pendingUpdate, (TableChange.AddColumn) change);
            } else if (change instanceof TableChange.DeleteColumn) {
                applyDeleteColumn(pendingUpdate, (TableChange.DeleteColumn) change);
            } else if (change instanceof TableChange.UpdateColumn) {
                applyUpdateColumn(pendingUpdate, (TableChange.UpdateColumn) change);
            } else {
                throw new UnsupportedOperationException("Cannot apply unknown table change: " + change);
            }
        }
        pendingUpdate.commit();
    }

    public static void applyAddColumn(UpdateSchema pendingUpdate, TableChange.AddColumn add) {
        Preconditions.checkArgument(add.isNullable(),
                "Incompatible change: cannot add required column: %s", leafName(add.fieldNames()));
        Type type = add.dataType().accept(new FlinkTypeToType(RowType.of(add.dataType())));
        pendingUpdate.addColumn(parentName(add.fieldNames()), leafName(add.fieldNames()), type, add.comment());

        if (add.position() instanceof TableChange.After) {
            TableChange.After after = (TableChange.After) add.position();
            String referenceField = peerName(add.fieldNames(), after.column());
            pendingUpdate.moveAfter(DOT.join(add.fieldNames()), referenceField);

        } else if (add.position() instanceof TableChange.First) {
            pendingUpdate.moveFirst(DOT.join(add.fieldNames()));

        } else {
            Preconditions.checkArgument(add.position() == null,
                    "Cannot add '%s' at unknown position: %s", DOT.join(add.fieldNames()), add.position());
        }
    }

    public static void applyDeleteColumn(UpdateSchema pendingUpdate, TableChange.DeleteColumn delete) {
        pendingUpdate.deleteColumn(DOT.join(delete.fieldNames()));
    }

    public static void applyUpdateColumn(UpdateSchema pendingUpdate, TableChange.UpdateColumn update) {
        Type type = update.dataType().accept(new FlinkTypeToType(RowType.of(update.dataType())));
        pendingUpdate.updateColumn(DOT.join(update.fieldNames()), type.asPrimitiveType(), update.comment());
    }

    public static String leafName(String[] fieldNames) {
        Preconditions.checkArgument(fieldNames.length > 0, "Invalid field name: at least one name is required");
        return fieldNames[fieldNames.length - 1];
    }

    public static String peerName(String[] fieldNames, String fieldName) {
        if (fieldNames.length > 1) {
            String[] peerNames = Arrays.copyOf(fieldNames, fieldNames.length);
            peerNames[fieldNames.length - 1] = fieldName;
            return DOT.join(peerNames);
        }
        return fieldName;
    }

    public static String parentName(String[] fieldNames) {
        if (fieldNames.length > 1) {
            return DOT.join(Arrays.copyOfRange(fieldNames, 0, fieldNames.length - 1));
        }
        return null;
    }
}
