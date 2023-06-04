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

package com.bytedance.bitsail.connector.legacy.streamingfile.core.sink.format;

import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.datasink.common.PartitionInfo;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.OutputFormatFactory;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.PartitionComputer;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.PartitionWriterFactory;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.TableMetaStoreFactory;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.option.FileSystemSinkOptions;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.tools.PartitionUtils;
import com.bytedance.bitsail.connector.legacy.streamingfile.core.sink.FileSystemCommitter;

import org.apache.flink.core.fs.Path;
import org.apache.flink.types.Row;

import java.io.Serializable;
import java.util.List;

import static com.bytedance.bitsail.connector.legacy.streamingfile.common.validator.StreamingFileSystemValidator.DUMP_TMP_SUFFIX;

/**
 * @class: AbstractFileSystemFactory
 * @desc:
 **/
public abstract class AbstractFileSystemFactory<IN extends Row> implements Serializable {

  protected final BitSailConfiguration jobConf;
  protected final Boolean hdfsOverwrite;
  protected final List<PartitionInfo> partitionKeys;

  public AbstractFileSystemFactory(final BitSailConfiguration jobConf) {
    this.jobConf = jobConf;
    this.hdfsOverwrite = jobConf.get(FileSystemSinkOptions.HDFS_OVERWRITE);

    this.partitionKeys = PartitionUtils.getPartitionInfo(jobConf);
  }

  public abstract String getOutputDir();

  public abstract OutputFormatFactory<IN> createOutputFormatFactory() throws Exception;

  public abstract FileSystemCommitter createFileSystemCommitter() throws Exception;

  public abstract PartitionComputer<IN> createPartitionComputer() throws Exception;

  public abstract TableMetaStoreFactory createMetaStoreFactory() throws Exception;

  public PartitionWriterFactory<IN> createPartitionWriterFactory() {
    return PartitionWriterFactory.get(true);
  }

  public String toStagingDir(String finalDir) {
    String res = finalDir;
    if (!finalDir.endsWith(Path.SEPARATOR)) {
      res += Path.SEPARATOR;
    }
    res += DUMP_TMP_SUFFIX;

    return res;
  }
}
