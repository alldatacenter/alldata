#!/bin/bash
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

SRCDIR=$1 #../build/src/protobuf
TARGDIR=$2 #../protobuf
INCDIR=$3 #../include/drill/protobuf


main() {
    if [ ! -e ${TARGDIR} ]
    then
        echo "Creating Protobuf directory"
        mkdir -p ${TARGDIR}
    fi

    cp -r ${SRCDIR}/*.cc ${TARGDIR}
    cp -r ${SRCDIR}/*.h ${TARGDIR}

    if [ ! -e ${INCDIR} ]
    then
        echo "Creating Protobuf includes directory"
        mkdir -p ${INCDIR}
    fi

    mv ${TARGDIR}/Types.pb.h ${INCDIR}
}

main

