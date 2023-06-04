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

import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.rollingpolicies.RollingPolicy;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.schema.FileSystemMetaManager;

import org.apache.flink.annotation.Internal;

import java.io.Serializable;

/**
 * Factory of {@link PartitionWriter} to avoid virtual function calls.
 */
@Internal
public interface PartitionWriterFactory<T> extends Serializable {

  /**
   * Util for get a {@link PartitionWriterFactory}.
   */
  static <T> PartitionWriterFactory<T> get(
      boolean dynamicPartition) {
    if (dynamicPartition) {
      return DynamicPartitionWriter::new;
    } else {
      return SingleDirectoryWriter::new;
    }
  }

  PartitionWriter<T> create(
      PartitionWriter.Context<T> context,
      PartitionTempFileManager manager,
      PartitionComputer<T> computer,
      RollingPolicy<T> rollingPolicy,
      FileSystemMetaManager fileSystemMetaManager) throws Exception;
}
