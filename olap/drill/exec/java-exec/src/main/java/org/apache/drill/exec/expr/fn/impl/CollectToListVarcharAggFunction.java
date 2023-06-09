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
import org.apache.drill.exec.expr.holders.NullableVarCharHolder;
import org.apache.drill.exec.expr.holders.ObjectHolder;
import org.apache.drill.exec.vector.complex.writer.BaseWriter;

/**
 * Aggregate function which collects incoming VarChar column values into the list.
 */
@FunctionTemplate(name = "collect_to_list_varchar",
                  scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE,
                  isInternal = true)
public class CollectToListVarcharAggFunction implements DrillAggFunc {

  @Param NullableVarCharHolder input;
  @Output BaseWriter.ComplexWriter writer;
  @Workspace ObjectHolder writerHolder;

  @Override
  public void setup() {
    writerHolder = new ObjectHolder();
  }

  @Override
  public void add() {
    org.apache.drill.exec.vector.complex.writer.BaseWriter.ListWriter listWriter;
    if (writerHolder.obj == null) {
      writerHolder.obj = writer.rootAsList();
    }

    listWriter = (org.apache.drill.exec.vector.complex.writer.BaseWriter.ListWriter) writerHolder.obj;

    if (input.isSet > 0) {
      listWriter.varChar().writeVarChar(input.start, input.end, input.buffer);
    }
  }

  @Override
  public void output() {
  }

  @Override
  public void reset() {
    writerHolder.obj = null;
  }
}
