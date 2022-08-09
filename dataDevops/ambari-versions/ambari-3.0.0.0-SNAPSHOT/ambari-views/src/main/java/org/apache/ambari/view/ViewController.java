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

package org.apache.ambari.view;

import org.apache.ambari.view.events.Listener;

import java.util.Map;

/**
 * View controller.
 */
public interface ViewController {
  /**
   * Notify all listeners registered to listen to this view
   * of the given view event.
   *
   * @param eventId          the event id
   * @param eventProperties  the event properties
   */
  public void fireEvent(String eventId, Map<String, String> eventProperties);

  /**
   * Register a listener to listen for events from the view identified by the
   * given name.
   *
   * @param listener  the listener
   * @param viewName  the view to listen to
   */
  public void registerListener(Listener listener, String viewName);

  /**
   * Register a listener to listen for events from the view identified by the
   * given name and version.
   *
   * @param listener     the listener
   * @param viewName     the view to listen to
   * @param viewVersion  the view version
   */
  public void registerListener(Listener listener, String viewName, String viewVersion);

  /**
   * Un-register the listener that is registered for the view identified by the
   * given name.
   *
   * @param listener  the listener
   * @param viewName  the view to listen to
   */
  public void unregisterListener(Listener listener, String viewName);

  /**
   * Un-register the listener that is registered for the view identified by the
   * given name and version.
   *
   * @param listener     the listener
   * @param viewName     the view to listen to
   * @param viewVersion  the view version
   */
  public void unregisterListener(Listener listener, String viewName, String viewVersion);
}
