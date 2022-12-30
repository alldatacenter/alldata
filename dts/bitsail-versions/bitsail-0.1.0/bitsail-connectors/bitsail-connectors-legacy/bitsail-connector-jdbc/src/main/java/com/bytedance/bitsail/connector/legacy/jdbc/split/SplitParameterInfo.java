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

package com.bytedance.bitsail.connector.legacy.jdbc.split;

import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.exception.CommonErrorCode;
import com.bytedance.bitsail.connector.legacy.jdbc.model.DbClusterInfo;
import com.bytedance.bitsail.connector.legacy.jdbc.model.DbShardInfo;

import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * @desc:
 */
@NoArgsConstructor
@SuppressWarnings("checkstyle:MagicNumber")
public class SplitParameterInfo<K> implements Serializable {
  private static final  Logger LOG = LoggerFactory.getLogger(SplitParameterInfo.class);

  private TaskGroupInfo taskGroupInfo;

  /**
   * The max parallelism num
   */
  private int taskGroupNum;

  private long totalSplitNum = -1;

  public SplitParameterInfo(int taskGroupNum) {
    this.taskGroupNum = taskGroupNum;
    taskGroupInfo = new TaskGroupInfo();
  }

  /**
   * input splits:
   * host1 -> 1 4 7
   * host2 -> 2 5 8 10
   * host3 -> 3 6 9 11 12 13
   * <p>
   * input taskGroupNum: 2
   * <p>
   * output:
   * Group0 -> 1 3 5 7 9 11 13
   * Group1 -> 2 4 6 8 10 12
   *
   * @param splitsMap
   */
  public void assign(Map<Integer, List<TableRangeInfo<K>>> splitsMap, DbClusterInfo dbClusterInfo) {
    int globalSplitId = 0;
    Map<String, List<SplitRangeInfo>> splitTasks = new HashMap<>();

    //Step 1: All tasks group by host
    for (Map.Entry<Integer, List<TableRangeInfo<K>>> entry : splitsMap.entrySet()) {
      int shardNum = entry.getKey();
      List<TableRangeInfo<K>> sortedTableRangeMap = entry.getValue();
      List<DbShardInfo> slaves = dbClusterInfo.getSlaves(shardNum);

      int currentSlave = 0;
      for (TableRangeInfo<K> split : sortedTableRangeMap) {
        DbShardInfo oneSlave = slaves.get(currentSlave++ % slaves.size());
        String host = oneSlave.getHost();
        List<SplitRangeInfo> tasksInOneHost = splitTasks.get(host);
        if (null == tasksInOneHost) {
          tasksInOneHost = new ArrayList<>();
          splitTasks.put(host, tasksInOneHost);
        }
        SplitRangeInfo splitRangeInfo = new SplitRangeInfo(globalSplitId, split, oneSlave.getDbURL());
        tasksInOneHost.add(splitRangeInfo);
        globalSplitId++;
      }
    }

    totalSplitNum = globalSplitId;

    //Step 2: Assign each host's tasks
    Map<String, Integer> offsetMap = new HashMap<>();
    int currentGlobalIndex = 0;
    while (currentGlobalIndex < globalSplitId) {
      for (Map.Entry<String, List<SplitRangeInfo>> entry : splitTasks.entrySet()) {
        String host = entry.getKey();
        List<SplitRangeInfo> tasksInOneHost = entry.getValue();

        Integer currentHostIndex = offsetMap.get(host);
        if (null == currentHostIndex) {
          currentHostIndex = 0;
          offsetMap.put(host, currentHostIndex);
        }
        if (currentHostIndex < tasksInOneHost.size()) {
          int taskGroupId = currentGlobalIndex % taskGroupNum;
          addSplitToTaskGroup(taskGroupId, tasksInOneHost.get(currentHostIndex));

          currentHostIndex += 1;
          offsetMap.put(host, currentHostIndex);

          currentGlobalIndex++;
        }
      }
    }

  }

  public void addSplitToTaskGroup(int taskGroupId, SplitRangeInfo splitRangeInfo) {
    List<SplitRangeInfo> oneGroupList = taskGroupInfo.get(taskGroupId);
    oneGroupList.add(splitRangeInfo);
  }

  public int getTaskGroupSize() {
    return taskGroupNum;
  }

  public long getTotalSplitNum() {
    if (totalSplitNum == -1) {
      throw new IllegalStateException("Total split num is -1, which indicates" +
          " SplitParameterInfo hasn't been initialized properly.");
    }
    return totalSplitNum;
  }

  public List<SplitRangeInfo> getOneTaskGroupParametersInfo(int taskGroupId) {
    return taskGroupInfo.get(taskGroupId);
  }

  @Override
  public String toString() {
    StringBuilder summary = new StringBuilder();
    summary.append("TaskGroup(Parallelism) Num: " + taskGroupNum).append("\n");
    for (int i = 0; i < taskGroupInfo.size(); i++) {
      summary.append("[Group ").append(i).append("]: ").append("\n");
      List<SplitRangeInfo> oneGroup = taskGroupInfo.get(i);
      for (SplitRangeInfo splitRangeInfo : oneGroup) {
        summary.append(splitRangeInfo.getDbURL()).append("\t")
            .append("TableName: ").append(splitRangeInfo.getQuoteTableWithSchema()).append("\t")
            .append("Begin: ").append(splitRangeInfo.getBeginPos())
            .append("\t").append("End: ").append(splitRangeInfo.getEndPos())
            .append("\n");
      }
    }
    return summary.toString();
  }

  private class TaskGroupInfo implements Serializable {
    private transient List<List<SplitRangeInfo>> taskGroupInfo;
    private byte[] taskGroupInfoBytes;

    public TaskGroupInfo() {
      this.taskGroupInfo = new ArrayList<>(taskGroupNum);
      for (int i = 0; i < taskGroupNum; i++) {
        List<SplitRangeInfo> oneGroup = new ArrayList<SplitRangeInfo>();
        taskGroupInfo.add(oneGroup);
      }
    }

    private List<SplitRangeInfo> get(int i) {
      if (taskGroupInfo == null) {
        decompress();
      }
      return taskGroupInfo.get(i);
    }

    private int size() {
      if (taskGroupInfo == null) {
        decompress();
      }
      return taskGroupInfo.size();
    }

    // ------------------------------------------------
    // Custom serialization
    // ------------------------------------------------
    private void writeObject(ObjectOutputStream out) throws IOException {
      compress();
      out.defaultWriteObject();
    }

    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException {
      in.defaultReadObject();
    }

    private void compress() throws IOException {
      if (taskGroupInfo == null) {
        return;
      }

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      GZIPOutputStream gzipOut = new GZIPOutputStream(baos, 64 * 1024);
      ObjectOutputStream objectOut = new ObjectOutputStream(gzipOut);
      objectOut.writeObject(taskGroupInfo);
      objectOut.close();
      baos.flush();
      this.taskGroupInfoBytes = baos.toByteArray();
      this.taskGroupInfo = null;
      LOG.info("Split info serialization compressed to bytes: " + taskGroupInfoBytes.length);
      baos.close();
    }

    private void decompress() {
      if (taskGroupInfoBytes == null || taskGroupInfoBytes.length == 0) {
        throw BitSailException.asBitSailException(CommonErrorCode.RUNTIME_ERROR,
            "Error while decompressing TaskGroupInfo in SplitParameterInfo: bytes array is null.");
      }

      try {
        ByteArrayInputStream bais = new ByteArrayInputStream(taskGroupInfoBytes);
        GZIPInputStream gzipIn = new GZIPInputStream(bais);
        ObjectInputStream objectIn = new ObjectInputStream(gzipIn);
        taskGroupInfo = (List) objectIn.readObject();
        objectIn.close();

        taskGroupInfoBytes = null;
        LOG.info("Split info recovered from bytes.");
      } catch (Exception e) {
        throw BitSailException.asBitSailException(CommonErrorCode.RUNTIME_ERROR,
            "Error while decompressing TaskGroupInfo in SplitParameterInfo.", e);
      }
    }
  }
}
