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

- [Introduction](#introduction)
  - [How it Works](#how-it-works)
  - [Enabling Kerberos](#enabling-kerberos)
  - [Adding Components](#adding-components)
  - [Adding Hosts](#adding-hosts)
  - [Regenerating Keytabs](#regenerating-keytabs)
  - [Disabling Kerberos](#disabling-kerberos)
- [The Kerberos Descriptor](kerberos_descriptor.md)
- [The Kerberos Service](kerberos_service.md)
- [Enabling Kerberos](enabling_kerberos.md)

<a name="introduction"></a>
## Introduction

Before Ambari 2.0.0, configuring an Ambari cluster to use Kerberos involved setting up the Kerberos
client infrastructure on each host, creating the required identities, generating and distributing the
needed keytabs files, and updating the necessary configuration properties. On a small cluster this may
not seem to be too large of an effort; however as the size of the cluster increases, so does the amount
of work that is involved.

This is where Ambari’s Kerberos Automation facility can help. It performs all of these steps and
also helps to maintain the cluster as new services and hosts are added.

Kerberos automation can be invoked using Ambari’s REST API as well as via the _Enable Kerberos Wizard_
in the Ambari UI.

<a name="how-it-works"></a>
### How it works

Stacks and services that can utilize Kerberos credentials for authentication must have a Kerberos
Descriptor declaring required Kerberos identities and how to update configurations. The Ambari
infrastructure uses this data, and any updates applied by an administrator, to perform Kerberos
related operations such as initially enabling Kerberos, enabling Kerberos for on hosts and added
components, regenerating credentials, and disabling Kerberos.

It should be notated that the Kerberos service is required to be installed on all hosts of the cluster
before any automated tasks can be performed. If using the Ambari UI, this should happen as part of the
Enable Kerberos wizard workflow.

<a name="enabling-kerberos"></a>
### Enabling Kerberos

When enabling Kerberos, all of the services in the cluster are expected to be stopped. The main
reason for this is to avoid state issues as the services are stopped and then started when the cluster
is transitioning to use Kerberos.

The following steps are taken to enable Kerberos on the cluster en masse:

1. Create or update accounts in the configured KDC (or Active Directory)
2. Generate keytab files and distribute them to the appropriate hosts
3. Update relevant configurations

<a name="adding-components"></a>
### Adding Components

If Kerberos is enabled for the Ambari cluster, whenever new components are added, the new components
will automatically be configured for Kerberos, and any necessary principals and  keytab files will be
created and distributed as needed.

For each new component, the following steps will occur before the component is installed and started:

1. Update relevant configurations
2. Create or update accounts in the configured KDC (or Active Directory)
3. Generate keytab files and distribute them to the appropriate hosts

<a name="adding-hosts"></a>
### Adding Hosts

When adding a new host, the Kerberos client must be installed on it. This does not happen automatically;
however the _Add Host Wizard_ in the Ambari UI will will perform this step if Kerberos was enabled for
the Ambari cluster. Once the host is added, generally one or more components are installed on
it - see [Adding Components](#adding-components).

<a name="regenerating-keytabs"></a>
### Regenerating Keytabs

Once a cluster has Kerberos enabled, it may be necessary to regenerate keytabs. There are two options
for this:

- `all` - create any missing principals and unconditionally update the passwords for existing principals, then create and distribute all relevant keytab files
- `missing` - create any missing principals; then create and distribute keytab files for the newly-created principals

In either case, the affected services should be restarted after the regeneration process is complete.

If performed through the Ambari UI, the user will be asked which keytab regeneration mode to use and
whether services are to be restarted or not. 

<a name="disabling-kerberos"></a>
### Disabling Kerberos

In the event Kerberos needs to be removed from the Ambari cluster, Ambari will remove the managed
Kerberos identities, keytab files, and Kerberos-specific configurations. The Ambari UI will perform
the steps of stopping and starting the services as well as removing the Kerberos service; however
this will need to be done manually, if using the Ambari REST API.
