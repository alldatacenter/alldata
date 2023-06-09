/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.store.table.function;

import org.apache.drill.exec.planner.logical.DrillTable;

import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * Definition of table parameters, contains parameter name, class type, type status (optional / required).
 * May also contain action this parameter can perform for the given value and table.
 */
public final class TableParamDef {

  private final String name;
  private final Class<?> type;
  private final boolean optional;
  private final BiConsumer<DrillTable, Object> action;

  public static TableParamDef required(String name, Class<?> type, BiConsumer<DrillTable, Object> action) {
    return new TableParamDef(name, type, false, action);
  }

  public static TableParamDef optional(String name, Class<?> type, BiConsumer<DrillTable, Object> action) {
    return new TableParamDef(name, type, true, action);
  }

  private TableParamDef(String name, Class<?> type, boolean optional, BiConsumer<DrillTable, Object> action) {
    this.name = name;
    this.type = type;
    this.optional = optional;
    this.action = action;
  }

  public String getName() {
    return name;
  }

  public Class<?> getType() {
    return type;
  }

  public boolean isOptional() {
    return optional;
  }

  public void apply(DrillTable drillTable, Object value) {
    if (action == null) {
      return;
    }
    action.accept(drillTable, value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, type, optional, action);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TableParamDef that = (TableParamDef) o;
    return optional == that.optional
      && Objects.equals(name, that.name)
      && Objects.equals(type, that.type)
      && Objects.equals(action, that.action);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    if (optional) {
      builder.append("[");
    }
    builder.append(name).append(": ").append(type);
    if (action != null) {
      builder.append(" (with action)");
    }
    if (optional) {
      builder.append("]");
    }
    return builder.toString();
  }
}
