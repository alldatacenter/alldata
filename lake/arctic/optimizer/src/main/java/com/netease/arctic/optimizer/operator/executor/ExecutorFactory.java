/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */

package com.netease.arctic.optimizer.operator.executor;

import com.netease.arctic.optimizer.OptimizerConfig;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.utils.TableTypeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExecutorFactory {
  private static final Logger LOG = LoggerFactory.getLogger(ExecutorFactory.class);

  public static Executor constructOptimize(NodeTask nodeTask, ArcticTable table,
                                           long startTime, OptimizerConfig config) {
    if (TableTypeUtil.isIcebergTableFormat(table)) {
      return new IcebergExecutor(nodeTask, table, startTime, config);
    } else {
      switch (nodeTask.getOptimizeType()) {
        case Minor:
          return new MinorExecutor(nodeTask, table, startTime, config);
        case Major:
        case FullMajor:
          return new MajorExecutor(nodeTask, table, startTime, config);
        default:
          LOG.error("not support optimize type: {}", nodeTask.getOptimizeType());
          throw new UnsupportedOperationException();
      }
    }
  }
}
