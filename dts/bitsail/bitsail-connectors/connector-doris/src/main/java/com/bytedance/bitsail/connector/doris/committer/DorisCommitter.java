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

package com.bytedance.bitsail.connector.doris.committer;

import com.bytedance.bitsail.base.connector.writer.v1.WriterCommitter;
import com.bytedance.bitsail.connector.doris.DorisConnectionHolder;
import com.bytedance.bitsail.connector.doris.config.DorisExecutionOptions;
import com.bytedance.bitsail.connector.doris.config.DorisOptions;
import com.bytedance.bitsail.connector.doris.partition.DorisPartition;
import com.bytedance.bitsail.connector.doris.partition.DorisPartitionManager;
import com.bytedance.bitsail.connector.doris.sink.ddl.DorisSchemaManagerGenerator;

import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@NoArgsConstructor
public class DorisCommitter implements WriterCommitter<DorisCommittable> {
  private static final Logger LOG = LoggerFactory.getLogger(DorisCommitter.class);
  protected DorisPartitionManager dorisPartitionManager;
  protected DorisConnectionHolder dorisConnectionHolder;
  protected DorisOptions dorisOptions;
  protected DorisExecutionOptions.WRITE_MODE writeMode;

  public DorisCommitter(DorisOptions dorisOptions, DorisExecutionOptions.WRITE_MODE writeMode) {
    this.dorisOptions = dorisOptions;
    this.writeMode = writeMode;
  }

  @Override
  public List<DorisCommittable> commit(List<DorisCommittable> committables) throws IOException {
    //TODO support 2PC commit
    if (!this.writeMode.equals(DorisExecutionOptions.WRITE_MODE.BATCH_REPLACE)) {
      return Collections.emptyList();
    }

    LOG.info("Try to commit temporary partition or table, num of committing events: {}", committables.size());
    try {
      dorisConnectionHolder = DorisSchemaManagerGenerator.getDorisConnection(new DorisConnectionHolder(), dorisOptions);
      dorisPartitionManager = DorisSchemaManagerGenerator.openDorisPartitionManager(dorisConnectionHolder, dorisOptions);
      if (!dorisOptions.isTableHasPartitions()) {
        //start to move temp table to normal table
        dorisPartitionManager.replacePartitionWithoutMutable();
        return Collections.emptyList();
      }
      List<DorisPartition> partitions = dorisOptions.getPartitions();

      if (!partitions.isEmpty()) {
        for (DorisPartition partition : partitions) {
          LOG.info("Start to commit temp partition {} to normal partition {}, start range: {}, end range: {}",
              partition.getTempName(),
              partition.getName(),
              partition.getStartRange(),
              partition.getEndRange()
          );
        }
      }
      //start to move temp partition to normal partition
      dorisPartitionManager.replacePartitionWithoutMutable();
      LOG.info("Succeed to commit temp partition");
      dorisPartitionManager.cleanTemporaryPartition();
      LOG.info("Cleaned temp partition");
      dorisConnectionHolder.closeDorisConnection();
    } catch (SQLException e) {
      throw new IOException("Failed to commit doris partition, Error: " + e.getMessage());
    } finally {
      if (Objects.nonNull(dorisConnectionHolder)) {
        dorisConnectionHolder.closeDorisConnection();
      }
    }
    return Collections.emptyList();
  }
}

