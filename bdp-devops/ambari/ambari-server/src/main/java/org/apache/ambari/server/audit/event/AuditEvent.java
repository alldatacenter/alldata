/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.audit.event;

/**
 * Audit event that contains
 * the details of an action/event
 * that is subject to audit.
 */
public interface AuditEvent {

  /**
   * Builder for {@link AuditEvent}
   *
   * @param <T> the type of the concrete audit event.
   */
  interface AuditEventBuilder<T extends AuditEvent> {
    /**
     * Builds an audit event.
     *
     * @return audit event instance
     */
    T build();
  }

  /**
   * Returns the timestamp of the audit event.
   *
   * @return timestamp of the audit event.
   */
  Long getTimestamp();

  /**
   * Returns the details of the audit event.
   *
   * @return detail of the audit event.
   */
  String getAuditMessage();

}
