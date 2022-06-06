---
name: Export API
route: /ExportAPI
menu: Documentation
submenu: Import/Export
---

import  themen  from 'theme/styles/styled-colors';
import  * as theme  from 'react-syntax-highlighter/dist/esm/styles/hljs';
import SyntaxHighlighter from 'react-syntax-highlighter';

# Export API
The general approach is:
   * Consumer specifies the scope of data to be exported (details below).
   * The API if successful, will return the stream in the format specified.
   * Error will be returned on failure of the call.

See [here](#/ExportHDFSAPI) for details on exporting *hdfs_path* entities.

|**Title**|**Export API**|
| ------------ | ------------ |
| _Example_ | See Examples sections below. |
| _URL_ |_api/atlas/admin/export_ |
| _Method_ |_POST_ |
| _URL Parameters_ |_None_ |
| _Data Parameters_| The class _AtlasExportRequest_ is used to specify the items to export. The list of _AtlasObjectId_(s) allows for specifying the multiple items to export in a session. The _AtlasObjectId_ is a tuple of entity type, name of unique attribute, value of unique attribute. Several items can be specified. See examples below.|
| _Success Response_|File stream as _application/zip_.|
|_Error Response_|Errors that are handled within the system will be returned as _AtlasBaseException_. |
| _Notes_ | Consumer could choose to consume the output of the API by programmatically using _java.io.ByteOutputStream_ or by manually, save the contents of the stream to a file on the disk.|

__Method Signature__

<SyntaxHighlighter wrapLines={true} language="shell" style={theme.dark}>
{`@POST
@Path("/export")
@Consumes("application/json;charset=UTF-8")`}
</SyntaxHighlighter>

### Additional Options
It is possible to specify additional parameters for the _Export_ operation.

The current implementation has 2 options. Both are optional:


* _matchType_ This option configures the approach used for fetching the starting entity. It has the following values:
    * _startsWith_ Search for an entity that is prefixed with the specified criteria.
    * _endsWith_ Search for an entity that is suffixed with the specified criteria.
    * _contains_ Search for an entity that has the specified criteria as a sub-string.
    *  _matches_ Search for an entity that is a regular expression match with the specified criteria.





* _fetchType_ This option configures the approach used for fetching entities. It has the following values:
    * _FULL_: This fetches all the entities that are connected directly and indirectly to the starting entity. E.g. If a starting entity specified is a table, then this option will fetch the table, database and all the other tables within the database.
    * _CONNECTED_: This fetches all the etnties that are connected directly to the starting entity. E.g. If a starting entity specified is a table, then this option will fetch the table and the database entity only.
    *  _INCREMENTAL_: See [here](#/IncrementalExport) for details.



If no _matchType_ is specified, an exact match is used. Which means, that the entire string is used in the search criteria.

Searching using _matchType_ applies to all types of entities. It is particularly useful for matching entities of type hdfs_path (see [here](#/ExportHDFSAPI)).

The _fetchType_ option defaults to _FULL_.

For a complete example see the section below.

### Contents of Exported ZIP File

The exported ZIP file has the following entries within it:
   * _atlas-export-result.json_:
      * Input filters: The scope of export.
      * File format: The format chosen for the export operation.
      * Metrics: The number of entity definitions, classifications and entities exported.
   * _atlas-typesdef.json_: Type definitions for the entities exported.
   * _atlas-export-order.json_: Order in which entities should be exported.
   * _{guid}.json_: Individual entities are exported with file names that correspond to their id.

### Examples
The _AtlasExportRequest_ below shows filters that attempt to export 2 databases in cluster cl1:

<SyntaxHighlighter wrapLines={true} language="json" style={theme.dark}>
{`{
    "itemsToExport": [
       { "typeName": "hive_db", "uniqueAttributes": { "qualifiedName": "accounts@cl1" } },
       { "typeName": "hive_db", "uniqueAttributes": { "qualifiedName": "hr@cl1" } }
    ]
}`}
</SyntaxHighlighter>

The _AtlasExportRequest_ below specifies the _fetchType_ as _FULL_. The _matchType_ option will fetch _accounts@cl1_.

<SyntaxHighlighter wrapLines={true} language="json" style={theme.dark}>
{`{
    "itemsToExport": [
       { "typeName": "hive_db", "uniqueAttributes": { "qualifiedName": "accounts@" } }
    ],
    "options": {
        "fetchType": "FULL",
        "matchType": "startsWith"
    }
}`}
</SyntaxHighlighter>

The _AtlasExportRequest_ below specifies the _guid_ instead of _uniqueAttribues_ to fetch _accounts@cl1_.

<SyntaxHighlighter wrapLines={true} language="json" style={theme.dark}>
{`{
    "itemsToExport": [
       { "typeName": "hive_db", "guid": "846c5e9c-3ac6-40ju-8289-fb0cebm64783" }
    ],
    "options": {
        "fetchType": "FULL",
    }
}`}
</SyntaxHighlighter>

The _AtlasExportRequest_ below specifies the _fetchType_ as _connected_. The _matchType_ option will fetch _accountsReceivable_, _accountsPayable_, etc present in the database.

<SyntaxHighlighter wrapLines={true} language="json" style={theme.dark}>
{`{
    "itemsToExport": [
       { "typeName": "hive_db", "uniqueAttributes": { "qualifiedName": "accounts" } }
    ],
    "options": {
        "fetchType": "CONNECTED",
        "matchType": "startsWith"
    }
}`}
</SyntaxHighlighter>

Below is the _AtlasExportResult_ JSON for the export of the _Sales_ DB present in the _QuickStart_.

The _metrics_ contains the number of types and entities exported as part of the operation.

<SyntaxHighlighter wrapLines={true} language="json" style={theme.dark}>
{`{
    "clientIpAddress": "10.0.2.15",
    "hostName": "10.0.2.2",
    "metrics": {
        "duration": 1415,
        "entitiesWithExtInfo": 12,
        "entity:DB_v1": 2,
        "entity:LoadProcess_v1": 2,
        "entity:Table_v1": 6,
        "entity:View_v1": 2,
        "typedef:Column_v1": 1,
        "typedef:DB_v1": 1,
        "typedef:LoadProcess_v1": 1,
        "typedef:StorageDesc_v1": 1,
        "typedef:Table_v1": 1,
        "typedef:View_v1": 1,
        "typedef:classification": 6
    },
    "operationStatus": "SUCCESS",
    "request": {
        "itemsToExport": [
            {
                "typeName": "DB_v1",
                "uniqueAttributes": {
                    "name": "Sales"
                }
            }
        ],
        "options": {
            "fetchType": "full"
        }
    },
    "userName": "admin"
}`}
</SyntaxHighlighter>

### CURL Calls
Below are sample CURL calls that demonstrate Export of _QuickStart_ database.

<SyntaxHighlighter wrapLines={true} language="shell" style={theme.dark}>
{`curl -X POST -u adminuser:password -H "Content-Type: application/json" -H "Cache-Control: no-cache" -d '{
    "itemsToExport": [
            { "typeName": "DB", "uniqueAttributes": { "name": "Sales" }},
            { "typeName": "DB", "uniqueAttributes": { "name": "Reporting" }},
            { "typeName": "DB", "uniqueAttributes": { "name": "Logging" }}
    ],
        "options": { "fetchType": "full" }
    }' "http://localhost:21000/api/atlas/admin/export" > quickStartDB.zip`}
</SyntaxHighlighter>
