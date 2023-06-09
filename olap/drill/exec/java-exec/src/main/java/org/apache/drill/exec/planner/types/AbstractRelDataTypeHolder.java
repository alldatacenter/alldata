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
package org.apache.drill.exec.planner.types;

import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.rel.type.RelDataTypeField;

import java.util.List;

/**
 * Base class-holder for the list of {@link RelDataTypeField}s.
 */
public abstract class AbstractRelDataTypeHolder {
  protected final List<RelDataTypeField> fields;
  protected RelDataTypeFactory typeFactory;

  public AbstractRelDataTypeHolder(List<RelDataTypeField> fields) {
    this.fields = Lists.newArrayList(fields);
  }

  /**
   * Returns RelDataTypeField field with specified name.
   */
  public abstract RelDataTypeField getField(RelDataTypeFactory typeFactory, String fieldName);

  /**
   * Returns list with all RelDataTypeField fields in this holder.
   */
  public List<RelDataTypeField> getFieldList(RelDataTypeFactory typeFactory) {
    return ImmutableList.copyOf(fields);
  }

  /**
   * Returns count of RelDataTypeField fields in this holder.
   */
  public int getFieldCount() {
    return fields.size();
  }

  /**
   * Returns list with names of RelDataTypeField fields.
   */
  public List<String> getFieldNames() {
    List<String> fieldNames = Lists.newArrayList();
    for(RelDataTypeField f : fields) {
      fieldNames.add(f.getName());
    }

    return fieldNames;
  }

  public void setRelDataTypeFactory(RelDataTypeFactory typeFactory) {
    this.typeFactory = typeFactory;
  }
}
