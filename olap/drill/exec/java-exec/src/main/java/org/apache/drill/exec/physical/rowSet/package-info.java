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
 * Provides a set of tools to work with row sets.
 * A row set is a batch of Drill vectors,
 * often called a "record batch." However, a record batch, in Drill, means
 * not just the data, but also an operator on that data. The classes
 * here work with the data itself, and can be used to test implementations
 * of things such as code generated classes and so on.
 * <p>
 * The classes include tools for reading and writing row sets, comparing
 * actual and expected results, and so on.
 * <p>
 * Drill defines a variety of record batch semantics, modeled here as
 * distinct row set classes:
 * <dl>
 * <dt>RowSet</dt>
 * <dd>The abstract definition of a row set that defines operations available
 * on all row sets.</dd>
 * <dt>SingleRowSet (abstract)</dt>
 * <dd>Represents a row set that contains a single record batch (the typical
 * case.</dd>
 * <dt>DirectRowSet</dt>
 * <dd>A read-only single row set without a selection vector.</dd>
 * <dt>IndirectRowSet</dt>
 * <dd>A read-only, single row set with an SV2. Note that the SV2 itself is
 * writable (such as for sorting.)</dd>
 * <dt>ExtendableRowSet</dt>
 * <dd>A write-only, single row set used to create a new row set. Because of
 * the way Drill sets row counts, an extendable row set cannot be read; instead
 * at the completion of the write the extendable row set becomes a direct or
 * indirect row set.</dd>
 * <dt>HyperRowSet</dt>
 * <dd>A read-only row set made up of a collection of record batches, indexed via an SV4.</dt>
 * </dl>
 * This package contains a number of helper classes:
 * <dl>
 * <dt>{@link org.apache.drill.exec.physical.rowSet.RowSetWriter}</dt>
 * <dd>Writes data into an extendable row set.</dd>
 * <dt>{@link org.apache.drill.exec.physical.rowSet.RowSetReader}</dt>
 * <dd>Reads data from any but an extendable row set.</dd>
 * <dt>{@link org.apache.drill.exec.physical.rowSet.RowSetFormatter}</dt>
 * <dd>Prints a row set to stdout in a CSV-like form for easy debugging.</dd>
 * <dt>{@link org.apache.drill.exec.physical.rowSet.RowSetBuilder}</dt>
 * <dd>Creates and populates a row set in a fluent builder style.</dd>
 * </dl>
 */
package org.apache.drill.exec.physical.rowSet;
