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
package org.apache.ambari.server.notifications;

import java.util.HashMap;
import java.util.Map;

import com.google.inject.Singleton;

/**
 * The {@link DispatchFactory} is used to provide singleton instances of
 * {@link NotificationDispatcher} based on a supplied type.
 */
@Singleton
public class DispatchFactory {

  /**
   * Singleton.
   */
  private static final DispatchFactory s_instance = new DispatchFactory();

  /**
   * Mapping of dispatch type to dispatcher singleton.
   */
  private final Map<String, NotificationDispatcher> m_dispatchers = new HashMap<>();

  /**
   * Constructor.
   *
   */
  private DispatchFactory() {
  }

  /**
   * Gets the single instance of this factory.
   *
   * @return
   */
  public static DispatchFactory getInstance() {
    return s_instance;
  }

  /**
   * Registers a dispatcher instance with a type.
   *
   * @param type
   *          the type
   * @param dispatcher
   *          the dispatcher to register with the type.
   */
  public void register(String type, NotificationDispatcher dispatcher) {
    if (null == dispatcher) {
      m_dispatchers.remove(type);
    } else {
      m_dispatchers.put(type, dispatcher);
    }
  }

  /**
   * Gets a dispatcher based on the type.
   *
   * @param type
   *          the type (not {@code null}).
   * @return a dispatcher instance, or {@code null} if none.
   */
  public NotificationDispatcher getDispatcher(String type) {
    return m_dispatchers.get(type);
  }
}
