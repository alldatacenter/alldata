# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

Check Apache Ranger Wiki for up to date instruction: 
https://cwiki.apache.org/confluence/display/RANGER/Index

NOTE about using sending pull request on github:
================================================
Apache Ranger is currently NOT setup to use pull requests to take in the changes for commit.
Please use the apache review board to submit your code changes for review and commit. https://reviews.apache.org
Also create a jira to go along with the review and mention it in the review board review. https://issues.apache.org/jira/browse/RANGER

Build Process
=============

1. Check out the code from GIT repository

2. On the root folder, please execute the following Maven command:

   $ mvn clean compile package install
   $ mvn eclipse:eclipse

   (Ranger Admin UI tests depend on PhantomJS. If the build fails with npm or Karma errors you can either
      i. install PhantomJS dependencies for your platform (bzip2 and fontconfig)
     ii. skip JavaScript test execution: mvn -DskipJSTests ...)

3. After the above build command execution, you should see the following TAR files in the target folder:


   ranger-<version>-admin.tar.gz
   ranger-<version>-atlas-plugin.tar.gz
   ranger-<version>-hbase-plugin.tar.gz
   ranger-<version>-hdfs-plugin.tar.gz
   ranger-<version>-hive-plugin.tar.gz
   ranger-<version>-kafka-plugin.tar.gz
   ranger-<version>-kms.tar.gz
   ranger-<version>-knox-plugin.tar.gz
   ranger-<version>-migration-util.tar.gz
   ranger-<version>-ranger-tools.tar.gz
   ranger-<version>-solr-plugin.tar.gz
   ranger-<version>-sqoop-plugin.tar.gz
   ranger-<version>-src.tar.gz
   ranger-<version>-storm-plugin.tar.gz
   ranger-<version>-tagsync.tar.gz
   ranger-<version>-usersync.tar.gz
   ranger-<version>-yarn-plugin.tar.gz
   ranger-<version>-kylin-plugin.tar.gz
   ranger-<version>-elasticsearch-plugin.tar.gz
   ranger-<version>-ozone-plugin.tar.gz
   ranger-<version>-presto-plugin.tar.gz
   ranger-<version>-schema-registry-plugin.tar.gz

Importing Apache Ranger Project into Eclipse
============================================

1. Create a Eclipse workspace called 'ranger'

2. Import maven project from the root directory where ranger source code is downloaded (and build)


Deployment Process
==================

Installation Host Information
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
1.  Ranger Admin Tool Component  (ranger-<version-number>-admin.tar.gz) should be installed on a host where Policy Admin Tool web application runs on port 6080 (default).
2.  Ranger User Synchronization Component (ranger-<version-number>-usersync.tar.gz) should be installed on a host to synchronize the external user/group information into Ranger database via Ranger Admin Tool.
3.  Ranger Component plugin should be installed on the component boxes:
    (a)  HDFS Plugin needs to be installed on Name Node hosts
    (b)  Hive Plugin needs to be installed on HiveServer2 hosts
    (c)  HBase Plugin needs to be installed on both Master and Regional Server nodes.
    (d)  Knox Plugin needs to be installed on Knox gateway host.
    (e)  Storm Plugin needs to be installed on Storm hosts.
    (f)  Kafka/Solr Plugin needs to be installed on their respective component hosts.
    (g)  YARN plugin needs to be installed on YARN Resource Manager hosts
    (h)  Sqoop plugin needs to be installed on Sqoop2 hosts
    (i)  Kylin plugin needs to be installed on Kylin hosts
    (j)  Elasticsearch plugin needs to be installed on Elasticsearch hosts
    (k)  Ozone plugin needs to be installed on Ozone hosts
    (l)  Presto plugin needs to be installed on Presto hosts
    (m)  Schema Registry plugin needs to be installed on Schema Registry hosts

Installation Process
~~~~~~~~~~~~~~~~~~~~

1. Download the tar.gz file into a temporary folder in the box where it needs to be installed.

2. Expand the tar.gz file into /usr/lib/ranger/ folder

3. Go to the component name under the expanded folder (e.g. /usr/lib/ranger/ranger-<version-number>-admin/)

4. Modify the install.properties file with appropriate variables

5. If the module has setup.sh, 
       Execute ./setup.sh

   If the install.sh file does not exists, 
       Execute ./enable-<component>-plugin.sh

