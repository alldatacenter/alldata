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

import org.apache.flink.table.types.logical.RowType;
import org.apache.iceberg.Schema;
import org.apache.iceberg.UpdateSchema;
import org.apache.iceberg.flink.FlinkSchemaUtil;
import org.apache.iceberg.relocated.com.google.common.base.Joiner;
import org.apache.iceberg.relocated.com.google.common.base.Preconditions;
import org.apache.iceberg.types.Type;
import org.apache.iceberg.types.Types.NestedField;
import org.apache.inlong.sort.base.sink.TableChange;
import org.apache.inlong.sort.iceberg.FlinkTypeToType;
import org.apache.inlong.sort.base.sink.TableChange.AddColumn;
import org.apache.inlong.sort.base.sink.TableChange.ColumnPosition;
import org.apache.inlong.sort.base.sink.TableChange.UnknownColumnChange;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SchemaChangeUtils {

    private static final Joiner DOT = Joiner.on(".");

    /**
     * Compare two schemas and get the schema changes that happened in them.
     * TODO: currently only support add column
     *
     * @param oldSchema
     * @param newSchema
     * @return
     */
    static List<TableChange> diffSchema(Schema oldSchema, Schema newSchema) {
        List<String> oldFields = oldSchema.columns().stream().map(NestedField::name).collect(Collectors.toList());
        List<String> newFields = newSchema.columns().stream().map(NestedField::name).collect(Collectors.toList());
        int oi = 0;
        int ni = 0;
        List<TableChange> tableChanges = new ArrayList<>();
        while (ni < newFields.size()) {
            if (oi < oldFields.size() && oldFields.get(oi).equals(newFields.get(ni))) {
                oi++;
                ni++;
            } else {
                NestedField newField = newSchema.findField(newFields.get(ni));
                tableChanges.add(
                        new AddColumn(
                                new String[]{newField.name()},
                                FlinkSchemaUtil.convert(newField.type()),
                                !newField.isRequired(),
                                newField.doc(),
                                ni == 0 ? ColumnPosition.first() : ColumnPosition.after(newFields.get(ni - 1))));
                ni++;
            }
        }

        if (oi != oldFields.size()) {
            tableChanges.clear();
            tableChanges.add(
                    new UnknownColumnChange(
                            String.format("Unsupported schema update.\n"
                                    + "oldSchema:\n%s\n, newSchema:\n %s", oldSchema, newSchema)));
        }

        return tableChanges;
    }

    public static void applySchemaChanges(UpdateSchema pendingUpdate, List<TableChange> tableChanges) {
        for (TableChange change : tableChanges) {
            if (change instanceof TableChange.AddColumn) {
                apply(pendingUpdate, (TableChange.AddColumn) change);
            } else {
                throw new UnsupportedOperationException("Cannot apply unknown table change: " + change);
            }
        }
        pendingUpdate.commit();
    }

    public static void apply(UpdateSchema pendingUpdate, TableChange.AddColumn add) {
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
