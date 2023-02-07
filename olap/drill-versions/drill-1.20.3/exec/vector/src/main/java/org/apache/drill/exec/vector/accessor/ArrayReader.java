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
 * Generic array reader. An array is one of the following:
 * <ul>
 * <li>Array of scalars. Read the values using {@link #elements()}, which provides
 * an array-like access to the scalars.</li>
 * <li>A repeated map. Use {@link #tuple(int)} to get a tuple reader for a
 * specific array element. Use {@link #size()} to learn the number of maps in
 * the array.</li>
 * <li>List of lists. Use the {@link #array(int)} method to get the nested list
 * at a given index. Use {@link #size()} to learn the number of maps in
 * the array.</li>
 * </ul>
 * {@see ArrayWriter}
 */

public interface ArrayReader extends ColumnReader {

  /**
   * Number of elements in the array.
   * @return the number of elements
   */

  int size();

  /**
   * The object type of the list entry. All entries have the same
   * type.
   * @return the object type of each entry
   */

  ObjectType entryType();

  /**
   * Return the generic object reader for the array element. This
   * version <i>does not</i> position the reader, the client must
   * call {@link #setPosn(int)} to set the position. This form allows
   * up-front setup of the readers when convenient for the caller.
   */

  ObjectReader entry();
  ScalarReader scalar();
  TupleReader tuple();
  ArrayReader array();
  VariantReader variant();

  /**
   * Set the array reader to read a given array entry. Not used for
   * scalars, only for maps and arrays when using the non-indexed
   * methods {@link #entry()}, {@link #tuple()} and {@link #array()}.
   */

  void setPosn(int index);

  void rewind();

  /**
   * Move forward one position.
   *
   * @return true if another position is available, false if
   * the end of the array is reached
   */

  boolean next();
}
