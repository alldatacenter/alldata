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
package org.apache.ambari.server.events.jpa;

import javax.persistence.EntityManagerFactory;

import org.apache.ambari.server.events.AmbariEvent;

/**
 * The {@link JPAEvent} class is the base for all JPA events in Ambari. Although
 * this class could inherit from {@link AmbariEvent}, the publishers for
 * {@link AmbariEvent} instances use an asynchronous model. With JPA, we want a
 * synchronous publication model in order to ensure that events are handled
 * within the scope of the method invoking them.
 */
public abstract class JPAEvent {

  /**
   * The {@link JPAEventType} defines the type of JPA event.
   */
  public enum JPAEventType {

    /**
     * An event which instructs an {@link EntityManagerFactory} to evict
     * instances of a particular class.
     */
    CACHE_INVALIDATION;
  }

  /**
   * The concrete event's type.
   */
  protected final JPAEventType m_eventType;

  /**
   * Constructor.
   *
   * @param eventType
   *          the type of event (not {@code null}).
   */
  public JPAEvent(JPAEventType eventType) {
    m_eventType = eventType;
  }

  /**
   * Gets the type of {@link JPAEvent}.
   *
   * @return the event type (never {@code null}).
   */
  public JPAEventType getType() {
    return m_eventType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    StringBuilder buffer = new StringBuilder(getClass().getSimpleName());
    buffer.append("{eventType=").append(m_eventType);
    buffer.append("}");
    return buffer.toString();
  }
}
