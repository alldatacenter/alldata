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

# This script will set the docker use a new partition as its storage

# $1 mount point to other part

mount_point=$1
docker_dir=/var/lib/docker

while [ ! -d "$docker_dir" ]; do
    echo "$docker_dir does not exist, wait for docker service to create the directory"
    sleep 5
done

echo "move $docker_dir to partition $mount_point"

service docker stop
cp -r ${docker_dir}/* $mount_point
rm -rf $docker_dir
ln -s $mount_point $docker_dir
service docker start