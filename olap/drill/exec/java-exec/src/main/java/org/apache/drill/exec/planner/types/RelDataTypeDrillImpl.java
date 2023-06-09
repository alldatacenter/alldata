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

import java.util.Collections;
import java.util.List;

import org.apache.calcite.rel.type.DynamicRecordType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.rel.type.RelDataTypeFamily;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.apache.calcite.rel.type.RelDataTypePrecedenceList;
import org.apache.calcite.sql.type.SqlTypeExplicitPrecedenceList;
import org.apache.calcite.sql.type.SqlTypeName;

/* We use an instance of this class as the dynamic row type for
 * Drill table. Since we don't know the schema before hand
 * whenever optiq requires us to validate that a field exists
 * we always return true and indicate that the type of that
 * field is 'ANY'
 */
public class RelDataTypeDrillImpl extends DynamicRecordType {

    private final RelDataTypeFactory typeFactory;
    private final AbstractRelDataTypeHolder holder;

    public RelDataTypeDrillImpl(AbstractRelDataTypeHolder holder, RelDataTypeFactory typeFactory) {
        this.typeFactory = typeFactory;
        this.holder = holder;
        this.holder.setRelDataTypeFactory(typeFactory);
        computeDigest();
    }

    @Override
    public List<RelDataTypeField> getFieldList() {
      return holder.getFieldList(typeFactory);
    }

    @Override
    public int getFieldCount() {
      return holder.getFieldCount();
    }

    @Override
    public RelDataTypeField getField(String fieldName, boolean caseSensitive, boolean elideRecord) {
      return holder.getField(typeFactory, fieldName);
    }

    @Override
    public List<String> getFieldNames() {
      return holder.getFieldNames();
    }

    @Override
    public SqlTypeName getSqlTypeName() {
        return SqlTypeName.ANY;
    }

    @Override
    public RelDataTypePrecedenceList getPrecedenceList() {
      return new SqlTypeExplicitPrecedenceList(Collections.<SqlTypeName>emptyList());
    }

    @Override
    protected void generateTypeString(StringBuilder sb, boolean withDetail) {
       sb.append("(DrillRecordRow" + getFieldNames() + ")");
    }

    @Override
    public boolean isStruct() {
        return true;
    }

    @Override
    public int hashCode() {
      return holder == null ? 0 : holder.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (!super.equals(obj)) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      RelDataTypeDrillImpl other = (RelDataTypeDrillImpl) obj;
      if (holder == null) {
        if (other.holder != null) {
          return false;
        }
      } else if (!holder.equals(other.holder)) {
        return false;
      }
      return true;
    }

    @Override
    public RelDataTypeFamily getFamily() {
      return getSqlTypeName().getFamily();
    }
}
