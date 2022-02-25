/**
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

package org.apache.ambari.server.events.publishers;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.ambari.server.events.STOMPEvent;

import com.google.common.eventbus.EventBus;

public abstract class BufferedUpdateEventPublisher<T> {

  private static final long TIMEOUT = 1000L;
  private final ConcurrentLinkedQueue<T> buffer = new ConcurrentLinkedQueue<>();

  public abstract STOMPEvent.Type getType();

  private ScheduledExecutorService scheduledExecutorService;

  public BufferedUpdateEventPublisher(STOMPUpdatePublisher stompUpdatePublisher) {
    stompUpdatePublisher.registerPublisher(this);
  }

  public void publish(T event, EventBus m_eventBus) {
    if (scheduledExecutorService == null) {
      scheduledExecutorService =
          Executors.newScheduledThreadPool(1);
      scheduledExecutorService
          .scheduleWithFixedDelay(getScheduledPublisher(m_eventBus), TIMEOUT, TIMEOUT, TimeUnit.MILLISECONDS);
    }
    buffer.add(event);
  }

  protected MergingRunnable getScheduledPublisher(EventBus m_eventBus) {
    return new MergingRunnable(m_eventBus);
  }

  protected List<T> retrieveBuffer() {
    List<T> bufferContent = new ArrayList<>();
    while (!buffer.isEmpty()) {
      bufferContent.add(buffer.poll());
    }
    return bufferContent;
  }

  public abstract void mergeBufferAndPost(List<T> events, EventBus m_eventBus);

  private class MergingRunnable implements Runnable {

    private final EventBus m_eventBus;

    public MergingRunnable(EventBus m_eventBus) {
      this.m_eventBus = m_eventBus;
    }

    @Override
    public final void run() {
      List<T> events = retrieveBuffer();
      if (events.isEmpty()) {
        return;
      }
      mergeBufferAndPost(events, m_eventBus);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    BufferedUpdateEventPublisher<?> that = (BufferedUpdateEventPublisher<?>) o;
    return Objects.equals(getType(), that.getType());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getType());
  }
}
