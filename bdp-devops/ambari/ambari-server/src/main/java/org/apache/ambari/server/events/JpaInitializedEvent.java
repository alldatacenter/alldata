/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import org.apache.ambari.server.orm.GuiceJpaInitializer;

import com.google.inject.persist.PersistService;

/**
 * Event signaling that JPA has been initialized through Guice (so that anyone
 * needs JPA context can work).
 * 
 * This events needs to be triggered by {@link GuiceJpaInitializer} to indicate
 * that JPA is initialized so that any client requires {@link PersistService} to
 * be started can start its work
 */
public class JpaInitializedEvent extends AmbariEvent {

  public JpaInitializedEvent() {
    super(AmbariEventType.JPA_INITIALIZED);
  }
}
