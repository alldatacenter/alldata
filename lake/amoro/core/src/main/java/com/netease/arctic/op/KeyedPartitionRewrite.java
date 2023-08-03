/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.op;

import com.netease.arctic.table.BaseTable;
import com.netease.arctic.table.KeyedTable;
import com.netease.arctic.table.TableProperties;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.ReplacePartitions;
import org.apache.iceberg.Transaction;
import org.apache.iceberg.relocated.com.google.common.base.Preconditions;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.apache.iceberg.relocated.com.google.common.collect.Maps;
import org.apache.iceberg.util.StructLikeMap;

import java.util.List;
import java.util.Map;

/**
 * Replace {@link BaseTable} partition files and change max transaction id map
 */
public class KeyedPartitionRewrite extends PartitionTransactionOperation implements RewritePartitions {

  protected List<DataFile> addFiles = Lists.newArrayList();
  private Long optimizedSequence;
  
  public KeyedPartitionRewrite(KeyedTable keyedTable) {
    super(keyedTable);
  }

  @Override
  public KeyedPartitionRewrite addDataFile(DataFile dataFile) {
    this.addFiles.add(dataFile);
    return this;
  }

  @Override
  public KeyedPartitionRewrite updateOptimizedSequenceDynamically(long sequence) {
    this.optimizedSequence = sequence;
    return this;
  }

  @Override
  protected StructLikeMap<Map<String, String>> apply(Transaction transaction) {
    PartitionSpec spec = transaction.table().spec();
    StructLikeMap<Map<String, String>> partitionProperties = StructLikeMap.create(spec.partitionType());
    if (this.addFiles.isEmpty()) {
      return partitionProperties;
    }

    Preconditions.checkNotNull(optimizedSequence, "optimized sequence must be set.");
    Preconditions.checkArgument(optimizedSequence > 0, "optimized sequence must > 0.");

    ReplacePartitions replacePartitions = transaction.newReplacePartitions();
    addFiles.forEach(replacePartitions::addFile);
    replacePartitions.commit();

    addFiles.forEach(f -> partitionProperties.computeIfAbsent(f.partition(), k -> Maps.newHashMap())
        .put(TableProperties.PARTITION_OPTIMIZED_SEQUENCE, String.valueOf(optimizedSequence)));
    return partitionProperties;
  }

  @Override
  protected boolean isEmptyCommit() {
    return this.addFiles.isEmpty();
  }
}
