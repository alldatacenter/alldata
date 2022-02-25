---
name: Export HDFS API
route: /ExportHDFSAPI
menu: Documentation
submenu: Import/Export
---

import  themen  from 'theme/styles/styled-colors';
import  * as theme  from 'react-syntax-highlighter/dist/esm/styles/hljs';
import SyntaxHighlighter from 'react-syntax-highlighter';

# Export & Import APIs for HDFS Path

### Introduction

The general approach for using the Import-Export APIs for HDFS Paths remain the same. There are minor variations caused how HDFS paths are handled within Atlas.

Unlike HIVE entities, HDFS entities within Atlas are created manually using the _Create Entity_ link within the Atlas Web UI.

Also, HDFS paths tend to be hierarchical, in the sense that users tend to model the same HDFS storage structure within Atlas.

__Sample HDFS Setup__

|**HDFS Path**|**Atlas Entity**|
| ------------ | ------------ |
|<em>/apps/warehouse/finance</em>|**Entity type: **<em>hdfs_path</em><br/>**Name: **<em>Finance</em><br/>**QualifiedName: **<em>FinanceAll</em>|
|<em>/apps/warehouse/finance/accounts-receivable</em>|**Entity type: **<em>hdfs_path</em><br/>**Name: **<em>FinanceReceivable</em><br/>**QualifiedName: **<em>FinanceReceivable</em><br/>**Path: **<em>/apps/warehouse/finance</em>|
|<em>/apps/warehouse/finance/accounts-payable</em>|**Entity type: **<em>hdfs_path</em><br/>**Name: **<em>Finance-Payable</em><br/>**QualifiedName: **<em>FinancePayable</em><br/>**Path: **<em>/apps/warehouse/finance/accounts-payable</em>|
|<em>/apps/warehouse/finance/billing</em>|**Entity type: **<em>hdfs_path</em><br/>**Name: **<em>FinanceBilling</em><br/>**QualifiedName: **<em>FinanceBilling</em><br/>**Path: **<em>/apps/warehouse/finance/billing</em>|


### Export API Using matchType
To export entities that represent the HDFS path, use the Export API using the _matchType_ option. Details can be found [here](#/ExportAPI).

### Example Using CURL Calls
Below are sample CURL calls perform an export operation on the _Sample HDFS Setup_ shown above.

<SyntaxHighlighter wrapLines={true} language="shell" style={theme.dark}>
{`curl -X POST -u adminuser:password -H "Content-Type: application/json" -H "Cache-Control: no-cache" -d '{
    "itemsToExport": [
            { "typeName": "hdfs_path", "uniqueAttributes": { "qualifiedName": "FinanceAll" } }
    ],
    "options": {
     "fetchType": "full",
     "matchType": "startsWith"
    }
}' "http://localhost:21000/api/atlas/admin/export" > financeAll.zip`}
</SyntaxHighlighter>

### Automatic Creation of HDFS entities
Given that HDFS entity creation is a manual process. The Export API offers a mechanism for the creation of requested HDFS entities.
