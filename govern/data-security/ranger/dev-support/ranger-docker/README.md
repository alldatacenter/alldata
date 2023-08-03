<!---
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

## Overview

Docker files in this folder create docker images and run them to build Apache Ranger, deploy Apache Ranger and dependent services in containers.

## Usage

1. Ensure that you have recent version of Docker installed from [docker.io](http://www.docker.io) (as of this writing: Engine 20.10.5, Compose 1.28.5).
   Make sure to configure docker with at least 6gb of memory.

2. Update environment variables in ```.env``` file, if necessary

3. Set ```dev-support/ranger-docker``` as your working directory.

4. Execute following command to download necessary archives to setup Ranger/HDFS/Hive/HBase/Kafka/Knox services:
   ~~~
   chmod +x download-archives.sh && ./download-archives.sh
   ~~~

5. Execute following commands to set environment variables to build Apache Ranger docker containers:
   ~~~
   export DOCKER_BUILDKIT=1
   export COMPOSE_DOCKER_CLI_BUILD=1
   export RANGER_DB_TYPE=postgres
   ~~~

6. Build Apache Ranger in containers using docker-compose

   1. Execute following command to build Apache Ranger:
      ~~~
      docker-compose -f docker-compose.ranger-base.yml -f docker-compose.ranger-build.yml up
      ~~~

      Time taken to complete the build might vary (upto an hour), depending on status of ```${HOME}/.m2``` directory cache.

   2. Alternatively, the following commands can be executed from the parent directory
      1. To generate tarballs:```mvn clean package -DskipTests```

      2. Copy the tarballs and version file to ```dev-support/ranger-docker/dist```
         ~~~
         cp target/ranger-* dev-support/ranger-docker/dist/
         cp target/version dev-support/ranger-docker/dist/
         ~~~

      3. Build the ranger-base image:
         ~~~
         docker-compose -f docker-compose.ranger-base.yml build --no-cache
         ~~~

7. Execute following command to start Ranger, Ranger enabled HDFS/YARN/HBase/Hive/Kafka/Knox and dependent services (Solr, DB) in containers:
   ~~~
   docker-compose -f docker-compose.ranger-base.yml -f docker-compose.ranger.yml -f docker-compose.ranger-${RANGER_DB_TYPE}.yml -f docker-compose.ranger-usersync.yml -f docker-compose.ranger-tagsync.yml -f docker-compose.ranger-kms.yml -f docker-compose.ranger-hadoop.yml -f docker-compose.ranger-hbase.yml -f docker-compose.ranger-kafka.yml -f docker-compose.ranger-hive.yml -f docker-compose.ranger-knox.yml up -d
   ~~~

	- valid values for RANGER_DB_TYPE: mysql or postgres

8. To rebuild specific images and start containers with the new image, use following command:
   ~~~
   docker-compose -f docker-compose.ranger-base.yml -f docker-compose.ranger.yml -f docker-compose.ranger-usersync.yml -f docker-compose.ranger-tagsync.yml -f docker-compose.ranger-kms.yml -f docker-compose.ranger-hadoop.yml -f docker-compose.ranger-hbase.yml -f docker-compose.ranger-kafka.yml -f docker-compose.ranger-hive.yml -f docker-compose.ranger-knox.yml up -d --no-deps --force-recreate --build <service-1> <service-2>
   ~~~

9. Ranger Admin can be accessed at http://localhost:6080 (admin/rangerR0cks!)
