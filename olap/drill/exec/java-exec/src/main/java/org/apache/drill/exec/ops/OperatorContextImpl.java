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
package org.apache.drill.exec.ops;

import java.security.PrivilegedExceptionAction;
import java.util.concurrent.Callable;

import org.apache.drill.exec.exception.OutOfMemoryException;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.hadoop.security.UserGroupInformation;

import org.apache.drill.shaded.guava.com.google.common.util.concurrent.ListenableFuture;
import org.apache.drill.shaded.guava.com.google.common.util.concurrent.ListeningExecutorService;
import org.apache.drill.shaded.guava.com.google.common.util.concurrent.MoreExecutors;

class OperatorContextImpl extends BaseOperatorContext implements AutoCloseable {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(OperatorContextImpl.class);

  private boolean closed = false;
  private final OperatorStats stats;

  /**
   * This lazily initialized executor service is used to submit a {@link Callable task} that needs a proxy user. There
   * is no pool that is created; this pool is a decorator around {@link WorkManager#executor the worker pool} that
   * returns a {@link ListenableFuture future} for every task that is submitted. For the shutdown sequence,
   * see {@link WorkManager#close}.
   */
  private ListeningExecutorService delegatePool;

  public OperatorContextImpl(PhysicalOperator popConfig, FragmentContextImpl context) throws OutOfMemoryException {
    this(popConfig, context, null);
  }

  public OperatorContextImpl(PhysicalOperator popConfig, FragmentContextImpl context, OperatorStats stats)
      throws OutOfMemoryException {
    super(context,
          context.getNewChildAllocator(popConfig.getClass().getSimpleName(),
              popConfig.getOperatorId(), popConfig.getInitialAllocation(), popConfig.getMaxAllocation()),
          popConfig);
    if (stats != null) {
      this.stats = stats;
    } else {
      OpProfileDef def =
          new OpProfileDef(popConfig.getOperatorId(), popConfig.getOperatorType(),
                           OperatorUtilities.getChildCount(popConfig));
      this.stats = context.getStats().newOperatorStats(def, allocator);
    }
  }

  public boolean isClosed() {
    return closed;
  }

  @Override
  public void close() {
    if (closed) {
      logger.debug("Attempted to close Operator context for {}, but context is already closed", popConfig != null ? getName() : null);
      return;
    }
    logger.debug("Closing context for {}", popConfig != null ? getName() : null);
    closed = true;
    super.close();
  }

  @Override
  public OperatorStats getStats() {
    return stats;
  }

  @Override
  public <RESULT> ListenableFuture<RESULT> runCallableAs(final UserGroupInformation proxyUgi,
                                                         final Callable<RESULT> callable) {
    synchronized (this) {
      if (delegatePool == null) {
        delegatePool = MoreExecutors.listeningDecorator(getExecutor());
      }
    }
    return delegatePool.submit(new Callable<RESULT>() {
      @Override
      public RESULT call() throws Exception {
        final Thread currentThread = Thread.currentThread();
        final String originalThreadName = currentThread.getName();
        currentThread.setName(proxyUgi.getUserName() + ":task-delegate-thread");
        final RESULT result;
        try {
          result = proxyUgi.doAs((PrivilegedExceptionAction<RESULT>) () -> callable.call());
        } finally {
          currentThread.setName(originalThreadName);
        }
        return result;
      }
    });
  }
}
