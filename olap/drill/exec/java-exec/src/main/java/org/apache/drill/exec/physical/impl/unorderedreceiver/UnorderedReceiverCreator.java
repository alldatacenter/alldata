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
package org.apache.drill.exec.physical.impl.unorderedreceiver;

import java.util.List;

import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.exec.ops.ExecutorFragmentContext;
import org.apache.drill.exec.physical.config.UnorderedReceiver;
import org.apache.drill.exec.physical.impl.BatchCreator;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.work.batch.IncomingBuffers;
import org.apache.drill.exec.work.batch.RawBatchBuffer;

public class UnorderedReceiverCreator implements BatchCreator<UnorderedReceiver> {

  @Override
  public UnorderedReceiverBatch getBatch(ExecutorFragmentContext context, UnorderedReceiver receiver, List<RecordBatch> children)
      throws ExecutionSetupException {
    assert children == null || children.isEmpty();
    IncomingBuffers bufHolder = context.getBuffers();
    assert bufHolder != null : "IncomingBuffers must be defined for any place a receiver is declared.";

    RawBatchBuffer[] buffers = bufHolder.getCollector(receiver.getOppositeMajorFragmentId()).getBuffers();
    assert buffers.length == 1;
    RawBatchBuffer buffer = buffers[0];
    return new UnorderedReceiverBatch(context, buffer, receiver);
  }
}
