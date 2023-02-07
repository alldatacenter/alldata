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

echo "Drill Stdout Message"
echo "Stderr Message" 1>&2

output="$DRILL_HOME/../output.txt"
while [[ $# > 0 ]]
do
  if [[ $1 =~ ^-Dlog.path= ]]; then
    thelog=${1/-Dlog.path=//}
    echo "Drill Log Message" >> $thelog
  fi
  echo $1 >> $output
  shift
done

function clean_up {
  echo "Received SIGTERM"
  if [ "$PRETEND_HUNG" == "1" ]; then
    echo "Pretending to be hung."
  else
    echo "Exiting"
    if [ "$PRETEND_FAIL" == "1" ]; then
      exit 55
    else
      exit 0
    fi
  fi
}

trap clean_up SIGTERM

if [ "$KEEP_RUNNING" == "1" ]; then
  while [[ 1 > 0 ]]
  do
    sleep 1
  done
fi
