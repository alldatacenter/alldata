---
name: Installation Instruction
route: /Installation
menu: Documentation
submenu: Setup
---


import  themen  from 'theme/styles/styled-colors';
import  * as theme  from 'react-syntax-highlighter/dist/esm/styles/hljs';
import SyntaxHighlighter from 'react-syntax-highlighter';


### Installing & Running Apache Atlas

#### Installing Apache Atlas
From the directory you would like Apache Atlas to be installed, run the following commands:

<SyntaxHighlighter wrapLines={true} language="powershell" style={theme.dark}>
{`tar -xzvf apache-atlas-{project.version}-server.tar.gz
cd apache-atlas-{project.version}`}
</SyntaxHighlighter>

#### Running Apache Atlas with Local Apache HBase & Apache Solr
To run Apache Atlas with local Apache HBase & Apache Solr instances that are started/stopped along with Atlas start/stop, run following commands:

<SyntaxHighlighter wrapLines={true} language="powershell" style={theme.dark}>
{`export MANAGE_LOCAL_HBASE=true
export MANAGE_LOCAL_SOLR=true
bin/atlas_start.py`}
</SyntaxHighlighter>

#### Running Apache Atlas with BerkeleyDB & Apache Solr
To run Apache Atlas with BerkeleyDB, and local instances of Apache Solr and Apache Zookeeper, run following commands:

<SyntaxHighlighter wrapLines={true} language="powershell" style={theme.dark}>
{`export MANAGE_LOCAL_SOLR=true
bin/atlas_start.py`}
</SyntaxHighlighter>

#### Using Apache Atlas

  * To verify if Apache Atlas server is up and running, run curl command as shown below:


<SyntaxHighlighter wrapLines={true} style={theme.dark}>
    {`curl -u username:password http://localhost:21000/api/atlas/admin/version
    {"Description":"Metadata Management and Data Governance Platform over Hadoop","Version":"2.2.0","Name":"apache-atlas"}`}
</SyntaxHighlighter>


  * Run quick start to load sample model and data

<SyntaxHighlighter wrapLines={true} language="powershell" style={theme.dark}>
  {`bin/quick_start.py
Enter username for atlas :-
Enter password for atlas :-`}
</SyntaxHighlighter>

  * Access Apache Atlas UI using a browser: http://localhost:21000

#### Stopping Apache Atlas Server
To stop Apache Atlas, run following command:

<SyntaxHighlighter wrapLines={true} language="powershell" style={theme.dark}>
{`bin/atlas_stop.py`}
</SyntaxHighlighter>


### Configuring Apache Atlas
By default config directory used by Apache Atlas is _{package dir}/conf_. To override this set environment variable ATLAS_CONF to the path of the conf dir.

Environment variables needed to run Apache Atlas can be set in _atlas-env.sh_ file in the conf directory. This file will be sourced by Apache Atlas scripts before any commands are executed. The following environment variables are available to set.

<SyntaxHighlighter wrapLines={true} language="powershell" style={theme.dark}>
{`# The java implementation to use. If JAVA_HOME is not found we expect java and jar to be in path
#export JAVA_HOME=

# any additional java opts you want to set. This will apply to both client and server operations
#export ATLAS_OPTS=

# any additional java opts that you want to set for client only
#export ATLAS_CLIENT_OPTS=

# java heap size we want to set for the client. Default is 1024MB
#export ATLAS_CLIENT_HEAP=

# any additional opts you want to set for atlas service.
#export ATLAS_SERVER_OPTS=

# java heap size we want to set for the atlas server. Default is 1024MB
#export ATLAS_SERVER_HEAP=

# What is is considered as atlas home dir. Default is the base location of the installed software
#export ATLAS_HOME_DIR=

# Where log files are stored. Defatult is logs directory under the base install location
#export ATLAS_LOG_DIR=

# Where pid files are stored. Defatult is logs directory under the base install location
#export ATLAS_PID_DIR=

# Where do you want to expand the war file. By Default it is in /server/webapp dir under the base install dir.
#export ATLAS_EXPANDED_WEBAPP_DIR=`}
</SyntaxHighlighter>

*Settings to support large number of metadata objects*

If you plan to store large number of metadata objects, it is recommended that you use values tuned for better GC performance of the JVM.

The following values are common server side options:

<SyntaxHighlighter wrapLines={true} language="powershell" style={theme.dark}>
{`export ATLAS_SERVER_OPTS="-server -XX:SoftRefLRUPolicyMSPerMB=0 -XX:+CMSClassUnloadingEnabled -XX:+UseConcMarkSweepGC -XX:+CMSParallelRemarkEnabled -XX:+PrintTenuringDistribution -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=dumps/atlas_server.hprof -Xloggc:logs/gc-worker.log -verbose:gc -XX:+UseGCLogFileRotation -XX:NumberOfGCLogFiles=10 -XX:GCLogFileSize=1m -XX:+PrintGCDetails -XX:+PrintHeapAtGC -XX:+PrintGCTimeStamps"`}
</SyntaxHighlighter>

The `-XX:SoftRefLRUPolicyMSPerMB` option was found to be particularly helpful to regulate GC performance for query heavy workloads with many concurrent users.

The following values are recommended for JDK 8:

<SyntaxHighlighter wrapLines={true} language="powershell" style={theme.dark}>
export ATLAS_SERVER_HEAP="-Xms15360m -Xmx15360m -XX:MaxNewSize=5120m -XX:MetaspaceSize=100M -XX:MaxMetaspaceSize=512m"
</SyntaxHighlighter>

*NOTE for Mac OS users*
If you are using a Mac OS, you will need to configure the ATLAS_SERVER_OPTS (explained above).

In _{package dir}/conf/atlas-env.sh_ uncomment the following line
<SyntaxHighlighter wrapLines={true} language="powershell" style={theme.dark}>
{`export ATLAS_SERVER_OPTS=`}
</SyntaxHighlighter>

and change it to look as below

<SyntaxHighlighter wrapLines={true} language="powershell" style={theme.dark}>
{`export ATLAS_SERVER_OPTS="-Djava.awt.headless=true -Djava.security.krb5.realm= -Djava.security.krb5.kdc="`}
</SyntaxHighlighter>

#### Configuring Apache HBase as the storage backend for the Graph Repository

By default, Apache Atlas uses JanusGraph as the graph repository and is the only graph repository implementation available currently. Apache HBase versions currently supported are 1.1.x. For configuring Apache Atlas graph persistence on Apache HBase, please see "Graph persistence engine - HBase" in the [Configuration](#/Configuration) section for more details.

Apache HBase tables used by Apache Atlas can be set using the following configurations:

<SyntaxHighlighter wrapLines={true} language="powershell" style={theme.dark}>
{`atlas.graph.storage.hbase.table=atlas
atlas.audit.hbase.tablename=apache_atlas_entity_audit`}
</SyntaxHighlighter>

#### Configuring Apache Solr as the indexing backend for the Graph Repository

By default, Apache Atlas uses JanusGraph as the graph repository and is the only graph repository implementation available currently. For configuring JanusGraph to work with Apache Solr, please follow the instructions below

  * Install Apache Solr if not already running. The version of Apache Solr supported is 5.5.1. Could be installed from http://archive.apache.org/dist/lucene/solr/5.5.1/solr-5.5.1.tgz



* Start Apache Solr in cloud mode.

SolrCloud mode uses a ZooKeeper Service as a highly available, central location for cluster management. For a small cluster, running with an existing ZooKeeper quorum should be fine. For larger clusters, you would want to run separate multiple ZooKeeper quorum with at least 3 servers.
  For more information, refer Apache Solr documentation - https://cwiki.apache.org/confluence/display/solr/SolrCloud


   * For e.g., to bring up an Apache Solr node listening on port 8983 on a machine, you can use the command:


  <SyntaxHighlighter wrapLines={true} language="powershell" style={theme.dark}>
    {`$SOLR_HOME/bin/solr start -c -z <zookeeper_host:port> -p 8983`}
  </SyntaxHighlighter>


   * Run the following commands from SOLR_BIN (e.g. $SOLR_HOME/bin) directory to create collections in Apache Solr corresponding to the indexes that Apache Atlas uses. In the case that the Apache Atlas and Apache Solr instances are on 2 different hosts, first copy the required configuration files from ATLAS_HOME/conf/solr on the Apache Atlas instance host to Apache Solr instance host. SOLR_CONF in the below mentioned commands refer to the directory where Apache Solr configuration files have been copied to on Apache Solr host:

<SyntaxHighlighter wrapLines={true} language="powershell" style={theme.dark}>
  {`$SOLR_BIN/solr create -c vertex_index -d SOLR_CONF -shards #numShards -replicationFactor #replicationFactor
$SOLR_BIN/solr create -c edge_index -d SOLR_CONF -shards #numShards -replicationFactor #replicationFactor
$SOLR_BIN/solr create -c fulltext_index -d SOLR_CONF -shards #numShards -replicationFactor #replicationFactor`}
</SyntaxHighlighter>

  Note: If numShards and replicationFactor are not specified, they default to 1 which suffices if you are trying out solr with ATLAS on a single node instance.
  Otherwise specify numShards according to the number of hosts that are in the Solr cluster and the maxShardsPerNode configuration.
  The number of shards cannot exceed the total number of Solr nodes in your SolrCloud cluster.

  The number of replicas (replicationFactor) can be set according to the redundancy required.

  Also note that Apache Solr will automatically be called to create the indexes when Apache Atlas server is started if the
  SOLR_BIN and SOLR_CONF environment variables are set and the search indexing backend is set to 'solr5'.

   * Change ATLAS configuration to point to Apache Solr instance setup. Please make sure the following configurations are set to the below values in ATLAS_HOME/conf/atlas-application.properties

<SyntaxHighlighter wrapLines={true} language="powershell" style={theme.dark}>
{`atlas.graph.index.search.backend=solr
atlas.graph.index.search.solr.mode=cloud
atlas.graph.index.search.solr.zookeeper-url=<the ZK quorum setup for solr as comma separated value> eg: 10.1.6.4:2181,10.1.6.5:2181
atlas.graph.index.search.solr.zookeeper-connect-timeout=<SolrCloud Zookeeper Connection Timeout>. Default value is 60000 ms
atlas.graph.index.search.solr.zookeeper-session-timeout=<SolrCloud Zookeeper Session Timeout>. Default value is 60000 ms`}
</SyntaxHighlighter>

For more information on JanusGraph solr configuration , please refer http://docs.janusgraph.org/0.2.0/solr.html

Pre-requisites for running Apache Solr in cloud mode
  * Memory - Apache Solr is both memory and CPU intensive. Make sure the server running Apache Solr has adequate memory, CPU and disk.
    Apache Solr works well with 32GB RAM. Plan to provide as much memory as possible to Apache Solr process
  * Disk - If the number of entities that need to be stored are large, plan to have at least 500 GB free space in the volume where Apache Solr is going to store the index data
  * SolrCloud has support for replication and sharding. It is highly recommended to use SolrCloud with at least two Apache Solr nodes running on different servers with replication enabled.
    If using SolrCloud, then you also need ZooKeeper installed and configured with 3 or 5 ZooKeeper nodes

  * Start Apache Solr in http mode - alternative setup to Solr in cloud mode.

  Solr Standalone is used for a single instance, and it keeps configuration information on the file system. It does not require zookeeper and provides high performance for medium size index.
  Can be consider as a good option for fast prototyping as well as valid configuration for development environments. In some cases it demonstrates a better performance than solr cloud mode in production grade setup of Atlas.

   * Change ATLAS configuration to point to Standalone Apache Solr instance setup. Please make sure the following configurations are set to the below values in ATLAS_HOME/conf/atlas-application.properties

<SyntaxHighlighter wrapLines={true} language="powershell" style={theme.dark}>
{`atlas.graph.index.search.backend=solr
atlas.graph.index.search.solr.mode=http
atlas.graph.index.search.solr.http-urls=<a single or list of URLs for the Solr instances must be provided.> eg: localhost:2181,10.1.6.5:2181`}
</SyntaxHighlighter>

  Note: Solr standalone can be run in embedded mode using `embedded-hbase-solr` profile.

*Configuring Elasticsearch as the indexing backend for the Graph Repository (Tech Preview)*

By default, Apache Atlas uses [JanusGraph](https://janusgraph.org/) as the graph repository and is the only graph repository implementation available currently. For configuring [JanusGraph](https://janusgraph.org/) to work with Elasticsearch, please follow the instructions below


   * Install an Elasticsearch cluster. The version currently supported is 5.6.4, and can be acquired from: https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-5.6.4.tar.gz


   * For simple testing a single Elasticsearch node can be started by using the 'elasticsearch' command in the bin directory of the Elasticsearch distribution.


   * Change Apache Atlas configuration to point to the Elasticsearch instance setup. Please make sure the following configurations are set to the below values in ATLAS_HOME/conf/atlas-application.properties


<SyntaxHighlighter wrapLines={true} language="powershell" style={theme.dark}>
{`atlas.graph.index.search.backend=elasticsearch
atlas.graph.index.search.hostname=<the hostname(s) of the Elasticsearch master nodes comma separated>
atlas.graph.index.search.elasticsearch.client-only=true`}
 </SyntaxHighlighter>

For more information on JanusGraph configuration for elasticsearch, please refer http://docs.janusgraph.org/0.2.0/elasticsearch.html

#### Configuring Kafka Topics

Apache Atlas uses Apache Kafka to ingest metadata from other components at runtime. This is described in the [Architecture](#/Architecture)
in more detail. Depending on the configuration of Apache Kafka, sometimes you might need to setup the topics explicitly before
using Apache Atlas. To do so, Apache Atlas provides a script =bin/atlas_kafka_setup.py= which can be run from Apache Atlas server. In some
environments, the hooks might start getting used first before Apache Atlas server itself is setup. In such cases, the topics
can be run on the hosts where hooks are installed using a similar script `hook-bin/atlas_kafka_setup_hook.py`. Both these
use configuration in `atlas-application.properties` for setting up the topics. Please refer to the [Configuration](#/Configuration)
for these details.

#### Setting up Apache Atlas
There are a few steps that setup dependencies of Apache Atlas. One such example is setting up the JanusGraph schema in the storage backend of choice. In a simple single server setup, these are automatically setup with default configuration when the server first accesses these dependencies.

However, there are scenarios when we may want to run setup steps explicitly as one time operations. For example, in a multiple server scenario using [High Availability](#/HighAvailability), it is preferable to run setup steps from one of the server instances the first time, and then start the services.

To run these steps one time, execute the command =bin/atlas_start.py -setup= from a single Apache Atlas server instance.

However, Apache Atlas server does take care of parallel executions of the setup steps. Also, running the setup steps multiple times is idempotent. Therefore, if one chooses to run the setup steps as part of server startup, for convenience, then they should enable the configuration option `atlas.server.run.setup.on.start` by defining it with the value `true` in the `atlas-application.properties` file.

### Examples: calling Apache Atlas REST APIs
Here are few examples of calling Apache Atlas REST APIs via curl command.
   * List the types in the repository

<SyntaxHighlighter wrapLines={true} language="powershell" style={theme.dark}>
  {`curl -u username:password http://localhost:21000/api/atlas/v2/types/typedefs/headers
    [ {"guid":"fa421be8-c21b-4cf8-a226-fdde559ad598","name":"Referenceable","category":"ENTITY"},
      {"guid":"7f3f5712-521d-450d-9bb2-ba996b6f2a4e","name":"Asset","category":"ENTITY"},
      {"guid":"84b02fa0-e2f4-4cc4-8b24-d2371cd00375","name":"DataSet","category":"ENTITY"},
      {"guid":"f93975d5-5a5c-41da-ad9d-eb7c4f91a093","name":"Process","category":"ENTITY"},
      {"guid":"79dcd1f9-f350-4f7b-b706-5bab416f8206","name":"Infrastructure","category":"ENTITY"}
    ]`}
</SyntaxHighlighter>

   * List the instances for a given type

<SyntaxHighlighter wrapLines={true} language="json" style={theme.dark}>
  {`curl -u username:password http://localhost:21000/api/atlas/v2/search/basic?typeName=hive_db
    {
      "queryType":"BASIC",
      "searchParameters":{
        "typeName":"hive_db",
        "excludeDeletedEntities":false,
        "includeClassificationAttributes":false,
        "includeSubTypes":true,
        "includeSubClassifications":true,
        "limit":100,
        "offset":0
      },
      "entities":[
        {
          "typeName":"hive_db",
          "guid":"5d900c19-094d-4681-8a86-4eb1d6ffbe89",
          "status":"ACTIVE",
          "displayText":"default",
          "classificationNames":[],
          "attributes":{
            "owner":"public",
            "createTime":null,
            "qualifiedName":"default@cl1",
            "name":"default",
            "description":"Default Hive database"
          }
        },
        {
          "typeName":"hive_db",
          "guid":"3a0b14b0-ab85-4b65-89f2-e418f3f7f77c",
          "status":"ACTIVE",
          "displayText":"finance",
          "classificationNames":[],
          "attributes":{
            "owner":"hive",
            "createTime":null,
            "qualifiedName":"finance@cl1",
            "name":"finance",
            "description":null
          }
        }
      ]
    }`}
</SyntaxHighlighter>

   * Search for entities

<SyntaxHighlighter wrapLines={true} language="json" style={theme.dark}>
  {`curl -u username:password http://localhost:21000/api/atlas/v2/search/dsl?query=hive_db%20where%20name='default'
      {
        "queryType":"DSL",
        "queryText":"hive_db where name='default'",
        "entities":[
          {
            "typeName":"hive_db",
            "guid":"5d900c19-094d-4681-8a86-4eb1d6ffbe89",
            "status":"ACTIVE",
            "displayText":"default",
            "classificationNames":[],
            "attributes":{
              "owner":"public",
              "createTime":null,
              "qualifiedName":"default@cl1",
              "name":"default",
              "description":
              "Default Hive database"
            }
          }
        ]
      }`}
</SyntaxHighlighter>


### Troubleshooting

#### Setup issues
If the setup of Apache Atlas service fails due to any reason, the next run of setup (either by an explicit invocation of
`atlas_start.py -setup` or by enabling the configuration option `atlas.server.run.setup.on.start`) will fail with
a message such as `A previous setup run may not have completed cleanly.`. In such cases, you would need to manually
ensure the setup can run and delete the Zookeeper node at `/apache_atlas/setup_in_progress` before attempting to
run setup again.

If the setup failed due to Apache HBase schema setup errors, it may be necessary to repair Apache HBase schema. If no
data has been stored, one can also disable and drop the Apache HBase tables used by Apache Atlas and run setup again.
