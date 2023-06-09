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
package org.apache.drill.exec.physical.base;

import org.apache.drill.exec.physical.config.BroadcastSender;
import org.apache.drill.exec.physical.config.Filter;
import org.apache.drill.exec.physical.config.FlattenPOP;
import org.apache.drill.exec.physical.config.HashAggregate;
import org.apache.drill.exec.physical.config.HashPartitionSender;
import org.apache.drill.exec.physical.config.HashToRandomExchange;
import org.apache.drill.exec.physical.config.IteratorValidator;
import org.apache.drill.exec.physical.config.LateralJoinPOP;
import org.apache.drill.exec.physical.config.Limit;
import org.apache.drill.exec.physical.config.MergingReceiverPOP;
import org.apache.drill.exec.physical.config.OrderedPartitionSender;
import org.apache.drill.exec.physical.config.ProducerConsumer;
import org.apache.drill.exec.physical.config.Project;
import org.apache.drill.exec.physical.config.RangePartitionSender;
import org.apache.drill.exec.physical.config.RowKeyJoinPOP;
import org.apache.drill.exec.physical.config.Screen;
import org.apache.drill.exec.physical.config.SingleSender;
import org.apache.drill.exec.physical.config.Sort;
import org.apache.drill.exec.physical.config.StatisticsAggregate;
import org.apache.drill.exec.physical.config.StatisticsMerge;
import org.apache.drill.exec.physical.config.StreamingAggregate;
import org.apache.drill.exec.physical.config.Trace;
import org.apache.drill.exec.physical.config.UnionAll;
import org.apache.drill.exec.physical.config.UnnestPOP;
import org.apache.drill.exec.physical.config.UnorderedReceiver;
import org.apache.drill.exec.physical.config.UnpivotMaps;
import org.apache.drill.exec.physical.config.Values;
import org.apache.drill.exec.physical.config.WindowPOP;

public abstract class AbstractPhysicalVisitor<T, X, E extends Throwable> implements PhysicalVisitor<T, X, E> {

  @Override
  public T visitExchange(Exchange exchange, X value) throws E {
    return visitOp(exchange, value);
  }

  @Override
  public T visitUnion(UnionAll union, X value) throws E {
    return visitOp(union, value);
  }

  @Override
  public T visitWriter(Writer writer, X value) throws E {
    return visitOp(writer, value);
  }

  @Override
  public T visitFilter(Filter filter, X value) throws E {
    return visitOp(filter, value);
  }

  @Override
  public T visitWindowFrame(WindowPOP windowFrame, X value) throws E {
    return visitOp(windowFrame, value);
  }

  @Override
  public T visitProject(Project project, X value) throws E {
    return visitOp(project, value);
  }

  @Override
  public T visitTrace(Trace trace, X value) throws E {
      return visitOp(trace, value);
  }

  @Override
  public T visitSort(Sort sort, X value) throws E {
    return visitOp(sort, value);
  }

  @Override
  public T visitLimit(Limit limit, X value) throws E {
    return visitOp(limit, value);
  }

  @Override
  public T visitStreamingAggregate(StreamingAggregate agg, X value) throws E {
    return visitOp(agg, value);
  }

  @Override
  public T visitStatisticsAggregate(StatisticsAggregate agg, X value) throws E {
    return visitOp(agg, value);
  }

  @Override
  public T visitStatisticsMerge(StatisticsMerge agg, X value) throws E {
    return visitOp(agg, value);
  }

  @Override
  public T visitHashAggregate(HashAggregate agg, X value) throws E {
    return visitOp(agg, value);
  }

  @Override
  public T visitSender(Sender sender, X value) throws E {
    return visitOp(sender, value);
  }

  @Override
  public T visitFlatten(FlattenPOP flatten, X value) throws E {
    return visitOp(flatten, value);
  }

  @Override
  public T visitReceiver(Receiver receiver, X value) throws E {
    return visitOp(receiver, value);
  }

  @Override
  public T visitGroupScan(GroupScan groupScan, X value) throws E {
    return visitOp(groupScan, value);
  }

  @Override
  public T visitSubScan(SubScan subScan, X value) throws E {
    return visitOp(subScan, value);
  }

  @Override
  public T visitStore(Store store, X value) throws E {
    return visitOp(store, value);
  }

  public T visitChildren(PhysicalOperator op, X value) throws E {
    for (PhysicalOperator child : op) {
      child.accept(this, value);
    }
    return null;
  }

  @Override
  public T visitRowKeyJoin(RowKeyJoinPOP join, X value) throws E {
    return visitOp(join, value);
  }

  @Override
  public T visitHashPartitionSender(HashPartitionSender op, X value) throws E {
    return visitSender(op, value);
  }

  @Override
  public T visitOrderedPartitionSender(OrderedPartitionSender op, X value) throws E {
    return visitSender(op, value);
  }

  @Override
  public T visitUnorderedReceiver(UnorderedReceiver op, X value) throws E {
    return visitReceiver(op, value);
  }

  @Override
  public T visitMergingReceiver(MergingReceiverPOP op, X value) throws E {
    return visitReceiver(op, value);
  }

  @Override
  public T visitHashPartitionSender(HashToRandomExchange op, X value) throws E {
    return visitExchange(op, value);
  }

  @Override
  public T visitRangePartitionSender(RangePartitionSender op, X value) throws E {
    return visitSender(op, value);
  }

  @Override
  public T visitBroadcastSender(BroadcastSender op, X value) throws E {
    return visitSender(op, value);
  }

  @Override
  public T visitScreen(Screen op, X value) throws E {
    return visitStore(op, value);
  }

  @Override
  public T visitSingleSender(SingleSender op, X value) throws E {
    return visitSender(op, value);
  }

  @Override
  public T visitProducerConsumer(ProducerConsumer op, X value) throws E {
    return visitOp(op, value);
  }

  @Override
  public T visitUnnest(UnnestPOP unnest, X value) throws E {
    return visitOp(unnest, value);
  }

  @Override
  public T visitLateralJoin(LateralJoinPOP lateralJoinPOP, X value) throws E {
    return visitOp(lateralJoinPOP, value);
  }

  @Override
  public T visitIteratorValidator(IteratorValidator op, X value) throws E {
    return visitOp(op, value);
  }

  @Override
  public T visitValues(Values op, X value) throws E {
    return visitOp(op, value);
  }

  @Override
  public T visitUnpivot(UnpivotMaps op, X value) throws E {
    return visitOp(op, value);
  }

  @Override
  public T visitOp(PhysicalOperator op, X value) throws E {
    throw new UnsupportedOperationException(String.format(
        "PhysicalVisitor of type %s does not support PhysicalOperator type %s.", this
            .getClass().getCanonicalName(), op.getClass().getCanonicalName()));
  }
}
