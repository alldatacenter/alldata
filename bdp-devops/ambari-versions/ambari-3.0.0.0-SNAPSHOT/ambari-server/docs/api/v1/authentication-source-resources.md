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

# Authentication Source Resources
Authentication Source resources represent authentication sources that a user may use to authenticate
so they may login to Ambari.  Each user account may have multiple authentication sources of various 
types (LOCAL, LDAP, JWT, KERBEROS, PAM, etc...). Each authentication source type has its own 
requirements.  For example, a user may have only one LOCAL authentication source.
<p/>
Users with the <code>AMBARI.MANAGE_USERS</code> privilege (currently, Ambari Administrators) can 
view and update all authentication source resources.  Any other user can only view and (partially) 
update their own authentication source resources. For example a user may change their own password
by updating the relevant authentication source resource.  

### API Summary

- [List authentication sources](authentication-source-list.md)
- [Get authentication source](authentication-source-get.md)
- [Create authentication source](authentication-source-create.md)
- [Update authentication source](authentication-source-update.md)
- [Delete authentication source](authenticationsource-delete.md)

### Properties

<table>
  <tr>
    <th>Property</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>AuthenticationSourceInfo/source_id</td>
    <td>
      The authentication source's unique id - this value may be used to uniquely identify an 
      authentication source. 
      <p/><p/>
      The value is generated internally and is read-only.
    </td>  
  </tr>
  <tr>
    <td>AuthenticationSourceInfo/user_name</td>
    <td>
      The parent resource's user name.
      <p/><p/>
      The value is read-only.
     </td>  
  </tr>
  <tr>
    <td>AuthenticationSourceInfo/authentication_type</td>
    <td>
      The type of authentication source. Possible values include:
      <ul>
        <li>LOCAL - the user has an Ambari-local password</li>
        <li>LDAP - the user authenticates using an LDAP server</li>
        <li>KERBEROS - the user authenticates using a Kerberos token</li>
        <li>PAM - the user authenticates using PAM</li>
        <li>JWT - the user authenticates using a JWT token from Knox</li>
      </ul>
      <p/><p/>
      The value must be set when creating the resource; otherwise it is read-only. 
    </td>  
  </tr>
  <tr>
    <td>AuthenticationSourceInfo/key</td>
    <td>
      The authencation type-specific key.  For example, if the authentcation type is LOCAL, than
      the authentication key is the password.
      <p/><p/>
      The value is settable by an Ambari administrator and potentially the parent user (depending 
      on authentication type); otherwise it not returned in queries to .
    </td>  
  </tr>
  <tr>
    <td>AuthenticationSourceInfo/old_key</td>
    <td>       
      This propery may be set when updating an authentication source resource if verification of the
      current key is needed before being allowed to set a new one.  For eample if setting a new 
      password for an authentication source of type LOCAL, this value is required when a user is 
      updating the value.  It is not used when an Ambari user administrator is updating a user's 
      password.  The need for this property is specific to the requirments of the authentication 
      source type.
      <p/><p/>
      The value is write-only.
    </td>  
  </tr>
  <tr>
    <td>AuthenticationSourceInfo/created</td>
    <td>
      The timestamp indicating when the authentcation source resource was created.
      <p/><p/>
      The value is generated internally and is read-only.
     </td>  
  </tr>
  <tr>
    <td>AuthenticationSourceInfo/updated</td>
    <td>
      The timestamp indicating when the authentcation source resource was updated.
      <p/><p/>
      The value is generated internally and is read-only.
     </td>  
  </tr>
  </tr>
</table>

