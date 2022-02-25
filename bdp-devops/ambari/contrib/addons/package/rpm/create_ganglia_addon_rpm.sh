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
  VERSION=${VERSION:-1.2.2}
fi

if [[ -z "${RELEASE}" ]]; then
  RELEASE=${RELEASE:-1}
fi

#rm -rf ${BUILD_DIR}/*

PKG_NAME="hdp_mon_ganglia_addons"

MON_TAR_DIR="${BUILD_DIR}/${PKG_NAME}-$VERSION/"

mkdir -p "${MON_TAR_DIR}"
cp -r ${BASEDIR}/../../src/addOns/ganglia/* ${MON_TAR_DIR}

TAR_DEST="${BUILD_DIR}/${PKG_NAME}-$VERSION.tar.gz"

cd ${BUILD_DIR};
tar -zcf "${TAR_DEST}" "${PKG_NAME}-$VERSION/"

RPM_BUILDDIR=${BUILD_DIR}/rpmbuild/

mkdir -p ${RPM_BUILDDIR}
mkdir -p ${RPM_BUILDDIR}/SOURCES/
mkdir -p ${RPM_BUILDDIR}/SPECS/
mkdir -p ${RPM_BUILDDIR}/BUILD/
mkdir -p ${RPM_BUILDDIR}/RPMS/
mkdir -p ${RPM_BUILDDIR}/SRPMS/

cp -f ${BASEDIR}/${PKG_NAME}.spec ${RPM_BUILDDIR}/SPECS/
cp -f ${TAR_DEST} ${RPM_BUILDDIR}/SOURCES/

echo "${VERSION}" > ${RPM_BUILDDIR}/SOURCES/version.txt
echo "${RELEASE}" > ${RPM_BUILDDIR}/SOURCES/release.txt

cd ${RPM_BUILDDIR}

cmd="rpmbuild --define \"_topdir ${RPM_BUILDDIR}\" \
    -bb ${RPM_BUILDDIR}/SPECS/${PKG_NAME}.spec"

echo $cmd
eval $cmd
ret=$?
if [[ "$ret" != "0" ]]; then
  echo "Error: rpmbuild failed, error=$ret"
  exit 1
fi

cd ${CUR_DIR}

RPM_DEST="${RPM_BUILDDIR}/RPMS/noarch/${PKG_NAME}-$VERSION-$RELEASE.noarch.rpm"
if [[ ! -f "${RPM_DEST}" ]]; then
  echo "Error: ${RPM_DEST} does not exist"
  exit 1
fi

exit 0
