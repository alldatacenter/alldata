# Column Writers

The sections thus far have set the stage; now lets move to the first act: the
column writers. On the surface, the column writers seem quite simple: write a
value to a vector and increment an index. The devil, as they say, is in the
details.

## Requirements

Let's start by identifying the requirements we wish to satisfy.

* Handle all Drill column (that is, vector) types, even obscure ones such
as unions and lists
* Coordinate writing to a set of columns in a row (common row write index)
* Handle writes to arrays (nested common write index)
* Allow adding new columns during the write (to maps, unions, repeated
lists, lists)
* Handle (but don't implement) vector overflow
* Minimize bounds checks for performance
* Simplified API
* Both type-aware and generic operations
* Support unprojected columns

That is quite the list! The list is not simply good ideas; it turns out to
be what is needed to pass a robust set of unit tests. So, let's dive into
each topic.

## Writer Index

As discussed earlier, the writers manage the row index common to the set of
vectors that make up a batch. Further, since Drill supports arrays (of scalars,
maps and others), we need nested indexes. Fortunately, for writes, we work only
with a single batch with no indirection vectors.

## Schema

The writers write to vectors. For a variety of reasons, it turns out to be very
handy to have a metadata description of the vectors. This allows, for example,
for the schema to be specified first, and to use the schema to construct the
vectors. The writes use the [[metadata mechanism|]BH Metadata] previously discussed.

### Up-Front Schema

Readers such as CSV, Parquet, JDBC and others are fortunate to know the schema
up-front. Parquet, for example, has a fixed schema defined in the file footer.
CSV assumes a single column (`columns[]`) or reads the header to determine the
schema. And so on.

In this case, the reader can define the schema, using the metadata mechanism,
then create a matching set of writers. (There is an additional projection
step, as we'll discuss later.)

So, in this case, the order is:

1. The reader determines the schema that the file offers.
2. Drill builds writers to match before we begin reading data.

We use the term "early schema" to describe the above use case.

### Schema Evolution

Readers such as JSON discover columns on the fly. That is, JSON does know
that column "a" exists until it is read. We use the term "late schema" for
such readers.

At the point we add a new column, Drill is already reading a row of data. So,
we must add columns in the fly, perhaps many rows into a record batch. The
writer mechanism must add the new column, then get the column "caught up" with
the existing columns. Much of the actual work is done by the result set loader
layer; the writer layer simply needs to provide the APIs to support column
addition. For example, in the `TupleWriter` class:

```
  int addColumn(ColumnMetadata column);
  int addColumn(MaterializedField schema);
```

The writers are defined in the `vector` module, with the actual column addition
logic defined in `java-exec`. Since `vector` cannot depend on `java-exec`,
the writers use the listener pattern. The writers define a listener to handle
column addition:

```
  interface TupleWriterListener {
    ObjectWriter addColumn(TupleWriter tuple, ColumnMetadata column);
    ObjectWriter addColumn(TupleWriter tuple, MaterializedField field);
    ProjectionType projectionType(String columnName);
  }
```

The `java-exec` layer implements the listener and registers it with the writer.

## Bounds Checking

Much has been said about the cost of doing per-write bounds checking in Drill
vectors. The writers were specifically designed to minimize bounds checks.
This part of the code is the most performance critical in the entire project.
Great care should be taken when "improving" this code. Let's take a look at
the specifics.

We start by observing that the writers manage the vectors and the write index.
The APIs are designed that neither of these critical items can be modified
except via code paths managed by the writers. Thus, the writer takes full
responsibility for monitoring vector size and the writer index.

The vector code does bounds checks because it has no idea who is making the
request: whether that caller knows what it is doing or not. So, we observe
that, if we tightly control the access path, then only "known good" callers
will write to vectors and so we can trust that caller and avoid the bounds
checks.

For this to work, the writers must be designed in a way that we know that
they will not write out of bounds. Much of this work can be done outside
the write path itself, allowing the write path to be as simple as possible.

### Requirements

To achieve our goals, we want the writers to handle six cases with respect
to vector allocation:

* Monitor current vector size to determine when a vector must be doubled.
* Before doubling, determine if doing so would cause the vector to exceed
the vector size limit (16 MB), and, if so, to take corrective action.
* Further check whether the doubling would cause the entire batch to exceed
the desired size limit and, if so, to take corrective action.
* When doubling is needed, avoid repeated doubling that occurs in the current
vector code. That is, if the vector is 256 bytes, but we need 1024, don't
first double to 512, then double again to 1024. Do it in one step (in this
case, quadrupling.)
* Avoid doubling as much as possible by allowing the client to specify good
initial size estimates (via the metadata discussed previously.)
* When doubling, do not zero the vector. Instead, carefully manage writes
to "fill empties" as needed. (The exception are bits vectors which require
that the default value be unset (NULL).)

### Implementation

Many of the above requirements are handled through the vector writer code.
The mechanism comes together in the generated `set()` methods. For example:

```
    @Override
    public final void setInt(final int value) {
      final int writeOffset = prepareWrite();
      drillBuf.unsafePutInt(writeOffset * VALUE_WIDTH, value);
      vectorIndex.nextElement();
    }
```

This code does three things:

* Determines the write position, which does bounds checks.
* Writes the data (without bounds checks).
* Optionally increments the writer index (as needed for scalar arrays.)

What, then, does `prepareWrite()` do?

```
    protected final int prepareWrite() {
      int writeIndex = vectorIndex.vectorIndex();
      if (lastWriteIndex + 1 < writeIndex || writeIndex >= capacity) {
        writeIndex = prepareWrite(writeIndex);
      }
      lastWriteIndex = writeIndex;
      return writeIndex;
    }
```

Here we do a single (two-part) check:

* Do we have room in the vector for our data?
* Do we need to "fill empties" since the last write?

If not, we just return the write position. Otherwise, we have some work to do.

```
    protected final int prepareWrite(int writeIndex) {
      writeIndex = resize(writeIndex);
      fillEmpties(writeIndex);
      return writeIndex;
    }
```

In the above, we resize the vector (if needed). If overflow occurs, the writer
index will change, so we handle that. Then, we fill any empty values.

Resize is:

```
  protected final int resize(final int writeIndex) {
    if (writeIndex < capacity) {
      return writeIndex;
    }
    final int width = width();
    final int size = BaseAllocator.nextPowerOfTwo(
        Math.max((writeIndex + 1) * width, MIN_BUFFER_SIZE));

    if (size <= ValueVector.MAX_BUFFER_SIZE &&
        canExpand(size - capacity * width)) {
      realloc(size);
    } else {
      overflowed();
    }
    return vectorIndex.vectorIndex();
  }
```

We compute the required vector size, rounding to a power of two as the allocator
will do. We then ask the listener if we are allowed to expand the buffer by the
desired amount. If the buffer will be below the maximum size, and we are allowed
to expand it, we do so. Else, we trigger overflow which will create a whole new
atch transparently.

As you can see, the bounds checks are minimized. Additional work is done only
when needed. This code has been heavily optimized, but we can always improve.
Just make sure that improvements really do reduce time; it is easy to make
simple changes that actually slow performance.

## Generated Writers

The code example above shows most of the generated code for the `INT` writer.
As in the readers, we generate code only for the non-nullable ("required") case.
Nullable and array writers are composed on top of the required writers. The
full generated code is as follows:

```
  public static class IntColumnWriter extends BaseFixedWidthWriter {

    private static final int VALUE_WIDTH = IntVector.VALUE_WIDTH;

    private final IntVector vector;

    public IntColumnWriter(final ValueVector vector) {
      this.vector = (IntVector) vector;
    }

    @Override public BaseDataValueVector vector() { return vector; }

    @Override public int width() { return VALUE_WIDTH; }

    @Override
    public ValueType valueType() {
      return ValueType.INTEGER;
    }

    @Override
    public final void setInt(final int value) {
      final int writeOffset = prepareWrite();
      drillBuf.unsafePutInt(writeOffset * VALUE_WIDTH, value);
      vectorIndex.nextElement();
    }
  }
```

The writer reports its value type and width (used by base classes.) It then
holds onto its vector and implements one or more `set` methods.

## Vector Overflow

A key contribution of this implementation compared to the "classic" complex
writer is the ability to handle overflow. Overflow describes the case in
which we want to write a value to a vector, do not have space in the current
buffer, but decide we do not want to expand the buffer.

It would seem we have put ourselves in quite a bind. But, there is a way out.
We can sneakily swap out the "full" buffer for a new empty one. Now, since the
buffer is part of a vector, and that vector is part of a record batch, we must
do the swap for *all* columns. (The code has some ASCII art that illustrates
the details.)

At the point we encounter overflow, we have to worry about four distinct
cases. Suppose we have columns a, b, c and d.

* We have written column a.
* Column b has had no values for the last few rows.
* We are trying to write column c.
* We will then follow by writing column d.

To handle these cases:

* We have a value for "c" ready to write, but have a full vector, and we write
the value to the first position in the new vector.
* We have written "a". A the time of overflow, we need to copy the value of "a"
for the current row from the old vector to the new one.
* We have not written "b" for a while. We must fill empties up to (but
excluding) the current row in the old "c" vector. The normal fill-empties
mechanism will take care of additional empties.
* We have not written "d", so there is no value to copy. We will fill in
"d" shortly.

Quite a bit of code is needed to precisely handle the above states. In arrays,
the conditions are a bit more complex. The "current row value" may be a great
number of values (the array values written for the current row.) In nested
structures (arrays of maps that contain arrays of maps that contain scalar
array), the logic must cascade.

The writer layer is responsible for keeping the writers in sync with the
changes made in overflow. The vector part of overflow handling is done by
the result set loader layer. The writer triggers the process via the listener
interface.

## Rewrite a Row

The writers are designed to handle an obscure, but useful, use case: the
desire to write a row, then discard it, and overwrite that data with new data.

Given that vectors are always written sequentially, abandoning a row is
simply a matter of moving the write index back to the start of the row.
(There are, of course, additional details.)

An application of row rewriting would be to push filters into a reader.
A reader could then do:

```
  while (batch is not full) {
    readRow(writer)
    if (keepRow()) {
      writer.save();
    }
  }
```

Here, `readRow()` reads a row of data. `keepRow()` would use the values in the
vectors to evaluate a filter expression. If the row passes the filter, we save
it. Else, we read a new row that overwrites the current row.

The result is that we still read each row, and the filter code always works
with value vectors. But, we need not fill a vector with unwanted values, only
to discard them in a later operator.

This functionality is extensively tested in
[`TestResultSetLoaderTorture`](https://github.com/apache/drill/blob/master/exec/java-exec/src/test/java/org/apache/drill/exec/physical/rowSet/impl/TestResultSetLoaderTorture.java).

It has been suggested that we add an explicit `discardRow()` method rather
than just implicitly rewriting the row if we do not call `save()`. That would
be a fine enhancement.

(In writing this, another test case came to mind: do we handle resetting the
"is-set" vector values? Something to verify.)

## Writer Events

The process described above for overflow is just one of several orchestration
processes that the writer must handle. Others include:

* Start a new batch
* Start a new row
* Start a new array value
* Finish a row
* Finish a batch

Because of tree structure of the writers, these events can cascade downwards.
An interface exists to clearly specify the events:

```
public interface WriterEvents {
  enum State ...
  void bindIndex(ColumnWriterIndex index);
  void startWrite();
  void startRow();
  void endArrayValue();
  void restartRow();
  void saveRow();
  void endWrite();
  void preRollover();
  void postRollover();
}
```

The actual class contains substantial documentation, so we won't repeat
those details here.

## Projection and Dummy Writers

The writers are designed to assist "dumb" readers such as CSV. CSV must
read each column (since it is reading a file, and must read column 1 in order
to find the start of column 2, and so on.) Queries can include projection:
that is, request a subset of columns. Put the two together, and readers end
up needing code such as:

```
  read column x
  if (column x is projected) {
    write column x to its vector
  }
```

The above is quite tedious. The writers support an alternative dummy columns.
A dummy column acts like a real one (implements the full writer protocol), but
it has no backing vector and discards values. Thus, a reader can be simplified to:

```
  read column x
  write column x to its vector
```

The write step is either a no-op, or an actual write.

The dummy columns themselves are created by the result set loader in response
to its projection analysis mechanism. The column writers, however, provide the
necessary mechanisms.

It may be worth noting that more sophisticated readers (JSON, Parquet) have
mechanisms to work with non-projected columns. In Parquet, we don't even read
the data. In JSON, a parser state "free-wheels" over unprojected columns
(reading nested objects, arrays, etc.)

Still, the dummy-column mechanism will be quite handy for community provided
readers as it provides a very simple way to hide projection details from format
plugin writers.

## Union and List

The writers are all pretty straightforward for the flat row case. Arrays and
maps add a bit of complexity. However, a very large source of complexity turned
out to be two barely-supported data types: union and list.

As described earlier, a union is, essentially, a map keyed by type. Any row
uses a value from only one type. Unions must allow new types to be added at
any time, mirroring how columns are added to rows and maps.

The insane level of complexity occurs in the list vector. A (non-repeated)
list has the following crazy semantics:

* The list can have no subtype. This is a list of null values.
* The list can have a single subtype. This is a list of, say, nullable
`INT` or nullable `VARCHAR`. (Or, even a non-nullable `Map`.
* The list can be "promoted" to a union. Now it is a list of unions, and
the union can hold multiple types.

The semantics of writers assume that the application can obtain a writer and
hold onto it for the lifetime of the write. The list vector wants to change
the fundamental structure as the write proceeds. To bridge the gap, some quite
complex code is needed in the `VariantWriter` to shield the application from
the above modes.

While the code works, and is fully unit tested, the list vector itself is
highly dubious. First, it turns out to be unsupported (despite having been
created specifically for JSON.) Second, the complexity is not worth the value
it provides.

Recommendation: drop support for the list vector. Come up with some more
sensible alternative. See Arrow's implementation of their List type for ideas.

## Repeated Lists

Another obscure type which Drill does appear to support is the repeated list.
(The list vector is repeated -- it is an array -- but is not considered a repeated
list. Sigh...) A repeated list is basically a 2D array. It introduces an offset
vector on top of a repeated type (including another repeated list.) The repeated
list does not have null bits per value as does the (non-repeated) list. Nor does
t allow unions.

The repeated list type is implemented simply as an array writer whose entries are
also array writers.

## Example

The above presents the developer's view of a writer. Here is the user's view to
create a writer and write one row. The schema is:

```
(a: VARCHAR, b: REPEATED INT, c: MAP{c1: INT, c2: VARCHAR})
```

The row is:

```
("fred", [10, 11], {12, "wilma"})
```

Code:

```
    RowSetWriter writer = ...

    writer.scalar("a").setString("fred");
    ArrayWriter bWriter = writer.array("b");
    bWriter.scalar().setInt(10);
    bWriter.scalar().setInt(11);
    TupleWriter cWriter = writer.tuple("c");
    cWriter.scalar("c1").setInt(12);
    cWriter.scalar("c2").setString("wilma");
    writer.save();
```

The full code is in
[`RowSetTest`](https://github.com/paul-rogers/drill/blob/RowSetRev3/exec/java-exec/src/test/java/org/apache/drill/test/rowSet/test/RowSetTest.java)`.example()`.

Using a result set loader is similar. The way we obtain a writer is different,
but the write steps are identical.