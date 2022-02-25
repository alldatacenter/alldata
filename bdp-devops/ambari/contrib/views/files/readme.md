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

Files View
============

Description
-----
This View provides a UI to browse HDFS, create directories and upload + download files.

Requirements
-----

- Ambari 1.7.0 or later
- HDFS with WebHDFS configured

Build
-----

The view can be built as a maven project.

    mvn clean install

The build will produce the view archive.

    target/files-0.1.0-SNAPSHOT.jar

Place the view archive on the Ambari Server and restart to deploy.    

    cp files-0.1.0-SNAPSHOT.jar /var/lib/ambari-server/resources/views/
    ambari-server restart

Cluster Configuration
-----

Configure HDFS for a proxy user. In core-site.xml, add the following properties:

    hadoop.proxyuser.root.hosts=*
    hadoop.proxyuser.root.groups=*

Create Hadoop users and make members of the hdfs, hadoop and users groups. For example, to create a user "admin": 

    useradd -G hdfs admin
    usermod -a -G users admin
    usermod -a -G hadoop admin

Check the "admin" user has the correct group membership.

    id admin
    uid=1002(admin) gid=1002(admin) groups=1002(admin),100(users),503(hadoop),498(hdfs)


Deploying the View
-----

Use the [Ambari Vagrant](https://cwiki.apache.org/confluence/display/AMBARI/Quick+Start+Guide) setup to create a cluster:

Deploy the Files view into Ambari.

    cp files-0.1.0-SNAPSHOT.jar /var/lib/ambari-server/resources/views/
    ambari-server restart

From the Ambari Administration interface, create a Files view instance.

|Property|Value|
|---|---|
| Details: Instance Name | FILES_1 |
| Details: Display Name | Files |
| Details: Description | Browse HDFS files and directories |
| Properties: WebHDFS FileSystem URI | webhdfs://c6401.ambari.apache.org:50070 |

Login to Ambari as "admin" and browse to the view instance.

    http://c6401.ambari.apache.org:8080/#/main/views/FILES/0.1.0/FILES_1

Optional: Development with Local Vagrant VM
-----

After deploying the view into a Vagrant VM, if you want to perform view development from your local build, perform the following:

Browse to the work directory.

    cd /var/lib/ambari-server/resources/views/work

Remove the FILES{0.1.0} directory.

    rm FILES\{0.1.0\}
    
Create a link to your local build.

    ln -s /vagrant/ambari/contrib/views/files/target/classes/ FILES\{0.1.0\}

Restart Ambari Server.

    ambari-server restart

