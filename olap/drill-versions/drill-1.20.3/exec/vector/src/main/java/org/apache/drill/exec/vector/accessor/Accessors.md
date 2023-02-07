# Column Accessors

The column accessors implement a (relatively) simple API to work with a
collection of vectors. They form the foundation of both the row set
reader/writer and the result set loader.

The accessors started with the design of the existing "complex writers",
then evolved in a distinct direction as we describe here. Like the complex
writers, the column accessors somewhat follow the JSON model. While Drill
provides only a complex "writer" (but no reader), the column accessors provide
both readers and writers. As much as possible, the API is similar between the two.

This section describes the common structure; later pages dive into the
specifics of the writers and readers.

## Structure

Accessors are based on a structure, expressed here as pseudo-BNF:

```
Tuple : Object*
Object : Scalar | Array | Tuple | Variant
Scalar : Nullable Scalar | Non-Nullable Scalar
Array : Scalar Array | Map Array | List | Repeated List
Variant : Object*
Repeated List : Array
Scalar Array : Scalar
Map Array : Tuple
```

### Tuple

The tuple accessor represents both a row and a map and provides both name and
index access to a set of columns. In the writer, the tuple is the unit of schema
evolution, providing methods to add new columns on the fly.

### Object

The object accessor represents a column in Drill. That is, it represents a
vector. The object accessor wraps the actual accessor, whether it be a scalar,
array, map or whatever. The purpose of the object is to allow generic operations
on accessors: all members of a tuple are objects, one then queries the object for
its type and treats the column accordingly. In general, however, convenience
methods on the accessors hide the object concept when not needed.

The object accessor for a map wraps a tuple accessor, consistent with the idea
that, conceptually, maps and rows are both tuples.

### Array

The array is a generic concept and consists of two parts: the array and the
"entry". The array handles indexing over the array elements. The entry handles
access to each entry. The entry is just one of the other accessors. (For a repeated
list, it is just another array for example.) Thus, code that works with values
need not care if the value is a top-level column, a column within a map, or an
entry in an array.

### Variant

The variant accessor represents both a union and a (non-repeated) list. (The
list is a very odd duck.) The term "variant" is borrowed from Microsoft (which
borrowed it from earlier systems) and simply means a union with a type field.
The Drill term "union" was avoided for two reasons. 1) Constant confusion with
the UNION operator in SQL, and 2) The union implementation is quick and dirty
and seems like a good candidate for rethinking.

A variant is like a map, but it is a map keyed by type, not by name. When used
in a (non-repeated) list, the union can be empty, a single item (which, in the
list, is not a union at all) or a true union of two or more types. (Yes, indeed
the list vector is odd.)

## Object Type

Every accessor provides an
[`ObjectType`](https://github.com/apache/drill/blob/master/exec/vector/src/main/java/org/apache/drill/exec/vector/accessor/ObjectType.java)
value that describes the above accessor types. Thus, interpreted code can get
a column, check the object type, and decide how to proceed. The object type is
akin to the JSON object type, but with specific types tailored to Drill.

## Typed Access

Drill provides 30+ data types (not all of which are supported or even fully
implemented.) The "classic" complex writers provide a distinct writer class for
each and every type: `INT`, `SMALLINT`, nullable `INT`, repeated `INT`, and so
on. The column accessors take a more restrained approach. All scalar accessors
provide the same interface. The implementations differ, but client code need
not care. Each implementation provides implementations for one or more of the
get or set methods, throwing exceptions for the others. Further, the get and
set method revolve around Java types, not Drill types. So, for example,
`TINYINT`, `SMALLINT`, `INT`, `UINT1` and `UINT2` all implement the same
`setInt()` and `getInt()` methods.

### ValueType enum

To allow interpreted access, each scalar reader provides a value type
expressed as an enum:

```
  ValueType valueType();
```

The value type identifies which `get` or `set` method is implemented.
(Accessors are free to implement other methods in addition to the main
one indicated by the value type.)

### Open Issues

One open issue is how best to deal with `INTERVAL` and `DECIMAL` types. At
present, these are passed as Joda objects (`INTERVAL`, `DATE`, `TIME`) and
`BigDecimal` (`DECIMAL` types.) Production code needs a different representation
that avoids object allocations for every value. This is a straightforward
addition best done once we figure out the mechanism that will best work for
generated code. (The current technique in generated code, a separate `get()`
or `set()` call for each field, is inefficient.)

## Column Accessor, Metadata and Generic Access

All the accessors defined above derive from a common base class, either
`ColumnReader` or `ColumnWriter`. The base class provides generic services:

* Access to [[metadata|BH Metadata]] for the column
* The object type (described above)
* Generic `Object get()` and `set(Object value)` methods

## Index Abstraction

Readers and writes implement the concept of an "index": a pointer to the current
read or write position. Writers increment the index, readers allow random access.

Consider a set of three vectors: a, b, and c. In most Drill code, the application
keeps track of the index. Then, the application passes its index, (`rowIndex`,
say) to each vector for each access. For example: `a.get(rowIndex)` or
`b.set(rowIndex, value)`.

This code is tedious, but not hard. However, the complexity vastly increases
when we consider arrays, especially map arrays. In this case, the application
must keep track not only of a `rowIndex`, but also a `repeatedMap1Index`, a
`repeatedIntIndex` and so on. Keeping all this straight is fraught with
opportunities for bugs, especially since there is only exactly one right way
to do the work.

The accessors take over this issue. In arrays, the accessors introduce new
"local" indexes for the array. Child accessors use the "inner" index for array
items. If we have multiple levels of repeated map, then we have multiple
levels of index. The application does nothing other than tell the reader or
writer to advance.

Readers must deal with another level of complexity: selection vectors. The
reader index encapsulates this indirection: the application asks for row 7,
the reader figures out that this is actually row 1234 (for an SV2), or row
4567 of batch 17 (for an SV4.)

In short, the index removes all the index complexity from operators and
readers and places it into the accessors where it can be implemented and
tested once and for all.

## Examples

The `RowSetTest` class shows examples of working with readers and writers
directly. The `RowSetBuilder` shows an example of generic (interpreted) use
of the writer. `RowSetPrinter` and `RowSetComparison` show interpreted use
of the reader.