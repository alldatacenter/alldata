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

import java.util.ArrayList;
import java.util.List;

import org.apache.calcite.rel.type.DynamicRecordType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.apache.calcite.rel.type.RelDataTypeFieldImpl;
import org.apache.calcite.sql.type.SqlTypeName;

import org.apache.drill.common.expression.SchemaPath;

public class RelDataTypeHolder extends AbstractRelDataTypeHolder {

  public RelDataTypeHolder() {
    super(new ArrayList<>());
  }

  public List<RelDataTypeField> getFieldList(RelDataTypeFactory typeFactory) {
    addStarIfEmpty(typeFactory);
    return fields;
  }

  public int getFieldCount() {
    addStarIfEmpty(this.typeFactory);
    return fields.size();
  }

  private void addStarIfEmpty(RelDataTypeFactory typeFactory) {
    if (fields.isEmpty()) {
      getField(typeFactory, SchemaPath.DYNAMIC_STAR);
    }
  }

  public RelDataTypeField getField(RelDataTypeFactory typeFactory, String fieldName) {

    /* First check if this field name exists in our field list */
    for (RelDataTypeField f : fields) {
      if (fieldName.equalsIgnoreCase(f.getName())) {
        return f;
      }
    }

    /* This field does not exist in our field list add it */
    final SqlTypeName typeName = DynamicRecordType.isDynamicStarColName(fieldName)
        ? SqlTypeName.DYNAMIC_STAR : SqlTypeName.ANY;

    // This field does not exist in our field list add it
    RelDataTypeField newField = new RelDataTypeFieldImpl(
        fieldName,
        fields.size(),
        typeFactory.createTypeWithNullability(typeFactory.createSqlType(typeName), true));

    /* Add the name to our list of field names */
    fields.add(newField);

    return newField;
  }
}
