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
 * Provides the reader hierarchy as explained in the API package.
 *
 * <h4>Structure</h4>
 *
 * The reader implementation divides into four parts:
 * <ol>
 * <li>The readers themselves which start with scalar readers to
 * decode data from vectors, then build up to nullable, array,
 * union and list readers. Readers are built up via composition,
 * often using the (internal) offset vector reader.</li>
 * <li>The column index abstraction that steps through items in a collection.
 * At the top level, the index points to the current row. The top level
 * may include an indirection (an SV2 or SV4) which is handled by the
 * column index. Within arrays, the column index points to each element
 * of the array.</li>
 * <li>The vector accessor which provides a unified interface for both
 * the single-batch and hyper-batch cases. The single-batch versions
 * simply hold onto the vector itself. The hyper-batch versions either
 * provide access to a specific vector within a hyper-vector (for
 * top-level vectors), or navigate from a top-level vector down to an
 * inner vector (for nested vectors.)</li>
 * <li>The null state abstraction which provides a uniform way to
 * detect nullability. For example, within the reader system, the
 * reader for nullable and required vectors differ only in the associated
 * null state reader. Unions and lists have complex null state
 * logic: the nullability of a value depends on the nullability
 * of the list, the union, and the value itself. The null state
 * class implements this logic independent of the reader structure.
 * </li>
 * </ul>
 *
 * <h4>Composition</h4>
 *
 * The result is that reader structure makes heavy use of composition:
 * readers are built up from each of the above components. The number of
 * actual reader classes is small, but the methods to build the readers are
 * complex. Most structure is built at build time. Indexes, however are
 * provided at a later "bind" time at which a bind call traverses the
 * reader tree to associate an index with each reader and vector accessor.
 * When a reader is for an array, the bind step creates the index for the
 * array elements.
 *
 * <h4>Construction</h4>
 *
 * Construction of readers is a multi-part process.
 * <ul>
 * <li>Start with a single or hyper-vector batch.</li>
 * <li>The reader builders in another package parse the batch structure,
 * create the required metadata, wrap the (single or hyper) vectors in
 * a vector accessor, and call methods in this package.</li>
 * <li>Methods here perform the final construction based on the specific
 * type of the reader.</li>
 * </ul>
 * <p>
 * The work divides into two main categories:
 * <ul>
 * <li>The work which is based on
 * the vector structure and single/hyper-vector structure, which is done
 * elsewhere.</li>
 * <li>The work which is based on the structure of the readers (with
 * vector cardinality factored out), which is done here.</li>
 * </ul>
 */

package org.apache.drill.exec.vector.accessor.reader;