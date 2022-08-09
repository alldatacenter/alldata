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
package org.apache.ambari.server.stack.upgrade.orchestrate;

import org.apache.ambari.server.stack.upgrade.Direction;

/**
 * Enum used to define placeholder text for replacement
 */
enum Placeholder {
  /**
   * No placeholder defined
   */
  OTHER(""),
  /**
   * A placeholder token that represents all of the hosts that a component is
   * deployed on. This can be used for cases where text needs to be rendered
   * with all of the hosts mentioned by their FQDN.
   */
  HOST_ALL("hosts.all"),
  /**
   * A placeholder token that represents a single, active master that a
   * component is deployed on. This can be used for cases where text needs to eb
   * rendered with a single master host FQDN inserted.
   */
  HOST_MASTER("hosts.master"),
  /**
   * The version that the stack is being upgraded or downgraded to, such as
   * {@code 2.2.1.0-1234}.
   */
  VERSION("version"),
  /**
   * The lower case of the {@link Direction} value.
   */
  DIRECTION_TEXT("direction.text"),
  /**
   * The proper case of the {@link Direction} value.
   */
  DIRECTION_TEXT_PROPER("direction.text.proper"),
  /**
   * The past tense of the {@link Direction} value.
   */
  DIRECTION_PAST("direction.past"),
  /**
   * The proper past tense of the {@link Direction} value.
   */
  DIRECTION_PAST_PROPER("direction.past.proper"),
  /**
   * The plural tense of the {@link Direction} value.
   */
  DIRECTION_PLURAL("direction.plural"),
  /**
   * The proper plural tense of the {@link Direction} value.
   */
  DIRECTION_PLURAL_PROPER("direction.plural.proper"),
  /**
   * The verbal noun of the {@link Direction} value.
   */
  DIRECTION_VERB("direction.verb"),
  /**
   * The proper verbal noun of the {@link Direction} value.
   */
  DIRECTION_VERB_PROPER("direction.verb.proper");

  private String pattern;
  Placeholder(String key) {
    pattern = "{{" + key + "}}";
  }

  static Placeholder find(String pattern) {
    for (Placeholder p : values()) {
      if (p.pattern.equals(pattern)) {
        return p;
      }
    }

    return OTHER;
  }
}