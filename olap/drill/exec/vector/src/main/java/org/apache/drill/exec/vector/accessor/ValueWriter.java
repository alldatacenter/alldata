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
 * Writer for a scalar value. Column writers implement this interface
 * to map values directly to the underlying vector types, throwing an
 * exception for type mis-matches. Format-specific column converters
 * can implement this interface to perform type conversion. Since
 * both format-specific and generic writers share the same interface
 * they can be used interchangeably, avoiding an extra call level when
 * no conversion is needed.
 */
public interface ValueWriter {

  /**
   * Set the current value to null.
   *
   * throws IllegalStateException if called on a non-nullable value.
   */

  void setNull();
  void setBoolean(boolean value);
  void setInt(int value);
  void setLong(long value);
  void setFloat(float value);
  void setDouble(double value);
  void setString(String value);
  void appendBytes(byte[] value, int len);
  void setBytes(byte[] value, int len);
  void setDecimal(BigDecimal value);
  void setPeriod(Period value);
  void setDate(LocalDate value);
  void setTime(LocalTime value);
  void setTimestamp(Instant value);

  /**
   * Write value to a vector as a Java object of the "native" type for
   * the column. This form is available only on scalar writers. The
   * object must be of the form for the primary write method above.
   * <p>
   * Primarily to be used when the code already knows the object type.
   *
   * @param value a value that matches the primary setter above, or null
   * to set the column to null
   *
   * @see ColumnWriter#setObject(Object) for the generic case
   */

  void setValue(Object value);
}
