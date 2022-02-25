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

Ambari Kerberos Automation
=========

- [Introduction](index.md)
- [The Kerberos Descriptor](kerberos_descriptor.md)
- [The Kerberos Service](#the-kerberos-service)
  - [Configurations](#configurations)
    - [kerberos-env](#kerberos-env)
    - [krb5-conf](#krb5-conf)
- [Enabling Kerberos](enabling_kerberos.md)

<a name="the-kerberos-service"></a>
## The Kerberos Service

<a name="configurations"></a>
### Configurations

<a name="kerberos-env"></a>
#### kerberos-env

##### kdc_type

The type of KDC being used. 

_Possible Values:_ 
- `none`
  - Ambari is not to integrate with a KDC.  In this case, it is expected that the Kerberos identities 
will be created and the keytab files are distributed manually
- `mit-kdc`
  - Ambari is to integrate with an MIT KDC
- `active-directory`
  - Ambari is to integrate with an Active Directory
- `ipa` 
  - Ambari is to integrate with a FreeIPA server

##### manage_identities

Indicates whether the Ambari-specified user and service Kerberos identities (principals and keytab files)
should be managed (created, deleted, updated, etc...) by Ambari (`true`) or managed manually by the
user (`false`).

_Possible Values:_ `true`, `false`

##### create_ambari_principal

Indicates whether the Ambari Kerberos identity (principal and keytab file used by Ambari, itself, and
its views) should be managed (created, deleted, updated, etc...) by Ambari (`true`) or managed manually
by the user (`false`).

_Possible Values:_ `true`, `false`

This property is dependent on the value of `manage_identities`, where as if `manage_identities` is
false, `create_ambari_principal` will assumed to be `false` as well.

##### manage_auth_to_local

Indicates whether the Hadoop auth-to-local rules should be managed by Ambari (`true`) or managed
manually by the user (`false`).

_Possible Values:_ `true`, `false`

##### install_packages

Indicates whether Ambari should install the Kerberos client packages (`true`) or not (`false`).
If not, it is expected that Kerberos utility programs installed by the user (such as kadmin, kinit,
klist, and kdestroy) are compatible with MIT Kerberos 5 version 1.10.3 in command line options and
behaviors.

_Possible Values:_ `true`, `false`

##### ldap_url

The URL to the Active Directory LDAP Interface. This value **must** indicate a secure channel using
LDAPS since it is required for creating and updating passwords for Active Directory accounts.
 
_Example:_  `ldaps://ad.example.com:636`

If the `kdc_type` is `active-directory`, this property is mandatory.

##### container_dn

The distinguished name (DN) of the container used store the Ambari-managed user and service principals
within the configured Active Directory

_Example:_  `OU=hadoop,DC=example,DC=com`

If the `kdc_type` is `active-directory`, this property is mandatory.

##### encryption_types

The supported (space-delimited) list of session key encryption types that should be returned by the KDC.

_Default value:_ aes des3-cbc-sha1 rc4 des-cbc-md5

##### realm

The default realm to use when creating service principals

_Example:_ `EXAMPLE.COM`

This value is expected to be in all uppercase characters.

##### kdc_hosts

A comma-delimited list of IP addresses or FQDNs for the list of relevant KDC hosts. Optionally a
port number may be included for each entry.

_Example:_ `kdc.example.com, kdc1.example.com`

_Example:_ `kdc.example.com:88, kdc1.example.com:88`

##### admin_server_host

The IP address or FQDN for the Kerberos administrative host. Optionally a port number may be included.

_Example:_ `kadmin.example.com`

_Example:_ `kadmin.example.com:88`

If the `kdc_type` is `mit-kdc` or `ipa`, the value must be the FQDN of the Kerberos administrative host. 

##### master_kdc

The IP address or FQDN of the master KDC host in a master-slave KDC deployment. Optionally a port 
number may be included.

_Example:_ `kadmin.example.com`

_Example:_ `kadmin.example.com:88`

##### executable_search_paths

A comma-delimited list of search paths to use to find Kerberos utilities like kadmin and kinit.

_Default value:_ `/usr/bin, /usr/kerberos/bin, /usr/sbin, /usr/lib/mit/bin, /usr/lib/mit/sbin`

##### password_length

The length required length for generated passwords.

_Default value:_ `20`

##### password_min_lowercase_letters

The minimum number of lowercase letters (a-z) required in generated passwords

_Default value:_ `1`

##### password_min_uppercase_letters

The minimum number of uppercase letters (A-Z) required in generated passwords

_Default value:_ `1`

##### password_min_digits

The minimum number of digits (0-9) required in generated passwords

_Default value:_ `1`

##### password_min_punctuation

The minimum number of punctuation characters (?.!$%^*()-_+=~) required in generated passwords

_Default value:_ `1`

##### password_min_whitespace

The minimum number of whitespace characters required in generated passwords

_Default value:_ `0`

##### service_check_principal_name

The principal name to use when executing the Kerberos service check

_Example:_ `${cluster_name}-${short_date}`

##### case_insensitive_username_rules

Force principal names to resolve to lowercase local usernames in auth-to-local rules

_Possible values:_ `true`, `false`

_Default value:_ `false`

##### ad_create_attributes_template

A Velocity template to use to generate a JSON-formatted document containing the set of attribute
names and values needed to create a new Kerberos identity in the relevant Active Directory.

Variables include: 

- `principal_name` - the components (primary and instance) portion of the principal
- `principal_primary` - the _primary component_ of the principal name
- `principal_instance` - the _instance component_ of the principal name
- `realm` - the `realm` portion of the principal
- `realm_lowercase` - the lowercase form of the `realm` of the principal
- `normalized_principal` - the full principal value, including the component and realms parts
- `principal_digest` - a binhexed-encoded SHA1 digest of the normalized principal
- `principal_digest_256` - a binhexed-encoded SHA256 digest of the normalized principal
- `principal_digest_512` - a binhexed-encoded SHA512 digest of the normalized principal
- `password` - the generated password
- `is_service` - `true` if the principal is a _service_ principal, `false` if the principal is a _user_ principal
- `container_dn` - the `kerberos-env/container_dn` property value 

_Note_: A principal is made up of the following parts:  primary component, instances component
(optional), and realm:

* User principal: **_`primary_component`_**@**_`realm`_**
* Service principal: **_`primary_component`_**/**_`instance_component`_**@**_`realm`_**

_Default value:_

```
{
"objectClass": ["top", "person", "organizationalPerson", "user"],
"cn": "$principal_name",
#if( $is_service )
"servicePrincipalName": "$principal_name",
#end
"userPrincipalName": "$normalized_principal",
"unicodePwd": "$password",
"accountExpires": "0",
"userAccountControl": "66048"
}
```

This property is mandatory and only used if the `kdc_type` is `active-directory`

##### kdc_create_attributes

The set of attributes to use when creating a new Kerberos identity in the relevant (MIT) KDC.

_Example:_ `-requires_preauth max_renew_life=7d`

This property is optional and only used if the `kdc_type` is `mit-kdc`

##### ipa_user_group

The group in IPA that user principals should be a member of.

This property is optional and only used if the `kdc_type` is `ipa`

<a name="krb5-conf"></a>
#### krb5-conf

##### manage_krb5_conf

Indicates whether the krb5.conf file should be managed (created, updated, etc...) by Ambari (`true`)
or managed manually by the user (`false`).

_Possible values:_ `true`, `false`

_Default value:_ `false`

##### domains

A comma-separated list of domain names used to map server host names to the realm name.

_Example:_ host.example.com, example.com, .example.com

This property is optional

##### conf_dir

The krb5.conf configuration directory
Default value: /etc

##### content

Customizable krb5.conf template (Jinja template engine)

_Default value:_

```
[libdefaults]
  renew_lifetime = 7d
  forwardable = true
  default_realm = {{realm}}
  ticket_lifetime = 24h
  dns_lookup_realm = false
  dns_lookup_kdc = false
  default_ccache_name = /tmp/krb5cc_%{uid}
  #default_tgs_enctypes = {{encryption_types}}
  #default_tkt_enctypes = {{encryption_types}}
{% if domains %}
[domain_realm]
{%- for domain in domains.split(',') %}
  {{domain|trim()}} = {{realm}}
{%- endfor %}
{% endif %}
[logging]
  default = FILE:/var/log/krb5kdc.log
  admin_server = FILE:/var/log/kadmind.log
  kdc = FILE:/var/log/krb5kdc.log

[realms]
  {{realm}} = {
{%- if master_kdc %}
    master_kdc = {{master_kdc|trim()}}
{%- endif -%}
{%- if kdc_hosts > 0 -%}
{%- set kdc_host_list = kdc_hosts.split(',')  -%}
{%- if kdc_host_list and kdc_host_list|length > 0 %}
    admin_server = {{admin_server_host|default(kdc_host_list[0]|trim(), True)}}
{%- if kdc_host_list -%}
{%- if master_kdc and (master_kdc not in kdc_host_list) %}
    kdc = {{master_kdc|trim()}}
{%- endif -%}
{% for kdc_host in kdc_host_list %}
    kdc = {{kdc_host|trim()}}
{%- endfor -%}
{% endif %}
{%- endif %}
{%- endif %}
  }

{# Append additional realm declarations below #}
```
