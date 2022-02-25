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

Utils
============

Description
-----
This module provides common utils for views

Requirements
-----

- Ambari 2.1.0 or later

HDFS Utility
-----

HdfsApi class provides business delegate for HDFS client that provides proxyuser configuration.
You can create the HdfsApi based on your ViewContext:

    HdfsApi api = HdfsUtil.connectToHDFSApi(viewContext);

It will read instance properties and create HdfsApi configured to specific cluster. NameNodes HA is supported.

AmbariApi
-----

AmbariApi provides methods to get Ambari configurations and cluster topology.

Custer association functionality:

    AmbariApi api = new AmbariApi(viewContext);
    Cluster cluster = api.getCluster();

It can work with local cluster or with remote cluster based on your instance properties of Ambari URL,
username and password in the ViewContext. To determine if you have associated cluster, either local or remote:

    boolean isAssociated = api.isClusterAssociated();

Also provides the API to get cluster topology:

    List<String> nnHosts = api.getHostsWithComponent("NAMENODE");
