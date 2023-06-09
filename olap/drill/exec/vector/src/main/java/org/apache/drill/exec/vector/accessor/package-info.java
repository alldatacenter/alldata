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
 * Provides a light-weight, simplified set of column readers and writers that
 * can be plugged into a variety of row-level readers and writers. The classes
 * and interfaces here form a framework for accessing rows and columns, but do
 * not provide the code to build accessors for a given row batch. This code is
 * meant to be generic, but the first (and, thus far, only) use is with the test
 * framework for the java-exec project. That one implementation is specific to
 * unit tests, but the accessor framework could easily be used for other
 * purposes as well.
 *
 * <h4>Vector Overflow Handling</h4>
 *
 * The writers provide integrated support for detecting and handling vector
 * overflow. Overflow occurs when a value exceeds some maximum, such as the
 * 16MB block size in Netty. Overflow handling consists of replacing the
 * "full" vector with a new, empty vector as part of a new batch. Overflow
 * handing code must copy partially written values from the "overflow" row
 * to the new vectors. The classes here do not provide overflow handling,
 * rather they provide the framework on top of which overflow handling can be
 * built by a higher level of abstraction.
 *
 * <h4>JSON-Like Model</h4>
 *
 * The object reader and writer provide a generic, JSON-like interface
 * to allow any valid combination of readers or writers (generically
 * accessors):<pre><code>
 * row : tuple
 * tuple : (name column) *
 * column : scalar obj | array obj | tuple obj
 * scalar obj : scalar accessor
 * array obj : array accessor
 * array accessor : element accessor
 * tuple obj : tuple</code></pre>
 * <p>
 * As seen above, the accessor tree starts with a tuple (a row in the form of
 * a class provided by the consumer.) Each column in the tuple is represented
 * by an object accesor. That object accessor contains a scalar, tuple or array
 * accessor. This models Drill's JSON structure: a row can have a list of lists
 * of tuples that contains lists of ints, say.
 *
 * <h4>Comparison with Previous Vector Readers and Writers</h4>
 *
 * Drill provides a set of vector readers and writers. Compared to those, this
 * set:
 * <ul>
 * <li>Works with all Drill data types. The other set works only with repeated
 * and nullable types.</li>
 * <li>Is a generic interface. The other set is bound tightly to the
 * {@link ScanBatch} class.</li>
 * <li>Uses generic types such as <tt>getInt()</tt> for most numeric types. The
 * other set has accessors specific to each of the ~30 data types which Drill
 * supports.</li>
 * </ul>
 * The key difference is that this set is designed for both developer ease-of-use
 * and performance. Developer eas-of-use is a
 * primary requirement for unit tests. Performance is critical for production
 * code. The other set is designed to be used in
 * machine-generated or write-once code and so can be much more complex.
 *
 * <h4>Overview of the Code Structure</h4>
 *
 * {@link ScalarReader} and {@link ColumnWriter} are the core abstractions: they
 * provide simplified access to the myriad of Drill column types via a
 * simplified, uniform API. {@link TupleReader} and {@link TupleWriter} provide
 * a simplified API to rows or maps (both of which are tuples in Drill.)
 * {@link AccessorUtilities} provides a number of data conversion tools.
 * <dl>
 * <dt>ObjectWriter, ObjectReader</dt>
 * <dd>Drill follows a JSON data model. A row is a tuple (AKA structure). Each
 * column is a scalar, a map (AKA tuple, structure) or an array (AKA a repeated
 * value.)</dd>
 * <dt>TupleWriter, TupleReader</dt>
 * <dd>In relational terms, a tuple is an ordered collection of values, where
 * the meaning of the order is provided by a schema (usually a name/type pair.)
 * It turns out that Drill rows and maps are both tuples. The tuple classes
 * provide the means to work with a tuple: get the schema, get a column by name
 * or by position. Note that Drill code normally references columns by name.
 * But, doing so is slower than access by position (index). To provide efficient
 * code, the tuple classes assume that the implementation imposes a column
 * ordering which can be exposed via the indexes.</dd>
 * <dt>ScalarWriter, ScalarReader</dt>
 * <dd>A uniform interface for the scalar types: Nullable (Drill optional) and
 * non-nullable (Drill required) fields use the same interface. Arrays (Drill
 * repeated) are special. To handle the array aspect, even array fields use the
 * same interface, but the <tt>getArray</tt> method returns another layer of
 * accessor (writer or reader) specific for arrays.
 * <p>
 * Both the column reader and writer use a reduced set of data types to access
 * values. Drill provides about 38 different types, but they can be mapped to a
 * smaller set for programmatic access. For example, the signed byte, short,
 * int; and the unsigned 8-bit, and 16-bit values can all be mapped to ints for
 * get/set. The result is a much simpler set of get/set methods compared to the
 * underlying set of vector types.</dt>
 * <dt>ArrayWriter, ArrayReader
 * <dt>
 * <dd>The interface for the array accessors as described above. Of particular
 * note is the difference in the form of the methods. The writer has only a
 * <tt>setInt()</tt> method, no index. The methods assume write-only, write-once
 * semantics: each set adds a new value. The reader, by contrast has a
 * <tt>getInt(int index)</tt> method: read access is random.</tt>
 * <dt>ScalarWriter<dt>
 * <dd>Because of the form of the array writer, both the array writer and
 * column writer have the same method signatures. To avoid repeating these
 * methods, they are factored out into the common <tt>ScalarWriter</tt>
 * interface.</dd>
 * <dt>ColumnAccessors (templates)</dt>
 * <dd>The Freemarker-based template used to generate the actual accessor
 * implementations.</dd>
 * <dt>ColumnAccessors (accessors)</dt>
 * <dd>The generated accessors: one for each combination of write/read, data
 * (minor) type and cardinality (data model).
 * <dd>
 * <dt>ColumnReaderIndex, ColumnWriterIndex</dt>
 * <dd>This nested class binds the accessor to the current row position for the
 * entire record batch. That is, you don't ask for the value of column a for row
 * 5, then the value of column b for row 5, etc. as with the "raw" vectors.
 * Instead, the implementation sets the row position (with, say an iterator.)
 * Then, all columns implicitly return values for the current row.
 * <p>
 * Different implementations of the row index handle the case of no selection
 * vector, a selection vector 2, or a selection vector 4.</dd>
 * <dt>VectorAccessor</dt>
 * <dd>The readers can work with single batches or "hyper"
 * batches. A hyper batch occurs in operators such as sort where an operator
 * references a collection of batches as if they were one huge batch. In this
 * case, each column consists of a "stack" of vectors. The vector accessor picks
 * out one vector from the stack for each row. Vector accessors are used only
 * for hyper batches; single batches work directly with the corresponding
 * vector.
 * <p>
 * You can think of the (row index + vector accessor, column index) as forming a
 * coordinate pair. The row index provides the y index (vertical position along
 * the rows.) The vector accessor maps the row position to a vector when needed.
 * The column index picks out the x coordinate (horizontal position along the
 * columns.)</dt>
 * </dl>
 * <h4>Column Writer Optimizations</h4>
 * The writer classes here started as a simple abstraction on top of the existing
 * vector mutators. The classes were then recruited for use in a new writer
 * abstraction for Drill's record readers. At that point, performance became
 * critical. The key to performance is to bypass the vector and the mutator and
 * instead work with the Netty direct memory functions. This seems a risky
 * approach until we realize that the writers form a very clear interface:
 * the same interface supported the original mutator-based implementation and
 * the revised Netty-based implementation. The benefit, however, is stark;
 * the direct-to-Netty version is up to 4x faster (for repeated types).
 *
 * <h4>Tuple Model</h4>
 *
 * Drill has the idea of row and of a map. (A Drill map is much like a "struct":
 * every instance of the "map" must have the same columns.) Both are instances
 * of the relational concept of a "tuple." In relational theory, a tuple is
 * a collection of values in which each value has a name and a position. The
 * name is for the user, the position (index) allows efficient implementation.
 * <p>
 * Drill is unusual among query and DB engines in that it does not normally
 * use indexes. The reason is easy to understand. Suppose two files contain
 * columns a and b. File 1, read by minor fragment 0, contains the columns in
 * the order (a, b). But, file 2, read by minor fragment 1, contains the columns
 * in the order (b, a). Drill considers this the same schema. Since column
 * order can vary, Drill has avoided depending on column order. (But, only
 * partly; many bugs have cropped up because some parts of the code do
 * require common ordering.)
 * <p>
 * Here we observe that column order varies only across fragments. We have
 * control of the column order within our own fragment. (We can coerce varying
 * order into a desired order. If the above two files are read by the same
 * scan operator, then the first file sets the order at (a, b), and the second
 * files (b, a) order can be coerced into the (a, b) order.
 * <p>
 * Given this insight, the readers and writers here promote position to a
 * first-class concept. Code can access columns by name (for convenience,
 * especially in testing) or by position (for efficiency.)
 * <p>
 * Further, it is often convenient to fetch a column accessor (reader or
 * writer) once, then cache it. The design here ensures that such caching works
 * well. The goal is that, eventually, operators will code-generate references
 * to cached readers and writers instead of generating code that works directly
 * with the vectors.
 *
 * <h4>Lists and Unions</h4>
 *
 * Drill provides a List and a Union type. These types are incomplete, buggy
 * and ill-supported by Drill's operators. However, they are key to Drill's
 * JSON-powered, schema-free marketing message. Thus, we must support them
 * in the reader/writer level even if they are broken and under-used elsewhere
 * in Drill. (If we did not support them, then the JSON readers could not use
 * the new model, and we'd have to support both the old and new versions, which
 * would create a bigger mess than we started with.)
 * <p>
 * Drill's other types have a more-or-less simple mapping to the relational
 * model, allowing simple reader and writer interfaces. But, the Union and List
 * types are not a good fit and cause a very large amount of complexity in the
 * reader and writer models.
 * <p>
 * A Union is just that: it is a container for a variety of typed vectors. It
 * is like a "union" in C: it has members for each type, but only one type is
 * in use at any one time. However, unlike C, the implementation is more like
 * a C "struct" every vector takes space or every row, even if no value is stored
 * in that row. That is, a Drill union is as if a naive C programmer used a
 * "struct" when s/he should have used a union.
 * <p>
 * Unions are designed to evolve dynamically as data is read. Suppose we read
 * the following JSON:<pre></code>
 * {a: 10} {a: "foo"} {a: null} {a: 12.34}
 * </code></pre>
 * Here, we discover the need for an Int type, then a Varchar, then mark a
 * value as null and finally a Float. The union adds the desired types as we
 * request them. The writer mimics this behavior, using a listener to do the
 * needed vector work.
 * <p>
 * Further, a union can be null. It carries a types vector that indicates the
 * type of each row. A zero-value indicates that the union as a whole is null.
 * In this case, null means no value, is is not, say, a null Int or null
 * Varchar: it is simply null (as in JSON). Since at most one vector within the union
 * carries a value, the element vectors must also be nullable. This means
 * that a union has two null bits: one or the union, the other for the
 * selected type. It is not clear what Drill semantics are supposed to be. Here
 * the writers assume that either the whole union is null, or that exactly one
 * member is non-null. Readers are more paranoid: they assume each member is null
 * if either the union is null or the member itself is null. (Yes, a bit of a
 * mess...)
 * <p>
 * The current union vector format is highly inefficient.
 * If the union concept is needed, then it should
 * be redesigned, perhaps as a variable-width vector in which each entry
 * consists of a type/value pair. (For variable-width values such as
 * strings, the implementation would be a triple of (type, length,
 * value). The API here is designed to abstract away the implementation
 * and should work equally well for the current "union" implementation and
 * the possible "variant" implementation. As a result, when changing the
 * API, avoid introducing methods that assume an implementation.
 * <p>
 * Lists add another layer of complexity. A list is, logically, a repeated
 * union. But, for whatever historical reasons, a List can be other things
 * as well. First, it can have no type at all: a list of nothing. This likely
 * has no meaning, but the semantics of the List vector allow it. Second, the
 * List can be an array of a single type in which each entry can be null.
 * (Normal Repeated types can have an empty array for a row, but cannot have
 * a null entry. Lists can have either an empty array or a null array in
 * order to model the JSON <tt>null</tt> and <tt>[]</tt> cases.)
 * <p>
 * When a List has a single type, it stores the backing vector directly within
 * the List. But, a list can also be a list of unions. In this case, the List
 * stores a union vector as its backing vector. Here, we now have three ways
 * to indicate null: the List's bits vector, the type vector in the union, and
 * the bits vector in each element vector. Again, the writer assumes that
 * if the List vector is null, the entire value for that row is null. The reader
 * is again paranoid and handles all three nullable states. (Again, a huge
 * mess.)
 * <p>
 * The readers can provide a nice API for these cases since we know the List
 * format up front. They can present the list as either a nullable array of
 * a single type, or as an array of unions.
 * <p>
 * Writers have more of a challenge. If we knew that a List was being used as
 * a list of, say, Nullable Int, we could present the List as an array writer
 * with Int elements. But, the List allows dynamic type addition, as with unions.
 * (In the case of the list, it has internal special handling for the single vs.
 * many type case.)
 * <p>
 * To isolate the client from the list representation, it is simpler to always
 * present a List an array of variants. But, this is awkward in the single-type
 * case. The solution is to use metadata. If the metadata promises to use only
 * a single type, the writer can use the nullable array of X format. If the
 * metadata says to use a union (the default), then the List is presented as
 * an array of unions, even when the list has 0 or 1 member types. (The
 * complexity here is excessive: Drill should really redesign this feature to make
 * it simpler and to better fit the relational model.)
 *
 * <h4>Vector Evolution</h4>
 *
 * Drill uses value vector classes created during the rush to ship Drill 1.0.
 * They are not optimal: the key value is that the vectors work.
 * <p>
 * The Apache Arrow project created a refined version of the vector classes.
 * Much talk has occurred about ripping out Drill's implementation to use
 * Arrow instead.
 * <p>
 * However, even Arrow has limits:
 * <ul>
 * <li>Like Drill, it uses twice the number of positions in the offset vector
 * as for the values vector. (Drill allocates power-of-two sizes. The offset
 * vector has one more entry than values. With a power-of-two number of values,
 * offsets are rounded to the next power of two.)</li>
 * <li>Like Drill before this work, Arrow does not manage vector sizes; it
 * allows vectors to grow without bound, causing the memory problems that this
 * project seeks to resolve.</li>
 * <li>Like Drill, Arrow implements unions as a space-inefficient collection
 * of vectors format.</li>
 * </ul>
 * If we learn from the above, we might want to create a Value Vectors 2.0
 * based on the following concepts:
 * <ul>
 * <li>Store vector values as a chain of fixed-size buffers. This avoids
 * memory fragmentation, makes memory allocation much more efficient, is
 * easier on the client, and avoids internal fragmentation.</li>
 * <li>Store offsets as the end value, not the start value. This eliminates
 * the extra offset position, simplifies indexing, and can save on internal
 * memory fragmentation.</li>
 * <li>Store unions using "variant encoding" as described above.</li>
 * </ul>
 * Such changes would be a huge project if every operator continued to work
 * directly with vectors and memory buffers. In fact, the cost would be so
 * high that these improvements might never be done.
 * <p>
 * Therefore, a goal of this reader/writer layer is to isolate the operators
 * from vector implementation. For this to work, the accessors must be at least
 * as efficient as direct vector access. (They are now more efficient.)
 * <p>
 * Once all operators use this layer, a switch to Arrow, or an evolution toward
 * Value Vectors 2.0 will be much easier. Simply change the vector format and
 * update the reader and writer implementations. The rest of the code will
 * remain unchanged. (Note, to achieve this goal, it is important to carefully
 * design the accessor API [interfaces] to hide implementation details.)
 *
 * <h4>Simpler Reader API</h4>
 *
 * A key value of Drill is the ability for users to add custom record readers.
 * But, at present, doing so is very complex because the developer must know
 * quite a bit about Drill internals. At this level, they must know how to
 * allocate vectors, how to write to each kind of vector, how to keep track
 * of array sizes, how to set the vector and batch row counts, and more. In
 * general, there is only one right way to do this work. (Though some readers
 * use the old-style vector writers, others work with direct memory instead
 * of with vectors, and so on.)
 * <p>
 * This layer handles all that work, providing a simple API that encourages
 * more custom readers because the work to create the readers becomes far
 * simpler. (Other layers tackle other parts of the problem as well.)
 */

package org.apache.drill.exec.vector.accessor;
