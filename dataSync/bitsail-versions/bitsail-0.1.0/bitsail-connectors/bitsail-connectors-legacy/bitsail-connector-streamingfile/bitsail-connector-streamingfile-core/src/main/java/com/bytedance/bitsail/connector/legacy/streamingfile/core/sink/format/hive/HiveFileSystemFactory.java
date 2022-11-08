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

package com.bytedance.bitsail.connector.legacy.streamingfile.core.sink.format.hive;

import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.OutputFormatFactory;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.TableMetaStoreFactory;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.schema.FileSystemMetaManager;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.option.FileSystemSinkOptions;
import com.bytedance.bitsail.connector.legacy.streamingfile.core.sink.FileSystemCommitter;
import com.bytedance.bitsail.connector.legacy.streamingfile.core.sink.format.AbstractFileSystemFactory;
import com.bytedance.bitsail.connector.legacy.streamingfile.core.sink.schema.HiveFileSystemMetaManager;

import org.apache.flink.api.common.io.OutputFormat;
import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.core.fs.Path;
import org.apache.flink.types.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;

/**
 * @class: HiveFileSystemFactory
 * @desc:
 **/
public class HiveFileSystemFactory<IN extends Row> extends AbstractFileSystemFactory<IN> {
  private static final Logger LOG = LoggerFactory.getLogger(HiveFileSystemFactory.class);

  public HiveFileSystemFactory(final BitSailConfiguration jobConf) {
    super(jobConf);
  }

  public static HiveTableMetaStoreFactory getHiveMetaFromTable(BitSailConfiguration jobConf) {
    String dbName = jobConf.get(FileSystemSinkOptions.DB_NAME);
    String tableName = jobConf.get(FileSystemSinkOptions.TABLE_NAME);
    String metaStoreProperties = jobConf.get(FileSystemSinkOptions.HIVE_METASTORE_PROPERTIES);

    return new HiveTableMetaStoreFactory(dbName, tableName, metaStoreProperties);
  }

  @Override
  public String getOutputDir() {
    try {
      HiveTableMetaStoreFactory hiveMeta = getHiveMetaFromTable(jobConf);
      return hiveMeta.createTableMetaStore().getLocationPath().toString();
    } catch (Exception e) {
      throw new RuntimeException("Error while calling HiveTableMetaStore::getLocationPath. " + e.getMessage(), e);
    }
  }

  @Override
  public OutputFormatFactory<IN> createOutputFormatFactory() {
    return new HiveOutputFormatFactory<>(jobConf);
  }

  @Override
  public FileSystemCommitter createFileSystemCommitter() throws Exception {
    String outputDir = getOutputDir();
    String stagingDir = toStagingDir(outputDir);
    LOG.info("Hive OutputDir={}", outputDir);
    LOG.info("Hive StagingDir={}", stagingDir);

    if (hdfsOverwrite) {
      Path outputPath = new Path(outputDir);
      if (FileSystem.get(outputPath.toUri()).exists(outputPath)) {
        FileSystem.get(outputPath.toUri()).delete(outputPath, true);
        LOG.info("Delete Hive OutputDir={}", outputDir);
      }

      Path stagingPath = new Path(stagingDir);
      if (FileSystem.get(stagingPath.toUri()).exists(stagingPath)) {
        FileSystem.get(stagingPath.toUri()).delete(stagingPath, true);
        LOG.info("Delete Hive StagingDir={}", stagingDir);
      }
    }

    return new FileSystemCommitter(
        FileSystem::get,
        createMetaStoreFactory(),
        false,
        new Path(stagingDir),
        new LinkedHashMap<>(),
        partitionKeys.size(),
        null,
        jobConf);
  }

  @Override
  public HivePartitionComputer<IN> createPartitionComputer() throws Exception {
    return new HivePartitionComputer<>(jobConf);
  }

  @Override
  public TableMetaStoreFactory createMetaStoreFactory() {
    return getHiveMetaFromTable(jobConf);
  }

  public static class HiveOutputFormatFactory<IN extends Row> implements OutputFormatFactory<IN> {
    private final BitSailConfiguration jobConf;

    private HiveOutputFormatFactory(BitSailConfiguration jobConf) {
      this.jobConf = jobConf;
    }

    @Override
    public OutputFormat<IN> createOutputFormat(Path path, FileSystemMetaManager manager) {
      return new HiveWritableOutputFormat<>(jobConf, path, (HiveFileSystemMetaManager) manager);
    }
  }
}
