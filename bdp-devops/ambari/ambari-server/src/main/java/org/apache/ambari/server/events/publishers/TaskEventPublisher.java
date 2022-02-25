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

package org.apache.ambari.server.events.publishers;

import org.apache.ambari.server.events.TaskEvent;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Singleton;

/**
 * The {@link TaskEventPublisher} is used to publish instances of
 * {@link TaskEvent} to any {@link com.google.common.eventbus.Subscribe} interested.
 * It uses a single-threaded, serial {@link EventBus}.
 */
@Singleton
public class TaskEventPublisher {

  /**
   * A single threaded, synchronous event bus for processing task events.
   */
  private final EventBus m_eventBus = new EventBus("ambari-task-report-event-bus");


  /**
   * Publishes the specified event to all registered listeners that
   * {@link Subscribe} to  {@link TaskEvent} instances.
   *
   * @param event {@link TaskEvent}
   */
  public void publish(TaskEvent event) {
    m_eventBus.post(event);
  }

  /**
   * Register a listener to receive events. The listener should use the
   * {@link Subscribe} annotation.
   *
   * @param object
   *          the listener to receive events.
   */
  public void register(Object object) {
    m_eventBus.register(object);
  }

}
