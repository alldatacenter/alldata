# Column Accessor Metadata System

Metadata is the glue that allows the many batch handling components to
work together. There are several ways to categorize metadata:

* Descriptive: Describe existing data (that is, existing vectors or
an existing table).
* Proscriptive: Describe the vectors we wish to build.

Drill's existing metadata mechanisms are primarily focused on describing
vectors, and turned out to not be a good it for the "proscriptive" use
case above. Hence, the column accessor project created an extended metadata
system that builds on the existing system to allow more dynamic description
and creation of batches, readers, writers and so on.

At present, metadata groups into two distinct systems:

* Column metadata: used for proscriptive schemas and throughout the
column accessor code. Provides many semantic services needed to describe,
create and manage various kinds of metadata operations.
* Materialized field and batch schema: used to describe existing vectors
and record batches (so-called vector containers.)

This section describes the purpose, services and role of each kind of metadata.

## Describe a Vector

Drill already provides the [`MaterializedField`](https://github.com/apache/drill/blob/master/exec/vector/src/main/java/org/apache/drill/exec/record/MaterializedField.java) class to describe a vector. This class has three components:

* Name
* Major type
  * Minor (data) type
  * Data mode (cardinality)
  * Subtypes (for a union)
* Children (for a map, a repeated list or a list)

### Immutability

The materialized field is straightforward for simple types: `INT`, `VARCHAR`,
`INTERVAL` and so on. The original design appears to be that, because these
types are simple, a `MaterializedField` is immutable. Quite simple.

Drill also supports complex types: those that are made up of one column that
contains other columns. In Drill, the complex types are; Map, Repeated Map,
(non-repeated) List, Repeated List and Union. In this case, `MaterializedField`
must describe not only the complex vector itself, but also the children. (A map
must describe its members, for example.)

This requirement puts great strain on the concept that a `MaterializedField` is
immutable. A map, say, allows adding new members at any time. How do we add new
children to the map's `MaterializedField` if it is immutable? The solution
appears to be that the name and type are immutable, but the list of children
is not. It is a compromise, but it works.

Unions (though they are barely supported) introduce another issue. A union
contains a series of "subtypes" (vectors) that represent the data of the union.
Each row contains a value in one of the subtypes: in, say, `INT` or `VARCHAR`.
Unions allow the addition of subtypes at any time. Since the `MajorType` and
`MaterializedField` are immutable, now do we handle this? The answer has been
to create a new `MaterializedField` for the union vector.

Now things get interesting. A map can contain a union. The map's
`MaterializedField` holds a list of children, including the `MaterializedField`
for the union. If we replace the union's `MaterializedField` the map will
then hold onto a stale version and the schema becomes inconsistent.

The code for this project tries to find and fix these issues where they are
found. (But, we really should test every pair of complex types to ensure
that all cases are handled.) In general, the solution is to discard the
assumption that the `MaterializedField` is immutable, replacing with a
finer-grain set of invariants (such as that neither the name nor the data
type/mode can change.)

### Hidden (Implementation) Children

A nullable `INT` vector is, conceptually just a nullable `INT` type an a name.
But, the nullable vector is represented as a container with children for the
"bits" and "data" vectors. The actual `MaterializedField` contains these
hidden vectors as children. But, from a conceptual point of view, the
children are implementation details. One key place where this issue arrises
is in comparison. Is a nullable `INT` (without children) equal to a nullable
`INT` with children? For some purposes, yes. For others, no.

The recently-added `isEquivalent()` method on `MaterializedField` resolves
this ambiguity by considering children only for types in which children are
user-visible (maps, unions, etc.)

### Union Structure

The (obscure) [`UnionVector`](https://github.com/apache/drill/blob/master/exec/vector/src/main/codegen/templates/UnionVector.java) is a single vector that can hold multiple types. A row is one type or another. The structure here is a bit unclear. What is clear is that, for a union vector, the child types are stored as subtypes in the `MajorType`. The children are also stored, as materialized fields, but in an awkward structure. Like a nullable vector, union vector has internal structure stored as children in the the materialized field. The actual type vectors are stored two levels down, as children of an internal map that is the child of the union.

This structure works, but is very awkward for semantic processing because of
the amount of implementation detail that "shows through" into the metadata.

### List Structure

Drill has two kinds of lists. The "non-repeated" (and basically unsupported)
[`ListVector`](https://github.com/apache/drill/blob/master/exec/vector/src/main/java/org/apache/drill/exec/vector/complex/ListVector.java)
 and the [`RepeatedListVector`](https://github.com/apache/drill/blob/master/exec/vector/src/main/java/org/apache/drill/exec/vector/complex/RepeatedListVector.java).
 Despite the similar names, the are very separate concepts.

The list vector is quite odd. It has a "bits" vector that allows arrays to
be marked as null (as is needed for JSON.) It also can act as either a
"nullable repeated nullable X" or as a "repeated union". The materialized
field thus has a very complex structure.

When acting as "nullable repeated nullable X", the materialized field for
the "nullable X" appears as a child.

But, when the list is "promoted to union", then the child structure changes.
In this case, the union is the child of the list, and the "nullable X" is
pushed two levels down as described for unions above.

Again, this works, but becomes quite vexing for semantic processing.

### Limitations

The `MaterializedField` has a number of other limitations that make it
awkward to use for semantic processing:

* No convenient access to the most important properties: data type and
cardinality. Each access requires an awkward chain of calls.
* Inconvenient equality checks: equality is based on the equality of
Protobufs, which have very odd semantics (such as whether a field is set
or not.) Also, there is ambiguity with the hidden children.
* Very awkward access to children (only an iterator is provided, not
indexed access.)

To be fair, `MaterializedField` was never meant for the kind of semantic
processing we need. All of this is not meant as a critique of the code (the
code is fine), but rather as a motivation for the enhanced metadata mechanism
described below.

## Batch Schema

Drill represents a record batch (the data, not the operator) as a `VectorContainer`.
The container gathers up the schemas of its vectors into a `BatchSchema`.
However, the `BatchSchema` is a bit awkward, it is missing methods needed
for semantic processing.

## Column and Tuple Schema

For these reasons (and more), this project decided to create a new metadata
representation: one that is better suited to semantic processing. One of the
key design goals of this approach was to realize a very helpful generalization:
rows and maps are both tuples and should have the same metadata description.

The metadata layer is defined in the `vector` module alongside the
`MaterializedField` class, vectors and column accessors (described later).
However, they are implemented in the `java-exec` package, which is where
the `BatchSchema` is defined.

The two key interfaces are:

* `TupleMetadata` - generalized representation of a row or map.
* `ColumnMetadata` - generalized representation of a named column.

Two additional interfaces exist for Drill's obscure data types:

* `VariantMetadata` - represents a Union or a (non-repeated) List

The column/tuple metadata combine to form a simple tree structure, rooted in a
tuple, that can describe (possibly repeated) maps to any level of depth.
Include unions, and the tree can represent any combination of nesting of maps
in unions, unions in maps, and so on.

### Tuple Semantics

Traditional databases employ the "tuple" concept. In practice, a tuple is a
collection of values identified by both name an position. Consider JDBC as a
typical example, we can access a column either by name ("myCol", say) or by
position (4, say). Today Drill is a bit conflicted on indexed access.
`VectorContainer` provides indexed access, as do maps. Unions do not, nor
do `MaterializedField` child lists. Drill also provides name access, but it
is implemented as a linear search in some cases.

Since both name an indexed lookups are vital (name lookup for semantic
processing, index for performant runtime lookups), the tuple metadata
(as well as the column accessors) provide both indexed and name access.
Name access is via a hash map for performance.

### Simplified Schema Description

In addition to the row/map unification, the column metadata helps to smooth
over a number of other awkward bits in the `MaterializedField` representation:

* The tuple schema reifies the concept of a map schema in a form much more
convenient than the `MaterializedField` child iterator. The tuple schema
provides services to easily build, traverse and query a row or map schema.
* Many query methods for common conditions.
* Divide columns up into four broad groups: Primitive, Tuple, Variant and
Multi-Array. (The last represents a repeated list.)

### Services

The key job of the metadata interfaces are to provide semantic services such
as names, types, cardinality, default data width, default array sizes, and more.
In the implementation package, the RowSet version of the `SchemaBuilder`
provides a fluent way to define a schema for tests. Once you understand the
urpose of these classes, the JavaDoc in the class should provide the necessary
details.

One non-obvious service is to report, in the context of a scan operator, if a
column is projected. In this way, a reader can declare it's view of the schema.
(A CSV reader, say, can declare that it has 10 columns.) After schema
negotiation, the scan operator will determine that only three of the ten
columns are projected. The column metadata for the table (the CSV file)
will identify which are projected into the scan operator's output schema.
(More on this concept later.)

### Vector Allocation

When allocating a new vector, we often need to know up to three pieces of
information:

* The number of rows
* The width (for VarChar fields)
* The inner size (for arrays)

Given this information, Drill can very accurately allocate a vector of the
equired length. Doing so is important because it eliminates the doubling and
copying required with poor guesses. Most Drill code provides only the number
of rows, and guesses for the others. (This is where the infamous VarChar size
of 50 comes in, along with a guess of 5 for array sizes.) The sort operator took
an initial stab at an advanced batch allocator that uses the additional values
(which it obtained by introspecting an incoming batch using the `BatchSizer`.)

This mechanism goes a step further and adds the additional values to the
metadata itself. Over time, readers can determine the value through inspection,
and can (we hope) pass the values along with each batch so that other operators
don't need to repeated run the `BatchSizer` over and over. But, that is a
long-term goal.

### Reverse Engineering a Batch

Tools exist to examine a batch (using the `MaterializedField`s) and create the
batch metadata. This is done, for example, in the `RowSet` classes to provide
a simple schema description which is then used to generate readers or writers
for that batch.

### Forward Engineer a Batch From a Schema

Other tools (also in the `RowSet` tools) can build a batch of vectors from a
schema. Thus, the column metadata provide complete "round trip" engineering.
(However, at present, the additional fields (width and array size) are lost
in the process since `MaterializedField` does not support them.)

### Scan Schema Negotiation

The scan operator implements projection push-down in which the query (via the
project list) "negotiates" with the data source (via the reader) for the of
columns to provide. The scanner projects some, creates nulls for others, and
implicit columns for others. In the new framework, all of this work uses the
column/tuple metadata to represent a schema.

## Future Directions

Obviously it is less than ideal to have two distinct metadata systems. A good
long-term goal would be to merge the two systems. In particular,
`MaterializedField` would be replaced by the column metadata, and the
`BatchSchema` would be replaced by the tuple schema.

The rub, of course, is that such a change breaks the client/server API.
Changing value vector metadata would require changes to the JDBC and ODBC
clients to handle the new format. Without API versioning, this is a difficult
change. So, for now, we use two systems as that approach, while awkward,
does have the advantage of not breaking backward compatibility.