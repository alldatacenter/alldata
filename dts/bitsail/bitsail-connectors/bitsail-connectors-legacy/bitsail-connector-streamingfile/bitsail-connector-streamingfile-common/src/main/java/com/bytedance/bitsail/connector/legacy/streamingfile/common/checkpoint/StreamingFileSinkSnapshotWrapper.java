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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

import static com.bytedance.bitsail.connector.legacy.streamingfile.common.validator.StreamingFileSystemValidator.DUMP_DEFAULT_WRAPPER_TASK_ID;

/**
 * Created 2020/7/28.
 */
public class StreamingFileSinkSnapshotWrapper extends AbstractSnapshotWrapper {
  private static final Logger LOG =
      LoggerFactory.getLogger(StreamingFileSinkSnapshotWrapper.class);

  private final double snapshotSuccessRate;
  private final double asyncSnapshotSuccessRate;
  private final Random snapshotSuccessDice;

  public StreamingFileSinkSnapshotWrapper(int subTaskId, BitSailConfiguration jobConf) {
    super(subTaskId, jobConf);
    snapshotSuccessDice = new Random();
    snapshotSuccessRate = jobConf.get(FileSystemCommonOptions.SnapshotOptions.DUMP_WRAPPER_SNAPSHOT_SUCCESS_RATE);
    asyncSnapshotSuccessRate = jobConf.get(FileSystemCommonOptions.SnapshotOptions.DUMP_WRAPPER_ASYNC_SNAPSHOT_SUCCESS_RATE);
    LOG.info("Subtask {} snapshot success rate {}.", subTaskId, snapshotSuccessRate);
  }

  @Override
  protected void snapshotInternal() throws Exception {
    if (!firstSnapshot && snapshotSuccessDice.nextDouble() > snapshotSuccessRate) {
      LOG.warn("Subtask {} snapshot will failed.", subTaskId);
      throw new RandomSnapshotException("Hit random snapshot failed.");
    }
  }

  @Override
  protected void asyncSnapshotStateInternal() throws Exception {
    if (!firstSnapshot && snapshotSuccessDice.nextDouble() > asyncSnapshotSuccessRate) {
      if (wrapperTaskId == DUMP_DEFAULT_WRAPPER_TASK_ID || wrapperTaskId == subTaskId) {
        LOG.warn("Subtask {} snapshot will failed.", subTaskId);
        throw new RandomSnapshotException("Hit random snapshot failed.");
      }
    }
  }

  @Override
  protected void notifyInternal() throws Exception {
    firstSnapshot = false;
  }

  private static class RandomSnapshotException extends RuntimeException {

    public RandomSnapshotException(String message) {
      super(message);
    }
  }

}
