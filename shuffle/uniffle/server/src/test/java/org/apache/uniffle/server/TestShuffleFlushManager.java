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

package org.apache.uniffle.server;

import java.util.concurrent.Executor;

import org.apache.uniffle.server.storage.StorageManager;

public class TestShuffleFlushManager extends ShuffleFlushManager {
  public TestShuffleFlushManager(ShuffleServerConf shuffleServerConf, String shuffleServerId,
                                 ShuffleServer shuffleServer, StorageManager storageManager) {
    super(shuffleServerConf, shuffleServerId, shuffleServer, storageManager);
  }

  @Override
  protected void eventLoop() {
    // do nothing
  }

  @Override
  protected Executor createFlushEventExecutor() {
    return Runnable::run;
  }

  public void flush() {
    while (!flushQueue.isEmpty()) {
      processNextEvent();
    }
  }

}
