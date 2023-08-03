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

package org.apache.uniffle.storage.common;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.hadoop.conf.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.uniffle.storage.handler.api.ServerReadHandler;
import org.apache.uniffle.storage.handler.api.ShuffleWriteHandler;
import org.apache.uniffle.storage.handler.impl.HdfsShuffleWriteHandler;
import org.apache.uniffle.storage.handler.impl.PooledHdfsShuffleWriteHandler;
import org.apache.uniffle.storage.request.CreateShuffleReadHandlerRequest;
import org.apache.uniffle.storage.request.CreateShuffleWriteHandlerRequest;

public class HdfsStorage extends AbstractStorage {

  private static final Logger LOG = LoggerFactory.getLogger(HdfsStorage.class);

  private final String storagePath;
  private final Configuration conf;
  private String storageHost;

  public HdfsStorage(String path, Configuration conf) {
    this.storagePath = path;
    this.conf = conf;
    try {
      URI uri = new URI(path);
      storageHost = uri.getHost();
    } catch (URISyntaxException e) {
      storageHost = "";
      LOG.warn("Invalid format of remoteStoragePath to get storage host, {}", path);
    }
  }

  @Override
  public String getStoragePath() {
    return storagePath;
  }

  @Override
  public String getStorageHost() {
    return storageHost;
  }

  @Override
  public boolean canWrite() {
    return true;
  }

  @Override
  public boolean lockShuffleShared(String shuffleKey) {
    return true;
  }

  @Override
  public boolean unlockShuffleShared(String shuffleKey) {
    return true;
  }

  @Override
  public void updateReadMetrics(StorageReadMetrics metrics) {
    // do nothing
  }

  @Override
  public void updateWriteMetrics(StorageWriteMetrics metrics) {
    // do nothing
  }

  @Override
  ShuffleWriteHandler newWriteHandler(CreateShuffleWriteHandlerRequest request) {
    try {
      String user = request.getUser();
      if (request.getMaxFileNumber() == 1) {
        return new HdfsShuffleWriteHandler(
            request.getAppId(),
            request.getShuffleId(),
            request.getStartPartition(),
            request.getEndPartition(),
            storagePath,
            request.getFileNamePrefix(),
            conf,
            user
        );
      } else {
        return new PooledHdfsShuffleWriteHandler(
            request.getAppId(),
            request.getShuffleId(),
            request.getStartPartition(),
            request.getEndPartition(),
            storagePath,
            request.getFileNamePrefix(),
            conf,
            user,
            request.getMaxFileNumber()
        );
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected ServerReadHandler newReadHandler(CreateShuffleReadHandlerRequest request) {
    throw new RuntimeException("Hdfs storage don't support to read from sever");
  }

  @Override
  public void createMetadataIfNotExist(String shuffleKey) {
    // do nothing
  }

  public Configuration getConf() {
    return conf;
  }
}
