---
name: Migration-0.8 to 1.0
route: /Migration-0.8-to-1.0
menu: Downloads
submenu: Migration
---

import  themen  from 'theme/styles/styled-colors';
import  * as theme  from 'react-syntax-highlighter/dist/esm/styles/hljs';
import SyntaxHighlighter from 'react-syntax-highlighter';

# Migrating data from Apache Atlas 0.8 to Apache Atlas 1.0

Apache Atlas 1.0 uses JanusGraph graph database to store its type and entity details. Prior versions of Apache Atlas
use Titan 0.5.4 graph database. The two databases use different formats for storage. For deployments upgrading from
earlier version Apache Atlas, the data in Titan 0.5.4 graph database should be migrated to JanusGraph graph database.

In addition to the change to the graph database, Apache Atlas 1.0 introduces few optimizations that require different internal
representation compared to previous versions. Migration steps detailed below will transform data to be compliant with
the new internal representation.

### Migration Steps

Migration of data is done in following steps:
   * Planning the migration.
   * Export Apache Atlas 0.8 data to a directory on the file system.
   * Import data from exported files into Apache Atlas 1.0.

#### Planning the migration

The duration of migration of data from Apache Atlas 0.8 to Apache Atlas 1.0 can be significant, depending upon the
amount of data present in Apache Atlas. This section helps you to estimate the time to migrate, so that you can plan the
upgrade process better.

To estimate the time needed to export data, first you need to find the number of entities in Apache Atlas 0.8. This can
be done by running the following DSL query:

<SyntaxHighlighter wrapLines={true} language="sql" style={theme.dark}>
{`Referenceable select count()`}
</SyntaxHighlighter>

Assuming Apache Atlas is deployed in a quad-core CPU with 4 GB of RAM allocated:
   * Export from Apache Atlas 0.8 will process approximately 2 million entities per hour.
   * Import into Apache Atlas 1.0 will process approximately 0.7 million entities per hour.

#### Exporting data from Apache Atlas 0.8
_Atlas Migration Export Utility_ from Apache Atlas branch-0.8 should be used to export the data from Apache Atlas 0.8 deployments. The implementation of which can be found [here](https://github.com/apache/atlas/tree/branch-0.8/tools/atlas-migration-exporter).

To build this utility:
   * Navigate to root directory of Apache Atlas branch-0.8 local repo (say, _/home/atlas/_)
   * Build using the command: _mvn clean -DskipTests package -Pdist_.
   * After successful build, the _Atlas Migration Utility_ can be found in this _distro/target_ directory. (Say, _/home/atlas/distro/target/apache-atlas-0.8.3-SNAPSHOT-bin/apache-atlas-0.8.3-SNAPSHOT/tools/migration-exporter_)

Move the _Atlas Migration Utility_ directory to the Apache Atlas 0.8 cluster.

Follow these steps to export the data:
   * Shutdown _Apache Atlas 0.8_, so that the database is not updated while the migration is in progress.
   * Execute the following command to export Apache Atlas data in Titan graph database to the specified directory:

<SyntaxHighlighter wrapLines={true} language="json" style={theme.dark}>
   {`atlas_migration_export.py -d <output directory>`}
</SyntaxHighlighter>

Example:

<SyntaxHighlighter wrapLines={true} language="shell" style={theme.dark}>
   /home/atlas-migration-utility/atlas_migration_export.py -d /home/atlas-0.8-data
</SyntaxHighlighter>

On successful execution, _Atlas Migration Utility_ tool will display messages like these:

<SyntaxHighlighter wrapLines={true} language="shell" style={theme.dark}>
{`atlas-migration-export: starting migration export. Log file location /var/log/atlas/atlas-migration-exporter.log
atlas-migration-export: initializing
atlas-migration-export: initialized
atlas-migration-export: exporting typesDef to file /home/atlas-0.8-data/atlas-migration-typesdef.json
atlas-migration-export: exported  typesDef to file /home/atlas-0.8-data/atlas-migration-typesdef.json
atlas-migration-export: exporting data to file /home/atlas-0.8-data/atlas-migration-data.json
atlas-migration-export: exported  data to file /home/atlas-0.8-data/atlas-migration-data.json
atlas-migration-export: completed migration export`}
</SyntaxHighlighter>

More details on the progress of export can be found in a log file named _atlas-migration-exporter.log_, in the log directory
specified in _atlas-log4j.xml_.

### Before importing into Apache Atlas 1.0
   * For Apache Atlas deployments that use Solr as index store, please ensure that existing Apache Atlas specific collections are deleted or renamed before installing Apache Atlas 1.0.

Apache Atlas specific Solr collections can be deleted using CURL commands shown below:

<SyntaxHighlighter wrapLines={true} language="shell" style={theme.dark}>
{`curl 'http://<solrHost:port>/solr/admin/collections?action=DELETE&name=vertex_index'
curl 'http://<solrHost:port>/solr/admin/collections?action=DELETE&name=edge_index'
curl 'http://<solrHost:port>/solr/admin/collections?action=DELETE&name=fulltext_index'`}
</SyntaxHighlighter>

   * Create Solr collections for Apache Atlas 1.0
Apache Atlas specific Solr collections can be created using CURL commands shown below:

<SyntaxHighlighter wrapLines={true} language="shell" style={theme.dark}>
   {`curl 'http://<solrHost:port>/solr/admin/collections?action=CREATE&name=vertex_index&numShards=1&replicationFactor=1&collection.configName=atlas_configs'
   curl 'http://<solrHost:port>/solr/admin/collections?action=CREATE&name=edge_index&numShards=1&replicationFactor=1&collection.configName=atlas_configs'
   curl 'http://<solrHost:port>/solr/admin/collections?action=CREATE&name=fulltext_index&numShards=1&replicationFactor=1&collection.configName=atlas_configs'`}
</SyntaxHighlighter>


* For Apache Atlas deployments that use HBase as backend store, please note that HBase table used by earlier version can't be used by Apache Atlas 1.0. If you are constrained on disk storage space, the table used by earlier version can be removed after successful export of data.
   * Apache Atlas 0.8 uses HBase table named 'atlas_titan' (by default)
   * Apache Atlas 1.0 uses HBase table named 'atlas_janus' (by default)


* Install Apache Atlas 1.0. Do not start yet!


* Make sure the directory containing exported data is accessible to Apache Atlas 1.0 instance.


#### Importing Data into Apache Atlas 1.0
Please follow the steps below to import the data exported above into Apache Atlas 1.0:
   * Specify the location of the directory containing exported data in following property to _atlas-application.properties_:

<SyntaxHighlighter wrapLines={true} language="shell" style={theme.dark}>
   {`atlas.migration.data.filename=<location of the directory containing exported data>`}
</SyntaxHighlighter>


* Start Apache Atlas 1.0. Apache Atlas will start in migration mode. It will start importing data from the specified directory.


* Monitor the progress of import process with the following curl command:

<SyntaxHighlighter wrapLines={true} language="shell" style={theme.dark}>
{`curl -X GET -u admin:<password> -H "Content-Type: application/json" -H "Cache-Control: no-cache" http://<atlasHost>:port/api/atlas/admin/status`}
</SyntaxHighlighter>

Progress of import will be indicated by a message like this:

<SyntaxHighlighter wrapLines={true} language="json" style={theme.dark}>
{`{"Status":"MIGRATING","MigrationStatus":{"operationStatus":"IN_PROGRESS","startTime":1526512275110,"endTime":1526512302750,"currentIndex":10,"currentCounter":101,"totalCount":0}}`}
</SyntaxHighlighter>

Successful completion of the operation will show a message like this:

<SyntaxHighlighter wrapLines={true} language="json" style={theme.dark}>
{`{"Status":"MIGRATING","MigrationStatus":{"operationStatus":"SUCCESS","startTime":1526512275110,"endTime":1526512302750,"currentIndex":0,"currentCounter":0,"totalCount":371}}`}
</SyntaxHighlighter>

Once migration import is complete, i.e. _operationStatus_ is _SUCCESS_, follow the steps given below to restart Apache Atlas
in _ACTIVE_ mode for regular use:
   * Stop Apache Atlas 1.0.
   * Remove property _atlas.migration.data.filename_ from _atlas-application.properties_.
   * Start Apache Atlas 1.0.

### Atlas Entity Defaults for Migrated Data

Apache Atlas 1.0 introduces number of new features. For data that is migrated, the following defaults are set:
   * All classifications will have _isPropagate_ set to _false_.
   * Taxonomy terms present in Apache Atlas 0.8, if any, will be converted to classification.

#### Handling of Entity Definitions that use Classifications as Types

This features is no longer supported. Classifications that are used as types in _attribute definitions_ (_AttributeDefs_) are converted in to new types whose name has _legacy_ prefix. These are then handled like any other type.
Creation of such types was prevented in an earlier release, hence only type definitions have potential to exist. Care has been taken to handle entities of this type as well.
