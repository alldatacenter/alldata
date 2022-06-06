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

# Dockerfile for installing the necessary dependencies for building Ambari on Ubuntu 16.04

FROM ubuntu:16.04

RUN apt-get update -q -y \
  && apt-get install -y --no-install-recommends \
    bzip2 \
    curl \
    g++ \
    gcc \
    git \
    jq \
    make \
    openjdk-8-jdk \
    python-setuptools \
    python2.7 \
    python2.7-dev \
    xz-utils \
  && rm -rf /var/lib/apt/lists/*
