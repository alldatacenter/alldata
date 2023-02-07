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

SRCDIR=$1  #../../../../protocol/src/main/protobuf
TARGDIR=$2 #../build/protobuf
FNAME=$3   #Types.proto

fixFile(){
    echo "Fixing $1"
    pushd ${TARGDIR} >& /dev/null
    sed  -e 's/REQUIRED/DM_REQUIRED/g' -e  's/OPTIONAL/DM_OPTIONAL/g' -e 's/REPEATED/DM_REPEATED/g' -e 's/NULL/DM_UNKNOWN/g' $1 > temp1.proto
    cp temp1.proto $1
    rm temp1.proto
    popd >& /dev/null
}

main() {
    if [ ! -e ${TARGDIR} ]
    then
        echo "Creating Protobuf directory"
        mkdir -p ${TARGDIR}
    fi
    cp -r ${SRCDIR}/* ${TARGDIR}

    if [ -e ${TARGDIR}/${FNAME} ]
    then
        fixFile ${FNAME}
    else
        echo "$FNAME not found"
        exit 1
    fi
}

main

