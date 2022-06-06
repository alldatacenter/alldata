#!/bin/bash

# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information rega4rding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

if [ "${AUTHORIZED_KEYS}" != "**None**" ]; then
    echo "=> Found authorized keys"
    mkdir -p /root/.ssh
    chmod 700 /root/.ssh
    touch /root/.ssh/authorized_keys
    chmod 600 /root/.ssh/authorized_keys
    IFS=$'\n'
    arr=$(echo ${AUTHORIZED_KEYS} | tr "," "\n")
    for x in $arr
    do
        x=$(echo $x |sed -e 's/^ *//' -e 's/ *$//')
        cat /root/.ssh/authorized_keys | grep "$x" >/dev/null 2>&1
        if [ $? -ne 0 ]; then
            echo "=> Adding public key to /root/.ssh/authorized_keys: $x"
            echo "$x" >> /root/.ssh/authorized_keys
        fi
    done
fi

if [ ! -f /.root_pw_set ]; then
	/set_root_pw.sh
fi
exec /usr/sbin/sshd -D