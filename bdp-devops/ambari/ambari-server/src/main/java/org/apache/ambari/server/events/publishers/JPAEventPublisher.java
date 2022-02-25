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

package org.apache.ambari.server.events.publishers;

import org.apache.ambari.server.events.jpa.JPAEvent;
import org.apache.ambari.server.orm.entities.ClusterEntity;

import com.google.common.eventbus.EventBus;
import com.google.inject.Singleton;

/**
 * The {@link JPAEventPublisher} is used to publish events pertaining to
 * EclipseLink and the persistence layer. This class uses a synchronized bus and
 * will therefore run on the current thread. This is desirable since JPA events
 * are time-senstive and cannot be executed in an asynchronous manner most of
 * the time.
 * <p/>
 * This publisher was created specifically for AMBARI-17970. The headless server
 * thread which monitors the database for things to do (known as the action
 * scheduler) polls every second. However, it takes EclipseLink up to 1000ms to
 * update L1 persistence context cache references in other threads. Therefore,
 * it's possible that the action scheduler can pickup a {@link ClusterEntity}
 * with stale data, including stale configurations. Those stale configurations
 * can then be sent in commands down to agents.
 */
@Singleton
public class JPAEventPublisher {
  /**
   * A single threaded, synchronous event bus for processing JPA events.
   */
  private final EventBus m_eventBus = new EventBus("ambari-jpa-event-bus");


  /**
   * Publishes the specified event to all registered listeners that
   * {@link com.google.common.eventbus.Subscribe} to any of the
   * {@link JPAEventPublisher} instances.
   *
   * @param event
   *          the event
   */
  public void publish(JPAEvent event) {
    m_eventBus.post(event);
  }

  /**
   * Register a listener to receive events. The listener should use the
   * {@link com.google.common.eventbus.Subscribe} annotation.
   *
   * @param object
   *          the listener to receive events.
   */
  public void register(Object object) {
    m_eventBus.register(object);
  }
}
