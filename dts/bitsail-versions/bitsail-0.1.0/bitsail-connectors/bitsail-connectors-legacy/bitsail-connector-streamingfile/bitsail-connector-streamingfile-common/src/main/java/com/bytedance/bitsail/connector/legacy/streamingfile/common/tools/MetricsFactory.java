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

package com.bytedance.bitsail.connector.legacy.streamingfile.common.tools;

import com.bytedance.bitsail.base.metrics.MetricManager;
import com.bytedance.bitsail.base.metrics.manager.BitSailMetricManager;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.option.ReaderOptions;
import com.bytedance.bitsail.common.option.WriterOptions;
import com.bytedance.bitsail.common.util.Pair;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.option.FileSystemSinkOptions;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class MetricsFactory {

  private static final Logger LOG = LoggerFactory.getLogger(MetricsFactory.class);
  private static final Map<Integer, MetricManager> METRIC_MANAGER_MAP = new HashMap<>();

  public static synchronized MetricManager getInstanceMetricsManager(BitSailConfiguration jobConf, Integer taskId) {
    MetricManager metricManager = METRIC_MANAGER_MAP.get(taskId);
    if (metricManager == null) {
      metricManager = getMetricManager(jobConf, taskId);
      metricManager.start();
      METRIC_MANAGER_MAP.put(taskId, metricManager);
      LOG.info("Instance {} add metric group", taskId);
    }
    return metricManager;
  }

  private static synchronized MetricManager getMetricManager(BitSailConfiguration jobConf, Integer taskId) {
    List<Pair<String, String>> dumpExtraMetricTag = getDumpExtraMetricTag(jobConf, taskId);
    return new BitSailMetricManager(jobConf, "dump",
        false,
        dumpExtraMetricTag);
  }

  // todo: Remove ReaderOption in filesystem sink module.
  private static List<Pair<String, String>> getDumpExtraMetricTag(BitSailConfiguration jobConf, Integer taskId) {
    String sourceType = jobConf.getUnNecessaryOption(ReaderOptions.READER_METRIC_TAG_NAME,
        getSimpleClassName(jobConf.get(ReaderOptions.READER_CLASS)));
    String sinkType = jobConf.getUnNecessaryOption(WriterOptions.WRITER_METRIC_TAG_NAME,
        getSimpleClassName(jobConf.get(WriterOptions.WRITER_CLASS)));

    List<Pair<String, String>> extraMetricTag = Lists.newArrayList(ImmutableList.of(
        Pair.newPair("source", sourceType),
        Pair.newPair("target", sinkType),
        Pair.newPair("task", String.valueOf(taskId)))
    );
    if (jobConf.fieldExists(FileSystemSinkOptions.HDFS_DUMP_TYPE)) {
      String dumpType = jobConf.get(FileSystemSinkOptions.HDFS_DUMP_TYPE);
      extraMetricTag.add(Pair.newPair("dump_type", dumpType));
    }
    return extraMetricTag;
  }

  public static synchronized void removeMetricManager(Integer taskId) throws IOException {
    if (METRIC_MANAGER_MAP.containsKey(taskId)) {
      MetricManager metricManager = METRIC_MANAGER_MAP.remove(taskId);
      metricManager.close();
    }
  }

  private static String getSimpleClassName(String completeClassName) {
    return completeClassName.substring(completeClassName.lastIndexOf('.'));
  }
}
