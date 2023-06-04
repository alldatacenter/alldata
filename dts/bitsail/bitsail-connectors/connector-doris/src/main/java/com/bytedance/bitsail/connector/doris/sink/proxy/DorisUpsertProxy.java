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

package com.bytedance.bitsail.connector.doris.sink.proxy;

import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.connector.doris.config.DorisExecutionOptions;
import com.bytedance.bitsail.connector.doris.config.DorisOptions;
import com.bytedance.bitsail.connector.doris.sink.streamload.DorisStreamLoad;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class DorisUpsertProxy extends AbstractDorisWriteModeProxy {
  private static final Logger LOG = LoggerFactory.getLogger(DorisUpsertProxy.class);

  private static final long SCHEDULE_DELAY = 5000;

  private List dorisBatchBuffers;
  private static long lastFlushTimestamp;
  private static long flushIntervalMs;
  private long dorisBatchBuffersSize;

  public DorisUpsertProxy(DorisExecutionOptions dorisExecutionOptions, DorisOptions dorisOptions) {
    this.dorisExecutionOptions = dorisExecutionOptions;
    this.dorisBatchBuffers = new ArrayList(dorisExecutionOptions.getBufferCount());
    this.dorisOptions = dorisOptions;
    this.dorisStreamLoad = new DorisStreamLoad(dorisExecutionOptions, dorisOptions);
    this.lastFlushTimestamp = System.currentTimeMillis();
    this.flushIntervalMs = this.dorisExecutionOptions.getFlushIntervalMs();
    this.dorisBatchBuffersSize = 0;
    if (!this.dorisExecutionOptions.isBatch()) {
      //flushIntervalMs only works in streaming mode
      new Timer().schedule(new TimerTask() {
        @Override
        public void run() {
          boolean flushIntervalReached = System.currentTimeMillis() - lastFlushTimestamp >= flushIntervalMs;
          if (flushIntervalReached) {
            LOG.info("Buffers is not fulfilled in {} ms", flushIntervalMs);
            try {
              addBatchBuffersOrFlush(null, true);
            } catch (IOException e) {
              e.printStackTrace();
            }
          }
        }
      }, SCHEDULE_DELAY, flushIntervalMs);
    }
  }

  @Override
  public void write(String record) throws IOException {
    addBatchBuffersOrFlush(record, false);
  }

  /**
   * @param record: the record to buffers
   * @param flushStraight: only try to flush buffers to remote doris
   **/
  private synchronized void addBatchBuffersOrFlush(String record, boolean flushStraight) throws IOException {
    if (flushStraight) {
      flush(false);
      return;
    }

    this.dorisBatchBuffers.add(record);
    this.dorisBatchBuffersSize += record.getBytes().length;
    if (dorisBatchBuffers.size() >= dorisExecutionOptions.getBufferCount()
        || this.dorisBatchBuffersSize >= dorisExecutionOptions.getBufferSize()) {
      LOG.info("Current buffer count:{}, buffer size is {}", dorisBatchBuffers.size(), this.dorisBatchBuffersSize);
      flush(false);
    }
  }

  @SuppressWarnings("checkstyle:MagicNumber")
  @Override
  public void flush(boolean endOfInput) throws IOException {
    this.lastFlushTimestamp = System.currentTimeMillis();
    if (dorisBatchBuffers.isEmpty()) {
      return;
    }
    LOG.info("BatchBuffers is being flushed!");
    String result;
    if (DorisOptions.LOAD_CONTENT_TYPE.JSON.equals(dorisOptions.getLoadDataFormat())) {
      result = dorisBatchBuffers.toString();
    } else {
      result = String.join(dorisOptions.getLineDelimiter(), dorisBatchBuffers);
    }
    for (int i = 0; i <= dorisExecutionOptions.getMaxRetries(); i++) {
      try {
        dorisStreamLoad.load(result, dorisOptions, false);
        dorisBatchBuffers.clear();
        this.dorisBatchBuffersSize = 0;
        break;
      } catch (BitSailException e) {
        LOG.error("doris sink error, retry times = {}", i, e);
        if (i >= dorisExecutionOptions.getMaxRetries()) {
          throw new IOException(e.getMessage());
        }
        try {
          LOG.warn("StreamLoad error", e);
          Thread.sleep(1000 * i);
        } catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
          throw new IOException("unable to flush; interrupted while doing another attempt", e);
        }
      }
      LOG.info("BatchBuffers is flushed successfully!");
    }
  }

  @Override
  public List prepareCommit() {
    return Collections.emptyList();
  }

  @Override
  public List snapshotState(long checkpointId) {
    return Collections.emptyList();
  }
}

