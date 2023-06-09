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
 * The reader structure is heavily recursive. The top-level reader
 * iterates over batches, possibly through an indirection vector
 * (SV2 or SV4.) The row is tuple of top-level vectors. Each top-level
 * vector may be an array. Iteration through the array works identically
 * to iteration over the batch as a whole. (In fact, the scalar readers
 * don't know if they are top-level or array readers.) Array nesting
 * can continue to any level of depth.
 * <p>
 * Further, when used with a logical join, the top-level iteration
 * may be over an array, with an implicit join out to enclosing nesting
 * levels.
 * <p>
 * Because of this, the same index interface must work at all nesting
 * levels: at the top, and within arrays. This interface
 * supports a usage model as follows:<pre><code>
 * ColumnReaderIndex index = ...
 * while (index.hasNext()) {
 *   index.next();
 *   int hyperIndex = index.hyperVectorIndex();
 *   int vectorOffset = index.offset();
 * }</code></pre>
 * <p>
 * When convenient, the following abbreviated form is also
 * supported:<pre><code>
 * ColumnReaderIndex index = ...
 * while (index.next()) {
 *   int hyperIndex = index.hyperVectorIndex();
 *   int vectorOffset = index.offset();
 * }</code></pre>
 * <p>
 * For a top-level index, the check of <tt>hasNext()</tt> and
 * call to <tt>next()</tt> is done by the row set reader. For
 * arrays, the call to <tt>hasNext()</tt> is done by the array
 * reader. The call to <tt>next()</tt> is done by the scalar
 * reader (for scalar arrays) or the array reader (for other
 * arrays.)
 * <p>
 * The hyper-vector index has meaning only for top-level vectors,
 * and is ignored by nested vectors. (Nested vectors work by navigating
 * down from a top-level vector.) But, as noted above, any given
 * reader does not know if it is at the top or nested level, instead
 * it is the {@link org.apache.drill.exec.vector.accessor.reader.VectorAccessor}
 * abstraction that works out the nesting levels.
 */

public interface ColumnReaderIndex {

  /**
   * Ordinal index within the batch or array. Increments from -1.
   * (The position before the first item.)
   * Identifies the logical row number of top-level records,
   * or the array element for arrays. Actual physical
   * index may be different if an indirection layer is in use.
   *
   * @return logical read index
   */

  int logicalIndex();

  /**
   * When used with a hyper-vector (SV4) based batch, returns the
   * index of the current batch within the hyper-batch. If this is
   * a single batch, or a nested index, then always returns 0.
   *
   * @return batch index of the current row within the
   * hyper-batch
   */

  int hyperVectorIndex();

  /**
   * Vector offset to read. For top-level vectors, the offset may be
   * through an indirection (SV2 or SV4). For arrays, the offset is the
   * absolute position, with the vector of the current array element.
   *
   * @return vector read index
   */

  int offset();

  /**
   * Advances the index to the next position. Used:
   * <ul>
   * <li>At the top level for normal readers or</li>
   * <liAt a nested level for implicit join readers, and</li>
   * <li>An each array level to iterate over arrays.</li>
   * </ul>
   *
   * @return true if another value is available, false if EOF
   */

  boolean next();

  /**
   * Reports if the index has another item.
   * @return <true> if more rows remain. That is, if a
   * call to {@link #next()} would return <tt>true</tt>.
   */
  boolean hasNext();

  /**
   * Return the number of items that this index indexes: top-level record
   * count for the root index; total element count for nested arrays.
   *
   * @return element count at this index level
   */

  int size();
}
