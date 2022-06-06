---
name: Falcon
route: /HookFalcon
menu: Documentation
submenu: Hooks
---

import  themen  from 'theme/styles/styled-colors';
import  * as theme  from 'react-syntax-highlighter/dist/esm/styles/hljs';
import SyntaxHighlighter from 'react-syntax-highlighter';

# Falcon Atlas Bridge

## Falcon Model
The default hive model includes the following types:
   * Entity types:
      * falcon_cluster
         * super-types: Infrastructure
         * attributes: timestamp, colo, owner, tags
      * falcon_feed
         * super-types: DataSet
         * attributes: timestamp, stored-in, owner, groups, tags
      * falcon_feed_creation
         * super-types: Process
         * attributes: timestamp, stored-in, owner
      * falcon_feed_replication
         * super-types: Process
         * attributes: timestamp, owner
      * falcon_process
         * super-types: Process
         * attributes: timestamp, runs-on, owner, tags, pipelines, workflow-properties

One falcon_process entity is created for every cluster that the falcon process is defined for.

The entities are created and de-duped using unique qualifiedName attribute. They provide namespace and can be used for querying/lineage as well. The unique attributes are:
   * falcon_process.qualifiedName          - `<process name>@<cluster name>`
   * falcon_cluster.qualifiedName          - `<cluster name>`
   * falcon_feed.qualifiedName             - `<feed name>@<cluster name>`
   * falcon_feed_creation.qualifiedName    - `<feed name>`
   * falcon_feed_replication.qualifiedName - `<feed name>`

## Falcon Hook
Falcon supports listeners on falcon entity submission. This is used to add entities in Atlas using the model detailed above.
Follow the instructions below to setup Atlas hook in Falcon:
   * Add 'org.apache.atlas.falcon.service.AtlasService' to application.services in `<falcon-conf>`/startup.properties
   * untar apache-atlas-${project.version}-falcon-hook.tar.gz
   * cd apache-atlas-falcon-hook-${project.version}
   * Copy entire contents of folder apache-atlas-falcon-hook-${project.version}/hook/falcon to `<atlas-home>`/hook/falcon
   * Link Atlas hook jars in Falcon classpath - 'ln -s `<atlas-home>`/hook/falcon/* `<falcon-home>`/server/webapp/falcon/WEB-INF/lib/'
   * In `<falcon_conf>`/falcon-env.sh, set an environment variable as follows:

<SyntaxHighlighter wrapLines={true} language="java" style={theme.dark}>
     {`export FALCON_SERVER_OPTS="<atlas_home>/hook/falcon/*:$FALCON_SERVER_OPTS"`}
     </SyntaxHighlighter>


The following properties in `<atlas-conf>`/atlas-application.properties control the thread pool and notification details:
   * atlas.hook.falcon.synchronous   - boolean, true to run the hook synchronously. default false
   * atlas.hook.falcon.numRetries    - number of retries for notification failure. default 3
   * atlas.hook.falcon.minThreads    - core number of threads. default 5
   * atlas.hook.falcon.maxThreads    - maximum number of threads. default 5
   * atlas.hook.falcon.keepAliveTime - keep alive time in msecs. default 10
   * atlas.hook.falcon.queueSize     - queue size for the threadpool. default 10000

Refer [Configuration](#/Configuration) for notification related configurations


## NOTES
   * In falcon cluster entity, cluster name used should be uniform across components like hive, falcon, sqoop etc. If used with ambari, ambari cluster name should be used for cluster entity
