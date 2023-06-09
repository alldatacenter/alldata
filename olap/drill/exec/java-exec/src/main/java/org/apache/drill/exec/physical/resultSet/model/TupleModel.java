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
package org.apache.drill.exec.physical.resultSet.model;

import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.TupleMetadata;

/**
 * Common interface to access a tuple backed by a vector container or a
 * map vector. Provides a visitor interface to apply tasks such as vector
 * allocation, reader or writer creation, and so on. Allows either static
 * or dynamic vector allocation.
 * <p>
 * The terminology used here:
 * <dl>
 * <dt>Row set</dt>
 * <dd>A collection of rows stored as value vectors. Elsewhere in
 * Drill we call this a "record batch", but that term has been overloaded to
 * mean the runtime implementation of an operator.</dd>
 * <dt>Tuple</dt>
 * <dd>The relational-theory term for a row. Drill maps have a fixed schema.
 * Impala, Hive and other tools use the term "structure" (or "struct") for
 * what Drill calls a map. A structure is simply a nested tuple, modeled
 * here by the same tuple abstraction used for rows.</dd>
 * <dt>Column</dt>
 * <dd>A column is represented by a vector (which may have internal
 * null-flag or offset vectors.) Maps are a kind of column that has an
 * associated tuple. Because this abstraction models structure, array
 * columns are grouped with single values: the array-ness is just cardinality.</dd>
 * <dt>Visitor</dt>
 * <dd>The visitor abstraction (classic Gang-of-Four pattern) allows adding
 * functionality without complicating the structure classes. Allows the same
 * abstraction to be used for the testing <b>RowSet</b> abstractions and
 * the scan operator "loader" classes.</dd>
 * <dt>Metadata</dt>
 * <dd>Metadata is simply data about data. Here, data about tuples and columns.
 * The column metadata mostly expands on that available in {@link org.apache.drill.exec.record.MaterializedField},
 * but also adds allocation hints.
 * </dl>
 * <p>
 * This abstraction is the physical dual of a {@link VectorContainer}.
 * The vectors are "owned" by
 * the associated container. The structure here simply applies additional
 * metadata and visitor behavior to allow much easier processing that is
 * possible with the raw container structure.
 * <p>
 * A key value of this abstraction is the extended {@link org.apache.drill.exec.record.metadata.TupleSchema}
 * associated with the structure.  Unlike a
 * {@link VectorContainer}, this abstraction keeps the schema in sync
 * with vectors as columns are added.
 * <p>
 * Some future version may wish to merge the two concepts. That way, metadata
 * discovered by one operator will be available to another. Complex recursive
 * functions can be replace by a visitor with the recursion handled inside
 * implementations of this interface.
 * <p>
 * Tuples provide access to columns by both index and name. Both the schema and
 * model classes follow this convention. Compared with the VectorContainer and
 * {@link org.apache.drill.exec.vector.complex.AbstractMapVector} classes, the vector index is a first-class concept:
 * the column model and schema are guaranteed to reside at the same index relative
 * to the enclosing tuple. In addition, name access is efficient using a hash
 * index.
 * <p>
 * Visitor classes are defined by the "simple" (single batch) and "hyper"
 * (multi-batch) implementations to allow vector implementations to work
 * with the specifics of each type of batch.
 */

public interface TupleModel {

  /**
   * Common interface to access a column vector, its metadata, and its
   * tuple definition (for maps.) Provides a visitor interface for common
   * vector tasks.
   */

  public interface ColumnModel {
    ColumnMetadata schema();
    TupleModel mapModel();
  }

  /**
   * Tuple-model interface for the top-level row (tuple) structure.
   * Provides access to the {@link VectorContainer} representation of the
   * row set (record batch.)
   */

  public interface RowSetModel extends TupleModel {
    VectorContainer container();
  }

  TupleMetadata schema();
  int size();
  ColumnModel column(int index);
  ColumnModel column(String name);
}
