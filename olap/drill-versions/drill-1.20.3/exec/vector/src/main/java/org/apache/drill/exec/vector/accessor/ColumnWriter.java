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
 * Generic information about a column writer including:
 * <ul>
 * <li>Metadata</li>
 * <li>Write position information about a writer needed by a vector overflow
 * implementation. Hides the details of implementation and the writer class
 * hierarchy, exposing just the required write position information.</li>
 * <li>Generic methods for writing to the object, primarily used for
 * testing.</li>
 */

public interface ColumnWriter {

  /**
   * Return the object (structure) type of this writer.
   *
   * @return type indicating if this is a scalar, tuple or array
   */

  ObjectType type();

  /**
   * Whether this writer allows nulls. This is not as simple as checking
   * for the {@link org.apache.drill.common.types.TypeProtos.DataMode#OPTIONAL} type in the schema. List entries
   * are nullable, if they are primitive, but not if they are maps or lists.
   * Unions are nullable, regardless of cardinality.
   *
   * @return true if a call to {@link #setNull()} is supported, false
   * if not
   */

  boolean nullable();

  /**
   * Whether this writer is projected (is backed by a materialized vector),
   * or is unprojected (is just a dummy writer.) In most cases, clients can
   * ignore whether the column is projected and just write to the writer.
   * This flag handles those special cases where it is helpful to know if
   * the column is projected or not.
   */

  boolean isProjected();

  /**
   * Returns the schema of the column associated with this writer.
   *
   * @return schema for this writer's column
   */

  ColumnMetadata schema();

  /**
   * Set the current value to null. Support depends on the underlying
   * implementation: only nullable types support this operation.
   *
   * throws IllegalStateException if called on a non-nullable value.
   */

  void setNull();

  /**
   * Copy a single value from the given reader, which must be of the
   * same type as this writer.
   *
   * @param from reader to provide the data
   */

  void copy(ColumnReader from);

  /**
   * Generic technique to write data as a generic Java object. The
   * type of the object must match the target writer.
   * Primarily for testing.
   * <ul>
   * <li>Scalar: The type of the Java object must match the type of
   * the target vector. <tt>String</tt> or <tt>byte[]</tt> can be
   * used for Varchar vectors.</li>
   * <li>Array: Write the array given an array of values. The object
   * must be a Java array. The type of the array must match the type of
   * element in the repeated vector. That is, if the vector is
   * a <tt>Repeated Int</tt>, provide an <tt>int[]</tt> array.</tt></li>
   * <li>Tuple (map or row): The Java object must be an array of objects
   * in which the members of the array have a 1:1 correspondence with the
   * members of the tuple in the order defined by the writer metadata.
   * That is, if the map is (Int, Varchar), provide a <tt>Object[]</tt>
   * array like this: <tt>{10, "fred"}</tt>.</li>
   * <li>Union: Uses the Java object type to determine the type of the
   * backing vector. Creates a vector
   * of the required type if needed.</li>
   *
   * @param value value to write to the vector. The Java type of the
   * object indicates the Drill storage type
   * @throws IllegalArgumentException if the type of the Java object
   * cannot be mapped to the type of the underlying vector or
   * vector structure
   */

  void setObject(Object value);
}
