---
name: WhatsNew-2.2
route: /WhatsNew-2.2
menu: Downloads
submenu: Whats New
---

# What's new in Apache Atlas 2.2?

## Features
* **(new) Deferred Action**: classification propagation will be handled as a background task (AtlasTask)
* **Re-indexing**: added Re-indexing as part of JAVA_PATCH
* **Model Change**: created JAVA_PATCH to add new super types to existing entities
* **Export Service**: added support for Business Metadata in Atlas Export API
* **Admin/AtlasTask API**: added HA support for admin/task API
* **Entity definition**: provided option to add mandatory attribute to existing entity definition

## Enhancements
* **DSL Search**:
  - added support for glossary terms and relationship
  - added support for null attribute values,
  - now uses Tinkerpop GraphTraversal instead of GremlinScriptEngine for performance improvement
  - added support search by classification and its attribute
  - added caching mechanism for translated DSL queries
* **Atlas Python Client**: refactored and enhanced Atlas Python Client supporting Python 2.7
* **Search**: updated free-text search processor to support Elasticsearch, Support search text with special characters, optimized pagination
* **Bulk Glossary Import**: improved and enhanced Bulk Glossary Import supporting import with relations
* **Performance**: improved performance of GraphHelper's guid and status getter methods
* **Authorization**: enhanced Atlas authorization for Add/Update/Remove classification on entities, "admin-audits" for Atlas Admin Audits authorization
* **Notification**: improved NotificationHookConsumer for Large Message Processing
* **Export/Import Service**: enhanced Export/Import Service to conditionally Support Simultaneous Operations and to export Terms
* **Hive Hook**: added support HiveServer2 Hook to send Lineage-only Messages
* **Apache Flink**: introduced model to capture Apache Flink entities and relationships
* **GCP**: introduced model to capture GCP entities and relationships
* **ADLS-Gen2**: updated model for Azure Data Lake Storage Gen2 entities and relationships
* **Dependencies Upgrade**: JanusGraph, elasticsearch, JQuery, Http core, Http Client, slf4j, log4j, ant, gremlin, Solr, groovy, netty, Kafka
* **UI**: fixed Atlas Web UI to load faster in case of large number of classifications & entities
* **Docker image**: improvements to Docker support
* [List of JIRAs resolved in Apache Atlas 2.2.0 release](https://issues.apache.org/jira/issues/?jql=project%20%3D%20ATLAS%20AND%20status%20%3D%20Resolved%20AND%20fixVersion%20%3D%202.2.0%20ORDER%20BY%20key%20DESC)
