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

Auto Cluster View Example
======

Description
-----
The Auto Cluster View is an example of a view that can be auto-created and configured to leverage
the Ambari REST APIs to access a cluster being managed by Ambari.

Build
-----

The view can be built as a maven project.

    cd ambari-views/examples/auto-cluster-view
    mvn clean package

The build will produce the view archive.

    ambari-views/examples/auto-cluster-view/target/auto-cluster-view-???.jar

Deploy
------

Place the view archive on the Ambari Server and start to deploy.    

    cp auto-cluster-view-???.jar /var/lib/ambari-server/resources/views/
    ambari-server start


Cluster Association
-----

To associate a view instance with a cluster, you need to have an Ambari managed cluster.
Use the Ambari install wizard to create a cluster that includes the YARN service.

When the YARN service is added to the cluster a new instance of the AUTO-CLUSTER view named AUTO_CLUSTER_INSTANCE should be automatically added.
 
Access the view instance end point:
     
     api/v1/views/AUTO-CLUSTER/versions/0.1.0/instances/AUTO_CLUSTER_INSTANCE
           