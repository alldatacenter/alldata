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

import java.io.IOException;

import org.apache.uniffle.storage.handler.api.ServerReadHandler;
import org.apache.uniffle.storage.handler.api.ShuffleWriteHandler;
import org.apache.uniffle.storage.request.CreateShuffleReadHandlerRequest;
import org.apache.uniffle.storage.request.CreateShuffleWriteHandlerRequest;


public interface Storage {

  boolean canWrite();

  boolean lockShuffleShared(String shuffleKey);

  boolean unlockShuffleShared(String shuffleKey);

  void updateWriteMetrics(StorageWriteMetrics metrics);

  void updateReadMetrics(StorageReadMetrics metrics);

  ShuffleWriteHandler getOrCreateWriteHandler(CreateShuffleWriteHandlerRequest request) throws IOException;

  ServerReadHandler getOrCreateReadHandler(CreateShuffleReadHandlerRequest request);

  void removeHandlers(String appId);

  void createMetadataIfNotExist(String shuffleKey);

  String getStoragePath();

  String getStorageHost();
}
