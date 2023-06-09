# Result Set Loader Overview

Please see `/vector/src/main/java/org/apache/drill/exec/vector/accessor/Overview.md`
for the basics.

The result set loader assembles batches, handles projection, allocates
batches, creates writers, handles overflow and more.

## Requirements

To understand the result set loader, it helps to start with requirements:

* Write to a set of batches (together, the "result set")
* Create and allocate the vectors and buffers needed for each batch
* Allow "early schema" (known up front) and "late schema" (discovered on
the fly)
* Use the column writers to write data
* Orchestrate the vector overflow process
* Support projection push-down
* Implement the "vector persistence" required by Drill's iterator protocol

An informal requirement is to keep the application-facing API as simple as
possible.

## Lifecycle

Another way to get a handle on the result set loader is to consider a
typical lifecycle:

* Setup
  * Optionally specify a schema
  * Optionally specify a set of projected columns
* Per Batch
  * Start the batch
  * Write rows to the batch
  * Handle overflow, if required
  * Harvest the batch
* Wrap-up
  * Release resources

## Structure

The result set loader API is meant to be simple. See
[`TestResultSetLoaderProtocol`](https://github.com/apache/drill/blob/master/exec/java-exec/src/test/java/org/apache/drill/exec/physical/rowSet/impl/TestResultSetLoaderProtocol.java)`.testInitialSchema()`
for a simple example of the public API. (This test uses the test-only dynamic
mechanism to write rows, but would work equally well using the form shown in the
column writers section of the overview mentioned above.)

The simple API masks a complex internal structure. The structure must handle
a number of tasks:

* Hold onto the set of columns that make up a row (or map)
* For each column, hold onto the vector and its column writer
* Orchestrate activities that involve both the vectors and writers (such as
allocation, overflow and so on.)

The classes that manage the write-time state are called "state" classes. It
has been pointed out that this is confusing. These are not "state" in the
sense of the name of a state. Rather, they are "state" in the sense of the
opposite of "stateless." Writers and vectors are stateless, the column and
other state add a stateful layer on top of them.

### Result Set Loader Implementation

The result set loader implementation class is
[`ResultSetLoaderImpl`](https://github.com/apache/drill/blob/master/exec/java-exec/src/main/java/org/apache/drill/exec/physical/rowSet/impl/ResultSetLoaderImpl.java).
Its primary task is to implement the lifecycle API, maintain a state machine
that tracks the lifecycle, and hold onto the root row structure.

### Tuple State

Every batch is made of rows. A row is a collection of columns. The tuple state
maintains the list of columns for the row. The same class also maintains the
state for a map. (In Drill, rows and maps are both tuples.)

The tuple state accepts events from the result set loader and passes them down
to each contained column. The tuple state is also the unit of schema evolution:
it implements the callback from the tuple writer to add a column.

The tuple state is mostly a collection of very specific operations: the best way
to understand it is simply to read the code and the ample comments within the code.

### Column State

A row is made up of columns (some of which are maps that contain another tuple.)
A column is defined by three key attributes:

* Metadata
* Vector
* Writer

If the column is a map then it will also hold onto a tuple schema for the map,
creating a recursive tree structure. (A similar structure exists for unions.)

The column state implements a state machine that tracks the column lifecycle:

* Normal: column is being written
* Overflow: the column has overflowed and has data "rolled over" to the "overflow"
batch.
* LookAhead: the previous row has been harvested. The roll-over data is now in
the main batch.
* NewLookAhead: the column was added in the overflow row. It did not exist in
the harvest row, but exists internally and will exist in the next batch.

The job of the column state is to "do the right thing" in response to various
events in the states defined above. Please see the code for a much more complete
description as the set of operations best understood at the code level.

### Vector State

Most columns hold onto a vector. (The exception is a map as we'll discuss
below.) If the column is unprojected (see below), then the vector might be
null. Further, different vectors have different internal structure: nullable
vectors, for example, have two "real" vectors: the bits and the data.

The vector state abstracts away this complexity by providing a defined API
that is implemented as needed for the various cases described above. This
allows the column state to be much simpler: the same "primitive" column
state can handle required, optional and repeated vectors since the differences
are handed within the vector state.

In addition, when overflow occurs, the column state will maintain two
vectors: one for the full batch, another for the "look ahead" batch. By
encapsulating the vector behavior in the vector state, the column state
need not contain either redundant code, nor if-statements, to work with the
two vectors.

### Column Builder

By now it should be clear that quite a bit of machinery is needed to
maintain a column. For a primitive column we need the vector, the vector
state, the writer, the column state. Arrays and maps add additional structure.
Building this structure is a task in its own right.

The [`ColumnBuilder`](https://github.com/apache/drill/blob/master/exec/java-exec/src/test/java/org/apache/drill/test/rowSet/schema/ColumnBuilder.java)
class tackles this work. It also handles the task of parsing metadata to
determine which kind of column to create. While "mainstream" Drill has only
a few types, the addition of unions, lists and repeated lists adds quite a
bit of complexity.

## Vector Overflow

All of the above machinery is in support of one specific task: handling
vector overflow. As noted in previous sections, overflow occurs when we
decide that we have a value to write to a vector, do not have room in the
vector, and decide that we don't want to increase the size of the vector.
Instead, we create a new vector and place the data in that new vector.

### Overflow Use Cases

This turns out to be extremely finicky. We must handle many cases such as:

* For a simple fixed-width vector, simply create the new vector and write
the value in the first position of that new vector.
* For an array of fixed-width values, we must also copy all previous values
for the same row. (If the array is multi-dimensional (an array of maps
containing an array of `INT`s, say), we must copy values for all arrays in
the current row.)
* For a `VarChar` or array vector, we must consider the offset vector. We
cannot simply copy the offsets, since the offsets are different in the new
vector. Instead, we must use the "old" offset values to calculate new offset
values for the new vector.

The tuple state, column state and vector state classes were created as a
way to manage the complexity of this process: each handles one particular
bit of the process, delegating work downward. For example, when working with
arrays, the array column state will determine the extent of the array, then
push that data downward. (In practice, this is maintained as row-start
positions in the associated writer index.)

### Look-ahead Vectors

Overflow works by maintaining two vectors: the "main" vector that holds the
data to be "harvested" for the current batch, and a "look-ahead" batch
holding the first row of data for the next batch. Rather than move the data
twice (from main batch to overflow and back), we "exchange" buffers between
the two batches. (This is like a transfer pair, but bidirectional.)

One implication of this design is that, when doing memory planning for an
operator that uses this mechanism, assign the operator *2* batch units: one
for the main batch, another for the overflow.

### Schema Considerations

Consider how to requirements cited above interact. Suppose an application
writes a row and encounters overflow. Suppose that, in the same row, the
application decides to add column `x`. The only value for 'x' will be
written into the look-ahead batch. What do we do?

We have two choices:

* Include column `x` in the full batch, even though all its values will
be NULL.
* Omit `x` from the full batch, but add it to the next batch.

One could argue both cases. The current code choose the second. The reason
is this. Suppose that overflow occurred on row 1000. The batch that results
will contain rows 1-999.

Suppose that, rather than overflowing, the application had decided to stop
reading just before the overflow row. The resulting batch would contain rows
1-999 and would not contain column `x`. (`x` will be in the next batch.)

The application receiving the batch should not know or care which case
occurred: overflow or stopping at 999. To keep things simple, we make
the overflow case look like the stop-at-999 case by omitting column `x`.

### Implementation Details

The best way to understand this process is to read the code, run the
tests, and step through the process. Expect to be confused the first
few times. (Heck, *I* wrote it and *I'm* still confused at times.) Focus
on each part rather than trying to understand the entire process in on go.

## Projection

As explained earlier, the result set loader mechanism is designed to help
with projection. Suppose that the query asks for columns ('b', 'a'). Suppose
that that we are in a scan operator and that the reader works with a table
that can provide rows ('a', 'c'). Several steps are needed to project the
table schema to the query schema.

The result set loader takes the project list. Each time the application
(record reader in this case) asks to add a column, the result set loader
consults its project list. Suppose the record reader adds column `a`. The
result set loader notices that `a` is projected and thus creates a vector,
writer column state and so on as described above.

Then, the record reader asks to create column `c`. The result set loader
finds that `c` is not projected. But, the record reader presumably wants
to write the values anyway (such as for CSV.) So, the result set loader
constructs a "dummy" column that has a dummy writer, but with no underlying
vector.

The example above illustrates work that must be done elsewhere: how to fill
in the missing column `b`, and how to reorder the table columns into the
order desired by the query. That is the job of the projection mechanism in
the scan operator. (This discussion again highlights a key design philosophy:
break tasks into small pieces, then build up the complete functionality via
composition.)

## Schema Versioning

The result set loader allows the application to add columns at any time.
Within a batch, the loader needs to know if a column has been added since
the last "harvest." The traditional solution in Drill is to add a "schema
has changed" flag. But, this raises two questions: "changed since when?"
and "who resets the flag?"

To avoid the ambiguity inherent in a flag, the result set loader uses a
schema version. The version is bumped each time a new column is added. So,
the version starts at 0. Add column "a" and the version bumps to 1. And so on.

The number is used internally (discussed below.) It is also visible to the
application. The application can compare the versions of two results to
determine if the schema has changed since the previous output batch.

## Harvest Operation

We now have enough background to consider the "harvest" operation: the
task of creating an output batch.

The result set loader uses tuple, column and vector states to track vectors.
It *does not* use a `VectorContainer`. So, the first job of harvest is to
assemble that vector container. To do this, the first harvest:

* Create a `VectorContainer` to hold the output batch.
* Add all columns to the vector container in the order they were added to
the writer (which is the order they appear in the tuple writer, map writer
and so on.)
* But, exclude columns added since overflow. (Clearly, those must always be
the last columns in any row or map.)
* Remember the schema version at the time of the harvest.
* Make the output container and the schema version available to the application.

Note a special case for the output version number. It must be the version
number at the end of the last non-overflow row. That is, we do not include
columns (and thus their version numbers) added in the overflow row.

Then, on the next harvest:

* Spin through columns, adding those added since the previous version (again
excluding newly added overflow columns.)
* Update the output version.

### Maps

We mentioned above that the result set loader does not maintain its vectors
in a vector container; instead it assembles the container at harvest time.
We also noted that maps and rows are both tuples. So, extending our logic,
we also do not maintain map vectors for maps. Instead, the map vector is
created only in the output container. (In Drill, a map vector is not a true
vector: it has no memory buffer. It is just a container for the map members.)
Repeated maps are a bit different. We keep only the offset vector in the
result set loader. We create the repeated map vector in the output, then
populate it with both the member vectors and the offset vector.

### Unions

Unions should probably follow the same rules as for maps and rows. (A union
is a "tuple" but one keyed by type, not name.) But, since union vectors are
barely supported, we did not go through the trouble of fancy schema management.
Instead, the full vector schema is always available. This rule cascades to
maps within the unions and so on.

## Vector Persistence

The result set loader is designed to work with a single record reader since
each reader should define its schema independent of any other reader that
happens to exist in the same scan operator. That is, the behavior of a record
reader should not depend on which readers might have preceded it in the scan
operator. (If it did, we'd have a very large number of cases to test.)

Drill, however, requires that the scan operator provide not just the same set
of columns ("a", "b" and "c", say), but that it provides the same *vectors*
for those columns.

So, we've got a challenge. Each reader should be independent. But, they must
oordinate to use the same vectors. What do we do?

The result set loader solves the problem with a
[`ResultVectorCache`](https://github.com/apache/drill/blob/master/exec/java-exec/src/main/java/org/apache/drill/exec/physical/rowSet/ResultVectorCache.java).
This cache, holds onto all vectors created at that level. When a reader asks
to create a new column, the result set loader first checks the cache, and
reuses any existing vector. Otherwise, it creates a new vector and adds it
to the cache for possible reuse later.

The result is that we achieve both goals. Readers have complete control over
their schema and output schema. But, if the readers do define the same columns,
they will end up using the same vectors.

There is not just one cache, but potentially many. Each tuple has its own.
There is one for the top-level row. If that row contains a map, then the map
gets its own cache. The same is true for unions, lists and repeated lists.

## Unit Tests

This page has described a number of complex mechanisms. There is quite a bit
that must work together, and quite a bit to go wrong.

An [extensive set of unit tests](https://github.com/paul-rogers/drill/tree/RowSetRev3/exec/java-exec/src/test/java/org/apache/drill/exec/physical/rowSet/impl)
exists for the mechanism. When making changes, start with the basic tests, then
work toward the most complex (the "torture" test).

Also, when making changes, **add new tests**. This mechanism is far to complex
to test the traditional way: make a change, run a query, and declare victory.
You must exercise every code path with a unit test so you can catch issues
early and in a form that the issues can be debugged. Waiting to find issues
in integration tests will waste a huge amount of time, will be quite
frustrating, and will thus slow the adoption and evolution of the feature.

For debugging, it works best to use Test-Driven Development (TDD). Find a
uitable test and run it. Make your change. Create new tests (or extend
existing ones) to exercise the new case (in all its code paths!) Run those
tests. Then, run all the related tests to catch regressions.

Is your change so simple (or under such time pressure) that you don't need
the above? If so, go back to the top of this section and reread it: you
don't have time to *not* test.