/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Original Files: apache/hudi (https://github.com/apache/hudi)
 * Copyright: Copyright 2019-2020 The Apache Software Foundation
 * SPDX-License-Identifier: Apache License 2.0
 *
 * This file may have been modified by ByteDance Ltd. and/or its affiliates.
 */

package com.bytedance.bitsail.connector.legacy.hudi.utils.source;

import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.configuration.ConfigOptions;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.core.fs.Path;
import org.apache.flink.runtime.state.CheckpointListener;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.apache.flink.table.connector.ChangelogMode;
import org.apache.flink.table.connector.source.DynamicTableSource;
import org.apache.flink.table.connector.source.ScanTableSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A continuous file source that can trigger checkpoints continuously.
 *
 * <p>It loads the data in the specified file and split the data into number of checkpoints batches.
 * Say, if you want 4 checkpoints and there are 8 records in the file, the emit strategy is:
 *
 * <pre>
 *   | 2 records | 2 records | 2 records | 2 records |
 *   | cp1       | cp2       |cp3        | cp4       |
 * </pre>
 *
 * <p>If all the data are flushed out, it waits for the next checkpoint to finish and tear down the source.
 */
public class ContinuousFileSource implements ScanTableSource {

  public static final ConfigOption<Integer> CHECKPOINTS = ConfigOptions
      .key("checkpoints")
      .intType()
      .defaultValue(2)
      .withDescription("Number of checkpoints to write the data set as, default 2");
  private final Path path;
  private final Configuration conf;

  public ContinuousFileSource(
      Path path,
      Configuration conf) {
    this.path = path;
    this.conf = conf;
  }

  @Override
  public ScanRuntimeProvider getScanRuntimeProvider(ScanContext scanContext) {
    return null;
  }

  @Override
  public ChangelogMode getChangelogMode() {
    return ChangelogMode.insertOnly();
  }

  @Override
  public DynamicTableSource copy() {
    return new ContinuousFileSource(this.path, this.conf);
  }

  @Override
  public String asSummaryString() {
    return "ContinuousFileSource";
  }

  /**
   * Source function that partition the data into given number checkpoints batches.
   */
  public static class BoundedSourceFunction implements SourceFunction<String>, CheckpointListener {
    private final Path path;
    private final int checkpoints;
    private final AtomicInteger currentCP = new AtomicInteger(0);
    private List<String> dataBuffer;
    private volatile boolean isRunning = true;

    public BoundedSourceFunction(Path path, int checkpoints) {
      this.path = path;
      this.checkpoints = checkpoints;
    }

    @SuppressWarnings("checkstyle:MagicNumber")
    @Override
    public void run(SourceContext<String> context) throws Exception {
      if (this.dataBuffer == null) {
        loadDataBuffer();
      }
      int oldCP = this.currentCP.get();
      boolean finish = false;
      while (isRunning) {
        int batchSize = this.dataBuffer.size() / this.checkpoints;
        int start = batchSize * oldCP;
        synchronized (context.getCheckpointLock()) {
          for (int i = start; i < start + batchSize; i++) {
            if (i >= this.dataBuffer.size()) {
              finish = true;
              break;
              // wait for the next checkpoint and exit
            }
            context.collect(this.dataBuffer.get(i));
          }
        }
        oldCP++;
        while (this.currentCP.get() < oldCP) {
          synchronized (context.getCheckpointLock()) {
            context.getCheckpointLock().wait(10);
          }
        }
        if (finish || !isRunning) {
          return;
        }
      }
    }

    @Override
    public void cancel() {
      this.isRunning = false;
    }

    private void loadDataBuffer() {
      try {
        this.dataBuffer = Files.readAllLines(Paths.get(this.path.toUri()));
      } catch (IOException e) {
        throw new RuntimeException("Read file " + this.path + " error", e);
      }
    }

    @Override
    public void notifyCheckpointComplete(long l) {
      this.currentCP.incrementAndGet();
    }
  }
}
