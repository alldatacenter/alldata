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

package com.bytedance.bitsail.connector.legacy.streamingfile.core.sink.format.hdfs;

import com.bytedance.bitsail.connector.legacy.streamingfile.common.datasink.common.PartitionInfo;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.datasink.common.PartitionMapping;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.datasink.common.PartitionType;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.TableMetaStoreFactory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import org.apache.commons.collections.MapUtils;
import org.apache.flink.core.fs.Path;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Hdfs {@link HdfsMetaStoreFactory}
 */
@Builder
@AllArgsConstructor
public class HdfsMetaStoreFactory implements TableMetaStoreFactory {
  private static final long serialVersionUID = 1L;

  private final String locationPath;

  private final Map<String, PartitionMapping> partSpecMapping;

  private final Map<String, PartitionInfo> partitionInfoMap;

  @Override
  public HdfsTableMetaStore createTableMetaStore() {
    return new HdfsTableMetaStore();
  }

  private String getPartSpecLocation(String partSpec) {
    if (MapUtils.isNotEmpty(partSpecMapping)
        && partSpecMapping.containsKey(partSpec)) {
      return partSpecMapping.get(partSpec).getLocation();
    }
    return locationPath;
  }

  public class HdfsTableMetaStore implements TableMetaStore {
    @Override
    public Path getLocationPath() {
      return new Path(locationPath);
    }

    /**
     * In split situation, if we find split part spec in mapping case, we will use partition mapping
     * instead of the location path, eg:
     * partition spec: (1,file://tmp/hdfs1)
     * location path: /hdfs2
     * dump temporary structure:
     * /split-part=0/part1=aa/part2=bb/1.txt
     * /split-part=1/part1=aa/part2=bb/2.txt
     * /split-part=2/part1=aa/part2=bb/3.txt
     * <p>
     * final document structure:
     * file://hdfs/part1=aa/part2=bb/1.txt
     * file://hdfs/part1=aa/part2=bb/3.txt
     * <p>
     * file://tmp/hdfs1/part1=aa/part2=bb/2.txt
     *
     * @param partitionSpec partition spec should be a full spec, must be in the same order as
     *                      the partition keys of the table.
     */
    @Override
    public Optional<Path> getPartition(LinkedHashMap<String, String> partitionSpec) {
      return Optional.of(getPartitionPath(partitionSpec));
    }

    @Override
    public Path getPartitionPath(LinkedHashMap<String, String> partitionSpec) {
      StringBuilder suffixBuf = new StringBuilder();
      int i = 0;
      String parentLocation = locationPath;
      for (Map.Entry<String, String> e : partitionSpec.entrySet()) {
        PartitionInfo partitionInfo = partitionInfoMap.get(e.getKey());
        if (PartitionType.SPLIT.equals(partitionInfo.getType())) {
          parentLocation = getPartSpecLocation(e.getValue());
          continue;
        }
        if (i > 0) {
          suffixBuf.append(Path.SEPARATOR);
        }
        suffixBuf.append(e.getValue());
        i++;
      }
      suffixBuf.append(Path.SEPARATOR);
      return new Path(parentLocation, suffixBuf.toString());
    }

    @Override
    public void createPartition(LinkedHashMap<String, String> partSpec, Path path) {
      throw new RuntimeException("Do not support partition in hdfs metastore");
    }

    @Override
    public void close() throws IOException {
    }
  }
}
