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

import java.text.NumberFormat;
import java.text.ParsePosition;

import org.apache.ambari.server.controller.spi.Resource;

/**
 * Predicate that compares a given value to a {@link Resource} property.
 */
public abstract class ComparisonPredicate<T> extends PropertyPredicate implements BasePredicate {
  private final Comparable<T> value;
  private final String stringValue;
  private final Double doubleValue;

  protected ComparisonPredicate(String propertyId, Comparable<T> value) {
    super(propertyId);
    this.value = value;

    if (value instanceof Number) {
      stringValue = null;
      doubleValue = ((Number) value).doubleValue();
    }
    else if (value instanceof String) {
      stringValue = (String) value;
      doubleValue = stringToDouble(stringValue);
    }
    else {
      stringValue = null;
      doubleValue = null;
    }
  }

  public Comparable<T> getValue() {
    return value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ComparisonPredicate)) return false;
    if (!super.equals(o)) return false;

    ComparisonPredicate that = (ComparisonPredicate) o;

    return !(value != null ? !value.equals(that.value) : that.value != null);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (value != null ? value.hashCode() : 0);
    return result;
  }

  @Override
  public void accept(PredicateVisitor visitor) {
    visitor.acceptComparisonPredicate(this);
  }

  protected int compareValueToIgnoreCase(Object propertyValue) throws ClassCastException{
    return compareValueTo(propertyValue, true); // case insensitive
  }

  protected int compareValueTo(Object propertyValue) throws ClassCastException{
    return compareValueTo(propertyValue, false); // case sensitive
  }

  private int compareValueTo(Object propertyValue, boolean ignoreCase) throws ClassCastException {
    if (doubleValue != null) {
      if (propertyValue instanceof Number) {
        return doubleValue.compareTo(((Number) propertyValue).doubleValue());
      }
      else if (propertyValue instanceof String) {
        Double doubleFromString = stringToDouble((String) propertyValue);
        if (doubleFromString != null) {
          return doubleValue.compareTo(doubleFromString);
        }
      }
    }

    if (stringValue != null) {
      if (ignoreCase) {
        return stringValue.compareToIgnoreCase(propertyValue.toString());
      }
      else {
        return stringValue.compareTo(propertyValue.toString());
      }
    }

    return getValue().compareTo((T) propertyValue);
  }

  private Double stringToDouble(String stringValue) {
    if (stringValue == null || stringValue.isEmpty()) {
      return null;
    }

    ParsePosition parsePosition = new ParsePosition(0);
    NumberFormat  numberFormat  = NumberFormat.getInstance();
    Number        parsedNumber  = numberFormat.parse(stringValue, parsePosition);

    return parsePosition.getIndex() == stringValue.length() ? parsedNumber.doubleValue() : null;
  }

  public abstract String getOperator();

  public abstract ComparisonPredicate<T> copy(String propertyId);


  // ----- Object overrides --------------------------------------------------

  @Override
  public String toString() {
    return getPropertyId() + getOperator() + getValue();
  }
}
