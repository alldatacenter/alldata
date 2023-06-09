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
package org.apache.drill.exec.vector.accessor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import org.joda.time.Period;

/**
 * Defines a reader to obtain values from value vectors using
 * a simple, uniform interface. Vector values are mapped to
 * their "natural" representations: the representation closest
 * to the actual vector value. For date and time values, this
 * generally means a numeric value. Applications can then map
 * this value to Java objects as desired. Decimal types all
 * map to BigDecimal as that is the only way in Java to
 * represent large decimal values.
 * <p>
 * In general, a column maps to just one value. However, derived
 * classes may choose to provide type conversions if convenient.
 * An exception is thrown if a call is made to a method that
 * is not supported by the column type.
 * <p>
 * Values of scalars are provided directly, using the get method
 * for the target type. Maps and arrays are structured types and
 * require another level of reader abstraction to access each value
 * in the structure.
 *
 * <h4>Joda Period</h4>
 *
 * Note that the interval columns here use the old Joda classes.
 * As it turns out, JSR-310, the specification on which the Java 8 date/time
 * classes are based, does not include the equivalent of the old Joda
 * Interval class: a single object which can hold years, months, days,
 * hours, minutes and seconds. Instead, JSR-310 has a Duration (for time)
 * and a Period (for dates). Drill may have to create its own class
 * to model the Drill INTERVAL type in JSR-310. Until then, we are stuck
 * with the Joda classes.
 * <p>
 * See {@link ScalarWriter}
 */
public interface ScalarReader extends ColumnReader {
  /**
   * Describe the type of the value. This is a compression of the
   * value vector type: it describes which method will return the
   * vector value.
   * @return the value type which indicates which get method
   * is valid for the column
   */
  ValueType valueType();

  /**
   * The extended type of the value, describes the secondary type
   * for DATE, TIME and TIMESTAMP for which the value type is
   * int or long.
   */
  ValueType extendedType();

  int getInt();
  boolean getBoolean();
  long getLong();
  float getFloat();
  double getDouble();
  String getString();
  byte[] getBytes();
  BigDecimal getDecimal();
  Period getPeriod();
  LocalDate getDate();
  LocalTime getTime();
  Instant getTimestamp();

  /**
   * Return the value of the object using the extended type.
   */
  Object getValue();
}
