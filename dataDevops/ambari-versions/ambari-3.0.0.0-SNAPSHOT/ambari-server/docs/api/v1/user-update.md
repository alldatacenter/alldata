
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

Update a User
=====

[Back to User Resources](user-resources.md)

**Summary**

Update an existing user resource identified by <code>:user_name</code>

    PUT /user/:user_name

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
    <td>The authenticated user does not have authorization to create/store user persisted data.</td>  
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

**Examples**

Update a user.

    PUT /users/jdoe

    {
      "User" : {
        "display_name" : "Jane Q. Doe"
      }
    }
    
    200 OK

Set (create/update) a user's password as a user administrator. Deprecated, see 
[Source Resources](authentication-source-resources.md).

    POST /users/jdoe

    {
      "User" : {
        "password" : "secret"
      }
    }
    
    200 OK

Change a user's existing password as the (non-administrative) user. Deprecated, see 
[Source Resources](authentication-source-resources.md).

    POST /users/jdoe

    {
      "User" : {
        "password" : "secret",
        "old_password" : "old_secret"
      }
    }
    
    200 OK

Set a user to be an Ambari Administrator, as a user administrator. Deprecated, see 
[Permission Resources](permission-resources.md).

    POST /users/jdoe

    {
      "User" : {
        "admin" : true
      }
    }
    
    200 OK
