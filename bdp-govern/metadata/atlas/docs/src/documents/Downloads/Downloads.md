---
name: Download
route: /Downloads
menu: Downloads
submenu: Download
---
import  themen  from 'theme/styles/styled-colors';
import  * as theme  from 'react-syntax-highlighter/dist/esm/styles/hljs';
import SyntaxHighlighter from 'react-syntax-highlighter';

# Downloads


Apache Atlas release artifacts are distributed via mirror sites and should be checked for tampering using GPG or SHA-256.

The table below lists release artifacts and their associated signatures and hashes. The keys used to sign the release
artifacts can be found in our published [KEYS file](https://www.apache.org/dist/atlas/KEYS).

| **Version** | **Release Date** | **Tarball** | **GPG** | **Hash** |
| : ------------- : | : ------------- : | : ------------- : | : ------------- : |: ------------- :|
| 2.2.0 | 2021-08-17 | [source](https://www.apache.org/dyn/closer.cgi/atlas/2.2.0/apache-atlas-2.2.0-sources.tar.gz) | [signature](https://www.apache.org/dist/atlas/2.2.0/apache-atlas-2.2.0-sources.tar.gz.asc) | [SHA512](https://www.apache.org/dist/atlas/2.2.0/apache-atlas-2.2.0-sources.tar.gz.sha512) |
| 2.1.0 | 2020-07-15 | [source](https://www.apache.org/dyn/closer.cgi/atlas/2.1.0/apache-atlas-2.1.0-sources.tar.gz) | [signature](https://www.apache.org/dist/atlas/2.1.0/apache-atlas-2.1.0-sources.tar.gz.asc) | [SHA512](https://www.apache.org/dist/atlas/2.1.0/apache-atlas-2.1.0-sources.tar.gz.sha512) |
| 0.8.4 | 2019-06-21 | [source](https://www.apache.org/dyn/closer.cgi/atlas/0.8.4/apache-atlas-0.8.4-sources.tar.gz) | [signature](https://www.apache.org/dist/atlas/0.8.4/apache-atlas-0.8.4-sources.tar.gz.asc) | [SHA512](https://www.apache.org/dist/atlas/0.8.4/apache-atlas-0.8.4-sources.tar.gz.sha512) |
| 1.2.0 | 2019-06-07 | [source](https://www.apache.org/dyn/closer.cgi/atlas/1.2.0/apache-atlas-1.2.0-sources.tar.gz) | [signature](https://www.apache.org/dist/atlas/1.2.0/apache-atlas-1.2.0-sources.tar.gz.asc) | [SHA512](https://www.apache.org/dist/atlas/1.2.0/apache-atlas-1.2.0-sources.tar.gz.sha512) |
| 2.0.0 | 2019-05-13 | [source](https://archive.apache.org/dist/atlas/2.0.0/apache-atlas-2.0.0-sources.tar.gz) | [signature](https://archive.apache.org/dist/atlas/2.0.0/apache-atlas-2.0.0-sources.tar.gz.asc) | [SHA512](https://archive.apache.org/dist/atlas/2.0.0/apache-atlas-2.0.0-sources.tar.gz.sha512) |
| 0.8.3 | 2018-10-31 | [source](https://archive.apache.org/dist/atlas/0.8.3/apache-atlas-0.8.3-sources.tar.gz) | [signature](https://archive.apache.org/dist/atlas/0.8.3/apache-atlas-0.8.3-sources.tar.gz.asc) | [SHA512](https://archive.apache.org/dist/atlas/0.8.3/apache-atlas-0.8.3-sources.tar.gz.sha512) |
| 1.1.0 | 2018-09-14 | [source](https://archive.apache.org/dist/atlas/1.1.0/apache-atlas-1.1.0-sources.tar.gz) | [signature](https://archive.apache.org/dist/atlas/1.1.0/apache-atlas-1.1.0-sources.tar.gz.asc) | [SHA512](https://archive.apache.org/dist/atlas/1.1.0/apache-atlas-1.1.0-sources.tar.gz.sha512) |
| 1.0.0 | 2018-06-02 | [source](https://archive.apache.org/dist/atlas/1.0.0/apache-atlas-1.0.0-sources.tar.gz) | [signature](https://archive.apache.org/dist/atlas/1.0.0/apache-atlas-1.0.0-sources.tar.gz.asc) | [SHA512](https://archive.apache.org/dist/atlas/1.0.0/apache-atlas-1.0.0-sources.tar.gz.sha512) |
| 0.8.2 | 2018-02-05 | [source](https://archive.apache.org/dist/atlas/0.8.2/apache-atlas-0.8.2-sources.tar.gz) | [signature](https://archive.apache.org/dist/atlas/0.8.2/apache-atlas-0.8.2-sources.tar.gz.asc) | [SHA512](https://archive.apache.org/dist/atlas/0.8.2/apache-atlas-0.8.2-sources.tar.gz.sha512) |
| 0.8.1 | 2017-08-29 | [source](https://archive.apache.org/dist/atlas/0.8.1/apache-atlas-0.8.1-sources.tar.gz) | [signature](https://archive.apache.org/dist/atlas/0.8.1/apache-atlas-0.8.1-sources.tar.gz.asc) | [SHA512](https://archive.apache.org/dist/atlas/0.8.1/apache-atlas-0.8.1-sources.tar.gz.sha512) |
| 0.8.0-incubating | 2017-06-26 | [source](https://archive.apache.org/dist/incubator/atlas/0.8.0-incubating/apache-atlas-0.8-incubating-sources.tar.gz) | [signature](https://archive.apache.org/dist/incubator/atlas/0.8.0-incubating/apache-atlas-0.8-incubating-sources.tar.gz.asc) | [SHA512](https://archive.apache.org/dist/incubator/atlas/0.8.0-incubating/apache-atlas-0.8-incubating-sources.tar.gz.sha512) |
| 0.7.1-incubating | 2017-01-28 | [source](https://archive.apache.org/dist/incubator/atlas/0.7.1-incubating/apache-atlas-0.7.1-incubating-sources.tar.gz) | [signature](https://archive.apache.org/dist/incubator/atlas/0.7.1-incubating/apache-atlas-0.7.1-incubating-sources.tar.gz.asc) | [checksum](https://archive.apache.org/dist/incubator/atlas/0.7.1-incubating/apache-atlas-0.7.1-incubating-sources.tar.gz.mds) |
| 0.7.0-incubating | 2016-07-09 | [source](https://archive.apache.org/dist/incubator/atlas/0.7.0-incubating/apache-atlas-0.7-incubating-sources.tar.gz) | [signature](https://archive.apache.org/dist/incubator/atlas/0.7.0-incubating/apache-atlas-0.7-incubating-sources.tar.gz.asc) | [SHA512](https://archive.apache.org/dist/incubator/atlas/0.7.0-incubating/apache-atlas-0.7-incubating-sources.tar.gz.sha512) |
| 0.6.0-incubating | 2015-12-31 | [source](https://archive.apache.org/dist/incubator/atlas/0.6.0-incubating/apache-atlas-0.6-incubating-sources.tar.gz) | [signature](https://archive.apache.org/dist/incubator/atlas/0.6.0-incubating/apache-atlas-0.6-incubating-sources.tar.gz.asc) | [SHA](https://archive.apache.org/dist/incubator/atlas/0.6.0-incubating/apache-atlas-0.6-incubating-sources.tar.gz.sha) |
| 0.5.0-incubating | 2015-07-10 | [source](https://archive.apache.org/dist/incubator/atlas/0.5.0-incubating/apache-atlas-0.5-incubating-sources.tar.gz) | [signature](https://archive.apache.org/dist/incubator/atlas/0.5.0-incubating/apache-atlas-0.5-incubating-sources.tar.gz.asc) | [SHA](https://archive.apache.org/dist/incubator/atlas/0.5.0-incubating/apache-atlas-0.5-incubating-sources.tar.gz.sha) |

## Verify the integrity of the files

It is essential that you verify the integrity of the downloaded file using the PGP signature (.asc file) or a hash
(.md5 or .sha* file). Please read [Verifying Apache Software Foundation Releases](https://www.apache.org/info/verification.html)
for more information on why you should verify our releases.

The PGP signature can be verified using PGP or GPG, with the following steps:
   * Download the release artifact from the link in the table above
   * Download the signature file for the release from the link in the table above
   * Download [Apache Atlas KEYS file](https://www.apache.org/dist/atlas/KEYS)
   * Verify the signatures using one of the following:

<SyntaxHighlighter wrapLines={true} language="shell" style={theme.dark}>
{`gpg --import KEYS
gpg --verify downloaded_file.asc downloaded_file`}
</SyntaxHighlighter>

or

<SyntaxHighlighter wrapLines={true} language="shell" style={theme.dark}>
{`pgpk -a KEYS
pgpv downloaded_file.asc`}
</SyntaxHighlighter>

or

<SyntaxHighlighter wrapLines={true} language="shell" style={theme.dark}>
{`pgp -ka KEYS
pgp downloaded_file.asc`}
</SyntaxHighlighter>

## Release Notes
**[Atlas 2.2.0](../2.2.0/index) (Released on 2021/08/17)**
   * (new) Deferred Action: classification propagation will be handled as a background task (AtlasTask)
   * Re-indexing: added Re-indexing as part of JAVA_PATCH
   * Model Change: created JAVA_PATCH to add new super types to existing entities
   * Export Service: added support for Business Metadata in Atlas Export API
   * Admin/AtlasTask API: added HA support for admin/task API
   * Entity definition: provided option to add mandatory attribute to existing entity definition
   * DSL Search:
        - added support for glossary terms and relationship
        - added support for null attribute values,
        - now uses Tinkerpop GraphTraversal instead of GremlinScriptEngine for performance improvement
        - added support search by classification and its attribute
        - added caching mechanism for translated DSL queries
   * Atlas Python Client: refactored and enhanced Atlas Python Client supporting Python 2.7
   * Search: updated free-text search processor to support Elasticsearch, Support search text with special characters, optimized pagination
   * Bulk Glossary Import: improved and enhanced Bulk Glossary Import supporting import with relations
   * Performance: improved performance of GraphHelper's guid and status getter methods
   * Authorization: enhanced Atlas authorization for Add/Update/Remove classification on entities, "admin-audits" for Atlas Admin Audits authorization
   * Notification: improved NotificationHookConsumer for Large Message Processing
   * Export/Import Service: enhanced Export/Import Service to conditionally Support Simultaneous Operations and to export Terms
   * Hive Hook: added support HiveServer2 Hook to send Lineage-only Messages
   * Apache Flink: introduced model to capture Apache Flink entities and relationships
   * GCP: introduced model to capture GCP entities and relationships
   * ADLS-Gen2: updated model for Azure Data Lake Storage Gen2 entities and relationships
   * Dependencies Upgrade: JanusGraph, elasticsearch, JQuery, Http core, Http Client, slf4j, log4j, ant, gremlin, Solr, groovy, netty, Kafka
   * UI: fixed Atlas Web UI to load faster in case of large number of classifications & entities
   * Docker image: improvements to Docker support
   * Various bug fixes and improvements
   * [List of JIRAs resolved in Apache Atlas 2.2.0 release](https://issues.apache.org/jira/issues/?jql=project%20%3D%20ATLAS%20AND%20status%20%3D%20Resolved%20AND%20fixVersion%20%3D%202.2.0%20ORDER%20BY%20key%20DESC)

**[Atlas 2.1.0](../2.1.0/index) (Released on 2020/07/15)**
   * Quick Search: provides a simpler search experience with type-ahead suggestions
   * Business Metadata: enables augmenting entity-types with additional attributes, search entities using these attributes
   * Labels: ability to add/remove labels on entities, and search entities using labels
   * Custom Attributes: ability to add entity instance specific custom attributes i.e. attributes not defined in entity-def or business metadata
   * Entity Purge: added REST APIs to purge deleted entities
   * Search: ability to find entities by more than one classification
   * Performance: improvements in lineage retrieval and classification-propagation
   * Notification: ability to process notificaitons from multiple Kafka topics
   * Hive Hook: tracks process-executions via hive_process_execution entities
   * Hive Hook: catures DDL operations via hive_db_ddl and hive_table_ddl entities
   * Notification: introduced shell entities to record references to non-existing entities in notifications
   * Spark: added model to capture Spark entities, processes and relationships
   * AWS S3: introduced updated model to capture AWS S3 entities and relationships
   * ADLS-Gen2: introduced model to capture Azure Data Lake Storage Gen2 entities and relationships
   * Dependencies: JanusGraph 0.5.1, Tinkerpop 3.4.6, Spring Framework 4.3.20
   * Authorization: updated to cover new features, like: business metadata, labels, purge
   * UI: multiple UI improvements, including a beta UI
   * Various Bug-fixes and optimizations
   * [List of JIRAs resolved in Apache Atlas 2.1.0 release](https://issues.apache.org/jira/issues/?jql=project%20%3D%20ATLAS%20AND%20status%20%3D%20Resolved%20AND%20fixVersion%20%3D%202.1.0%20ORDER%20BY%20key%20DESC)

**[Atlas 2.0.0](../2.0.0/index) (Released on 2019/05/14)**
   * Soft-reference attribute implementation.
   * Unique-attributes constraints at graph store-level
   * Atlas Index Repair tool for Janusgraph
   * Relationship notifications when new relationships are created in atlas
   * Atlas Import Transform handler implementation
   * Updated component versions to use Hadoop 3.1, Hive 3.1, HBase 2.0, Solr 7.5 and Kafka 2.0
   * Updated JanusGraph version to 0.3.1
   * Updated authentication to support trusted proxy
   * Updated patch framework to persist typedef patches applied to atlas and handle data patches.
   * Updated metrics module to collect notification metrics
   * Updated Atlas Export to support incremental export of metadata.
   * Notification Processing Improvements:
      * Notification processing to support batch-commits
      * New option in notification processing to ignore potentially incorrect hive_column_lineage
      * Updated Hive hook to avoid duplicate column-lineage entities; also updated Atlas server to skip duplicate column-lineage entities
      * Improved batch processing in notificaiton handler to avoid processing of an entity multiple times
      * Add option to ignore/prune metadata for temporary/staging hive tables
      * Avoid unnecessary lookup when creating new relationships
   * UI Improvements:
      * UI: Display counts besides the Type and Classification dropdown list in basic search
      * UI: Display lineage information for process entities
      * UI: Display entity specific icon for the lineage graph
      * UI: Add relationships table inside relationships view in entity details page.
      * UI: Add service-type dropdown in basic search to filter entitydef type.
   * Various Bug-fixes and optimizations
   * [List of JIRAs resolved in Apache Atlas 2.0.0 release](https://issues.apache.org/jira/issues/?jql=project%20%3D%20ATLAS%20AND%20status%20%3D%20Resolved%20AND%20fixVersion%20%3D%202.0.0%20ORDER%20BY%20updated%20DESC%2C%20priority%20DESC)

**[Atlas 1.1.0](../1.1.0/index) (Released on 2018/09/17)**
   * Updated authorization model to support access control on relationship operations
   * Added support for AWS S3 datatypes, in Atlas server and Hive hook
   * Updated [JanusGraph](https://janusgraph.org/) version from 0.2.0 to 0.3.0
   * Updated hooks to send Kafka notifications asynchronously
   * Enhanced classification-propagation with options to handle entity-deletes
   * BugFixes and Optimizations

**[Atlas 1.0.0](../1.0.0/index) (Released on 2018/06/02)**

   * Core model enhancement to support Relationship as first-class construct
   * Support for JanusGraph graph database
   * New DSL implementation, using ANTLR instead of Scala
   * Removal of older type system implementation in atlas-typesystem library
   * Metadata security - fine grained authorization
   * Notification enhancements to support V2 style data structures
   * Jackson library update from 1.9.13 to 2.9.2
   * Classification propagation via entity relationships
   * Glossary terms, categories
   * HBase Hook
   * UI updates to show entity relationships
   * [List of JIRAs resolved in Apache Atlas 1.0.0 release](https://issues.apache.org/jira/issues/?jql=project%20%3D%20Atlas%20AND%20resolution%20%3D%20Fixed%20AND%20fixVersion%20%3D%201.0.0%20ORDER%20BY%20key%20DESC)

**[Atlas 0.8.2](../0.8.2/index) (Released on 2018/02/05)**

   * Search improvements:
      * Basic search enhancement to optionally exclude sub-type entities and sub-classification-types
      * Basic search to return classification attributes
      * Support for saving searches
      * UI support to reorder columns in search results page
   * UI - updates for classification rendering – tree/flat view
   * UI – minification of js, css; cache busting for static content (css, js)
   * notification updates to handle large messages
   * fix type initialization issues in HA deployment
   * In HA, the passive node redirects the request with wrong URL encoding
   * tool kit to recover from lost/bad index data
   * [List of JIRAs resolved in Apache Atlas 0.8.2 release](https://issues.apache.org/jira/issues/?jql=project%20%3D%20Atlas%20AND%20fixVersion%20%3D%200.8.2%20ORDER%20BY%20key%20ASC)

**[Atlas 0.8.1](../0.8.1/index) (Released on 2017/08/29)**

   * Basic-search improvement in use of index for attribute filtering
   * DSL query enhancement to support 'like' operator
   * REST API and UI enhancements to update classification attributes
   * Export/import support to copy data between Apache Atlas instances
   * Ability to delete a tag from UI (and API)
   * UI enhancements: lineage, attribute search filter, ability to search historical data
   * Knox SSO for Atlas REST APIs
   * Moved from use of Guice + Spring framework for dependency-injection to use only Spring framework

**[Atlas 0.8-incubating](../0.8.0-incubating/index) (Released on 2017/03/16)**

   * API revamp - new, structured REST API
   * Simplified search UI
   * UI to create/update entities - HDFS/HBase/Kafka
   * Performance and scalability improvements
   * Knox SSO for Atlas UI

**[Atlas 0.7.1-incubating](../0.7.1-incubating/index) (Released on 2017/01/29)**

   * Performance and scalability improvements (ATLAS-1403, ATLAS-1404)
   * Bug fixes

**[Atlas 0.7-incubating](../0.7.0-incubating/index) (Released on 2016/07/09)**

   * High Availability (ATLAS-510)
   * Business Catalog / Taxonomy (ATLAS-491)
   * Improved Hive Integration (ATLAS-492)
   * Improved Sqoop Integration
   * Improved Falcon Integration
   * Improved Storm Integration
   * Improved Ambari Deployment & Management Support
   * Entity Versioning / Audit (ATLAS-493)
   * Performance Improvements
   * Authorization (ATLAS-497)
   * Atlas / Ranger Authorization Integration (ATLAS-495)
   * Standalone HBase Support (ATLAS-498)
   * Upgrade Support (ATLAS-631)

**[Atlas 0.6-incubating](../0.6.0-incubating/index) (Released on 2015/12/30)**
   * Improved Hive Server 2 Integration
   * Sqoop Integration
   * Falcon Integration
   * Storm Integration
   * Various Bug Fixes
   * Atlas / Ranger Integration

**[Atlas 0.5-incubating](../0.5.0-incubating/index) (Released on 2015/07/09)**
   * Hive Server 2 Integration
   * Basic Hive Lineage
   * Basic Ambari Integration
   * Kerberos Support
   * DSL for query of datastore
   * Basic Storage of Metadata
   * Support for BerkleyDB
   * Support for Titan 0.5
   * Support
