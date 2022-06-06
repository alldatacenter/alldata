---
name: Import API
route: /ImportAPI
menu: Documentation
submenu: Import/Export
---

import  themen  from 'theme/styles/styled-colors';
import  * as theme  from 'react-syntax-highlighter/dist/esm/styles/hljs';
import SyntaxHighlighter from 'react-syntax-highlighter';

# Import API

The general approach is:
   * Consumer makes a ZIP file available for import operation. See details below for the 2 flavors of the API.
   * The API if successful, will return the results of the operation.
   * Error will be returned on the failure of the call.

### Import ZIP File Using POST


| **Title** | **Import API**  |
| ------------ | ------------ |
| _Example_ | See Examples sections below. |
| _Description_|Provide the contents of the file to be imported in the request body.|
| _URL_ |_api/atlas/admin/import_ |
| _Method_ |_POST_ |
| _URL Parameters_ |_None_ |
| _Data Parameters_|_None_|
| _Success Response_ | _AtlasImporResult_ is returned as JSON. See details below.|
|_Error Response_|Errors that are handled within the system will be returned as _AtlasBaseException_. |

### Import ZIP File Available on Server

|**Title**|**Import API**|
| ------------ | ------------ |
| _Example_ | See Examples sections below. |
| _Description_|Provide the path of the file to be imported.|
| _URL_ |_api/atlas/admin/importfile_ |
| _Method_ |_POST_ |
| _URL Parameters_ |_None_ |
| _Data Parameters_|_None_|
| _Success Response_ | _AtlasImporResult_ is returned as JSON. See details below.|
|_Error Response_|Errors that are handled within the system will be returned as _AtlasBaseException_. |
|_Notes_| The file to be imported needs to be present on the server at the location specified by the _FILENAME_ parameter.|

__Method Signature for Import__

<SyntaxHighlighter wrapLines={true} language="shell" style={theme.dark}>
{`@POST
@Path("/import")
@Produces("application/json; charset=UTF-8")
@Consumes("multipart/form-data")`}
</SyntaxHighlighter>

__Method Signature for Import File__

<SyntaxHighlighter wrapLines={true} language="shell" style={theme.dark}>
{`@POST
@Path("/importfile")
@Produces("application/json; charset=UTF-8")
@Consumes("application/json")`}
</SyntaxHighlighter>

__Import Options__
Please see [here](#/ImportAPIOptions) for the available options during import process.

__AtlasImportResult Response__
The API will return the results of the import operation in the format defined by the _AtlasImportResult_:
   * _AtlasImportParameters_: This contains a collection of name-value pair of the options that are applied during the import operation.
   * _Metrics_: Operation metrics. These include details on the number of types imported, number of entities imported, etc.
   * _Processed Entities_: Contains a list of GUIDs for the entities that were processed.
   * _Operation Status_: Overall status of the operation. Values are _SUCCESS_, PARTIAL_SUCCESS, _FAIL_.

### Examples Using CURL Calls
The call below performs Import of _QuickStart_ database using POST.

<SyntaxHighlighter wrapLines={true} language="shell" style={theme.dark}>
{`curl -g -X POST -u adminuser:password -H "Content-Type: multipart/form-data"
            -H "Cache-Control: no-cache"
            -F request=@importOptions.json
            -F data=@quickStartDB.zip
            "http://localhost:21000/api/atlas/admin/import"`}
</SyntaxHighlighter>

The _request_ parameter is optional. If import has to be run without any options use:

<SyntaxHighlighter wrapLines={true} language="shell" style={theme.dark}>
{`curl -g -X POST -u adminuser:password -H "Content-Type: multipart/form-data"
            -H "Cache-Control: no-cache"
            -F data=@quickStartDB.zip
            "http://localhost:21000/api/atlas/admin/import"`}
</SyntaxHighlighter>


The call below performs Import of _QuickStart_ database using a ZIP file available on the server.

<SyntaxHighlighter wrapLines={true} language="shell" style={theme.dark}>
{`curl -X POST -u adminuser:password -H "Cache-Control: no-cache" -d ./importOptions.json
"http://localhost:21000/api/atlas/admin/importFile" > quickStartDB-import-result.json`}
</SyntaxHighlighter>

Below is the _AtlasImportResult_ JSON for an import that contains _hive_db_.

The _processedEntities_ contains the _guids_ of all the entities imported.

The _metrics_ contain a breakdown of the types and entities imported along with the operation performed on them viz. _created_ or _updated_.

<SyntaxHighlighter wrapLines={true} language="json" style={theme.dark}>
{`{
    "request": {
        "options": {}
    },
    "userName": "admin",
    "clientIpAddress": "10.0.2.2",
    "hostName": "10.0.2.15",
    "timeStamp": 1491285622823,
    "metrics": {
        "duration": 9143,
        "typedef:enum": 0,
        "typedef:struct": 0,
        "entity:hive_column:created": 461,
        "entity:hive_storagedesc:created": 20,
        "entity:hive_process:created": 12,
        "entity:hive_db:created": 5,
        "entity:hive_table:created": 20,
        "entity:hdfs_path:created": 2,
        "typedef:entitydef": 0,
        "typedef:classification": 3
    },
    "processedEntities": [
        "2c4aa713-030b-4fb3-98b1-1cab23d9ac81",
        "e4aa71ed-70fd-4fa7-9dfb-8250a573e293",
       ...
        "ea0f9bdb-1dfc-4e48-9848-a006129929f9",
        "b5e2cb41-3e7d-4468-84e1-d87c320e75f9"
    ],
    "operationStatus": "SUCCESS"
}`}
</SyntaxHighlighter>
