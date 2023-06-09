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
package org.apache.drill.exec.work.foreman.rm;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.proto.UserBitShared.QueryId;
import org.apache.drill.exec.proto.helper.QueryIdHelper;
import org.apache.drill.exec.server.DrillbitContext;

import org.apache.drill.shaded.guava.com.google.common.annotations.VisibleForTesting;

/**
 * Query queue to be used in an embedded Drillbit. This queue has scope of only
 * the one Drillbit (not even multiple Drillbits in the same process.) Primarily
 * intended for testing, but may possibly be useful for other embedded
 * applications.
 * <p>
 * Configuration is via config parameters (not via system options as for the
 * distributed queue.)
 * <dl>
 * <dt><tt>drill.queue.embedded.enabled</tt></dt>
 * <dd>Set to true to enable the embedded queue. But, this setting has effect
 * only if the Drillbit is, in fact, embedded.</dd>
 * <dt><tt>drill.queue.embedded.size</tt></dt>
 * <dd>The number of active queries, all others queue. There is no upper limit
 * on the number of queued entries.</dt>
 * <dt><tt>drill.queue.embedded.timeout_ms</tt></dt>
 * <dd>The maximum time a query will wait in the queue before failing.</dd>
 * </dl>
 */

public class EmbeddedQueryQueue implements QueryQueue {

  public static String EMBEDDED_QUEUE = "drill.exec.queue.embedded";
  public static String ENABLED = EMBEDDED_QUEUE + ".enable";
  public static String QUEUE_SIZE = EMBEDDED_QUEUE + ".size";
  public static String TIMEOUT_MS = EMBEDDED_QUEUE + ".timeout_ms";

  public class EmbeddedQueueLease implements QueueLease {

    private final QueryId queryId;
    private boolean released;
    private long queryMemory;

    public EmbeddedQueueLease(QueryId queryId, long queryMemory) {
      this.queryId = queryId;
      this.queryMemory = queryMemory;
    }

    @Override
    public String toString( ) {
      return new StringBuilder()
          .append("Embedded queue lease for ")
          .append(QueryIdHelper.getQueryId(queryId))
          .append(released ? " (released)" : "")
          .toString();
    }

    @Override
    public long queryMemoryPerNode() {
      return queryMemory;
    }

    @Override
    public void release() {
      EmbeddedQueryQueue.this.release(this);
      released = true;
    }

    @VisibleForTesting
    boolean isReleased() { return released; }

    @Override
    public String queueName() { return "local-queue"; }
  }

  private final int queueTimeoutMs;
  private final int queueSize;
  private final Semaphore semaphore;
  private long memoryPerQuery;
  private final long minimumOperatorMemory;

  public EmbeddedQueryQueue(DrillbitContext context) {
    DrillConfig config = context.getConfig();
    queueTimeoutMs = config.getInt(TIMEOUT_MS);
    queueSize = config.getInt(QUEUE_SIZE);
    semaphore = new Semaphore(queueSize, true);
    minimumOperatorMemory = context.getOptionManager()
        .getOption(ExecConstants.MIN_MEMORY_PER_BUFFERED_OP);
  }

  @Override
  public boolean enabled() { return true; }

  @Override
  public void setMemoryPerNode(long memoryPerNode) {
    memoryPerQuery = memoryPerNode / queueSize;
  }

  @Override
  public long defaultQueryMemoryPerNode(double cost) {
    return memoryPerQuery;
  }

  @Override
  public QueueLease enqueue(QueryId queryId, double cost)
      throws QueueTimeoutException, QueryQueueException {
    try {
      if (! semaphore.tryAcquire(queueTimeoutMs, TimeUnit.MILLISECONDS) ) {
        throw new QueueTimeoutException(queryId, "embedded", queueTimeoutMs);
      }
    } catch (InterruptedException e) {
      throw new QueryQueueException("Interrupted", e);
    }
    return new EmbeddedQueueLease(queryId, memoryPerQuery);
  }

  private void release(EmbeddedQueueLease lease) {
    assert ! lease.released;
    semaphore.release();
  }

  @Override
  public void close() {
    assert semaphore.availablePermits() == queueSize;
  }

  @Override
  public long minimumOperatorMemory() {
    return minimumOperatorMemory;
  }
}
