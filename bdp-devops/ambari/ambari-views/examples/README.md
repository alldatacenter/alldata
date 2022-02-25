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

Ambari Views Examples
============

The *Ambari Views Examples* are a collection of simple Ambari Views provided as a development guide.

## Documentation
See the documentation pages for the view examples.

* [Hello World View](helloworld-view/docs/index.md) : Demonstrates the very basics of how to write and deploy a view in Ambari.
* [Hello Servlet View](hello-servlet-view/docs/index.md) : Includes instance parameters and a servlet for a dynamic UI. 
* [Hello Spring View](hello-spring-view/docs/index.md) : A very simple view example that uses Spring MVC.
* [Favorite view](favorite-view/docs/index.md) : Exposes a simple resource to work with instance parameters and data.
* [Calculator View](calculator-view/docs/index.md) : Includes a simple resource.
* [Phone List View](phone-list-view/docs/index.md) : Demonstrates simple view persistence.
* [Phone List Upgrade View](phone-list-upgrade-view/docs/index.md) : Demonstrates view data migration from Phone List View.
* [Property View](property-view/docs/index.md) : Demonstrates view configuration property options.
* [Property Validator View](property-validator-view/docs/index.md) : Demonstrates configuration property validator.
* [Auto Cluster view](auto-cluster-view/docs/index.md) : Example view that can be auto-created and configured to leverage the Ambari REST APIs to access a cluster being managed by Ambari.

Please also visit the [Apache Ambari Project](http://ambari.apache.org/) page for more information.


## License

*Ambari* is released under [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

## Issue Tracking

Report any issues via the [Ambari JIRA](https://issues.apache.org/jira/browse/AMBARI).

## Build

####Examples

######Requirements
* JDK 1.6
* Maven 3.0

######Maven modules
* ambari-views (ambari view interfaces)

######Maven build goals
 * Clean : mvn clean
 * Compile : mvn compile
 * Run tests : mvn test
 * Create JARS : mvn package
 * Install JARS in M2 cache : mvn install

######Tests options
  * -DskipTests to skip tests when running the following Maven goals:
    'package', 'install', 'deploy' or 'verify'
  * -Dtest=\<TESTCLASSNAME>,\<TESTCLASSNAME#METHODNAME>,....
  * -Dtest.exclude=\<TESTCLASSNAME>
  * -Dtest.exclude.pattern=\*\*/\<TESTCLASSNAME1>.java,\*\*/\<TESTCLASSNAME2>.java


