
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

List Credentials
=====

[Back to Credential Resources](credential-resources.md)

**Summary**

Gets the details about an existing credential. Nor all information about a credential will be returned due to security concerns.

    GET /clusters/:clusterName/credentials/:alias

**Response**

<table>
  <tr>
    <th>HTTP CODE</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>200</td>
    <td>OK</td>  
  </tr>
  <tr>
    <td>400</td>
    <td>Bad Request</td>  
  </tr>
  <tr>
    <td>401</td>
    <td>Unauthorized</td>  
  </tr>
  <tr>
    <td>403</td>
    <td>Forbidden</td>  
  </tr> 
  <tr>
    <td>404</td>
    <td>Not Found</td>  
  </tr>
  <tr>
    <td>500</td>
    <td>Internal Server Error</td>  
  </tr>
</table>

**Example**

Get the credential with the alias name of 'kdc.admin.credentials'.

    GET /clusters/c1/credentials/kdc.admin.credentials

    200 OK
    {
      "href" : "http://your.ambari.server/api/v1/clusters/c1/credentials/kdc.admin.credentials",
      "Credential" : {
        "alias" : "kdc.admin.credentials",
        "cluster_name" : "c1",
        "type" : "temporary"
      }
    }