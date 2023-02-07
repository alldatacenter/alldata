# Row Set Mechanism

The row set mechanism is a handy, general purpose way to work with a batch
of records. It came about from a desire to operator-level unit testing in
which we create a batch of data, give it to the operator, and verify the
resulting output. At that time, it was very difficult to create a batch of
data: it required a large amount of complex code. (In production, this kind
of code is generated, so we didn't have need to a simple interface.)

The term "row set" came about because of the overloaded nature of the term
"batch" in Drill where it is both a set of rows and the operator that
operates on those rows. The term "row set" is meant to invoke the idea of a
collection of relational rows. (Also, the term was not previously used.)

The row set layer provides tools to work with all aspects of a batch life cycle:

* Define a schema (using the `SchemaBuilder` class)
* Build a row set from a schema and a fluent description of the data (`RowSetBuilder`)
* Attach to an existing vector container (`RowSet` and its implementations)
* Print the row set (`print()` method on the `RowSet` class)
* Compare two row sets to verify test results (the awkwardly named `RowSetComparison`.)

The classes currently reside in `src/main/test`. As least some of the classes
may want to move to `src/main/java` so they can be used to diagnose problems
in a Drill server.

## Row Set Classes

Drill has a whole menagerie of batch types:

* `RowSet`
  * `SingleRowSet`
    * `DirectRowSet` (single batch without a selection vector)
      * `ExtendableRowSet` (a writable single row set)
    * `IndirectRowSet` (using an SV2)
  * `HyperRowSet` (using an SV4)

Many operations are similar across row set types, while some differ. The row
set class hierarchy captures those differences to provide a simple, consistent,
easy-to-use API, primarily for tests.

## Schema Builder

The fluent schema builder can build all types of Drill schemas from the simplest
(scalar types in a flat row) to the most complex (deeply nested complex types.)

(It is not entirely clear that the complex types earn their keep: they are very
complex but only lightly used. They add tremendous complexity to the schema
builder and all other parts of this project. We had to make them work, but one
often wonders if they are really necessary.)

See `ExampleTest` (or most of the tests in this project) for examples.

## Row Set Writer

The row set writer is the little brother of the result set loader. The row set
writer works on a single row set with a fixed schema. This limitation is fine
in tests. Indeed, most of the time you will never actually see the row set writer:
it is wrapped by the `RowSetBuilder` which builds the vectors from schema,
allocates the vectors, writes values to the vectors, and returns the fully
populated row set.

For our purposes in this project, the row set writer serves two purposes:

* Allows us to create test batches easily.
* Provides a simple, pure test of the column writers without the added complexity
of the result set loader.

Indeed, the row set writer uses exactly the same column writers as it's big
brother, but without all the complexity of schema evolution, vector overflow,
projection and the like. In short, if you change the column writers, test with
the row set writer first before testing with the result set loader.

For obvious reasons, the row set writer works only with an extendable single row
set (no indirection, no hyper vector.) There is a tool, however, to build up a
hyper vector from a set of single row sets.

## Row Set Reader

In some ways, the row set reader is simpler than the writer: it only need retrieve
existing values. In other ways, the reader must handle more cases:

* Direct row set
* Indirect row set (using an SV2)
* Hyper row set (using an SV4)

The reader interface, however, is designed to hide all these details. The
application simply creates a reader, steps through the rows, and reads out
the values. The reader itself handles all necessary indirection.

The row set reader also works with a single batch. If we want to use this
mechanism in operators, then we'll need a "result set reader" that handles
multiple batches (along with any schema changes across batches.) That should
be a very simple mechanism to build on top of the existing row set reader.

Most of the time you never use the row set reader directly. In a test, you
want to know if a some "actual" batch (produced by an operator, say) matches
an "expected" batch. The `RowSetComparison` class does that work for you.

## Typed and Generic Object Access

Both the reader and writer are designed for two distinct use cases:

* Type-aware code, such as that produced by code generation.
* Generic code, such as that that is convenient in tests.

Here, "generic" means (as a Java object) (not in the sense of Java Generics.)

The typed methods are much simpler than the "classic" complex writers which
had a distinct "get" method for every data type. The readers and writers use
a reduced set centered around the Java primitives. All types smaller than an
`int` are accessed as an `int` (since Java uses that much space to pass values
anyway.) Both `FLOAT4` and `FLOAT8` are returned as doubles. And so on.

(Date/time and interval classes are currently passed as Joda objects. For
production code, this must be replaced with something more performant, such
as returning fields in an array of `int`s or some such.)

The generic methods are designed to enable simple generic code, such as that
in `RowSetComparison`: all values are treated as Java objects in both `set`
and `get` methods. While we would never use these techniques in production,
they same vast amount of complexity in tests.

Further, the accessor mechanism itself is design for generic (interpreted)
access. The accessors are self describing so it is easy for test code to
walk a column three for various tasks. (Something used again and again in
test code.)
