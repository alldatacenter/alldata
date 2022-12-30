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

package com.bytedance.bitsail.connector.legacy.streamingfile.core.sink;

import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.datasink.file.AbstractPartitionCommitter;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.datasink.file.AbstractPartitionComputer;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.schema.FileSystemMetaManager;

import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.runtime.state.FunctionInitializationContext;
import org.apache.flink.runtime.state.FunctionSnapshotContext;
import org.apache.flink.types.Row;

import java.io.IOException;
import java.io.Serializable;

public interface WatermarkEmitter extends Serializable {
  static WatermarkEmitter createDefault(BitSailConfiguration jobConf,
                                        AbstractPartitionCommitter partitionCommitter) {
    return new DefaultWatermarkEmitter(jobConf,
        partitionCommitter);
  }

  void open(RuntimeContext runtimeContext, AbstractPartitionComputer partitionComputer, FileSystemMetaManager fileSystemMetaManager);

  void initializeState(FunctionInitializationContext context, RuntimeContext runtimeContext) throws Exception;

  void snapshotState(FunctionSnapshotContext context, long nextCheckpointId, int partFileSize) throws Exception;

  void notifyCheckpointComplete(long checkpointId, long nextCheckpointId) throws Exception;

  void onProcessingTime(long timestamp) throws IOException;

  void emit(Row row, boolean eventTimeSafeModeEnabled);

  void stimulate();
}
