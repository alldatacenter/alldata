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

Ambari Single Sign-on Configuration
=========

- [Introduction](#introduction)
- [How it Works](#how-it-works)
- [Setting SSO Configuration Using the CLI](#setup-using-cli)
  - [Silent Mode](#setup-using-cli-silent)
  - [Interactive Mode](#setup-using-cli-interactive)
- [Managing SSO Configuration Using the REST API](#setup-using-api)
  - [Getting the SSO Configuration](#setup-using-api-get)
  - [Setting the SSO Configuration](#setup-using-api-post)
  - [Updating the SSO Configuration](#setup-using-api-put)
  - [Deleting the SSO Configuration](#setup-using-api-delete)
- [Implementation Details](#implementation-details)
  - [Configuration Data](#implementation-details-data)
  - [Stack Advisor](#implementation-details-stack-advisor)
    - [AmbariConfiguration class](#implementation-details-stack-advisor-ambariconfiguration)
    - [AmbariSSODetails class](#implementation-details-stack-advisor-ambarissodetails)
    - [Example](#implementation-details-stack-advisor-example)

<a name="introduction"></a>
## Introduction

Ambari has a feature to integrate with [Apache Knox](https://knox.apache.org/) to 
provide single sign-on (SSO) services. To set this up, the Ambari Server CLI may be used by executing
the following on the command line:

```
ambari-server setup-sso
```
   
After answering a few prompts, the CLI utility would update the `/etc/ambari-server/conf/ambari.properties` 
file with the appropriate properties so that when Ambari was restarted, it would rely on Knox for 
authentication. One caveat is that both Ambari and Knox would need to know about the same set of users.
This is typically done by having both Ambari and Knox sync with the same LDAP server. 

See [Setting up Knox SSO for Ambari](https://docs.hortonworks.com/HDPDocuments/HDP2/HDP-2.6.1/bk_security/content/setting_up_knox_sso_for_ambari.html) 
for more details on how it *used* to work. 

One issue with this feature is that it only sets up Ambari to use Knox. However, other services have 
the ability to integrate with Knox and each requires the user to configure separately.  This is
inconvenient and error prone for the user. 

As of Ambari 2.7.0, Ambari has the ability to collect SSO configuration details and configure itself, 
as well as, any eligible services for SSO.  This is done using the Ambari Server CLI, as before; however,
once complete, the collected information is stored in the Ambari database and then used to configure
selected service. Any altered configurations would cause the relevant services to request to be 
restarted.  Ambari, on the other hand, will load the SSO configuration automatically and change its
behavior without needing to be restarted. 

<a name="how-it-works"></a>
## How it works

Ambari, by default, is eligible to be configured for SSO. Any other service that is to be eligible 
for SSO configuration by Ambari needs to declare this by adding an "sso" declaration in its service 
definition's `metainfo.xml` file.  The declaration is as follows:

```
<metainfo>
  ...
  <services>
    <service>
    ...
      <sso>
        <supported>true</supported>
        <ssoEnabledTest>
          {
            "equals": [
              "service-properties/sso.knox.enabled",
              "true"
            ]
          }      
        <ssoEnabledTest>
      </sso>
    ...
    </service>
  </services>
</metainfo>
```  

Inside the `<sso>` block, the `<supported>` element with the value of "true" tells Ambari that this
service is eligible to be configured for SSO.  The `<ssoEnabledTest>` element contains a JSON structure 
that describes a Boolean expression indicating whether the service has been configured for SSO or 
not.  For backwards compatibility with Ambari 2.7.0, the `<enabledConfiguration>` element 
remains supported.  It contains a property specification (`config-type`/`property_name`) that 
indicates the boolean property to check to tell whether the service has been configured for SSO or 
not.  

For example, the `metainfo.xml` file for Atlas:

```
    <sso>
      <supported>true</supported>
      <ssoEnabledTest>
        {
          "equals": [
            "application-properties/atlas.sso.knox.enabled",
            "true"
          ]
        }      
      <ssoEnabledTest>
    </sso>
```

This indicates automated SSO configuration by Ambari is enabled.  It also declares how to test the 
service configurations for the SSO integration status.  If the property value for 
`atlas.sso.knox.enabled` in the `application-properties` configuration type is "true", then SSO has 
been enabled for Atlas; else SSO has not yet been enabled.

For backwards compatibility, the following is accepted as well:

```
    <sso>
      <supported>true</supported>
      <enabledConfiguration>application-properties/atlas.sso.knox.enabled</enabledConfiguration>
    </sso>
```

##### Kerberos 
Some services require that Kerberos is enabled in order to allow SSO to be enabled.  If this is the 
case, the service is to declare it by setting `<kerberosRequired>` to "true" inside the `<sso>` block.
If Kerberos is required, then a `<kerberosEnabledTest>` block is needed, as well, to tell Ambari 
how to calculate whether Kerberos is enabled for that service or not.  It must be noted that the 
`<kerberosEnabledTest>` block is a sibling to the `<sso>` block.  The Kerberos test can be used for 
SSO calculations, but is not directly related to the SSO facility. For example:

```
    <sso>
      <supported>true</supported>
      <kerberosRequired>true</kerberosRequired>
      <ssoEnabledTest>
        {
          "equals": [
            "service-site/knox.sso.enabled",
            "true"
          ]
        }      
      <ssoEnabledTest>
    </sso>

    <kerberosEnabledTest>
      {
        "equals": [
          "service-site/service.authentication.type",
          "kerberos"
        ]
      }      
    <kerberosEnabledTest>
```

Once support is declared by a service **and** it is installed, it will be listed as an eligible service
while selecting services for which to enable SSO via the Ambari Server CLI.  However, if Kerberos 
is required and not enabled, the service may be filtered from that list. 

Example:
```
# ambari-server setup-sso
...
Manage SSO configurations for eligible services [y/n] (n)? y
 Use SSO for all services [y/n] (n)? n
   Use SSO for ATLAS [y/n] (n)? y
...
```

When the Ambari CLI is complete, the SSO configuration data will be stored in the Ambari database and
then the relevant service configurations will be updated.  Service configurations are updated using 
the stack advisor. Any SSO-specific recommendations generated by the stack advisor are silently 
and automatically applied.  If changes are detected, the relevant services will request to be restarted. 

To indicate SSO-specific changes via the stack advisor. The stack's stack advisor or the service's 
service advisor needs to override the `ServiceAdvisor.getServiceConfigurationRecommendationsForSSO` 
function:

```
  def getServiceConfigurationRecommendationsForSSO(self, configurations, clusterData, services, hosts):
    ...

```      

<a name="setup-using-cli"></a>
## Setting SSO Configuration Using the CLI

To enable or disable SSO configuration via the Ambari Server CLI, use the following command:

```
ambari-server setup-sso
```

This command works in a silent or an interactive mode. 

<a name="setup-using-cli-silent"></a>
### Silent Mode

In silent mode all configuration details may be set on the command line via arguments.  However, one 
argument is a password that is needed for authentication to use Ambari's REST API.  This argument 
may be left off of the command causing the CLI to prompt for it. 

For example:
```
# ambari-server setup-sso --ambari-admin-username=admin ...
Using python  /usr/bin/python
Setting up SSO authentication properties...
Enter Ambari Admin password:
```

The following arguments must be supplied when **enabling** SSO:

```
  --sso-enabled=true
                        Indicates whether to enable/disable SSO
  --sso-enabled-ambari=<true|false>
                        Indicates whether to enable/disable SSO authentication
                        for Ambari, itself
  --sso-manage-services=<true|false>
                        Indicates whether Ambari should manage the SSO
                        configurations for specified services
  --sso-enabled-services=<service list>
                        A comma separated list of services that are expected
                        to be configured for SSO (you are allowed to use '*'
                        to indicate ALL services)
  --sso-provider-url=<URL>
                        The URL of SSO provider; this must be provided when
                        --sso-enabled is set to 'true'
  --sso-public-cert-file=SSO_PUBLIC_CERT_FILE
                        The path where the public certificate PEM is located;
                        this must be provided when --sso-enabled is set to
                        'true'
  --ambari-admin-username=<username>
                        Ambari administrator username for accessing Ambari's
                        REST API
```

Optionally, the following arguments may be set:

```
  --sso-jwt-cookie-name=<cookie name>
                        The name of the JWT cookie 
                        Default value: hadoop-jwt
  --sso-jwt-audience-list=<audience list>
                        A comma separated list of JWT audience(s)
                        Default value <empty>
  --ambari-admin-password=<password>
                        Ambari administrator password for accessing Ambari's
                        REST API
```

The following arguments must be supplied when **disabling** SSO:

```
  --sso-enabled=false
                        Indicates whether to enable/disable SSO
  --ambari-admin-username=AMBARI_ADMIN_USERNAME
                        Ambari administrator username for accessing Ambari's
                        REST API
```

Optionally, the following arguments may be set:

```
  --ambari-admin-password=<password>
                        Ambari administrator password for accessing Ambari's
                        REST API
```

For more options and up-to-date information, execute the following command:
```
ambari-server setup-sso --help
```

<a name="setup-using-cli-interactive"></a>
### Interactive Mode

In interactive mode some configuration details may be set on the command line via arguments and the
CLI will prompt for the rest.

```
# ambari-server setup-sso
Using python  /usr/bin/python
Setting up SSO authentication properties...
Enter Ambari Admin login: admin
Enter Ambari Admin password:

SSO is currently not configured
Do you want to configure SSO authentication [y/n] (y)? y
Provider URL (https://knox.example.com:8443/gateway/knoxsso/api/v1/websso):
Public Certificate PEM (empty line to finish input):
MIICVTCCAb6gAwIBAgIIKwH4/V7SjxEwDQYJKoZIhvcNAQEFBQAwbTELMAkGA1UE
...
6fSqZSwbBXwFKf0gIBttufyldePpAsM7Yg==

Use SSO for Ambari [y/n] (n)? y
Manage SSO configurations for eligible services [y/n] (n)? y
 Use SSO for all services [y/n] (n)? n
   Use SSO for ATLAS [y/n] (n)? y
JWT Cookie name (hadoop-jwt):
JWT audiences list (comma-separated), empty for any ():
Ambari Server 'setup-sso' completed successfully.
```

In either case, the CLI collects the data and submits it to Ambari via the REST API.  This then 
triggers processes in Ambari to enable or disable SSO as needed. 
  
<a name="setup-using-api"></a>
## Managing SSO Configuration Using the REST API

The SSO configuration may be managed using Ambari's REST API, via the following entry point:
 
```
/api/v1/services/AMBARI/components/AMBARI_SERVER/configurations
```

This entry point supports the following request types:
- GET - retrieve the SSO configuration data
- POST - explicitly set the SSO configuration data, replacing all properties
- PUT - update the SSO configuration data, only the specified properties are updated 
- DELETE - removes the SSO configuration data

<a name="setup-using-api-get"></a>
### Getting the SSO Configuration
To retrieve the SSO configuration data:
```
GET /api/v1/services/AMBARI/components/AMBARI_SERVER/configurations/sso-configuration
```
Example 404 response:
```
{
  "status" : 404,
  "message" : "The requested resource doesn't exist: RootServiceComponentConfiguration not found where Configuration/service_name=AMBARI AND Configuration/component_name=AMBARI_SERVER AND Configuration/category=sso-configuration."
}
```

Example 200 response:
```
{
  "href" : "http://ambari_server.host:8080/api/v1/services/AMBARI/components/AMBARI_SERVER/configurations/sso-configuration",
  "Configuration" : {
    "category" : "sso-configuration",
    "component_name" : "AMBARI_SERVER",
    "service_name" : "AMBARI",
    "properties" : {
      "ambari.sso.authentication.enabled" : "false",
      "ambari.sso.enabled_services" : "AMBARI, ATLAS",
      "ambari.sso.jwt.audiences" : "",
      "ambari.sso.jwt.cookieName" : "hadoop-jwt",
      "ambari.sso.manage_services" : "true",
      "ambari.sso.provider.certificate" : "-----BEGIN CERTIFICATE-----\nMIIC...TYptEVg==\n-----END CERTIFICATE-----",
      "ambari.sso.provider.originalUrlParamName" : "originalUrl",
      "ambari.sso.provider.url" : "https://knox.host:8443/gateway/knoxsso/api/v1/websso"
    },
    "property_types" : {
      "ambari.sso.authentication.enabled" : "PLAINTEXT",
      "ambari.sso.enabled_services" : "PLAINTEXT",
      "ambari.sso.jwt.audiences" : "PLAINTEXT",
      "ambari.sso.jwt.cookieName" : "PLAINTEXT",
      "ambari.sso.manage_services" : "PLAINTEXT",
      "ambari.sso.provider.certificate" : "PLAINTEXT",
      "ambari.sso.provider.originalUrlParamName" : "PLAINTEXT",
      "ambari.sso.provider.url" : "PLAINTEXT"
    }
  }
}
```

<a name="setup-using-api-post"></a>
### Setting the SSO Configuration
To set the SSO configuration data, replacing any previously existing data:
```
POST /api/v1/services/AMBARI/components/AMBARI_SERVER/configurations
```
Example payload:
```
{
  "Configuration": {    
    "category" : "sso-configuration",
    "properties": {
      "ambari.sso.manage_services" : "true",
      "ambari.sso.enabled_services": "AMBARI, ATLAS",
      "ambari.sso.provider.url": "https://knox.host:8443/gateway/knoxsso/api/v1/websso",
      "ambari.sso.provider.certificate": "-----BEGIN CERTIFICATE-----\nMIIC...TYptEVg==\n-----END CERTIFICATE-----",
      "ambari.sso.authentication.enabled": "true",
      "ambari.sso.jwt.audiences": "",
      "ambari.sso.jwt.cookieName": "hadoop-jwt",
      "ambari.sso.provider.originalUrlParamName": "originalUrl"
    }
  }
}
```

<a name="setup-using-api-put"></a>
### Updating the SSO Configuration
To update the SSO configuration data, only replacing or adding specific properties:
```
PUT /api/v1/services/AMBARI/components/AMBARI_SERVER/configurations/sso-configuration
```
Example payload:
```
{
  "Configuration": {    
    "properties": {
      "ambari.sso.manage_services" : "true",
      "ambari.sso.enabled_services": "AMBARI, ATLAS, RANGER",
      "ambari.sso.authentication.enabled": "false"
    }
  }
}
```

<a name="setup-using-api-delete"></a>
### Deleting the SSO Configuration
To delete the SSO configuration data, removing all properties:
```
DELETE /api/v1/services/AMBARI/components/AMBARI_SERVER/configurations/sso-configuration
```

<a name="implementation-details"></a>
## Implementation Details

<a name="implementation-details-data"></a>
### Configuration Data

The SSO configuration data is stored in the `ambari_configuration` table of the Ambari database. 
This table contains a set of property names and their values, grouped by a _category_ name.  For the
SSO configuration data, the properties are stored using the category name "sso-configuration". 

The following properties are available for the sso-configuration category:
- `ambari.sso.manage_services`
  - A Boolean value indicating whether Ambari is to manage the SSO configuration for services or not.
  - Default value: "false"
- `ambari.sso.enabled_services`
  - A comma-delimited list of services that are expected to be configured for SSO.  A "*" indicates all services.
- `ambari.sso.provider.url`
  - The URL of the SSO provider.
- `ambari.sso.provider.certificate`
  - The PEM-encoded x509 certificate containing the public key to use when verifying the authenticity of a JWT token from the SSO provider.
  - The PEM file header and footer lines are optional, but recommended.
- `ambari.sso.provider.originalUrlParamName`
  - The original URL to use when constructing the URL for SSO provider.
  - Default value: "originalUrl"
- `ambari.sso.jwt.audiences`
  - A list of the JWT audiences expected. Leaving this blank will allow for any audience.
  - Default value: ""
- `ambari.sso.jwt.cookieName`
  - The name of the cookie which will be used to extract the JWT token from the request.
  - Default value: "hadoop-jwt"
- `ambari.sso.authentication.enabled`
  -   Determines whether to use JWT authentication when logging into Ambari.
  - Default value: "false"
 
 
<a name="implementation-details-stack-advisor"></a>
### Stack Advisor

After updating the SSO configuration data via the CLI (which invokes the REST API) or the REST API,
Ambari triggers a process to reconfigure services as needed.  This happens in the background where
as the only indication that something has changed is that services may request to be restarted.

The process that is triggered in the background invokes the Stack Advisor, requesting SSO-specific
recommendations.  It is expected that a service supporting this feature overrides the 
`getServiceConfigurationRecommendationsForSSO` function and adheres to the convention that only 
the SSO-relevant configurations are altered.  
 
 ```
def getServiceConfigurationRecommendationsForSSO(self, configurations, clusterData, services, hosts):
  ...
 
 ```    
 
To help determine how this function should behave, the following classes are available:
 
 - `AmbariConfiguration`
 - `AmbariSSODetails`
 
<a name="implementation-details-stack-advisor-ambariconfiguration"></a>
#### AmbariConfiguration class

`AmbariConfiguration` is an entry point into configuration-specific information about Ambari.  It 
uses data from the `ambari-server-configuration` section of the `services.json` file to build 
_category_-specific utility classes. An instance of this object is created using the base StackAdvisor
class's  `get_ambari_configuration` function. For example:

```
ambari_configuration = self.get_ambari_configuration(services)
```

<a name="implementation-details-stack-advisor-ambarissodetails"></a>
#### AmbariSSODetails class

The `AmbariSSODetails` class contains utility functions to get and interpret data from the 
`sso-configiration` data parsed from the `ambari-server-configuration` section of the `services.json` 
file.  To create an instance of this class, the `AmbariSSODetails.get_ambari_sso_detail` function is
used. For example:

```
ambari_sso_details = ambari_configuration.get_ambari_sso_details() if ambari_configuration else None
```

The following functions are available:
 
- `is_managing_services()`
  - Tests the configuration data to determine if Ambari should be configuring servcies to enable SSO 
  integration.
  - The relevant property is "sso-configuration/ambari.sso.manage_services", which is expected to be 
  a "true" or "false".

- `get_services_to_enable()`
    - Safely gets the list of services that Ambari should enabled for SSO.
    - The returned value is a list of the relevant service names converted to lowercase.

- `should_enable_sso(service_name)`
  - Tests the configuration data to determine if the specified service should be configured by 
  Ambari to enable SSO integration.
  - The relevant property is "sso-configuration/ambari.sso.enabled_services", which is expected to 
  be a comma-delimited list of services to be enabled.

- `should_disable_sso(service_name)`
  - Tests the configuration data to determine if the specified service should be configured by
    Ambari to disable SSO integration.
  - The relevant property is "sso-configuration/ambari.sso.enabled_services", which is expected
    to be a comma-delimited list of services to be enabled.
    
- `get_jwt_audiences()`
    - Gets the configured JWT audiences list
    - The relevant property is "sso-configuration/ambari.sso.jwt.audiences", which is expected
    to be a comma-delimited list of audience names.

- `get_jwt_cookie_name()`:
  - Gets the configured JWT cookie name
  - The relevant property is "sso-configuration/ambari.sso.jwt.cookieName", which is expected
    to be a string.

- `get_sso_provider_url()`
  - Gets the configured SSO provider URL
  - The relevant property is "sso-configuration/ambari.sso.provider.url", which is expected
    to be a string.

- `get_sso_provider_original_parameter_name()`
  - Gets the configured SSO provider's original URL parameter name
  - The relevant property is "sso-configuration/ambari.sso.provider.originalUrlParamName", which is
    expected to be a string.

- `get_sso_provider_certificate(include_header_and_footer=False, remove_line_breaks=True)`
  - Retrieves, formats, and returns the PEM data from the stored 509 certificate.
  - The relevant property is "sso-configuration/ambari.sso.provider.certificate", which is expected
    to be a PEM-encoded x509 certificate, including the header and footer.
  - If the header and footer need to exist, and do not, the will be added. If they need to be removed,
    they will be removed if they exist.  Any line break characters will be left alone unless the
    caller specifies them to be removed. Line break characters will not be added if missing.
 
 <a name="implementation-details-stack-advisor-example"></a>
 #### Example
 
```
  def recommendConfigurationsForSSO(self, configurations, clusterData, services, hosts):
1   ambari_configuration = self.get_ambari_configuration(services)
2   ambari_sso_details = ambari_configuration.get_ambari_sso_details() if ambari_configuration else None

3   if ambari_sso_details and ambari_sso_details.is_managing_services():
      putProperty = self.putProperty(configurations, "service-site", services)

      # If SSO should be enabled for this service
4     if ambari_sso_details.should_enable_sso('MY_SERVICE'):
        putProperty('sso.knox.enabled', "true")
        putProperty('sso.knox.providerurl', ambari_sso_details.get_sso_provider_url())
        putProperty('sso.knox.publicKey', ambari_sso_details.get_sso_provider_certificate(False, True))
 
       # If SSO should be disabled for this service
5      elif ambari_sso_details.should_disable_sso('MY_SERVICE'):
         putProperty('atlas.sso.knox.enabled', "false")
```

Explanations:
1. Obtain an instance of the `AmbariConfiguration` object using data from the `services` dictionary.
2. Retrieve the `AmbariSSODetails` object by calling the `AmbariConfiguration`'s `get_ambari_sso_details` 
function. 
3. Test to see if Ambari should be managing the SSO configuration for installed services.  If so, 
continue; else, there is nothing left to do but exit the function. 
4. Test to see if Ambari should **enable** SSO for the service with the name of "MY_SERVICE". If so,
set the relevant properties using values from the `AmbariSSODetails` object.
5. Test to see if Ambari should **disable** SSO for the service with the name of "MY_SERVICE". If so,
set or unset the relevant properties.

**Note:**  Both `should_enable_sso` and `should_disable_sso` should be called to determine what action
to take. This is because even though Ambari is managing the SSO configuration for installed services,
it may not be managing the SSO-configuration for the particular service. So one of the following 
actions are to be performed: enable, disable, or ignore.