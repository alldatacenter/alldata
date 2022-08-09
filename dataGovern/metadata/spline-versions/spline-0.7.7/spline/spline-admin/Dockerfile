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

# Run image using:
# $> docker run --rm -it absaoss/spline-admin:latest

ARG DOCKER_BASE_IMAGE_PREFIX

FROM "$DOCKER_BASE_IMAGE_PREFIX"adoptopenjdk:11-jre

ARG VERSION
ARG IMAGE_NAME

ENV IMAGE_NAME=$IMAGE_NAME

LABEL \
    vendor="ABSA" \
    copyright="2021 ABSA Group Limited" \
    license="Apache License, version 2.0" \
    name="Spline Admin Tool"

# Install Tini
RUN apt-get update \
 && apt-get install -y --no-install-recommends \
            tini=0.18* \
 && rm -rf /var/lib/apt/lists/*

USER 1001

COPY target/admin-$VERSION.jar ./admin.jar
COPY entrypoint.sh .

ENTRYPOINT ["tini", "-g", "--", "sh", "./entrypoint.sh"]
CMD ["--help"]
