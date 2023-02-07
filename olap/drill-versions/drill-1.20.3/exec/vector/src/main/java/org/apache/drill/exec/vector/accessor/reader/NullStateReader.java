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
package org.apache.drill.exec.vector.accessor.reader;

import org.apache.drill.exec.vector.accessor.ColumnReaderIndex;

/**
 * Internal mechanism to detect if a value is null. Handles the multiple ways
 * that Drill represents nulls:
 * <ul>
 * <li>Required and repeated modes: value is never null.</li>
 * <li>Optional mode: null state is carried by an associated "bits" vector.</li>
 * <li>Union: null state is carried by <i>both</i> the bits state of
 * the union itself, and the null state of the associated nullable vector.
 * (The union states if the column value itself is null; the vector state is
 * if that value is null, which will occur if either the column is null, or
 * if the type of the column is something other than the type in question.)</li>
 * <li>List, with a single data vector: null state is carried by the list vector
 * and the associated nullable data vector. Presumably the list vector state
 * takes precedence.</li>
 * <li>List, with a union data vector (AKA variant array or union array): the
 * null state is carried by all three of a) the list vector, b) the union
 * vector, and c) the type vectors. Presumably, the list vector state has
 * precedence.</li>
 * </ul>
 * <p>
 * The interface here allows each reader to delegate the null logic to a
 * separate component, keeping the data access portion itself simple.
 * <p>
 * As with all readers, this reader must handle both the single-batch and
 * the hyper-batch cases.
 */

public interface NullStateReader {
  void bindIndex(ColumnReaderIndex rowIndex);
  boolean isNull();
  void bindBuffer();
}
