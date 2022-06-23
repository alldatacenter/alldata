---
name: WhatsNew-2.1
route: /WhatsNew-2.1
menu: Downloads
submenu: Whats New
---

# What's new in Apache Atlas 2.1?

## Features
* **Quick Search**: provides a simpler search experience with type-ahead suggestions
* **Business Metadata**: enables augmenting entity-types with additional attributes, search entities using these attributes
* **Labels**: ability to add/remove labels on entities, and search entities using labels
* **Custom Attributes**: ability to add entity instance specific custom attributes i.e. attributes not defined in entity-def or business metadata
* **Entity Purge**: added REST APIs to purge deleted entities

## Enhancements
* **Search**: ability to find entities by more than one classification
* **Performance**: improvements in lineage retrieval and classification-propagation
* **Notification**: ability to process notificaitons from multiple Kafka topics
* **Hive Hook**: tracks process-executions via hive_process_execution entities
* **Hive Hook**: catures DDL operations via hive_db_ddl and hive_table_ddl entities
* **Notification**: introduced shell entities to record references to non-existing entities in notifications
* **Spark**: added model to capture Spark entities, processes and relationships
* **AWS S3**: introduced updated model to capture AWS S3 entities and relationships
* **ADLS-Gen2**: introduced model to capture Azure Data Lake Storage Gen2 entities and relationships
* **Dependencies**: JanusGraph 0.5.1, Tinkerpop 3.4.6, Spring Framework 4.3.20
* **Authorization**: updated to cover new features, like: business metadata, labels, purge
* **UI**: multiple UI improvements, including a beta UI
* <a href="https://issues.apache.org/jira/issues/?jql=project%20%3D%20ATLAS%20AND%20status%20%3D%20Resolved%20AND%20fixVersion%20%3D%202.1.0%20ORDER%20BY%20updated%20DESC%2C%20priority%20DESC">List of JIRAs resolved in Apache Atlas 2.1.0</a>
