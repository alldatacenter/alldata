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
package org.apache.drill.exec.physical.impl.unnest;

import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.ops.OperatorContext;
import org.apache.drill.exec.physical.base.LateralContract;
import org.apache.drill.exec.physical.impl.sort.RecordBatchData;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.record.CloseableRecordBatch;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.TypedFieldId;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.VectorWrapper;
import org.apache.drill.exec.record.WritableBatch;
import org.apache.drill.exec.record.selection.SelectionVector2;
import org.apache.drill.exec.record.selection.SelectionVector4;
import org.apache.drill.exec.vector.ValueVector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A mock lateral join implementation for testing unnest. This ignores all the other input and
 * simply puts the unnest output into a results hypervector.
 * Since Unnest returns an empty batch when it encounters a schema change, this implementation
 * will also output an empty batch when it sees a schema change from unnest
 */
public class MockLateralJoinBatch implements LateralContract, CloseableRecordBatch {

  private final RecordBatch incoming;

  private int recordIndex = 0;
  private RecordBatch unnest;
  private int unnestLimit = -1; // Unnest will EMIT if the number of records cross this limit

  private boolean isDone;
  private IterOutcome currentLeftOutcome = IterOutcome.NOT_YET;

  private final FragmentContext context;
  private  final OperatorContext oContext;

  private final List<ValueVector> resultList = new ArrayList<>();

  public MockLateralJoinBatch(FragmentContext context, OperatorContext oContext, RecordBatch incoming) {
    this.context = context;
    this.oContext = oContext;
    this.incoming = incoming;
    this.isDone = false;
  }

  @Override public RecordBatch getIncoming() {
    return incoming; // don't need this
  }

  @Override public int getRecordIndex() {
    return recordIndex;
  }

  @Override public IterOutcome getLeftOutcome() {
    return currentLeftOutcome;
  }

  public void moveToNextRecord() {
    recordIndex++;
  }

  public void reset() {
    recordIndex = 0;
  }

  public void setUnnest(RecordBatch unnest){
    this.unnest = unnest;
  }

  public void setUnnestLimit(int limit){
    this.unnestLimit = limit;
  }

  public RecordBatch getUnnest() {
    return unnest;
  }

  @Override
  public IterOutcome next() {

    IterOutcome currentOutcome = incoming.next();
    currentLeftOutcome = currentOutcome;
    recordIndex = 0;

    switch (currentOutcome) {
      case OK_NEW_SCHEMA:
        // Nothing to do for this.
      case OK:
        IterOutcome outcome;
        // consume all the outout from unnest until EMIT or end of
        // incoming data
        int unnestCount = 0; // number of values unnested by current iteration
        while (true) {
          outcome = unnest.next();
          if (outcome == IterOutcome.OK_NEW_SCHEMA) {
            // setup schema does nothing (this is just a place holder)
            setupSchema();
            // however unnest is also expected to return an empty batch
            // which we will add to our output
          }
          // We put each batch output from unnest into a hypervector
          // the calling test can match this against the baseline
          unnestCount += addBatchToHyperContainer(unnest);
          if (outcome == IterOutcome.EMIT) {
            // reset unnest count
            unnestCount = 0;
            break;
          }
          // Pretend that an operator somewhere between lateral and unnest
          // wants to terminate processing of the record.
          if(unnestLimit > 0 && unnestCount >= unnestLimit) {
            // break here rather than sending kill to unnest since with partitionLimitBatch kill will never be
            // sent to unnest from subquery
            break;
          }
        }
        return currentOutcome;
      case NONE:
        isDone = true;
        return currentOutcome;
      case NOT_YET:
        return currentOutcome;
      default:
        throw new UnsupportedOperationException("This state is not supported");
    }
  }

  @Override public WritableBatch getWritableBatch() {
    return null;
  }

  public List<ValueVector> getResultList() {
    return resultList;
  }

  @Override
  public void close() throws Exception {

  }

  @Override
  public void dump() { }

  @Override
  public int getRecordCount() {
    return 0;
  }

  @Override
  public SelectionVector2 getSelectionVector2() {
    return null;
  }

  @Override
  public SelectionVector4 getSelectionVector4() {
    return null;
  }

  @Override
  public FragmentContext getContext() {
    return context;
  }

  @Override
  public BatchSchema getSchema() {
    return null;
  }

  @Override
  public void cancel() {
    unnest.cancel();
  }

  @Override
  public VectorContainer getOutgoingContainer() {
    return null;
  }

  @Override
  public TypedFieldId getValueVectorId(SchemaPath path) {
    return null;
  }

  @Override
  public VectorWrapper<?> getValueAccessorById(Class<?> clazz, int... ids) {
    return null;
  }

  private void setupSchema(){
    // Nothing to do in this test
    return;
  }

  public boolean isCompleted() {
    return isDone;
  }

  // returns number of records added to output hyper container
  private int addBatchToHyperContainer(RecordBatch inputBatch) {
    int count = 0;
    final RecordBatchData batchCopy = new RecordBatchData(inputBatch, oContext.getAllocator());
    boolean success = false;
    try {
      for (VectorWrapper<?> w : batchCopy.getContainer()) {
        ValueVector vv = w.getValueVector();
        count += vv.getAccessor().getValueCount();
        resultList.add(vv);
      }
      success = true;
    } finally {
      if (!success) {
        batchCopy.clear();
      }
    }
    return count;
  }

  @Override
  public VectorContainer getContainer() { return null; }

  @Override
  public Iterator<VectorWrapper<?>> iterator() {
    return null;
  }
}
