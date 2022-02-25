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

package org.apache.ambari.server.collections;

import java.util.Map;

import com.google.gson.Gson;

/**
 * {@link Predicate} wraps {@link org.apache.commons.collections.Predicate} to
 * provide additional functionality like serializing to and from a Map and JSON formatted data.
 */
public abstract class Predicate implements org.apache.commons.collections.Predicate {

  /**
   * The name of this predicate. For example "and", "or", etc...
   */
  private final String name;

  protected Predicate(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  /**
   * Serialize this {@link Predicate} to a {@link Map}
   *
   * @return a {@link Map}
   */
  public abstract Map<String, Object> toMap();

  /**
   * Serialize this {@link Predicate} to a JSON-formatted {@link String}
   *
   * @return a JSON-formatted {@link String}
   */
  public String toJSON() {
    Map<String, Object> map = toMap();
    return (map == null) ? null : new Gson().toJson(map);
  }


  @Override
  public int hashCode() {
    return 37 * ((name == null) ? 0 : name.hashCode());
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    } else if (obj == null) {
      return false;
    } else if ((obj instanceof Predicate) && (hashCode() == obj.hashCode())) {
      Predicate p = (Predicate) obj;
      return (name == null) ? (p.name == null) : name.equals(p.name);
    } else {
      return false;
    }
  }

}

