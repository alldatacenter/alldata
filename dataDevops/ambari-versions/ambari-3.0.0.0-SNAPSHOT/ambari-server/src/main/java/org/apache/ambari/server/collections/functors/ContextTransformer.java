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

package org.apache.ambari.server.collections.functors;

import java.util.Map;

import org.apache.commons.collections.Transformer;

/**
 * {@link ContextTransformer} is a {@link Transformer} implementation that traverses a {@link Map}
 * of {@link Map}s to find the value related to the specified key.
 * <p>
 * This implementation first checks the context map for the explicit key. If the key exists, it
 * returns the value associated with it. If the key does not exist and appears to represent a path
 * via "/"-delimited keys (configurations/service-site/property_name), the path is traversed
 * recursively looking for the requested data.
 * <p>
 * Example context:
 * <pre>
 * |- services : [set of services]
 * |
 * |- configurations
 * |  |- service-site
 * |  |  |- property : [value]
 * |
 * |- key/looks/like/path : [value2]
 * </pre>
 * <ul>
 * <li>If the key was <code>services</code>, <code>[set of services]</code> would be returned</li>
 * <li>If the key was <code>configurations/service-site/property</code>, <code>value</code> would be returned</li>
 * <li>If the key was <code>configurations/service-site</code>, <code>[map of service-site properties]</code> would be returned</li>
 * <li>If the key was <code>key/looks/like/path</code>, <code>value2</code> would be returned</li>
 * </ul>
 */
public class ContextTransformer implements Transformer {

  /**
   * The key to search for
   */
  private final String key;

  /**
   * Constructor.
   *
   * @param key the key to search for
   */
  public ContextTransformer(String key) {
    this.key = key;
  }

  /**
   * Returns the key this transformer is using to search for data
   *
   * @return a string
   */
  public String getKey() {
    return key;
  }

  @Override
  public Object transform(Object o) {
    return transform(key, o);
  }

  /**
   * Traverses the input object (expected to be a {@link Map}) to find the data associated with
   * the specified key.
   * <p>
   * Note: This method is recursive in the even the key represents a path
   *
   * @param key the key to search for
   * @param o   the object containing data to process
   * @return the found data or null
   */
  private Object transform(String key, Object o) {
    Object transformedData = null;

    if (key != null) {
      if (o instanceof Map) {
        Map<?, ?> data = (Map) o;

        if (data.containsKey(key)) {
          transformedData = data.get(key);
        } else {
          // See if the key implies a tree that needs to be traversed...
          // For example:  configurations/service-conf/property_name
          //  A map of maps is expected such that the top level map has a map identified by the key
          //  of "configuration".  The "configuration" map has a map identified by the key of
          // "service-conf". The "service-conf" has a value identified by the key of "property_name".
          String[] parts = key.split("\\/", 2);

          // If only a single item is returned, than the key does not indicate a tree.
          if (parts.length == 2) {
            // If the first item is empty, a leading "/" was encountered... retry with the pruned key
            if (parts[0].isEmpty()) {
              transformedData = transform(parts[1], o);
            } else {
              transformedData = transform(parts[1], data.get(parts[0]));
            }
          }
        }
      }
    }

    return transformedData;
  }

  @Override
  public int hashCode() {
    return (37 * ((key == null) ? 0 : key.hashCode()));
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    } else if (obj == null) {
      return false;
    } else if ((obj instanceof ContextTransformer) && (hashCode() == obj.hashCode())) {
      ContextTransformer t = (ContextTransformer) obj;
      return (key == null) ? (t.key == null) : key.equals(t.key);
    } else {
      return false;
    }
  }
}
