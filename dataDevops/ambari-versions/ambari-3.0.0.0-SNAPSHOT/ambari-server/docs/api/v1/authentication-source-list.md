
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

List Authentication Sources
=====

[Back to Authentication Source Resources](authentication-source-resources.md)

**Summary**

Returns a collection of the existing authentication sources for a given user, identified by 
<code>:user_name</code>.

    GET /users/:user_name/sources

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

Get the collection of all authentication sources for user with username jdoe.

    GET /users/jdoe/sources

    200 OK
    {
      "href" : "http://your.ambari.server/api/v1/users/jdoe/sources?fields=*",
      "items" : [
        {
          "href" : "http://your.ambari.server/api/v1/users/jdoe/sources/1004",
          "AuthenticationSourceInfo" : {
            "authentication_type" : "LOCAL",
            "created" : 1497472842579,
            "source_id" : 1004,
            "updated" : 1497472842579,
            "user_name" : "jdoe"
          }
        },
        {
          "href" : "http://your.ambari.server/api/v1/users/jdoe/sources/3653",
          "AuthenticationSourceInfo" : {
            "authentication_type" : "LDAP",
            "created" : 1499372841818,
            "source_id" : 3653,
            "updated" : 1499372841818,
            "user_name" : "jdoe"
          }
        },
        {
          "href" : "http://your.ambari.server/api/v1/users/jdoe/sources/3654",
          "AuthenticationSourceInfo" : {
            "authentication_type" : "LDAP",
            "created" : 1499373089670,
            "source_id" : 3654,
            "updated" : 1499373089670,
            "user_name" : "jdoe"
          }
        },
        {
          "href" : "http://your.ambari.server/api/v1/users/jdoe/sources/3655",
          "AuthenticationSourceInfo" : {
            "authentication_type" : "PAM",
            "created" : 1499373089677,
            "source_id" : 3655,
            "updated" : 1499373089677,
            "user_name" : "jdoe"
          }
        }
      ]
    }
    