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

import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.schema.FunctionParameter;
import org.apache.calcite.schema.TableMacro;
import org.apache.calcite.schema.TranslatableTable;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.planner.logical.DrillTable;
import org.apache.drill.exec.planner.logical.DrillTranslatableTable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Implementation of a table macro that generates a table based on parameters.
 */
public class WithOptionsTableMacro implements TableMacro {

  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(WithOptionsTableMacro.class);

  private final TableSignature sig;
  private final Function<List<Object>, DrillTable> function;

  public WithOptionsTableMacro(TableSignature sig, Function<List<Object>, DrillTable> function) {
    this.sig = sig;
    this.function = function;
  }

  @Override
  public TranslatableTable apply(List<Object> arguments) {
    DrillTable drillTable = function.apply(arguments);
    if (drillTable == null) {
      throw UserException
        .validationError()
        .message("Unable to find table [%s]", sig.getName())
        .build(logger);
    }
    return drillTable instanceof TranslatableTable
        ? (TranslatableTable) drillTable :
        new DrillTranslatableTable(drillTable);
  }

  @Override
  public List<FunctionParameter> getParameters() {
    List<FunctionParameter> result = new ArrayList<>();
    for (int i = 0; i < sig.getParams().size(); i++) {
      final TableParamDef p = sig.getParams().get(i);
      final int ordinal = i;
      FunctionParameter functionParameter = new FunctionParameter() {
        @Override
        public int getOrdinal() {
          return ordinal;
        }

        @Override
        public String getName() {
          return p.getName();
        }

        @Override
        public RelDataType getType(RelDataTypeFactory typeFactory) {
          return typeFactory.createJavaType(p.getType());
        }

        @Override
        public boolean isOptional() {
          return p.isOptional();
        }
      };
      result.add(functionParameter);
    }
    return result;
  }
}
