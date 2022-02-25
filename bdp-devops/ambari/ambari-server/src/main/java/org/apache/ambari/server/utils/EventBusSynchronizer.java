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
package org.apache.ambari.server.utils;

import java.lang.reflect.Field;

import org.apache.ambari.server.events.listeners.alerts.AlertAggregateListener;
import org.apache.ambari.server.events.listeners.alerts.AlertLifecycleListener;
import org.apache.ambari.server.events.listeners.alerts.AlertMaintenanceModeListener;
import org.apache.ambari.server.events.listeners.alerts.AlertReceivedListener;
import org.apache.ambari.server.events.listeners.alerts.AlertServiceStateListener;
import org.apache.ambari.server.events.listeners.alerts.AlertStateChangedListener;
import org.apache.ambari.server.events.listeners.upgrade.DistributeRepositoriesActionListener;
import org.apache.ambari.server.events.listeners.upgrade.HostVersionOutOfSyncListener;
import org.apache.ambari.server.events.publishers.AlertEventPublisher;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.google.inject.Binder;
import com.google.inject.Injector;

/**
 * The {@link EventBusSynchronizer} is used to replace the {@link AsyncEventBus}
 * used by Guava with a synchronous, serial {@link EventBus} instance. This
 * enables testing that relies on testing the outcome of asynchronous events by
 * executing the events on the current thread serially.
 */
public class EventBusSynchronizer {

  /**
   * Force the {@link EventBus} from {@link AmbariEventPublisher} to be serial
   * and synchronous.
   *
   * @param binder
   */
  public static void synchronizeAmbariEventPublisher(Binder binder) {
    EventBus synchronizedBus = new EventBus();
    AmbariEventPublisher ambariEventPublisher = new AmbariEventPublisher();

    replaceEventBus(AmbariEventPublisher.class, ambariEventPublisher,
        synchronizedBus);

    binder.bind(AmbariEventPublisher.class).toInstance(ambariEventPublisher);
  }

  /**
   * Force the {@link EventBus} from {@link AlertEventPublisher} to be serial
   * and synchronous. Also register the known listeners. Registering known
   * listeners is necessary since the event bus was replaced.
   *
   * @param injector
   */
  public static EventBus synchronizeAmbariEventPublisher(Injector injector) {
    EventBus synchronizedBus = new EventBus();
    AmbariEventPublisher publisher = injector.getInstance(AmbariEventPublisher.class);

    replaceEventBus(AmbariEventPublisher.class, publisher, synchronizedBus);

    // register common ambari event listeners
    registerAmbariListeners(injector, synchronizedBus);

    return synchronizedBus;
  }

  /**
   * Force the {@link EventBus} from {@link AlertEventPublisher} to be serial
   * and synchronous. Also register the known listeners. Registering known
   * listeners is necessary since the event bus was replaced.
   *
   * @param injector
   */
  public static EventBus synchronizeAlertEventPublisher(Injector injector) {
    EventBus synchronizedBus = new EventBus();
    AlertEventPublisher publisher = injector.getInstance(AlertEventPublisher.class);

    replaceEventBus(AlertEventPublisher.class, publisher, synchronizedBus);

    // register common alert event listeners
    registerAlertListeners(injector, synchronizedBus);

    return synchronizedBus;
  }

  /**
   * Register the normal listeners with the replaced synchronous bus.
   *
   * @param injector
   * @param synchronizedBus
   */
  private static void registerAmbariListeners(Injector injector,
      EventBus synchronizedBus) {
    synchronizedBus.register(injector.getInstance(AlertMaintenanceModeListener.class));
    synchronizedBus.register(injector.getInstance(AlertLifecycleListener.class));
    synchronizedBus.register(injector.getInstance(AlertServiceStateListener.class));
    synchronizedBus.register(injector.getInstance(DistributeRepositoriesActionListener.class));
    synchronizedBus.register(injector.getInstance(HostVersionOutOfSyncListener.class));
  }

  /**
   * Register the normal listeners with the replaced synchronous bus.
   *
   * @param injector
   * @param synchronizedBus
   */
  private static void registerAlertListeners(Injector injector,
      EventBus synchronizedBus) {
    synchronizedBus.register(injector.getInstance(AlertAggregateListener.class));
    synchronizedBus.register(injector.getInstance(AlertReceivedListener.class));
    synchronizedBus.register(injector.getInstance(AlertStateChangedListener.class));
  }

  private static void replaceEventBus(Class<?> eventPublisherClass,
      Object instance, EventBus eventBus) {

    try {
      Field field = eventPublisherClass.getDeclaredField("m_eventBus");
      field.setAccessible(true);
      field.set(instance, eventBus);
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }
}
