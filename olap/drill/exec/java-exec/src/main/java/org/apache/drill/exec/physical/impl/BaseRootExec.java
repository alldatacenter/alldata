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
package org.apache.drill.exec.physical.impl;

import java.util.List;

import org.apache.drill.common.DeferredException;
import org.apache.drill.exec.exception.OutOfMemoryException;
import org.apache.drill.exec.ops.OpProfileDef;
import org.apache.drill.exec.ops.OperatorContext;
import org.apache.drill.exec.ops.OperatorStats;
import org.apache.drill.exec.ops.OperatorUtilities;
import org.apache.drill.exec.ops.RootFragmentContext;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.proto.ExecProtos.FragmentHandle;
import org.apache.drill.exec.record.CloseableRecordBatch;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.RecordBatch.IterOutcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseRootExec implements RootExec {
  private static final Logger logger = LoggerFactory.getLogger(BaseRootExec.class);

  public static final String ENABLE_BATCH_DUMP_CONFIG = "drill.exec.debug.dump_batches";
  protected OperatorStats stats;
  protected OperatorContext oContext;
  protected RootFragmentContext fragmentContext;
  private List<CloseableRecordBatch> operators;

  public BaseRootExec(final RootFragmentContext fragmentContext, final PhysicalOperator config) throws OutOfMemoryException {
    this(fragmentContext, null, config);
  }

  public BaseRootExec(final RootFragmentContext fragmentContext, final OperatorContext oContext,
                      final PhysicalOperator config) throws OutOfMemoryException {
    if (oContext == null) {
      this.oContext = fragmentContext.newOperatorContext(config, stats);
    } else {
      this.oContext = oContext;
    }
    //Creating new stat for appending to list
    stats = new OperatorStats(new OpProfileDef(config.getOperatorId(),
        config.getOperatorType(), OperatorUtilities.getChildCount(config)),
      this.oContext.getAllocator());
    fragmentContext.getStats().addOperatorStats(this.stats);
    this.fragmentContext = fragmentContext;
  }

  void setOperators(List<CloseableRecordBatch> operators) {
    this.operators = operators;

    if (logger.isDebugEnabled()) {
      final StringBuilder sb = new StringBuilder();
      sb.append("BaseRootExec(");
      sb.append(Integer.toString(System.identityHashCode(this)));
      sb.append(") operators: ");
      for(final CloseableRecordBatch crb : operators) {
        sb.append(crb.getClass().getName());
        sb.append(' ');
        sb.append(Integer.toString(System.identityHashCode(crb)));
        sb.append(", ");
      }

      // Cut off the last trailing comma and space
      sb.setLength(sb.length() - 2);

      logger.debug(sb.toString());
    }
  }

  @Override
  public final boolean next() {
    assert stats != null;
    fragmentContext.getExecutorState().checkContinue();
    try {
      stats.startProcessing();
      return innerNext();
    } finally {
      stats.stopProcessing();
    }
  }

  public final IterOutcome next(final RecordBatch b){
    stats.stopProcessing();
    IterOutcome next;
    try {
      next = b.next();
    } finally {
      stats.startProcessing();
    }

    switch(next){
      case OK_NEW_SCHEMA:
        stats.batchReceived(0, b.getRecordCount(), true);
        break;
      case OK:
        stats.batchReceived(0, b.getRecordCount(), false);
        break;
      default:
    }
    return next;
  }

  public abstract boolean innerNext();

  @Override
  public void receivingFragmentFinished(final FragmentHandle handle) {
    logger.warn("Currently not handling FinishedFragment message");
  }

  @Override
  public void dumpBatches(Throwable t) {
    if (operators == null) {
      return;
    }
    if (!fragmentContext.getConfig().getBoolean(ENABLE_BATCH_DUMP_CONFIG)) {
      return;
    }

    CloseableRecordBatch leafMost = findLeaf(operators, t);
    if (leafMost == null) {
      // Don't know which batch failed.
      return;
    }
    int batchPosn = operators.indexOf(leafMost);
    final int numberOfBatchesToDump = Math.min(batchPosn + 1, 2);
    logger.error("Batch dump started: dumping last {} failed batches", numberOfBatchesToDump);
    // As batches are stored in a 'flat' List there is a need to filter out the failed batch
    // and a few of its parent (actual number of batches is set by a constant defined above)
    for (int i = 0; i < numberOfBatchesToDump; i++) {
      operators.get(batchPosn--).dump();
    }
    logger.error("Batch dump completed.");
  }

  @Override
  public void close() throws Exception {
    // We want to account for the time spent waiting here as Wait time in the operator profile
    try {
      stats.startProcessing();
      stats.startWait();
      fragmentContext.waitForSendComplete();
    } finally {
      stats.stopWait();
      stats.stopProcessing();
    }

    // close all operators.
    if (operators != null) {
      final DeferredException df = new DeferredException();

      for (final CloseableRecordBatch crb : operators) {
        df.suppressingClose(crb);
        if (logger.isDebugEnabled()) {
          logger.debug(String.format("closed operator %d", System.identityHashCode(crb)));
        }
      }

      try {
        df.close();
      } catch (Exception e) {
        fragmentContext.getExecutorState().fail(e);
      }
    }
  }

  /**
   * Given a list of operators and a stack trace, walks the stack trace and
   * the operator list to find the leaf-most operator, which is the one
   * that was active when the exception was thrown. Handle the cases in
   * which no operator was active, each operator had multiple methods on
   * the stack, or the exception was thrown in some class called by
   * the operator.
   * <p>
   * Not all operators leave a mark in the trace. In particular if a the
   * call stack is only through base-class methods, then we have no way to
   * know the actual class during the call. This is OK because the leaf
   * methods are just pass-through operations, they are unlikely to fail.
   *
   * @param <T> the type of the operator. Parameterized to allow easier
   * testing
   * @param dag the list of operators from root-most to leaf-most
   * @param e the exception thrown somewhere in the operator tree
   * @return the leaf-most operator, if any
   */
  public static <T> T findLeaf(List<T> dag, Throwable e) {
    StackTraceElement[] trace = e.getStackTrace();
    for (int i = dag.size() - 1; i >= 0; i--) {
      T leaf = dag.get(i);
      String opName = leaf.getClass().getName();
      for (StackTraceElement element : trace) {
        String frameName = element.getClassName();
        if (frameName.contentEquals(opName)) {
          return leaf;
        }
      }
    }
    return null;
  }
}
