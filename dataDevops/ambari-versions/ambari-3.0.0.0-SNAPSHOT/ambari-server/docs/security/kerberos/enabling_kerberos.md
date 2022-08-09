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
- [The Kerberos Service](kerberos_service.md)
- [Enabling Kerberos](#enabling-kerberos)
  - [The Enable Kerberos Wizard](#the-enable-kerberos-wizard)
  - [The REST API](#the-rest-api)
  - [Miscellaneous Technical Information](#miscellaneous-technical-information)
    - [Password Generation](#password-generation)

<a name="enabling-kerberos"></a>
## Enabling Kerberos

Enabling Kerberos on the cluster may be done using the _Enable Kerberos Wizard_ within the Ambari UI
or using the REST API.

<a name="the-enable-kerberos-wizard"></a>
### The Enable Kerberos Wizard

The _Enable Kerberos Wizard_, in the Ambari UI, provides an easy to use wizard interface that walks
through the process of enabling Kerberos.

<a name="the-rest-api"></a>
### The REST API

It is possible to enable Kerberos using Ambari's REST API using the following API calls:

**_Notes:_**

- Change the authentication credentials as needed
  - `curl ... -u username:password ...`
  - The examples below use
    - username: admin
    - password: admin
- Change the Ambari server host name and port as needed
  - `curl ... http://HOST:PORT/api/v1/...`
  - The examples below use
    - HOST:  AMBARI_SERVER
    - PORT: 8080
- Change the cluster name as needed
  - `curl ... http://.../CLUSTER/...`
  - The examples below use
    - CLUSTER:  CLUSTER_NAME
- @./payload indicates the the payload data is stored in some file rather than declared inline
  - `curl ... -d @./payload ...`
  - The examples below use `./payload` which should be replace with the actual file path
  - The contents of the payload file are indicated below the curl statement

#### Add the KERBEROS Service to cluster

```
curl -H "X-Requested-By:ambari" -u admin:admin -i -X POST http://AMBARI_SERVER:8080/api/v1/clusters/CLUSTER_NAME/services/KERBEROS
```

#### Add the KERBEROS_CLIENT component to the KERBEROS service

```
curl -H "X-Requested-By:ambari" -u admin:admin -i -X POST http://AMBARI_SERVER:8080/api/v1/clusters/CLUSTER_NAME/services/KERBEROS/components/KERBEROS_CLIENT
```

####  Create and set KERBEROS service configurations

```
curl -H "X-Requested-By:ambari" -u admin:admin -i -X PUT -d @./payload http://AMBARI_SERVER:8080/api/v1/clusters/CLUSTER_NAME
```

Example payload when using an MIT KDC:

```
[
  {
    "Clusters": {
      "desired_config": {
        "type": "krb5-conf",
        "tag": "version1",
        "properties": {
          "domains":"",
          "manage_krb5_conf": "true",
          "conf_dir":"/etc",
          "content" : "[libdefaults]\n  renew_lifetime = 7d\n  forwardable = true\n  default_realm = {{realm}}\n  ticket_lifetime = 24h\n  dns_lookup_realm = false\n  dns_lookup_kdc = false\n  default_ccache_name = /tmp/krb5cc_%{uid}\n  #default_tgs_enctypes = {{encryption_types}}\n  #default_tkt_enctypes = {{encryption_types}}\n{% if domains %}\n[domain_realm]\n{%- for domain in domains.split(',') %}\n  {{domain|trim()}} = {{realm}}\n{%- endfor %}\n{% endif %}\n[logging]\n  default = FILE:/var/log/krb5kdc.log\n  admin_server = FILE:/var/log/kadmind.log\n  kdc = FILE:/var/log/krb5kdc.log\n\n[realms]\n  {{realm}} = {\n{%- if master_kdc %}\n    master_kdc = {{master_kdc|trim()}}\n{%- endif -%}\n{%- if kdc_hosts > 0 -%}\n{%- set kdc_host_list = kdc_hosts.split(',')  -%}\n{%- if kdc_host_list and kdc_host_list|length > 0 %}\n    admin_server = {{admin_server_host|default(kdc_host_list[0]|trim(), True)}}\n{%- if kdc_host_list -%}\n{%- if master_kdc and (master_kdc not in kdc_host_list) %}\n    kdc = {{master_kdc|trim()}}\n{%- endif -%}\n{% for kdc_host in kdc_host_list %}\n    kdc = {{kdc_host|trim()}}\n{%- endfor -%}\n{% endif %}\n{%- endif %}\n{%- endif %}\n  }\n\n{# Append additional realm declarations below #}"
        }
      }
    }
  },
  {
    "Clusters": {
      "desired_config": {
        "type": "kerberos-env",
        "tag": "version1",
        "properties": {
          "kdc_type": "mit-kdc",
          "manage_identities": "true",
          "create_ambari_principal": "true",
          "manage_auth_to_local": "true",
          "install_packages": "true",
          "encryption_types": "aes des3-cbc-sha1 rc4 des-cbc-md5",
          "realm" : "EXAMPLE.COM",
          "kdc_hosts" : "FQDN.KDC.SERVER",
          "master_kdc" : "FQDN.MASTER.KDC.SERVER",
          "admin_server_host" : "FQDN.ADMIN.KDC.SERVER",
          "executable_search_paths" : "/usr/bin, /usr/kerberos/bin, /usr/sbin, /usr/lib/mit/bin, /usr/lib/mit/sbin",
          "service_check_principal_name" : "${cluster_name}-${short_date}",
          "case_insensitive_username_rules" : "false"
        }
      }
    }
  }
]
```

Example payload when using an Active Directory:

```
[
  {
    "Clusters": {
      "desired_config": {
        "type": "krb5-conf",
        "tag": "version1",
        "properties": {
          "domains":"",
          "manage_krb5_conf": "true",
          "conf_dir":"/etc",
          "content" : "[libdefaults]\n  renew_lifetime = 7d\n  forwardable = true\n  default_realm = {{realm}}\n  ticket_lifetime = 24h\n  dns_lookup_realm = false\n  dns_lookup_kdc = false\n  default_ccache_name = /tmp/krb5cc_%{uid}\n  #default_tgs_enctypes = {{encryption_types}}\n  #default_tkt_enctypes = {{encryption_types}}\n{% if domains %}\n[domain_realm]\n{%- for domain in domains.split(',') %}\n  {{domain|trim()}} = {{realm}}\n{%- endfor %}\n{% endif %}\n[logging]\n  default = FILE:/var/log/krb5kdc.log\n  admin_server = FILE:/var/log/kadmind.log\n  kdc = FILE:/var/log/krb5kdc.log\n\n[realms]\n  {{realm}} = {\n{%- if master_kdc %}\n    master_kdc = {{master_kdc|trim()}}\n{%- endif -%}\n{%- if kdc_hosts > 0 -%}\n{%- set kdc_host_list = kdc_hosts.split(',')  -%}\n{%- if kdc_host_list and kdc_host_list|length > 0 %}\n    admin_server = {{admin_server_host|default(kdc_host_list[0]|trim(), True)}}\n{%- if kdc_host_list -%}\n{%- if master_kdc and (master_kdc not in kdc_host_list) %}\n    kdc = {{master_kdc|trim()}}\n{%- endif -%}\n{% for kdc_host in kdc_host_list %}\n    kdc = {{kdc_host|trim()}}\n{%- endfor -%}\n{% endif %}\n{%- endif %}\n{%- endif %}\n  }\n\n{# Append additional realm declarations below #}"
        }
      }
    }
  },
  {
    "Clusters": {
      "desired_config": {
        "type": "kerberos-env",
        "tag": "version1",
        "properties": {
          "kdc_type": "active-directory",
          "manage_identities": "true",
          "create_ambari_principal": "true",
          "manage_auth_to_local": "true",
          "install_packages": "true",
          "encryption_types": "aes des3-cbc-sha1 rc4 des-cbc-md5",
          "realm" : "EXAMPLE.COM",
          "kdc_hosts" : "FQDN.AD.SERVER",
          "master_kdc" : "FQDN.MASTER.AD.SERVER",
          "admin_server_host" : "FQDN.AD.SERVER",
          "ldap_url" : "LDAPS://AD_HOST:PORT",
          "container_dn" : "OU=....,....",
          "executable_search_paths" : "/usr/bin, /usr/kerberos/bin, /usr/sbin, /usr/lib/mit/bin, /usr/lib/mit/sbin",
          "password_length": "20",
          "password_min_lowercase_letters": "1",
          "password_min_uppercase_letters": "1",
          "password_min_digits": "1",
          "password_min_punctuation": "1",
          "password_min_whitespace": "0",
          "service_check_principal_name" : "${cluster_name}-${short_date}",
          "case_insensitive_username_rules" : "false",
          "create_attributes_template" :  "{\n \"objectClass\": [\"top\", \"person\", \"organizationalPerson\", \"user\"],\n \"cn\": \"$principal_name\",\n #if( $is_service )\n \"servicePrincipalName\": \"$principal_name\",\n #end\n \"userPrincipalName\": \"$normalized_principal\",\n \"unicodePwd\": \"$password\",\n \"accountExpires\": \"0\",\n \"userAccountControl\": \"66048\"}"
        }
      }
    }
  }
]
```
Example payload when using IPA:

```
[
  {
    "Clusters": {
      "desired_config": {
        "type": "krb5-conf",
        "tag": "version1",
        "properties": {
          "domains":"",
          "manage_krb5_conf": "true",
          "conf_dir":"/etc",
          "content" : "[libdefaults]\n  renew_lifetime = 7d\n  forwardable = true\n  default_realm = {{realm}}\n  ticket_lifetime = 24h\n  dns_lookup_realm = false\n  dns_lookup_kdc = false\n  default_ccache_name = /tmp/krb5cc_%{uid}\n  #default_tgs_enctypes = {{encryption_types}}\n  #default_tkt_enctypes = {{encryption_types}}\n{% if domains %}\n[domain_realm]\n{%- for domain in domains.split(',') %}\n  {{domain|trim()}} = {{realm}}\n{%- endfor %}\n{% endif %}\n[logging]\n  default = FILE:/var/log/krb5kdc.log\n  admin_server = FILE:/var/log/kadmind.log\n  kdc = FILE:/var/log/krb5kdc.log\n\n[realms]\n  {{realm}} = {\n{%- if master_kdc %}\n    master_kdc = {{master_kdc|trim()}}\n{%- endif -%}\n{%- if kdc_hosts > 0 -%}\n{%- set kdc_host_list = kdc_hosts.split(',')  -%}\n{%- if kdc_host_list and kdc_host_list|length > 0 %}\n    admin_server = {{admin_server_host|default(kdc_host_list[0]|trim(), True)}}\n{%- if kdc_host_list -%}\n{%- if master_kdc and (master_kdc not in kdc_host_list) %}\n    kdc = {{master_kdc|trim()}}\n{%- endif -%}\n{% for kdc_host in kdc_host_list %}\n    kdc = {{kdc_host|trim()}}\n{%- endfor -%}\n{% endif %}\n{%- endif %}\n{%- endif %}\n  }\n\n{# Append additional realm declarations below #}"
        }
      }
    }
  },
  {
    "Clusters": {
      "desired_config": {
        "type": "kerberos-env",
        "tag": "version1",
        "properties": {
          "kdc_type": "ipa",
          "manage_identities": "true",
          "create_ambari_principal": "true",
          "manage_auth_to_local": "true",
          "install_packages": "true",
          "encryption_types": "aes des3-cbc-sha1 rc4 des-cbc-md5",
          "realm" : "EXAMPLE.COM",
          "kdc_hosts" : "FQDN.KDC.SERVER",
          "master_kdc" : "FQDN.MASTER.KDC.SERVER",
          "admin_server_host" : "FQDN.ADMIN.KDC.SERVER",
          "executable_search_paths" : "/usr/bin, /usr/kerberos/bin, /usr/sbin, /usr/lib/mit/bin, /usr/lib/mit/sbin",
          "service_check_principal_name" : "${cluster_name}-${short_date}",
          "case_insensitive_username_rules" : "false"
        }
      }
    }
  }
]
```

#### Create the KERBEROS_CLIENT host components
_Once for each host, replace HOST_NAME_

```
curl -H "X-Requested-By:ambari" -u admin:admin -i -X POST -d '{"host_components" : [{"HostRoles" : {"component_name":"KERBEROS_CLIENT"}}]}' http://AMBARI_SERVER:8080/api/v1/clusters/CLUSTER_NAME/hosts?Hosts/host_name=HOST_NAME
```

#### Install the KERBEROS service and components

```
curl -H "X-Requested-By:ambari" -u admin:admin -i -X PUT -d '{"ServiceInfo": {"state" : "INSTALLED"}}' http://AMBARI_SERVER:8080/api/v1/clusters/CLUSTER_NAME/services/KERBEROS
```

#### Stop all services

```
curl -H "X-Requested-By:ambari" -u admin:admin -i -X PUT -d  '{"RequestInfo":{"context":"Stop Service"},"Body":{"ServiceInfo":{"state":"INSTALLED"}}}' http://AMBARI_SERVER:8080/api/v1/clusters/CLUSTER_NAME/services
```

#### Get the default Kerberos Descriptor

```
curl -H "X-Requested-By:ambari" -u admin:admin -i -X GET http://AMBARI_SERVER:8080/api/v1/stacks/STACK_NAME/versions/STACK_VERSION/artifacts/kerberos_descriptor
```

#### Get the customized Kerberos Descriptor (if previously set)

```
curl -H "X-Requested-By:ambari" -u admin:admin -i -X GET http://AMBARI_SERVER:8080/api/v1/clusters/CLUSTER_NAME/artifacts/kerberos_descriptor
```

#### Set the Kerberos Descriptor

```
curl -H "X-Requested-By:ambari" -u admin:admin -i -X POST -d @./payload http://AMBARI_SERVER:8080/api/v1/clusters/CLUSTER_NAME/artifacts/kerberos_descriptor
```

Payload:

```
{
  "artifact_data" : {
    ... 
  } 
}
```

**_Note:_** The Kerberos Descriptor payload may be a complete Kerberos Descriptor or just the updates to overlay on top of the default Kerberos Descriptor.

#### Set the KDC administrator credentials

```
curl -H "X-Requested-By:ambari" -u admin:admin -i -X POST -d @./payload http://AMBARI_SERVER:8080/api/v1/clusters/CLUSTER_NAME/credentials/kdc.admin.credential
```

Payload:

```
{
  "Credential" : {
    "principal" : "admin/admin@EXAMPLE.COM",
    "key" : "h4d00p&!",
    "type" : "temporary"
  }
}
```

**_Note:_** the _principal_ and _key_ (password) values should be updated to match the correct credentials
for the KDC administrator account

**_Note:_** the `type` value may be `temporary` or `persisted`; however the value may only be `persisted`
if Ambari's credential store has been previously setup.

#### Enable Kerberos

```
curl -H "X-Requested-By:ambari" -u admin:admin -i -X PUT -d @./payload http://AMBARI_SERVER:8080/api/v1/clusters/CLUSTER_NAME
```

Payload

```
{
  "Clusters": {
    "security_type" : "KERBEROS"
  }
}
```

#### Start all services

```
curl -H "X-Requested-By:ambari" -u admin:admin -i -X PUT -d '{"ServiceInfo": {"state" : "STARTED"}}' http://AMBARI_SERVER:8080/api/v1/clusters/CLUSTER_NAME/services
```

<a name="miscellaneous-technical-information"></a>
### Miscellaneous Technical Information

<a name="password-generation"></a>
#### Password Generation

When enabling Kerberos using an Active Directory, Ambari must use an internal mechanism to build 
the keytab files. This is because keytab files cannot be requested remotely from an Active Directory. 
In order to create keytab files, Ambari needs to know the password for each relevant Kerberos 
identity.  Therefore, Ambari sets or updates the identity's password as needed.

The password for each Ambari-managed account in an Active Directory is randomly generated and
stored only long enough in memory to set the account's password and generate the keytab file.  
Passwords are generated using the following user-settable parameters:

- Password length (`kerberos-env/password_length`)
  - Default Value: 20
- Minimum number of lower-cased letters (`kerberos-env/password_min_lowercase_letters`)
  - Default Value:  1
  - Character Set: `abcdefghijklmnopqrstuvwxyz`
- Minimum number of upper-cased letters (`kerberos-env/password_min_uppercase_letters`)
  - Default Value: 1
  - Character Set: `ABCDEFGHIJKLMNOPQRSTUVWXYZ`
- Minimum number of digits (`kerberos-env/password_min_digits`)
  - Default Value: 1
  - Character Set: `1234567890`
- Minimum number of punctuation characters (`kerberos-env/password_min_punctuation`)
  - Default Value: 1
  - Character Set: `?.!$%^*()-_+=~`
- Minimum number of whitespace characters (`kerberos-env/password_min_whitespace`)
  - Default Value: 0
  - Character Set: `(space character)`

The following algorithm is executed:

1. Create an array to store password characters
2. For each character class (upper-case letter, lower-case letter, digit, ...), randomly select the
minimum number of characters from the relevant character set and store them in the array
3. For the number of characters calculated as the difference between the expected password length and
the number of characters already collected, randomly select a character from a randomly-selected character
class and store it into the array
4. For the number of characters expected in the password, randomly pull one from the array and append
to the password result
5. Return the generated password

To generate a random integer used to identify an index within a character set, a static instance of
the `java.security.SecureRandom` class ([http://docs.oracle.com/javase/7/docs/api/java/security/SecureRandom.html](http://docs.oracle.com/javase/7/docs/api/java/security/SecureRandom.html))
is used.
