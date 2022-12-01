#
# Copyright 2019 ABSA Group Limited
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

FROM "$DOCKER_BASE_IMAGE_PREFIX"tomcat:9.0.45-jdk11-corretto

LABEL \
    vendor="ABSA" \
    copyright="2021 ABSA Group Limited" \
    license="Apache License, version 2.0" \
    name="Spline Kafka Gateway Server"

EXPOSE 8080
EXPOSE 8009

RUN rm -rf /usr/local/tomcat/webapps/*

USER 1001
ARG PROJECT_BUILD_FINAL_NAME
COPY --chown=1001 target/$PROJECT_BUILD_FINAL_NAME/ /usr/local/tomcat/webapps/ROOT/
