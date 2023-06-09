This directory contains source code artifacts to launch Drill in cluster mode
along with a ZooKeeper. The Drill image is based on a minor customisation of
the official Drill image that switches it from an embedded to a cluster mode
launch. Logging is redirected to stdout.

In the docker-cluster-mode directory:

1. docker build -t apache/drill-cluster-mode
2. docker-compose up

Then access the web UI at http://localhost:8047 or connect a JDBC client to
jdbc:drill:drillbit=localhost or jdbc:drill:zk=localhost but note that you
will need to make the drillbit container hostnames resolvable from the host
to use a ZooKeeper JDBC URL.

To launch a cluster of 3 Drillbits

3. docker-compose up --scale drillbit=3

but first note that to use docker-compose's "scale" feature to run multiple
Drillbit containers on a single host you will need to remove the host port
mappings from the compose file to prevent collisions (see the comments
on the relevant lines in that file). Once the Drillbits are launched run
`docker-compose ps` to list the ephemeral ports that have been allocated on
the host.
