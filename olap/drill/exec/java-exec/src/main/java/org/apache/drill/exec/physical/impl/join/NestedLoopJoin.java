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
package org.apache.drill.exec.physical.impl.join;

import org.apache.calcite.rel.core.JoinRelType;
import org.apache.drill.exec.compile.TemplateClassDefinition;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.record.ExpandableHyperContainer;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.VectorContainer;

import java.util.LinkedList;

/**
 * Interface for the nested loop join operator.
 */
public interface NestedLoopJoin {
  public static TemplateClassDefinition<NestedLoopJoin> TEMPLATE_DEFINITION =
      new TemplateClassDefinition<>(NestedLoopJoin.class, NestedLoopJoinTemplate.class);

  public void setupNestedLoopJoin(FragmentContext context, RecordBatch left,
                                  ExpandableHyperContainer rightContainer,
                                  LinkedList<Integer> rightCounts,
                                  NestedLoopJoinBatch outgoing);

  void setTargetOutputCount(int targetOutputCount);

  // Produce output records taking into account join type
  public int outputRecords(JoinRelType joinType);

  // Project the record at offset 'leftIndex' in the left input batch into the output container at offset 'outIndex'
  public void emitLeft(int leftIndex, int outIndex);

  // Project the record from the hyper container given the batch index and the record within the batch at 'outIndex'
  public void emitRight(int batchIndex, int recordIndexWithinBatch, int outIndex);

  // Setup the input/output value vector references
  public void doSetup(FragmentContext context, VectorContainer rightContainer, RecordBatch leftBatch, RecordBatch outgoing);
}
