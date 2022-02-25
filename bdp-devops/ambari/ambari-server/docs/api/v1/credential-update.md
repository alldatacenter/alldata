
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

Update Credential
=====

[Back to Credential Resources](credential-resources.md)

**Summary**

Update an existing credential resource to change it's principal, key, and/or persist values.

    PUT /clusters/:clusterName/credentials/:alias

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
    <td>405</td>
    <td>Method Not Allowed</td>  
  </tr> 
  <tr>
    <td>500</td>
    <td>Internal Server Error</td>  
  </tr>
</table>

**Examples**

Update a credential, changing its principal and key values, but leaving its persistence value alone.

    PUT /clusters/c1/credentials/external.db

    {
      "Credential" : {
        "principal" : "db_admin",
        "key" : "N3WS3cr3tK3y!"
      }
    }
    
    200 OK

Update a credential, changing only its persistence value.  This will remove the credential from its current storage facility and create a new entry in the requested storage facility.

    POST /clusters/c1/credentials/kdc.admin

    {
      "Credential" : {
        "type" : "temporary"
      }
    }
    
    200 OK
