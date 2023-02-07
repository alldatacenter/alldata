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
 * Writer for values into an array. Array writes are write-once, sequential:
 * each call to a <tt>setFoo()</tt> method writes a value and advances the array
 * index.
 * <p>
 * The array writer represents a Drill repeated type, including repeated maps.
 * The array writer also represents the Drill list and repeated list types as
 * follows:
 * <ul>
 * <li>A repeated scalar type is presented as an array writer with scalar
 * entries. As a convenience, writing to the scalar automatically advances
 * the current array write position, since exactly one item can be written
 * per array entry.</li>
 * <li>A repeated map type is presented as an array writer with tuple
 * entries. The client must advance the array write position explicitly since
 * a tuple can have any number of entries and the array writer cannot determine
 * when a value is complete.</li>
 * <li>A list type is presented as an array of variant entries. The client
 * must explicitly advance the array position.</li>
 * <li>A repeated list type is presented as an array of arrays of variants.
 * The client advances the array position of both lists.</li>
 * <li>Lists of repeated lists have three levels of arrays, repeated lists
 * of repeated lists have four levels of arrays, and so on.</li>
 * </ul>
 * <p>
 * Although the list vector supports a union of any Drill type, the only sane
 * combinations are:
 * <ul>
 * <li>One of a (single or repeated) (map or list), or</li>
 * <li>One or more scalar type.</li>
 * </ul>
 *
 * If a particular array has only one type (single/repeated map/list), then,
 * for convenience, the caller can directly request a writer of that type
 * without having to first retrieve the variant (although the indirect
 * route is, of course, available.)
 *
 * @see ArrayReader
 */

public interface ArrayWriter extends ColumnWriter {

  /**
   * Number of elements written thus far to the array.
   * @return the number of elements
   */

  int size();

  /**
   * Return a generic object writer for the array entry.
   *
   * @return generic object reader
   */

  ObjectType entryType();

  void setNull(boolean isNull);

  /**
   * The object type of the list entry. All entries have the same
   * type.
   * @return the object type of each entry
   */

  ObjectWriter entry();
  ScalarWriter scalar();
  TupleWriter tuple();
  ArrayWriter array();
  VariantWriter variant();
  DictWriter dict();

  /**
   * When the array contains a tuple or an array, call <tt>save()</tt>
   * after each array value. Not necessary when writing scalars; each
   * set operation calls save automatically.
   */

  void save();
}
