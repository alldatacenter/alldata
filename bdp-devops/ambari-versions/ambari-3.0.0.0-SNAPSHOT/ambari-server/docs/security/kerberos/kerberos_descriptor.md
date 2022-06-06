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
- [The Kerberos Descriptor](#the-kerberos-descriptor)
  - [Components of a Kerberos Descriptor](#components-of-a-kerberos-descriptor)
    - [Stack-level Properties](#stack-level-properties)
    - [Stack-level Identities](#stack-level-identities)
    - [Stack-level Auth-to-local-properties](#stack-level-auth-to-local-properties)
    - [Stack-level Configurations](#stack-level-configuratons)
    - [Services](#services)
    - [Service-level Identities](#service-level-identities)
    - [Service-level Auth-to-local-properties](#service-level-auth-to-local-properties)
    - [Service-level Configurations](#service-level-configurations)
    - [Components](#service-components)
    - [Component-level Identities](#component-level-identities)
    - [Component-level Auth-to-local-properties](#component-level-auth-to-local-properties)
    - [Component-level Configurations](#component-level-configurations)
  - [Kerberos Descriptor specifications](#kerberos-descriptor-specifications)
    - [properties](#properties)
    - [auth-to-local-properties](#auth-to-local-properties)
    - [configurations](#configurations)
    - [identities](#identities)
    - [principal](#principal)
    - [keytab](#keytab)
    - [services](#services)
    - [components](#components)
  - [Examples](#examples)
- [The Kerberos Service](kerberos_service.md)
- [Enabling Kerberos](enabling_kerberos.md)

<a name="the-kerberos-descriptor"></a>
## The Kerberos Descriptor

The Kerberos Descriptor is a JSON-formatted text file containing information needed by Ambari to enable
or disable Kerberos for a stack and its services. This file must be named **_kerberos.json_** and should
be in the root directory of the relevant stack or service definition. Kerberos Descriptors are meant to
be hierarchical such that details in the stack-level descriptor can be overwritten (or updated) by details
in the service-level descriptors.

For the services in a stack to be Kerberized, there must be a stack-level Kerberos Descriptor. This
ensures that even if a common service has a Kerberos Descriptor, it may not be Kerberized unless the
relevant stack indicates that supports Kerberos by having a stack-level Kerberos Descriptor.

For a component of a service to be Kerberized, there must be an entry for it in its containing service's
service-level descriptor. This allows for some of a services' components to be managed and other
components of that service to be ignored by the automated Kerberos facility.

Kerberos Descriptors are inherited from the base stack or service, but may be overridden as a full
descriptor - partial descriptors are not allowed.

A complete descriptor (which is built using the stack-level descriptor, the service-level descriptors,
and any updates from user input) has the following structure:

- Stack-level Properties
- Stack-level Identities
- Stack-level Configurations
- Stack-level Auth-to-local-properties
- Services
  - Service-level Identities
  - Service-level Auth-to-local-properties
  - Service-level Configurations
  - Components
    - Component-level Identities
    - Component-level Auth-to-local-properties
    - Component-level Configurations

Each level of the descriptor inherits the data from its parent. This data, however, may be overridden
if necessary. For example, a component will inherit the configurations and identities of its container
service; which in turn inherits the configurations and identities from the stack.

<a name="components-of-a-kerberos-descriptor"></a>
### Components of a Kerberos Descriptor

<a name="stack-level-properties"></a>
#### Stack-level Properties

Stack-level properties is an optional set of name/value pairs that can be used in variable replacements.
For example, if a property named "**_property1_**" exists with the value of "**_value1_**", then any instance of
"**_${property1}_**" within a configuration property name or configuration property value will be replaced
with "**_value1_**".

This property is only relevant in the stack-level Kerberos Descriptor and may not be overridden by
lower-level descriptors.

See [properties](#properties).

<a name="stack-level-identities"></a>
#### Stack-level Identities

Stack-level identities is an optional identities block containing a list of zero or more identity
descriptors that are common among all services in the stack. An example of such an identity is the
Ambari smoke test user, which is used by all services to perform service check operations. Service-
and component-level identities may reference (and specialize) stack-level identities using the
identity’s name with a forward slash (/) preceding it. For example if there was a stack-level identity
with the name "smokeuser", then a service or a component may create an identity block that references
and specializes it by declaring a "**_reference_**" property and setting it to "/smokeuser".  Within
this identity block details of the identity may be and overwritten as necessary. This does not alter
the stack-level identity, it essentially creates a copy of it and updates the copy's properties.

See [identities](#identities).

<a name="stack-level-auth-to-local-properties"></a>
#### Stack-level Auth-to-local-properties

Stack-level auth-to-local-properties is an optional list of zero or more configuration property
specifications `(config-type/property_name[|concatenation_scheme])` indicating which properties should
be updated with dynamically generated auto-to-local rule sets.

See [auth-to-local-properties](#auth-to-local-properties).

<a name="stack-level-configurations"></a>
#### Stack-level Configurations

Stack-level configurations is an optional configurations block containing a list of zero or more
configuration descriptors that are common among all services in the stack. Configuration descriptors
are overridable due to the structure of the data.  However, overriding configuration properties may
create undesired behavior since it is not known until after the Kerberization process is complete
what value a property will have.

See [configurations](#configurations).

<a name="services"></a>
#### Services

Services is a list of zero or more service descriptors. A stack-level Kerberos Descriptor should not
list any services; however a service-level Kerberos Descriptor should contain at least one.

See [services](#services).

<a name="service-level-identities"></a>
#### Service-level Identities

Service-level identities is an optional identities block containing a list of zero or more identity
descriptors that are common among all components of the service. Component-level identities may
reference (and specialize) service-level identities by specifying a relative or an absolute path
to it.

For example if there was a service-level identity with the name "service_identity", then a child
component may create an identity block that references and specializes it by setting its "reference"
attribute to "../service_identity" or "/service_name/service_identity" and overriding any values as
necessary. This does not override the service-level identity, it essentially creates a copy of it and
updates the copy's properties.

##### Examples

```
{
  "name" : "relative_path_example",
  "reference" : "../service_identity",
  ...
}
```

```
{
  "name" : "absolute_path_example",
  "reference" : "/SERVICE/service_identity",
  ...
}
```

**Note**: By using the absolute path to an identity, any service-level identity may be referenced by
any other service or component.

See [identities](#identities).

<a name="service-level-auth-to-local-properties"></a>
#### Service-level Auth-to-local-properties

Service-level auth-to-local-properties is an optional list of zero or more configuration property
specifications `(config-type/property_name[|concatenation_scheme])` indicating which properties should
be updated with dynamically generated auto-to-local rule sets.

See [auth-to-local-properties](#auth-to-local-properties).

<a name="service-level-configurations"></a>
#### Service-level Configurations

Service-level configurations is an optional configurations block listing of zero or more configuration
descriptors that are common among all components within a service. Configuration descriptors may be
overridden due to the structure of the data. However, overriding configuration properties may create
undesired behavior since it is not known until after the Kerberization process is complete what value
a property will have.

See [configurations](#configurations).

<a name="service-components"></a>
#### Components

Components is a list of zero or more component descriptor blocks.

See [components](#components).

<a name="component-level-identities"></a>
#### Component-level Identities

Component-level identities is an optional identities block containing a list of zero or more identity
descriptors that are specific to the component. A Component-level identity may be referenced
(and specialized) by using the absolute path to it (`/service_name/component_name/identity_name`).
This does not override the component-level identity, it essentially creates a copy of it and updates
the copy's properties.

See [identities](#identities).

<a name="component-level-auth-to-local-properties"></a>
#### Component-level Auth-to-local-properties

Component-level auth-to-local-properties is an optional list of zero or more configuration property
specifications `(config-type/property_name[|concatenation_scheme])` indicating which properties should
be updated with dynamically generated auto-to-local rule sets.

See [auth-to-local-properties](#auth-to-local-properties).

<a name="component-level-configurations"></a>
#### Component-level Configurations

Component-level configurations is an optional configurations block listing zero or more configuration
descriptors that are specific to the component.

See [configurations](#configurations).

### Descriptor Specifications

<a name="properties"></a>
#### properties

The `properties` block is only valid in the service-level Kerberos Descriptor file. This block is
a set of name/value pairs as follows:

```
"properties" : {
  "property_1" : "value_1",
  "property_2" : "value_2",
  ...
}
```

<a name="auth-to-local-properties"></a>
#### auth-to-local-properties

The `auth-to-local-properties` block is valid in the stack-, service-, and component-level
descriptors. This block is a list of configuration specifications
(`config-type/property_name[|concatenation_scheme]`) indicating which properties contain
auth-to-local rules that should be dynamically updated based on the identities used within the
Kerberized cluster.

The specification optionally declares the concatenation scheme to use to append
the rules into a rule set value. If specified one of the following schemes may be set:

- **`new_lines`** - rules in the rule set are separated by a new line (`\n`)
- **`new_lines_escaped`** - rules in the rule set are separated by a `\` and a new line (`\n`)
- **`spaces`** - rules in the rule set are separated by a whitespace character (effectively placing all rules in a single line)

If not specified, the default concatenation scheme is `new_lines`.

```
"auth-to-local-properties" : [
  "core-site/hadoop.security.auth_to_local",
  "service.properties/http.authentication.kerberos.name.rules|new_lines_escaped",
  ...
]
```

<a name="configurations"></a>
#### configurations

A `configurations` block may exist in stack-, service-, and component-level descriptors.
This block is a list of one or more configuration blocks containing a single structure named using
the configuration type and containing values for each relevant property.

Each property name and value may be a concrete value or contain variables to be replaced using values
from the stack-level `properties` block or any available configuration. Properties from the `properties`
block are referenced by name (`${property_name}`), configuration properties are reference by
configuration specification (`${config-type/property_name}`) and kerberos principals are referenced by the principal path
(`principals/SERVICE/COMPONENT/principal_name`).

```
"configurations" : [
  {
    "config-type-1" : {
      "${cluster-env/smokeuser}_property" : "value1",
      "some_realm_property" : "${realm}",
       ...
    }
  },
  {
    "config-type-2" : {
      "property-2" : "${cluster-env/smokeuser}",
      ...
    }
  },
  ...
]
```

If `cluster-env/smokuser` was `"ambari-qa"` and realm was `"EXAMPLE.COM"`, the above block would
effectively be translated to

```
"configurations" : [
  {
    "config-type-1" : {
      "ambari-qa_property" : "value1",
      "some_realm_property" : "EXAMPLE.COM",
      ...
    }
  },
  {
    "config-type-2" : {
      "property-2" : "ambari-qa",
      ...
    }
  },
  ...
]
```

<a name="identities"></a>
#### identities

An `identities` descriptor may exist in stack-, service-, and component-level descriptors. This block
is a list of zero or more identity descriptors. Each identity descriptor is a block containing a `name`,
an optional `reference` identifier, an optional `principal` descriptor, and an optional `keytab`
descriptor.

The `name` property of an `identity` descriptor should be a concrete name that is unique with in its
`local` scope (stack, service, or component). However, to maintain backwards-compatibility with
previous versions of Ambari, it may be a reference identifier to some other identity in the
Kerberos Descriptor. This feature is deprecated and may not be available in future versions of Ambari.

The `reference` property of an `identitiy` descriptor is optional. If it exists, it indicates that the
properties from referenced identity is to be used as the base for the current identity and any properties
specified in the local identity block overrides the base data. In this scenario, the base data is copied
to the local identities and therefore changes are realized locally, not globally. Referenced identities
may be hierarchical, so a referenced identity may reference another identity, and so on.  Because of
this, care must be taken not to create cyclic references. Reference values must be in the form of a
relative or absolute _path_ to the referenced identity descriptor. Relative _paths_ start with a `../`
and may be specified in component-level identity descriptors to reference an identity descriptor
in the parent service. Absolute _paths_ start with a `/` and may be specified at any level as follows:

- **Stack-level** identity reference: `/identitiy_name`
- **Service-level** identity reference: `/SERVICE_NAME/identitiy_name`
- **Component-level** identity reference: `/SERVICE_NAME/COMPONENT_NAME/identitiy_name`

```
"identities" : [
  {
    "name" : "local_identity",
    "principal" : {
      ...
    },
    "keytab" : {
      ...
    }
  },
  {
    "name" : "/smokeuser",
    "principal" : {
      "configuration" : "service-site/principal_property_name"
    },
    "keytab" : {
      "configuration" : "service-site/keytab_property_name"
    }
  },
  ...
]
```

<a name="principal"></a>
#### principal

The `principal` block is an optional block inside an `identity` descriptor block. It declares the
details about the identity’s principal, including the principal’s `value`, the `type` (user or service),
the relevant `configuration` property, and a local username mapping. All properties are optional; however
if no base or default value is available (via the parent identity's `reference` value) for all properties,
the principal may be ignored.

The `value` property of the principal is expected to be the normalized principal name, including the
principal’s components and realm. In most cases, the realm should be specified using the realm variable
(`${realm}` or `${kerberos-env/realm}`). Also, in the case of a service principal, "`_HOST`" should be
used to represent the relevant hostname.  This value is typically replaced on the agent side by either
the agent-side scripts or the services themselves to be the hostname of the current host. However the
built-in hostname variable (`${hostname}`) may be used if "`_HOST`" replacement on the agent-side is
not available for the service. Examples: `smokeuser@${realm}`, `service/_HOST@${realm}`.

The `type` property of the principal may be either `user` or `service`. If not specified, the type is
assumed to be `user`. This value dictates how the identity is to be created in the KDC or Active Directory.
It is especially important in the Active Directory case due to how accounts are created. It also,
indicates to Ambari how to handle the principal and relevant keytab file reguarding the user interface
behavior and data caching.

The `configuration` property is an optional configuration specification (`config-type/property_name`)
that is to be set to this principal's `value` (after its variables have been replaced).

The `local_username` property, if supplied, indicates which local user account to use when generating
auth-to-local rules for this identity. If not specified, no explicit auth-to-local rule will be generated.

```
"principal" : {
  "value": "${cluster-env/smokeuser}@${realm}",
  "type" : "user" ,
  "configuration": "cluster-env/smokeuser_principal_name",
  "local_username" : "${cluster-env/smokeuser}"
}
```

```
"principal" : {
  "value": "component1/_HOST@${realm}",
  "type" : "service" ,
  "configuration": "service-site/component1.principal"
}
```

<a name="keytab"></a>
#### keytab

The `keytab` block is an optional block inside an `identity` descriptor block. It describes how to
create and store the relevant keytab file.  This block declares the keytab file's path in the local
filesystem of the destination host, the permissions to assign to that file, and the relevant
configuration property.

The `file` property declares an absolute path to use to store the keytab file when distributing to
relevant hosts. If this is not supplied, the keytab file will not be created.

The `owner` property is an optional block indicating the local user account to assign as the owner of
the file and what access  (`"rw"` - read/write; `"r"` - read-only) should
be granted to that user. By default, the owner will be given read-only access.

The `group` property is an optional block indicating which local group to assigned as the group owner
of the file and what access (`"rw"` - read/write; `"r"` - read-only; `“”` - no access) should be granted
to local user accounts in that group. By default, the group will be given no access.

The `configuration` property is an optional configuration specification (`config-type/property_name`)
that is to be set to the path of this keytabs file (after any variables have been replaced).

```
"keytab" : {
  "file": "${keytab_dir}/smokeuser.headless.keytab",
  "owner": {
    "name": "${cluster-env/smokeuser}",
    "access": "r"
  },
  "group": {
    "name": "${cluster-env/user_group}",
    "access": "r"
  },
  "configuration": "${cluster-env/smokeuser_keytab}"
}
```

<a name="services"></a>
#### services

A `services` block may exist in the stack-level and the service-level Kerberos Descriptor file.
This block is a list of zero or more service descriptors to add to the Kerberos Descriptor.

Each service block contains a service `name`, and optionals `identities`,  `auth_to_local_properties`
`configurations`, and `components` blocks.

```
"services": [
  {
    "name": "SERVICE1_NAME",
    "identities": [
      ...
    ],
    "auth_to_local_properties" : [
      ...
    ],
    "configurations": [
      ...
    ],
    "components": [
      ...
    ]
  },
  {
    "name": "SERVICE2_NAME",
    "identities": [
      ...
    ],
    "auth_to_local_properties" : [
      ...
    ],
    "configurations": [
      ...
    ],
    "components": [
      ...
    ]
  },
  …
]
```

<a name="components"></a>
#### components

A `components` block may exist within a `service` descriptor block. This block is a list of zero or
more component descriptors belonging to the containing service descriptor. Each component descriptor
is a block containing a component `name`, and optional `identities`, `auth_to_local_properties`,
and `configurations` blocks.

```
"components": [
  {
    "name": "COMPONENT_NAME",
    "identities": [
      ...
    ],
    "auth_to_local_properties" : [
      ...
    ],
    "configurations": [
      ...
    ]
  },
  ...
]
```

<a name="examples"></a>
### Examples

#### Example Stack-level Kerberos Descriptor
The following example is annotated for descriptive purposes. The annotations are not valid in a real
JSON-formatted file.

```
{
  // Properties that can be used in variable replacement operations.
  // For example, ${keytab_dir} will resolve to "/etc/security/keytabs".
  // Since variable replacement is recursive, ${realm} will resolve
  // to ${kerberos-env/realm}, which in-turn will resolve to the
  // declared default realm for the cluster
  "properties": {
    "realm": "${kerberos-env/realm}",
    "keytab_dir": "/etc/security/keytabs"
  },
  // A list of global Kerberos identities. These may be referenced
  // using /identity_name. For example the “spnego” identity may be
  // referenced using “/spnego”
  "identities": [
    {
      "name": "spnego",
      // Details about this identity's principal. This instance does not
      // declare any value for configuration or local username. That is
      // left up to the services and components use wish to reference
      // this principal and set overrides for those values.
      "principal": {
        "value": "HTTP/_HOST@${realm}",
        "type" : "service"
      },
      // Details about this identity’s keytab file. This keytab file
      // will be created in the configured keytab file directory with
      // read-only access granted to root and users in the cluster’s
      // default user group (typically, hadoop). To ensure that only
      // a single copy exists on the file system, references to this
      // identity should not override the keytab file details;
      // however if it is desired that multiple keytab files are
      // created, these values may be overridden in a reference
      // within a service or component. Since no configuration
      // specification is set, the the keytab file location will not
      // be set in any configuration file by default. Services and
      // components need to reference this identity to update this
      // value as needed.
      "keytab": {
        "file": "${keytab_dir}/spnego.service.keytab",
        "owner": {
          "name": "root",
          "access": "r"
        },
        "group": {
          "name": "${cluster-env/user_group}",
          "access": "r"
        }
      }
    },
    {
      "name": "smokeuser",
      // Details about this identity's principal. This instance declares
      // a configuration and local username mapping. Services and
      // components can override this to set additional configurations
      // that should be set to this principal value.  Overriding the
      // local username may create undesired behavior since there may be
      // conflicting entries in relevant auth-to-local rule sets.
      "principal": {
        "value": "${cluster-env/smokeuser}@${realm}",
        "type" : "user",
        "configuration": "cluster-env/smokeuser_principal_name",
        "local_username" : "${cluster-env/smokeuser}"
      },
      // Details about this identity’s keytab file. This keytab file
      // will be created in the configured keytab file directory with
      // read-only access granted to the configured smoke user
      // (typically ambari-qa) and users in the cluster’s default
      // user group (typically hadoop). To ensure that only a single
      // copy exists on the file system, references to this identity
      // should not override the keytab file details; however if it
      // is desired that multiple keytab files are created, these
      // values may be overridden in a reference within a service or
      // component.
      "keytab": {
        "file": "${keytab_dir}/smokeuser.headless.keytab",
        "owner": {
          "name": "${cluster-env/smokeuser}",
          "access": "r"
        },
        "group": {
          "name": "${cluster-env/user_group}",
          "access": "r"
        },
        "configuration": "cluster-env/smokeuser_keytab"
      }
    }
  ]
}
```

#### Example Service-level Kerberos Descriptor
The following example is annotated for descriptive purposes. The annotations are not valid in a real
JSON-formatted file.

```
{
  // One or more services may be listed in a service-level Kerberos
  // Descriptor file
  "services": [
    {
      "name": "SERVICE_1",
      // Service-level identities to be created if this service is installed.
      // Any relevant keytab files will be distributed to hosts with at least
      // one of the components on it.
      "identities": [
        // Service-specific identity declaration, declaring all properties
        // needed initiate the creation of the principal and keytab files,
        // as well as setting the service-specific  configurations.  This may
        // be referenced by contained components using ../service1_identity.
        {
          "name": "service1_identity",
          "principal": {
            "value": "service1/_HOST@${realm}",
            "type" : "service",
            "configuration": "service1-site/service1.principal"
          },
          "keytab": {
            "file": "${keytab_dir}/service1.service.keytab",
            "owner": {
              "name": "${service1-env/service_user}",
              "access": "r"
            },
            "group": {
              "name": "${cluster-env/user_group}",
              "access": "r"
            },
            "configuration": "service1-site/service1.keytab.file"
          }
        },
        // Service-level identity referencing the stack-level spnego
        // identity and overriding the principal and keytab configuration
        // specifications.
        {
          "name": "service1_spnego",
          "reference": "/spnego",
          "principal": {
            "configuration": "service1-site/service1.web.principal"
          },
          "keytab": {
            "configuration": "service1-site/service1.web.keytab.file"
          }
        },
        // Service-level identity referencing the stack-level smokeuser
        // identity. No properties are being overridden and overriding
        // the principal and keytab configuration specifications.
        // This ensures that the smokeuser principal is created and its
        // keytab file is distributed to all hosts where components of this
        // this service are installed.
        {
          "name": "service1_smokeuser",
          "reference": "/smokeuser"
        }
      ],
      // Properties related to this service that require the auth-to-local
      // rules to be dynamically generated based on identities create for
      // the cluster.
      "auth_to_local_properties" : [
        "service1-site/security.auth_to_local"
      ],
      // Configuration properties to be set when this service is installed,
      // no matter which components are installed
      "configurations": [
        {
          "service-site": {
            "service1.security.authentication": "kerberos",
            "service1.security.auth_to_local": ""
          }
        }
      ],
      // A list of components related to this service
      "components": [
        {
          "name": "COMPONENT_1",
          // Component-specific identities to be created when this component
          // is installed.  Any keytab files specified will be distributed
          // only to the hosts where this component is installed.
          "identities": [
            // An identity "local" to this component
            {
              "name": "component1_service_identity",
              "principal": {
                "value": "component1/_HOST@${realm}",
                "type" : "service",
                "configuration": "service1-site/comp1.principal",
                "local_username" : "${service1-env/service_user}"
              },
              "keytab": {
                "file": "${keytab_dir}/s1c1.service.keytab",
                "owner": {
                  "name": "${service1-env/service_user}",
                  "access": "r"
                },
                "group": {
                  "name": "${cluster-env/user_group}",
                  "access": ""
                },
                "configuration": "service1-site/comp1.keytab.file"
              }
            },
            // The stack-level spnego identity overridden to set component-specific
            // configurations
            {
              "name": "component1_spnego_1",
              "reference": "/spnego",
              "principal": {
                "configuration": "service1-site/comp1.spnego.principal"
              },
              "keytab": {
                "configuration": "service1-site/comp1.spnego.keytab.file"
              }
            },
            // The stack-level spnego identity overridden to set a different set of component-specific
            // configurations
            {
              "name": "component1_spnego_2",
              "reference": "/spnego",
              "principal": {
                "configuration": "service1-site/comp1.someother.principal"
              },
              "keytab": {
                "configuration": "service1-site/comp1.someother.keytab.file"
              }
            }
          ],
          // Component-specific configurations to set if this component is installed
          "configurations": [
            {
              "service-site": {
                "comp1.security.type": "kerberos"
              }
            }
          ]
        },
        {
          "name": "COMPONENT_2",
          "identities": [
            {
              "name": "component2_service_identity",
              "principal": {
                "value": "component2/_HOST@${realm}",
                "type" : "service",
                "configuration": "service1-site/comp2.principal",
                "local_username" : "${service1-env/service_user}"
              },
              "keytab": {
                "file": "${keytab_dir}/s1c2.service.keytab",
                "owner": {
                  "name": "${service1-env/service_user}",
                  "access": "r"
                },
                "group": {
                  "name": "${cluster-env/user_group}",
                  "access": ""
                },
                "configuration": "service1-site/comp2.keytab.file"
              }
            },
            // The service-level service1_identity identity overridden to
            // set component-specific configurations
            {
              "name": "component2_service1_identity",
              "reference": "../service1_identity",
              "principal": {
                "configuration": "service1-site/comp2.service.principal"
              },
              "keytab": {
                "configuration": "service1-site/comp2.service.keytab.file"
              }
            }
          ],
          "configurations" : [
            {
              "service-site" : {
                "comp2.security.type": "kerberos"
              }
            }
          ]
        }
      ]
    }
  ]
}
```
