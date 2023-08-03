#!/bin/bash
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
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

echo_stderr ()
{
    echo "$@" >&2
}


if [ $# -ne 5 ]
then
        echo_stderr "usage: $0 <service_name> <num_of_tags> <initial_id> <output_file> <seconds_to_sleep>"
        exit 1
fi

service_name=$1
num_of_tags=$2
initial_id=$3
output_file=$4
seconds_to_sleep=$5

echo_stderr "$0 $service_name $num_of_tags $initial_id $output_file $seconds_to_sleep"

while true
do
	./gen_service_tags.sh ${service_name} ${num_of_tags} ${initial_id} > /tmp/$$-${output_file}
	mv /tmp/$$-${output_file} ${output_file}
	((initial_id+=${num_of_tags}))
	sleep ${seconds_to_sleep}
done
