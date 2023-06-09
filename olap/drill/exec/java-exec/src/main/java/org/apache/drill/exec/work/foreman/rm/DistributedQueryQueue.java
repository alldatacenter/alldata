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

import java.util.concurrent.TimeUnit;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.coord.ClusterCoordinator;
import org.apache.drill.exec.coord.DistributedSemaphore;
import org.apache.drill.exec.coord.DistributedSemaphore.DistributedLease;
import org.apache.drill.exec.proto.UserBitShared.QueryId;
import org.apache.drill.exec.proto.helper.QueryIdHelper;
import org.apache.drill.exec.server.DrillbitContext;
import org.apache.drill.exec.server.options.SystemOptionManager;

/**
 * Distributed query queue which uses a Zookeeper distributed semaphore to
 * control queuing across the cluster. The distributed queue is actually two
 * queues: one for "small" queries, another for "large" queries. Query size is
 * determined by the Planner's estimate of query cost.
 * <p>
 * This queue is configured using system options:
 * <dl>
 * <dt><tt>exec.queue.enable</tt>
 * <dt>
 * <dd>Set to true to enable the distributed queue.</dd>
 * <dt><tt>exec.queue.large</tt>
 * <dt>
 * <dd>The maximum number of large queries to admit. Additional
 * queries wait in the queue.</dd>
 * <dt><tt>exec.queue.small</tt>
 * <dt>
 * <dd>The maximum number of small queries to admit. Additional
 * queries wait in the queue.</dd>
 * <dt><tt>exec.queue.threshold</tt>
 * <dt>
 * <dd>The cost threshold. Queries below this size are small, at
 * or above this size are large..</dd>
 * <dt><tt>exec.queue.timeout_millis</tt>
 * <dt>
 * <dd>The maximum time (in milliseconds) a query will wait in the
 * queue before failing.</dd>
 * </dl>
 * <p>
 * The above values are refreshed every five seconds. This aids performance
 * a bit in systems with very high query arrival rates.
 */

public class DistributedQueryQueue implements QueryQueue {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DistributedQueryQueue.class);

  private class DistributedQueueLease implements QueueLease {
    private final QueryId queryId;
    private DistributedLease lease;
    private final String queueName;

    /**
     * Memory allocated to the query. Though all queries in the queue use
     * the same memory allocation rules, those rules can change at any time
     * as the user changes system options. This value captures the value
     * calculated at the time that this lease was granted.
     */
    private final long queryMemory;

    public DistributedQueueLease(QueryId queryId, String queueName,
                    DistributedLease lease, long queryMemory) {
      this.queryId = queryId;
      this.queueName = queueName;
      this.lease = lease;
      this.queryMemory = queryMemory;
    }

    @Override
    public String toString() {
      return String.format("Lease for %s queue to query %s",
          queueName, QueryIdHelper.getQueryId(queryId));
    }

    @Override
    public long queryMemoryPerNode() { return queryMemory; }

    @Override
    public void release() {
      DistributedQueryQueue.this.release(this);
    }

    @Override
    public String queueName() { return queueName; }
  }

  /**
   * Exposes a snapshot of internal state information for use in status
   * reporting, such as in the UI.
   */

  @XmlRootElement
  public static class ZKQueueInfo {
    public final int smallQueueSize;
    public final int largeQueueSize;
    public final double queueThreshold;
    public final long memoryPerNode;
    public final long memoryPerSmallQuery;
    public final long memoryPerLargeQuery;

    public ZKQueueInfo(DistributedQueryQueue queue) {
      smallQueueSize = queue.configSet.smallQueueSize;
      largeQueueSize = queue.configSet.largeQueueSize;
      queueThreshold = queue.configSet.queueThreshold;
      memoryPerNode = queue.memoryPerNode;
      memoryPerSmallQuery = queue.memoryPerSmallQuery;
      memoryPerLargeQuery = queue.memoryPerLargeQuery;
    }
  }

  public interface StatusAdapter {
    boolean inShutDown();
  }

  /**
   * Holds runtime configuration options. Allows polling the options
   * for changes, and easily detecting changes.
   */

  private static class ConfigSet {
    private final long queueThreshold;
    private final int queueTimeout;
    private final int largeQueueSize;
    private final int smallQueueSize;
    private final double largeToSmallRatio;
    private final double reserveMemoryRatio;
    private final long minimumOperatorMemory;

    public ConfigSet(SystemOptionManager optionManager) {
      queueThreshold = optionManager.getOption(ExecConstants.QUEUE_THRESHOLD_SIZE);
      queueTimeout = (int) optionManager.getOption(ExecConstants.QUEUE_TIMEOUT);

      // Option manager supports only long values, but we do not expect
      // more than 2 billion active queries, so queue size is stored as
      // an int.
      // TODO: Once DRILL-5832 is available, add an getInt() method to
      // the option system to get the value as an int and do a range
      // check.

      largeQueueSize = (int) optionManager.getOption(ExecConstants.LARGE_QUEUE_SIZE);
      smallQueueSize = (int) optionManager.getOption(ExecConstants.SMALL_QUEUE_SIZE);
      largeToSmallRatio = optionManager.getOption(ExecConstants.QUEUE_MEMORY_RATIO);
      reserveMemoryRatio = optionManager.getOption(ExecConstants.QUEUE_MEMORY_RESERVE);
      minimumOperatorMemory = optionManager.getOption(ExecConstants.MIN_MEMORY_PER_BUFFERED_OP);
    }

    /**
     * Determine if this config set is the same as another one. Detects
     * whether the configuration has changed between one sync point and
     * another.
     * <p>
     * Note that we cannot use <tt>equals()</tt> here as, according to
     * Drill practice, <tt>equals()</tt> is for use in collections and
     * must be accompanied by a <tt>hashCode()</tt> method. Since this
     * class will never be used in a collection, and does not need a
     * hash function, we cannot use <tt>equals()</tt>.
     *
     * @param otherSet another snapshot taken at another time
     * @return true if this configuration is the same as another
     * (no update between the two snapshots), false if the config has
     * changed between the snapshots
     */

    public boolean isSameAs(ConfigSet otherSet) {
      if (otherSet == null) {
        return false;
      }
      return queueThreshold == otherSet.queueThreshold &&
             queueTimeout == otherSet.queueTimeout &&
             largeQueueSize == otherSet.largeQueueSize &&
             smallQueueSize == otherSet.smallQueueSize &&
             largeToSmallRatio == otherSet.largeToSmallRatio &&
             reserveMemoryRatio == otherSet.reserveMemoryRatio &&
             minimumOperatorMemory == otherSet.minimumOperatorMemory;
    }
  }

  private long memoryPerNode;
  private final SystemOptionManager optionManager;
  private ConfigSet configSet;
  private final ClusterCoordinator clusterCoordinator;
  private long nextRefreshTime;
  private long memoryPerSmallQuery;
  private long memoryPerLargeQuery;
  private final StatusAdapter statusAdapter;

  public DistributedQueryQueue(DrillbitContext context, StatusAdapter adapter) {
    statusAdapter = adapter;
    optionManager = context.getOptionManager();
    clusterCoordinator = context.getClusterCoordinator();
  }

  @Override
  public void setMemoryPerNode(long memoryPerNode) {
    this.memoryPerNode = memoryPerNode;
    refreshConfig();
  }

  @Override
  public long defaultQueryMemoryPerNode(double cost) {
    return (cost < configSet.queueThreshold)
        ? memoryPerSmallQuery
        : memoryPerLargeQuery;
  }

  @Override
  public long minimumOperatorMemory() { return configSet.minimumOperatorMemory; }

  /**
   * This limits the number of "small" and "large" queries that a Drill cluster will run
   * simultaneously, if queuing is enabled. If the query is unable to run, this will block
   * until it can. Beware that this is called under run(), and so will consume a thread
   * while it waits for the required distributed semaphore.
   *
   * @param queryId query identifier
   * @param cost the query plan
   * @throws QueryQueueException if the underlying ZK queuing mechanism fails
   * @throws QueueTimeoutException if the query waits too long in the
   * queue
   */

  @Override
  public QueueLease enqueue(QueryId queryId, double cost) throws QueryQueueException, QueueTimeoutException {
    final String queueName;
    DistributedLease lease = null;
    long queryMemory;
    final DistributedSemaphore distributedSemaphore;
    try {

      // Only the refresh and queue computation is synchronized.

      synchronized(this) {
        refreshConfig();

        // get the appropriate semaphore
        if (cost >= configSet.queueThreshold) {
          distributedSemaphore = clusterCoordinator.getSemaphore("query.large", configSet.largeQueueSize);
          queueName = "large";
          queryMemory = memoryPerLargeQuery;
        } else {
          distributedSemaphore = clusterCoordinator.getSemaphore("query.small", configSet.smallQueueSize);
          queueName = "small";
          queryMemory = memoryPerSmallQuery;
        }
      }
      logger.debug("Query {} with cost {} placed into the {} queue.",
                   QueryIdHelper.getQueryId(queryId), cost, queueName);

      lease = distributedSemaphore.acquire(configSet.queueTimeout, TimeUnit.MILLISECONDS);
    } catch (final Exception e) {
      logger.error("Unable to acquire slot for query " +
                   QueryIdHelper.getQueryId(queryId), e);
      throw new QueryQueueException("Unable to acquire slot for query.", e);
    }

    if (lease == null) {
      int timeoutSecs = (int) Math.round(configSet.queueTimeout/1000.0);
      logger.warn("Queue timeout: {} after {} ms. ({} seconds)", queueName,
        String.format("%,d", configSet.queueTimeout), timeoutSecs);
      throw new QueueTimeoutException(queryId, queueName, configSet.queueTimeout);
    }
    return new DistributedQueueLease(queryId, queueName, lease, queryMemory);
  }

  private synchronized void refreshConfig() {
    long now = System.currentTimeMillis();
    if (now < nextRefreshTime) {
      return;
    }
    nextRefreshTime = now + 5000;

    // Only update numbers, and log changes, if something
    // actually changes.

    ConfigSet newSet = new ConfigSet(optionManager);
    if (newSet.isSameAs(configSet)) {
      return;
    }

    configSet = newSet;

    // Divide up memory between queues using admission rate
    // to give more memory to larger queries and less to
    // smaller queries. We assume that large queries are
    // larger than small queries by a factor of
    // largeToSmallRatio.

    double totalUnits = configSet.largeToSmallRatio * configSet.largeQueueSize + configSet.smallQueueSize;
    double availableMemory = Math.round(memoryPerNode * (1.0 - configSet.reserveMemoryRatio));
    double memoryUnit = availableMemory / totalUnits;
    memoryPerLargeQuery = Math.round(memoryUnit * configSet.largeToSmallRatio);
    memoryPerSmallQuery = Math.round(memoryUnit);

    logger.debug("Memory config: total memory per node = {}, available: {},  large/small memory ratio = {}",
        memoryPerNode, availableMemory, configSet.largeToSmallRatio);
    logger.debug("Reserve memory ratio: {}, bytes: {}",
        configSet.reserveMemoryRatio, memoryPerNode - availableMemory);
    logger.debug("Minimum operator memory: {}", configSet.minimumOperatorMemory);
    logger.debug("Small queue: {} slots, {} bytes per slot",
        configSet.smallQueueSize, memoryPerSmallQuery);
    logger.debug("Large queue: {} slots, {} bytes per slot",
        configSet.largeQueueSize, memoryPerLargeQuery);
    logger.debug("Cost threshold: {}, timeout: {} ms.",
        configSet.queueThreshold, configSet.queueTimeout);
  }

  @Override
  public boolean enabled() { return true; }

  public synchronized ZKQueueInfo getInfo() {
    refreshConfig();
    return new ZKQueueInfo(this);
  }

  private void release(QueueLease lease) {
    DistributedQueueLease theLease = (DistributedQueueLease) lease;
    while (true) {
      try {
        theLease.lease.close();
        theLease.lease = null;
        break;
      } catch (final InterruptedException e) {
        // if we end up here, the loop will try again
      } catch (final Exception e) {
        logger.warn("Failure while releasing lease.", e);
        break;
      }
      if (inShutdown()) {
        logger.warn("In shutdown mode: abandoning attempt to release lease");
      }
    }
  }

  private boolean inShutdown() {
    if (statusAdapter == null) {
      return false;
    }
    return statusAdapter.inShutDown();
  }

  @Override
  public void close() {
    // Nothing to do.
  }
}