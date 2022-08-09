<!---
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements. See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

# Permission Resources
Permission resources help to determine access control for a user upon a resource (Ambari, a cluster, a view, etc...).

### API Summary

- [List permissions](permission-list.md)
- [Get permission](permission-get.md)
- [Create permission](permission-create.md)
- [Update permission](permission-update.md)
- [Delete permission](permission-delete.md)

### Properties

<table>
  <tr>
    <th>Property</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>PermissionInfo/permission_id</td>
    <td>The permission's unique id - this value may be used to uniquely identify a permission.</td>  
  </tr>
  <tr>
    <td>PermissionInfo/permission_name</td>
    <td>The permission's unique name -this value may be used to uniquely identify a permission.</td>  
  </tr>
  <tr>
    <td>PermissionInfo/permission_label</td>
    <td>The permission's descriptive label - this value may be used to present the permission in a user interface.</td>  
  </tr>
  <tr>
    <td>PermissionInfo/resource_name</td>
    <td>
    The resource type this permission is related to. Possible values include:
    <ul>
    <li>AMBARI - the Ambari server, itself</li>
    <li>CLUSTER - a cluster managed by the Ambari server</li>
    <li>VIEW - a view managed by the Ambari server</li>
    </ul>
    </td>  
  </tr>
</table>

