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

# Credential Resources
Credential resources represent named principal/key pairs related to a particular cluster. Credentials are sub-resources of a Cluster.

Credential resources may be stored in the persisted credential store or the temporary credential store. If stored in the persisted credential store, the credential will be stored until deleted. If stored in the temporary credential store, the credential will be stored until a timeout is met (default 90 minutes) or Ambari is restarted. 

### API Summary

- [List credentials](credential-list.md)
- [Get credential](credential-get.md)
- [Create credential](credential-create.md)
- [Update credential](credential-update.md)
- [Delete credential](credential-delete.md)

### Properties

<table>
  <tr>
    <th>Property</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>Credential/cluster_name</td>
    <td>The cluster name</td>  
  </tr>
  <tr>
    <td>Credential/alias</td>
    <td>The alias name</td>  
  </tr>
  <tr>
    <td>Credential/principal</td>
    <td>The principal (or username) - this value is not visible when getting credentials</td>  
  </tr>
  <tr>
    <td>Credential/key</td>
    <td>The secret key (or password) - this value is not visible when getting credentials</td>  
  </tr>
  <tr>
    <td>Credential/type</td>
    <td>The type of credential store: persisted or temporary</td>  
  </tr>
</table>

