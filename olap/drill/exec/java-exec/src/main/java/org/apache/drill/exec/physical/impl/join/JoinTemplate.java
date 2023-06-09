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

import javax.inject.Named;

import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.calcite.rel.core.JoinRelType;

/**
 * Merge Join implementation using RecordIterator.
 */
public abstract class JoinTemplate implements JoinWorker {

  @Override
  public void setupJoin(FragmentContext context, JoinStatus status, VectorContainer outgoing) throws SchemaChangeException {
    doSetup(context, status, outgoing);
  }

  /**
   * Copy rows from the input record batches until the output record batch is full
   * @param status  State of the join operation (persists across multiple record batches/schema changes)
   * @return  true of join succeeded; false if the worker needs to be regenerated
   */
  public final boolean doJoin(final JoinStatus status) {
    final boolean isLeftJoin = status.outputBatch.getPopConfig().getJoinType() == JoinRelType.LEFT;
    status.setHasMoreData(false);
    while (!status.isOutgoingBatchFull()) {
      if (status.right.finished()) {
        if (isLeftJoin) {
          while (!status.left.finished()) {
            if (status.isOutgoingBatchFull()) {
              status.setHasMoreData(true);
              return true;
            }
            doCopyLeft(status.left.getCurrentPosition(), status.getOutPosition());
            status.incOutputPos();
            status.left.next();
          }
        }
        return true;
      }
      if (status.left.finished()) {
        return true;
      }
      final int comparison = Integer.signum(doCompare(status.left.getCurrentPosition(), status.right.getCurrentPosition()));
      switch (comparison) {
        case -1:
          // left key < right key
          if (isLeftJoin) {
            doCopyLeft(status.left.getCurrentPosition(), status.getOutPosition());
            status.incOutputPos();
          }
          status.left.next();
          continue;

        case 0:
          // left key == right key
          // Mark current position in right iterator.
          // If we have set a mark in previous iteration but didn't finish the inner loop,
          // skip current right side as its already copied in earlier iteration.
          if (status.shouldMark()) {
            status.right.mark();
            // Copy all equal keys from right side to the output record batch.
            doCopyLeft(status.left.getCurrentPosition(), status.getOutPosition());
            doCopyRight(status.right.getCurrentPosition(), status.getOutPosition());
            status.incOutputPos();
          }
          if (status.isOutgoingBatchFull()) {
            // Leave iterators at their current positions and markers.
            // Don't mark on all subsequent doJoin iterations.
            status.setHasMoreData(true);
            status.disableMarking();
            return true;
          }
          // Move to next position in right iterator.
          status.right.next();
          while (!status.right.finished()) {
            if (doCompare(status.left.getCurrentPosition(), status.right.getCurrentPosition()) == 0) {
              doCopyLeft(status.left.getCurrentPosition(), status.getOutPosition());
              doCopyRight(status.right.getCurrentPosition(), status.getOutPosition());
              status.incOutputPos();
              if (status.isOutgoingBatchFull()) {
                status.setHasMoreData(true);
                status.disableMarking();
                return true;
              }
              status.right.next();
            } else {
              break;
            }
          }
          status.right.reset();
          // Enable marking only when we have consumed all equal keys on right side.
          status.enableMarking();
          status.left.next();
          continue;
        case 1:
          // left key > right key
          status.right.next();
          continue;

        default:
          throw new IllegalStateException();
      }
    }
    return true;
  }

  // Generated Methods

  public abstract void doSetup(@Named("context") FragmentContext context,
                               @Named("status") JoinStatus status,
                               @Named("outgoing") VectorContainer outgoing) throws SchemaChangeException;

  /**
   * Copy the data to the new record batch (if it fits).
   *
   * @param  leftIndex   position of batch (lower 16 bits) and record (upper 16 bits) in left SV4
   * @param  outIndex  position of the output record batch
   * @return Whether or not the data was copied.
   */
  public abstract void doCopyLeft(@Named("leftIndex") int leftIndex, @Named("outIndex") int outIndex);
  public abstract void doCopyRight(@Named("rightIndex") int rightIndex, @Named("outIndex") int outIndex);


  /**
   * Compare the values of the left and right join key to determine whether the left is less than, greater than
   * or equal to the right.
   *
   * @param leftIndex
   * @param rightIndex
   * @return  0 if both keys are equal
   *         -1 if left is < right
   *          1 if left is > right
   */
  protected abstract int doCompare(@Named("leftIndex") int leftIndex,
                                   @Named("rightIndex") int rightIndex);
}