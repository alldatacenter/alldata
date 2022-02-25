---
name: Configuration
route: /Configuration
menu: Documentation
submenu: Setup 
---
import  themen  from 'theme/styles/styled-colors';
import  * as theme  from 'react-syntax-highlighter/dist/esm/styles/hljs';
import SyntaxHighlighter from 'react-syntax-highlighter';

# Configuring Apache Atlas - Application Properties

All configuration in Atlas uses java properties style configuration. The main configuration file is atlas-application.properties which is in the *conf* dir at the deployed location. It consists of the following sections:


## Graph Configs

### Graph Persistence engine - HBase
Set the following properties to configure [JanusGraph](https://janusgraph.org/) to use HBase as the persistence engine. Please refer to [link](http://docs.janusgraph.org/0.2.0/configuration.html#_hbase_caching) for more details.

<SyntaxHighlighter wrapLines={true} language="shell" style={theme.dark}>
{`atlas.graph.storage.backend=hbase
atlas.graph.storage.hostname=<ZooKeeper Quorum>
atlas.graph.storage.hbase.table=atlas`}
</SyntaxHighlighter>

If any further JanusGraph configuration needs to be setup, please prefix the property name with "atlas.graph.".

In addition to setting up configurations, please ensure that environment variable HBASE_CONF_DIR is setup to point to
the directory containing HBase configuration file hbase-site.xml.

### Graph Index Search Engine

An index search engine is required for ATLAS. This search engine runs separately from the ATLAS server and from the
storage backend. Only two search engines are currently supported: Solr and Elasticsearch. Pick the search engine
best suited for your environment and follow the configuration instructions below.

#### Graph Search Index - Solr
Solr installation in Cloud mode is a prerequisite for Apache Atlas use. Set the following properties to configure JanusGraph to use Solr as the index search engine.

<SyntaxHighlighter wrapLines={true} language="bash" style={themen}>
{`atlas.graph.index.search.backend=solr5
atlas.graph.index.search.solr.mode=cloud
atlas.graph.index.search.solr.wait-searcher=true
# ZK quorum setup for solr as comma separated value. Example: 10.1.6.4:2181,10.1.6.5:2181
atlas.graph.index.search.solr.zookeeper-url=
# SolrCloud Zookeeper Connection Timeout. Default value is 60000 ms
atlas.graph.index.search.solr.zookeeper-connect-timeout=60000
# SolrCloud Zookeeper Session Timeout. Default value is 60000 ms
atlas.graph.index.search.solr.zookeeper-session-timeout=60000`}
</SyntaxHighlighter>

#### Graph Search Index - Elasticsearch (Tech Preview)
Elasticsearch is a prerequisite for Apache Atlas use. Set the following properties to configure JanusGraph to use Elasticsearch as the index search engine.

<SyntaxHighlighter wrapLines={true} language="bash" style={theme.dark}>
{`atlas.graph.index.search.backend=elasticsearch
atlas.graph.index.search.hostname=<hostname(s) of the Elasticsearch master nodes comma separated>
atlas.graph.index.search.elasticsearch.client-only=true`}
</SyntaxHighlighter>


## Search Configs
Search APIs (DSL, basic search, full-text search) support pagination and have optional limit and offset arguments. Following configs are related to search pagination

<SyntaxHighlighter wrapLines={true} language="bash" style={theme.dark}>
{`# Default limit used when limit is not specified in API
atlas.search.defaultlimit=100
# Maximum limit allowed in API. Limits maximum results that can be fetched to make sure the atlas server doesn't run out of memory
atlas.search.maxlimit=10000`}
</SyntaxHighlighter>


## Notification Configs
Refer http://kafka.apache.org/documentation.html#configuration for Kafka configuration. All Kafka configs should be prefixed with 'atlas.kafka.'

<SyntaxHighlighter wrapLines={true} language="bash"  style={theme.dark}>
{`
atlas.kafka.auto.commit.enable=false
#Kafka servers. Example: localhost:6667
atlas.kafka.bootstrap.servers=
atlas.kafka.hook.group.id=atlas
#Zookeeper connect URL for Kafka. Example: localhost:2181
atlas.kafka.zookeeper.connect=
atlas.kafka.zookeeper.connection.timeout.ms=30000
atlas.kafka.zookeeper.session.timeout.ms=60000
atlas.kafka.zookeeper.sync.time.ms=20
#Setup the following configurations only in test deployments where Kafka is started within Atlas in embedded mode
#atlas.notification.embedded=true
#atlas.kafka.data={sys:atlas.home}/data/kafka
#Setup the following two properties if Kafka is running in Kerberized mode.
#atlas.notification.kafka.service.principal=kafka/_HOST@EXAMPLE.COM
#atlas.notification.kafka.keytab.location=/etc/security/keytabs/kafka.service.keytab`}
</SyntaxHighlighter>

## Client Configs

<SyntaxHighlighter wrapLines={true} language="bash" style={theme.dark}>
{`atlas.client.readTimeoutMSecs=60000
atlas.client.connectTimeoutMSecs=60000
# URL to access Atlas server. For example: http://localhost:21000
atlas.rest.address=`}
</SyntaxHighlighter>


## Security Properties

### SSL config
The following property is used to toggle the SSL feature.

<SyntaxHighlighter wrapLines={true} language="bash" style={theme.dark}>
atlas.enableTLS=false
</SyntaxHighlighter>

## High Availability Properties
The following properties describe High Availability related configuration options:

<SyntaxHighlighter wrapLines={true} language="bash" style={theme.dark}>
{`
# Set the following property to true, to enable High Availability. Default = false.
atlas.server.ha.enabled=true
# Specify the list of Atlas instances
atlas.server.ids=id1,id2
# For each instance defined above, define the host and port on which Atlas server listens.
atlas.server.address.id1=host1.company.com:21000
atlas.server.address.id2=host2.company.com:31000
# Specify Zookeeper properties needed for HA.
# Specify the list of services running Zookeeper servers as a comma separated list.
atlas.server.ha.zookeeper.connect=zk1.company.com:2181,zk2.company.com:2181,zk3.company.com:2181
# Specify how many times should connection try to be established with a Zookeeper cluster, in case of any connection issues.
atlas.server.ha.zookeeper.num.retries=3
# Specify how much time should the server wait before attempting connections to Zookeeper, in case of any connection issues.
atlas.server.ha.zookeeper.retry.sleeptime.ms=1000
# Specify how long a session to Zookeeper should last without inactiviy to be deemed as unreachable.
atlas.server.ha.zookeeper.session.timeout.ms=20000
# Specify the scheme and the identity to be used for setting up ACLs on nodes created in Zookeeper for HA.
# The format of these options is <scheme:identity>.
# For more information refer to 
http://zookeeper.apache.org/doc/r3.2.2/zookeeperProgrammers.html#sc_ZooKeeperAccessControl
# The 'acl' option allows to specify a scheme, identity pair to setup an ACL for.
atlas.server.ha.zookeeper.acl=sasl:client@comany.com
# The 'auth' option specifies the authentication that should be used for connecting to Zookeeper.
atlas.server.ha.zookeeper.auth=sasl:client@company.com
# Since Zookeeper is a shared service that is typically used by many components,
# it is preferable for each component to set its znodes under a namespace.
# Specify the namespace under which the znodes should be written. Default = /apache_atlas
atlas.server.ha.zookeeper.zkroot=/apache_atlas
# Specify number of times a client should retry with an instance before selecting another active instance, or failing an operation.
atlas.client.ha.retries=4
# Specify interval between retries for a client.
atlas.client.ha.sleep.interval.ms=5000`}
</SyntaxHighlighter>

## Server Properties
<SyntaxHighlighter wrapLines={true} language="bash" style={theme.dark}>
{`# Set the following property to true, to enable the setup steps to run on each server start. Default = false.
atlas.server.run.setup.on.start=false`}
</SyntaxHighlighter>

## Performance configuration items
The following properties can be used to tune performance of Atlas under specific circumstances:

<SyntaxHighlighter wrapLines={true} language="bash" style={theme.dark}>
{`
# The number of times Atlas code tries to acquire a lock (to ensure consistency) while committing a transaction.
# This should be related to the amount of concurrency expected to be supported by the server. For e.g. with retries set to 10, upto 100 threads can concurrently create types in the Atlas system.
# If this is set to a low value (default is 3), concurrent operations might fail with a PermanentLockingException.
atlas.graph.storage.lock.retries=10
# Milliseconds to wait before evicting a cached entry. This should be > atlas.graph.storage.lock.wait-time x atlas.graph.storage.lock.retries
# If this is set to a low value (default is 10000), warnings on transactions taking too long will occur in the Atlas application log.
atlas.graph.storage.cache.db-cache-time=120000
# Minimum number of threads in the atlas web server
atlas.webserver.minthreads=10
# Maximum number of threads in the atlas web server
atlas.webserver.maxthreads=100
# Keepalive time in secs for the thread pool of the atlas web server
atlas.webserver.keepalivetimesecs=60
# Queue size for the requests(when max threads are busy) for the atlas web server
atlas.webserver.queuesize=100
# Set to the property to true to enable warn on no relationships defined between entities on a particular attribute
# Not having relationships defined can lead to performance loss while adding new entities
atlas.relationships.warnOnNoRelationships=false`}
</SyntaxHighlighter>

### Recording performance metrics
To enable performance logs for various Atlas operations (like REST API calls, notification processing), setup the following in atlas-log4j.xml:

<SyntaxHighlighter wrapLines={true} language="xml" style={theme.dark}>
{`<appender name="perf_appender" class="org.apache.log4j.DailyRollingFileAppender">
  <param name="File" value="/var/log/atlas/atlas_perf.log"/>
  <param name="datePattern" value="'.'yyyy-MM-dd"/>
  <param name="append" value="true"/>
  <layout class="org.apache.log4j.PatternLayout">
    <param name="ConversionPattern" value="%d|%t|%m%n"/>
  </layout>
</appender>

 <logger name="org.apache.atlas.perf" additivity="false">
   <level value="debug"/>
   <appender-ref ref="perf_appender"/>
 </logger>`}
</SyntaxHighlighter>
