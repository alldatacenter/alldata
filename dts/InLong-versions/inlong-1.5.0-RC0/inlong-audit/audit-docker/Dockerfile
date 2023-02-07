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
# add tarball from target output
RUN apt-get update \
    && apt-get install -y net-tools vim default-mysql-client \
    && rm -rf /var/lib/apt/lists/*
ARG AUDIT_TARBALL
ADD ${AUDIT_TARBALL} /opt/inlong-audit
EXPOSE 10081
# pulsar or tubemq
ENV MQ_TYPE=pulsar
ENV PULSAR_BROKER_LIST="127.0.0.1:6650"
ENV PULSAR_AUDIT_TOPIC="persistent:\/\/public\/default\/inlong-audit"
ENV TUBE_MASTER_LIST="localhost:8715"
ENV TUBE_AUDIT_TOPIC="inlong-audit"
ENV AUDIT_DBNAME="apache_inlong_audit"
# proxy/store/all, start audit module individually, or all
ENV START_MODE="all"
# mysql / clickhouse / elasticsearch
ENV STORE_MODE=mysql
# mysql
ENV JDBC_URL=127.0.0.1:3306
ENV USERNAME=root
ENV PASSWORD=inlong
# clickhouse
ENV STORE_CK_URL=127.0.0.1:8123
ENV STORE_CK_USERNAME=default
ENV STORE_CK_PASSWD=default
ENV STORE_CK_DBNAME="apache_inlong_audit"
# elasticsearch
ENV STORE_ES_HOST=127.0.0.1
ENV STORE_ES_PORT=9200
ENV STORE_ES_AUTHENABLE=false
ENV STORE_ES_USERNAME=elastic
ENV STORE_ES_PASSWD=inlong
# jvm
ENV AUDIT_JVM_HEAP_OPTS="-XX:+UseContainerSupport -XX:InitialRAMPercentage=40.0 -XX:MaxRAMPercentage=80.0 -XX:-UseAdaptiveSizePolicy"
WORKDIR /opt/inlong-audit
# add mysql connector
RUN wget -P lib/ https://repo1.maven.org/maven2/mysql/mysql-connector-java/8.0.27/mysql-connector-java-8.0.27.jar
ADD audit-docker.sh bin/
RUN chmod +x bin/audit-docker.sh
CMD ["bin/audit-docker.sh"]
