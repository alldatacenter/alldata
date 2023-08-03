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


1. Introduction

2. SampleApp
   A simple application to demonstrate use of pluggable authorization.
   - IAuthorizer:
      the authorization interface. Authorizes read/write/execute access to a given file
   - DefaultAuthorizer:
      default authorizer implementation, authorizes all accesses
   - SampleApp:
      - main application that prompts the user to enter access to authorize in the following format:
         read filePath user1 userGroup1 userGroup2 userGroup3
         write filePath user1 userGroup1 userGroup2 userGroup3
         execute filePath user1 userGroup1 userGroup2 userGroup3

3. SampleApp Plugin
   - RangerAuthorizer implements IAuthorizer interface and performs authorization using Ranger policies.
   - For simplicity, uses policies in a HDFS service instance (like cl1_hadoop): which uses 'path' as the resource and supports 'read', 'write' and 'execute' accessTypes
   - conf/ranger-sampleapp-security.xml: has configurations for plugin, like Ranger Admin URL, name of the service containing policies
   - conf/ranger-sampleapp-audit.xml: has configurations for plugin audit, like log4j logger name, HDFS folder, DB connection details

4. Build
   $ mvn clean compile package assembly:assembly
   $ cd ranger-examples
   $ mvn clean compile package assembly:assembly
   # Following files created by the build will be required to setup SampleApp:
     target/ranger-examples-<version>-sampleapp.tar.gz
     target/ranger-examples-<version>-sampleapp-plugin.tar.gz

5. Setup SampleApp
   # Create a empty directory to setup the application
   $ mkdir /tmp/sampleapp
   $ cd /tmp/sampleapp
   $ tar xvfz ranger-examples-<version>-sampleapp.tar.gz
   # add Ranger authorizer bits
   $ tar xvfz ranger-examples-<version>-sampleapp-plugin.tar.gz
   # Review and update properties in conf/ranger-sampleapp-security.xml, especially the following:
     - ranger.plugin.sampleapp.policy.rest.url
     - ranger.plugin.sampleapp.service.name
   # Review and update properties in conf/ranger-sampleapp-audit.xml
   # Review and update properties in conf/log4j.xml

6. Execute
   - Use default authorizer i.e. not Ranger:
     $ cd /tmp/sampleapp
     $ ./run-sampleapp.sh
     # At the prompt, enter commands to trigger access authorization, like:
     command> read filePath user1 userGroup1 userGroup2 userGroup3
     command> write filePath user1 userGroup1 userGroup2 userGroup3
     command> execute filePath user1 userGroup1 userGroup2 userGroup3

   - Use Ranger authorizer
     $ cd /tmp/sampleapp
     $ ./run-sampleapp.sh ranger-authz
     # At the prompt, enter commands to trigger access authorization, like:
     command> read filePath user1 userGroup1 userGroup2 userGroup3
     command> write filePath user1 userGroup1 userGroup2 userGroup3
     command> execute filePath user1 userGroup1 userGroup2 userGroup3
	 # audit logs can be seen in /tmp/ranger_audit.log
