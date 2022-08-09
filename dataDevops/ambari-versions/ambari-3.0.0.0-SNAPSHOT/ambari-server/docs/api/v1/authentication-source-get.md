
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

Get Authentication Source
=====

[Back to Authentication Source Resources](authentication-source-resources.md)

**Summary**

Gets the details about an existing authentication source identified by <code>:source_id</code> for 
a user identified by <code>:user_name</code> 

    GET /users/:user_name/sources/:source_id

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

Get a specific authentication source for user with the user_name of jdoe.

    GET /users/jdoe/sources/1234

    200 OK
    {
      "href" : "http://your.ambari.server/api/v1/users/userc/sources/1234",
      "AuthenticationSourceInfo" : {
        "source_id" : 1234,
        "user_name" : "jdoe"
      }
    }     

Get more details about specific authentication source for user with the user_name of jdoe.

    GET /users/jdoe/sources/1234?fields=*

    200 OK
    {
      "href" : "http://your.ambari.server/api/v1/users/userc/sources/1234",
      "AuthenticationSourceInfo" : {
        "authentication_type" : "LOCAL",
        "created" : 1498844132119,
        "source_id" : 1234,
        "updated" : 1498844157794,
        "user_name" : "jdoe"
      }
    }