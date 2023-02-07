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

import org.apache.drill.exec.record.metadata.ColumnMetadata;

/**
 * Base interface for all column readers, defining a generic set of methods
 * that all readers provide. In particular, given the metadata and the object
 * type, one can determine what to do with each reader when working with readers
 * generically. The <tt>getObject()</tt> and <tt>getAsString()</tt> methods provide
 * generic data access for tests and debugging.
 */

public interface ColumnReader {

  ColumnMetadata schema();

  /**
   * The type of this reader.
   *
   * @return type of reader
   */

  ObjectType type();

  /**
   * Determine if this value is null.
   * <ul>
   * <li>Nullable scalar: determine if the value is null.</li>
   * <li>Non-nullable scalar: always returns <tt>false</tt>.</li>
   * <li>Arrays: always returns </tt>false</tt.></li>
   * <li>Lists: determine if the list for the current row is null.
   * In a list, an array entry can be null, empty, or can contain
   * items. In repeated types, the array itself is never null.
   * If the array is null, then it implicitly has no entries.</li>
   * <li>Map or Repeated Map: Always returns <tt>false</tt>.</li>
   * <li>Map inside a union, or in a list that contains a union,
   * the tuple itself can be null.</li>
   * <li>Union: Determine if the current value is null. Null values have no type
   * and no associated reader.</li>
   * </ul>
   *
   * @return <tt>true</tt> if this value is null; <tt>false</tt> otherwise
   */

  boolean isNull();

  /**
   * Return the value of the underlying data as a Java object.
   * Primarily for testing
   * <ul>
   * <li>Array: Return the entire array as an <tt>List</tt> of objects.
   * Note, even if the array is scalar, the elements are still returned
   * as a list.</li>
   * </ul>
   * @return the value as a Java object
   */

  Object getObject();

  /**
   * Return the entire object as a string. Primarily for debugging.
   * @return string representation of the object
   */

  String getAsString();
}
