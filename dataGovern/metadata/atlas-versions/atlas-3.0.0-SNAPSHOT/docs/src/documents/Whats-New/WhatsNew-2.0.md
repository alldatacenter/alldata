---
name: WhatsNew-2.0
route: /WhatsNew-2.0
menu: Downloads
submenu: Whats New
---

# What's new in Apache Atlas 2.0?

## Features
* Soft-reference attribute implementation.
* Unique-attributes constraint at graph store-level
* Atlas Index Repair tool for Janusgraph
* Relationship notifications when new relationships are created in atlas
* Atlas Import Transform handler implementation

## Updates
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
* <a href="https://issues.apache.org/jira/issues/?jql=project%20%3D%20ATLAS%20AND%20status%20%3D%20Resolved%20AND%20fixVersion%20%3D%202.0.0%20ORDER%20BY%20updated%20DESC%2C%20priority%20DESC">List of JIRAs resolved in Apache Atlas 2.0.0</a>

## Data Migration
With the introduction of unique-attributes constraint at graph store changes - when atlas starts up for the first time after migration to 2.0, expect some delay during startup since unique attribute constraint will be added to existing atlas metadata.
