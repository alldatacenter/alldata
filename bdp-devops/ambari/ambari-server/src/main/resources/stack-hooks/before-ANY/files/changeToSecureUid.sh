#!/usr/bin/env bash
#
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#

username=$1
directories=$2
newUid=$3

function find_available_uid() {
 for ((i=1001; i<=2000; i++))
 do
   grep -q $i /etc/passwd
   if [ "$?" -ne 0 ]
   then
    newUid=$i
    break
   fi
 done
}

if [ -z $2 ]; then
  test $(id -u ${username} 2>/dev/null)
  if [ $? -ne 1 ]; then
   newUid=`id -u ${username}`
  else
   find_available_uid
  fi
  echo $newUid
  exit 0
else
  find_available_uid
fi

if [ $newUid -eq 0 ]
then
  echo "Failed to find Uid between 1000 and 2000"
  exit 1
fi

set -e
dir_array=($(echo $directories | sed 's/,/\n/g'))
old_uid=$(id -u $username)
sudo_prefix="/var/lib/ambari-agent/ambari-sudo.sh -H -E"
echo "Changing uid of $username from $old_uid to $newUid"
echo "Changing directory permisions for ${dir_array[@]}"
$sudo_prefix usermod -u $newUid $username && for dir in ${dir_array[@]} ; do ls $dir 2> /dev/null && echo "Changing permission for $dir" && $sudo_prefix chown -Rh $newUid $dir ; done
exit 0
