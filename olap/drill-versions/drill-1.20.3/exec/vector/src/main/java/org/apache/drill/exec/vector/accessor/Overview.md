# Conceptual Overview

Memory management is a core service in a query engine. The column acessor
project came about in support of enhancement memory management in Drill.
This page discusses the motivation for the project. A more detailed version
of this information can be found in
[DRILL-5211](https://issues.apache.org/jira/browse/DRILL-5211).

## Original Drill Memory Management

The original Drill design appeared to be based on two complementary assumptions:

* The user will ensure that Drill is provided with sufficient memory for each query and,
* Memory management is best handled as a negotiation among operators within a minor fragment.

### User-Centric Memory Management

Drill is an in-memory, big-data query engine. Drill benefits from generous
memory allocation. As originally designed, all operators work in memory. For
example, to sort 1 TB of data, Drill would need 1 TB of memory (distributed
across all nodes) to hold the data in memory during the sort. Since only the
user knows the size of the queries to be run, and the user can make decisions
about number of nodes, and memory per node, then the user is really the best
one to ensure that Drill is provided with sufficient memory to run the desired
queries.

While this is a very simple and clear design, it turned out to not be workable
in a multi-user environment: there is simply no good way for multiple users
to coordinate to decide on memory usage, or to ensure that a query from one
user does not use memory that another users may be planning to use.

### Operator Negotiation

Drill uses direct (not heap) memory to hold query data. Drill provides a very
sophisticated memory manager to allocate, track, share, and release direct
memory buffers. One of the features of the memory manager is a per-operator
allocator which enforces a memory limit. For example, operator X may be given
1 GB of memory. The allocator will raise an exception if operator X asks for
even a single byte over the limit. Operator X can also receive an OOM if either
1) the entire global memory pool is exhausted, or 2) the memory allocated to
the minor fragment is exhausted.

The original design appeared to be that each operator would catch the
out-of-memory (OOM) exceptions, then negotiate with other operators for
additional memory by sending a `OUT_OF_MEMORY` status to downstream operators.
For example, a reader that runs out of memory can stop reading the current row,
send the OOM status downstream, then resume reading the row once it is called
again (presumably with more memory.)

In practice, the mechanism was never fully implemented, nor is it clear that
it could actually work. If, for example, operator X exhausts its local memory
limit, sending OOM downstream won't change that fact. Operator code is made
vastly more complex if each memory allocation is required to stop processing
if OOM is returned. Further, most operators don't have much flexibility in
managing memory: most operators work with a single batch and can't do useful
work without sufficient memory for that batch. Buffering operators (sort,
joins, etc.) might be able to take action in response to memory pressure
(such as spilling). Since operators share a global memory pool, there is no
good way for a sort for query Q1 to respond to memory pressures created by
another sort in query Q1.

In short, the idea of operators negotiating memory among themselves is elegant,
it is not entirely clear how to write code to realize the idea.

## Netty Slab Size

Drill uses Netty as its memory allocator. Any memory management solution should
also be aware of a feature of Netty: the Netty slab size. Netty contains a
[jemalloc](http://jemalloc.net)-like memory allocator that works with
fixed-size blocks (subdivided into power-of-two slices). The default size of a
Netty block is 16 MB (though the size can be configured.) If an application asks
or memory larger than the block size, Netty allocates the memory from the OS.
This leads to a possible fragmentation scenario: all memory may be on a Netty free
list where it is available for allocations of 16 MB or smaller, but none is
available from the OS for allocations of 32 MB or larger. (All allocations must
be in power-of-two sizes.)

One possible solution is to increase the Netty slab size: but what is the largest
size Drill will need? The maximum Drill vector size is 2 GB, so should the block
size be that large? Are there reasons, aside from Netty concerns, to limit
allocations in Drill? We'll discuss that issue below.

Another possible solution is to find a way to force Netty to release unused
blocks back to the OS. The
[jemalloc paper](https://people.freebsd.org/~jasone/jemalloc/bsdcan2006/jemalloc.pdf)
suggests that the Linux implementation does, in fact, release empty blocks.
Perhaps Netty does also, with proper configuration. However, this still does
not address the issue that, due to some use pattern, each Netty block is
partially allocated. In that case, no free blocks can be released.

The alert reader may be wondering how Java avoids these issues with Java
objects. The answer is that the Java heap-based memory allocator is able
to reallocate objects to coalesce free regions to create a single, large
free block. Netty does not (and, indeed, cannot) provide this feature for
direct memory.

The key design constraint highlighted by this discussion is: Drill should
avoid allocating buffers larger than 16 MB. This won't solve all fragmentation
issues, but it will help reduce the problems related to OS/Netty free list
competition.

## Classic Database Memory Management: Fixed Size Blocks

It is worth remembering that the
[classic approach](http://users.informatik.uni-halle.de/~hinnebur/Lehre/2008_db_iib_web/uebung3_p560-effelsberg.pdf)
to database memory management is to create a *buffer pool* of fixed-size
blocks. In a classic DB, the blocks correspond to disk pages, allowing very
elegant paging of database page into and out of memory. The same system works
for temporary results (which can be materialized as temporary files as needed
in response to memory pressures.)

Indeed, the paper points out the classic problem with Drill's approach to
variable-size block allocation:

> Since pages cannot be displaced deliberately, variable-size pages would
cause heavy fragmentation problems.

Indeed, we cannot displace Drill's memory pages (blocks), Drill's blocks are
variable-size, and we do see memory fragmentation problems under heavy load.

Because Drill's design is now quite mature, we are not in a position to move to
fixed-size pages (at least not in a single move.) We will return to this topic,
however, in the conclusion to see how the mechanism described here can eventually
allow the use of fixed-size pages, if doing so is ever desired.

## Requirements

From the above discussion, we can extract two key requirements:

* Provide a mechanism to control memory use so that queries succeed (or at
least don't fail due to unexpected memory consumption).
* Work around the memory fragmentation issues inherent in the variable-size-page,
unlimited-page-size, two-level memory allocator which Drill currently employs.

## Budget-Based Memory Management

To address the first requirement, Drill has discussed the idea of moving to a
budget-based memory management concept. In brief, the idea is that each query
is given some memory budget (using a mechanism discussed below). Once the
query is given a memory budget, two things must follow:

* The memory must actually be available (prevent the over-subscription of
physical memory inherent in the original Drill memory allocation design).
* Queries must implement mechanisms (such as spilling, reduced parallelism,
etc.) to stay within their budget.

### Admission Control

The above is not quite complete, however. We must consider another factor:
how is the budget provided? Drill currently allows an unlimited number of
queries to run. How does one set a budget if one does not know the number
of queries that will arrive?

The solution is to move to a throttling model similar to that in most other
query engines. The system *admits* queries until resources are consumed, then
enqueues additional queries until resources free up. Since it is quite
difficult to predict the actual resource consumption of a query, we might
use a hybrid model:

* The planner produces an order-of-magnitude estimate of memory needs for
each query.
* The scheduler (*admission control* mechanism) assigns an actual budget
and admits the query only when that budget can be provided.
* The query operates with the budget, even if the budget is not ideal, by
spilling to disk or other means.

### Divide and Conquer

In this way, we divide scheduling into bite-size chunks, each of which
can be refined in parallel:

* How to more accurately estimate the memory needs of a query.
* If a system is overloaded (queries are queued), how does the system determine
which query to admit next (that is, establish a priority for queries, perhaps
based on user, size, application, etc.)
* If memory management works at the system level, then add resource pools so
that each is given a slice of resources, and manages its own slice separately.
* How to improve spilling performance, how to avoid spilling, and so on.

The budget-based approach is used in many products and is very well
understood. However, we should always keep an open mind: perhaps there is
some way to make the original design work. Perhaps Drill will end up being
used only in use cases in which memory management, to this degree, is not
important. So, this is just one possible design; we should always consider
alternatives.

## Budget vs. Reactive Approaches

It may be worth pointing out an intuitive alternative to the budget-based
approach. The budget approach says that we allocate a fixed memory allotment
up front, then a query must operate within that allotment. But, there is an
alternative (tried, then abandoned by Impala), that uses a reactive approach.
Why not launch a query, observe the amount of resources it actually uses, then
admit new queries based on actual usage? Very easy, right?

There are two problems.

First, the very nature of a "memory-challenged" query (one that does heavy
buffering) is that it accumulates data as it runs. The longer it runs, the
more data it accumulates. The memory use early in the run is a poor predictor
of the memory use later in the (as more data arrives.) Thus, simply observing
memory use a point in time (especially early in the run) will cause us to assume
that memory is available for other uses, only to be proved wrong when the query's
memory use grows.

In the controller community, such an approach is called an "open loop" system
and such systems are prone to oscillations for the very reasons described above.

The second problem is related: we are attracted to this solution because we
don't have to figure out how much memory a query needs (or should be given),
we just watch it to see what it uses. But, in such an environment, how would
we ever know if we can admit a second query? How do we know if the first query
has left enough resources for the second query to run successfully? In fact,
we don't. We'd have to guess, and if we guess wrong, then the system may run
out of memory, queries will fail, and users must retry.

But, since the problem of OOM and retry is the very problem we are trying
to solve, moving to an open-loop controller design really buys us nothing.

## Fragment Budget

Drill is columnar and works with data in *batches*: a set of rows implemented
as a collection of *value vectors* which implement columns.

Returning to the budget-based approach, it turns out we have two distinct
challenges when setting memory budget:

* For buffering operators (those that work with variable numbers of batches),
determine the number of batches that can be in memory before spilling must start.
* For single-batch (sometimes 2- or 3-batch) operators, how big should each batch be?

Given the above decisions, we can work out the total budget. (Or, said another
way, given a total budget, we can work out the actual numbers for the budget
for each operator.)

Let's focus on a single minor fragment, and let's assume that all buffering
operators are to use the same amount of memory. (The solution can easily be
extended for the more general case.) Let:
```
B = the number of buffering operators (sort, hash agg, hash join, etc.)
F = the number of fixed-count operators (project, SVR, filter, screen, etc.)
```
We can observe that Drill runs the operators as a stack. Only one batch is in
flight (though no batch may be in flight if a buffering operator is "doing
its thing" such as sorting.)

So, we need to allocate memory to hold enough batches for the larges of the
fixed-count operators F. Let:
```
S = the size of each batch
N = the number of batches used by the largest fixed operator (such as 2 for
    SRV or project, 1 for filter, etc.)
```
So, our fragment needs, at a bare minimum memory, S*N bytes just to run
batches up the operator tree.

But, of course, we want our buffering operators to store as many in-memory
batches as possible. Let:
```
M = the number of batches buffered by each buffering operator.
```
So, our buffering operators need B*M*S bytes to hold the desired number
of batches.

From this, we can figure out the total budget. Let
```
T = total fragment budget

T = B\*N + B\*M\*S = B \* (N + M\*S)
```
Here, we see that the batch size, B, is a key piece of information.

## Controlling Batch Size

At present, Drill uses record counts to control the batch size. This
means that the size of each batch is:
```
R = Number of rows in each batch (often 1000, 1K, 4K, 8K or 64K)
W = The width of each row (which depends entirely on the input data)

S = R*W
```
But, as noted, Drill has no control over row width; that depends on the
user's data. (Perhaps Drill could advise users to keep rows small, or
refuse to read rows above some maximum width.)

Better is to control that which Drill has the ability to control: the number
of rows per batch. Thus, rather than fixing the row count (and requiring the
user to constrain row width to match), derive the row count from the batch
size budget and observed row width. That is:
```
R = S/W
```
## Setting the Fragment Budget

Once we can control batch size, budgeting becomes simple. We define a batch
size S and limit batches to fit that size. The needs of the fixed-size operators
is, well, fixed, so we treat those as a constant. That simply leaves working
out the memory per buffering operator:
```
T = B*N + B*M*S

T - B*N = B*M*S

M = (T - B*N) / B*S
```
This says that if we know the number of buffering operators, the number of
batches needed by fixed-size operators, the total memory and the batch size,
then we can work out how many batches each buffering operator is allowed to
store. The result is a complete fragment budget based on first principles.

## Batch Size Control in Practice

One of the key goals of this project is to create a mechanism to implement
this idea. The concept is simple: limit each batch to the number of rows that
fit in some batch size budget. In practice, we are faced with a number of challenges:

* Drill allocation is lumpy: vectors grow in powers-of-two.
* Effective batch size control requires that we check memory *before* we double
a vector size, not after.
* Row width is unknown (especially in readers): we must read the data to learn
its size.
* But, if we read data, we need a place to put it. If the data is too large for
the current batch, we must store it somewhere (we can't "push" it back onto
the input.)

At the same time:

* Handle all manner of Drill types, including arrays, arrays of maps and
other complex types.
* Ensure that no single vector exceeds the Netty block size (default of
16 MB).

We must also consider the consumer of the system: the reader or the internal
(non-scan) operator. All operators (and readers) work row-by-row; they cannot
easily stop in the middle of a row if a batch becomes full. That is, every
operator must be able to write code that looks something like this:

```
void doBatch() {
  while (! batchIsFull) {
    for each column {
      process column
    }
  }
```

Just to be crystal clear, let's state this in yet another way:

* Drill stores data in vectors.
* Vectors double in size as needed to hold more values.
* We want to prevent the final doubling that would exceed our batch (or vector) size target.
* We will thus exceed our batch size target on some column with some row.
* Writers (operators, readers) cannot stop part-way through a row, they must write an entire row.

A key feature (and source of complexity) in the present solution is that the mechanism
handles "overflow": it quietly sets aside that row that causes the size limit to be
exceeded, and instead includes that row in the following batch, ensuring that the
current batch stays within the defined limit.

## Design Approach

To better understand the code, it helps to understand the design
philosophy behind the code. Here are a few guiding principles.

* Design for the long term. Determine what we'll need in a production
system and work toward that. Avoid short-term hacks.
* Minimize functional change. Our goal is to limit batch size by
upgrading certain parts of Drill. We tried to minimize user-visible
changes as such changes, even when useful, require extensive discussion
and vetting. Better to make the core changes in one project (this one),
then do a later project to roll out user-visible imrpovements (such as
those to improve JSON.)
* Let the code tell us what works. The project tried various approaches.
The one that was cleanest, simplest, and easiest to test won out.
* Keep it simple. Complex, deeply nested functions. Classes that do many
things. State maintained by many flags. All are the path to chaos. Instead,
this project used small, focused classes. Composition is used to build up
complex functionality from simple pieces. Clean APIs are preferred over
complex coupling of components.
* Test. Unit testing is an integral part of the project. Every component
is wrapped in tests. It is not quite TDD (test-driven development), but
it is absolutely of the form "write a bit of code, test it, write a bit
more and repeat."
* Loose Coupling. Each component is loosely coupled to others. This allows
unit testing, but it also makes it much easier to reason about each component.
* Document. These notes. The specs. Extensive code comments. They not only
help future readers such as yourself, they also served
as a way to think through the design issues when writing the code.

## Summary: The Big Picture

Let's put all the pieces together.

Because batch size is precisely controlled, the fragment budget calculations work, and
we can set a per-fragment budget. Because we can set a per-fragment budget, we
can set a per-query budget. With the per-query budget, we can effectively
implement admission control to control the total queries. The result is that
Drill queries never fail with OOM errors and the user experience is enhanced
because users never see (memory related) query failures.

That is quite a chain of consequences; which is why it is vital to implement
the batch-size limitation mechanism.