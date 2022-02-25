
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

Create Credential
=====

[Back to Credential Resources](credential-resources.md)

**Summary**

Create a new credential resource and stores it in either the persistent or temporary credential store.

    POST /clusters/:clusterName/credentials/:alias

**Response**

<table>
  <tr>
    <th>HTTP CODE</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>201</td>
    <td>Created</td>
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
    <td>500</td>
    <td>Internal Server Error</td>  
  </tr>
</table>

**Examples**

Create a credential with an alias of 'external.db' that is stored in the _persistent_ credential store.

    POST /clusters/c1/credentials/external.db

    {
      "Credential" : {
        "principal" : "db_admin",
        "key" : "S3cr3tK3y!",
        "type" : "persisted"
      }
    }
 
    201 Created

Create a credential with an alias of 'kdc.admin' that is stored in the _temporary_ credential store.

    POST /clusters/c1/credentials/kdc.admin

    {
      "Credential" : {
        "principal" : "admin/admin@EXAMPLE.COM",
        "key" : "!h4d00p!",
        "type" : "temporary"
      }
    }

    201 Created
