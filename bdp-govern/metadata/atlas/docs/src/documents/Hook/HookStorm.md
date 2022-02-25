---
name: Storm
route: /HookStorm
menu: Documentation
submenu: Hooks
---

import  themen  from 'theme/styles/styled-colors';
import  * as theme  from 'react-syntax-highlighter/dist/esm/styles/hljs';
import SyntaxHighlighter from 'react-syntax-highlighter';

# Apache Atlas Hook for Apache Storm

## Introduction

Apache Storm is a distributed real-time computation system. Storm makes it
easy to reliably process unbounded streams of data, doing for real-time
processing what Hadoop did for batch processing. The process is essentially
a DAG of nodes, which is called *topology*.

Apache Atlas is a metadata repository that enables end-to-end data lineage,
search and associate business classification.

The goal of this integration is to push the operational topology
metadata along with the underlying data source(s), target(s), derivation
processes and any available business context so Atlas can capture the
lineage for this topology.

There are 2 parts in this process detailed below:
   * Data model to represent the concepts in Storm
   * Storm Atlas Hook to update metadata in Atlas


## Storm Data Model

A data model is represented as Types in Atlas. It contains the descriptions
of various nodes in the topology graph, such as spouts and bolts and the
corresponding producer and consumer types.

The following types are added in Atlas.

   * storm_topology - represents the coarse-grained topology. A storm_topology derives from an Atlas Process type and hence can be used to inform Atlas about lineage.
   * Following data sets are added - kafka_topic, jms_topic, hbase_table, hdfs_data_set. These all derive from an Atlas Dataset type and hence form the end points of a lineage graph.
   * storm_spout - Data Producer having outputs, typically Kafka, JMS
   * storm_bolt - Data Consumer having inputs and outputs, typically Hive, HBase, HDFS, etc.

The Storm Atlas hook auto registers dependent models like the Hive data model
if it finds that these are not known to the Atlas server.

The data model for each of the types is described in
the class definition at org.apache.atlas.storm.model.StormDataModel.

## Storm Atlas Hook

Atlas is notified when a new topology is registered successfully in
Storm. Storm provides a hook, backtype.storm.ISubmitterHook, at the Storm client used to
submit a storm topology.

The Storm Atlas hook intercepts the hook post execution and extracts the metadata from the
topology and updates Atlas using the types defined. Atlas implements the
Storm client hook interface in org.apache.atlas.storm.hook.StormAtlasHook.


## Limitations

The following apply for the first version of the integration.

   * Only new topology submissions are registered with Atlas, any lifecycle changes are not reflected in Atlas.
   * The Atlas server needs to be online when a Storm topology is submitted for the metadata to be captured.
   * The Hook currently does not support capturing lineage for custom spouts and bolts.


## Installation

The Storm Atlas Hook needs to be manually installed in Storm on the client side.
   * untar apache-atlas-${project.version}-storm-hook.tar.gz
   * cd apache-atlas-storm-hook-${project.version}
   * Copy entire contents of folder apache-atlas-storm-hook-${project.version}/hook/storm to $ATLAS_PACKAGE/hook/storm

Storm Atlas hook jars in $ATLAS_PACKAGE/hook/storm need to be copied to $STORM_HOME/extlib.
Replace STORM_HOME with storm installation path.

Restart all daemons after you have installed the atlas hook into Storm.


## Configuration

### Storm Configuration

The Storm Atlas Hook needs to be configured in Storm client config
in *$STORM_HOME/conf/storm.yaml* as:

<SyntaxHighlighter wrapLines={true} language="shell" style={theme.dark}>
{`storm.topology.submission.notifier.plugin.class: "org.apache.atlas.storm.hook.StormAtlasHook"`}
</SyntaxHighlighter>

Also set a 'cluster name' that would be used as a namespace for objects registered in Atlas.
This name would be used for namespacing the Storm topology, spouts and bolts.

The other objects like data sets should ideally be identified with the cluster name of
the components that generate them. For e.g. Hive tables and databases should be
identified using the cluster name set in Hive. The Storm Atlas hook will pick this up
if the Hive configuration is available in the Storm topology jar that is submitted on
the client and the cluster name is defined there. This happens similarly for HBase
data sets. In case this configuration is not available, the cluster name set in the Storm
configuration will be used.

<SyntaxHighlighter wrapLines={true} language="shell" style={theme.dark}>
atlas.cluster.name: "cluster_name"
</SyntaxHighlighter>

In *$STORM_HOME/conf/storm_env.ini*, set an environment variable as follows:

<SyntaxHighlighter wrapLines={true} language="shell" style={theme.dark}>
STORM_JAR_JVM_OPTS:"-Datlas.conf=$ATLAS_HOME/conf/"
</SyntaxHighlighter>

where ATLAS_HOME is pointing to where ATLAS is installed.

You could also set this up programatically in Storm Config as:

<SyntaxHighlighter wrapLines={true} language="shell" style={theme.dark}>
    {`Config stormConf = new Config();
        ...
        stormConf.put(Config.STORM_TOPOLOGY_SUBMISSION_NOTIFIER_PLUGIN,
                org.apache.atlas.storm.hook.StormAtlasHook.class.getName());`}
</SyntaxHighlighter>
