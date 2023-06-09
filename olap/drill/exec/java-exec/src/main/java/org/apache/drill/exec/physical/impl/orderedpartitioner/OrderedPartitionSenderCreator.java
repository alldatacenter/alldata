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
package org.apache.drill.exec.physical.impl.orderedpartitioner;

import java.util.List;

import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.exec.ops.ExecutorFragmentContext;
import org.apache.drill.exec.physical.config.HashPartitionSender;
import org.apache.drill.exec.physical.config.OrderedPartitionSender;
import org.apache.drill.exec.physical.impl.RootCreator;
import org.apache.drill.exec.physical.impl.RootExec;
import org.apache.drill.exec.physical.impl.partitionsender.PartitionSenderRootExec;
import org.apache.drill.exec.record.RecordBatch;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

/**
 * <br/>
 * <h1>Known Issues:</h1>
 * <h2>Creation of batches</h2>
 * <p>
 *   The {@link org.apache.drill.exec.work.fragment.FragmentExecutor} is only aware of the operators in the tree that it has a reference too. In the case of the
 *   {@link OrderedPartitionSenderCreator}, an upstream {@link org.apache.drill.exec.record.RecordBatch} is wrapped in an
 *   {@link org.apache.drill.exec.physical.impl.orderedpartitioner.OrderedPartitionRecordBatch}. Since the
 *   {@link org.apache.drill.exec.physical.impl.orderedpartitioner.OrderedPartitionRecordBatch} is instantiated in the creator the
 *   {@link org.apache.drill.exec.work.fragment.FragmentExecutor} does not have a reference to it. So when the {@link org.apache.drill.exec.work.fragment.FragmentExecutor}
 *   closes the operators it closes the original operator, but not not the wrapping {@link OrderedPartitionSenderCreator}. This is an issue since the
 *   {@link OrderedPartitionSenderCreator} allocates {@link org.apache.drill.exec.record.VectorContainer}s which are consequentially never released.
 *   <br/>
 *   <ol>
 *     <li>
 *       We change the Creators in some way to communicate to the FragmentExecutor that they have wrapped an operator, so the FragmentExecutor can close the wrapped operator
 *       instead of the original operator.
 *     </li>
 *     <li>
 *       Or we take a less invasive approach and simply tell the {@link org.apache.drill.exec.physical.impl.partitionsender.PartitionSenderRootExec} whether to close the wrapped
 *       operator.
 *     </li>
 *   </ol>
 *   <br/>
 *   For now we've taken approach 2. In the future we should we should implement approach 1.
 * </p>
 */
public class OrderedPartitionSenderCreator implements RootCreator<OrderedPartitionSender> {

  @Override
  public RootExec getRoot(ExecutorFragmentContext context, OrderedPartitionSender config,
      List<RecordBatch> children) throws ExecutionSetupException {
    Preconditions.checkArgument(children.size() == 1);

    final OrderedPartitionRecordBatch recordBatch = new OrderedPartitionRecordBatch(config, children.iterator().next(), context);
    final HashPartitionSender hpc = new HashPartitionSender(config.getOppositeMajorFragmentId(), config, config.getRef(), config.getDestinations());
    return new PartitionSenderRootExec(context, recordBatch, hpc, true);
  }

}
