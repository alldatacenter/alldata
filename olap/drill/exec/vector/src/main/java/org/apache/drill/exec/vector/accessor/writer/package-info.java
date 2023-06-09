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
 * Implementation of the vector writers. The code will make much more sense if
 * we start with a review of Drill’s complex vector data model. Drill has 38+
 * data ("minor") types. Drill also has three cardinalities ("modes"). The
 * result is over 120+ different vector types. Then, when you add maps, repeated
 * maps, lists and repeated lists, you rapidly get an explosion of types that
 * the writer code must handle.
 *
 * <h4>Understanding the Vector Model</h4>
 *
 * Vectors can be categorized along multiple dimensions:
 * <ul>
 * <li>By data (minor) type</li>
 * <li>By cardinality (mode)</li>
 * <li>By fixed or variable width</li>
 * <li>By repeat levels</li>
 * </ul>
 * <p>
 * A repeated map, a list, a repeated list and any array (repeated) scalar all
 * are array-like. Nullable and required modes are identical (single values),
 * but a nullable has an additional is-set ("bit") vector.
 * <p>
 * The writers (and readers) borrow concepts from JSON and relational theory
 * to simplify the problem:
 * <p>
 * <ul>
 * <li>Both the top-level row, and a Drill map are "tuples" and are treated
 * similarly in the model.</li>
 * <li>All non-map, non-list (that is, scalar) data types are treated
 * uniformly.</li>
 * <li>All arrays (whether a list, a repeated list, a repeated map, or a
 * repeated scalar) are treated uniformly.</li>
 * </ul>
 *
 * <h4>Repeat Levels</h4>
 *
 * JSON and Parquet can be understood as a series of one or more "repeat
 * levels." First, let's identify the repeat levels above the batch
 * level:
 * <ul>
 * <li>The top-most level is the "result set": the entire collection of
 * rows that come from a file (or other data source.)</li>
 * <li>Result sets are divided into batches: collections of up to 64K
 * rows.</li>
 * </ul>
 *
 * Then, within a batch:
 * <ul>
 * <li>Each batch is a collection or rows. A batch-level index points
 * to the current row.</li>
 * </ul>Scalar arrays introduce a repeat level: each row has 0, 1 or
 * many elements in the array-valued column. An offset vector indexes
 * to the first value for each row. Each scalar array has its own
 * per-array index to point to the next write position.</li>
 * <li>Map arrays introduce a repeat level for a group of columns
 * (those that make up the map.) A single offset vector points to
 * the common start position for the columns. A common index points
 * to the common next write position.<li>
 * <li>Lists also introduce a repeat level. (Details to be worked
 * out.</li>
 * </ul>
 *
 * For repeated vectors, one can think of the structure either top-down
 * or bottom-up:
 * <ul>
 * <li>Top down: the row position points into an offset vector. The
 * offset vector value points to either the data value, or into another
 * offset vector.</li>
 * <li>Bottom-up: values are appended to the end of the vector. Values
 * are "pinched off" to form an array (for repeated maps) or for a row.
 * In this view, indexes bubble upward. The inner-most last write position
 * is written as the array end position in the enclosing offset vector.
 * This may occur up several levels.</li>
 * </ul>
 *
 * <h4>Writer Data Model</h4>
 *
 * The above leads to a very simple, JSON-like data model:
 * <ul>
 * <li>A tuple reader or writer models a row. (Usually via a subclass.) Column
 * are accessible by name or position.</li>
 * <li>Every column is modeled as an object.</li>
 * <li>The object can have an object type: scalar, tuple or array.</li>
 * <li>An array has a single element type (but many run-time elements)</li>
 * <li>A scalar can be nullable or not, and provides a uniform get/set
 * interface.</li>
 * </ul>
 * <p>
 * This data model is similar to; but has important differences from, the prior,
 * generated, readers and writers. This version is based on the concept of
 * minimizing the number of writer classes, and leveraging Java primitives to
 * keep the number of get/set methods to a reasonable size. This version also
 * automates vector allocation, vector overflow and so on.
 * <p>
 * The object layer is new: it is the simplest way to model the three "object
 * types." An app using this code would use just the leaf scalar readers and
 * writers.
 * <p>
 * Similarly, the {@link ColumnWriter} interface provides a uniform way to
 * access services common to all writer types, while allowing each JSON-like
 * writer to provide type-specific ways to access data.
 *
 * <h4>Writer Performance</h4>
 *
 * To maximize performance, have a single version for all "data modes":
 * (nullable, required, repeated). Some items of note:
 * <ul>
 * <li>The writers bypass DrillBuf and the UDLE to needed writes to direct
 * memory.</li>
 * <li>The writers buffer the buffer address and implement a number of methods
 * to synchronize that address when the buffer changes (on a new batch or during
 * vector resize).</li>
 * <li>Writing require a single bounds check. In most cases, the write is within
 * bounds so the single check is all that is needed.</li>
 * <li>If the write is out of bounds, then the writer determines the new vector
 * size and performs the needed reallocation. To avoid multiple doublings, the
 * writer computes the needed new size and allocates that size directly.</li>
 * <li>Vector reallocation is improved to eliminate zeroing the new half of the
 * buffer, data is left "garbage-filled."</li>
 * <li>If the vector would grow beyond 16 MB, then overflow is triggered, via a
 * listener, which causes the buffer to be replaced. The write then
 * continues.</li>
 * <li>Offset vector updates are integrated into the writers using an
 * `OffsetVectorWriter`. This writer caches the last write position so that each
 * array write needs a single offset update, rather than the read and write as
 * in previous code.</li>
 * <li>The writers keep track of the "last write position" and perform
 * "fill-empties" work if the new write position is more than one position
 * behind the last write. All types now correctly support "fill-empties"
 * (before, only nullable types did so reliably.)</li>
 * <li>Null handling is done by an additional writer layer that wraps the
 * underlying data writer. This avoids the need for a special nullable writer:
 * the same nullable layer works for all data types.</li>
 * <li>Array handling is done similarly: an array writer manages the offset
 * vector and works the same for repeated scalars, repeated maps and
 * (eventually) lists and repeated lists.</li>
 * </ul>
 *
 * <h4>Lists</h4>
 *
 * As described in the API package, Lists and Unions in Drill are highly
 * complex, and not well supported. This creates huge problems in the
 * writer layer because we must support something which is broken and
 * under-used, but which most people assume works (it is part of Drill's
 * JSON-like, schema-free model.) Our goal here is to support Union and
 * List well enough that nothing new is broken; though this layer cannot
 * fix the issues elsewhere in Drill.
 * <p>
 * The most complex part is List support for the transition from a single
 * type to a union of types. The API should be simple: the client should
 * not have to be aware of the transition.
 * <p>
 * To make this work, the writers provide two options:
 * <ol>
 * <li>Use metadata to state that a List will have exactly one type and
 * to specify that type. The List will present as an array of that type in
 * which each array can be null.</li>
 * <li>Otherwise, the list is repeated union (a array of variants), even
 * if the list happens to have 0 or 1 types. In this case, the list presents
 * as an array of variants.</li>
 * </ol>
 * The result is that client code assumes one or the other model, and never
 * has to worry about transitioning from one to the other within a single
 * operator.
 * <p>
 * The {@link PromotableListWriter} handles the complex details of providing
 * the above simple API in the array-of-variant case.
 *
 * <h4>Possible Improvements</h4>
 *
 * The code here works and has extensive unit tests. But, many improvements
 * are possible:
 * <ul>
 * <li>Drill has four "container" vector types (Map, Union, List, Repeated
 * List), each with its own quirky semantics. The code could be far simpler
 * if the container semantics were unified.</li>
 * <li>Similarly, the corresponding writers implement some very awkward
 * logic to handle union and list containers. But, these vectors are not
 * fully supported in Drill. This means that the code implements (and tests)
 * many odd cases which no one may ever use. Better to limit the data types
 * that Drill supports, implement those well, and deprecate the obscure
 * cases.</li>
 * <li>The same schema-parsing logic appears over and over in different
 * guises. Some parse vectors, some parse batch schemas, others parse the
 * column metadata (which provides information not available in the
 * materialized field) and so on. Would be great to find some common way
 * to do this work, perhaps in the form of a visitor. An earlier version of
 * this code tried using visitors. But, since each representation has its
 * own quirks, that approach didn't work out. A first step would be to come
 * up with a standard schema description which can be used in all cases,
 * then build a visitor on that.</li>
 * <li>Many tests exist. But, the careful reader will note that Drill's
 * vectors define a potentially very complex recursive structure (most
 * variations of which are never used.) Additional testing should cover
 * all cases, such as repeated lists that contain unions, or unions that
 * contain repeated lists of tuples.</li>
 * <li>Experience with the code may show additional redundancies that can
 * be removed. Each fresh set of eyes may see things that prior folks
 * missed.</li>
 * </ul>
 *
 * <h4>Caveats</h4>
 *
 * The column accessors are divided into two packages: <tt>vector</tt> and
 * <tt>java-exec</tt>. It is easy to add functionality in the wrong place,
 * breaking abstraction and encapsulation. Here are some general guidelines:
 * <ul>
 * <li>The core reader and writer logic is implemented in this <tt>vector</tt>
 * package. This package provides low-level tools to build accessors, but
 * not the construction logic itself. (That is found in the <tt>java-exec</tt>
 * layer.)</li>
 * <li>The vector layer is designed to support both the simple "row set" and
 * the more complex "result set loader" implementations.</li>
 * <li>The "row set" layer wraps the accessors in tools that work on one batch
 * (row set) at a time, without regard for schema versions, schema changes
 * and the like. The row set layer is primarily for testing: building an input
 * batch for some operator, and verifying an output batch. It also serves as a
 * simple test framework for the accessors, without the complexity of other
 * layers.</li>
 * <li>The "result set loader" layer handles the complexity of dynamic schema
 * evolution, schema versioning, vector overflow and projection. It provides
 * an "industrial strength" version of the accessor mechanism intended for
 * use in the scan operator (but which can be generalized for other operators.)
 * </li>
 * <li>The "listener" pattern is used to allow higher levels to "plug in"
 * functionality to the accessor layer. This is especially true for schema
 * evolution: listeners notify the higher layer to add a column, delegating
 * the actual work to that higher layer.</li>
 * </ul>
 *
 * Given all this, plan carefully where to make any improvement. If your change
 * violates the dependencies below, perhaps reconsider another way to do the
 * change.
 * <code><pre>
 *                                                  +------------+
 *                      +-------------------------- | Result Set |
 *                      v                           |   Loader   |
 *             +----------------+     +---------+   +------------+
 *             |    Metadata    | <-- | Row Set |     |
 *             | Implementation |     |  Tools  |     |
 *             +----------------+     +---------+     |
 * java-exec           |                     |        |
 * ------------------- | ------------------- | ------ | ------------
 * vector              v                     v        v
 *               +------------+            +-----------+
 *               | Metadata   | <--------- |  Column   |
 *               | Interfaces |            | Accessors |
 *               +------------+            +-----------+
 *                                               |
 *                                               v
 *                                          +---------+
 *                                          |  Value  |
 *                                          | Vectors |
 *                                          +---------+
 * </pre></code>
 */

package org.apache.drill.exec.vector.accessor.writer;
