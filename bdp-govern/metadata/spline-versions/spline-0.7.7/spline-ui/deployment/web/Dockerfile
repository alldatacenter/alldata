#
# Copyright 2020 ABSA Group Limited
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

FROM "$DOCKER_BASE_IMAGE_PREFIX"tomcat:9.0.45-jdk8-openjdk-slim

LABEL \
    vendor="ABSA" \
    copyright="2020 ABSA Group Limited" \
    license="Apache License, version 2.0" \
    name="Spline Web UI"

EXPOSE 8080
EXPOSE 8009

RUN rm -rf /usr/local/tomcat/webapps/*
COPY target/*.war /usr/local/tomcat/webapps/ROOT.war
