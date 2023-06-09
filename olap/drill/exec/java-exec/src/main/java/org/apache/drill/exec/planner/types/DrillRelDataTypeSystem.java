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

import org.apache.calcite.rel.type.RelDataTypeSystem;
import org.apache.calcite.rel.type.RelDataTypeSystemImpl;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.drill.common.types.Types;

public class DrillRelDataTypeSystem extends RelDataTypeSystemImpl {

  public static final RelDataTypeSystem DRILL_REL_DATATYPE_SYSTEM = new DrillRelDataTypeSystem();

  @Override
  public int getDefaultPrecision(SqlTypeName typeName) {
    switch (typeName) {
      case CHAR:
      case BINARY:
      case VARCHAR:
      case VARBINARY:
        return Types.MAX_VARCHAR_LENGTH;
      default:
        return super.getDefaultPrecision(typeName);
    }
  }

  @Override
  public int getMaxNumericScale() {
    return 38;
  }

  @Override
  public int getMaxNumericPrecision() {
    return 38;
  }

  @Override
  public boolean isSchemaCaseSensitive() {
    // Drill uses case-insensitive policy
    return false;
  }

}
