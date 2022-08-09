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
package org.apache.ambari.server.events;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Singleton;

/**
 * The {@link MockEventListener} is used to provide a way to capture events
 * being fired via an {@link EventBus}.
 */
@Singleton
public class MockEventListener {

  /**
   * When an event is received, its class is captured and the event object is
   * added to the list.
   */
  private final Map<Class<?>, List<AmbariEvent>> m_receivedAmbariEvents = new HashMap<>();

  /**
   * When an event is received, its class is captured and the event object is
   * added to the list.
   */
  private final Map<Class<?>, List<AlertEvent>> m_receivedAlertEvents = new HashMap<>();

  /**
   * Resets the captured events.
   */
  public void reset() {
    m_receivedAmbariEvents.clear();
    m_receivedAlertEvents.clear();
  }

  /**
   * Gets whether an event of the specified class was received.
   *
   * @param clazz
   * @return
   */
  public boolean isAmbariEventReceived(Class<? extends AmbariEvent> clazz) {
    if (!m_receivedAmbariEvents.containsKey(clazz)) {
      return false;
    }

    return m_receivedAmbariEvents.get(clazz).size() > 0;
  }

  /**
   * Gets whether an event of the specified class was received.
   *
   * @param clazz
   * @return
   */
  public boolean isAlertEventReceived(Class<? extends AlertEvent> clazz) {
    if (!m_receivedAlertEvents.containsKey(clazz)) {
      return false;
    }

    return m_receivedAlertEvents.get(clazz).size() > 0;
  }

  /**
   * Gets the total number of events received for the specified class.
   *
   * @param clazz
   * @return
   */
  public int getAmbariEventReceivedCount(Class<? extends AmbariEvent> clazz) {
    if (!m_receivedAmbariEvents.containsKey(clazz)) {
      return 0;
    }

    return m_receivedAmbariEvents.get(clazz).size();
  }

  /**
   * Gets the total number of events received for the specified class.
   *
   * @param clazz
   * @return
   */
  public int getAlertEventReceivedCount(Class<? extends AlertEvent> clazz) {
    if (!m_receivedAlertEvents.containsKey(clazz)) {
      return 0;
    }

    return m_receivedAlertEvents.get(clazz).size();
  }


  /**
   * Get the instances
   *
   * @param clazz
   * @return
   */
  public List<AmbariEvent> getAmbariEventInstances(Class<? extends AmbariEvent> clazz){
    if (!m_receivedAmbariEvents.containsKey(clazz)) {
      return Collections.emptyList();
    }

    return m_receivedAmbariEvents.get(clazz);
  }

  /**
   * Get the instances
   *
   * @param clazz
   * @return
   */
  public List<AlertEvent> getAlertEventInstances(Class<? extends AlertEvent> clazz){
    if (!m_receivedAlertEvents.containsKey(clazz)) {
      return Collections.emptyList();
    }

    return m_receivedAlertEvents.get(clazz);
  }

  /**
   * @param event
   */
  @Subscribe
  public void onAmbariEvent(AmbariEvent event) {
    List<AmbariEvent> events = m_receivedAmbariEvents.get(event.getClass());
    if (null == events) {
      events = new ArrayList<>();
      m_receivedAmbariEvents.put(event.getClass(), events);
    }

    events.add(event);
  }

  /**
   * @param event
   */
  @Subscribe
  public void onAlertEvent(AlertEvent event) {
    List<AlertEvent> events = m_receivedAlertEvents.get(event.getClass());
    if (null == events) {
      events = new ArrayList<>();
      m_receivedAlertEvents.put(event.getClass(), events);
    }

    events.add(event);
  }
}
