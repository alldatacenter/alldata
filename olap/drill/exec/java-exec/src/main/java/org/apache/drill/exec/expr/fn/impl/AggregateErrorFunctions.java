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
package org.apache.drill.exec.expr.fn.impl;

import org.apache.drill.exec.expr.DrillAggFunc;
import org.apache.drill.exec.expr.annotations.FunctionTemplate;
import org.apache.drill.exec.expr.annotations.Output;
import org.apache.drill.exec.expr.annotations.Param;
import org.apache.drill.exec.expr.annotations.Workspace;
import org.apache.drill.exec.expr.holders.BigIntHolder;
import org.apache.drill.exec.expr.holders.BitHolder;
import org.apache.drill.exec.expr.holders.NullableBitHolder;
import org.apache.drill.exec.expr.holders.NullableVarCharHolder;
import org.apache.drill.exec.expr.holders.VarCharHolder;

/*
 * TODO: For a handful of functions this approach of using function binding to detect that it is an invalid function is okay.
 * However moving forward we should introduce a validation phase after we learn the data types and before we try
 * to perform function resolution. Otherwise with implicit cast we will try to bind to an existing function.
 */
public class AggregateErrorFunctions {

  @FunctionTemplate(names = {"sum", "avg", "stddev_pop", "stddev_samp", "stddev", "var_pop",
      "var_samp", "variance"}, scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class BitAggregateErrorFunctions implements DrillAggFunc {

    @Param BitHolder in;
    @Workspace BigIntHolder value;
    @Output BigIntHolder out;

    public void setup() {
      if (true) {
        throw org.apache.drill.common.exceptions.UserException.unsupportedError()
          .message("Only COUNT aggregate function supported for Boolean type")
          .build();
      }
    }

    @Override
    public void add() {
    }

    @Override
    public void output() {
    }

    @Override
    public void reset() {
    }

  }

  @FunctionTemplate(names = {"sum", "avg", "stddev_pop", "stddev_samp", "stddev", "var_pop",
      "var_samp", "variance"}, scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class NullableBitAggregateErrorFunctions implements DrillAggFunc {

    @Param NullableBitHolder in;
    @Workspace BigIntHolder value;
    @Output BigIntHolder out;

    public void setup() {
      if (true) {
        throw org.apache.drill.common.exceptions.UserException.unsupportedError()
          .message("Only COUNT aggregate function supported for Boolean type")
          .build();
      }
    }

    @Override
    public void add() {
    }

    @Override
    public void output() {
    }

    @Override
    public void reset() {
    }
  }


  @FunctionTemplate(names = {"sum", "avg", "stddev_pop", "stddev_samp", "stddev", "var_pop", "var_samp", "variance"},
      scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class VarCharAggregateErrorFunctions implements DrillAggFunc {

    @Param VarCharHolder in;
    @Workspace BigIntHolder value;
    @Output BigIntHolder out;

    public void setup() {
      if (true) {
        throw org.apache.drill.common.exceptions.UserException.unsupportedError()
          .message("Only COUNT, MIN and MAX aggregate functions supported for VarChar type")
          .build();
      }
    }

    @Override
    public void add() {
    }

    @Override
    public void output() {
    }

    @Override
    public void reset() {
    }

  }

  @FunctionTemplate(names = {"sum", "avg", "stddev_pop", "stddev_samp", "stddev", "var_pop", "var_samp", "variance"},
      scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class NullableVarCharAggregateErrorFunctions implements DrillAggFunc {

    @Param NullableVarCharHolder in;
    @Workspace BigIntHolder value;
    @Output BigIntHolder out;

    public void setup() {
      if (true) {
        throw org.apache.drill.common.exceptions.UserException.unsupportedError()
          .message("Only COUNT, MIN and MAX aggregate functions supported for VarChar type")
          .build();
      }
    }

    @Override
    public void add() {
    }

    @Override
    public void output() {
    }

    @Override
    public void reset() {
    }
  }
}
