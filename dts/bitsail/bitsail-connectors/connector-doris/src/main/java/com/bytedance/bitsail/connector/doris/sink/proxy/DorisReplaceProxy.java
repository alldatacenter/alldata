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
import com.bytedance.bitsail.connector.doris.committer.DorisCommittable;
import com.bytedance.bitsail.connector.doris.config.DorisExecutionOptions;
import com.bytedance.bitsail.connector.doris.config.DorisOptions;
import com.bytedance.bitsail.connector.doris.sink.DorisWriterState;
import com.bytedance.bitsail.connector.doris.sink.streamload.DorisStreamLoad;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DorisReplaceProxy extends AbstractDorisWriteModeProxy {
  private static final Logger LOG = LoggerFactory.getLogger(DorisReplaceProxy.class);
  protected List dorisBatchBuffers;
  protected long dorisBatchBuffersSize;

  public DorisReplaceProxy(DorisExecutionOptions dorisExecutionOptions, DorisOptions dorisOptions) {
    this.dorisExecutionOptions = dorisExecutionOptions;
    this.dorisBatchBuffers = new ArrayList(dorisExecutionOptions.getBufferCount());
    this.dorisOptions = dorisOptions;
    this.dorisStreamLoad = new DorisStreamLoad(dorisExecutionOptions, dorisOptions);
    this.dorisBatchBuffersSize = 0;
  }

  @VisibleForTesting
  public DorisReplaceProxy() {}

  @Override
  public void write(String record) throws IOException {
    addBatchBuffers(record);
  }

  private void addBatchBuffers(String record) throws IOException {
    this.dorisBatchBuffers.add(record);
    this.dorisBatchBuffersSize += record.getBytes().length;
    if (dorisBatchBuffers.size() >= dorisExecutionOptions.getBufferCount()
        || this.dorisBatchBuffersSize >= dorisExecutionOptions.getBufferSize()) {
      flush(false);
    }
  }

  @SuppressWarnings("checkstyle:MagicNumber")
  @Override
  public void flush(boolean endOfInput) throws IOException {
    if (dorisBatchBuffers.isEmpty()) {
      return;
    }
    String result;
    if (DorisOptions.LOAD_CONTENT_TYPE.JSON.equals(dorisOptions.getLoadDataFormat())) {
      result = dorisBatchBuffers.toString();
    } else {
      result = String.join(dorisOptions.getLineDelimiter(), dorisBatchBuffers);
    }
    for (int i = 0; i <= dorisExecutionOptions.getMaxRetries(); i++) {
      try {
        dorisStreamLoad.load(result, dorisOptions, true);
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
          Thread.sleep(1000L * i);
        } catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
          throw new IOException("unable to flush; interrupted while doing another attempt", e);
        }
      }
    }
  }

  @Override
  public List<DorisCommittable> prepareCommit() throws IOException {
    return Collections.emptyList();
  }

  @Override
  public List<DorisWriterState> snapshotState(long checkpointId) {
    return null;
  }
}
