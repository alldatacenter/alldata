=====================================
Building with a chosen graph database
=====================================

The Atlas build is currently set up to include one of the graph backends in the Atlas war file.
The choice of graph backend is determined by the setting of the GRAPH-PROVIDER system variable.

If GRAPH-PROVIDER is not set, the default graph backend is adopted. This is currently JanusGraph 0.2.0

In order to build with a specific (non-default) graph backend set the GRAPH-PROVDER system variable.

If GRAPH-PROVIDER is set to janus, the build will contain JanusGraph 0.2.0 (i.e. the default above)

For example, to build Atlas with the janus graph-provider:
mvn install [-P dist] -DGRAPH-PROVIDER=janus

JanusGraph support Gremlin3 only (and NOT Gremlin2).

REQUIREMENTS
------------
JanusGraph 0.2.0 require Java 8 to be used both when building and running Atlas.
Unless Java 8 is used, the janus module will not be built - this is checked by the maven-enforcer-plugin.
