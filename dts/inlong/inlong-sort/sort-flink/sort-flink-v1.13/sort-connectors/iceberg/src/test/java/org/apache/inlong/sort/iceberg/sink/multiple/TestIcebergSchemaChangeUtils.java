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

import org.apache.inlong.sort.schema.TableChange;

import org.apache.iceberg.Schema;
import org.apache.iceberg.types.Types;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.apache.inlong.sort.iceberg.sink.multiple.DynamicSchemaHandleOperator.extractColumnSchema;

public class TestIcebergSchemaChangeUtils {

    private Schema baseSchema;

    @Before
    public void setup() {
        baseSchema = new Schema(
                Types.NestedField.required(1, "id", Types.LongType.get(), "primary key"),
                Types.NestedField.optional(2, "name", Types.StringType.get(), "name"),
                Types.NestedField.optional(3, "age", Types.IntegerType.get(), "age"));
    }

    @Test
    public void testAddColumn() {
        List<TableChange> tableChanges = addColumn();
        Assert.assertEquals("Table changes size must be 2 when add  2 column", 2, tableChanges.size());
        for (TableChange tableChange : tableChanges) {
            Assert.assertTrue("The table change must be AddColumn ", tableChange instanceof TableChange.AddColumn);
        }
    }

    public List<TableChange> addColumn() {
        Schema addColSchema = new Schema(
                Types.NestedField.required(1, "id", Types.LongType.get(), "primary key"),
                Types.NestedField.optional(2, "name", Types.StringType.get(), "name"),
                Types.NestedField.optional(3, "age", Types.IntegerType.get(), "age"),
                Types.NestedField.optional(4, "city", Types.StringType.get(), "city"),
                Types.NestedField.optional(5, "sex", Types.StringType.get(), "sex"));

        Assert.assertFalse(baseSchema.sameSchema(addColSchema));

        return IcebergSchemaChangeUtils.diffSchema(extractColumnSchema(baseSchema), extractColumnSchema(addColSchema));
    }

    @Test
    public void testDeleteColumn() {
        List<TableChange> tableChanges = deleteColumn();
        Assert.assertEquals("Table changes size must be 1 when del 1 column", 1, tableChanges.size());
        for (TableChange tableChange : tableChanges) {
            Assert.assertTrue("The table change must be DeleteColumn ",
                    tableChange instanceof TableChange.DeleteColumn);
        }
    }

    public List<TableChange> deleteColumn() {
        Schema delColSchema = new Schema(
                Types.NestedField.required(1, "id", Types.LongType.get(), "primary key"),
                Types.NestedField.optional(2, "name", Types.StringType.get(), "name"));

        Assert.assertFalse(baseSchema.sameSchema(delColSchema));

        return IcebergSchemaChangeUtils.diffSchema(extractColumnSchema(baseSchema), extractColumnSchema(delColSchema));
    }

    @Test
    public void testUpdateColumn() {
        List<TableChange> updateTypeTableChanges = testUpdateTypeColumn();
        Assert.assertEquals("update 2 col, id: int -> double; age: int -> long", 2, updateTypeTableChanges.size());
        for (TableChange tableChange : updateTypeTableChanges) {
            Assert.assertTrue("The table changes must be UpdateColumn ",
                    tableChange instanceof TableChange.UpdateColumn);
        }

        List<TableChange> updateCommentTableChanges = testCommentTypeColumn();
        Assert.assertEquals("update 1 col comment, name comment: name -> family name:", 1,
                updateCommentTableChanges.size());
        for (TableChange tableChange : updateCommentTableChanges) {
            Assert.assertTrue("The table changes must be UpdateColumn ",
                    tableChange instanceof TableChange.UpdateColumn);
        }
    }

    public List<TableChange> testUpdateTypeColumn() {
        Schema updateTypeColSchema = new Schema(
                Types.NestedField.required(1, "id", Types.DoubleType.get(), "primary key"),
                Types.NestedField.optional(2, "name", Types.StringType.get(), "name"),
                Types.NestedField.optional(3, "age", Types.LongType.get(), "age"));

        Assert.assertFalse(baseSchema.sameSchema(updateTypeColSchema));

        return IcebergSchemaChangeUtils.diffSchema(extractColumnSchema(baseSchema),
                extractColumnSchema(updateTypeColSchema));
    }

    public List<TableChange> testCommentTypeColumn() {
        Schema updateCommentColSchema = new Schema(
                Types.NestedField.required(1, "id", Types.LongType.get(), "primary key"),
                Types.NestedField.optional(2, "name", Types.StringType.get(), "family_name"),
                Types.NestedField.optional(3, "age", Types.IntegerType.get(), "age"));

        Assert.assertFalse(baseSchema.sameSchema(updateCommentColSchema));

        return IcebergSchemaChangeUtils.diffSchema(extractColumnSchema(baseSchema),
                extractColumnSchema(updateCommentColSchema));
    }

    @Test
    public void testRenameColumn() {
        Schema renameColumnSchema = new Schema(
                Types.NestedField.required(1, "id", Types.LongType.get(), "primary key"),
                Types.NestedField.optional(2, "family_name", Types.StringType.get(), "name"),
                Types.NestedField.optional(3, "age", Types.IntegerType.get(), "age"));

        Assert.assertFalse(baseSchema.sameSchema(renameColumnSchema));

        List<TableChange> tableChanges = IcebergSchemaChangeUtils.diffSchema(extractColumnSchema(baseSchema),
                extractColumnSchema(renameColumnSchema));
        Assert.assertEquals("rename column is not supported.", 1, tableChanges.size());
        for (TableChange tableChange : tableChanges) {
            Assert.assertTrue("The table changes must be UnknownColumnChange ",
                    tableChange instanceof TableChange.UnknownColumnChange);
        }
    }

    @Test
    public void testColumnPositionChange() {
        Schema positionChangeSchema = new Schema(
                Types.NestedField.required(1, "id", Types.LongType.get(), "primary key"),
                Types.NestedField.optional(2, "age", Types.StringType.get(), "age"),
                Types.NestedField.optional(3, "name", Types.IntegerType.get(), "name"));

        Assert.assertFalse(baseSchema.sameSchema(positionChangeSchema));

        List<TableChange> tableChanges = IcebergSchemaChangeUtils.diffSchema(extractColumnSchema(baseSchema),
                extractColumnSchema(positionChangeSchema));
        Assert.assertTrue(tableChanges.size() == 1);
        for (TableChange tableChange : tableChanges) {
            Assert.assertTrue("The table changes must be UnknownColumnChange ",
                    tableChange instanceof TableChange.UnknownColumnChange);
        }
    }
}
