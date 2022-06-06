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

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.ambari.server.controller.spi.Resource;

/**
 * Predicate that checks if current property matches the filter expression
 */
public class FilterPredicate extends ComparisonPredicate {
  private final Matcher matcher;
  private final String patternExpr;
  private final String emptyString = "";

  /**
   * Takes the PropertyId and a regex to match with the property value
   * @param propertyId Property Id
   * @param patternExpr Regex pattern
   */
  @SuppressWarnings("unchecked")
  public FilterPredicate(String propertyId, String patternExpr) {
    super(propertyId, patternExpr);
    this.patternExpr = patternExpr;
    try {
      Pattern pattern = Pattern.compile(patternExpr != null ? patternExpr : emptyString);
      matcher = pattern.matcher(emptyString);
    } catch (PatternSyntaxException pe) {
      throw new IllegalArgumentException(pe);
    }
  }

  @Override
  public boolean evaluate(Resource resource) {
    Object propertyValue =  resource.getPropertyValue(getPropertyId());
    matcher.reset(propertyValue != null ? propertyValue.toString() : emptyString);

    return patternExpr == null ?
      propertyValue == null :
      propertyValue != null && matcher.matches();
  }

  @Override
  public String getOperator() {
    return ".FILTER";
  }

  @Override
  public ComparisonPredicate copy(String propertyId) {
    return new FilterPredicate(propertyId, patternExpr);
  }

  @Override
  public String toString() {
    return getPropertyId() + getOperator() + "(" + getValue() + ")";
  }
}
