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

import com.netease.arctic.ams.api.OptimizeTaskStat;
import org.apache.iceberg.ContentFile;

public class OptimizeTaskResult {
  private OptimizeTaskStat optimizeTaskStat;

  private Iterable<? extends ContentFile<?>> targetFiles;

  public OptimizeTaskStat getOptimizeTaskStat() {
    return optimizeTaskStat;
  }

  public void setOptimizeTaskStat(OptimizeTaskStat optimizeTaskStat) {
    this.optimizeTaskStat = optimizeTaskStat;
  }

  public Iterable<? extends ContentFile<?>> getTargetFiles() {
    return targetFiles;
  }

  public void setTargetFiles(Iterable<? extends ContentFile<?>> targetFiles) {
    this.targetFiles = targetFiles;
  }
}
