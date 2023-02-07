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
package org.apache.drill.exec.rpc.data;

import io.netty.buffer.DrillBuf;

import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.proto.BitData.FragmentRecordBatch;
import org.apache.drill.exec.record.RawFragmentBatch;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

/**
 * An incoming batch of data. The data is held by the original allocator. Any use of the associated data must be
 * leveraged through the use of newRawFragmentBatch().
 */
public class IncomingDataBatch {

  private final FragmentRecordBatch header;
  private final DrillBuf body;
  private final AckSender sender;

  /**
   * Create a new batch. Does not impact reference counts of body.
   *
   * @param header
   *          Batch header
   * @param body
   *          Data body. Could be null.
   * @param sender
   *          AckSender to use for underlying RawFragmentBatches.
   */
  public IncomingDataBatch(FragmentRecordBatch header, DrillBuf body, AckSender sender) {
    Preconditions.checkNotNull(header);
    Preconditions.checkNotNull(sender);
    this.header = header;
    this.body = body;
    this.sender = sender;
  }

  /**
   * Create a new RawFragmentBatch based on this incoming data batch that is transferred into the provided allocator.
   * Also increments the AckSender to expect one additional return message.
   *
   * @param allocator
   *          Target allocator that should be associated with data underlying this batch.
   * @return The newly created RawFragmentBatch
   */
  public RawFragmentBatch newRawFragmentBatch(final BufferAllocator allocator) {
    final DrillBuf transferredBuffer = body == null ? null : body.transferOwnership(allocator).buffer;
    sender.increment();
    return new RawFragmentBatch(header, transferredBuffer, sender);
  }

  public FragmentRecordBatch getHeader() {
    return header;
  }
}
