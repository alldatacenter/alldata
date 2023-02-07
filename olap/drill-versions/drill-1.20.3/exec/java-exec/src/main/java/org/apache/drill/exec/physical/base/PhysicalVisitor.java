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

/**
 * Visitor class designed to traversal of a operator tree.  Basis for a number of operator manipulations including fragmentation and materialization.
 * @param <RETURN> The class associated with the return of each visit method.
 * @param <EXTRA> The class object associated with additional data required for a particular operator modification.
 * @param <EXCEP> An optional exception class that can be thrown when a portion of a modification or traversal fails.  Must extend Throwable.  In the case where the visitor does not throw any caught exception, this can be set as RuntimeException.
 */
public interface PhysicalVisitor<RETURN, EXTRA, EXCEP extends Throwable> {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PhysicalVisitor.class);


  public RETURN visitExchange(Exchange exchange, EXTRA value) throws EXCEP;
  public RETURN visitGroupScan(GroupScan groupScan, EXTRA value) throws EXCEP;
  public RETURN visitSubScan(SubScan subScan, EXTRA value) throws EXCEP;
  public RETURN visitStore(Store store, EXTRA value) throws EXCEP;
  public RETURN visitFilter(Filter filter, EXTRA value) throws EXCEP;
  public RETURN visitUnion(UnionAll union, EXTRA value) throws EXCEP;
  public RETURN visitProject(Project project, EXTRA value) throws EXCEP;
  public RETURN visitTrace(Trace trace, EXTRA value) throws EXCEP;
  public RETURN visitSort(Sort sort, EXTRA value) throws EXCEP;
  public RETURN visitLimit(Limit limit, EXTRA value) throws EXCEP;
  public RETURN visitFlatten(FlattenPOP flatten, EXTRA value) throws EXCEP;
  public RETURN visitSender(Sender sender, EXTRA value) throws EXCEP;
  public RETURN visitRowKeyJoin(RowKeyJoinPOP join, EXTRA value) throws EXCEP;
  public RETURN visitReceiver(Receiver receiver, EXTRA value) throws EXCEP;
  public RETURN visitStreamingAggregate(StreamingAggregate agg, EXTRA value) throws EXCEP;
  public RETURN visitStatisticsAggregate(StatisticsAggregate agg, EXTRA value) throws EXCEP;
  public RETURN visitStatisticsMerge(StatisticsMerge agg, EXTRA value) throws EXCEP;
  public RETURN visitHashAggregate(HashAggregate agg, EXTRA value) throws EXCEP;
  public RETURN visitWriter(Writer op, EXTRA value) throws EXCEP;
  public RETURN visitUnpivot(UnpivotMaps op, EXTRA value) throws EXCEP;
  public RETURN visitValues(Values op, EXTRA value) throws EXCEP;
  public RETURN visitOp(PhysicalOperator op, EXTRA value) throws EXCEP;

  public RETURN visitHashPartitionSender(HashPartitionSender op, EXTRA value) throws EXCEP;
  public RETURN visitOrderedPartitionSender(OrderedPartitionSender op, EXTRA value) throws EXCEP;
  public RETURN visitUnorderedReceiver(UnorderedReceiver op, EXTRA value) throws EXCEP;
  public RETURN visitMergingReceiver(MergingReceiverPOP op, EXTRA value) throws EXCEP;
  public RETURN visitHashPartitionSender(HashToRandomExchange op, EXTRA value) throws EXCEP;
  public RETURN visitRangePartitionSender(RangePartitionSender op, EXTRA value) throws EXCEP;
  public RETURN visitBroadcastSender(BroadcastSender op, EXTRA value) throws EXCEP;
  public RETURN visitScreen(Screen op, EXTRA value) throws EXCEP;
  public RETURN visitSingleSender(SingleSender op, EXTRA value) throws EXCEP;
  public RETURN visitWindowFrame(WindowPOP op, EXTRA value) throws EXCEP;
  public RETURN visitProducerConsumer(ProducerConsumer op, EXTRA value) throws EXCEP;
  public RETURN visitUnnest(UnnestPOP unnest, EXTRA value) throws EXCEP;
  public RETURN visitLateralJoin(LateralJoinPOP lateralJoinPOP, EXTRA value) throws EXCEP;

  public RETURN visitIteratorValidator(IteratorValidator op, EXTRA value) throws EXCEP;
}
