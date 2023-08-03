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

package com.netease.arctic.server.optimizing.plan;

import com.netease.arctic.optimizing.IcebergRewriteExecutorFactory;
import com.netease.arctic.optimizing.OptimizingInputProperties;
import com.netease.arctic.server.table.TableRuntime;
import com.netease.arctic.table.ArcticTable;

import java.util.Collections;

public class IcebergPartitionPlan extends AbstractPartitionPlan {

  protected IcebergPartitionPlan(TableRuntime tableRuntime, ArcticTable table, String partition, long planTime) {
    super(tableRuntime, table, partition, planTime);
  }

  @Override
  protected TaskSplitter buildTaskSplitter() {
    // TODO not split tasks in a partition now
    return targetTaskCount -> Collections.singletonList(new SplitTask(fragmentFiles, segmentFiles));
  }

  @Override
  protected OptimizingInputProperties buildTaskProperties() {
    OptimizingInputProperties properties = new OptimizingInputProperties();
    properties.setExecutorFactoryImpl(IcebergRewriteExecutorFactory.class.getName());
    return properties;
  }
}
