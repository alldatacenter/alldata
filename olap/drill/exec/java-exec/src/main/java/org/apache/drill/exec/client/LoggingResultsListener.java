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
 */
package org.apache.drill.exec.client;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.drill.common.DrillAutoCloseables;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.client.QuerySubmitter.Format;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.memory.RootAllocatorFactory;
import org.apache.drill.exec.proto.UserBitShared.QueryData;
import org.apache.drill.exec.proto.UserBitShared.QueryId;
import org.apache.drill.exec.proto.UserBitShared.QueryResult.QueryState;
import org.apache.drill.exec.record.RecordBatchLoader;
import org.apache.drill.exec.rpc.ConnectionThrottle;
import org.apache.drill.exec.rpc.user.QueryDataBatch;
import org.apache.drill.exec.rpc.user.UserResultsListener;
import org.apache.drill.exec.util.VectorUtil;

import org.apache.drill.shaded.guava.com.google.common.base.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.DrillBuf;

public class LoggingResultsListener implements UserResultsListener {
  private static Logger logger = LoggerFactory.getLogger(LoggingResultsListener.class);

  private final AtomicInteger count = new AtomicInteger();
  private final Stopwatch w = Stopwatch.createUnstarted();
  private final RecordBatchLoader loader;
  private final Format format;
  private final int columnWidth;
  private final BufferAllocator allocator;

  public LoggingResultsListener(DrillConfig config, Format format, int columnWidth) {
    this.allocator = RootAllocatorFactory.newRoot(config);
    this.loader = new RecordBatchLoader(allocator);
    this.format = format;
    this.columnWidth = columnWidth;
  }

  @Override
  public void submissionFailed(UserException ex) {
    logger.info("Exception (no rows returned). Returned in {} ms.", w.elapsed(TimeUnit.MILLISECONDS), ex);
  }

  @Override
  public void queryCompleted(QueryState state) {
    DrillAutoCloseables.closeNoChecked(allocator);
    logger.info("Total rows returned: {}. Returned in {} ms.", count.get(), w.elapsed(TimeUnit.MILLISECONDS));
  }

  @Override
  public void dataArrived(QueryDataBatch result, ConnectionThrottle throttle) {
    final QueryData header = result.getHeader();
    final DrillBuf data = result.getData();

    try {
      if (data != null) {
        count.addAndGet(header.getRowCount());
        loader.load(header.getDef(), data);

        try {
          switch(format) {
            case TABLE:
              VectorUtil.logVectorAccessibleContent(loader, columnWidth);
              break;
            case TSV:
              VectorUtil.logVectorAccessibleContent(loader, "\t");
              break;
            case CSV:
              VectorUtil.logVectorAccessibleContent(loader, ",");
              break;
            default:
              throw new IllegalStateException(format.toString());
          }
        } finally {
          loader.clear();
        }
      }
    }
    finally {
      result.release();
    }
  }

  @Override
  public void queryIdArrived(QueryId queryId) {
    w.start();
  }
}
