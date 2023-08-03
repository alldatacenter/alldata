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

import com.netease.arctic.ams.api.PropertyNames;
import org.apache.iceberg.relocated.com.google.common.base.MoreObjects;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.Serializable;

/**
 * Common config of Optimizer, it can be extended for custom Optimizer.
 */
public class OptimizerConfig implements Serializable {

  @Option(name = "-a", aliases = "--" + PropertyNames.OPTIMIZER_AMS_URL, usage = "The ams url", required = true)
  private String amsUrl;

  @Option(name = "-p", aliases = "--" + PropertyNames.OPTIMIZER_EXECUTION_PARALLEL,
      usage = "Optimizer execution parallel", required = true)
  private int executionParallel;

  @Option(name = "-m", aliases = "--" + PropertyNames.OPTIMIZER_MEMORY_SIZE,
      usage = "Optimizer memory size(MB)", required = true)
  private int memorySize;

  @Option(name = "-g", aliases = "--" + PropertyNames.OPTIMIZER_GROUP_NAME,
      usage = "Group name optimizer belong", required = true)
  private String groupName;

  @Option(name = "-hb", aliases = "--" + PropertyNames.OPTIMIZER_HEART_BEAT_INTERVAL,
      usage = "Heart beat interval with ams(ms), default 10s")
  private long heartBeat = 10000; // 10 s

  @Option(name = "-eds", aliases = "--" + PropertyNames.OPTIMIZER_EXTEND_DISK_STORAGE,
      usage = "Whether extend storage to disk, default false")
  private boolean extendDiskStorage = false;

  @Option(name = "-dsp", aliases = "--" + PropertyNames.OPTIMIZER_DISK_STORAGE_PATH, usage = "Disk storage path")
  private String diskStoragePath;

  @Option(name = "-msz", aliases = "--" + PropertyNames.OPTIMIZER_MEMORY_STORAGE_SIZE,
      usage = "Memory storage size limit when extending disk storage(MB), default 512MB")
  private long memoryStorageSize = 512; // 512 M

  @Option(name = "-id", aliases = "--" + PropertyNames.RESOURCE_ID, usage = "Resource id")
  private String resourceId;

  public OptimizerConfig() {
  }

  public OptimizerConfig(String[] args) throws CmdLineException {
    CmdLineParser parser = new CmdLineParser(this);
    parser.parseArgument(args);
  }

  public String getAmsUrl() {
    return amsUrl;
  }

  public void setAmsUrl(String amsUrl) {
    this.amsUrl = amsUrl;
  }

  public long getHeartBeat() {
    return heartBeat;
  }

  public void setHeartBeat(long heartBeat) {
    this.heartBeat = heartBeat;
  }

  public int getExecutionParallel() {
    return executionParallel;
  }

  public void setExecutionParallel(int executionParallel) {
    this.executionParallel = executionParallel;
  }

  public int getMemorySize() {
    return memorySize;
  }

  public void setMemorySize(int memorySize) {
    this.memorySize = memorySize;
  }

  public String getGroupName() {
    return groupName;
  }

  public void setGroupName(String groupName) {
    this.groupName = groupName;
  }

  public long getMemoryStorageSize() {
    return memoryStorageSize;
  }

  public void setMemoryStorageSize(long memoryStorageSize) {
    this.memoryStorageSize = memoryStorageSize;
  }

  public boolean isExtendDiskStorage() {
    return extendDiskStorage;
  }

  public void setExtendDiskStorage(boolean extendDiskStorage) {
    this.extendDiskStorage = extendDiskStorage;
  }

  public String getDiskStoragePath() {
    return diskStoragePath;
  }

  public void setDiskStoragePath(String diskStoragePath) {
    this.diskStoragePath = diskStoragePath;
  }

  public String getResourceId() {
    return resourceId;
  }

  public void setResourceId(String resourceId) {
    this.resourceId = resourceId;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("amsUrl", amsUrl)
        .add("executionParallel", executionParallel)
        .add("memorySize", memorySize)
        .add("groupName", groupName)
        .add("heartBeat", heartBeat)
        .add("extendDiskStorage", extendDiskStorage)
        .add("rocksDBBasePath", diskStoragePath)
        .add("memoryStorageSize", memoryStorageSize)
        .add("resourceId", resourceId)
        .toString();
  }
}
