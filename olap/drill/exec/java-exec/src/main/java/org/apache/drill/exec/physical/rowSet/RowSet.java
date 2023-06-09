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
package org.apache.drill.exec.physical.rowSet;

import java.util.Set;

import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.record.BatchSchema.SelectionVectorMode;
import org.apache.drill.exec.record.VectorAccessible;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.record.selection.SelectionVector2;
import org.apache.drill.exec.record.selection.SelectionVector4;
import org.apache.drill.shaded.guava.com.google.common.annotations.VisibleForTesting;

/**
 * A row set is a collection of rows stored as value vectors. Elsewhere in
 * Drill we call this a "record batch", but that term has been overloaded to
 * mean the runtime implementation of an operator.
 * <p>
 * A row set encapsulates a set of vectors and provides access to Drill's
 * various "views" of vectors: {@link VectorContainer},
 * {@link VectorAccessible}, etc. The row set wraps a {#link TupleModel}
 * which holds the vectors and column metadata. This form is optimized
 * for easy use in testing; use other implementations for production code.
 * <p>
 * A row set is defined by a {@link TupleMetadata}. For testing purposes, a row
 * set has a fixed schema; we don't allow changing the set of vectors
 * dynamically.
 * <p>
 * The row set also provides a simple way to write and read records using the
 * {@link RowSetWriter} and {@link RowSetReader} interfaces. As per Drill
 * conventions, a row set can be written (once), read many times, and finally
 * cleared.
 * <p>
 * Drill provides a large number of vector (data) types. Each requires a
 * type-specific way to set data. The row set writer uses a
 * {@link org.apache.drill.exec.vector.accessor.ColumnWriter}
 * to set each value in a way unique to the specific data type. Similarly, the
 * row set reader provides a {@link org.apache.drill.exec.vector.accessor.ScalarReader}
 * interface. In both cases, columns can be accessed by index number
 * (as defined in the schema) or by name.
 * <p>
 * A row set follows a schema. The schema starts as a
 * {@link BatchSchema}, but is parsed and restructured into a variety of
 * forms. In the original form, maps contain their value vectors. In the
 * flattened form, all vectors for all maps (and the top-level tuple) are
 * collected into a single structure. Since this structure is for testing,
 * this somewhat-static structure works just file; we don't need the added
 * complexity that comes from building the schema and data dynamically.
 * <p>
 * Putting this all together, the typical life-cycle flow is:
 * <ul>
 * <li>Define the schema using {@link org.apache.drill.exec.record.metadata.SchemaBuilder}.</li>
 * <li>Create the row set from the schema.</li>
 * <li>Populate the row set using a writer from {@link ExtendableRowSet#writer(int)}.</li>
 * <li>Process the vector container using the code under test.</li>
 * <li>Retrieve the results using a reader from {@link #reader()}.</li>
 * <li>Dispose of vector memory with {@link #clear()}.</li>
 * </ul>
 */

public interface RowSet {

  boolean isExtendable();

  boolean isWritable();

  VectorAccessible vectorAccessible();

  VectorContainer container();

  int rowCount();

  RowSetReader reader();

  void clear();

  TupleMetadata schema();

  BufferAllocator allocator();

  SelectionVectorMode indirectionType();

  /**
   * Debug-only tool to visualize a row set for inspection.
   * <b>Do not</b> use this in production code.
   */
  @VisibleForTesting
  void print();

  /**
   * Return the size in memory of this record set, including indirection
   * vectors, null vectors, offset vectors and the entire (used and unused)
   * data vectors.
   *
   * @return memory size in bytes
   */

  long size();

  BatchSchema batchSchema();

  /**
   * Row set that manages a single batch of rows.
   */

  interface SingleRowSet extends RowSet {
    SingleRowSet toIndirect();
    SingleRowSet toIndirect(Set<Integer> skipIndices);
    SelectionVector2 getSv2();
  }

  /**
   * Single row set which is empty and allows writing.
   * Once writing is complete, the row set becomes an
   * immutable direct row set.
   */

  interface ExtendableRowSet extends SingleRowSet {
    void allocate(int recordCount);
    RowSetWriter writer();
    RowSetWriter writer(int initialRowCount);
  }

  /**
   * Row set comprised of multiple single row sets, along with
   * an indirection vector (SV4).
   */

  interface HyperRowSet extends RowSet {
    SelectionVector4 getSv4();
  }

  interface HyperRowSetBuilder {
    void addBatch(SingleRowSet rowSet);
    void addBatch(VectorContainer container);
    HyperRowSet build() throws SchemaChangeException;
  }
}
