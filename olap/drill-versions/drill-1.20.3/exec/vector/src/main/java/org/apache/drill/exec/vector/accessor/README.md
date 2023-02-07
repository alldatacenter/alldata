# Column Accessor Overview

The column accessor framework consists of a number of layers that combine to
enable Drill to control the size of each record batch, which in turn allows
Drill to implement effective memory management and admission control.

The material here starts with concepts, then provides a tour of the various
components. Each component is heavily commented, so after reading this
material, you should be able to get the details from the code itself.

The material here is a slightly-revised version of the material that
orginally appeared in the `paul-rogers/drill` Wiki.

* [Conceptual Overview](Overview.md)
* [Components](Components.md)
* [Metadata](Metadata.md)
* [Row Set Mechanism](RowSet.md)
* [Column Accessors](Accessors.md)
* [Column Readers](Readers.md)
* [Column Writers](Writers.md)
* [Future Work](Futures.md)

* [[Result Set Loader|BH Result Set Loader]]
* [[Operator Framework|BH Operator Framework]]
* [[Projection Framework|BH Projection Framework]]
* [[Scan Framework|BH Scan Framework]]
* [[Easy Format Plugin|BH Easy Format Plugin]]
* [[CSV (Compliant Text) Reader|BH Compliant Text Reader]]
* [[JSON Reader|BH JSON Reader]]

## Code Overview

The code for the mechanisms described here appears in multiple Drill modules,
along with its unit tests. It is described here in one place to provide
a complete overview.

It can be hard to understand complex code. To make doing so easier, we offer
three paths:

* This set of notes (plus additional material in the JIRA).
* Comments in the code.
* Unit tests which illustrate how to use each component.

The code itself has many comments and explanations; it is the best resource
to understand the detailed algorithms and approaches.

