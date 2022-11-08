/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytedance.bitsail.connector.legacy.jdbc.split.strategy;

import com.bytedance.bitsail.connector.legacy.jdbc.model.DbClusterInfo;
import com.bytedance.bitsail.connector.legacy.jdbc.model.DbShardInfo;
import com.bytedance.bitsail.connector.legacy.jdbc.split.SplitParameterInfo;
import com.bytedance.bitsail.connector.legacy.jdbc.split.TableRangeInfo;

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class NoSplitParametersProvider<K extends Comparable> implements ParameterValuesProvider {
  private static final Logger LOG = LoggerFactory.getLogger(NoSplitParametersProvider.class);

  private DbClusterInfo dbClusterInfo;
  private SplitParameterInfo<K> spInfo;

  public NoSplitParametersProvider(DbClusterInfo dbClusterInfo) {
    this.dbClusterInfo = dbClusterInfo;
    this.spInfo = new SplitParameterInfo<>(1);
  }

  @Override
  public SplitParameterInfo getParameterValues() throws ExecutionException, InterruptedException {
    Map<Integer, List<DbShardInfo>> shardsInfo = dbClusterInfo.getShardsInfo();
    HashMap<Integer, List<TableRangeInfo<K>>> splitsMap = new HashMap<>();
    splitsMap.put(0, Lists.newArrayList(TableRangeInfo.<K>getEmptyRange()));
    spInfo.assign(splitsMap, dbClusterInfo);
    return spInfo;
  }
}
