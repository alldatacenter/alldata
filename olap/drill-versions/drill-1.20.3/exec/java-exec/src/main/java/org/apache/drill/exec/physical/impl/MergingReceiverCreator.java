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

import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.exec.exception.OutOfMemoryException;
import org.apache.drill.exec.ops.ExecutorFragmentContext;
import org.apache.drill.exec.physical.config.MergingReceiverPOP;
import org.apache.drill.exec.physical.impl.mergereceiver.MergingRecordBatch;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.work.batch.IncomingBuffers;
import org.apache.drill.exec.work.batch.RawBatchBuffer;

public class MergingReceiverCreator implements BatchCreator<MergingReceiverPOP> {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MergingReceiverCreator.class);

  @Override
  public MergingRecordBatch getBatch(ExecutorFragmentContext context,
                              MergingReceiverPOP receiver,
                              List<RecordBatch> children)
      throws ExecutionSetupException, OutOfMemoryException {

    assert children == null || children.isEmpty();
    IncomingBuffers bufHolder = context.getBuffers();

    assert bufHolder != null : "IncomingBuffers must be defined for any place a receiver is declared.";
    RawBatchBuffer[] buffers = bufHolder.getCollector(receiver.getOppositeMajorFragmentId()).getBuffers();

    return new MergingRecordBatch(context, receiver, buffers);
  }
}
