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

import org.apache.drill.exec.compile.TemplateClassDefinition;
import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.physical.impl.common.HashPartition;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.calcite.rel.core.JoinRelType;
import org.apache.drill.exec.record.VectorContainer;

public interface HashJoinProbe {
  TemplateClassDefinition<HashJoinProbe> TEMPLATE_DEFINITION = new TemplateClassDefinition<HashJoinProbe>(HashJoinProbe.class, HashJoinProbeTemplate.class);

  /* The probe side of the hash join can be in the following two states
   * 1. PROBE_PROJECT: Inner join case, we probe our hash table to see if we have a
   *    key match and if we do we project the record
   * 2. PROJECT_RIGHT: Right Outer or Full Outer joins where we are projecting the records
   *    from the build side that did not match any records on the probe side. For Left outer
   *    case we handle it internally by projecting the record if there isn't a match on the build side
   * 3. DONE: Once we have projected all possible records we are done
   */
  enum ProbeState {
    PROBE_PROJECT, PROJECT_RIGHT, DONE
  }

  void setupHashJoinProbe(RecordBatch probeBatch, HashJoinBatch outgoing, JoinRelType joinRelType, boolean semiJoin,
                          RecordBatch.IterOutcome leftStartState, HashPartition[] partitions, int cycleNum,
                          VectorContainer container, HashJoinBatch.HashJoinSpilledPartition[] spilledInners,
                          boolean buildSideIsEmpty, int numPartitions, int rightHVColPosition);
  int  probeAndProject() throws SchemaChangeException;
  void changeToFinalProbeState();
  void setTargetOutputCount(int targetOutputCount);
  int getOutputCount();
}
