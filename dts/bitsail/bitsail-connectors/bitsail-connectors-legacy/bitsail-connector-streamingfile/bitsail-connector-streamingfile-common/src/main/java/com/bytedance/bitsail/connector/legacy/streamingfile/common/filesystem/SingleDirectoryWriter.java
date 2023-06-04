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

package com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem;

import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.rollingpolicies.PartFileInfo;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.rollingpolicies.RollingPolicy;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.schema.FileSystemMetaManager;

import org.apache.flink.annotation.Internal;
import org.apache.flink.api.common.io.OutputFormat;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.core.fs.Path;

import java.util.Map;

/**
 * {@link PartitionWriter} for single directory writer. It just use one format to write.
 *
 * @param <T> The type of the consumed records.
 */
@Internal
public class SingleDirectoryWriter<T> extends PartitionWriter<T> {

  private OutputFormat<T> format;
  private PartFileInfo partFileInfo;

  public SingleDirectoryWriter(
      Context<T> context,
      PartitionTempFileManager manager,
      PartitionComputer<T> computer,
      RollingPolicy<T> rollingPolicy,
      FileSystemMetaManager fileSystemMetaManager) {
    super(context, computer, manager, rollingPolicy, fileSystemMetaManager);
  }

  @Override
  public Tuple2<OutputFormat<T>, PartFileInfo> getOrCreateFormatForPartition(String partition) throws Exception {
    if (format == null) {
      long timestamp = System.currentTimeMillis();
      Path path = manager.getStaticPartSpecs().size() == 0 ?
          manager.createPartitionFile(timestamp) :
          manager.createPartitionFile(timestamp, PartitionPathUtils.generatePartitionPath(manager.getStaticPartSpecs()));

      Tuple2<OutputFormat<T>, PartFileInfo> currentFormatInfo = createFormatForPath(path, timestamp, partition);

      format = currentFormatInfo.f0;
      partFileInfo = currentFormatInfo.f1;
    }
    return new Tuple2<>(format, partFileInfo);
  }

  @Override
  public void closeFormatForPartition(String partition) throws Exception {
    if (format != null) {
      format.close();
      format = null;
    }
  }

  @Override
  public void onProcessingTime(long timestamp) throws Exception {
    if (format != null && rollingPolicy.shouldRollOnProcessingTime(partFileInfo, timestamp)) {
      format.close();
      format = null;
    }
  }

  @Override
  public void close(long jobMinTimestamp, long checkpointId, boolean clearPartFileInfo) throws Exception {
    if (format != null) {
      format.close();
      format = null;
    }
    super.close(jobMinTimestamp, checkpointId, clearPartFileInfo);
  }

  @Override
  public Map<String, OutputFormat<T>> getOutputFormats() {
    return null;
  }
}
