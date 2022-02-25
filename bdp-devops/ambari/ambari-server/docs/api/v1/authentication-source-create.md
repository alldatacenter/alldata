
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

Create Authentication Source
=====

[Back to Authentication Source Resources](authentication-source-resources.md)

**Summary**

Create a new authentication source resource as a child to a user identified by <code>:user_name</code>. 
<p/><p/>
Only users with the <code>AMBARI.MANAGE_USERS</code> privilege (currently, Ambari Administrators)
may perform this operation.

    POST /users/:user_name/sources

**Response**

<table>
  <tr>
    <th>HTTP CODE</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>500</td>
    <td>Internal Server Error</td>  
  </tr>
  <tr>
    <td>403</td>
    <td>Forbidden</td>  
  </tr>
</table>


**Examples*
    
Create a LOCAL authentication source for the user with a username of "jdoe".
    
    POST /users/jdoe/sources
    
    {
      "AuthenticationSourceInfo": {
        "authentication_type": "LDAP",
        "key": "some dn"
      }
    }

    201 Created
    

Create multiple authentication sources for the user with a username of "jdoe".
    
    POST /users/jdoe/sources
    
    [
      {
        "AuthenticationSourceInfo": {
          "authentication_type": "PAM",
          "key": "pam_key"
        }
      },
      {
        "AuthenticationSourceInfo": {
          "authentication_type": "LDAP",
          "key": "ldap_key"
        }
      }
    ]
    
    201 Created
