#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#
FROM openjdk:8-jdk
RUN apt-get update \
    && apt-get install -y libsnappy-java net-tools vim \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /opt/inlong-agent
# add tarball from target output
ARG AGENT_TARBALL
ADD ${AGENT_TARBALL} /opt/inlong-agent
RUN cp /usr/share/java/snappy-java.jar lib/snappy-java-*.jar
# add mysql connector
RUN wget -P lib/ https://repo1.maven.org/maven2/mysql/mysql-connector-java/8.0.27/mysql-connector-java-8.0.27.jar
EXPOSE 8008
ENV MANAGER_OPENAPI_IP=127.0.0.1
ENV MANAGER_OPENAPI_PORT=8082
ENV DATAPROXY_IP=127.0.0.1
ENV DATAPROXY_PORT=46801
ENV AUDIT_PROXY_URL=127.0.0.1:10081
ENV ETH_NETWORK=eth0
ENV AGENT_FETCH_INTERVAL=10
ENV AGENT_HEARTBEAT_INTERVAL=10
ENV AGENT_JVM_HEAP_OPTS="-XX:+UseContainerSupport -XX:InitialRAMPercentage=40.0 -XX:MaxRAMPercentage=80.0 -XX:-UseAdaptiveSizePolicy"
ADD agent-docker.sh bin/
RUN chmod +x bin/agent-docker.sh
CMD ["bin/agent-docker.sh"]
