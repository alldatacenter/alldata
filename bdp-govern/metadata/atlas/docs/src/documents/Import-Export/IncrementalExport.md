---
name: Incremental Export
route: /IncrementalExport
menu: Documentation
submenu: Import/Export
---

import  themen  from 'theme/styles/styled-colors';
import  * as theme  from 'react-syntax-highlighter/dist/esm/styles/hljs';
import SyntaxHighlighter from 'react-syntax-highlighter';

## Incremental Export

#### Background
Incremental export allows for export of entities after a specified timestamp. This allows for synchronization capability to be built as it makes payloads lighter.

#### Export Options
New _fetchType_ added to indicate incremental export. This option can be used with any _matchType_. When _fetchType_ is _incremental_, it is necessary to specify the _changeMarker_ option for incremental export to function, else full export will be performed.

<SyntaxHighlighter wrapLines={true} language="json" style={theme.dark}>
{`{
 "itemsToExport": [
 { "typeName": "hive_db", "uniqueAttributes": { "qualifiedName": "stocks@cl1" } }
 ],
"options": {
 "fetchType": "incremental",
 "changeMarker": 10000
 }
}`}
</SyntaxHighlighter>

#### Getting Change Marker

The very first call to export with _fetchType_ set to _incremental_ should be made with _changeMarker_ set to 0. This will perform a full export. The _AtlasExportResult_ will have the _changeMarker_ set to a value. This is the value that should be used for a subsequent call to Export.

#### Skip Lineage Option

Export can be performed by skipping lineage information. This avoids all lineage information getting into the exported file.

#### Benefit of Incremental Export

The real benefit of incremental export comes when the export is done with _skipLineage_ option set to _true_. This greatly improves performance when fetching entities that have changed since the last export.

