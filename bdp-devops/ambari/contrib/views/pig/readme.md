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

Pig View
============

Description
-----
This View provides a UI to create, save and run pig scripts. You can browse the list of pig scripts you have created and saved.
You can see the history of runs of the pig scripts, view the logs and download the results. You can also upload and use
UDFs with your pig scripts.

Requirements
-----

- Ambari 1.7.0 or later
- HDFS with WebHDFS configured
- WebHCat with Pig configured

Build
-----

The view can be built as a maven project.

    mvn clean install

The build will produce the view archive.

    target/pig-0.1.0-SNAPSHOT.jar

Place the view archive on the Ambari Server and restart to deploy.    

    cp pig-0.1.0-SNAPSHOT.jar /var/lib/ambari-server/resources/views/
    ambari-server restart

Cluster Configuration
-----
Configure HDFS for a proxy user. In core-site.xml, add the following properties:

    hadoop.proxyuser.root.hosts=*
    hadoop.proxyuser.root.groups=*
    hadoop.proxyuser.hcat.hosts=*
    hadoop.proxyuser.hcat.groups=*

Configure WebHCat for a proxy user. In webhcat-site.xml, add the following properties:

    webhcat.proxyuser.hcat.hosts=*
    webhcat.proxyuser.hcat.groups=*

Create Hadoop users and make members of the hdfs, hadoop and users groups. For example, to create a user "admin": 

    useradd -G hdfs admin
    usermod -a -G users admin
    usermod -a -G hadoop admin

Check the "admin" user has the correct group membership.

    id admin
    uid=1002(admin) gid=1002(admin) groups=1002(admin),100(users),503(hadoop),498(hdfs)


Single Node Cluster
-----

The following section describes how to use the [Ambari Vagrant](https://cwiki.apache.org/confluence/display/AMBARI/Quick+Start+Guide) setup to create a single-node cluster with the Pig View. 

Install Ambari Server and Ambari Agent.

Manually register Ambari Agent with Server.

Setup and Start Ambari Server.

Create Blueprint using the provided [blueprint.json](blueprint.json) file.
  
    POST
    http://c6401.ambari.apache.org:8080/api/v1/blueprints/pig-view

Create Cluster using the provided [clustertemplate.json](clustertemplate.json) file
    
    POST
    http://c6401.ambari.apache.org:8080/api/v1/clusters/PigView

After the cluster is created, deploy the Pig View into Ambari.

    cp pig-0.1.0-SNAPSHOT.jar /var/lib/ambari-server/resources/views/
    ambari-server restart

From the Ambari Administration interface, create a Pig view instance.

|Property|Value|
|---|---|
| Details: Instance Name | PIG_1 |
| Details: Display Name | Pig |
| Details: Description | Save and execute Pig scripts |
| Properties: WebHDFS FileSystem URI | webhdfs://c6401.ambari.apache.org:50070 |
| Properties: WebHCat URL | http://c6401.ambari.apache.org:50111/templeton/v1 |
| Properties: Scripts HDFS Directory | /tmp/${username}/scripts |
| Properties: Jobs HDFS Directory | /tmp/${username}/jobs |

Login to Ambari as "admin" and browse to the view instance.

    http://c6401.ambari.apache.org:8080/#/main/views/PIG/0.1.0/PIG_1
