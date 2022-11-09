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

package com.bytedance.bitsail.connector.legacy.hudi.utils;

import com.bytedance.bitsail.connector.legacy.hudi.configuration.HadoopConfigurations;
import com.bytedance.bitsail.connector.legacy.hudi.util.StreamerUtil;

import org.apache.flink.configuration.Configuration;
import org.apache.hudi.common.table.HoodieTableMetaClient;
import org.apache.hudi.common.table.timeline.HoodieInstant;
import org.apache.hudi.common.table.timeline.HoodieTimeline;

import javax.annotation.Nullable;

/**
 * Common test utils.
 */
public class TestUtils {
  public static String getLastPendingInstant(String basePath) {
    final HoodieTableMetaClient metaClient = HoodieTableMetaClient.builder()
        .setConf(HadoopConfigurations.getHadoopConf(new Configuration())).setBasePath(basePath).build();
    return StreamerUtil.getLastPendingInstant(metaClient);
  }

  public static String getLastCompleteInstant(String basePath) {
    final HoodieTableMetaClient metaClient = HoodieTableMetaClient.builder()
        .setConf(HadoopConfigurations.getHadoopConf(new Configuration())).setBasePath(basePath).build();
    return StreamerUtil.getLastCompletedInstant(metaClient);
  }

  public static String getLastDeltaCompleteInstant(String basePath) {
    final HoodieTableMetaClient metaClient = HoodieTableMetaClient.builder()
        .setConf(HadoopConfigurations.getHadoopConf(new Configuration())).setBasePath(basePath).build();
    return metaClient.getCommitsTimeline().filterCompletedInstants()
        .filter(hoodieInstant -> hoodieInstant.getAction().equals(HoodieTimeline.DELTA_COMMIT_ACTION))
        .lastInstant()
        .map(HoodieInstant::getTimestamp)
        .orElse(null);
  }

  public static String getFirstCompleteInstant(String basePath) {
    final HoodieTableMetaClient metaClient = HoodieTableMetaClient.builder()
        .setConf(HadoopConfigurations.getHadoopConf(new Configuration())).setBasePath(basePath).build();
    return metaClient.getCommitsAndCompactionTimeline().filterCompletedInstants().firstInstant()
        .map(HoodieInstant::getTimestamp).orElse(null);
  }

  @Nullable
  public static String getNthCompleteInstant(String basePath, int n, boolean isDelta) {
    final HoodieTableMetaClient metaClient = HoodieTableMetaClient.builder()
        .setConf(HadoopConfigurations.getHadoopConf(new Configuration())).setBasePath(basePath).build();
    return metaClient.getActiveTimeline()
        .filterCompletedInstants()
        .filter(instant -> isDelta ? HoodieTimeline.DELTA_COMMIT_ACTION.equals(instant.getAction()) : HoodieTimeline.COMMIT_ACTION.equals(instant.getAction()))
        .nthInstant(n).map(HoodieInstant::getTimestamp)
        .orElse(null);
  }
}
