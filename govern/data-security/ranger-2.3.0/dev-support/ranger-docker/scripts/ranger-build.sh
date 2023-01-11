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
  BRANCH=ranger-2.1
fi

if [ "${GIT_URL}" == "" ]
then
  GIT_URL=https://github.com/apache/ranger.git
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
export M2=/home/ranger/.m2


if [ "${BUILD_HOST_SRC}" == "true" ]
then
  if [ ! -f /home/ranger/src/pom.xml ]
  then
    echo "ERROR: BUILD_HOST_SRC=${BUILD_HOST_SRC}, but /home/ranger/src/pom.xml is not found "
    exit 1
  fi

  echo "Building from /home/ranger/src"

  cd /home/ranger/src
else
  echo "Building ${BRANCH} branch from ${GIT_URL}"

  cd /home/ranger/git

  if [ -d ranger ]
  then
    renamedDir=ranger-`date +"%Y%m%d-%H%M%S"`

    echo "Renaming existing directory `pwd`/ranger to ${renamedDir}"

    mv ranger $renamedDir
  fi

  git clone --single-branch --branch ${BRANCH} ${GIT_URL}

  cd /home/ranger/git/ranger

  for patch in `ls -1 /home/ranger/patches | sort`
  do
    echo "applying patch /home/ranger/patches/${patch}"
    git apply /home/ranger/patches/${patch}
  done
fi

mvn ${ARG_PROFILES} ${ARG_SKIPTESTS} -DskipDocs clean package

mv -f target/version /home/ranger/dist/
mv -f target/ranger-* /home/ranger/dist/
