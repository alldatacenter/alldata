
<!--
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

Ambari LDAP Configuration
=========

- [Introduction](#introduction)
- [How it Works](#how-it-works)
- [Setting LDAP Configuration Using the CLI](#setup-using-cli)
  - [Silent Mode](#setup-using-cli-silent)
  - [Interactive Mode](#setup-using-cli-interactive)
- [Managing LDAP Configuration Using the REST API](#setup-using-api)
  - [Getting the LDAP Configuration](#setup-using-api-get)
  - [Setting the LDAP Configuration](#setup-using-api-post)
  - [Updating the LDAP Configuration](#setup-using-api-put)
  - [Deleting the LDAP Configuration](#setup-using-api-delete)
- [Implementation Details](#implementation-details)
  - [Configuration Data](#implementation-details-data)
  - [Stack Advisor](#implementation-details-stack-advisor)
    - [AmbariConfiguration class](#implementation-details-stack-advisor-ambariconfiguration)
    - [AmbariLDAPDetails class](#implementation-details-stack-advisor-ambarildapdetails)
    - [Example](#implementation-details-stack-advisor-example)
- [Synching LDAP users with Ambari Using the CLI](#sync-using-cli)

<a name="introduction"></a>
## Introduction

Ambari has a feature to integrate with custom LDAP providers to 
provide LDAP authentication services. To set this up, the Ambari Server CLI may be used by executing
the following on the command line:

```
ambari-server setup-ldap
```
   
After answering a few prompts, the CLI utility would update the `/etc/ambari-server/conf/ambari.properties` 
file and Ambari's own database with the appropriate properties so that when Ambari was restarted, it would rely on the configured LDAP server for authentication. One caveat is that Ambari would need to know about the same set of users who you would like to use in Ambari for authentication.
This is typically done by having Ambari sync with the configured LDAP server. 

As of Ambari 2.7.0, Ambari has the ability to collect LDAP configuration details and configure itself,  as well as, any eligible services for LDAP.  This is done using the Ambari Server CLI, as before; however, once complete, the collected information is stored in the Ambari database and then used to configure selected service. Any altered configurations would cause the relevant services to request to be restarted.  Ambari, on the other hand, will load the LDAP configuration automatically and change its behavior without needing to be restarted (there is only one exception : in case you setup a custom trust store for LDAP; in this case you need to restart Ambari).

<a name="how-it-works"></a>
## How it works

Ambari, by default, is eligible to be configured for LDAP. Any other service that is to be eligible 
for LDAP configuration by Ambari needs to declare this by adding an `ldap` declaration in its service 
definition's `metainfo.xml` file.  The declaration is as follows:

```
<metainfo>
  ...
  <services>
    <service>
    ...
      <ldap>
        <supported>true</supported>
        <ldapEnabledTest>
          {
            "equals": [
              "service-properties/ldap.enabled",
              "true"
            ]
          }      
        <ldapEnabledTest>
      </ldap>
    ...
    </service>
  </services>
</metainfo>
```

Inside the `<ldap>` block, the `<supported>` element with the value of `true` tells Ambari that this
service is eligible to be configured for LDAP.  The `<ldapEnabledTest>` element contains a `JSON structure` 
that describes a Boolean expression indicating whether the service has been configured for LDAP or 
not. 

For example, the `metainfo.xml` file for Ranger:

```
    <ldap>
      <supported>true</supported>
      <ldapEnabledTest>
        {
          "equals": [
            "ranger-admin-site/ranger.authentication.method",
            "LDAP"
          ]
        }      
      <ldapEnabledTest>
    </ldap>
```

This indicates automated LDAP configuration by Ambari is enabled.  It also declares how to test the  service configurations for the LDAP integration status.  If the property value for `ranger.authentication.method` in the `ranger-admin-site` configuration type is `LDAP`, then LDAP has been enabled for Ranger; else LDAP has not yet been enabled.

Once support is declared by a service **and** it is installed, it will be listed as an eligible service
while selecting services for which to enable LDAP via the Ambari Server CLI.

Example:

```
# ambari-server setup-ldap
...
Manage LDAP configurations for eligible services [y/n] (n)? y
 Use LDAP for all services [y/n] (n)? n
   Use LDAP for RANGER [y/n] (n)? y
...
```

When the Ambari CLI is complete, the LDAP configuration data will be stored in the Ambari database (and, optionally, some in `ambari.properties`) 
and then the relevant service configurations will be updated.  Service configurations are updated using 
the stack advisor. Any LDAP-specific recommendations generated by the stack advisor are silently 
and automatically applied. If changes are detected, the relevant services will request to be restarted.

To indicate LDAP-specific changes via the stack advisor. The stack's stack advisor or the service's 
service advisor needs to override the `ServiceAdvisor.getServiceConfigurationRecommendationsForLDAP` 
function:

```
  def getServiceConfigurationRecommendationsForLDAP(self, configurations, clusterData, services, hosts):
    ...
```

<a name="setup-using-cli"></a>
## Setting LDAP Configuration Using the CLI

To enable or disable LDAP configuration via the Ambari Server CLI, use the following command:

```
ambari-server setup-ldap
```

This command works in a silent or an interactive mode. 

<a name="setup-using-cli-silent"></a>
### Silent Mode

In silent mode all configuration details may be set on the command line via arguments.  However, some of the arguments are passwords that are

 - needed for authentication to use Ambari's REST API
 - needed for the LDAP manager to connect to the LDAP server

These arguments may be left off of the command causing the CLI to prompt for it. 

For example:

```
# ambari-server setup-ldap --ambari-admin-username=admin ...
Using python  /usr/bin/python
Enter Ambari Admin password:
```

The following arguments must be supplied when **enabling** LDAP:

```
  --ldap-url=<primary LDAP URL in host:port format>
                        Primary URL for LDAP (must not be used together with
                        --ldap-primary-host and --ldap-primary-port)
  --ldap-primary-host=<primary LDAP host>
                        Primary Host for LDAP (must not be used together with
                        --ldap-url)
  --ldap-primary-port=<primary LDAP port>
                        Primary Port for LDAP (must not be used together with
                        --ldap-url)
  --ldap-ssl=<true|false>
                        Whether to use SSL for LDAP or not
  --ldap-type=<type>
                        Specifies the LDAP provider type [AD/IPA/Generic] for
                        offering defaults for missing options.
  --ldap-user-class=<user object class attribute>
                        User Object Class attribute name for LDAP
  --ldap-user-attr=<user name attribute>
                        User Name attribute name for LDAP
  --ldap-user-group-member-attr=<user group member attribute>
                        User Group Member Attribute for LDAP
  --ldap-group-class=<group object class>
                        Group Object Class attribute for LDAP
  --ldap-group-attr=<group name attrbiute>
                        Group Name attribute for LDAP
  --ldap-member-attr=<group member attribute>
                        Group Membership attribute name for LDAP
  --ldap-dn=<dn>
                        Distinguished Name attribute for LDAP
  --ldap-base-dn=<base DN>
                        Base DN for LDAP
  --ldap-manager-dn=<manager DN>
                        Manager DN for LDAP
  --ldap-save-settings
                        Saves the given configuration without giving a chance
                        for review
  --ldap-referral=<referral method>
                        Referral method [follow/ignore] for LDAP
  --ldap-bind-anonym=<true|false>
                        Whether to bind anonymously for LDAP
  --ldap-sync-username-collisions-behavior=<convert|skip>
                        Handling behavior for username collisions for LDAP sync
  --ldap-sync-disable-endpoint-identification=<true|false>
                        Determines whether to disable endpoint identification
                        (hostname verification) during SSL handshake for LDAP
                        sync. This option takes effect only if --ldap-ssl is
                        set to 'true'
  --ldap-force-lowercase-usernames=<true|false>
                        Declares whether to force the ldap user name to be
                        lowercase or leave as-is
  --ldap-pagination-enabled=LDAP_PAGINATION_ENABLED
                        Determines whether results from LDAP are paginated
                        when requested
  --ldap-force-setup
                        Forces the use of LDAP even if other (i.e. PAM)
                        authentication method is configured already or if
                        there is no authentication method configured at all
  --ldap-enabled-ambari=<true|false>
                        Whether to enable/disable LDAP authentication
                        for Ambari, itself
  --ldap-manage-services=<true|false>
                        Whether Ambari should manage the LDAP configurations
                        for specified services
  --ldap-enabled-services=<*|service names>
                        A comma separated list of services that are expected to be 
                        configured for LDAP (you are allowed to use '*' to indicate
                        ALL services)
  --ambari-admin-username=<username>
                        Ambari administrator username for accessing Ambari's
                        REST API
```

Optionally, the following arguments may be set:

```
  --ambari-admin-password=<password>
                        Ambari administrator password for accessing Ambari's
                        REST API
  --ldap-manager-password=LDAP_MANAGER_PASSWORD
                        Manager Password For LDAP
```

For more options and up-to-date information, execute the following command:
```
ambari-server setup-ldap --help
```

<a name="setup-using-cli-interactive"></a>
### Interactive Mode

In interactive mode some configuration details may be set on the command line via arguments and the CLI will prompt for the rest.

```
# ambari-server setup-ldap
Using python  /usr/bin/python

Enter Ambari Admin login: admin
Enter Ambari Admin password:

Fetching LDAP configuration from DB. No configuration.
Please select the type of LDAP you want to use [AD/IPA/Generic](Generic):
Primary LDAP Host (ldap.ambari.apache.org):
Primary LDAP Port (389):
Secondary LDAP Host <Optional>:
Secondary LDAP Port <Optional>:
Use SSL [true/false] (false):
User object class (posixUser):
User ID attribute (uid):
User group member attribute (memberof):
Group object class (posixGroup):
Group name attribute (cn):
Group member attribute (memberUid):
Distinguished name attribute (dn):
Search Base (dc=ambari,dc=apache,dc=org):
Referral method [follow/ignore] (follow):
Bind anonymously [true/false] (false):
Bind DN (uid=ldapbind,cn=users,dc=ambari,dc=apache,dc=org):
Enter Bind DN Password:
Confirm Bind DN Password:
Handling behavior for username collisions [convert/skip] for LDAP sync (skip):
Force lower-case user names [true/false]:false
Results from LDAP are paginated when requested [true/false]:false
Use LDAP authentication for Ambari [y/n] (n)? y
Manage LDAP configurations for eligible services [y/n] (n)? y
Manage LDAP for all services [y/n] (n)? y
====================
Review Settings
====================
Primary LDAP Host (ldap.ambari.apache.org):  ldap.ambari.apache.org
Primary LDAP Port (389):  389
Use SSL [true/false] (false):  false
User object class (posixUser):  posixUser
User ID attribute (uid):  uid
User group member attribute (memberof):  memberof
Group object class (posixGroup):  posixGroup
Group name attribute (cn):  cn
Group member attribute (memberUid):  memberUid
Distinguished name attribute (dn):  dn
Search Base (dc=ambari,dc=apache,dc=org):  dc=ambari,dc=apache,dc=org
Referral method [follow/ignore] (follow):  follow
Bind anonymously [true/false] (false):  false
Handling behavior for username collisions [convert/skip] for LDAP sync (skip):  skip
Force lower-case user names [true/false]: false
Results from LDAP are paginated when requested [true/false]: false
ambari.ldap.connectivity.bind_dn: uid=ldapbind,cn=users,dc=ambari,dc=apache,dc=org
ambari.ldap.connectivity.bind_password: *****
ambari.ldap.manage_services: true
ambari.ldap.enabled_services: *
Save settings [y/n] (y)? y
Saving LDAP properties...
Saving LDAP properties finished
Ambari Server 'setup-ldap' completed successfully.
```

In either case, the CLI collects the data and submits it to Ambari via the REST API.  This then 
triggers processes in Ambari to enable LDAP as needed.
  
<a name="setup-using-api"></a>
## Managing LDAP Configuration Using the REST API

The LDAP configuration may be managed using Ambari's REST API, via the following entry point:
 
```
/api/v1/services/AMBARI/components/AMBARI_SERVER/configurations
```

This entry point supports the following request types:
- GET - retrieve the LDAP configuration data
- POST - explicitly set the LDAP configuration data, replacing all properties
- PUT - update the LDAP configuration data, only the specified properties are updated 
- DELETE - removes the LDAP configuration data

<a name="setup-using-api-get"></a>
### Getting the LDAP Configuration
To retrieve the LDAP configuration data:
```
GET /api/v1/services/AMBARI/components/AMBARI_SERVER/configurations/ldap-configuration
```
Example 404 response:
```
{
  "status" : 404,
  "message" : "The requested resource doesn't exist: RootServiceComponentConfiguration not found where Configuration/service_name=AMBARI AND Configuration/component_name=AMBARI_SERVER AND Configuration/category=ldap-configuration."
}
```

Example 200 response:
```
{
  "href" : "http://ambari_server.host:8080/api/v1/services/AMBARI/components/AMBARI_SERVER/configurations/ldap-configuration",
  "Configuration" : {
    "category" : "ldap-configuration",
    "component_name" : "AMBARI_SERVER",
    "service_name" : "AMBARI",
    "properties" : {
      "ambari.ldap.advanced.collision_behavior" : "skip",
      "ambari.ldap.advanced.force_lowercase_usernames" : "false",
      "ambari.ldap.advanced.pagination_enabled" : "false",
      "ambari.ldap.advanced.referrals" : "follow",
      "ambari.ldap.attributes.dn_attr" : "dn",
      "ambari.ldap.attributes.group.member_attr" : "memberUid",
      "ambari.ldap.attributes.group.name_attr" : "cn",
      "ambari.ldap.attributes.group.object_class" : "posixGroup",
      "ambari.ldap.attributes.user.group_member_attr" : "memberof",
      "ambari.ldap.attributes.user.name_attr" : "uid",
      "ambari.ldap.attributes.user.object_class" : "posixUser",
      "ambari.ldap.attributes.user.search_base" : "dc=ambari,dc=apache,dc=org",
      "ambari.ldap.authentication.enabled" : "true",
      "ambari.ldap.connectivity.anonymous_bind" : "false",
      "ambari.ldap.connectivity.bind_dn" : "uid=ldapbind,cn=users,dc=ambari,dc=apache,dc=org",
      "ambari.ldap.connectivity.bind_password" : "/etc/ambari-server/conf/ldap-password.dat",
      "ambari.ldap.connectivity.server.host" : "ldap.ambari.apache.org",
      "ambari.ldap.connectivity.server.port" : "389",
      "ambari.ldap.connectivity.use_ssl" : "false",
      "ambari.ldap.enabled_services" : "*",
      "ambari.ldap.manage_services" : "true"
    },
    "property_types" : {
      "ambari.ldap.advanced.collision_behavior" : "PLAINTEXT",
      "ambari.ldap.advanced.force_lowercase_usernames" : "PLAINTEXT",
      "ambari.ldap.advanced.pagination_enabled" : "PLAINTEXT",
      "ambari.ldap.advanced.referrals" : "PLAINTEXT",
      "ambari.ldap.attributes.dn_attr" : "PLAINTEXT",
      "ambari.ldap.attributes.group.member_attr" : "PLAINTEXT",
      "ambari.ldap.attributes.group.name_attr" : "PLAINTEXT",
      "ambari.ldap.attributes.group.object_class" : "PLAINTEXT",
      "ambari.ldap.attributes.user.group_member_attr" : "PLAINTEXT",
      "ambari.ldap.attributes.user.name_attr" : "PLAINTEXT",
      "ambari.ldap.attributes.user.object_class" : "PLAINTEXT",
      "ambari.ldap.attributes.user.search_base" : "PLAINTEXT",
      "ambari.ldap.authentication.enabled" : "PLAINTEXT",
      "ambari.ldap.connectivity.anonymous_bind" : "PLAINTEXT",
      "ambari.ldap.connectivity.bind_dn" : "PLAINTEXT",
      "ambari.ldap.connectivity.bind_password" : "PASSWORD",
      "ambari.ldap.connectivity.server.host" : "PLAINTEXT",
      "ambari.ldap.connectivity.server.port" : "PLAINTEXT",
      "ambari.ldap.connectivity.use_ssl" : "PLAINTEXT",
      "ambari.ldap.enabled_services" : "PLAINTEXT",
      "ambari.ldap.manage_services" : "PLAINTEXT"
    }
  }
}
```

<a name="setup-using-api-post"></a>
### Setting the LDAP Configuration
To set the LDAP configuration data, replacing any previously existing data:
```
POST /api/v1/services/AMBARI/components/AMBARI_SERVER/configurations
```
Example payload:
```
{
    "Configuration": {
        "category": "ldap-configuration",
        "properties": {
            "ambari.ldap.connectivity.server.port": "389",
            "ambari.ldap.advanced.pagination_enabled": "false",
            "ambari.ldap.attributes.user.search_base": "dc=ambari,dc=apache,dc=org",
            "ambari.ldap.attributes.user.object_class": "posixUser",
            "ambari.ldap.attributes.user.group_member_attr": "memberof",
            "ambari.ldap.attributes.group.member_attr": "memberUid",
            "ambari.ldap.enabled_services": "*",
            "ambari.ldap.authentication.enabled": "true",
            "ambari.ldap.attributes.user.name_attr": "uid",
            "ambari.ldap.advanced.collision_behavior": "skip",
            "ambari.ldap.advanced.force_lowercase_usernames": "false",
            "ambari.ldap.connectivity.bind_password": "/etc/ambari-server/conf/ldap-password.dat",
            "ambari.ldap.attributes.group.object_class": "posixGroup",
            "ambari.ldap.manage_services": "true",
            "ambari.ldap.advanced.referrals": "follow",
            "ambari.ldap.attributes.dn_attr": "dn",
            "ambari.ldap.connectivity.anonymous_bind": "false",
            "ambari.ldap.connectivity.use_ssl": "false",
            "ambari.ldap.connectivity.server.host": "ldap.ambari.apache.org",
            "ambari.ldap.attributes.group.name_attr": "cn",
            "ambari.ldap.connectivity.bind_dn": "uid=ldapbind,cn=users,dc=ambari,dc=apache,dc=org"
        }
    }
}
```

<a name="setup-using-api-put"></a>
### Updating the LDAP Configuration
To update the LDAP configuration data, only replacing or adding specific properties:
```
PUT /api/v1/services/AMBARI/components/AMBARI_SERVER/configurations/ldap-configuration
```
Example payload:
```
{
  "Configuration": {    
    "properties": {
      "ambari.ldap.manage_services" : "true",
      "ambari.ldap.enabled_services": "AMBARI, RANGER"
    }
  }
}
```

<a name="setup-using-api-delete"></a>
### Deleting the LDAP Configuration
To delete the LDAP configuration data, removing all properties:
```
DELETE /api/v1/services/AMBARI/components/AMBARI_SERVER/configurations/ldap-configuration
```

<a name="implementation-details"></a>
## Implementation Details

<a name="implementation-details-data"></a>
### Configuration Data

The LDAP configuration data is stored in the `ambari_configuration` table of the Ambari database. This table contains a set of property names and their values, grouped by a _category_ name.  For the LDAP configuration data, the properties are stored using the category name `ldap-configuration`. 

The only properties that can be inserted in this table are listed in `org.apache.ambari.server.configuration.AmbariServerConfigurationKey` with `category` of `AmbariServerConfigurationCategory.LDAP_CONFIGURATION`. In addition to this enumeration a helper class has been created to provide a common Java interface for  LDAP related properties; this class is called `org.apache.ambari.server.ldap.domain.AmbariLdapConfiguration`

Within `Guice`  a custom `Provider<AmbariLdapConfiguration>` implementation should be used to get an instance of LDAP related data; this class is called `org.apache.ambari.server.ldap.service.AmbariLdapConfigurationProvider`. It's very important that this provider cannot be used until the `JPA` context is not initialized (since the underlying implementation fetches the configuration from the database).
 
 
<a name="implementation-details-stack-advisor"></a>
### Stack Advisor

After updating the LDAP configuration data via the CLI (which invokes the REST API) or the REST API, Ambari triggers a process to reconfigure services as needed.  This happens in the background where as the only indication that something has changed is that services may request to be restarted.

The process that is triggered in the background invokes the Stack Advisor, requesting LDAP-specific recommendations.  It is expected that a service supporting this feature overrides the 
`getServiceConfigurationRecommendationsForLDAP` function and adheres to the convention that only the LDAP-relevant configurations are altered.  
 
 ```
def getServiceConfigurationRecommendationsForLDAP(self, configurations, clusterData, services, hosts):
  ...
 
 ```    
 
To help determine how this function should behave, the following classes are available:
 
 - `AmbariConfiguration`
 - `AmbariLDAPDetails`
 
<a name="implementation-details-stack-advisor-ambariconfiguration"></a>
#### AmbariConfiguration class

`AmbariConfiguration` (located in `ambari-server/src/main/resources/stacks/ambari_configuration.py`) is an entry point into configuration-specific information about Ambari.  It 
uses data from the `ambari-server-configuration` section of the `services.json` file to build 
_category_-specific utility classes. An instance of this object is created using the base StackAdvisor
class's  `get_ambari_configuration` function. For example:

```
ambari_configuration = self.get_ambari_configuration(services)
```

<a name="implementation-details-stack-advisor-ambarildapdetails"></a>
#### AmbariLDAPDetails class

The `AmbariLDAPDetails` class contains utility functions to get and interpret data from the 
`ldap-configiration` data parsed from the `ambari-server-configuration` section of the `services.json` file.  To create an instance of this class, the `AmbariLDAPDetails.get_ambari_ldap_details` function is used. For example:
```
ambari_ldap_details = ambari_configuration.get_ambari_ldap_details() if ambari_configuration else None
```

The public API of this class mirrors above mentioned Java class's (`org.apache.ambari.server.ldap.domain.AmbariLdapConfiguration`) public API; except for trust store related API and getLdapServerProperties which we do not need in Pyton side.

 <a name="implementation-details-stack-advisor-example"></a>
 #### Stack Advisor Example
 
```
   def recommendConfigurationsForLDAP(self, configurations, clusterData, services, hosts):
1    ambari_configuration = self.get_ambari_configuration(services)
2    ambari_ldap_details = ambari_configuration.get_ambari_ldap_details() if ambari_configuration else None

3    if ambari_ldap_details and ambari_ldap_details.is_managing_services():
       putProperty = self.putProperty(configurations, "service-site", services)

4      if ambari_ldap_details.should_enable_ldap('MY_SERVICE'):
        if ambari_ldap_details.get_user_search_base() is not None:
          putProperty('my.service.ldap.searchBase', ambari_ldap_details.get_user_search_base())
```

Explanations:
1. Obtain an instance of the `AmbariConfiguration` object using data from the `services` dictionary.
2. Retrieve the `AmbariLDAPDetails` object by calling the `AmbariConfiguration`'s `get_ambari_ldap_details` 
function. 
3. Test to see if Ambari should be managing the LDAP configuration for installed services.  If so, 
continue; else, there is nothing left to do but exit the function. 
4. Test to see if Ambari should enable LDAP for the service with the name of `MY_SERVICE`. If so, set the relevant properties using values from the `AmbariLDAPDetails` object.

**Note:**  `should_enable_ldap` should be called to determine what action
to take. This is because even though Ambari is managing the LDAP configuration for installed services, it may not be managing the LDAP-configuration for the particular service. So one of the following actions are to be performed: enable or ignore.

<a name="sync-using-cli"></a>
## Synching LDAP users with Ambari Using the CLI

After setting up your LDAP integration, you must synchronize LDAP users and groups with Ambari, using the `ambari-server sync-ldap [option]` utility. Please read [this guide](https://docs.hortonworks.com/HDPDocuments/HDP3/HDP-3.0.1/ambari-authentication-ldap-ad/content/authe_ldapad_synchronizing_ldap_users_and_groups.html) carefully for further information.
