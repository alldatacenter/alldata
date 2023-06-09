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

package com.netease.arctic.trace;

import com.netease.arctic.table.KeyedTable;
import org.apache.iceberg.Schema;
import org.apache.iceberg.UpdateSchema;
import org.apache.iceberg.types.Type;

import java.util.Collection;

/**
 * Schema evolution API implementation for {@link KeyedTable}.
 */
public class TracedSchemaUpdate implements UpdateSchema {
  private final UpdateSchema updateSchema;
  private final TableTracer tracer;

  public TracedSchemaUpdate(UpdateSchema updateSchema, TableTracer tracer) {
    this.tracer = tracer;
    this.updateSchema = updateSchema;
  }

  @Override
  public TracedSchemaUpdate allowIncompatibleChanges() {
    updateSchema.allowIncompatibleChanges();
    return this;
  }

  @Override
  public UpdateSchema addColumn(String name, Type type, String doc) {
    updateSchema.addColumn(name, type, doc);
    TableTracer.UpdateColumn updateColumn = new TableTracer.UpdateColumn(name, null, type, doc,
        TableTracer.SchemaOperateType.ADD, null, null);
    tracer.updateColumn(updateColumn);
    return this;
  }

  @Override
  public UpdateSchema addColumn(String parent, String name, Type type, String doc) {
    updateSchema.addColumn(parent, name, type, doc);
    TableTracer.UpdateColumn updateColumn = new TableTracer.UpdateColumn(name, parent, type, doc,
        TableTracer.SchemaOperateType.ADD, false, null);
    tracer.updateColumn(updateColumn);
    return this;
  }

  @Override
  public UpdateSchema addRequiredColumn(String name, Type type, String doc) {
    updateSchema.addRequiredColumn(name, type, doc);
    TableTracer.UpdateColumn updateColumn = new TableTracer.UpdateColumn(name, null, type, doc,
        TableTracer.SchemaOperateType.ADD, false, null);
    tracer.updateColumn(updateColumn);
    return this;
  }

  @Override
  public UpdateSchema addRequiredColumn(String parent, String name, Type type, String doc) {
    updateSchema.addRequiredColumn(parent, name, type, doc);
    TableTracer.UpdateColumn updateColumn = new TableTracer.UpdateColumn(name, parent, type, doc,
        TableTracer.SchemaOperateType.ADD, false, null);
    tracer.updateColumn(updateColumn);
    return this;
  }

  @Override
  public UpdateSchema deleteColumn(String name) {
    updateSchema.deleteColumn(name);
    TableTracer.UpdateColumn updateColumn = new TableTracer.UpdateColumn(name, null, null, null,
        TableTracer.SchemaOperateType.DROP, null, null);
    tracer.updateColumn(updateColumn);
    return this;
  }

  @Override
  public UpdateSchema renameColumn(String name, String newName) {
    updateSchema.renameColumn(name, newName);
    TableTracer.UpdateColumn updateColumn = new TableTracer.UpdateColumn(name, null, null, null,
        TableTracer.SchemaOperateType.RENAME, null, newName);
    tracer.updateColumn(updateColumn);
    return this;
  }

  @Override
  public UpdateSchema requireColumn(String name) {
    updateSchema.requireColumn(name);
    TableTracer.UpdateColumn updateColumn = new TableTracer.UpdateColumn(name, null, null, null,
        TableTracer.SchemaOperateType.ALERT, false, null);
    tracer.updateColumn(updateColumn);
    return this;
  }

  @Override
  public UpdateSchema makeColumnOptional(String name) {
    updateSchema.makeColumnOptional(name);
    TableTracer.UpdateColumn updateColumn = new TableTracer.UpdateColumn(name, null, null, null,
        TableTracer.SchemaOperateType.ALERT, true, null);
    tracer.updateColumn(updateColumn);
    return this;
  }

  @Override
  public UpdateSchema updateColumn(String name, Type.PrimitiveType newType) {
    updateSchema.updateColumn(name, newType);
    TableTracer.UpdateColumn updateColumn = new TableTracer.UpdateColumn(name, null, newType, null,
        TableTracer.SchemaOperateType.ALERT, null, null);
    tracer.updateColumn(updateColumn);
    return this;
  }

  @Override
  public UpdateSchema updateColumnDoc(String name, String doc) {
    updateSchema.updateColumnDoc(name, doc);
    TableTracer.UpdateColumn updateColumn = new TableTracer.UpdateColumn(name, null, null, doc,
        TableTracer.SchemaOperateType.ALERT, null, null);
    tracer.updateColumn(updateColumn);
    return this;
  }

  @Override
  public UpdateSchema moveFirst(String name) {
    updateSchema.moveFirst(name);
    TableTracer.UpdateColumn updateColumn = new TableTracer.UpdateColumn(name, null, null, null,
        TableTracer.SchemaOperateType.MOVE_FIRST, null, null);
    tracer.updateColumn(updateColumn);
    return this;
  }

  @Override
  public UpdateSchema moveBefore(String name, String beforeName) {
    updateSchema.moveBefore(name, beforeName);
    TableTracer.UpdateColumn updateColumn = new TableTracer.UpdateColumn(name, null, null, null,
        TableTracer.SchemaOperateType.MOVE_BEFORE, null, beforeName);
    tracer.updateColumn(updateColumn);
    return this;
  }

  @Override
  public UpdateSchema moveAfter(String name, String afterName) {
    updateSchema.moveAfter(name, afterName);
    TableTracer.UpdateColumn updateColumn = new TableTracer.UpdateColumn(name, null, null, null,
        TableTracer.SchemaOperateType.MOVE_AFTER, null, afterName);
    tracer.updateColumn(updateColumn);
    return this;
  }

  @Override
  public UpdateSchema unionByNameWith(Schema newSchema) {
    updateSchema.unionByNameWith(newSchema);
    return this;
  }

  @Override
  public UpdateSchema setIdentifierFields(Collection<String> names) {
    throw new UnsupportedOperationException("unsupported setIdentifierFields arctic table.");
  }

  @Override
  public Schema apply() {
    return updateSchema.apply();
  }

  @Override
  public void commit() {
    updateSchema.commit();
    tracer.commit();
  }
}
