==================
JanusGraph README
==================

IMPORTANT: Support for JanusGraph in Atlas is currently a work in progress.


ARCHITECTURE NOTES
------------------

To build Atlas with JanusGraph, you must set GRAPH-PROVIDER as follows:

mvn install [-P dist] -DGRAPH-PROVIDER=janus


JanusGraph support Gremlin3 only (and NOT Gremlin2), so the gremlin used by Atlas is translated into Gremlin3
by the GremlinExpressionFactory.



REQUIREMENTS
--------------

JanusGraph requires Java 8 to be used both when building and running Atlas.
Unless Java 8 is used, the janus module will not be built - this is checked by the maven-enforcer-plugin.


USING ATLAS WITH JANUS GRAPH
----------------------------

1) Build Atlas with the janus graph-provider maven profile enabled:

mvn install [-P dist] -DGRAPH-PROVIDER=janus

Some tests in the repository and webapp projects are skipped when running with the janus provider, due to hard
dependencies on Gremlin2. These components need to be updated. Please refer to "known issues" section below.

This will build Atlas and run all of the tests against Janus.

2) Configure the Atlas runtime to use JanusGraph by setting the atlas.graphdb.backend property in
ATLAS_HOME/conf/atlas-application.properties, as follows:

atlas.graphdb.backend=org.apache.atlas.repository.graphdb.janus.AtlasJanusGraphDatabase

3) Attempt to start the Atlas server.


KNOWN ISSUES
------------

None yet...