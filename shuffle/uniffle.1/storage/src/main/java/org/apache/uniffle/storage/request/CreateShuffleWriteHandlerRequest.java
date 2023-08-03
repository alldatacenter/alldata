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

package org.apache.uniffle.storage.request;

import org.apache.hadoop.conf.Configuration;

public class CreateShuffleWriteHandlerRequest {

  private String storageType;
  private String appId;
  private int shuffleId;
  private int startPartition;
  private int endPartition;
  private String[] storageBasePaths;
  private String fileNamePrefix;
  private Configuration conf;
  private int storageDataReplica;
  private String user;
  private int maxFileNumber;

  public CreateShuffleWriteHandlerRequest(
      String storageType,
      String appId,
      int shuffleId,
      int startPartition,
      int endPartition,
      String[] storageBasePaths,
      String fileNamePrefix,
      Configuration conf,
      int storageDataReplica,
      String user) {
    this(
        storageType,
        appId,
        shuffleId,
        startPartition,
        endPartition,
        storageBasePaths,
        fileNamePrefix,
        conf,
        storageDataReplica,
        user,
        1
    );
  }

  public CreateShuffleWriteHandlerRequest(
      String storageType,
      String appId,
      int shuffleId,
      int startPartition,
      int endPartition,
      String[] storageBasePaths,
      String fileNamePrefix,
      Configuration conf,
      int storageDataReplica,
      String user,
      int maxFileNumber) {
    this.storageType = storageType;
    this.appId = appId;
    this.shuffleId = shuffleId;
    this.startPartition = startPartition;
    this.endPartition = endPartition;
    this.storageBasePaths = storageBasePaths;
    this.fileNamePrefix = fileNamePrefix;
    this.conf = conf;
    this.storageDataReplica = storageDataReplica;
    this.user = user;
    this.maxFileNumber = maxFileNumber;
  }

  public String getStorageType() {
    return storageType;
  }

  public String getAppId() {
    return appId;
  }

  public int getShuffleId() {
    return shuffleId;
  }

  public int getStartPartition() {
    return startPartition;
  }

  public int getEndPartition() {
    return endPartition;
  }

  public String[] getStorageBasePaths() {
    return storageBasePaths;
  }

  public String getFileNamePrefix() {
    return fileNamePrefix;
  }

  public Configuration getConf() {
    return conf;
  }

  public int getStorageDataReplica() {
    return storageDataReplica;
  }

  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public int getMaxFileNumber() {
    return maxFileNumber;
  }
}
