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
/*
 * This class is automatically generated from AggrTypeFunctions2.tdd using FreeMarker.
 */

package org.apache.drill.exec.expr.fn.impl;

import org.apache.drill.exec.expr.DrillAggFunc;
import org.apache.drill.exec.expr.annotations.FunctionTemplate;
import org.apache.drill.exec.expr.annotations.Output;
import org.apache.drill.exec.expr.annotations.Param;
import org.apache.drill.exec.expr.annotations.Workspace;
import org.apache.drill.exec.expr.holders.BitHolder;
import org.apache.drill.exec.expr.holders.NullableBitHolder;

@SuppressWarnings("unused")
public class BooleanAggrFunctions {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(BooleanAggrFunctions.class);


@FunctionTemplate(name = "bool_or", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
public static class BitBooleanOr implements DrillAggFunc{

  @Param BitHolder in;
  @Workspace BitHolder inter;
  @Output BitHolder out;

  public void setup() {
  inter = new BitHolder();

    // Initialize the workspace variables
    inter.value = 0;
  }

  @Override
  public void add() {
    inter.value = inter.value | in.value;
  }

  @Override
  public void output() {
    out.value = inter.value;
  }

  @Override
  public void reset() {
    inter.value = 0;
  }
}



@FunctionTemplate(name = "bool_or", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
public static class NullableBitBooleanOr implements DrillAggFunc{

  @Param NullableBitHolder in;
  @Workspace BitHolder inter;
  @Output BitHolder out;

  public void setup() {
  inter = new BitHolder();

    // Initialize the workspace variables
    inter.value = 0;
  }

  @Override
  public void add() {
    sout: {
    if (in.isSet == 0) {
     // processing nullable input and the value is null, so don't do anything...
     break sout;
    }

    inter.value = inter.value | in.value;
    } // end of sout block
  }


  @Override
  public void output() {
    out.value = inter.value;
  }

  @Override
  public void reset() {
    inter.value = 0;
  }
}


@FunctionTemplate(name = "bool_and", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
public static class BitBooleanAnd implements DrillAggFunc{

  @Param BitHolder in;
  @Workspace BitHolder inter;
  @Output BitHolder out;

  public void setup() {
  inter = new BitHolder();

    // Initialize the workspace variables
    inter.value = Integer.MAX_VALUE;
  }

  @Override
  public void add() {

    inter.value = inter.value & in.value;

  }

  @Override
  public void output() {
    out.value = inter.value;
  }

  @Override
  public void reset() {
    inter.value = Integer.MAX_VALUE;
  }
}


@FunctionTemplate(name = "bool_and", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
public static class NullableBitBooleanAnd implements DrillAggFunc{

  @Param NullableBitHolder in;
  @Workspace BitHolder inter;
  @Output BitHolder out;

  public void setup() {
  inter = new BitHolder();

    // Initialize the workspace variables
    inter.value = Integer.MAX_VALUE;
  }

  @Override
  public void add() {
    sout: {
    if (in.isSet == 0) {
     // processing nullable input and the value is null, so don't do anything...
     break sout;
    }

    inter.value = inter.value & in.value;

    } // end of sout block
  }


  @Override
  public void output() {
    out.value = inter.value;
  }

  @Override
  public void reset() {
    inter.value = Integer.MAX_VALUE;
  }
}

}