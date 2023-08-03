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

package org.apache.uniffle.coordinator.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.uniffle.common.PartitionRange;
import org.apache.uniffle.common.util.Constants;
import org.apache.uniffle.coordinator.strategy.assignment.PartitionRangeAssignment;
import org.apache.uniffle.proto.RssProtos;
import org.apache.uniffle.proto.RssProtos.GetShuffleAssignmentsResponse;

public class CoordinatorUtils {

  private static final Logger LOG = LoggerFactory.getLogger(CoordinatorUtils.class);

  public static final String COORDINATOR_ID = "coordinator.id";

  public static GetShuffleAssignmentsResponse toGetShuffleAssignmentsResponse(
      PartitionRangeAssignment pra) {
    List<RssProtos.PartitionRangeAssignment> praList = pra.convertToGrpcProto();

    return GetShuffleAssignmentsResponse.newBuilder()
               .addAllAssignments(praList)
               .build();
  }

  public static int nextIdx(int idx, int size) {
    ++idx;
    if (idx >= size) {
      idx = 0;
    }
    return idx;
  }

  /**
   * Assign multiple adjacent partitionRanges to several servers, The result returned is a double
   * PartitionRange list, the first list will be assigned to server1,
   * the second list will be assigned to server2, and so on.
   * Suppose totalPartitionNum=52, partitionNumPerRange=2, serverNum=5, estimateTaskConcurrency=20
   * The final result generated is:
   * server1: [0,1] [2,3] [4,5] [6,7] [40,41] [42,43]
   * server2: [8,9] [10,11] [12,13] [14,15] [44,45]
   * server3: [16,17] [18,19] [20,21] [22,23] [46,47]
   * server4: [24,25] [26,27] [28,29] [30,31] [48,49]
   * server5: [32,33] [34,35] [36,37] [38,39] [50,51]
   */
  public static List<List<PartitionRange>> generateRangesGroup(int totalPartitionNum, int partitionNumPerRange,
      int serverNum, int estimateTaskConcurrency) {
    List<List<PartitionRange>> res = Lists.newArrayList();
    if (totalPartitionNum <= 0 || partitionNumPerRange <= 0) {
      return res;
    }
    estimateTaskConcurrency = Math.min(totalPartitionNum, estimateTaskConcurrency);
    int rangePerGroup = estimateTaskConcurrency > serverNum * partitionNumPerRange
                            ? Math.floorDiv(estimateTaskConcurrency, serverNum * partitionNumPerRange) : 1;
    int totalRanges = (int) Math.ceil(totalPartitionNum * 1.0 / partitionNumPerRange);
    int groupCount = 0;
    int round =  Math.floorDiv(totalRanges, rangePerGroup * serverNum);
    int remainRange = totalRanges % (rangePerGroup * serverNum);
    int lastRoundRangePerGroup = Math.floorDiv(remainRange, serverNum);
    int lastRoundRemainRange = remainRange % serverNum;
    int rangeInGroupCount = 0;

    List<PartitionRange> rangeGroup = Lists.newArrayList();
    for (int start = 0; start < totalPartitionNum; start += partitionNumPerRange) {
      int end = start + partitionNumPerRange - 1;
      PartitionRange range = new PartitionRange(start, end);
      rangeGroup.add(range);
      rangeInGroupCount += 1;

      boolean isLastRound = groupCount >= round * serverNum;
      int groupIndexInRound = groupCount % serverNum;
      if ((!isLastRound && rangeInGroupCount == rangePerGroup)
              || (isLastRound
                      && ((groupIndexInRound < lastRoundRemainRange
                               && rangeInGroupCount == lastRoundRangePerGroup + 1)
                              || (groupIndexInRound >= lastRoundRemainRange
                                      && rangeInGroupCount == lastRoundRangePerGroup)))) {
        res.add(Lists.newArrayList(rangeGroup));
        rangeGroup.clear();
        rangeInGroupCount = 0;
        groupCount += 1;
      }
    }

    if (!rangeGroup.isEmpty()) {
      res.add(Lists.newArrayList(rangeGroup));
    }
    return res;
  }

  public static List<PartitionRange> generateRanges(int totalPartitionNum, int partitionNumPerRange) {
    List<PartitionRange> ranges = new ArrayList<>();
    if (totalPartitionNum <= 0 || partitionNumPerRange <= 0) {
      return ranges;
    }

    for (int start = 0; start < totalPartitionNum; start += partitionNumPerRange) {
      int end = start + partitionNumPerRange - 1;
      PartitionRange range = new PartitionRange(start, end);
      ranges.add(range);
    }

    return ranges;
  }

  public static Map<String, Map<String, String>> extractRemoteStorageConf(String confString) {
    Map<String, Map<String, String>> res = Maps.newHashMap();
    if (StringUtils.isEmpty(confString)) {
      return res;
    }

    String[] clusterConfItems = confString.split(Constants.SEMICOLON_SPLIT_CHAR);
    String msg = "Cluster specific conf[{}] format[cluster,k1=v1;...] is wrong.";
    if (ArrayUtils.isEmpty(clusterConfItems)) {
      LOG.warn(msg, confString);
      return res;
    }

    for (String s : clusterConfItems) {
      String[] item = s.split(Constants.COMMA_SPLIT_CHAR);
      if (ArrayUtils.isEmpty(item) || item.length < 2) {
        LOG.warn(msg, s);
        return Maps.newHashMap();
      }

      String clusterId = item[0];
      Map<String, String> curClusterConf = Maps.newHashMap();
      for (int i = 1; i < item.length; ++i) {
        String[] kv = item[i].split(Constants.EQUAL_SPLIT_CHAR);
        if (ArrayUtils.isEmpty(item) || kv.length != 2) {
          LOG.warn(msg, s);
          return Maps.newHashMap();
        }
        String key = kv[0].trim();
        String value = kv[1].trim();
        if (StringUtils.isEmpty((key)) || StringUtils.isEmpty(value)) {
          LOG.warn("This cluster conf[{}] format is wrong[k=v]", s);
          return Maps.newHashMap();
        }
        curClusterConf.put(key, value);
      }
      res.put(clusterId, curClusterConf);
    }
    return res;
  }
}
