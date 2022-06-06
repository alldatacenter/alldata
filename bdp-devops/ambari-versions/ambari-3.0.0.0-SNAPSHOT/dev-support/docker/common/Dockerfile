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

# Dockerfile for installing the necessary dependencies for building Ambari

ARG BUILD_OS
FROM ambari-build-base:${BUILD_OS}

ARG MAVEN_VERSION=3.3.9
ENV MAVEN_HOME /opt/maven
RUN mkdir -p $MAVEN_HOME \
  && maven_url=$(curl -L -s -S http://www.apache.org/dyn/closer.cgi/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz?as_json=1 \
      | jq --raw-output '.preferred,.path_info' \
      | sed -e '1N' -e 's/\n//') \
  && : ${maven_url:="http://www.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz"} \
  && echo "Downloading Maven from ${maven_url}" \
  && curl -L -s -S "${maven_url}" \
    | tar -xzf - --strip-components 1 -C $MAVEN_HOME
ENV PATH "$PATH:$MAVEN_HOME/bin"

COPY .bashrc /root/
