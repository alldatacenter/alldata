#!/bin/sh

# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.


filename="version_241-12345.xml"

rm $filename

python version_builder.py --file $filename --release-type STANDARD
python version_builder.py --file $filename --release-stack HDP-2.4
python version_builder.py --file $filename --release-version 2.4.1.1
python version_builder.py --file $filename --release-build 12345
python version_builder.py --file $filename --release-notes http://example.com
python version_builder.py --file $filename --release-display HDP-2.4.1.1-1234
python version_builder.py --file $filename --release-compatible 2.4.[0-1].0

# call any number of times for each service in the repo
python version_builder.py --file $filename --manifest --manifest-id HDFS-271 --manifest-service HDFS --manifest-version 2.7.1.2.4 --manifest-release-version 2.4.0.0
python version_builder.py --file $filename --manifest --manifest-id HBASE-132 --manifest-service HBASE --manifest-version 1.3.2.4.3

#call any number of times for the target services to upgrade
python version_builder.py --file $filename --available --manifest-id HDFS-271
python version_builder.py --file $filename --available --manifest-id HBASE-132 --release-version 2.4.0

# must be before repo calls
python version_builder.py --file $filename --os --os-family redhat6 --os-package-version 2_4_1_1_12345

#call any number of times for repo per os
python version_builder.py --file $filename --repo --repo-os redhat6 --repo-id HDP-2.4 --repo-name HDP --repo-url http://public-repo-1.hortonworks.com/HDP/centos6/2.x/updates/2.4.1.1 --repo-unique true
python version_builder.py --file $filename --repo --repo-os redhat6 --repo-id HDP-UTILS-1.1.0.20 --repo-name HDP-UTILS --repo-url http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.20/repos/centos6 --repo-unique false


python version_builder.py --file $filename --finalize --xsd ../../ambari-server/src/main/resources/version_definition.xsd

# to upload this to running Ambari instance on localhost:
# curl -u admin:admin -H 'Content-Type: text/xml' -X POST -d @$filename http://localhost:8080/api/v1/version_definitions
