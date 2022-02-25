#!/bin/bash

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

if [ "${BRANCH}" == "" ]
then
  BRANCH=master
fi

if [ "${GIT_URL}" == "" ]
then
  GIT_URL=https://github.com/apache/atlas.git
fi

if [ "${PROFILE}" != "" ]
then
  ARG_PROFILES="-P${PROFILE}"
fi

if [ "${SKIPTESTS}" == "" ]
then
  ARG_SKIPTESTS="-DskipTests"
else
  ARG_SKIPTESTS="-DskipTests=${SKIPTESTS}"
fi

if [ "${BUILD_HOST_SRC}" == "" ]
then
  BUILD_HOST_SRC=true
fi

export MAVEN_OPTS="-Xms2g -Xmx2g"
export M2=/home/atlas/.m2


if [ "${BUILD_HOST_SRC}" == "true" ]
then
  if [ ! -f /home/atlas/src/pom.xml ]
  then
    echo "ERROR: BUILD_HOST_SRC=${BUILD_HOST_SRC}, but /home/atlas/src/pom.xml is not found "

	exit 1
  fi

  echo "Building from /home/atlas/src"

  cd /home/atlas/src
else
  echo "Building ${BRANCH} branch from ${GIT_URL}"

  cd /home/atlas/git

  if [ -d atlas ]
  then
    renamedDir=atlas-`date +"%Y%m%d-%H%M%S"`

    echo "Renaming existing directory `pwd`/atlas to ${renamedDir}"

    mv atlas $renamedDir
  fi

  git clone --single-branch --branch ${BRANCH} ${GIT_URL}

  cd /home/atlas/git/atlas

  for patch in `ls -1 /home/atlas/patches | sort`
  do
    echo "applying patch /home/atlas/patches/${patch}"
    git apply /home/atlas/patches/${patch}
  done
fi

mvn ${ARG_PROFILES} ${ARG_SKIPTESTS} -DskipDocs clean package

mv -f distro/target/apache-atlas-${ATLAS_VERSION}-server.tar.gz     /home/atlas/dist/
mv -f distro/target/apache-atlas-${ATLAS_VERSION}-hive-hook.tar.gz  /home/atlas/dist/
mv -f distro/target/apache-atlas-${ATLAS_VERSION}-hbase-hook.tar.gz /home/atlas/dist/
