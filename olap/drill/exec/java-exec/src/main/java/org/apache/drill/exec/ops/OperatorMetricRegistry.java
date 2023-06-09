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
package org.apache.drill.exec.ops;

import org.apache.drill.exec.physical.config.BroadcastSender;
import org.apache.drill.exec.physical.config.ExternalSort;
import org.apache.drill.exec.physical.config.FlattenPOP;
import org.apache.drill.exec.physical.config.HashAggregate;
import org.apache.drill.exec.physical.config.HashJoinPOP;
import org.apache.drill.exec.physical.config.HashPartitionSender;
import org.apache.drill.exec.physical.config.LateralJoinPOP;
import org.apache.drill.exec.physical.config.MergeJoinPOP;
import org.apache.drill.exec.physical.config.MergingReceiverPOP;
import org.apache.drill.exec.physical.config.RuntimeFilterPOP;
import org.apache.drill.exec.physical.config.Screen;
import org.apache.drill.exec.physical.config.SingleSender;
import org.apache.drill.exec.physical.config.UnionAll;
import org.apache.drill.exec.physical.config.UnnestPOP;
import org.apache.drill.exec.physical.config.UnorderedReceiver;
import org.apache.drill.exec.physical.impl.ScreenCreator;
import org.apache.drill.exec.physical.impl.SingleSenderCreator;
import org.apache.drill.exec.physical.impl.aggregate.HashAggTemplate;
import org.apache.drill.exec.physical.impl.broadcastsender.BroadcastSenderRootExec;
import org.apache.drill.exec.physical.impl.filter.RuntimeFilterRecordBatch;
import org.apache.drill.exec.physical.impl.flatten.FlattenRecordBatch;
import org.apache.drill.exec.physical.impl.join.HashJoinBatch;
import org.apache.drill.exec.physical.impl.mergereceiver.MergingRecordBatch;
import org.apache.drill.exec.physical.impl.partitionsender.PartitionSenderRootExec;
import org.apache.drill.exec.physical.impl.unnest.UnnestRecordBatch;
import org.apache.drill.exec.physical.impl.unorderedreceiver.UnorderedReceiverBatch;
import org.apache.drill.exec.physical.impl.xsort.ExternalSortBatch;
import org.apache.drill.exec.record.AbstractBinaryRecordBatch;
import org.apache.drill.exec.store.parquet.ParquetRowGroupScan;
import org.apache.drill.exec.store.parquet.columnreaders.ParquetRecordReader;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Registry of operator metrics.
 */
public class OperatorMetricRegistry {
//  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(OperatorMetricRegistry.class);

  // Mapping: key : operator type, value : metric id --> metric name
  private static final Map<String, String[]> OPERATOR_METRICS = new HashMap<>();

  static {
    register(Screen.OPERATOR_TYPE, ScreenCreator.ScreenRoot.Metric.class);
    register(SingleSender.OPERATOR_TYPE, SingleSenderCreator.SingleSenderRootExec.Metric.class);
    register(BroadcastSender.OPERATOR_TYPE, BroadcastSenderRootExec.Metric.class);
    register(HashPartitionSender.OPERATOR_TYPE, PartitionSenderRootExec.Metric.class);
    register(MergingReceiverPOP.OPERATOR_TYPE, MergingRecordBatch.Metric.class);
    register(UnorderedReceiver.OPERATOR_TYPE, UnorderedReceiverBatch.Metric.class);
    register(HashAggregate.OPERATOR_TYPE, HashAggTemplate.Metric.class);
    register(HashJoinPOP.OPERATOR_TYPE, HashJoinBatch.Metric.class);
    register(ExternalSort.OPERATOR_TYPE, ExternalSortBatch.Metric.class);
    register(ParquetRowGroupScan.OPERATOR_TYPE, ParquetRecordReader.Metric.class);
    register(FlattenPOP.OPERATOR_TYPE, FlattenRecordBatch.Metric.class);
    register(MergeJoinPOP.OPERATOR_TYPE, AbstractBinaryRecordBatch.Metric.class);
    register(LateralJoinPOP.OPERATOR_TYPE, AbstractBinaryRecordBatch.Metric.class);
    register(UnnestPOP.OPERATOR_TYPE, UnnestRecordBatch.Metric.class);
    register(UnionAll.OPERATOR_TYPE, AbstractBinaryRecordBatch.Metric.class);
    register(RuntimeFilterPOP.OPERATOR_TYPE, RuntimeFilterRecordBatch.Metric.class);
  }

  private static void register(String operatorType, Class<? extends MetricDef> metricDef) {
    // Currently registers a metric def that has enum constants
    MetricDef[] enumConstants = metricDef.getEnumConstants();
    if (enumConstants != null) {
      String[] names = Arrays.stream(enumConstants)
              .map(MetricDef::name)
              .toArray((String[]::new));
      OPERATOR_METRICS.put(operatorType, names);
    }
  }

  /**
   * Given an operator type, this method returns an array of metric names (indexable by metric id).
   *
   * @param operatorType the operator type
   * @return metric names if operator was registered, null otherwise
   */
  public static String[] getMetricNames(String operatorType) {
    return OPERATOR_METRICS.get(operatorType);
  }

  // to prevent instantiation
  private OperatorMetricRegistry() {
  }
}
