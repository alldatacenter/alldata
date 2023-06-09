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

returnCode=0
if [ -f $1.md5 ]; then
    if [ "$(cat $1.md5)" = "$(openssl md5 $1 | cut -d ' ' -f 2)" ]; then
        echo "md5 present and Ok"
    else
        echo "md5 does not match"
        returnCode=1
    fi
fi

if [ -f $1.sha1 ]; then
    if [ "$(cat $1.sha1)" = "$(openssl sha1 $1 | cut -d ' ' -f 2)" ]; then
        echo "sha1 present and Ok"
    else
        echo "sha1 does not match"
        returnCode=2
    fi
fi

if [ -f $1.sha512 ]; then
    if [ "$(cat $1.sha512)" = "$(openssl sha1 -sha512 $1 | cut -d ' ' -f 2)" ]; then
        echo "sha512 present and Ok"
    else
        echo "sha512 does not match"
        returnCode=3
    fi
fi

if [ -f $1.asc ]; then
    echo "GPG verification output"
    returnCode=`gpg --verify $1.asc $1`
fi
exit $returnCode
