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

import org.apache.ambari.server.events.ClusterEvent;

import com.google.common.eventbus.EventBus;
import com.google.inject.Singleton;

/**
 * The {@link VersionEventPublisher} is used to publish instances of
 * {@link ClusterEvent} to any {@link com.google.common.eventbus.Subscribe} interested.
 * It uses a single-threaded, serial {@link EventBus}.
 */
@Singleton
public class VersionEventPublisher {
  /**
   * A single threaded event bus for processing version events serially.
   */
  private final EventBus m_eventBus;

  /**
   * Constructor.
   */
  public VersionEventPublisher() {
    m_eventBus = new EventBus("version-event-bus");
  }

  /**
   * Publishes the specified event to all registered listeners that
   * {@link com.google.common.eventbus.Subscribe} to any of the
   * {@link ClusterEvent} instances.
   *
   * @param event the event
   */
  public void publish(ClusterEvent event) {
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
