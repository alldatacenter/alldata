# Future Directions

We've now taken the grand tour though the entire project and we
can discuss futures.

## Completing the Work

The discussions here discussed not just how the code works, but *why* it was
created as it was. The project spanned many months during which many issues
arose, were discussed, and solutions adopted. For this reason, the project,
as it exists, evolved to solve very specific issues that Drill presents.

The path of least resistance is to complete the roll-out of the work up
through the CSV and JSON readers, then to upgrade other important readers,
uch as Parquet.

Once the core readers are upgraded, attention can turn to other readers and
operators as discussed below.

## Next Steps

After the present work is committed, two paths become open to the team:

* Upgrade other readers
* Upgrade other operators

### Upgrade Readers

The project chose to start with CSV (actually, the "compliant text") reader
because it is simple and proved the basic concept. The project then tackled JSON
because it is the hardest. (As noted, the work identified many unresolved design
issues in the JSON reader.) All other readers fall somewhere in between.

Readers where the focus of this project because readers are the largest source of
batch size problems. While alternative solutions exist for internal (non-scan)
operators, no solution exists for readers since, in general, readers do not know
the size of incoming data until it is read.

The logical progression is that each community member upgrades the readers that
are important to them. MapR is Drill's primary sponsor, so perhaps MapR upgrades
readers that are commercially important to MapR (such as the Parquet reader.)
Other teams, at their convenience, upgrade other readers.

The timing is flexible. Once the code is available, upgrades can happen at any
time. But, once Drill moves to create an admission control solution, then Drill
must be able to manage memory, and all readers must be upgraded or deprecated
t that time. If any reader declines to limit its batch size, then the entire
query may die because the fragment (and each operator in the fragment) will be
forced to handle data beyond the budgeted memory size. The only three solutions
at that time will be to either:

* Turn off admission control for "unmanaged" readers.
* Disable unmanaged readers when admission control is enabled.
* Upgrade unmanaged readers so that they manage batch size.

### Upgrade Other Operators

Drills internal (non-scan) operators must also be upgraded, but here we have
two choices.

* Use the result set loader to write batches (and the corresponding reader
to read batches), or
* Use the "batch sizer" solutions employed in sort and hash agg to implement
ad-hoc solutions. (This is the solution actually adopted.)

Frankly, the "batch sizer" solution is the simplest short-term solution if
our focus is exclusively batch size control. (Efforts are underway using
this approach.)

The cost of upgrading existing operators will be high because:

* The generated code that works with vectors must be replaced by generated
code that works with result set readers and writers.
* The code generation mechanism, including semantic analysis, must be changed
to understand the higher-level view of batches.
* Mechanisms that currently work at the vector level (such as transfer pairs)
must be evolved to work with vectors created by the result set loader.
* The ad-hoc projection, batch assembly, vector allocation code should be
upgraded to use common mechanisms (perhaps derived from the work done for scans.)
* To enable this work (and unit testing) the operators should evolve to use
the common operator framework rather than providing ad-hoc iterator
implementations in each operator.

## Long-Term Architectural Goals

The above work is not justified just for batch size control since a simpler
lternative exists. Instead, the above work is justified only if we get more
value. Some long-term architectural advantages include:

* Once all operators use the result set readers and writers, we become free to
enhance the underlying vector storage format. Perhaps we move to Arrow or to
using fixed-sized buffers. Since only the generated accessor code deals with
the vector memory layout, such changes become much easier to make.
* If we with to improve code generation, we will have to understand and enhance
the code generation framework. (Perhaps we move to plain Java. Perhaps we
replace some generated code with "pre-written" code such as for copying rows
of data.) If we are going to revisit code generation, we might as well generate
code for the column accessors than continue to work directly with vectors.
* As Drill matures, thorough unit testing becomes the only reliable way to
ensure continued quality. The current approach of running a few system tests
is stochastic: maybe tests find problems and maybe they don't. We'd need
well-designed, blackbox tests that exercise all (or most) code paths. That
can only be done via unit tests. If we must modify operator structure to allow
such testing, we might as well use that opportunity to use the new operator
framework, which enables us to more easily move to use the new column accessors.

Each of the above deserves far more detail than can be provided here. This
should at least provide the flavor of the opportunities available.

These are all complex architectural issues which are not at all obvious from
a superficial task-oriented view of the code. For this reason, it may take years
to understand the need for, and move toward, the above approaches. This again
argues for using simpler (if less robust) solutions short-term.

## Toward Fixed-Size Memory Buffers

This project started, in part, from a desire to address memory fragmentation in
Drill. The project solves one aspect of fragmentation (limiting vector sizes to
the Netty block size or smaller.) But, it does not directly solve a more
fundamental issue: that database experts have known since at least the '80s
that memory fragmentation is unavoidable in a system that does random allocations
or random sized blocks without compaction.

C++ applications are subject to memory fragmentation because the follow the above
patterns. Database systems written in C++ (such as Impala) solve the issue by
allocing fixed size "buffers" that reside in a "buffer pool." Database systems
have long shown that such a solution works well and avoids fragmentation.

### Drill Challenges

Java applications are not subject to fragmentation because the Java GC performs
compaction and coalescing of free blocks. This prevents the issue of fragmentation
by ensuring a steady supply of large spans of free memory (until the application
allocates so many blocks that it exhausts heap.) Many Java developers (including
Drill's) see GC as an unwanted cost, and look for alternatives.

Fixed-size blocks work well for row-based systems: the system simply packs a block
with rows until no more fit. Fixed-sized blocks are less obvious for columnar
systems. Each column grows independently and each has different sized data.
This is why Drill adopted a `malloc`-like approach. But, in so doing, Drill
became subject to memory fragmentation. Is this a Catch-22?

Drill uses direct memory to avoid GC costs. But, in so doing, Drill gave up
the solution that allows random-sized allocations to work: compaction. So, to
continue to use direct memory without compaction, Drill may find it necessary
to follow most other database systems and adopt a memory model based on
fixed-size blocks.

### Fixed-Size Blocks for Drill

One possible solution (for which a prototype exists) is to rethink the value
vector. Today each value vector is represented as a single, variable-sized
(but power-of-two sized) block. This leads to two issues:

* Fragmentation of the Netty free list.
* Excessive internal fragmentation (on average, 25% of each vector is unused).

This model has the advantage of representing vectors as, essentially, Netty
byte buffers. But, Netty points to an alternative solution: a composite byte buffer.

In Netty, we can have a logical byte buffer which is actually comprised of
a series of underlying buffers. Netty does the required offset translations.

The prototype mentioned earlier does something similar, but with fixed size
blocks. Rather than a vector being a single block, it is physically comprised
of a string of fixed-size blocks:

```
Current:  [The quick brown fox jumps over the lazy dog.........]
Revised:
Logical:  [The quick brown fox jumps over the lazy dog.]
Physical: [The quic][k brown ][fox jump][s over t][he lazy ][dog.....]
```

The implementation can be based on power-of-two blocks. The example uses
8 bytes, but a real solution would use much larger. (The prototype found
that 1 MB blocks amortize costs and are at least as fast as the current
solution.) Power-of-two blocks allow a very simple indexing schema: just
mask and shift similar to how SV4 vectors work.

The prototype adds a simple caching scheme: writes happen sequentially,
so the block reference need be resolved only when a block becomes full.

### Simpler Memory Allocator

Once fixed size blocks are implemented, various memory management designs
become available. For example, if we decide to give each fragment x bytes
of memory, then we are effectively giving the fragment (x / block size)
blocks of memory. A block pool for the fragment can recycle those blocks:

```
Scan --> Filter --> SVR --> Sender
 ^                   ^        |
 |                   |        |
 |                   v        v
 +---- Block pool ---+--------+
```

Such a memory allocator would be, essentially, lock-free for all but the
sender. (Sent blocks would be released in a Netty thread.) Since Drill's
current allocator is designed to be lock-free, we would retain this benefit
in the new solution.

Further, the allocator becomes **far** simpler: we no longer need to
anage variable-sized blocks at the byte level, no longer need ledgers, no
longer need to manage operator allocations. All we need to do is manage the
overall fragment memory, which should be driven by the kind of memory
management plan discussed at the [start](Overview.md).

## Drive Adoption via Community Reader Contributions

To change topics just a bit, one of Drill's compelling advantages is that it
is written in Java and thus can enable community-provided extensions. Drill
already enjoys a variety of community-provided readers. However, there seems to
e general agreement that it is far harder than necessary to create a reader.
Each reader author must:

* Become familiar with Drill internals
* Learn how value vectors work, including the mutators
* Learn the scan operator and its mutator
* Learn how to do projection push-down
* Learn how the Easy format plugin mechanism works, or start from scratch
with the core format plugin mechanism
* Learn how file extensions work, how plan time operations work, etc.
* Figure out some way to debug the reader, often within a complete Drill
server.

One of the background goals of this project is to make it far easier to
create a format plugin. This work can't solve all the issues, but it did
attempt to do the following:

* Provide a very simple API for most cases.
* Allow readers to focus on getting data from the input source and writing
it to the result set loader. The scan framework does all the other standard
work.

A good task for a community member would be, once the code is available,
to create an example reader using the new framework, then publicize that
example to encourage new reader contributions.

## Conclusion

And so we come full circle. We started with a desire to define a memory
budget for a fragment (and thus a query) and to avoid one form of memory
allocation. We worked out the technical mechanisms to do that. We have now
seen how those solutions allow us to move to a fixed-size-block structure
that sweeps away the remaining memory fragmentation issues, while resulting
in a much simpler, proven memory allocation model.

As the team moves forward with this project, please keep this big picture
in mind. By doing so, we can continue to march forward towards our common
goal of making Drill the best big-data query engine available.