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

package com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.schema;

import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.syncer.AbstractMetaSyncer;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created 2020/11/11.
 */
public class FileSystemMetaManager implements Serializable {

  protected final BitSailConfiguration jobConf;

  private volatile AtomicBoolean needUpdate;

  public FileSystemMetaManager(BitSailConfiguration jobConf) {
    this.jobConf = jobConf;
    this.needUpdate = new AtomicBoolean(false);
  }

  public void open() {

  }

  /**
   * Trigger on processing time
   */
  public void updateFileSystemMeta(FileSystemMeta fileSystemMeta, int taskId) {
    //empty body
  }

  /**
   * The return value means should we schedule update meta info.
   */
  public boolean shouldScheduleUpdate() {
    return false;
  }

  public boolean shouldUpdate() {
    return needUpdate.get();
  }

  public AbstractMetaSyncer createMetaSyncer() {
    return null;
  }

  public void setUpdateValue(boolean newValue) {
    needUpdate.set(newValue);
  }
}
