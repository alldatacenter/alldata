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

package com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.directory;

import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.datasink.common.PartitionInfo;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.option.FileSystemCommonOptions;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.validator.StreamingFileSystemValidator;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.core.fs.Path;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Created 2020/5/28.
 */
public class PartitionDirectoryManager {

  public static DirectoryType getDirectoryType(BitSailConfiguration jobConf) {
    String directoryMode =
        jobConf.getUnNecessaryOption(FileSystemCommonOptions.CommitOptions.DUMP_DIRECTORY_FREQUENCY, StreamingFileSystemValidator.DUMP_DIRECTORY_FREQUENCY_DAY);
    String formatType = jobConf.get(FileSystemCommonOptions.DUMP_FORMAT_TYPE);
    if (StreamingFileSystemValidator.HIVE_FORMAT_TYPE_VALUE.equalsIgnoreCase(formatType)) {
      return DirectoryType.HIVE_DIRECTORY_MODE;
    }

    if (StreamingFileSystemValidator.DUMP_DIRECTORY_FREQUENCY_HOUR.equalsIgnoreCase(directoryMode)) {
      return DirectoryType.HDFS_DIRECTORY_HOUR_MODE;
    }
    return DirectoryType.HDFS_DIRECTORY_DAY_MODE;
  }

  public enum DirectoryType {
    /**
     * use '/date={date}/' as base dir of dump path
     * support hdfs streaming sink and batch sink, output path is as follows:
     * streaming path: taskTmpDir/date={date}/{hour}_task_dorado_{job_id}_{task_id}_{cp}.{timestamp}.{compression}
     */
    HDFS_DIRECTORY_DAY_MODE {
      @Override
      public LinkedHashMap<String, String> getPartSpec(LinkedHashMap<String, String> partSpec, int hourIndex) {
        LinkedHashMap<String, String> result = new LinkedHashMap<>(partSpec.size());
        int index = 0;
        for (Map.Entry<String, String> entry : partSpec.entrySet()) {
          if (hourIndex != index) {
            result.put(entry.getKey(), entry.getValue());
          }
          index++;
        }
        return result;
      }

      @Override
      public Path createPartitionDirectionPath(Path parentDir,
                                               int hourIndex,
                                               String... partitions) {
        Path partitionDir = parentDir;
        if (ArrayUtils.isNotEmpty(partitions)) {
          String[] partSpecs = StringUtils.split(partitions[0], "/");
          int index = 0;
          for (; index < partSpecs.length; index++) {
            if (StringUtils.isNoneEmpty(partSpecs[index]) && hourIndex != index) {
              partitionDir = new Path(partitionDir, partSpecs[index]);
            }
          }
        }
        return partitionDir;
      }

      @Override
      public String getPartitionFileName(String fileName, int hourIndex, String... partitions) {
        if (ArrayUtils.isNotEmpty(partitions)) {
          String[] partSpecs = StringUtils.split(partitions[0], "/");

          // move hour partition to file name
          String hourStr = partSpecs[hourIndex].split("=")[1];
          return hourStr + "_" + fileName;
        }
        return fileName;
      }

      @Override
      public int getPartitionSize(List<PartitionInfo> partitionInfos) {
        return CollectionUtils.size(partitionInfos) - 1;
      }

      @Override
      public Path getEmptyFilePath(Path locationPath, String dateStr, String hourStr, String filename) {
        filename = hourStr + "_" + filename;
        return new Path(locationPath, filename);
      }
    },

    /**
     * Split directory as hour.
     */
    HDFS_DIRECTORY_HOUR_MODE,

    /**
     * Split directory as hive mode.
     */
    HIVE_DIRECTORY_MODE;

    @VisibleForTesting
    public Path createPartitionFilePath(Supplier<String> fileSupplier,
                                        Path parentDir,
                                        int hourIndex,
                                        String... partitions) {
      return createPartitionFilePath(fileSupplier.get(), parentDir, hourIndex, partitions);
    }

    public Path createPartitionFilePath(Function<Long, String> fileFunction,
                                        Long timestamp,
                                        Path parentDir,
                                        int hourIndex,
                                        String... partitions) {
      return createPartitionFilePath(fileFunction.apply(timestamp), parentDir, hourIndex, partitions);
    }

    protected Path createPartitionFilePath(String fileName,
                                           Path parentDir,
                                           int hourIndex,
                                           String... partitions) {
      Path partitionDir = createPartitionDirectionPath(parentDir, hourIndex, partitions);
      fileName = getPartitionFileName(fileName, hourIndex, partitions);
      return new Path(partitionDir, fileName);
    }

    public Path createPartitionDirectionPath(Path parentDir,
                                             int hourIndex,
                                             String... partitions) {
      Path partitionDir = parentDir;
      for (String dir : partitions) {
        partitionDir = new Path(partitionDir, dir);
      }
      return partitionDir;
    }

    public String getPartitionFileName(String fileName, int hourIndex, String... partitions) {
      return fileName;
    }

    public LinkedHashMap<String, String> getPartSpec(LinkedHashMap<String, String> partSpec, int hourIndex) {
      return partSpec;
    }

    public int getPartitionSize(List<PartitionInfo> partitionInfos) {
      return CollectionUtils.size(partitionInfos);
    }

    public Path getEmptyFilePath(Path locationPath,
                                 String dateStr,
                                 String hourStr,
                                 String filename) {
      return new Path(locationPath, filename);
    }
  }

}
