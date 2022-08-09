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

package org.apache.ambari.server.view.events;

import java.util.Collections;
import java.util.Map;

import org.apache.ambari.view.ViewDefinition;
import org.apache.ambari.view.ViewInstanceDefinition;
import org.apache.ambari.view.events.Event;

/**
 * View event implementation.
 */
public class EventImpl implements Event {
  /**
   * The event id.
   */
  private final String id;

  /**
   * The event properties.
   */
  private final Map<String, String> properties;

  /**
   * The view subject of the event.
   */
  private final ViewDefinition viewSubject;

  /**
   * The instance subject of the event.
   */
  private final ViewInstanceDefinition viewInstanceSubject;


  // ----- Constructors ------------------------------------------------------

  /**
   * Construct an event.
   *
   * @param id           the event id
   * @param properties   the event properties
   * @param viewSubject  the subject of the event
   */
  public EventImpl(String id, Map<String, String> properties, ViewDefinition viewSubject) {
    this(id, properties, viewSubject, null);
  }

  /**
   * Construct an event.
   *
   * @param id                   the event id
   * @param properties           the event properties
   * @param viewInstanceSubject  the subject of the event
   */
  public EventImpl(String id, Map<String, String> properties, ViewInstanceDefinition viewInstanceSubject) {
    this(id, properties, viewInstanceSubject.getViewDefinition(), viewInstanceSubject);
  }

  // private constructor
  private EventImpl(String id, Map<String, String> properties,
                ViewDefinition viewSubject, ViewInstanceDefinition viewInstanceSubject) {
    this.id                  = id;
    this.viewSubject         = viewSubject;
    this.viewInstanceSubject = viewInstanceSubject;
    this.properties          = properties == null ? Collections.emptyMap() :
        Collections.unmodifiableMap(properties);
  }


  // ----- Event -------------------------------------------------------------

  /**
   * Get the event identifier.
   *
   * @return the id
   */
  @Override
  public String getId() {
    return id;
  }

  /**
   * Get the event properties.
   *
   * @return the properties
   */
  @Override
  public Map<String, String> getProperties() {
    return properties;
  }

  /**
   * Get the view subject of the event.
   *
   * @return the view subject
   */
  @Override
  public ViewDefinition getViewSubject() {
    return viewSubject;
  }

  /**
   * Get the instance subject of the event.
   *
   * @return the instance subject; null if this is a view level event
   */
  @Override
  public ViewInstanceDefinition getViewInstanceSubject() {
    return viewInstanceSubject;
  }

  // ----- Object overrides --------------------------------------------------

  @Override
  public String toString() {
    return "View Event " + id;
  }
}
