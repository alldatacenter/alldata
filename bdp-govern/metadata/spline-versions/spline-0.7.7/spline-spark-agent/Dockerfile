#
# Copyright 2021 ABSA Group Limited
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

ARG DOCKER_BASE_IMAGE_PREFIX=

FROM "$DOCKER_BASE_IMAGE_PREFIX"maven:3.6.0-jdk-8-slim

LABEL \
    vendor="ABSA" \
    copyright="2021 ABSA Group Limited" \
    license="Apache License, version 2.0" \
    name="Spline Agent for Apache Spark - Example suite"

COPY . /opt/spline-spark-agent

ENV SPLINE_PRODUCER_URL=http://host.docker.internal:8080/producer
ENV SPLINE_MODE=REQUIRED

ENV HTTP_PROXY_HOST=
ENV HTTP_PROXY_PORT=
ENV HTTP_NON_PROXY_HOSTS=

RUN cd /opt/spline-spark-agent && \
    mvn install -D skipTests && \
    cd ./examples && \
    # it's for dry-run maven caching
    mvn test -P examples -D spline.mode=DISABLED

ENV WORKDIR=/opt/spline-spark-agent/examples

WORKDIR $WORKDIR

CMD exec mvn test -P examples \
    -D spline.producer.url=$SPLINE_PRODUCER_URL \
    -D spline.mode=$SPLINE_MODE \
    -D http.proxyHost=$HTTP_PROXY_HOST \
    -D http.proxyPort=$HTTP_PROXY_PORT \
    -D http.nonProxyHosts=$HTTP_NON_PROXY_HOSTS \
