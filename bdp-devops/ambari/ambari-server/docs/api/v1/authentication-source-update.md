
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

Modify Authentication Source
=====

[Back to Authentication Source Resources](authentication-source-resources.md)

**Summary**

Update an existing authentication source identified by <code>:source_id</code> for a user identified
by <code>:user_name</code>.  If the <code>AuthenticationSourceInfo/authentication_type</code> is set
the found authentication source resource is tested to ensure it matches the expected value.  

    PUT /user/:user_name/sources/:source_id

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

**Examples**

Update an authentication source for user jdoe when authenticated as a user administrator.

    PUT /users/jdoe/sources/1234

    {
      "AuthenticationSourceInfo": {
        "key": "new_secret"
      }
    }

    200 OK

Update an authentication source for user jdoe when authenticated as a user administrator, verifying 
that the authentication source resource is a LOCAL authentication source.

    PUT /users/jdoe/sources/1234

    {
      "AuthenticationSourceInfo": {
        "authentication_type": "LOCAL",
        "key": "new_secret"
      }
    }

    200 OK

Update an authentication source for user jdoe when authenticated as jdoe.

    PUT /users/jdoe/sources/1234

    {
      "AuthenticationSourceInfo": {
        "old_key": "secret",
        "key": "new_secret"
      }
    }

    200 OK
