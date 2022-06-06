#!/bin/sh
#
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
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#
if [ "$1" != '${hdpUrlForCentos6}' ]
then
  # Updating for new stack version:
  #  Create the new stack definition with parameterized repoinfo.xml files.
  #  Typically, copying the files from the previous version will work.
  #  Modify the VERSION variable in this file to match the new version
  #  Modify the previous version to store concrete public repo url

  VERSION=2.1
  C6URL="$1"
  LATEST_URL="$2"
  C5URL="${C6URL/centos6/centos5}"
  S11URL="${C6URL/centos6/suse11}"
  U12URL="${C6URL/centos6/ubuntu12}"

  STACKS_DIR=$3/src/main/resources/stacks/HDP/
  HDPREPO_DIR=$STACKS_DIR/${VERSION}/repos

  echo "Processing '${HDPREPO_DIR}/repoinfo.xml' and '${HDPLOCALREPO}/repoinfo.xml'"
  echo "$3"

  echo "Setting centos5 stack url to '$C5URL'"
  sed "s;REPLACE_WITH_CENTOS5_URL;$C5URL;" ${HDPREPO_DIR}/repoinfo.xml >  ${HDPREPO_DIR}/repoinfo.xml.tmp; mv ${HDPREPO_DIR}/repoinfo.xml.tmp ${HDPREPO_DIR}/repoinfo.xml

  echo "Setting centos6 stack url to '$C6URL'"
  sed "s;REPLACE_WITH_CENTOS6_URL;$C6URL;" ${HDPREPO_DIR}/repoinfo.xml >  ${HDPREPO_DIR}/repoinfo.xml.tmp; mv ${HDPREPO_DIR}/repoinfo.xml.tmp ${HDPREPO_DIR}/repoinfo.xml

  echo "Setting suse11 stack url to '$S11URL'"
  sed  "s;REPLACE_WITH_SUSE11_URL;$S11URL;" ${HDPREPO_DIR}/repoinfo.xml >  ${HDPREPO_DIR}/repoinfo.xml.tmp; mv ${HDPREPO_DIR}/repoinfo.xml.tmp ${HDPREPO_DIR}/repoinfo.xml

  echo "Setting ubuntu12 stack url to '$U12URL'"
  sed  "s;REPLACE_WITH_UBUNTU12_URL;$U12URL;" ${HDPREPO_DIR}/repoinfo.xml >  ${HDPREPO_DIR}/repoinfo.xml.tmp; mv ${HDPREPO_DIR}/repoinfo.xml.tmp ${HDPREPO_DIR}/repoinfo.xml

   
  # all stacks get the same url
  for HDP_VERSION_DIR in $(find $STACKS_DIR -mindepth 1 -maxdepth 1 -type d) ; do
    HDPREPO_DIR=$HDP_VERSION_DIR/repos
    echo "Replacing $ver latest lookup url to '$LATEST_URL'"
    sed "s;\(<latest>\)\([^>]*\)\(<\/latest>\);\1$LATEST_URL\3;" ${HDPREPO_DIR}/repoinfo.xml > ${HDPREPO_DIR}/repoinfo.xml.tmp; mv ${HDPREPO_DIR}/repoinfo.xml.tmp ${HDPREPO_DIR}/repoinfo.xml
  done
fi
