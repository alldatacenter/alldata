---
name: Export Import Audits
route: /ExportImportAudits
menu: Documentation
submenu: Import/Export
---

import  themen  from 'theme/styles/styled-colors';
import  * as theme  from 'react-syntax-highlighter/dist/esm/styles/hljs';
import SyntaxHighlighter from 'react-syntax-highlighter';

# Export & Import Audits

#### Background

The new audits for Export and Import operations also have corresponding REST APIs to programmatically fetch the audit entries.

#### REST APIs

|**Title**          | **Replication Audits for a Cluster**                                 |
|----------------|------------------------------------------------------------------|
|Example         | See below.                                                       |
|URL             | api/atlas/admin/expimp/audit                                     |
|Method          | GET                                                              |
|URL Parameters  | _sourceClusterName_: Name of source cluster.                     |
|                | _targetClusterName_: Name of target cluster.                     |
|                | _userName_: Name of the user who initiated the operation.        |
|                | _operation_: EXPORT or IMPORT operation type.                    |
|                | _startTime_: Time, in milliseconds, when operation was started.  |
|                | _endTime_: Time, in milliseconds, when operation ended.          |
|                | _limit_: Number of results to be returned                        |
|                | _offset_: Offset                                                 |
|Data Parameters | None                                                             |
|Success Response| List of _ExportImportAuditEntry_                                 |
|Error Response  | Errors Returned as AtlasBaseException                            |
|Notes           | None                                                             |

##### CURL

<SyntaxHighlighter wrapLines={true} language="json" style={theme.dark}>
{`
curl -X GET -u admin:admin -H "Content-Type: application/json" -H "Cache-Control: no-cache"
http://localhost:port/api/atlas/admin/expimp/audit?sourceClusterName=cl2
`}
</SyntaxHighlighter>

##### RESPONSE

<SyntaxHighlighter wrapLines={true} language="json" style={theme.dark}>
{`{
    "queryType": "BASIC",
    "searchParameters": {
        "typeName": "ReplicationAuditEntry",
        "excludeDeletedEntities": false,
        "includeClassificationAttributes": false,
        "includeSubTypes": true,
        "includeSubClassifications": true,
        "limit": 100,
        "offset": 0,
        "entityFilters": {
            "attributeName": "name",
            "operator": "eq",
            "attributeValue": "cl2",
            "criterion": []
        }
    },
    "entities": [{
        "typeName": "ReplicationAuditEntry",
        "attributes": {
            "owner": null,
            "uniqueName": "cl2:EXPORT:1533037289411",
            "createTime": null,
            "name": "cl2",
            "description": null
        },
        "guid": "04844141-af72-498a-9d26-f70f91e8adf8",
        "status": "ACTIVE",
        "displayText": "cl2",
        "classificationNames": []
    }, {
        "typeName": "ReplicationAuditEntry",
        "attributes": {
            "owner": null,
            "uniqueName": "cl2:EXPORT:1533037368407",
            "createTime": null,
            "name": "cl2",
            "description": null
        },
        "guid": "837abe66-20c8-4296-8678-e715498bf8fb",
        "status": "ACTIVE",
        "displayText": "cl2",
        "classificationNames": []
    }]
}`}
</SyntaxHighlighter>
