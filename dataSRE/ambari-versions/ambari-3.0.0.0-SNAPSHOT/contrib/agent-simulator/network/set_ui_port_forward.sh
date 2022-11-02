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

# This script will forward the port from eth0 to the weave internal IP

# run this script with root
# $1 <external IP>
# $2 <Weave internal IP>
# $3 <Port>

if [ $# -lt 3 ]; then
    echo "usage: ./set_ui_port_forward.sh <external IP> <Weave internal IP> <Port>"
    echo "example: ./set_ambari_server_network.sh 104.196.56.56 192.168.2.2 8088"
    exit 1
fi

from_ip=$1
to_ip=$2
port=$3

# enable forwarding
echo 1 > /proc/sys/net/ipv4/ip_forward

echo "forward port $port from $from_ip to $to_ip"

#just to be sure firewall doesn't block
iptables -I FORWARD -p tcp -d $from_ip --dport $port -j ACCEPT
#actual forward
iptables -t nat -A PREROUTING -p tcp -i eth0 --dport $port -j DNAT --to $to_ip

#same for UDP
iptables -I FORWARD -p udp -d $from_ip --dport $port -j ACCEPT
iptables -t nat -A PREROUTING -p udp -i eth0 --dport $port -j DNAT --to $to_ip