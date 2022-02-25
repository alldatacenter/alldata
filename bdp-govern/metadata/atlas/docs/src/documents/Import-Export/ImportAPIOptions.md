---
name: Import API Options
route: /ImportAPIOptions
menu: Documentation
submenu: Import/Export
---

import  themen  from 'theme/styles/styled-colors';
import  * as theme  from 'react-syntax-highlighter/dist/esm/styles/hljs';
import SyntaxHighlighter from 'react-syntax-highlighter';

# Import API Options

Import API options are specified as _options_ JSON. Since the API accepts multi-part form data, it is possible to specify multiple input streams within the CURL call.

### Examples Using CURL Calls
<SyntaxHighlighter wrapLines={true} language="shell" style={theme.dark}>
{`curl -g -X POST -u adminuser:password -H "Content-Type: multipart/form-data"
            -H "Cache-Control: no-cache"
            -F request=@importOptions.json
            -F data=@quickStartDB.zip
            "http://localhost:21000/api/atlas/admin/import"`}
</SyntaxHighlighter>

To use the defaults, set the contents of _importOptions.json_ to:

<SyntaxHighlighter wrapLines={true} language="json" style={theme.dark}>
{`{
  "options": {
  }
}`}
</SyntaxHighlighter>


### Options
Following options are supported for Import process:

   * Specify transforms during the import operation.
   * Resume import by specifying starting entity guid.
   * Optionally import type definition.
   * Handling large imports.

#### Transforms

During the import process, the attribute value of the incoming entity can be changed.

This is possible by specifying entity type and at attribute to be modified and then the manner in which it needs to be modified.

Right now these are the transforms that can be applied:
   * _lowercase_ Converts the attribute value to lower case.
   * _replace_ This performs a string find and replace operation. It takes two parameters, the first is the string to search for and the next one is the string to replace it with.

Example:

The example below applies a couple of transforms to the _qualifiedName_ attribute of hive_table. It converts the value to lower case, then searches for 'cl1', if found, replaces it with 'cl2'.

To use the option, set the contents of _importOptions.json_ to:

<SyntaxHighlighter wrapLines={true} language="json" style={theme.dark}>
{`{
  "options": {
    "transforms": {"hive_table": { "qualifiedName": [ replace:@cl1:@cl2 ] }, "hive_db": { "qualifiedName": [ replace:@cl1:@cl2 ] } }
  }
}`}
</SyntaxHighlighter>

Please refer to [ATLAS-1825](https://issues.apache.org/jira/browse/ATLAS-1825) for details scenarios when this option could be used.

#### Start Guid or Start Index

When an import operation is in progress and the server goes down, it would be possible to resume import from the last successfully imported entity. This would allow the import to resume from where it left off.

Server-side logging is improved to display the detail of the last successfully imported entity, this includes the index within the import list and the entity's guid. Either can be used to specify the point to resume import.

To use the option, set the contents of _importOptions.json_ to:

<SyntaxHighlighter wrapLines={true} language="shell" style={theme.dark}>
{`{
  "options": {
    "startGuid": "bd97c78e-3fa5-4f9c-9f48-3683ca3d1fb1"
  }
}`}
</SyntaxHighlighter>

To use _startPosition_, use the following in the _importOptions.json_:

<SyntaxHighlighter wrapLines={true} language="json" style={theme.dark}>
{`{
  "options": {
    "startPosition": "332"
  }
}`}
</SyntaxHighlighter>

Steps to use the behavior:
   * Start an import (using the CURL) that is fairly long, say about 1000# entities.
   * While the import is in progress, stop atlas server (using atlas_stop.py).
   * From the log file located at _/var/log/atlas/application.log_ get the last successfully imported entity GUID or index position.
   * Update the _importOptions.json_ with the guid.
   * Restart import.

#### Optional Importing Type Definition

The output of Export has _atlas-typedef.json_ that contains the type definitions for the entities exported.

By default (that is if no options are specified), the type definitions are imported and applied to the system being imported to. The entity import is performed after this.

In some cases, you would not want to modify the type definitions. The import may be better off failing than the types be modified.

This option allows for optionally importing of the type definition. The option is set to _true_ by default, which means that type definition is imported. With this option set to _false_, type definitions present in the source will not be imported. In case of mismatch between the entities being imported the types present in the system where the import is being performed, the operation will fail.

Table below enumerates the conditions that get addressed as part of type definition import:

|**Condition**|**Action**|
|-------------|----------|
| Incoming type does not exist in the target system | Type is created. |
|Type to be imported and type in the target system are same | No change |
|Type to be imported and type in target system differ by some attributes| Target system type is updated to the attributes present in the source.<br /> It is possible that the target system will have attributes in addition to the one present in the source.<br /> In that case, the target system's type attributes will be a union of the attributes.<br /> Attributes in target system will not be deleted to match the source. <br />If the type of the attribute differ, the import process will be aborted and exception logged.|

To use the option, set the contents of _importOptions.json_ to:

<SyntaxHighlighter wrapLines={true} language="json" style={theme.dark}>
{`{
  "options": {
    "updateTypeDefinition": true
  }
}`}
</SyntaxHighlighter>

#### Specifying File to be Imported From Server Location

In a scenario where the file to be imported is present at a location on the server, the _importfile_ API can be used. It behaves like the Import API.

To use the option, set the contents of _importOptions.json_ to:

<SyntaxHighlighter wrapLines={true} language="json" style={theme.dark}>
{`{
  "options": {
    "fileName": "/root/fileToBeImported.zip"
  }
}`}
</SyntaxHighlighter>

_CURL_

<SyntaxHighlighter wrapLines={true} language="json" style={theme.dark}>
{`curl -g -X POST -u adminuser:password -H "Content-Type: application/json"
            -H "Cache-Control: no-cache"
            -d r@importOptions.json
            "http://localhost:21000/api/atlas/admin/importfile"`}
</SyntaxHighlighter>

#### Handling Large Imports

By default, the Import Service stores all of the data in memory. This may be limiting for ZIPs containing a large amount of data.

To configure the temporary directory use the application property _atlas.import.temp.directory_. If this property is left blank, the default in-memory implementation is used.

Please ensure that there is sufficient disk space available for the operation.

The contents of the directory created as a backing store for the import operation will be erased after the operation is over.
