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
package org.apache.ambari.server.view;

import java.io.IOException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ambari.server.configuration.Configuration;
import org.eclipse.jetty.continuation.Continuation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * The {@link ViewThrottleFilter} is used to ensure that views which misbehave
 * do not cause a loss of service for Ambari. The underlying problem is that
 * views are accessed off of the REST endpoint (/api/v1/views). This means that
 * the Ambari REST API connector is going to handle the request from its own
 * threadpool. There is no way to configure Jetty to use a different threadpool
 * for the same connector. As a result, if a request to load a view holds the
 * Jetty thread hostage, eventually we will see thread starvation and loss of
 * service.
 * <p/>
 * An example of this situation is a view which makes an innocent request to a
 * remote resource. If the view's request has a timeout of 60 seconds, then the
 * Jetty thread is going to be held for that amount of time. With concurrent
 * users and multiple instances of that view deployed, the Jetty threadpool can
 * becomes exhausted quickly.
 * <p/>
 * Although there are more graceful ways of handling this situation, they mostly
 * involve substantial re-architecture and design.
 * <ul>
 * <li>The use of a new connector and threadpool would require binding to
 * another port for view requests. This will cause problems with "local" views
 * and their assumption that if they run on the Ambari server they can share the
 * same session.
 * <li>The use of a {@link Continuation} in Jetty which can suspend the incoming
 * request. We would need the ability for views to signal that they have
 * completed their work in order to proceed with the suspended request.
 * </ul>
 */
@Singleton
public class ViewThrottleFilter implements Filter {

  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(ViewThrottleFilter.class);

  /**
   * Used to determine the correct number of threads to allocate to view
   * requests.
   */
  @Inject
  private Configuration m_configuration;

  /**
   * Used to restrict how many REST API threads can be utilizied concurrently by
   * view requests.
   */
  private Semaphore m_semaphore;

  /**
   * A timeout that a blocked view request should wait for an available thread
   * before returning an error.
   */
  private int m_timeout;

  /**
   * {@inheritDoc}
   */
  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    m_timeout = m_configuration.getViewRequestThreadPoolTimeout();

    int clientThreadPoolSize = m_configuration.getClientThreadPoolSize();
    int viewThreadPoolSize = m_configuration.getViewRequestThreadPoolMaxSize();

    // start out using 1/2 of the available REST API request threads
    int viewSemaphoreCount = clientThreadPoolSize / 2;

    // if the size is specified, see if it's valid
    if (viewThreadPoolSize > 0) {
      viewSemaphoreCount = viewThreadPoolSize;

      if (viewThreadPoolSize > clientThreadPoolSize) {
        LOG.warn(
            "The number of view processing threads ({}) cannot be greater than the REST API client threads {{})",
            viewThreadPoolSize, clientThreadPoolSize);

        viewSemaphoreCount = clientThreadPoolSize;
      }
    }

    // log that we are restricting it
    LOG.info("Ambari Views will be able to utilize {} concurrent REST API threads",
        viewSemaphoreCount);

    m_semaphore = new Semaphore(viewSemaphoreCount);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    // do nothing if this is not an http request
    if (!(request instanceof HttpServletRequest)) {
      chain.doFilter(request, response);
      return;
    }

    HttpServletResponse httpResponse = (HttpServletResponse) response;
    boolean acquired = false;

    try {
      acquired = m_semaphore.tryAcquire(m_timeout, TimeUnit.MILLISECONDS);
    } catch (InterruptedException interruptedException) {
      LOG.warn("While waiting for an available thread, the view request was interrupted");
    }

    if (!acquired) {
      httpResponse.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
          "There are no available threads to handle view requests");

      // return to prevent the view's request from making it down any farther
      return;
    }

    // let the request go through
    try {
      chain.doFilter(request, response);
    } finally {
      m_semaphore.release();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void destroy() {
  }
}
