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
package org.apache.drill.common;

import org.apache.commons.lang3.StringEscapeUtils;

/**
 * Builds a string in Drill's "plan string" format: that shown in the
 * text version of {@code EXPLAIN PLAN FOR} output. Example: <pre><code>
 * Mumble[foo=fred, bar=barney]</code></pre>
 * <p>
 * Similar to the Guava {@code Objects.ToStringHelper} class but for
 * the Drill "plan-string" format. Use this class for any object that
 * may be displayed in an query plan.
 * <p>
 * Example usage:<pre><code>
 * public String toString() {
 *   return PlanStringBuilder(this)
 *     .field("foo", foo)
 *     .field("bar", bar)
 *     .toString();
 * }</code></pre>
 */
public class PlanStringBuilder {

  private final StringBuilder buf = new StringBuilder();
  private int fieldCount = 0;

  public PlanStringBuilder(Object node) {
    this(node.getClass().getSimpleName());
  }

  public PlanStringBuilder(String node) {
    buf.append(node).append(" [");
  }

  /**
   * Displays the field as a quoted string: {@code foo="bar"}.
   */
  public PlanStringBuilder field(String key, String value) {
    if (value != null) {
      startField(key);
      buf.append("\"").append(value).append("\"");
    }
    return this;
  }

  /**
   * Displays the field as an unquoted string. Use this for things
   * like names: {@code mode=OPTIONAL}.
   */
  public PlanStringBuilder unquotedField(String key, String value) {
    if (value != null) {
      startField(key);
      buf.append(value);
    }
    return this;
  }

  /**
   * Displays the field as an unquoted {@code toString()} value.
   * Omits the field if the value is null.
   */
  public PlanStringBuilder field(String key, Object value) {
    if (value != null) {
      startField(key);
      buf.append(value);
    }
    return this;
  }

  /**
   * Displays a numeric field: {@code size=10}.
   */
  public PlanStringBuilder field(String key, int value) {
    startField(key);
    buf.append(value);
    return this;
  }

  /**
   * Displays a character in Java-quoted format: {@code delimiter="\n"}.
   */
  public PlanStringBuilder escapedField(String key, char value) {
    return escapedField(key, Character.toString(value));
  }

  /**
   * Displays a string in Java-quoted format: {@code delimiter="\t"}.
   */
  public PlanStringBuilder escapedField(String key, String value) {
    return field(key, StringEscapeUtils.escapeJava(value));
  }

  public PlanStringBuilder maskedField(String key, String value) {
    // Intentionally ignore length
    return field(key, value == null ? null : "*******");
  }

  private void startField(String key) {
    if (fieldCount++ != 0) {
      buf.append(", ");
    }
    buf.append(key).append("=");
  }

  @Override
  public String toString() { return buf.append("]").toString(); }
}
