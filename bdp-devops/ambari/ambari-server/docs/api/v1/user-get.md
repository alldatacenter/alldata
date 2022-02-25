
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

Get a User
=====

[Back to User Resources](user-resources.md)

**Summary**

Gets the details about an existing user identified by <code>:user_name</code> 

    GET /users/:user_name

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
    <td>The authenticated user is not authorized to perform the requested operation</td>  
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

Get the user with the user_name of jdoe.

    GET /users/jdoe

    200 OK
    {
      "href" : "http://your.ambari.server/api/v1/users/jdoe",
      "Users" : {
        "user_id" : 100,
        "user_name" : "jdoe",
        "local_user_name" : "jdoe",
        "display_name" : "Jane Doe",
        "admin" : false,
        "active" : true,
        "consecutive_failures" : 0,
        "created" : 1497472842569,
        "groups" : [ ],
        "ldap_user" : false,
        "user_type" : "LOCAL"        
      }
      "widget_layouts" : [ ],
      "privileges" : [ ],
      "sources" : [
        {
          "href" : "http://your.ambari.server/api/v1/users/jdoe/sources/1004",
          "AuthenticationSourceInfo" : {
            "source_id" : 1004,
            "user_name" : "jdoe"
          }
        }
      ]
    }
    