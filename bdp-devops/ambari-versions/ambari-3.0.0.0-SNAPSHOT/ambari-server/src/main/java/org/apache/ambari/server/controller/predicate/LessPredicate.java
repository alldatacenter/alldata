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
package org.apache.ambari.server.controller.predicate;

import org.apache.ambari.server.controller.spi.Resource;

/**
 * Predicate that checks if a given value is less than a {@link Resource} property.
 */
public class LessPredicate<T> extends ComparisonPredicate<T> {

  /**
   * Construct a LessPredicate.
   *
   * @param propertyId  the property id
   * @param value       the value
   *
   * @throws IllegalArgumentException if the given value is null
   */
  public LessPredicate(String propertyId, Comparable<T> value) {
    super(propertyId, value);
    if (value == null) {
      throw new IllegalArgumentException("Value can't be null.");
    }
  }

  @Override
  public boolean evaluate(Resource resource) {
    Object propertyValue = resource.getPropertyValue(getPropertyId());
    return propertyValue != null && compareValueTo(propertyValue) > 0;
  }

  @Override
  public String getOperator() {
    return "<";
  }

  @Override
  public ComparisonPredicate<T> copy(String propertyId) {
    return new LessPredicate<>(propertyId, getValue());
  }
}
