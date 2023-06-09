#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# This Dockerfile may be used during development. It adds built binaries from distribution/target folder
# into the target image based on openjdk:8 image.  If you've built Drill using a JDK version greater than
# the one in the FROM command in this Dockerfile then you should bump this one up to match or exceed that.

FROM openjdk:8

# Project version defined in pom.xml is passed as an argument
ARG VERSION

ENV DRILL_HOME=/opt/drill DRILL_USER=drilluser

RUN mkdir /opt/drill

COPY target/apache-drill-$VERSION/apache-drill-$VERSION /opt/drill

RUN groupadd -g 999 $DRILL_USER \
 && useradd -r -u 999 -g $DRILL_USER $DRILL_USER -m -d /var/lib/drill \
 && chown -R $DRILL_USER: $DRILL_HOME

USER $DRILL_USER

# Starts Drill in embedded mode and connects to Sqlline
ENTRYPOINT /opt/drill/bin/drill-embedded
