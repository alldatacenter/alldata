# Column Accessor Components

The code to manage batch sizes affects many aspects of Drill's low-level
batch handling. This page provides an overview of the components so that,
as you read the details for each, you can fit them into the larger picture.

We list the components from the top downward. In general, higher-level
components depend on lower level components, but not visa-versa (a
restriction that is necessary to allow component-level unit testing.)

## Scan Operator

The readers are the worst offenders in terms of uncontrolled batch sizes.
The original plan was to first create a framework, then upgrade the dozen
readers using the framework. That is, replace a dozen ad-hoc implementations
of projection and batch assembly with a single, robust, fully tested
implementation, leaving the readers to just worry about fetching data from
the input source. Two readers have been upgraded: CSV and JSON.

### Easy Plugin Framework

Package: [`org/apache/drill/exec/store/dfs/easy/`](https://github.com/apache/drill/blob/master/exec/java-exec/src/main/java/org/apache/drill/exec/store/dfs/easy)

The highest-level components in this project are the revisions to the Easy
Plugin framework which is used by many file-based readers. The work here extends
the framework to support either the "classic" scan operator or the new size-aware
scan operator. By allowing both, we allow each plugin to upgrade on its own schedule.
Once all plugins are upgraded, the classic support can be deprecated.

### JSON And CSV Plugins

The project upgraded the CSV and JSON plugins. CSV was done first as a proof-of-concept
because it has relatively modest needs. JSON was done next because JSON is, in
some ways, Drill's most complex data structure. The work here simply replaces
the old code to create readers with a new version.

### JSON and CSV Readers

Packages:
* [`org/apache/drill/exec/store/easy/text/compliant/`](https://github.com/apache/drill/blob/master/exec/java-exec/src/main/java/org/apache/drill/exec/store/easy/text/compliant)
* [`org/apache/drill/exec/store/easy/json/`](https://github.com/apache/drill/blob/master/exec/java-exec/src/main/java/org/apache/drill/exec/store/easy/json)

The bulk of the work to replace the JSON and CSV readers are in the readers
themselves. In the case of CSV, we replace the ad-hoc code that worked with
direct memory buffers with code that uses the row set loader. The JSON reader
was a bit of a larger project for two reasons. First, the old reader was a bit
of a rat's nest of states and if statements, so we replaced it with a much
cleaner state machine. Second, the old "complex readers" did some of the work
that the JSON reader did, so that work was shifted to the JSON reader itself.
The work to write to the row set loader was actually quite easy.

As we will discuss, JSON presented some unique challenges because Drill's
design is incomplete and ad-hoc: there is no good mapping from an arbitrary
JSON structure into a columnar structure; instead we have a number of ad-hoc
rules that, together, handle some of the more complex cases -- but only in
some scenarios.

### File Scan Framework

Packages:
* [`org/apache/drill/exec/physical/impl/scan/columns`](https://github.com/apache/drill/blob/master/exec/java-exec/src/main/java/org/apache/drill/exec/physical/impl/scan/columns)
* [`org/apache/drill/exec/physical/impl/scan/file`](https://github.com/apache/drill/blob/master/exec/java-exec/src/main/java/org/apache/drill/exec/physical/impl/scan/file)

Existing file-based readers do quite a bit of common work, but each in its own way.
For example, different readers have different approaches to how they populate
the file metadata (AKA "implicit") columns. The file scan framework is an
implementation of a scan framework (see below) that provides a uniform way
to handle file-related tasks. Plus, as a bonus, this framework provides
incremental creation of readers as they are needed, rather than creating
all up front.

As we improve aspects of the file framework (such as Salim's improvements to
the storage of implicit columns), we can implement it once in the common
framework, and it will be immediately put to use by all file-oriented readers.

### Scan Framework

Package: [`org/apache/drill/exec/physical/impl/scan/framework`](https://github.com/apache/drill/blob/master/exec/java-exec/src/main/java/org/apache/drill/exec/physical/impl/scan/framework)

Each reader also handles projection push-down in its own way. Projection turns
out to be quite complex in the general cases with many special cases to be
considered. (For example, Drill allows columns of the form `a.b`, which are
not used for simple readers such as CSV, but such readers have to detect such
columns anyway to create the necessary null columns.) Again, each reader does
projection slightly differently.

The scan framework provides a single, shared mechanism for projection. It
handles all cases: scalar, array and map projection, file metadata,
directory metadata, and so on. This framework highlights a common theme in
this work: implement a feature once, do it completely, then unit test
exhaustively to ensure that the mechanism will work everywhere needed.

### Projection Push-Down Framework

Package: [`org/apache/drill/exec/physical/impl/scan/project/`](https://github.com/apache/drill/blob/master/exec/java-exec/src/main/java/org/apache/drill/exec/physical/impl/scan/project)

Most (all?) readers implement projection push-down. They all do it their own
way. New readers must implement their own solutions. (This will be a topic in
an upcoming book on Apache Drill.) Although the dozen implementations show a
great flowering of creativity, it would be a daunting task to upgrade so many
different implementations of the same basic idea.

The projection framework provides a single, standard implementation. Projection
turns out to be surprisingly complex because of Drill's schema-free nature.
Projected columns can be simple, maps or arrays. Drill creates nulls for missing
columns, so we need null simple, array and map columns. We must match up table
schema and the project list, dropping unwanted columns and adding nulls. Things
get particularly interesting if, say, the user requests `a.b, a.c`, but the
table provides only `a.b` and Drill must invent a map member `a.c`.

### Scan Operator

Package: [`org/apache/drill/exec/physical/impl/scan/`](https://github.com/apache/drill/blob/master/exec/java-exec/src/main/java/org/apache/drill/exec/physical/impl/scan)

The "classic" scan operator handles a variety of tasks making the code quite
complex. The revised version does just one thing, and does it well. It handles
the implementation of an operator that iterates over a set of readers,
using a scan framework, and returns the resulting batches. All of the work specific
to a kind of scan is factored into the scan framework described above. The work of
writing to vectors is factored out into the row set loader. The scan operator itself
is an implementation of the operator framework, described next.

## Operator Framework

Package: [`exec/java-exec/src/main/java/org/apache/drill/exec/physical/impl/protocol`](https://github.com/apache/drill/blob/master/exec/java-exec/src/main/java/org/apache/drill/exec/physical/impl/protocol)

Drill's operator implementations (so-called "record batches") also handle a
variety of tasks, making the implementations very complex and error-prone.
The operator framework tackles this complexity using composition. The framework
divides operator tasks into three parts: Drill iterator protocol, record batch
(that is, vector container) management, and operator implementation. The first
two can be common to all opeators, the third is unique to each.

An important aspect of this design (and the reason for this version) is that
it allows very convenient unit testing of each operator independent of the
rest of Drill. As code grows in complexity, it becomes necessary to hit it
with dozens of test cases in which we tightly control the test cases. That
is hard to do with server-level integration tests, but very easy to do with
JUnit tests.

The operator framework is meant to handle all operators, but has been put to
use for only the sort operator. Expect to extend or adjust the operator
framework to fit other use cases.

## Result Set Loader

Packages:
* [`org/apache/drill/test/rowSet/`](https://github.com/apache/drill/blob/master/exec/java-exec/src/test/java/org/apache/drill/test/rowSet)
* [`org/apache/drill/exec/physical/rowSet/model/`](https://github.com/apache/drill/blob/master/exec/java-exec/src/main/java/org/apache/drill/exec/physical/rowSet/model)

The result set loader is the core of this project. It provides a very simple
way to write data to the set of vectors that form a batch, and to write to a
sequence of batches (combined, the "result set.") The result set loader is
similar to the existing "complex writers", but with a number of differences.
First, the result set loader manages both the writers ("column writers" in
this case) and vectors (a job formerly handled by the scan operator's
`Mutator` class.)

The result set loader is the pivot for this project. All changes below this
level were done to provide the services that the row set loader needs to do
its job. Changes in the layers above were done to enable readers (and other
operators) to exploit the row set loader (and to reduce the cost of updating
operators to use the result set loader by factoring out redundant
implementations.)

The result set loader is schema-aware: it can work with "early schema"
(schema known up-front, as in Parquet) and "late schema" (schema discovered
on the fly, as in JSON.) It also implements part of the projection mechanism.
It allows writers to write all columns (such as in CSV), and will quietly
discard data for non-projected columns. This makes it trivially easy to create
a simple record reader: just read a row, write all columns, and let the row
set loader worry about which are actually needed. More sophisticated readers
(such as JSON or Parquet) can, of course, do the job their own way when needed.

The main feature of the result set loader is the ability to limit batch size
using a "roll-over" mechanism. That mechanism requires that the row set
loader manage vectors: creation, allocation and so on.

The "model" portion of this package provides tools to work with schemas: to
build readers and writers given a schema and so on. Two forms exist: one
for "single" batches and another for "hyper" batches.

## Row Set

Package: [`org/apache/drill/test/rowSet/`](https://github.com/apache/drill/blob/master/exec/java-exec/src/test/java/org/apache/drill/test/rowSet)

The row set tools have existed for a year or so and started as a way to
create or verify record batches during unit testing. The column accessors,
described below, started as the way that row sets work with vectors. As this
project progressed, the row set tests were the first line of testing for
each accessor change. The row set mechanism, including the ability to define
schemas, build row sets, print row sets, compare row sets and so on, are used
throughout the unit tests for the rest of the project.

The `RowSetWriter` has a very simple relationship with the `ResultSetLoader`.
The row set writer writes a single batch, with a fixed schema, then is done.
The `ResultSetLoader` adds the complexity that ensues when the schema evolves,
when writing multiple batches, and so on. Thus, to learn the mechanisms, start
with the row set writer. Then, once that is familiar, graduate up to the
result set loader.

## Column Accessors

Package: [`vector/org/apache/drill/exec/vector/accessor/`](https://github.com/apache/drill/blob/master/exec/vector/src/main/java/org/apache/drill/exec/vector/accessor)

The result set loader is basically an orchestrator. The actual work of writing
to vectors is done by a set of "column writers." There is a similar set of
readers. At this level, the readers and writers work with one batch at a time.
This turns out to be very handy in its own right: it has allowed creation of
the `RowSet` test framework that provides simple creation, display and comparison
of row sets. This feature is used in low-level unit tests of all of the above layers.

### Column Metadata

Packages:
* [`org/apache/drill/exec/record/metadata/`](https://github.com/apache/drill/blob/master/exec/java-exec/src/main/java/org/apache/drill/exec/record/metadata) (Implementation)

Metadata is an important part of any data representation. While Drill provides
one form (see below), that version turned out to be rather limited for the
purposes of this project. So, we developed a new set, called the column metadata.
It is defined in the vector layer (where the accessors reside), but implemented
in `java-exec` (where some of the required dependencies reside.) Over time, it
might be good to merge the two implementations, which can be done once both are
together in the master branch.

## Physical Vector Layer

All data in Drill is stored in vectors. Some adjustments were needed to this layer.

## Materialized Field Metadata

As noted above, the vector layer currently provides the `MaterializedField` class
that provides metadata for each vector. Adjustments where needed here to properly
implement comparisons, to handle metadata for nested vectors and so on.

## Vectors

In several cases, new methods were added here and there. The union vector was
heavily refurbished. The list vector was made to work (it was evidently not
previously used.) And so on.