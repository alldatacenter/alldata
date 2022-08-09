---
name: Sqoop
route: /HookSqoop
menu: Documentation
submenu: Hooks
---

import  themen  from 'theme/styles/styled-colors';
import  * as theme  from 'react-syntax-highlighter/dist/esm/styles/hljs';
import SyntaxHighlighter from 'react-syntax-highlighter';

# Apache Atlas Hook for Apache Sqoop

## Sqoop Model
Sqoop model includes the following types:
   * Entity types:
      * sqoop_process
         * super-types: Process
         * attributes: qualifiedName, name, description, owner, inputs, outputs, operation, commandlineOpts, startTime, endTime, userName
      * sqoop_dbdatastore
         * super-types: DataSet
         * attributes: qualifiedName, name, description, owner, dbStoreType, storeUse, storeUri, source
   * Enum types:
      * sqoop_operation_type
         * values: IMPORT, EXPORT, EVAL
      * sqoop_dbstore_usage
         * values: TABLE, QUERY, PROCEDURE, OTHER

Sqoop entities are created and de-duped in Atlas using unique attribute qualifiedName, whose value should be formatted as detailed below.

<SyntaxHighlighter wrapLines={true} language="shell" style={theme.dark}>
{`sqoop_process.qualifiedName:     sqoop <operation> --connect <url> {[--table <tableName>] || [--database <databaseName>]} [--query <storeQuery>]
sqoop_dbdatastore.qualifiedName: <storeType> --url <storeUri> {[--table <tableName>] || [--database <databaseName>]} [--query <storeQuery>]  --hive-<operation> --hive-database <databaseName> [--hive-table <tableName>] --hive-cluster <clusterName>`}
</SyntaxHighlighter>

## Sqoop Hook
Sqoop added a SqoopJobDataPublisher that publishes data to Atlas after completion of import Job. Today, only hiveImport is supported in SqoopHook.
This is used to add entities in Atlas using the model detailed above.

Follow the instructions below to setup Atlas hook in Hive:

Add the following properties to  to enable Atlas hook in Sqoop:
   * Set-up Atlas hook in `<sqoop-conf>`/sqoop-site.xml by adding the following:

<SyntaxHighlighter wrapLines={true} language="shell" style={theme.dark}>
{`<property>
     <name>sqoop.job.data.publish.class</name>
     <value>org.apache.atlas.sqoop.hook.SqoopHook</value>
   </property>`}
</SyntaxHighlighter>


   * untar apache-atlas-${project.version}-sqoop-hook.tar.gz
   * cd apache-atlas-sqoop-hook-${project.version}
   * Copy entire contents of folder apache-atlas-sqoop-hook-${project.version}/hook/sqoop to `<atlas package>`/hook/sqoop
   * Copy `<atlas-conf>`/atlas-application.properties to to the sqoop conf directory `<sqoop-conf>`/
   * Link `<atlas package>`/hook/sqoop/*.jar in sqoop lib



The following properties in atlas-application.properties control the thread pool and notification details:

<SyntaxHighlighter wrapLines={true} language="shell" style={theme.dark}>
{`atlas.hook.sqoop.synchronous=false # whether to run the hook synchronously. false recommended to avoid delays in Sqoop operation completion. Default: false
atlas.hook.sqoop.numRetries=3      # number of retries for notification failure. Default: 3
atlas.hook.sqoop.queueSize=10000   # queue size for the threadpool. Default: 10000
atlas.cluster.name=primary # clusterName to use in qualifiedName of entities. Default: primary
atlas.kafka.zookeeper.connect=                    # Zookeeper connect URL for Kafka. Example: localhost:2181
atlas.kafka.zookeeper.connection.timeout.ms=30000 # Zookeeper connection timeout. Default: 30000
atlas.kafka.zookeeper.session.timeout.ms=60000    # Zookeeper session timeout. Default: 60000
atlas.kafka.zookeeper.sync.time.ms=20             # Zookeeper sync time. Default: 20`}
</SyntaxHighlighter>

Other configurations for Kafka notification producer can be specified by prefixing the configuration name with "atlas.kafka.". For list of configuration supported by Kafka producer, please refer to [Kafka Producer Configs](http://kafka.apache.org/documentation/#producerconfigs)

## NOTES
   * Only the following sqoop operations are captured by sqoop hook currently
      * hiveImport
