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

# This script is used to add a Host/VM to the Docker network
# for example, an Ambari-agent within a VM (not in a Docker container)

# run this script with root
# $1 <Weave internal IP of the VM>
# $2 <Weave DNS IP of the VM>
# $3 <Subnet mask of Weave network>
# $4 <The host name of this VM inside the Weave network>
# $5 <The domain name of this VM inside the Weave network>, which is supposed to be end with "weave.local"
# $6 <External IP address of Ambari server>

if [ $# -lt 6 ]; then
    echo "usage: ./set_ambari_server_network.sh <Weave internal IP> <Weave DNS IP> <Mask> <Weave host name> <Weave domain name> <Ambari server IP>"
    echo "example: ./set_ambari_server_network.sh 192.168.254.1 192.168.254.2 16 yourname-group-c-service-server-1 yourname-group-c-service-server-1.weave.local 104.196.91.170"
    exit 1
fi

weave_internal_ip=$1
weave_dns_ip=$2
mask=$3
weave_host_name=$4
weave_domain_name=$5
ambari_server_ip=$6

# install weave
chmod 755 ../Linux/CentOS7/weave_install.sh
../Linux/CentOS7/weave_install.sh

# install docker
chmod 755 ../Linux/CentOS7/docker_install.sh
../Linux/CentOS7/docker_install.sh

# reset weave
weave reset

# launch weave
weave launch $ambari_server_ip

# launch Weave DNS
weave launch-dns ${weave_dns_ip}/${mask}

# expose IP and a new domain name.
# MUST use one expose command to set both ip and domain name, OR the domain name will not be bind to this weave internal IP
weave expose ${weave_internal_ip}/${mask} -h $weave_domain_name

# set domain name resolution
python dns_edit.py $weave_dns_ip

# add Weave local IP, host name, domain name mapping
content="$(cat /etc/hosts)"
echo "${weave_internal_ip} ${weave_domain_name} ${weave_host_name}" > /etc/hosts
echo "$content" >> /etc/hosts