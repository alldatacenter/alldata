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
package org.apache.drill.exec.store.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Closes Kafka resources asynchronously which result does not depend on close method
 * in order to improve query execution performance.
 * For example, {@link org.apache.kafka.clients.consumer.KafkaConsumer}.
 */
public class KafkaAsyncCloser implements AutoCloseable {

  private static final Logger logger = LoggerFactory.getLogger(KafkaAsyncCloser.class);

  private volatile ExecutorService executorService;

  /**
   * Closes given resource in separate thread using thread executor.
   *
   * @param autoCloseable resource to close
   */
  public void close(AutoCloseable autoCloseable) {
    if (autoCloseable != null) {
      ExecutorService executorService = executorService();
      executorService.submit(() -> {
        try {
          autoCloseable.close();
          logger.debug("Closing {} resource", autoCloseable.getClass().getCanonicalName());
        } catch (Exception e) {
          logger.debug("Resource {} failed to close: {}", autoCloseable.getClass().getCanonicalName(), e.getMessage());
        }
      });
    }
  }

  @Override
  public void close() {
    if (executorService != null) {
      logger.trace("Closing Kafka async closer: {}", executorService);
      executorService.shutdownNow();
    }
  }

  /**
   * Initializes executor service instance using DCL.
   * Created thread executor instance allows to execute only one thread at a time
   * but unlike single thread executor does not keep this thread in the pool.
   * Custom thread factory is used to define Kafka specific thread names.
   *
   * @return executor service instance
   */
  private ExecutorService executorService() {
    if (executorService == null) {
      synchronized (this) {
        if (executorService == null) {
          this.executorService = new ThreadPoolExecutor(0, 1, 0L,
            TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), new KafkaThreadFactory());
        }
      }
    }
    return executorService;
  }

  /**
   * Wraps default thread factory and adds Kafka closer prefix to the original thread name.
   * Is used to uniquely identify Kafka closer threads.
   * Example: drill-kafka-closer-pool-1-thread-1
   */
  private static class KafkaThreadFactory implements ThreadFactory {

    private static final String THREAD_PREFIX = "drill-kafka-closer-";
    private final ThreadFactory delegate = Executors.defaultThreadFactory();

    @Override
    public Thread newThread(Runnable runnable) {
      Thread thread = delegate.newThread(runnable);
      thread.setName(THREAD_PREFIX + thread.getName());
      return thread;
    }
  }
}
