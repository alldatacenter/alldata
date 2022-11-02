<!---
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at [http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

Ambari SCOM Management Pack
============

The *Ambari SCOM Management Pack* gives insight into the performance and health of an Apache Hadoop cluster to users of
Microsoft System Center Operations Manager (SCOM). *Ambari SCOM* integrates with the Ambari REST API which aggregates and exposes cluster information and metrics.

## Documentation

Look for *Ambari SCOM* documentation on the [Apache Ambari Wiki](https://cwiki.apache.org/confluence/display/AMBARI/Ambari+SCOM+Management+Pack). Please also visit the [Apache Ambari Project](http://incubator.apache.org/ambari/) page for more information.

## License

*Ambari SCOM* is released under [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

## Issue Tracking

Report any issues via the [Ambari JIRA](https://issues.apache.org/jira/browse/AMBARI) using component `Ambari-SCOM`.

## Build

####Ambari-SCOM and Metrics Sink

######Requirements
* JDK 1.6
* Maven 3.0
    
######Maven modules
* ambari-scom-project (Parent POM for all modules)
  * ambari-scom (ambari MSI and SQL Server provider)
  * metrics-sink (Metrics SQL Server sink)       
  
######Maven build goals
 * Clean : mvn clean
 * Compile : mvn compile
 * Run tests : mvn test 
 * Create JAR : mvn package
 * Install JAR in M2 cache : mvn install     
    
######Tests options
  * -DskipTests to skip tests when running the following Maven goals:
    'package', 'install', 'deploy' or 'verify'
  * -Dtest=\<TESTCLASSNAME>,\<TESTCLASSNAME#METHODNAME>,....
  * -Dtest.exclude=\<TESTCLASSNAME>
  * -Dtest.exclude.pattern=\*\*/\<TESTCLASSNAME1>.java,\*\*/\<TESTCLASSNAME2>.java

####Management Pack

See [Building the Management Pack](management-pack/Hadoop_MP/BUILDING.md) for more information.


