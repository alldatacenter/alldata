---
name: High Availability
route: /HighAvailability
menu: Documentation
submenu: Features
---

import  themen  from 'theme/styles/styled-colors';
import  * as theme  from 'react-syntax-highlighter/dist/esm/styles/hljs';
import SyntaxHighlighter from 'react-syntax-highlighter';

# Fault Tolerance and High Availability Options

## Introduction

Apache Atlas uses and interacts with a variety of systems to provide metadata management and data lineage to data
administrators. By choosing and configuring these dependencies appropriately, it is possible to achieve a high degree
of service availability with Atlas. This document describes the state of high availability support in Atlas,
including its capabilities and current limitations, and also the configuration required for achieving this level of
high availability.

[The architecture page](#/Architecture) in the wiki gives an overview of the various components that make up Atlas.
The options mentioned below for various components derive context from the above page, and would be worthwhile to
review before proceeding to read this page.

## Atlas Web Service

Currently, the Atlas Web Service has a limitation that it can only have one active instance at a time. In earlier
releases of Atlas, a backup instance could be provisioned and kept available. However, a manual failover was
required to make this backup instance active.

From this release, Atlas will support multiple instances of the Atlas Web service in an active/passive configuration
with automated failover. This means that users can deploy and start multiple instances of the Atlas Web Service on
different physical hosts at the same time. One of these instances will be automatically selected as an 'active'
instance to service user requests. The others will automatically be deemed 'passive'. If the 'active' instance
becomes unavailable either because it is deliberately stopped, or due to unexpected failures, one of the other
instances will automatically be elected as an 'active' instance and start to service user requests.

An 'active' instance is the only instance that can respond to user requests correctly. It can create, delete, modify
or respond to queries on metadata objects. A 'passive' instance will accept user requests, but will redirect them
using HTTP redirect to the currently known 'active' instance. Specifically, a passive instance will not itself
respond to any queries on metadata objects. However, all instances (both active and passive), will respond to admin
requests that return information about that instance.

When configured in a High Availability mode, users can get the following operational benefits:

   * **Uninterrupted service during maintenance intervals**: If an active instance of the Atlas Web Service needs to be brought down for maintenance, another instance would automatically become active and can service requests.
   * **Uninterrupted service in event of unexpected failures**: If an active instance of the Atlas Web Service fails due to software or hardware errors, another instance would automatically become active and can service requests.

In the following sub-sections, we describe the steps required to setup High Availability for the Atlas Web Service.
We also describe how the deployment and client can be designed to take advantage of this capability.
Finally, we describe a few details of the underlying implementation.

### Setting up the High Availability feature in Atlas

The following pre-requisites must be met for setting up the High Availability feature.

   * Ensure that you install Apache Zookeeper on a cluster of machines (a minimum of 3 servers is recommended for production).
   * Select 2 or more physical machines to run the Atlas Web Service instances on. These machines define what we refer to as a 'server ensemble' for Atlas.


To setup High Availability in Atlas, a few configuration options must be defined in the `atlas-application.properties`
file. While the complete list of configuration items are defined in the [Configuration Page](#/Configuration), this
section lists a few of the main options.


* High Availability is an optional feature in Atlas. Hence, it must be enabled by setting the configuration option `atlas.server.ha.enabled` to true.


* Next, define a list of identifiers, one for each physical machine you have selected for the Atlas Web Service instance. These identifiers can be simple strings like `id1`, `id2` etc. They should be unique and should not contain a comma.


* Define a comma separated list of these identifiers as the value of the option `atlas.server.ids`.


* For each physical machine, list the IP Address/hostname and port as the value of the configuration `atlas.server.address.id`, where `id` refers to the identifier string for this physical machine.
   * For e.g., if you have selected 2 machines with hostnames `host1.company.com` and `host2.company.com`, you can define the configuration options as below:

<SyntaxHighlighter wrapLines={true} language="java" style={theme.dark}>
{`atlas.server.ids=id1,id2
atlas.server.address.id1=host1.company.com:21000
atlas.server.address.id2=host2.company.com:21000`}
</SyntaxHighlighter>

   * Define the Zookeeper quorum which will be used by the Atlas High Availability feature.

<SyntaxHighlighter wrapLines={true} language="java" style={theme.dark}>
      atlas.server.ha.zookeeper.connect=zk1.company.com:2181,zk2.company.com:2181,zk3.company.com:2181
    </SyntaxHighlighter>

   * You can review other configuration options that are defined for the High Availability feature, and set them up as desired in the `atlas-application.properties` file.
   * For production environments, the components that Atlas depends on must also be set up in High Availability mode. This is described in detail in the following sections. Follow those instructions to setup and configure them.
   * Install the Atlas software on the selected physical machines.
   * Copy the `atlas-application.properties` file created using the steps above to the configuration directory of all the machines.
   * Start the dependent components.
   * Start each instance of the Atlas Web Service.

To verify that High Availability is working, run the following script on each of the instances where Atlas Web Service
is installed.

<SyntaxHighlighter wrapLines={true} language="java" style={theme.dark}>
$ATLAS_HOME/bin/atlas_admin.py -status
</SyntaxHighlighter>
This script can print one of the values below as response:

   * **ACTIVE**: This instance is active and can respond to user requests.
   * **PASSIVE**: This instance is PASSIVE. It will redirect any user requests it receives to the current active instance.
   * **BECOMING_ACTIVE**: This would be printed if the server is transitioning to become an ACTIVE instance. The server cannot service any metadata user requests in this state.
   * **BECOMING_PASSIVE**: This would be printed if the server is transitioning to become a PASSIVE instance. The server cannot service any metadata user requests in this state.

Under normal operating circumstances, only one of these instances should print the value *ACTIVE* as response to
the script, and the others would print *PASSIVE*.

### Configuring clients to use the High Availability feature

The Atlas Web Service can be accessed in two ways:

   * **Using the Atlas Web UI**: This is a browser based client that can be used to query the metadata stored in Atlas.
   * **Using the Atlas REST API**: As Atlas exposes a RESTful API, one can use any standard REST client including libraries in other applications. In fact, Atlas ships with a client called AtlasClient that can be used as an example to build REST client access.

In order to take advantage of the High Availability feature in the clients, there are two options possible.

#### Using an intermediate proxy

The simplest solution to enable highly available access to Atlas is to install and configure some intermediate proxy
that has a capability to transparently switch services based on status. One such proxy solution is [HAProxy](http://www.haproxy.org/).

Here is an example HAProxy configuration that can be used. Note this is provided for illustration only, and not as a
recommended production configuration. For that, please refer to the HAProxy documentation for appropriate instructions.

<SyntaxHighlighter wrapLines={true} language="java" style={theme.dark}>
{`frontend atlas_fe
  bind *:41000
  default_backend atlas_be
backend atlas_be
  mode http
  option httpchk get /api/atlas/admin/status
  http-check expect string ACTIVE
  balance roundrobin
  server host1_21000 host1:21000 check
  server host2_21000 host2:21000 check backup
listen atlas
  bind localhost:42000`}
</SyntaxHighlighter>

The above configuration binds HAProxy to listen on port 41000 for incoming client connections. It then routes
the connections to either of the hosts host1 or host2 depending on a HTTP status check. The status check is
done using a HTTP GET on the REST URL `/api/atlas/admin/status`, and is deemed successful only if the HTTP response
contains the string ACTIVE.

#### Using automatic detection of active instance

If one does not want to setup and manage a separate proxy, then the other option to use the High Availability
feature is to build a client application that is capable of detecting status and retrying operations. In such a
setting, the client application can be launched with the URLs of all Atlas Web Service instances that form the
ensemble. The client should then call the REST URL `/api/atlas/admin/status` on each of these to determine which is
the active instance. The response from the Active instance would be of the form `{Status:ACTIVE}`. Also, when the
client faces any exceptions in the course of an operation, it should again determine which of the remaining URLs
is active and retry the operation.

The AtlasClient class that ships with Atlas can be used as an example client library that implements the logic
for working with an ensemble and selecting the right Active server instance.

Utilities in Atlas, like `quick_start.py` and `import-hive.sh` can be configured to run with multiple server
URLs. When launched in this mode, the AtlasClient automatically selects and works with the current active instance.
If a proxy is set up in between, then its address can be used when running quick_start.py or import-hive.sh.

### Implementation Details of Atlas High Availability

The Atlas High Availability work is tracked under the master JIRA
[ATLAS-510](https://issues.apache.org/jira/browse/ATLAS-510).
The JIRAs filed under it have detailed information about how the High Availability feature has been implemented.
At a high level the following points can be called out:

   * The automatic selection of an Active instance, as well as automatic failover to a new Active instance happen through a leader election algorithm.
   * For leader election, we use the [Leader Latch Recipe](http://curator.apache.org/curator-recipes/leader-latch.html) of [Apache Curator](http://curator.apache.org)
   * The Active instance is the only one which initializes, modifies or reads state in the backend stores to keep them consistent.
   * Also, when an instance is elected as Active, it refreshes any cached information from the backend stores to get up to date.
   * A servlet filter ensures that only the active instance services user requests. If a passive instance receives these requests, it automatically redirects them to the current active instance.

## Metadata Store

As described above, Atlas uses JanusGraph to store the metadata it manages. By default, Atlas uses a standalone HBase
instance as the backing store for JanusGraph. In order to provide HA for the metadata store, we recommend that Atlas be
configured to use distributed HBase as the backing store for JanusGraph.  Doing this implies that you could benefit from the
HA guarantees HBase provides. In order to configure Atlas to use HBase in HA mode, do the following:

   * Choose an existing HBase cluster that is set up in HA mode to configure in Atlas (OR) Set up a new HBase cluster in [HA mode](http://hbase.apache.org/book.html#quickstart_fully_distributed).
      * If setting up HBase for Atlas, please following instructions listed for setting up HBase in the [Installation Steps](#/Installation).
   * We recommend using more than one HBase masters (at least 2) in the cluster on different physical hosts that use Zookeeper for coordination to provide redundancy and high availability of HBase.
      * Refer to the [Configuration page](#/Configuration) for the options to configure in atlas.properties to setup Atlas with HBase.

## Index Store

As described above, Atlas indexes metadata through JanusGraph to support full text search queries. In order to provide HA
for the index store, we recommend that Atlas be configured to use Solr or Elasticsearch as the backing index store for JanusGraph.

### Solr
In order to configure Atlas to use Solr in HA mode, do the following:

   * Choose an existing SolrCloud cluster setup in HA mode to configure in Atlas (OR) Set up a new [SolrCloud cluster](https://cwiki.apache.org/confluence/display/solr/SolrCloud).
      * Ensure Solr is brought up on at least 2 physical hosts for redundancy, and each host runs a Solr node.
      * We recommend the number of replicas to be set to at least 2 for redundancy.
   * Create the SolrCloud collections required by Atlas, as described in [Installation Steps](#/Installation)
   * Refer to the [Configuration page](#/Configuration) for the options to configure in atlas.properties to setup Atlas with Solr.

### Elasticsearch  (Tech Preview)
In order to configure Atlas to use Elasticsearch in HA mode, do the following:

   * Choose an existing Elasticsearch cluster setup, (OR) setup a new cluster [Elasticsearch cluster](https://www.elastic.co/guide/en/elasticsearch/reference/5.6/setup.html).
      * Ensure that Elasticsearch is brought up on at least five physical hosts for redundancy.
      * A replica count of 3 is recommended
   * Refer to the [Configuration page](#/Configuration) for the options to configure in atlas.properties to setup Atlas with Elasticsearch.

## Notification Server

Metadata notification events from Hooks are sent to Atlas by writing them to a Kafka topic called **ATLAS_HOOK**. Similarly, events from
Atlas to other integrating components like Ranger, are written to a Kafka topic called **ATLAS_ENTITIES**. Since Kafka
persists these messages, the events will not be lost even if the consumers are down as the events are being sent. In
addition, we recommend Kafka is also setup for fault tolerance so that it has higher availability guarantees. In order
to configure Atlas to use Kafka in HA mode, do the following:

* Choose an existing Kafka cluster set up in HA mode to configure in Atlas (OR) Set up a new Kafka cluster.


* We recommend that there are more than one Kafka brokers in the cluster on different physical hosts that use Zookeeper for coordination to provide redundancy and high availability of Kafka.
   * Setup at least 2 physical hosts for redundancy, each hosting a Kafka broker.


* Set up Kafka topics for Atlas usage:
   * The number of partitions for the ATLAS topics should be set to 1 (numPartitions)
   * Decide number of replicas for Kafka topic: Set this to at least 2 for redundancy.
   * Run the following commands:

<SyntaxHighlighter wrapLines={true} language="java" style={theme.dark}>
   {`$KAFKA_HOME/bin/kafka-topics.sh --create --zookeeper <list of zookeeper host:port entries> --topic ATLAS_HOOK --replication-factor <numReplicas> --partitions 1
   $KAFKA_HOME/bin/kafka-topics.sh --create --zookeeper <list of zookeeper host:port entries> --topic ATLAS_ENTITIES --replication-factor <numReplicas> --partitions 1
   Here KAFKA_HOME points to the Kafka installation directory.`}
</SyntaxHighlighter>

   * In atlas-application.properties, set the following configuration:

<SyntaxHighlighter wrapLines={true} language="java" style={theme.dark}>
   {`atlas.notification.embedded=false
   atlas.kafka.zookeeper.connect=<comma separated list of servers forming Zookeeper quorum used by Kafka>
   atlas.kafka.bootstrap.servers=<comma separated list of Kafka broker endpoints in host:port form> - Give at least 2 for redundancy.`}
</SyntaxHighlighter>

## Known Issues

   * If the HBase region servers hosting the Atlas table are down, Atlas would not be able to store or retrieve metadata from HBase until they are brought back online.
