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
/**
 * The "row set model" provides a "dual" of the vector structure used to create,
 * allocate and work with a collection of vectors. The model provides an enhanced
 * "metadata" schema, given by {@link TupleMetadata} and {@link ColumnMetadata},
 * with allocation hints that goes beyond the {@link MaterializedField}
 * used by value vectors.
 * <p>
 * In an ideal world, this structure would not be necessary; the vectors could, by
 * themselves, provide the needed structure. However, vectors are used in many
 * places, in many ways, and are hard to evolve. Further, Drill may eventually
 * choose to move to Arrow, which would not have the structure provided here.
 * <p>
 * A set of visitor classes provide the logic to traverse the vector structure,
 * avoiding the need for multiple implementations of vector traversal. (Traversal
 * is needed because maps contain vectors, some of which can be maps, resulting
 * in a tree structure. Further, the API provided by containers (a top-level
 * tuple) differs from that of a map vector (nested tuple.) This structure provides
 * a uniform API for both cases.
 * <p>
 * Three primary tasks provided by this structure are:
 * <ol>
 * <li>Create writers for a set of vectors. Allow incremental write-time
 * addition of columns, keeping the vectors, columns and metadata all in
 * sync.</li>
 * <li>Create readers for a set of vectors. Vectors are immutable once written,
 * so the reader mechanism does not provide any dynamic schema change
 * support.</li>
 * <li>Allocate vectors based on metadata provided. Allocation metadata
 * includes estimated widths for variable-width columns and estimated
 * cardinality for array columns.</li>
 * </ol>
 * <p>
 * Drill supports two kinds of batches, reflected by two implementations of
 * the structure:
 * <dl>
 * <dt>Single batch</dt>
 * <dd>Represents a single batch in which each column is backed by a single
 * value vector. Single batches support both reading and writing. Writing can
 * be done only for "new" batches; reading can be done only after writing
 * is complete. Modeled by the {#link org.apache.drill.exec.physical.rowSet.model.single
 * single} package.</dd>
 * <dt>Hyper batch</dt>
 * <dd>Represents a stacked set of batches in which each column is backed
 * by a list of columns. A hyper batch is indexed by an "sv4" (four-byte
 * selection vector.) A hyper batch allows only reading. Modeled by the
 * {@link org.apache.drill.exec.physical.resultSet.model.hyper hyper} package.</dd>
 * </dl>
 */

package org.apache.drill.exec.physical.resultSet.model;