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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import org.apache.ambari.server.collections.functors.PredicateClassFactory;
import org.apache.commons.lang.StringUtils;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

/**
 * PredicateUtils is a utility class providing methods to help perform tasks on {@link Predicate}s.
 */
public class PredicateUtils {
  private static final Type PARSED_TYPE = new TypeToken<Map<String, Object>>() {
  }.getType();

  /**
   * Safely serializes the specified {@link Predicate} to a {@link Map}
   *
   * @param predicate the {@link Predicate} to process
   * @return a {@link Map} or null of the supplied predicate is null
   */
  public static Map<String, Object> toMap(Predicate predicate) {
    return (predicate == null) ? null : predicate.toMap();
  }

  /**
   * Builds a {@link Predicate} from a {@link Map}
   *
   * @param map a map containing the details of the predicate to create.
   * @return a {@link Predicate}
   */
  public static Predicate fromMap(Map<?, ?> map) {
    Predicate predicate = null;

    if ((map != null) && !map.isEmpty()) {
      if (map.size() == 1) {
        Map.Entry<?, ?> entry = map.entrySet().iterator().next();
        String name = Objects.toString(entry.getKey());

        Class<? extends Predicate> predicateClass = PredicateClassFactory.getPredicateClass(name);

        if (predicateClass == null) {
          throw new IllegalArgumentException(String.format("Unexpected predicate name - %s", name));
        } else {
          try {
            // Dynamically locate and invoke the static toMap method for the named Predicate
            // implementation using reflection
            Method method = predicateClass.getMethod("fromMap", Map.class);
            if (method == null) {
              throw new UnsupportedOperationException(String.format("Cannot translate data to a %s - %s", predicateClass.getName(), "Failed to find toMap method"));
            } else {
              predicate = (Predicate) method.invoke(null, Collections.singletonMap(name, entry.getValue()));
            }
          } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new UnsupportedOperationException(String.format("Cannot translate data to a %s - %s", predicateClass.getName(), e.getLocalizedMessage()), e);
          }
        }
      } else {
        throw new IllegalArgumentException(String.format("Too many map entries have been encountered - %d", map.size()));
      }
    }

    return predicate;
  }


  /**
   * Safely serializes the specified {@link Predicate} to a JSON-formatted {@link String}
   *
   * @param predicate the {@link Predicate} to process
   * @return a {@link String} or null of the supplied predicate is null
   */
  public static String toJSON(Predicate predicate) {
    return (predicate == null) ? null : predicate.toJSON();
  }

  /**
   * Builds a {@link Predicate} from a JSON-formatted {@link String}
   *
   * @param json a string containing the details of the predicate to create.
   * @return a {@link Predicate}
   * @see #fromMap(Map)
   */
  public static Predicate fromJSON(String json) {
    Map<String, Object> map = new Gson().fromJson(json, PARSED_TYPE);
    return (StringUtils.isEmpty(json) ? null : fromMap(map));
  }
}
