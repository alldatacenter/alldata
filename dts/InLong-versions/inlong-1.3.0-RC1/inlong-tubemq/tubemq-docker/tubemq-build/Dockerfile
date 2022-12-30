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
# compile protobuf
FROM gcc:5.4.0 as protobuf
RUN apt-get update && apt-get install -y unzip \
    && rm -rf /var/lib/apt/lists/
ARG PROTOBUF_VERSION=2.5.0
RUN curl -LOk "https://github.com/protocolbuffers/protobuf/releases/download/v${PROTOBUF_VERSION}/protobuf-${PROTOBUF_VERSION}.zip" \
    && unzip "protobuf-${PROTOBUF_VERSION}.zip" -d "/opt/" \
    && cd /opt/protobuf-${PROTOBUF_VERSION} \
    && ./configure --prefix=/usr && make && make DESTDIR=/protobuf/ install \
    && rm -f "protobuf-${PROTOBUF_VERSION}.zip"
# for building tubemq
FROM maven:3-openjdk-8
WORKDIR /tubemq/
COPY --from=protobuf /protobuf/usr/bin/* /usr/bin/
COPY --from=protobuf /protobuf/usr/include/* /usr/include/
COPY --from=protobuf /protobuf/usr/lib/* /usr/lib/
# ADD settings.xml /root/.m2/
# mvn command
ENTRYPOINT ["/usr/bin/mvn"]
