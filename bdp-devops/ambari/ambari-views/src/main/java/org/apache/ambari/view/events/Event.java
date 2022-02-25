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

package org.apache.ambari.view.events;

import org.apache.ambari.view.ViewDefinition;
import org.apache.ambari.view.ViewInstanceDefinition;

import java.util.Map;

/**
 * View event interface.
 */
public interface Event {
  /**
   * Get the event identifier.
   *
   * @return the id
   */
  public String getId();

  /**
   * Get the event properties.
   *
   * @return the properties
   */
  public Map<String, String> getProperties();

  /**
   * Get the view subject of the event.
   *
   * @return the view subject
   */
  public ViewDefinition getViewSubject();

  /**
   * Get the instance subject of the event.
   *
   * @return the instance subject; null if this is a view level event
   */
  public ViewInstanceDefinition getViewInstanceSubject();
}
