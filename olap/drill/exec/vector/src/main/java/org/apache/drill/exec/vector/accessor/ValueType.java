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

/**
 * Represents the primitive types supported to read and write data
 * from value vectors. Vectors support many data widths. For simplicity
 * (and because of no difference in performance), the get/set methods
 * use a reduced set of types. In general, each reader and writer
 * supports just one type. Though some may provide more than one
 * (such as access to bytes for a <tt>STRING</tt> value.)
 */

public enum ValueType {

  /**
   * The value is set from a boolean: BIT.
   */

  BOOLEAN,

  /**
   * The value is set from an integer: TINYINT,
   * SMALLINT, INT, UINT1, and UINT2.
   */

  INTEGER,

  /**
   * The value set from a long: BIGINT and
   * UINT4.
   */

  LONG,

  /**
   * Type is set from a float: FLOAT4.
   */
  FLOAT,

  /**
   * Type is set from a double: FLOAT4 and FLOAT8.
   */
  DOUBLE,

  /**
   * The value can be set from a string (for convenience).
   * VARCHAR and VAR16CHAR.
   */

  STRING,

  /**
   * The value is set from a byte buffer. VARCHAR (in production
   * code), VAR16CHAR, VARBINARY.
   */

  BYTES,

  /**
   * The value is set from a BigDecimal: any of Drill's decimal
   * types.
   */

  DECIMAL,

  /**
   * The value is set from a Period. Any of Drill's date/time
   * types. (Note: there is a known bug in which Drill incorrectly
   * differentiates between local date/times (those without a timezone)
   * and absolute date/times (those with a timezone.) Caveat emptor.
   */

  PERIOD,

  /**
   * The value is set from a Joda LocalDate. (Should upgrade to
   * Java 8 LocalDate.) Native type is an int.
   */

  DATE,

  /**
   * The value is set from a Joda LocalTime. (Should upgrade to
   * Java 8 LocalTime.) Native type is an int.
   */

  TIME,

  /**
   * The value is set from a Joda Instant. (Should upgrade to
   * Java 8 Instant.) Native type is a long.
   */

  TIMESTAMP,

  /**
   * The value has no type. This is typically a dummy writer used
   * for unprojected columns.
   */

  NULL
}
