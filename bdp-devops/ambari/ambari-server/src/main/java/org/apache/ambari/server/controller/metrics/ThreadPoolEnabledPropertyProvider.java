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

package org.apache.ambari.server.controller.metrics;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.internal.AbstractPropertyProvider;
import org.apache.ambari.server.controller.internal.PropertyInfo;
import org.apache.ambari.server.controller.jmx.JMXPropertyProvider;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.PropertyProvider;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.utilities.BufferedThreadPoolExecutorCompletionService;
import org.apache.ambari.server.controller.utilities.ScalingThreadPoolExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;

/**
 * Unites common functionality for multithreaded metrics providers (JMX and REST
 * as of now). Shares the same pool of executor threads across all
 * implementations.
 * <p/>
 * <b>This {@link PropertyProvider} should not be mistaken for a way to perform
 * expensive operations, as it is still called as part of the incoming REST
 * Jetty request.</b> It is poor design to have UI threads from the web client
 * waiting on expensive operations from a {@link PropertyProvider}, even if they
 * are spread across multiple threads.
 * <p/>
 * Instead, this {@link PropertyProvider} is useful for spreading many small,
 * quick operations across a threadpool. This is why the known implementations
 * of this class (such as the {@link JMXPropertyProvider}) use a cache instead
 * of reaching out to network endpoints on their own.
 * <p/>
 * This is also why the {@link ThreadPoolExecutor} used here has an unbounded
 * worker queue and essentially a fixed core size to perform its work. When
 * {@link Callable}s are rejected because of a worker queue exhaustion, they are
 * never submitted for execution, yet the {@link Future} instance is still
 * returned. Therefore, if the queue is ever exhausted, incoming REST API
 * requests must wait the entire {@link CompletionService#poll(long, TimeUnit)}
 * timeout before skipping the result and returning control.
 *
 */
public abstract class ThreadPoolEnabledPropertyProvider extends AbstractPropertyProvider {

  private static final Logger LOG = LoggerFactory.getLogger(ThreadPoolEnabledPropertyProvider.class);

  /**
   * Host states that make available metrics collection
   */
  public static final Set<String> healthyStates = Collections.singleton("STARTED");
  protected final String hostNamePropertyId;
  private final MetricHostProvider metricHostProvider;
  private final String clusterNamePropertyId;

  /**
   * Executor service is shared between all instances.
   */
  private static ThreadPoolExecutor EXECUTOR_SERVICE;
  private static int THREAD_POOL_CORE_SIZE;
  private static int THREAD_POOL_MAX_SIZE;
  private static int THREAD_POOL_WORKER_QUEUE_SIZE;
  private static long COMPLETION_SERVICE_POLL_TIMEOUT;
  private static final long THREAD_POOL_TIMEOUT_MILLIS = 30000L;

  @Inject
  public static void init(Configuration configuration) {
    THREAD_POOL_CORE_SIZE = configuration.getPropertyProvidersThreadPoolCoreSize();
    THREAD_POOL_MAX_SIZE = configuration.getPropertyProvidersThreadPoolMaxSize();
    THREAD_POOL_WORKER_QUEUE_SIZE = configuration.getPropertyProvidersWorkerQueueSize();
    COMPLETION_SERVICE_POLL_TIMEOUT = configuration.getPropertyProvidersCompletionServiceTimeout();
    EXECUTOR_SERVICE = initExecutorService();
  }

  private static final Cache<String, Throwable> exceptionsCache = CacheBuilder.newBuilder()
          .expireAfterWrite(5, TimeUnit.MINUTES)
          .build();

  // ----- Constructors ------------------------------------------------------

  /**
   * Construct a provider.
   *
   * @param componentMetrics map of metrics for this provider
   */
  public ThreadPoolEnabledPropertyProvider(Map<String, Map<String, PropertyInfo>> componentMetrics,
                                           String hostNamePropertyId,
                                           MetricHostProvider metricHostProvider,
                                           String clusterNamePropertyId) {
    super(componentMetrics);
    this.hostNamePropertyId = hostNamePropertyId;
    this.metricHostProvider = metricHostProvider;
    this.clusterNamePropertyId = clusterNamePropertyId;
  }

  // ----- Thread pool -------------------------------------------------------

  /**
   * Generates thread pool with default parameters
   */
  private static ThreadPoolExecutor initExecutorService() {
    ThreadPoolExecutor threadPoolExecutor =
        new ScalingThreadPoolExecutor(
            THREAD_POOL_CORE_SIZE,
            THREAD_POOL_MAX_SIZE,
            THREAD_POOL_TIMEOUT_MILLIS,
            TimeUnit.MILLISECONDS,
            THREAD_POOL_WORKER_QUEUE_SIZE);
    threadPoolExecutor.allowCoreThreadTimeOut(true);

    ThreadFactory threadFactory = new ThreadFactoryBuilder().setDaemon(true).setNameFormat(
        "ambari-property-provider-thread-%d").build();

    threadPoolExecutor.setThreadFactory(threadFactory);

    return threadPoolExecutor;
  }

  // ----- Common PropertyProvider implementation details --------------------

  @Override
  public Set<Resource> populateResources(Set<Resource> resources, Request request, Predicate predicate)
      throws SystemException {

    if(!checkAuthorizationForMetrics(resources, clusterNamePropertyId)) {
      return resources;
    }

    // Get a valid ticket for the request.
    final Ticket ticket = new Ticket();

    // in most cases, the buffered completion service will not be utlized for
    // its advantages since the worker queue is unbounded. However, if is
    // configured with a boundary, then the buffered service ensures that no
    // requests are discarded.
    final CompletionService<Resource> completionService =
      new BufferedThreadPoolExecutorCompletionService<>(EXECUTOR_SERVICE);

    // In a large cluster we could have thousands of resources to populate here.
    // Distribute the work across multiple threads.
    for (Resource resource : resources) {
      completionService.submit(
          getPopulateResourceCallable(resource, request, predicate, ticket));
    }

    Set<Resource> keepers = new HashSet<>();
    try {
      for (int i = 0; i < resources.size(); ++i) {
        Future<Resource> resourceFuture = completionService.poll(COMPLETION_SERVICE_POLL_TIMEOUT,
            TimeUnit.MILLISECONDS);

        if (resourceFuture == null) {
          // its been more than the populateTimeout since the last callable
          // completed ...
          // invalidate the ticket to abort the threads and don't wait any
          // longer
          ticket.invalidate();
          LOG.error("Timed out after waiting {}ms waiting for request {}",
              COMPLETION_SERVICE_POLL_TIMEOUT, request);

          // stop iterating
          break;
        }

        // future should already be completed... no need to wait on get
        Resource resource = resourceFuture.get();
        if (resource != null) {
          keepers.add(resource);
        }
      }
    } catch (InterruptedException e) {
      logException(e);
    } catch (ExecutionException e) {
      rethrowSystemException(e.getCause());
    }

    return keepers;
  }

  /**
   * Get a callable that can be used to populate the given resource.
   *
   * @param resource  the resource to be populated
   * @param request   the request
   * @param predicate the predicate
   * @param ticket    a valid ticket
   *
   * @return a callable that can be used to populate the given resource
   */
  private Callable<Resource> getPopulateResourceCallable(
      final Resource resource, final Request request, final Predicate predicate, final Ticket ticket) {
    return new Callable<Resource>() {
      @Override
      public Resource call() throws SystemException {
        return populateResource(resource, request, predicate, ticket);
      }
    };
  }


  /**
   * Populate a resource by obtaining the requested JMX properties.
   *
   * @param resource  the resource to be populated
   * @param request   the request
   * @param predicate the predicate
   * @return the populated resource; null if the resource should NOT be part of the result set for the given predicate
   */


  protected abstract Resource populateResource(Resource resource,
                                               Request request, Predicate predicate, Ticket ticket)

      throws SystemException;

  /**
   * Set the populate timeout value for this provider.
   *
   * @param populateTimeout the populate timeout value
   */


  protected void setPopulateTimeout(long populateTimeout) {
    COMPLETION_SERVICE_POLL_TIMEOUT = populateTimeout;
  }


  // ----- helper methods ----------------------------------------------------

  /**
   * Determine whether or not the given property id was requested.
   */
  protected static boolean isRequestedPropertyId(String propertyId, String requestedPropertyId, Request request) {
    return request.getPropertyIds().isEmpty() || propertyId.startsWith(requestedPropertyId);
  }

  protected static String getCacheKeyForException(final Throwable throwable) {
    if (throwable == null) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    for (Throwable t : Throwables.getCausalChain(throwable)) {
      if (t != null) {
        sb.append(t.getClass().getName());
      }
      sb.append('\n');
    }
    return sb.toString();
  }

  /**
   * Log an error for the given exception.
   *
   * @param throwable  the caught exception
   *
   * @return the error message that was logged
   */
  protected static String logException(final Throwable throwable) {
    final String msg = "Caught exception getting metrics : " + throwable.getLocalizedMessage();

    // JsonParseException includes InputStream's hash code into the message.
    // getMessage and printStackTrace returns a different String every time.
    String cacheKey = getCacheKeyForException(throwable);

    if (LOG.isDebugEnabled()) {
      LOG.debug(msg, throwable);
    } else {
      try {
        exceptionsCache.get(cacheKey, new Callable<Throwable>() {
          @Override
          public Throwable call() {
            LOG.error(msg + ", skipping same exceptions for next 5 minutes", throwable);
            return throwable;
          }
        });
      } catch (ExecutionException ignored) {
      }
    }


    return msg;
  }

  /**
   * Rethrow the given exception as a System exception and log the message.
   *
   * @param throwable  the caught exception
   *
   * @throws org.apache.ambari.server.controller.spi.SystemException always around the given exception
   */
  protected static void rethrowSystemException(Throwable throwable) throws SystemException {
    String msg = logException(throwable);

    if (throwable instanceof SystemException) {
      throw (SystemException) throwable;
    }
    throw new SystemException (msg, throwable);
  }

  /**
   * Returns a hostname for component
   */


  public String getHost(Resource resource, String clusterName, String componentName) throws SystemException {
    return hostNamePropertyId == null ?
        metricHostProvider.getHostName(clusterName, componentName) :
        (String) resource.getPropertyValue(hostNamePropertyId);

  }


  /**
   * Get complete URL from parts
   */

  protected String getSpec(String protocol, String hostName,
                           String port, String url) {
    return protocol + "://" + hostName + ":" + port + url;

  }

  // ----- inner class : Ticket ----------------------------------------------

  /**
   * Ticket used to cancel provider threads.  The provider threads should
   * monitor the validity of the passed in ticket and bail out if it becomes
   * invalid (as in a timeout).
   */
  protected static class Ticket {
    /**
     * Indicate whether or not the ticket is valid.
     */
    private volatile boolean valid = true;

    /**
     * Invalidate the ticket.
     */
    public void invalidate() {
      valid = false;
    }

    /**
     * Determine whether or not this ticket is valid.
     *
     * @return true if the ticket is valid
     */
    public boolean isValid() {
      return valid;
    }
  }

}
