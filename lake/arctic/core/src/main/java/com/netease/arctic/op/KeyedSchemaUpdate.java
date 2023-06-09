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

package com.netease.arctic.op;

import com.netease.arctic.table.KeyedTable;
import com.netease.arctic.table.PrimaryKeySpec;
import org.apache.commons.lang3.StringUtils;
import org.apache.iceberg.Schema;
import org.apache.iceberg.UpdateSchema;
import org.apache.iceberg.relocated.com.google.common.base.Preconditions;
import org.apache.iceberg.types.Type;
import org.apache.iceberg.types.Types;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;

/**
 * Schema evolution API implementation for {@link KeyedTable}.
 */
public class KeyedSchemaUpdate implements UpdateSchema {
  private static final Logger LOG = LoggerFactory.getLogger(KeyedSchemaUpdate.class);

  public static final String DOT = ".";

  private final KeyedTable keyedTable;
  private final UpdateSchema baseTableUpdateSchema;
  private final UpdateSchema changeTableUpdateSchema;

  public KeyedSchemaUpdate(KeyedTable keyedTable) {
    this.keyedTable = keyedTable;
    baseTableUpdateSchema = keyedTable.baseTable().updateSchema();
    changeTableUpdateSchema = keyedTable.changeTable().updateSchema();
  }

  @Override
  public KeyedSchemaUpdate allowIncompatibleChanges() {
    baseTableUpdateSchema.allowIncompatibleChanges();
    changeTableUpdateSchema.allowIncompatibleChanges();
    return this;
  }

  @Override
  public UpdateSchema addColumn(String name, Type type, String doc) {
    baseTableUpdateSchema.addColumn(name, type, doc);
    changeTableUpdateSchema.addColumn(name, type, doc);
    return this;
  }

  @Override
  public UpdateSchema addColumn(String parent, String name, Type type, String doc) {
    baseTableUpdateSchema.addColumn(parent, name, type, doc);
    changeTableUpdateSchema.addColumn(parent, name, type, doc);
    return this;
  }

  @Override
  public UpdateSchema addRequiredColumn(String name, Type type, String doc) {
    baseTableUpdateSchema.addRequiredColumn(name, type, doc);
    changeTableUpdateSchema.addRequiredColumn(name, type, doc);
    return this;
  }

  @Override
  public UpdateSchema addRequiredColumn(String parent, String name, Type type, String doc) {
    baseTableUpdateSchema.addRequiredColumn(parent, name, type, doc);
    changeTableUpdateSchema.addRequiredColumn(parent, name, type, doc);
    return this;
  }

  @Override
  public UpdateSchema deleteColumn(String name) {
    Preconditions.checkArgument(!containsPk(name), "Cannot delete primary key. %s", name);
    baseTableUpdateSchema.deleteColumn(name);
    changeTableUpdateSchema.deleteColumn(name);
    return this;
  }

  @Override
  public UpdateSchema renameColumn(String name, String newName) {
    Preconditions.checkArgument(!containsPk(name), "Cannot rename primary key %s", name);
    baseTableUpdateSchema.renameColumn(name, newName);
    changeTableUpdateSchema.renameColumn(name, newName);
    return this;
  }

  @Override
  public UpdateSchema requireColumn(String name) {
    baseTableUpdateSchema.requireColumn(name);
    changeTableUpdateSchema.requireColumn(name);
    return this;
  }

  @Override
  public UpdateSchema makeColumnOptional(String name) {
    Preconditions.checkArgument(!containsPk(name), "Cannot make primary key optional. %s", name);
    baseTableUpdateSchema.makeColumnOptional(name);
    changeTableUpdateSchema.makeColumnOptional(name);
    return this;
  }

  @Override
  public UpdateSchema updateColumn(String name, Type.PrimitiveType newType) {
    baseTableUpdateSchema.updateColumn(name, newType);
    changeTableUpdateSchema.updateColumn(name, newType);
    return this;
  }

  @Override
  public UpdateSchema updateColumnDoc(String name, String doc) {
    baseTableUpdateSchema.updateColumnDoc(name, doc);
    changeTableUpdateSchema.updateColumnDoc(name, doc);
    return this;
  }

  @Override
  public UpdateSchema moveFirst(String name) {
    baseTableUpdateSchema.moveFirst(name);
    changeTableUpdateSchema.moveFirst(name);
    return this;
  }

  @Override
  public UpdateSchema moveBefore(String name, String beforeName) {
    baseTableUpdateSchema.moveBefore(name, beforeName);
    changeTableUpdateSchema.moveBefore(name, beforeName);
    return this;
  }

  @Override
  public UpdateSchema moveAfter(String name, String afterName) {
    baseTableUpdateSchema.moveAfter(name, afterName);
    changeTableUpdateSchema.moveAfter(name, afterName);
    return this;
  }

  @Override
  public UpdateSchema unionByNameWith(Schema newSchema) {
    baseTableUpdateSchema.unionByNameWith(newSchema);
    changeTableUpdateSchema.unionByNameWith(newSchema);
    return this;
  }

  @Override
  public UpdateSchema setIdentifierFields(Collection<String> names) {
    throw new UnsupportedOperationException("unsupported setIdentifierFields arctic table.");
  }

  /**
   * Apply the pending changes to the original schema and returns the result.
   * <p>
   * This does not result in a permanent update.
   *
   * @return the result Schema when all pending updates are applied
   */
  @Override
  public Schema apply() {
    syncSchema(keyedTable);
    Schema newSchema = baseTableUpdateSchema.apply();
    changeTableUpdateSchema.apply();

    return newSchema;
  }

  @Override
  public void commit() {
    baseTableUpdateSchema.commit();
    try {
      changeTableUpdateSchema.commit();
    } catch (Exception e) {
      LOG.warn("change table schema commit exception", e);
    }
  }

  private boolean containsPk(String name) {
    if (!keyedTable.primaryKeySpec().primaryKeyExisted()) {
      return false;
    }
    return keyedTable.primaryKeySpec().fieldNames().contains(name);
  }

  public static void syncSchema(KeyedTable keyedTable) {
    if (PrimaryKeySpec.noPrimaryKey().equals(keyedTable.primaryKeySpec())) {
      return;
    }

    int baseSchemaSize = keyedTable.baseTable().schemas().size();
    int changeSchemaSize = keyedTable.changeTable().schemas().size();
    if (baseSchemaSize <= changeSchemaSize) {
      return;
    }
    if (baseSchemaSize == changeSchemaSize + 1) {
      Schema newer = keyedTable.baseTable().schema();
      syncSchema(newer, keyedTable.changeTable().schema(), keyedTable.changeTable().updateSchema());
      return;
    }
    // just allow base table schema's versions are one more than change table's
    throw new IllegalStateException("base table have two more versions than change table");
  }

  private static void syncSchema(Schema newer, Schema old, UpdateSchema changeTableUs) {
    // To keep the order of adding columns with base table's
    PriorityQueue<Add> adds = new PriorityQueue<>();

    for (Types.NestedField newField : newer.columns()) {
      Types.NestedField oldField = old.findField(newField.fieldId());
      syncField(newField, oldField, changeTableUs, null, adds);
    }

    old.columns().forEach((c) -> {
      if (newer.findField(c.fieldId()) == null) {
        syncField(null, c, changeTableUs, null, adds);
      }
    });

    doAddColumns(adds, changeTableUs);
    LOG.info("sync schema to changeTable. from: {}, base: {}, actual: {}", old, newer, changeTableUs.apply());
    changeTableUs.commit();
  }

  private static void syncField(Types.NestedField newField,
      Types.NestedField oldField,
      UpdateSchema us,
      String fieldPrefix,
      Collection<Add> adds) {
    if (oldField == null && newField == null) {
      return;
    }
    if (oldField == null) {
      addColumnInternal(newField, fieldPrefix, adds);
      return;
    }
    if (newField == null) {
      deleteColumnInternal(oldField.name(), us, fieldPrefix);
      return;
    }

    if (Objects.equals(newField, oldField)) {
      return;
    }
    updateField(newField, oldField, us, fieldPrefix, adds);
  }

  private static void doAddColumns(PriorityQueue<Add> adds, UpdateSchema us) {
    while (!adds.isEmpty()) {
      Add add = adds.poll();
      if (StringUtils.isBlank(add.parent)) {
        us.addColumn(add.field, add.type, add.doc);
      } else {
        if (add.parent.contains(DOT)) {
          LOG.error("field: {}", add);
          throw new UnsupportedOperationException("do not support add deeper than two nested field");
        }
        us.addColumn(add.parent, add.field, add.type, add.doc);
      }
    }
  }

  private static void addColumnInternal(Types.NestedField field, String fieldPrefix, Collection<Add> adds) {
    adds.add(new Add(field, fieldPrefix));
  }

  private static void deleteColumnInternal(String field, UpdateSchema changeTableUs, String fieldPrefix) {
    changeTableUs.deleteColumn(getFullName(fieldPrefix, field));
  }

  private static String getFullName(String fieldPrefix, String field) {
    return StringUtils.isBlank(fieldPrefix) ? field : String.join(DOT, fieldPrefix, field);
  }

  private static void updateField(Types.NestedField newField,
      Types.NestedField oldField,
      UpdateSchema us,
      String fieldPrefix,
      Collection<Add> adds) {
    String oldFullFieldName = getFullName(fieldPrefix, oldField.name());
    if (!Objects.equals(newField.doc(), oldField.doc())) {
      us.updateColumnDoc(oldFullFieldName, newField.doc());
    }

    if (!Objects.equals(newField.isRequired(), oldField.isRequired())) {
      if (newField.isRequired()) {
        us.requireColumn(oldFullFieldName);
      } else {
        us.makeColumnOptional(oldFullFieldName);
      }
    }

    if (!Objects.equals(newField.name(), oldField.name())) {
      us.renameColumn(oldFullFieldName, newField.name());
    }

    if (newField.type().isPrimitiveType()) {
      updatePrimativeFieldType(newField, oldField, us, fieldPrefix);
    } else {
      updateNestedField(newField, oldField, us, fieldPrefix, adds);
    }
  }

  private static void updateNestedField(Types.NestedField newField,
      Types.NestedField oldField,
      UpdateSchema us,
      String fieldPrefix,
      Collection<Add> adds) {
    if (oldField.type().isMapType()) {
      updateMapField(newField, oldField, us, fieldPrefix, adds);
      return;
    }

    Type.NestedType newType = newField.type().asNestedType();
    Type.NestedType oldType = oldField.type().asNestedType();
    String prefix = getFullName(fieldPrefix, oldField.name());
    updateNestedField(newType, oldType, us, prefix, adds);
  }

  private static void updateNestedField(Type.NestedType newType,
      Type.NestedType oldType,
      UpdateSchema us,
      String fieldPrefix,
      Collection<Add> adds) {
    if (Objects.equals(newType, oldType)) {
      return;
    }
    newType.fields().forEach((field -> {
      Types.NestedField old = oldType.field(field.fieldId());
      syncField(field, old, us, fieldPrefix, adds);
    }));

    oldType.fields().forEach((o -> {
      // won't sync repeatedly
      if (newType.field(o.fieldId()) == null) {
        syncField(null, o, us, fieldPrefix, adds);
      }
    }));
  }

  private static void updateMapField(Types.NestedField newField,
      Types.NestedField oldField,
      UpdateSchema us,
      String fieldPrefix,
      Collection<Add> adds) {
    Types.MapType newType = newField.type().asMapType();
    Types.MapType oldType = oldField.type().asMapType();

    List<Types.NestedField> newFields = newType.fields();
    List<Types.NestedField> oldFields = oldType.fields();

    String crtPrefix = getFullName(fieldPrefix, oldField.name());
    for (int i = 0; i < newFields.size(); i++) {
      Types.NestedField newF = newFields.get(i);
      Types.NestedField oldF = oldFields.get(i);
      Type t = newF.type();
      // just support same type update
      if (t.isPrimitiveType()) {
        syncField(newF, oldF, us, crtPrefix, adds);
      } else {
        updateNestedField(newF.type().asNestedType(), oldF.type().asNestedType(), us, crtPrefix, adds);
      }
    }
  }

  private static void updatePrimativeFieldType(Types.NestedField newField,
      Types.NestedField oldField,
      UpdateSchema us,
      String fieldPrefix) {
    String fullName = getFullName(fieldPrefix, oldField.name());
    if (!Objects.equals(newField.type(), oldField.type())) {
      us.updateColumn(fullName, newField.type().asPrimitiveType());
    }
  }

  static class Add implements Comparable<Add>, Serializable {

    private final int baseFieldId;
    private final String parent;
    private final String field;
    private final Type type;
    private final String doc;

    public Add(Types.NestedField field, String parent) {
      this(field.fieldId(), parent, field.name(), field.type(), field.doc());
    }

    public Add(int baseFieldId, String parent, String field, Type type, String doc) {
      this.baseFieldId = baseFieldId;
      this.parent = parent;
      this.field = field;
      this.type = type;
      this.doc = doc;
    }

    @Override
    public int compareTo(@Nonnull Add o) {
      return this.baseFieldId - o.baseFieldId;
    }

    @Override
    public String toString() {
      return "Add{" +
          "baseFieldId=" + baseFieldId +
          ", parent='" + parent + '\'' +
          ", field='" + field + '\'' +
          ", type=" + type +
          ", doc='" + doc + '\'' +
          '}';
    }
  }

}