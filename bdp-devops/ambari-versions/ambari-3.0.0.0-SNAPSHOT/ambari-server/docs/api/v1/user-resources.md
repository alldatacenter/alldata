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

# User Resources
User resources represent user accounts in Ambari.  Each user account has a set of authentication 
sources and is is given permission to perform tasks within Ambari.
<p/>
Users with the <code>AMBARI.MANAGE_USERS</code> privilege (currently, Ambari Administrators) can 
view and update all user resources.  Any other user can only view and (partially) update their own 
user resource.

### API Summary

- [List users](user-list.md)
- [Get user](user-get.md)
- [Create user](user-create.md)
- [Update user](user-update.md)
- [Delete user](user-delete.md)

### Properties

<table>
  <tr>
    <th>Property</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>Users/user_id</td>
    <td>
      The user's unique id - this value may be used to uniquely identify a user. 
      <p/><p/>
      The value is generated internally and is read-only.
    </td>  
  </tr>
  <tr>
    <td>Users/user_name</td>
    <td>
      The user's unique name - this value is case-insensitive and may be used to uniquely 
      identify a user.
      <p/><p/>
      The value must be set when creating the resource; otherwise it is read-only. 
    </td>  
  </tr>
  <tr>
    <td>Users/local_user_name</td>
    <td>
      The user's local user name - this value is case-sensitive and used as the username to use 
      when accessing service via Ambari Views. If not set, the username value will be used.
      <p/><p/>
      The value is settable by an Ambari administrator; otherwise it is read-only.
    </td>  
  </tr>
  <tr>
    <td>Users/display_name</td>
    <td>
      The user's local user name - this value is used for display purposes in messages and user 
      intefaces. If not set, the username value will be used.
      <p/><p/>
      The value is settable by the user or an Ambari administrator.
    </td>  
  </tr>
  <tr>
    <td>Users/active</td>
    <td>
      The user's active/inactive status - <code>true</code> if active; <code>false</code> if 
      inactive.
      <p/><p/>
      The value is settable by an Ambari administrator; otherwise it is read-only.
    </td>  
  </tr>
  <tr>
    <td>Users/consecutive_failures</td>
    <td>
      The number of consecutive authentication failures since the last successful authentication 
      attempt.
      <p/><p/>
      The value is read-only.
    </td>  
  </tr>
  <tr>
    <td>Users/created</td>
    <td>
      The timestamp indicating when the user resource was created.
      <p/><p/>
      The value is generated internally and is read-only.
     </td>  
  </tr>
  <tr>
    <td>Users/groups</td>
    <td>
      The set of groups for which the user is a member.
      <p/><p/>
      The value is read-only.
    </td>  
  </tr>
  <tr>
    <td>Users/admin</td>
    <td>
      Indicates wheather the user has administrative privilieges (<code>true</code>) or not 
      (<code>false</code>). This propery is deprecated and is provided to maintain the REST API V1 
      contract. This information may be found by querying for the user's permissions (or roles).
      <p/><p/>
      The value is settable by an Ambari administrator; otherwise it is read-only.
    </td>  
  </tr>
  <tr>
    <td>Users/ldap_user</td>
    <td>
      Indicates wheather the user was imported from an LDAP server. This propery is deprecated 
      and is provided to maintain the REST API V1 contract. This information may be found by querying
      for the user's authentication sources.
      <p/><p/>
      The value is read-only.
    </td>  
  </tr>
  <tr>
    <td>Users/user_type</td>
    <td>
      The type of user account. Possible values include:
      <ul>
        <li>LOCAL - the user has an Ambari-local password</li>
        <li>LDAP - the user authenticates using an LDAP server</li>
        <li>KERBEROS - the user authenticates using a Kerberos token</li>
        <li>PAM - the user authenticates using PAM</li>
        <li>JWT - the user authenticates using a JWT token from Knox</li>
      </ul>
      This propery is deprecated and is provided to maintain the REST API V1 contract. This 
      information may be found by querying for the user's authentication sources.  
      <p/><p/>
      Since this value contains a single entry, it does not properly indicate what authentication 
      sources a user may use.  However, if the set of authentication sources contains an LDAP source,
      this value will be set to LDAP.
      <p/><p/>
      The value is read-only.
    </td>  
  </tr>
  <tr>
    <td>Users/password</td>
    <td>
      This propery is deprecated and is provided to maintain the REST API V1 contract.  
      This propery may be set when creating or updating a user resource to set it's (Ambari) local 
      password. However, it is expected that a LOCAL authentication source resource is created and
      updated instead.
      <p/><p/>
      The value is write-only.
    </td>  
  </tr>
  <tr>
    <td>Users/old_password</td>
    <td>
      This propery is deprecated and is provided to maintain the REST API V1 contract.  
      This propery may be set when updating a user resource to set a new password for a (Ambari) local 
      password. This value is required when a user is updating their own password.  It is not used
      when an Ambari administrator is updating a user's password.  However, it is expected that a 
      LOCAL authentication source resource is updated instead.
      <p/><p/>
      The value is write-only.
    </td>  
  </tr>
</table>

