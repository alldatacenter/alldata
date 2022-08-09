---
name: Atlas Simple Authorizer
route: /AtlasSimpleAuthorizer
menu: Documentation
submenu: Security
---

import  themen  from 'theme/styles/styled-colors';
import  * as theme  from 'react-syntax-highlighter/dist/esm/styles/hljs';
import SyntaxHighlighter from 'react-syntax-highlighter';

# Atlas Simple Authorizer

## Setting up Atlas to use Simple Authorizer

As detailed in Atlas [Authorization Model](#/AuthorizationModel), Apache Atlas supports a pluggable authorization
model. Simple authorizer is the default authorizer implementation included in Apache Atlas. Simple authorizer uses
policies defined in a JSON file. This document provides details of steps to configure Apache Atlas to use the simple
authorizer and details of the JSON file format containing authorization policies.


##  Configure Apache Atlas

To configure Apache Atlas to use simple authorizer, include the following properties in application.properties config file:

<SyntaxHighlighter wrapLines={true} language="shell" style={theme.dark}>
{`atlas.authorizer.impl=simple
atlas.authorizer.simple.authz.policy.file=/etc/atlas/conf/atlas-simple-authz-policy.json`}
</SyntaxHighlighter>

Please note that if the policy file location specified is not an absolute path, the file will be looked up in following paths:
   * Apache Atlas configuration directory (specified by system property =atlas.conf=)
   * Apache Atlas server's current directory
   * CLASSPATH

### Policy file format

Simple authorizer uses =roles= to group permissions, which can then be assigned to users and user-groups. Following examples
would help to understand the details of the policy file format:

###  Roles
Following policy file defines 3 roles:
   * ROLE_ADMIN: has all permissions
   * PROD_READ_ONLY: has access to read entities having qualifiedName ending with "@prod"
   * TEST_ALL_ACCESS: has all access to entities having qualifiedName ending with "@test"

Simple authorizer supports Java reg-ex to specify values for privilege/entity-type/entity-id/classification/typeName/typeCategory.

<SyntaxHighlighter wrapLines={true} language="shell" style={theme.dark}>
{`{
  "roles": {
    "ROLE_ADMIN": {
      "adminPermissions": [
        {
          "privileges": [ ".*" ]
        }
      ],

      "entityPermissions": [
        {
          "privileges":      [ ".*" ],
          "entityTypes":     [ ".*" ],
          "entityIds":       [ ".*" ],
          "classifications": [ ".*" ]
        }
      ],

      "typePermissions": [
        {
          "privileges":     [ ".*" ],
          "typeCategories": [ ".*" ],
          "typeNames":      [ ".*" ]
        }
      ]
    },

    "PROD_READ_ONLY" : {
      "entityPermissions": [
        {
          "privileges":      [ "entity-read", "entity-read-classification" ],
          "entityTypes":     [ ".*" ],
          "entityIds":       [ ".*@prod" ],
          "classifications": [ ".*" ]
        }
    }

    "TEST_ALL_ACCESS" : {
      "entityPermissions": [
        {
          "privileges":      [ ".*" ],
          "entityTypes":     [ ".*" ],
          "entityIds":       [ ".*@test" ],
          "classifications": [ ".*" ]
        }
    }
  },

  "userRoles": {
   ...
  },

  "groupRoles": {
   ...
  }
}`}

</SyntaxHighlighter>

###  Assign Roles to Users and User Groups

Roles defined above can be assigned (granted) to users as shown below:

<SyntaxHighlighter wrapLines={true} language="shell" style={theme.dark}>
{`{
  "roles": {
   ...
  },
  "userRoles": {
    "admin":   [ "ROLE_ADMIN" ],
    "steward": [ "DATA_STEWARD" ],
    "user1":   [ "PROD_READ_ONLY" ],
    "user2":   [ "TEST_ALL_ACCESS" ],
    "user3":   [ "PROD_READ_ONLY", "TEST_ALL_ACCESS" ],
  },
  "groupRoles": {
   ...
  }
}`}
</SyntaxHighlighter>


Roles can be assigned (granted) to user-groups as shown below. An user can belong to multiple groups; roles assigned to
all groups the user belongs to will be used to authorize the access.

<SyntaxHighlighter wrapLines={true} language="shell" style={theme.dark}>
{`{
  "roles": {
   ...
  },
  "userRoles": {
   ...
  },
  "groupRoles": {
    "admins":        [ "ROLE_ADMIN" ],
    "dataStewards":  [ "DATA_STEWARD" ],
    "testUsers":     [ "TEST_ALL_ACCESS" ],
    "prodReadUsers": [ "PROD_READ_ONLY" ]
  }
}`}
</SyntaxHighlighter>
