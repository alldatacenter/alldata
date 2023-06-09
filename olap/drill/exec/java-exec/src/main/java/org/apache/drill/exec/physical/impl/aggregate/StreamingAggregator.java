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
package org.apache.drill.exec.physical.impl.aggregate;

import org.apache.drill.exec.compile.TemplateClassDefinition;
import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.ops.OperatorContext;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.RecordBatch.IterOutcome;

public interface StreamingAggregator {

  TemplateClassDefinition<StreamingAggregator> TEMPLATE_DEFINITION =
    new TemplateClassDefinition<StreamingAggregator>(StreamingAggregator.class, StreamingAggTemplate.class);


  /**
   * The Aggregator can return one of the following outcomes:
   * <p>
   * <b>RETURN_OUTCOME:</b> The aggregation has seen a change in the group and should send data downstream. If
   * complex writers are involved, then rebuild schema.
   * <p>
   * <b>CLEANUP_AND_RETURN:</b> End of all data. Return the data downstream, and cleanup.
   * <p>
   * <b>UPDATE_AGGREGATOR:</b> A schema change was encountered. The aggregator's generated  code and (possibly)
   * container need to be updated
   * <p>
   * <b>RETURN_AND_RESET:</b> If the aggregator encounters an EMIT, then that implies the end of a data set but
   * not of all the data. Return the data (aggregated so far) downstream, reset the internal state variables and
   * come back for the next data set.
   * <p>
   * @see org.apache.drill.exec.physical.impl.aggregate.HashAggregator.AggOutcome HashAggregator.AggOutcome
   */
  enum AggOutcome {
    RETURN_OUTCOME,
    CLEANUP_AND_RETURN,
    UPDATE_AGGREGATOR,
    RETURN_AND_RESET;
  }

  void setup(OperatorContext context, RecordBatch incoming, StreamingAggBatch outgoing, int outputRowCount)
    throws SchemaChangeException;

  IterOutcome getOutcome();

  int getOutputCount();

  // do the work. Also pass in the Iteroutcome of the batch already read in case it might be an EMIT. If the
  // outerOutcome is EMIT, we need to do the work without reading any more batches.
  AggOutcome doWork(IterOutcome outerOutcome);

  boolean isDone();

  void cleanup();

  boolean previousBatchProcessed();
}
