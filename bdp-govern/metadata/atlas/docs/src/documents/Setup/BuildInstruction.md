---
name: Build Instruction
route: /BuildInstallation
menu: Documentation
submenu: Setup
---

import  themen  from 'theme/styles/styled-colors';
import  * as theme  from 'react-syntax-highlighter/dist/esm/styles/hljs';
import SyntaxHighlighter from 'react-syntax-highlighter';

## Building & Installing Apache Atlas

### Building Apache Atlas
Download Apache Atlas 1.0.0 release sources, apache-atlas-1.0.0-sources.tar.gz, from the [downloads](#/Downloads) page.
Then follow the instructions below to to build Apache Atlas.



<SyntaxHighlighter wrapLines={true} language="powershell" style={theme.dark}>
{`tar xvfz apache-atlas-1.0.0-sources.tar.gz
cd apache-atlas-sources-1.0.0/
export MAVEN_OPTS="-Xms2g -Xmx2g"
mvn clean -DskipTests install`}
</SyntaxHighlighter>


### Packaging Apache Atlas
To create Apache Atlas package for deployment in an environment having functional Apache HBase and Apache Solr instances, build with the following command:

<SyntaxHighlighter wrapLines={true} language="powershell" style={theme.dark}>
mvn clean -DskipTests package -Pdist
</SyntaxHighlighter>

   * NOTES:
      * Remove option '-DskipTests' to run unit and integration tests
      * To build a distribution without minified js,css file, build with _skipMinify_ profile. By default js and css files are minified.


Above will build Apache Atlas for an environment having functional HBase and Solr instances. Apache Atlas needs to be setup with the following to run in this environment:
   * Configure atlas.graph.storage.hostname (see "Graph persistence engine - HBase" in the [Configuration](#/Configuration) section).
   * Configure atlas.graph.index.search.solr.zookeeper-url (see "Graph Search Index - Solr" in the [Configuration](#/Configuration) section).
   * Set HBASE_CONF_DIR to point to a valid Apache HBase config directory (see "Graph persistence engine - HBase" in the [Configuration](#/Configuration) section).
   * Create indices in Apache Solr (see "Graph Search Index - Solr" in the [Configuration](#/Configuration) section).


### Packaging Apache Atlas with embedded Apache HBase & Apache Solr
To create Apache Atlas package that includes Apache HBase and Apache Solr, build with the embedded-hbase-solr profile as shown below:

<SyntaxHighlighter wrapLines={true} language="powershell" style={theme.dark}>
{`mvn clean -DskipTests package -Pdist,embedded-hbase-solr`}
</SyntaxHighlighter>

Using the embedded-hbase-solr profile will configure Apache Atlas so that an Apache HBase instance and an Apache Solr instance will be started and stopped along with the Apache Atlas server.

>NOTE: This distribution profile is only intended to be used for single node development not in production.

### Packaging Apache Atlas with BerkeleyDB & Apache Solr
To create Apache Atlas package that includes BerkeleyDB and Apache Solr, build with the berkeley-solr profile as shown below:

<SyntaxHighlighter wrapLines={true} language="powershell" style={theme.dark}>
{`mvn clean -DskipTests package -Pdist,berkeley-solr`}
</SyntaxHighlighter>

Using the berkeley-solr profile will configure Apache Atlas so that instances of Apache Solr and Apache Zookeeper will be started and stopped along with the Apache Atlas server.

>NOTE: This distribution profile is only intended to be used for single node development not in production.

### Packaging Apache Atlas with embedded Apache Cassandra & Apache Solr
To create Apache Atlas package that includes Apache Cassandra and Apache Solr, build with the embedded-cassandra-solr profile as shown below:

<SyntaxHighlighter wrapLines={true} language="powershell" style={theme.dark}>
{`mvn clean package -Pdist,embedded-cassandra-solr`}
</SyntaxHighlighter>

Using the embedded-cassandra-solr profile will configure Apache Atlas so that an Apache Cassandra instance and an Apache Solr instance will be started and stopped along with the Atlas server.

>NOTE: This distribution profile is only intended to be used for single node development not in production.

### Apache Atlas Package
Build will create following files, which are used to install Apache Atlas.


<SyntaxHighlighter wrapLines={true} language="powershell" style={theme.dark}>
{`distro/target/apache-atlas-{project.version}-bin.tar.gz
distro/target/apache-atlas-{project.version}-hbase-hook.tar.gz
distro/target/apache-atlas-{project.version}-hive-hook.gz
distro/target/apache-atlas-{project.version}-kafka-hook.gz
distro/target/apache-atlas-{project.version}-sources.tar.gz
distro/target/apache-atlas-{project.version}-sqoop-hook.tar.gz
distro/target/apache-atlas-{project.version}-storm-hook.tar.gz`}
</SyntaxHighlighter>
