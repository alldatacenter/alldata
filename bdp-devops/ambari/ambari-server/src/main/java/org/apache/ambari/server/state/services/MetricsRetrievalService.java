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
package org.apache.ambari.server.state.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.ambari.server.AmbariService;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.jmx.JMXMetricHolder;
import org.apache.ambari.server.controller.utilities.ScalingThreadPoolExecutor;
import org.apache.ambari.server.controller.utilities.StreamProvider;
import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.inject.Inject;

/**
 * The {@link MetricsRetrievalService} is used as a headless, autonomous service
 * which encapsulates:
 * <ul>
 * <li>An {@link ExecutorService} for fulfilling remote metric URL requests
 * <li>A cache for JMX metrics
 * <li>A cache for REST metrics
 * </ul>
 *
 * Classes can inject an instance of this service in order to gain access to its
 * caches and request mechanism.
 * <p/>
 * Callers must submit a request to the service in order to reach out and pull
 * in remote metric data. Otherwise, the cache will never be populated. On the
 * first usage of this service, the cache will always be empty. On every
 * subsequent request, the data from the prior invocation of
 * {@link #submitRequest(MetricSourceType, StreamProvider, String)} will be available.
 * <p/>
 * Metric data is cached temporarily and is controlled by
 * {@link Configuration#getMetricsServiceCacheTimeout()}.
 * <p/>
 * In order to control throttling requests to the same endpoint,
 * {@link Configuration#isMetricsServiceRequestTTLCacheEnabled()} can be enabled
 * to allow for a fixed interval of time to pass between requests.
 */
@AmbariService
public class MetricsRetrievalService extends AbstractService {

  /**
   * The type of web service hosting the metrics.
   */
  public enum MetricSourceType {
    /**
     * JMX
     */
    JMX,

    /**
     * REST
     */
    REST
  }

  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(MetricsRetrievalService.class);

  /**
   * The timeout for exceptions which are caught and then cached to prevent log
   * spamming.
   *
   * @see #s_exceptionCache
   */
  private static final int EXCEPTION_CACHE_TIMEOUT_MINUTES = 20;

  /**
   * Exceptions from this service should not SPAM the logs; so cache exceptions
   * and log once every {@vale #EXCEPTION_CACHE_TIMEOUT_MINUTES} minutes.
   */
  private static final Cache<String, Throwable> s_exceptionCache = CacheBuilder.newBuilder().expireAfterWrite(
      EXCEPTION_CACHE_TIMEOUT_MINUTES, TimeUnit.MINUTES).build();

  /**
   * Configuration.
   */
  @Inject
  private Configuration m_configuration;

  /**
   * Used for reading REST JSON responses.
   */
  @Inject
  private Gson m_gson;

  /**
   * A cache of URL to parsed JMX beans
   */
  private Cache<String, JMXMetricHolder> m_jmxCache;

  /**
   * A cache of URL to parsed REST data.
   */
  private Cache<String, Map<String, String>> m_restCache;

  /**
   * The {@link Executor} which will handle all of the requests to load remote
   * metrics from URLs.
   */
  private ThreadPoolExecutor m_threadPoolExecutor;

  /**
   * Used to parse remote JMX JSON into a {@link Map}.
   */
  private final ObjectReader m_jmxObjectReader;

  /**
   * A thread-safe collection of all of the URL endpoints queued for processing.
   * This helps prevent the same endpoint from being queued multiple times.
   */
  private final Set<String> m_queuedUrls = Sets.newConcurrentHashSet();

  /**
   * An evicting cache which ensures that multiple requests for the same
   * endpoint are not executed back-to-back. When enabled, a fixed period of
   * time must pass before this service will make requests to a previously
   * retrieved endpoint.
   * <p/>
   * If this cache is not enabled, then it will be {@code null}.
   * <p/>
   * For simplicity, this is a cache of URL to URL.
   */
  private Cache<String, String> m_ttlUrlCache;


  /**
   * The size of the worker queue (used for logged warnings about size).
   */
  private int m_queueMaximumSize;

  /**
   * Constructor.
   *
   */
  public MetricsRetrievalService() {
    ObjectMapper jmxObjectMapper = new ObjectMapper();
    jmxObjectMapper.configure(DeserializationConfig.Feature.USE_ANNOTATIONS, false);
    jmxObjectMapper.configure(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS, true);
    m_jmxObjectReader = jmxObjectMapper.reader(JMXMetricHolder.class);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void doStart() {
    // initialize the caches
    int jmxCacheExpirationMinutes = m_configuration.getMetricsServiceCacheTimeout();
    m_jmxCache = CacheBuilder.newBuilder().expireAfterWrite(jmxCacheExpirationMinutes,
        TimeUnit.MINUTES).build();

    m_restCache = CacheBuilder.newBuilder().expireAfterWrite(jmxCacheExpirationMinutes,
        TimeUnit.MINUTES).build();

    // enable the TTL cache if configured; otherwise leave it as null
    int ttlSeconds = m_configuration.getMetricsServiceRequestTTL();
    boolean ttlCacheEnabled = m_configuration.isMetricsServiceRequestTTLCacheEnabled();
    if (ttlCacheEnabled) {
      m_ttlUrlCache = CacheBuilder.newBuilder().expireAfterWrite(ttlSeconds,
          TimeUnit.SECONDS).build();
    }

    // iniitalize the executor service
    int corePoolSize = m_configuration.getMetricsServiceThreadPoolCoreSize();
    int maxPoolSize = m_configuration.getMetricsServiceThreadPoolMaxSize();
    m_queueMaximumSize = m_configuration.getMetricsServiceWorkerQueueSize();
    int threadPriority = m_configuration.getMetricsServiceThreadPriority();
    m_threadPoolExecutor = new ScalingThreadPoolExecutor(corePoolSize, maxPoolSize, 30,
        TimeUnit.SECONDS, m_queueMaximumSize);

    m_threadPoolExecutor.allowCoreThreadTimeOut(true);
    m_threadPoolExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());

    ThreadFactory threadFactory = new ThreadFactoryBuilder().setDaemon(true).setNameFormat(
        "ambari-metrics-retrieval-service-thread-%d").setPriority(
            threadPriority).setUncaughtExceptionHandler(
            new MetricRunnableExceptionHandler()).build();

    m_threadPoolExecutor.setThreadFactory(threadFactory);

    LOG.info(
        "Initializing the Metrics Retrieval Service with core={}, max={}, workerQueue={}, threadPriority={}",
        corePoolSize, maxPoolSize, m_queueMaximumSize, threadPriority);

    if (ttlCacheEnabled) {
      LOG.info("Metrics Retrieval Service request TTL cache is enabled and set to {} seconds",
          ttlSeconds);
    }
    notifyStarted();
  }

  /**
   * Testing method for setting a synchronous {@link ThreadPoolExecutor}.
   *
   * @param threadPoolExecutor
   */
  public void setThreadPoolExecutor(ThreadPoolExecutor threadPoolExecutor) {
    m_threadPoolExecutor = threadPoolExecutor;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void doStop() {
    m_jmxCache.invalidateAll();
    m_restCache.invalidateAll();

    if (null != m_ttlUrlCache) {
      m_ttlUrlCache.invalidateAll();
    }

    m_queuedUrls.clear();
    m_threadPoolExecutor.shutdownNow();
    notifyStopped();
  }

  /**
   * Submit a {@link Runnable} for execution which retrieves metric data from
   * the supplied endpoint. This will run inside of an {@link ExecutorService}
   * to retrieve metric data from a URL endpoint and parse the result into a
   * cached value.
   * <p/>
   * Once metric data is retrieved it is cached. Data in the cache can be
   * retrieved via {@link #getCachedJMXMetric(String)} or
   * {@link #getCachedRESTMetric(String)}, depending on the type of metric
   * requested.
   * <p/>
   * Callers need not worry about invoking this multiple times for the same URL
   * endpoint. A single endpoint will only be enqueued once regardless of how
   * many times this method is called until it has been fully retrieved and
   * parsed. If the last endpoint request was too recent, then this method will
   * opt to not make another call until the TTL period expires.
   *
   * @param type
   *          the type of service hosting the metric (not {@code null}).
   * @param streamProvider
   *          the {@link StreamProvider} to use to read from the remote
   *          endpoint.
   * @param url
   *          the URL to read from
   *
   * @see #getCachedJMXMetric(String)
   */
  public void submitRequest(MetricSourceType type, StreamProvider streamProvider, String url) {
    // check to ensure that the request isn't already queued
    if (m_queuedUrls.contains(url)) {
      return;
    }

    // check to ensure that the request wasn't made too recently
    if (null != m_ttlUrlCache && null != m_ttlUrlCache.getIfPresent(url)) {
      return;
    }

    // log warnings if the queue size seems to be rather large
    BlockingQueue<Runnable> queue = m_threadPoolExecutor.getQueue();
    int queueSize = queue.size();
    if (queueSize > Math.floor(0.9f * m_queueMaximumSize)) {
      LOG.warn("The worker queue contains {} work items and is at {}% of capacity", queueSize,
          ((float) queueSize / m_queueMaximumSize) * 100);
    }

    // enqueue this URL
    m_queuedUrls.add(url);

    Runnable runnable = null;
    switch (type) {
      case JMX:
        runnable = new JMXRunnable(m_jmxCache, m_queuedUrls, m_ttlUrlCache, m_jmxObjectReader,
            streamProvider, url);
        break;
      case REST:
        runnable = new RESTRunnable(m_restCache, m_queuedUrls, m_ttlUrlCache, m_gson,
            streamProvider, url);
        break;
      default:
        LOG.warn("Unable to retrieve metrics for the unknown type {}", type);
        break;
    }

    if (null != runnable) {
      m_threadPoolExecutor.execute(runnable);
    }
  }

  /**
   * Gets a cached JMX metric in the form of a {@link JMXMetricHolder}. If there
   * is no metric data cached for the given URL, then {@code null} is returned.
   * <p/>
   * The onky way this cache is populated is by requesting the data to be loaded
   * asynchronously via
   * {@link #submitRequest(MetricSourceType, StreamProvider, String)} with the
   * {@link MetricSourceType#JMX} type.
   *
   * @param jmxUrl
   *          the URL to retrieve cached data for (not {@code null}).
   * @return the metric, or {@code null} if none.
   */
  public JMXMetricHolder getCachedJMXMetric(String jmxUrl) {
    return m_jmxCache.getIfPresent(jmxUrl);
  }

  /**
   * Gets a cached REST metric in the form of a {@link Map}. If there is no
   * metric data cached for the given URL, then {@code null} is returned.
   * <p/>
   * The only way this cache is populated is by requesting the data to be loaded
   * asynchronously via
   * {@link #submitRequest(MetricSourceType, StreamProvider, String)} with the
   * {@link MetricSourceType#REST} type.
   *
   * @param restUrl
   *          the URL to retrieve cached data for (not {@code null}).
   * @return the metric, or {@code null} if none.
   */
  public Map<String, String> getCachedRESTMetric(String restUrl) {
    return m_restCache.getIfPresent(restUrl);
  }

  /**
   * Encapsulates the common logic for all metric {@link Runnable} instances.
   */
  private static abstract class MetricRunnable implements Runnable {

    /**
     * An initialized stream provider to read the remote endpoint.
     */
    protected final StreamProvider m_streamProvider;

    /**
     * A fully-qualified URL to read from.
     */
    protected final String m_url;

    /**
     * The URLs which have been requested but not yet read.
     */
    private final Set<String> m_queuedUrls;

    /**
     * An evicting cache used to control whether a request for a metric can be
     * made or if it is too soon after the last request.
     */
    private final Cache<String, String> m_ttlUrlCache;

    /**
     * Constructor.
     *
     * @param streamProvider
     *          the stream provider to read the URL with
     * @param url
     *          the URL endpoint to read data from (JMX or REST)
     * @param queuedUrls
     *          the URLs which are currently waiting to be processed. This
     *          method will remove the specified URL from this {@link Set} when
     *          it completes (successful or not).
     * @param ttlUrlCache
     *          an evicting cache which is used to determine if a request for a
     *          metric is too soon after the last request, or {@code null} if
     *          requests can be made sequentially without any separation.
     */
    private MetricRunnable(StreamProvider streamProvider, String url, Set<String> queuedUrls,
        Cache<String, String> ttlUrlCache) {
      m_streamProvider = streamProvider;
      m_url = url;
      m_queuedUrls = queuedUrls;
      m_ttlUrlCache = ttlUrlCache;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void run() {

      // provide some profiling
      long startTime = 0;
      long endTime = 0;
      boolean isDebugEnabled = LOG.isDebugEnabled();
      if (isDebugEnabled) {
        startTime = System.currentTimeMillis();
      }

      InputStream inputStream = null;

      try {
        if (isDebugEnabled) {
          endTime = System.currentTimeMillis();
          LOG.debug("Loading metric JSON from {} took {}ms", m_url, (endTime - startTime));
        }

        // read the stream and process it
        inputStream = m_streamProvider.readFrom(m_url);
        processInputStreamAndCacheResult(inputStream);

        // cache the URL, but only after successful parsing of the response
        if (null != m_ttlUrlCache) {
          m_ttlUrlCache.put(m_url, m_url);
        }
      } catch (IOException exception)
      {
        LOG.debug("Removing cached values for url {}", m_url);
        // need to ensure old values are removed because they could be not valid if the state have changed.
        removeCachedMetricsForCurrentURL();
        logException(exception, m_url);
      } catch (Exception exception) {
        logException(exception, m_url);
      } finally {
        IOUtils.closeQuietly(inputStream);

        // remove this URL from the list of queued URLs to ensure it will be
        // requested again
        m_queuedUrls.remove(m_url);
      }
    }

    /**
     * Removes metric values for current URL from cache.
     */
    protected abstract void removeCachedMetricsForCurrentURL();

    /**
     * Reads data from the specified {@link InputStream} and processes that into
     * a cachable value. The value will then be cached by this method.
     *
     * @param inputStream
     * @throws Exception
     */
    protected abstract void processInputStreamAndCacheResult(InputStream inputStream)
        throws Exception;

    /**
     * Logs the exception for the URL exactly once and caches the fact that the
     * exception was logged. This is to prevent an endpoint being down from
     * spamming the logs.
     *
     * @param throwable
     *          the exception to log (not {@code null}).
     * @param url
     *          the URL associated with the exception (not {@code null}).
     */
    final void logException(Throwable throwable, String url) {
      String cacheKey = buildCacheKey(throwable, url);
      if (null == s_exceptionCache.getIfPresent(cacheKey)) {
        // cache it and log it
        s_exceptionCache.put(cacheKey, throwable);
        LOG.error(
            "Unable to retrieve metrics from {}. Subsequent failures will be suppressed from the log for {} minutes.",
            url, EXCEPTION_CACHE_TIMEOUT_MINUTES, throwable);
      }
    }

    /**
     * Builds a unique cache key for the combination of {@link Throwable} and
     * {@link String} URL.
     *
     * @param throwable
     * @param url
     * @return the key, such as <code>IOException-http://www.server.com/jmx</code>.
     */
    private String buildCacheKey(Throwable throwable, String url) {
      if (null == throwable || null == url) {
        return "";
      }

      String throwableName = throwable.getClass().getSimpleName();
      return throwableName + "-" + url;
    }

  }

  /**
   * A {@link Runnable} used to retrieve JMX data from a remote URL endpoint.
   * There is no need for a {@link Callable} here since the
   * {@link MetricsRetrievalService} doesn't care about when the value returns or
   * whether an exception is thrown.
   */
  private static final class JMXRunnable extends MetricRunnable {

    private final ObjectReader m_jmxObjectReader;
    private final Cache<String, JMXMetricHolder> m_cache;

    /**
     * Constructor.
     *
     * @param cache
     * @param queuedUrls
     * @param ttlUrlCache
     * @param jmxObjectReader
     * @param streamProvider
     * @param jmxUrl
     */
    private JMXRunnable(Cache<String, JMXMetricHolder> cache, Set<String> queuedUrls,
        Cache<String, String> ttlUrlCache, ObjectReader jmxObjectReader,
        StreamProvider streamProvider, String jmxUrl) {
      super(streamProvider, jmxUrl, queuedUrls, ttlUrlCache);
      m_cache = cache;
      m_jmxObjectReader = jmxObjectReader;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void removeCachedMetricsForCurrentURL() {
      m_cache.invalidate(m_url);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void processInputStreamAndCacheResult(InputStream inputStream) throws Exception {
      JMXMetricHolder jmxMetricHolder = m_jmxObjectReader.readValue(inputStream);
      m_cache.put(m_url, jmxMetricHolder);
    }
  }

  /**
   * A {@link Runnable} used to retrieve REST data from a remote URL endpoint.
   * There is no need for a {@link Callable} here since the
   * {@link MetricsRetrievalService} doesn't care about when the value returns
   * or whether an exception is thrown.
   */
  private static final class RESTRunnable extends MetricRunnable {

    private final Gson m_gson;
    private final Cache<String, Map<String, String>> m_cache;

    /**
     * Constructor.
     *
     * @param cache
     * @param queuedUrls
     * @param ttlUrlCache
     * @param gson
     * @param streamProvider
     * @param restUrl
     */
    private RESTRunnable(Cache<String, Map<String, String>> cache, Set<String> queuedUrls,
        Cache<String, String> ttlUrlCache, Gson gson, StreamProvider streamProvider,
        String restUrl) {
      super(streamProvider, restUrl, queuedUrls, ttlUrlCache);
      m_cache = cache;
      m_gson = gson;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void removeCachedMetricsForCurrentURL() {
      m_cache.invalidate(m_url);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void processInputStreamAndCacheResult(InputStream inputStream) throws Exception {
      Type type = new TypeToken<Map<Object, Object>>() {}.getType();

      JsonReader jsonReader = new JsonReader(
          new BufferedReader(new InputStreamReader(inputStream)));

      Map<String, String> jsonMap = m_gson.fromJson(jsonReader, type);
      m_cache.put(m_url, jsonMap);
    }
  }

  /**
   * A default exception handler.
   */
  private static final class MetricRunnableExceptionHandler implements UncaughtExceptionHandler {

    /**
     * {@inheritDoc}
     */
    @Override
    public void uncaughtException(Thread t, Throwable e) {
      LOG.error("Asynchronous metric retrieval encountered an exception with thread {}", t, e);
    }
  }
}
