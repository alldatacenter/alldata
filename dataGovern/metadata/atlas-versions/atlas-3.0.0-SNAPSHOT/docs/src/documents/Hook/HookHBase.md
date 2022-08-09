---
name: HBase
route: /HookHBase
menu: Documentation
submenu: Hooks
---

import  themen  from 'theme/styles/styled-colors';
import  * as theme  from 'react-syntax-highlighter/dist/esm/styles/hljs';
import SyntaxHighlighter from 'react-syntax-highlighter';

# Apache Atlas Hook & Bridge for Apache HBase

## HBase Model
HBase model includes the following types:
   * Entity types:
      * hbase_namespace
         * super-types: Asset
         * attributes: qualifiedName, name, description, owner, clusterName, parameters, createTime, modifiedTime
      * hbase_table
         * super-types: DataSet
         * attributes: qualifiedName, name, description, owner, namespace, column_families, uri, parameters, createtime, modifiedtime, maxfilesize, isReadOnly, isCompactionEnabled, isNormalizationEnabled, ReplicaPerRegion, Durability
      * hbase_column_family
         * super-types: DataSet
         * attributes:  qualifiedName, name, description, owner, columns, createTime, bloomFilterType, compressionType, compactionCompressionType, encryptionType, inMemoryCompactionPolicy, keepDeletedCells, maxversions, minVersions, datablockEncoding, storagePolicy, ttl, blockCachedEnabled, cacheBloomsOnWrite, cacheDataOnWrite, evictBlocksOnClose, prefetchBlocksOnOpen, newVersionsBehavior, isMobEnabled, mobCompactPartitionPolicy

HBase entities are created and de-duped in Atlas using unique attribute qualifiedName, whose value should be formatted as detailed below. Note that namespaceName, tableName and columnFamilyName should be in lower case.

<SyntaxHighlighter wrapLines={true} language="java" style={theme.dark}>
{`hbase_namespace.qualifiedName:      <namespaceName>@<clusterName>
hbase_table.qualifiedName:          <namespaceName>:<tableName>@<clusterName>
hbase_column_family.qualifiedName:  <namespaceName>:<tableName>.<columnFamilyName>@<clusterName>`}
</SyntaxHighlighter>


## HBase Hook
Atlas HBase hook registers with HBase master as a co-processor. On detecting changes to HBase namespaces/tables/column-families, Atlas hook updates the metadata in Atlas via Kafka notifications.
Follow the instructions below to setup Atlas hook in HBase:
   * Register Atlas hook in hbase-site.xml by adding the following:

<SyntaxHighlighter wrapLines={true} language="xml" style={theme.dark}>
{`<property>
<name>hbase.coprocessor.master.classes</name>
<value>org.apache.atlas.hbase.hook.HBaseAtlasCoprocessor</value>
</property>`}
</SyntaxHighlighter>

   * untar apache-atlas-${project.version}-hbase-hook.tar.gz
   * cd apache-atlas-hbase-hook-${project.version}
   * Copy entire contents of folder apache-atlas-hbase-hook-${project.version}/hook/hbase to `<atlas package>`/hook/hbase
   * Link Atlas hook jars in HBase classpath - 'ln -s `<atlas package>`/hook/hbase/* `<hbase-home>`/lib/'
   * Copy `<atlas-conf>`/atlas-application.properties to the HBase conf directory.

The following properties in atlas-application.properties control the thread pool and notification details:

<SyntaxHighlighter wrapLines={true} language="java" style={theme.dark}>
{`atlas.hook.hbase.synchronous=false # whether to run the hook synchronously. false recommended to avoid delays in HBase operations. Default: false
atlas.hook.hbase.numRetries=3      # number of retries for notification failure. Default: 3
atlas.hook.hbase.queueSize=10000   # queue size for the threadpool. Default: 10000
atlas.cluster.name=primary # clusterName to use in qualifiedName of entities. Default: primary
atlas.kafka.zookeeper.connect=                    # Zookeeper connect URL for Kafka. Example: localhost:2181
atlas.kafka.zookeeper.connection.timeout.ms=30000 # Zookeeper connection timeout. Default: 30000
atlas.kafka.zookeeper.session.timeout.ms=60000    # Zookeeper session timeout. Default: 60000
atlas.kafka.zookeeper.sync.time.ms=20             # Zookeeper sync time. Default: 20`}
</SyntaxHighlighter>

Other configurations for Kafka notification producer can be specified by prefixing the configuration name with "atlas.kafka.".
For list of configuration supported by Kafka producer, please refer to [Kafka Producer Configs](http://kafka.apache.org/documentation/#producerconfigs)

## NOTES
   * Only the namespace, table and column-family create/update/ delete operations are captured by Atlas HBase hook. Changes to columns are be captured.


## Importing HBase Metadata
Apache Atlas provides a command-line utility, import-hbase.sh, to import metadata of Apache HBase namespaces and tables into Apache Atlas.
This utility can be used to initialize Apache Atlas with namespaces/tables present in a Apache HBase cluster.
This utility supports importing metadata of a specific table, tables in a specific namespace or all tables.

<SyntaxHighlighter wrapLines={true} language="java" style={theme.dark}>
{`Usage 1: <atlas package>/hook-bin/import-hbase.sh
Usage 2: <atlas package>/hook-bin/import-hbase.sh [-n <namespace regex> OR --namespace <namespace regex>] [-t <table regex> OR --table <table regex>]
Usage 3: <atlas package>/hook-bin/import-hbase.sh [-f <filename>]
           File Format:
             namespace1:tbl1
             namespace1:tbl2
             namespace2:tbl1`}
</SyntaxHighlighter>
