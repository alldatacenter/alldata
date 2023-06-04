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

package com.bytedance.bitsail.connector.legacy.streamingfile.common.checkpoint;

import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.option.FileSystemCommonOptions;

import java.io.Serializable;

/**
 * Created 2020/7/28.
 */
public abstract class AbstractSnapshotWrapper implements Serializable {

  protected final BitSailConfiguration jobConf;

  protected final int subTaskId;
  protected final int wrapperTaskId;
  private final boolean enableWrapperSnapshot;
  protected boolean firstSnapshot;

  public AbstractSnapshotWrapper(int subTaskId, BitSailConfiguration jobConf) {
    this.subTaskId = subTaskId;
    this.jobConf = jobConf;
    this.enableWrapperSnapshot = jobConf.get(FileSystemCommonOptions.SnapshotOptions.DUMP_WRAPPER_SNAPSHOT_ENABLE);
    this.firstSnapshot = true;
    this.wrapperTaskId = jobConf.get(FileSystemCommonOptions.SnapshotOptions.DUMP_WRAPPER_TASK_ID);
  }

  public final void snapshotState() throws Exception {
    if (checkEnable()) {
      snapshotInternal();
    }
  }

  public final void asyncSnapshotState() throws Exception {
    if (checkEnable()) {
      asyncSnapshotStateInternal();
    }
  }

  public final void notifyCheckpointComplete() throws Exception {
    if (checkEnable()) {
      notifyInternal();
    }
  }

  protected abstract void notifyInternal() throws Exception;

  protected abstract void snapshotInternal() throws Exception;

  protected abstract void asyncSnapshotStateInternal() throws Exception;

  private boolean checkEnable() {
    return enableWrapperSnapshot;
  }

}
