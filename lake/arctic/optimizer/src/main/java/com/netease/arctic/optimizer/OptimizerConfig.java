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

package com.netease.arctic.optimizer;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.Serializable;

/**
 * Common config of Optimizer, it can be extended for custom Optimizer.
 */
public class OptimizerConfig implements Serializable {
  @Option(name = "-a", aliases = "--ams-url", usage = "The ams url")
  private String amsUrl;
  @Option(name = "-p", aliases = "--executor-parallel", usage = "Optimize parallel")
  private int executorParallel;
  @Option(name = "-q", aliases = "--queue-id", usage = "queue")
  private int queueId;
  @Option(name = "-id", aliases = "--optimizer-id", usage = "Optimizer uuid")
  private String optimizerId = "unknown";
  @Option(name = "-hb", aliases = "--heart-beat", usage = "heart beat interval (ms)")
  private long heartBeat = 10000; // 10 s

  // support register optimizer
  @Option(name = "-qn", aliases = "--queue-name", usage = "queue name")
  private String queueName;

  @Option(name = "-m", aliases = "--memory-size", usage = "memory size")
  private int executorMemory;

  @Option(name = "-es", aliases = "--enable-spill-map", usage = "whether enable spill map in optimizer")
  private String enableSpillMap = "false";

  @Option(name = "-mm", aliases = "--max-delete-memory-size", usage = "max delete map byte size in memory(MB)")
  private long maxInMemorySize = 512; // 512 M

  @Option(name = "-rp", aliases = "--rock-base-path", usage = "rocks db base path")
  private String rocksDBBasePath;

  public OptimizerConfig() {
  }

  public OptimizerConfig(String[] args) throws CmdLineException {
    CmdLineParser parser = new CmdLineParser(this);
    parser.parseArgument(args);
  }

  public String getAmsUrl() {
    return amsUrl;
  }

  public long getHeartBeat() {
    return heartBeat;
  }

  public void setHeartBeat(long heartBeat) {
    this.heartBeat = heartBeat;
  }

  public void setAmsUrl(String amsUrl) {
    this.amsUrl = amsUrl;
  }

  public int getExecutorParallel() {
    return executorParallel;
  }

  public void setExecutorParallel(int executorParallel) {
    this.executorParallel = executorParallel;
  }

  public int getQueueId() {
    return queueId;
  }

  public void setQueueId(int queueId) {
    this.queueId = queueId;
  }

  public String getOptimizerId() {
    return optimizerId;
  }

  public void setOptimizerId(String optimizerId) {
    this.optimizerId = optimizerId;
  }

  public String getQueueName() {
    return queueName;
  }

  public void setQueueName(String queueName) {
    this.queueName = queueName;
  }

  public int getExecutorMemory() {
    return executorMemory;
  }

  public void setExecutorMemory(int executorMemory) {
    this.executorMemory = executorMemory;
  }

  public long getMaxInMemorySize() {
    return maxInMemorySize;
  }

  public void setMaxInMemorySize(long maxInMemorySize) {
    this.maxInMemorySize = maxInMemorySize;
  }

  public String getEnableSpillMap() {
    return enableSpillMap;
  }

  public void setEnableSpillMap(String enableSpillMap) {
    this.enableSpillMap = enableSpillMap;
  }

  public String getRocksDBBasePath() {
    return rocksDBBasePath;
  }

  public void setRocksDBBasePath(String rocksDBBasePath) {
    this.rocksDBBasePath = rocksDBBasePath;
  }

  @Override
  public String toString() {
    return "OptimizerConfig{" +
        "amsUrl='" + amsUrl + '\'' +
        ", executorParallel=" + executorParallel +
        ", queueId=" + queueId +
        ", optimizerId='" + optimizerId + '\'' +
        ", heartBeat=" + heartBeat +
        ", enableSpillMap='" + enableSpillMap + '\'' +
        ", maxInMemorySize=" + maxInMemorySize +
        ", rocksDBBasePath='" + rocksDBBasePath + '\'' +
        '}';
  }

}
