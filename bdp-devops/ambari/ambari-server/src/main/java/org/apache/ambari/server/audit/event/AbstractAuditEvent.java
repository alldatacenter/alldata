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

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * Base class for concrete audit event types.
 */
public abstract class AbstractAuditEvent implements AuditEvent {

  /**
   * Timestamp for the audit event creation
   */
  private final Long timestamp;

  /**
   * Message to log
   */
  private final String auditMessage;

  /**
   * Base class for concrete audit event builders.
   *
   * @param <T>        the type of the concrete audit event built by this builder
   * @param <TBuilder> the type of the concrete audit event builder.
   */
  protected static abstract class AbstractAuditEventBuilder<T extends AbstractAuditEvent, TBuilder extends AbstractAuditEventBuilder<T, TBuilder>>
    implements AuditEventBuilder<T> {

    private Long timestamp;
    private String auditMessage;
    private final Class<? extends TBuilder> builderClass;

    /**
     * Creates a new audit event instance from this builder.
     *
     * @return the build audit event instance.
     */
    protected abstract T newAuditEvent();

    /**
     * Appends details from this builder to the detailed description of the audit event.
     *
     * @param builder builder for the audit event details.
     */
    protected abstract void buildAuditMessage(StringBuilder builder);

    protected AbstractAuditEventBuilder(Class<? extends TBuilder> builderClass) {
      this.builderClass = builderClass;
    }

    /**
     * The timestamp of the audit event.
     *
     * @param timestamp
     * @return
     */
    public TBuilder withTimestamp(Long timestamp) {
      this.timestamp = timestamp;

      return self();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final T build() {
      final StringBuilder auditMessageBuilder = new StringBuilder();

      buildAuditMessage(auditMessageBuilder);
      auditMessage = auditMessageBuilder.toString();

      return newAuditEvent();
    }

    protected TBuilder self() {
      return builderClass.cast(this);
    }
  }


  protected AbstractAuditEvent() {
    this.timestamp = null;
    this.auditMessage = null;
  }

  /**
   * Constructor. Initializes fields using the provided builder.
   *
   * @param builder
   */
  protected AbstractAuditEvent(AbstractAuditEventBuilder<?, ?> builder) {
    timestamp = builder.timestamp;
    auditMessage = builder.auditMessage;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Long getTimestamp() {
    return timestamp;
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public String getAuditMessage() {
    return auditMessage;
  }

  @Override
  public final boolean equals(Object o) {
    if (this == o) return true;

    if (!(o instanceof AbstractAuditEvent)) return false;

    AbstractAuditEvent that = (AbstractAuditEvent) o;

    return new EqualsBuilder()
      .append(getTimestamp(), that.getTimestamp())
      .append(getAuditMessage(), that.getAuditMessage())
      .isEquals();
  }

  @Override
  public final int hashCode() {
    return new HashCodeBuilder(17, 37)
      .append(getTimestamp())
      .append(getAuditMessage())
      .toHashCode();
  }
}
