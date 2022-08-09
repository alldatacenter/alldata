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
package org.apache.ambari.server.controller.utilities;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.internal.PropertyPredicateVisitor;
import org.apache.ambari.server.controller.predicate.BasePredicate;
import org.apache.ambari.server.controller.predicate.PredicateVisitor;
import org.apache.ambari.server.controller.predicate.PredicateVisitorAcceptor;
import org.apache.ambari.server.controller.spi.Predicate;

/**
 *
 */
public class PredicateHelper {

  public static Set<String> getPropertyIds(Predicate predicate) {
    if (predicate instanceof BasePredicate) {
      return ((BasePredicate) predicate).getPropertyIds();
    }
    return Collections.emptySet();
  }

  public static void visit(Predicate predicate, PredicateVisitor visitor) {
    if (predicate instanceof PredicateVisitorAcceptor) {
      ((PredicateVisitorAcceptor) predicate).accept(visitor);
    }
  }

  /**
   * Get a map of property values from a given predicate.
   *
   * @param predicate  the predicate
   *
   * @return the map of properties
   */
  public static Map<String, Object> getProperties(Predicate predicate) {
    if (predicate == null) {
      return Collections.emptyMap();
    }
    PropertyPredicateVisitor visitor = new PropertyPredicateVisitor();
    visit(predicate, visitor);
    return visitor.getProperties();
  }
}
