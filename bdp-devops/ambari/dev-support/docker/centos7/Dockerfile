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

# Dockerfile for installing the necessary dependencies for building Ambari on CentOS7

FROM centos:7

RUN yum -q -y install \
    curl \
    gcc \
    gcc-c++ \
    git \
    java-1.8.0-openjdk-devel \
    make \
    openssl \
    python \
    python-devel \
    python-setuptools \
    rpm-build \
    which \
  && yum clean all \
  && rm -rf /var/cache/yum

RUN curl -LSs -o /usr/local/bin/jq https://github.com/stedolan/jq/releases/download/jq-1.5/jq-linux64 \
  && chmod 755 /usr/local/bin/jq
