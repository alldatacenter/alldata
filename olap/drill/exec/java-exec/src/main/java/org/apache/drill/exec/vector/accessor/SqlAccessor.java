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

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;

import org.apache.drill.common.types.TypeProtos.MajorType;


// TODO:  Doc.:  Document more of basics of pattern of contracts for getXxx(...):
// - What constitutes invalid access (that throws InvalidAccessException):
//   - Does it include out-of-bound index values?  (The lack of "throws
//     InvalidAccessException" on isNull(...) suggests no, but ...)

/**
 * Column-data accessor that implements JDBC's Java-null--when--SQL-NULL mapping.
 * <p>
 *  Each {@code rowOffset} parameter specifies the (zero-based) offset (in rows)
 *  of the requested value.
 * </p>
 * <p>
 *   When the requested value is logically a SQL NULL:
 * </p>
 * <li>
 *   a get method that return primitive type throws an exception (callers are
 *   responsible for calling {@link #isNull(int)} to check for null before calling
 *   such methods)
 * </li>
 * <li>
 *   a get method that returns a non-primitive type returns Java {@code null}
 *   (the caller does not need to call {@link #isNull(int)} to check for nulls)
 * </li>
 */
public interface SqlAccessor {

  /**
   * Reports the (native) type of data accessed by this accessor.
   * <p>
   *   (Some implementations may support more than just the minimum
   *   <code>get<i>Type</i>(<i>...</i>)</code> method implied by the type.
   * </p>
   */
  MajorType getType();

  /**
   * Reports the class returned by getObject() of this accessor.
   * <p>
   *  (Is for {@link ResultSetMetaData#getColumnClassName(...)}.)
   * </p>
   */
  Class<?> getObjectClass();

  /**
   * Reports whether the logical value is a SQL NULL.
   */
  boolean isNull(int rowOffset);

  /** (See {@link SqlAccessor class description}.) */
  BigDecimal getBigDecimal(int rowOffset) throws InvalidAccessException;

  /** (See {@link SqlAccessor class description}.) */
  boolean getBoolean(int rowOffset) throws InvalidAccessException;

  /** (See {@link SqlAccessor class description}.) */
  byte getByte(int rowOffset) throws InvalidAccessException;

  /** (See {@link SqlAccessor class description}.) */
  byte[] getBytes(int rowOffset) throws InvalidAccessException;

  /** (See {@link SqlAccessor class description}.) */
  Date getDate(int rowOffset) throws InvalidAccessException;

  /** (See {@link SqlAccessor class description}.) */
  double getDouble(int rowOffset) throws InvalidAccessException;

  /** (See {@link SqlAccessor class description}.) */
  float getFloat(int rowOffset) throws InvalidAccessException;

  /** (See {@link SqlAccessor class description}.) */
  char getChar(int rowOffset) throws InvalidAccessException;

  /** (See {@link SqlAccessor class description}.) */
  int getInt(int rowOffset) throws InvalidAccessException;

  /** (See {@link SqlAccessor class description}.) */
  long getLong(int rowOffset) throws InvalidAccessException;

  /** (See {@link SqlAccessor class description}.) */
  short getShort(int rowOffset) throws InvalidAccessException;

  /** (See {@link SqlAccessor class description}.) */
  InputStream getStream(int rowOffset) throws InvalidAccessException;

  /** (See {@link SqlAccessor class description}.) */
  Reader getReader(int rowOffset) throws InvalidAccessException;

  // TODO: Doc./Spec.:  What should happen if called on non-string type?  (Most
  // are convertible to string.  Does that result in error or conversion?)
  // Similar question for many other methods.
  /** (See {@link SqlAccessor class description}.) */
  String getString(int rowOffset) throws InvalidAccessException;

  /** (See {@link SqlAccessor class description}.) */
  Time getTime(int rowOffset) throws InvalidAccessException;

  /** (See {@link SqlAccessor class description}.) */
  Timestamp getTimestamp(int rowOffset) throws InvalidAccessException;

  /** (See {@link SqlAccessor class description}.) */
  Object getObject(int rowOffset) throws InvalidAccessException;

}
