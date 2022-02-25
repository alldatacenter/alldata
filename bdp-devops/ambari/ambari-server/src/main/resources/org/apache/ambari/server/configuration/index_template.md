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

Ambari Server Configuration
---
- [Introduction](#introduction)
- [Configuration Properties](#configuration-properties)
- [Baseline Values](#baseline-values)
- [Database And Persistence](#db-persistence)

<a name="introduction"></a>
## Introduction
Ambari Server is configured using a flat text properties file. The file is read once when starting Ambari. Any changes made to the properties file are only reflected after restarting Ambari. 

```
/etc/ambari-server/conf/ambari.properties
```

<a name="configuration-properties"></a>
## Configuration Properties
The following are the properties which can be used to configure Ambari. 

$configuration-properties

<a name="baseline-values"></a>
## Baseline Values
As the size of a cluster grows, some of the default property values may no longer be sufficient. The below tables offer recommendations on the values of some configuration properties based on the size and usage of the cluster.

$baseline-values

<a name="db-persistence"></a>
## Database And Persistence
In addition to the static database connection properties, it's possible to supply custom properties for both EclipseLink and the database driver through `ambari.properties`. 

### Database Driver
Using the `server.jdbc.properties.` prefix, it's possible to supply specific properties to the database driver being used by Ambari. These properties do not affect JPA or any of the persistence-unit operations. They are solely to govern the behavior of the database driver. 

```
server.jdbc.properties.lockTimeout=15000
server.jdbc.properties.loginTimeout=15000
```

### Persistence Unit
EclipseLink properties can also be configured using a prefix of `server.persistence.properties.`. The EclipseLink properties should be defined in their entirety with the prefix prepended in front of them.

```
server.persistence.properties.eclipselink.jdbc.batch-writing.size=25
server.persistence.properties.eclipselink.profiler=QueryMonitor
```