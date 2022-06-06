#!/bin/bash
#
#/*
# * Licensed to the Apache Software Foundation (ASF) under one
# * or more contributor license agreements.  See the NOTICE file
# * distributed with this work for additional information
# * regarding copyright ownership.  The ASF licenses this file
# * to you under the Apache License, Version 2.0 (the
# * "License"); you may not use this file except in compliance
# * with the License.  You may obtain a copy of the License at
# *
# *     http://www.apache.org/licenses/LICENSE-2.0
# *
# * Unless required by applicable law or agreed to in writing, software
# * distributed under the License is distributed on an "AS IS" BASIS,
# * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# * See the License for the specific language governing permissions and
# * limitations under the License.
# */

CUR_DIR=`pwd`

BASEDIR="$( cd "$( dirname "$0" )" && pwd )"

if [[ -z "${BUILD_DIR}" ]]; then
  BUILD_DIR="${BASEDIR}/build/"
fi

if [[ -z "${VERSION}" ]]; then
  VERSION=${VERSION:-1.7.0}
fi

if [[ -z "${RELEASE}" ]]; then
  RELEASE=${RELEASE:-1}
fi

rm -rf ${BUILD_DIR}/*

PKG_NAME="hdp_mon_nagios_addons"
PKG_FULL_NAME="${PKG_NAME}-$VERSION"

MON_TAR_DIR="${BUILD_DIR}/${PKG_FULL_NAME}/"
SRC_DIR="${BASEDIR}/../../src/addOns/nagios/"
NAGIOS_ALERTS_DIR="${BASEDIR}/../../../nagios-alerts"


############ Mapping

mkdir -p "${MON_TAR_DIR}/usr/lib64/nagios"
cp -r "${SRC_DIR}/plugins" "${MON_TAR_DIR}/usr/lib64/nagios"
cp -r "${NAGIOS_ALERTS_DIR}/plugins" "${MON_TAR_DIR}/usr/lib64/nagios"

mkdir -p "${MON_TAR_DIR}/etc/apache2"
cp -r "${SRC_DIR}/conf.d" "${MON_TAR_DIR}/etc/apache2"

mkdir -p "${MON_TAR_DIR}/usr/share/hdp/nagios"
cp -r ${SRC_DIR}/scripts/* "${MON_TAR_DIR}/usr/share/hdp/nagios"

############ Create data tar

cd ${BUILD_DIR}
tar -zcvf data.tar.gz -C "$PKG_FULL_NAME" .

############ Create control archive
cp "${BASEDIR}/nagios_addon_deb_control" "${BUILD_DIR}/control"

TAR_CONTROL_DEST="${BUILD_DIR}/control.tar.gz"
tar -czf "${TAR_CONTROL_DEST}" control
############ Create debian-binary
echo "2.0" > debian-binary

############ Pack to deb package
ar rcv "$PKG_FULL_NAME.deb" debian-binary control.tar.gz data.tar.gz
