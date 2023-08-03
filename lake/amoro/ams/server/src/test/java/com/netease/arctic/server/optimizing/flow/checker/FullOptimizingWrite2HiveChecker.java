/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.server.optimizing.flow.checker;

import com.netease.arctic.optimizing.OptimizingInputProperties;
import com.netease.arctic.server.optimizing.OptimizingType;
import com.netease.arctic.server.optimizing.UnKeyedTableCommit;
import com.netease.arctic.server.optimizing.flow.view.TableDataView;
import com.netease.arctic.server.optimizing.plan.OptimizingPlanner;
import com.netease.arctic.server.optimizing.plan.TaskDescriptor;
import com.netease.arctic.table.ArcticTable;
import org.apache.commons.collections.CollectionUtils;

import javax.annotation.Nullable;
import java.util.List;


public class FullOptimizingWrite2HiveChecker extends AbstractHiveChecker {

  public FullOptimizingWrite2HiveChecker(TableDataView view) {
    super(view);
  }

  @Override
  protected boolean internalCondition(
      ArcticTable table,
      @Nullable List<TaskDescriptor> latestTaskDescriptors,
      OptimizingPlanner latestPlanner,
      @Nullable UnKeyedTableCommit latestCommit) {
    return CollectionUtils.isNotEmpty(latestTaskDescriptors) &&
        latestPlanner.getOptimizingType() == OptimizingType.FULL &&
        OptimizingInputProperties.parse(latestTaskDescriptors.stream().findAny().get().properties()).getOutputDir() !=
            null;
  }
}
