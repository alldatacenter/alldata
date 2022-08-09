/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.audit;


import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.ambari.server.audit.event.AuditEvent;
import org.apache.ambari.server.configuration.Configuration;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

/**
 * This is a wrapper for an audit log implementation that uses {@link EventBus} to make audit logging asynchronous
 */
@Singleton
class AsyncAuditLogger implements AuditLogger {
  /**
   * Name for guice injection
   */
  final static String InnerLogger = "AsyncAuditLogger";

  /**
   * Event bus that holds audit event objects
   */
  private EventBus eventBus;

  /**
   * Indicates if audit log feature is enabled
   */
  private final boolean isEnabled;

  /**
   * Constructor.
   *
   * @param auditLogger the audit logger to use
   */
  @Inject
  public AsyncAuditLogger(@Named(InnerLogger) AuditLogger auditLogger, Configuration configuration) {
    isEnabled = configuration.isAuditLogEnabled();
    if(isEnabled) {
      eventBus = new AsyncEventBus("AuditLoggerEventBus", new ThreadPoolExecutor(0, 1, 5L, TimeUnit.MINUTES,
        new LinkedBlockingQueue<>(configuration.getAuditLoggerCapacity()), new AuditLogThreadFactory(),
        new ThreadPoolExecutor.CallerRunsPolicy()));
      eventBus.register(auditLogger);
    }
  }

  @Override
  public void log(AuditEvent event) {
    if(isEnabled) {
      eventBus.post(event);
    }
  }

  @Override
  public boolean isEnabled() {
    return isEnabled;
  }

  /**
   * A custom {@link ThreadFactory} for the threads that logs audit events
   */
  private static final class AuditLogThreadFactory implements ThreadFactory {

    private static final AtomicInteger nextId = new AtomicInteger(1);

    /**
     * {@inheritDoc}
     */
    @Override
    public Thread newThread(Runnable runnable) {
      Thread thread = new Thread(runnable, "auditlog-" + nextId.getAndIncrement());
      thread.setDaemon(true);
      return thread;
    }
  }
}
