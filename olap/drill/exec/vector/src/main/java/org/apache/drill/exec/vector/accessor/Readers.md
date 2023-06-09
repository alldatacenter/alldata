# Column Readers

The column readers implement the patterns described in the
[Column Accessors](Accessors.md) section. Here we look at the specific
details of the readers.

## Construction

At present, the implementation provides only a per-batch reader
(the `RowSetReader`). To construct a reader, we start with an existing batch
(in the form of a row set.) We then do a recursive walk of the vectors,
starting with the vector container. We examine each column, determine its
category (map, scalar, union, etc.) and then build each column accordingly.
Maps, unions, lists and repeated lists are containers: they contain other
columns. So, we continue the recursive descent though these containers into
their columns. The recursion stops when we hit a leaf (a possibly repeated
scalar.)

Then, on the way back up the call stack, we assemble container readers
using the child readers. We continue up until we build the tuple reader
for the whole row.

As if this process were not complex enough, we have to do the work twice:
once for a single batch, and a second way for a hyper batch. In a single
batch, we can just inspect each vector directly to find the children. In a
hyper batch, we have to make one pass over the batches to build up a
combined schema (especially needed for unions), then a second pass to use
the metadata to build the readers.

* Single Batch: [`model.single.BaseReaderBuilder`](https://github.com/apache/drill/blob/master/exec/java-exec/src/main/java/org/apache/drill/exec/physical/rowSet/model/single/BaseReaderBuilder.java)
* Hyper Batch: [`model.hyper.BaseReaderBuilder`](https://github.com/apache/drill/blob/master/exec/java-exec/src/main/java/org/apache/drill/exec/physical/rowSet/model/hyper/BaseReaderBuilder.java)

Scalar readers are generated (see below). The template also generates a class to
create a scalar reader given a type. The leaf reader builder methods call these
generated methods. Then, to build up higher-level methods, we use composition
to, say, build up a scalar array from an array (which handles the offset vector)
and a scalar reader (to read each value.) A considerable amount of code goes into
glueing the bits together.

## Reader Index

The reader must handle three distinct forms of indexing:

* Direct (position x in the reader corresponds to row x in each vector)
* Indirect (using an SV2: position x in the reader maps to `sv2[x]` in the
vector)
* Hyper: (using an SV4: position x in the reader corresponds to s`v4[x].offset`
in batch `sv4[x].batch` of the hyper vector)

The [`ColumnReaderIndex`](https://github.com/apache/drill/blob/master/exec/vector/src/main/java/org/apache/drill/exec/vector/accessor/ColumnReaderIndex.java)
interface defines the common protocol; implementations exist to handle each case.
The result is that reader code is ignorant of indirection: it simply asks the
index which batch and offset to read.

## HyperVectors and the Vector Accessor

Readers would be quite simple if they worked only with a single batch. But,
hyper vectors (hyper batches) add a large amount of complexity. A hyper batch
is an abstraction that makes a collection of batches look like a single batch.
That is, we start with a stack of batches:

```
+---------+
| Batch 1 |
|---------|
| Batch 2 |
|---------|
|   ...   |
|---------|
| Batch N |
+---------+
```

That is pretty simple. But, we want to work with these not as an array of
batches, each with its list of columns, but rather as a list of columns, each
associated with multiple batches.

```
      Hyper Batch
+-----------------------+
|    a      b      c    |
|  +---+  +---+  +---+  |
|  | 1 |  | a |  | x |  | Batch 1
|  | . |  | . |  | . |  |
|  - - -  - - -  - - -  |
|  | 8 |  | c |  | z |  | Batch 2
|  | . |  | . |  | . |  |
|  - - -  - - -  - - -  |
|  | 3 |  | e |  | w |  | Batch 3
|  | . |  | . |  | . |  |
|  +---+  +---+  +---+  |
+-----------------------+
```

The diagram (attempts to) show that the hyper batch has three vectors:
a, b, and c. All rows from all batches logically appear in each column, though
each column is physically defined by a separate vector for each batch. (Yes,
very complex. Plus, it is not entirely clear that this complexity will still
be needed once operators migrate to the new mechanism. Something to keep in
mind.)

From the application's perspective, it just iterates over rows and asks for
values. The reader must jump from one batch to another; find the right column,
get the offset within that batch, and return the value.

Hyper vectors make use of something called the `VectorWrapper` which is of two
kinds: simple or hyper. (If you've ever wondered why getting a vector from a
`VectorContainer` gives you a `VectorWrapper`, now you know.)

The vector wrapper is the "glue" that splices together a hyper vector from a
set of normal vectors: it glues them end-to-end.

So, to access a top-level vector, we first get the column (column "c" above,
say), then we go down to the 3rd entry (say) and get that vector.

The SV4 provides two fields: the batch (index within the `VectorWrapper` and)
offset (position within the vector.) (In practice, these "fields" are packed
into a single `int`, hence all the strange shifting in generated code.) In
the reader, the reader index takes care of splitting the two fields.

### Nested Vectors and the Access Path

The story told thus far, while it has some complexity, is not too bad:

* Look up column c
* Look up vector for batch b
* Get the value from offset x

What happens when we have complex types? Say we have a map, a union, a map
of unions, a map of maps, and so on. The vector wrapper concept exists only
at the top level of a row; it would be far too complex to push the
representation down into maps. (In fact, it is too complex to even do this
at the top level; that's why we can drop this complexity after moving to
readers.)

For a nested vector, the pattern is extended to:

* Look up column c
* Look up vector for batch b
* Get the vector for map member m1
* If the map is nested, get the vector for map member m2
* At the leaf, get the value from offset x

In a single batch, we can work out the navigation paths at build time: each
reader can directly hold onto the vector that it reads.

For top-level readers, each reader can hold onto the vector wrapper, and use
the batch index to access the right vector.

But, for nested vectors, we have to walk the access path *for every value*.
Obviously this is quite slow. (It will lead to a vast slowdown as the depth
gets greater in operators that use hyper vectors, such as sort.)

### Vector Accessors

The reader implements the access path concept using a vector accessor. Vector
accessors nest.

* At the top level, the vector accessor handles the vector wrapper indexing.
* At nested levels, the vector accessor takes a parent, specifies the child
to access, and looks up that child.

To make the single and hyper cases consistent, a single-batch vector accessor
simply holds onto the target vector, no per-access navigation needed.

## Generated Scalar Readers

A key goal of the scalar reader is to maximize speed: put as few instructions
as possible in the code path of each `get` method. This means minimizing bounds
checks. In most Drill code, the application decides the vector index, calls
the vector accessor, which calls into the `DrillBuf` which checks bounds and
calls into the UDLE, which checks bounds and calls into `PlatformDependent`
to access direct memory. All the checks are needed because Drill has many,
many ways to call the vector accessor, and some of them may have bugs.

The readers avoid bounds checks by doing bounds checks only when a position
advances (in a row or array.) Since the only way to access data is via the
index that the reader keeps private, we need not do bounds checks on each
alue access. Instead, we need only get the correct vector (as described above),
then call an "unsafe" method in `DrillBuf` to get data from direct memory.

Performance tests have shown that this design produces significant performance
improvements. Depending on the cardinality (AKA "data mode") the performance
boost has ranged from 50% to 200%. (Use the `PerformanceTool` class to run
the tests yourself to check current performance.)

As with the value vectors, the readers are generated from a Freemarker
template defined in
[`ColumnAccessors.java`](https://github.com/apache/drill/blob/master/exec/vector/src/main/codegen/templates/ColumnAccessors.java).
 (The name "vector accessor" was already used, so this project chose the
 name "column accessor" instead. In a way, the name is more accurate:
 their job is to access columns regardless of the number of vectors involved.)

Each generated class is as simple as possible: just some simple setup, then
a type-specific `get` method. Everything else resides in an abstract base
class. For example:

```
  public static class IntColumnReader extends BaseFixedWidthReader {

    private static final int VALUE_WIDTH = IntVector.VALUE_WIDTH;

    @Override
    public ValueType valueType() {
      return ValueType.INTEGER;
    }

    @Override public int width() { return VALUE_WIDTH; }

    @Override
    public int getInt() {
      final DrillBuf buf = bufferAccessor.buffer();
      final int readOffset = vectorIndex.offset();
      return buf.unsafeGetInt(readOffset * VALUE_WIDTH);
    }
  }
```

Aside from the `get` method, the three key pieces of information provided
by the generated code are:

* The value type (which `get` method is implemented)
* The width of each field (for fixed-width readers)
* A method to bind the reader to an index and a vector accessor

The bind method is needed to simplify object construction. We create the
readers dynamically. It is much easier to call the no-argument constructor
this way, then pass in the required bindings as a separate step. (**Note**:
It turns out to be rather simple to pass these values into the constructor.
Doing so would be a nice improvement. See the generated writers for an example.)

The careful reader may notice that we generate code only for the "required"
(non-nullable) cardinality. What about "optional" (nullable) and "repeated"
(array)? Those are built up via composition as described below.

## Nullable Scalar Reader

A nullable reader is simply a wrapper around a non-nullable reader which
mimics the internal structure of a nullable vector. The nullable reader
holds two readers: one for the "bits" vector, another for the values vector.
Here we see the value of the generic interfaces: we do not need type-specific
nullable readers: a single reader handles all scalar types.

## Array Reader

An array reader is also composed of two parts: an offset reader and an
element reader. The array reader introduces a new index level. The child level
provides the index into the data vector. Because of this, the element reader
is just a generated scalar reader. To make it an element reader, we pass in
the child index for the array.

Again, because of the generic structure of the readers, the same array
implementation works for repeated scalars, lists, repeated maps and repeated lists.

The application reads from an array by treating the array as an iterator.
This ensures that the same mechanism works whether the array has a single
value (a scalar array) or multiple values (such as a repeated map, or a
repeated map that contains other repeated maps.) Methods exist to reset
the index, or move the index to a specific location.

## Tuple Reader

The tuple reader holds onto a collection of object readers indexed by both
position and name. Applications can use either, or switch back and forth.
(Test code often does this.) The tuple reader represents both the row and
any maps within the row. (The row reader often has few extra methods, such
as those to advance the read position to the next row.)

## Null State

Detecting null values turns out to be complex in the general case. For a
simple nullable column, we need only check the associated "bits" (is-set)
vector. But, unions introduce another layer of complexity. A union can hold
vectors of multiple types, each of which must be nullable. The union itself
can be null. So, if we have a union of an `INT` and a `VARCHAR`, the `INT`
is null in any of the following three cases:

* The union itself is null
* The union value is a type other than `INT`
* The type is `INT` but the nullable `INT` is null

The `LIST` vector adds yet another complexity. A list can be a repeated union,
but has a null flag for each array. Thus, now a scalar can be null for any
of the three reasons above, plus:

* The list entry is null

Clearly, writing if-statements to handle all this complexity would be a
nightmare. Instead, the reader uses a "null state" object. Implementations
exist for all three cases: simple nullable, union and list. The nullable
reader simply asks the null state object if the value is null; the
implementation of the null state object worries about the messy details.

## Example Usage

The reader mechanism is meant to be used directly by the application,
so let's look at an example. The example uses this schema:

```
a: Varchar
b: Repeated Int
c: map
  c1: Int
  c2: Varchar
```

We'll use the reader to print the value of each row. Let's assume we
have a `print()` method available to us.

The key steps:

* Get a reader somehow.
* Iterate over the rows.
* For each, call a method to print the row
* Print column `a`
* Iterate over the values in `b` and print them
* Recursively call this method to visit the columns in map `c`.

Here is the code:

```
    RowSetReader reader = ...

    while (reader.next()) {
      print(reader.scalar("a").getString());
      ArrayReader bReader = reader.array("b");
      while (bReader.next()) {
        print(bReader.scalar().getInt());
      }
      TupleReader cReader = reader.tuple("c");
      print(cReader.scalar("c1").getInt());
      print(cReader.scalar("c2").getString());
      endRow();
    }
```

## Futures

The reader code is complete and tested for all types of Drill vectors
(including the obscure `List` and `Union` vectors.) Extensive unit tests
show that the code works for direct, indirect (SV2) and hyper (SV4) cases.

The two key future extensions are possible.

First, as described earlier, provide a more efficient way to retrieve values
for interval and decimal types. (The current implementation uses objects,
hich is fine for testing, but slow in production.)

Second, provide a multi-batch (not hyper-batch) reader. That is, a reader
that accepts a sequence of batches, such as would be needed in, say, the
filter or project operators. The batches are sequential over time. If the
schema is identical, then the only task is to reset the indexes to 0 and
set the new row count. If the schema changes (which we probably should not
support), rebuild the readers to match the new schema.

The multi-batch reader (call it a "result set reader") is the read dual
of the "result set loader" for writing. This parallels the single-batch
reader and writer in the row set package.