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
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#
##################################################
# Script to define, install and start FLUME service
# after Ambari installer finishes.
#
# Flume service is defined, installed and started 
# via API calls.
##################################################

if (($# != 4)); then
  echo "Usage: $0: <AMBARI_HOST> <FLUME1_INTERNAL_HOST> <FLUME2_INTERNAL_HOST> <CLUSTER_NAME>";
  exit 1;
fi

AMBARIURL="http://$1:8080"
USERID="admin"
PASSWD="admin"

defineService () {
  if curl -s -u $USERID:$PASSWD "$AMBARIURL/api/v1/clusters/$1/services" | grep service_name | cut -d : -f 2 | grep -q FLUME ; then
    echo "FLUME service already defined.";
  else
    echo "Defining FLUME";
    curl -u $USERID:$PASSWD -X POST "$AMBARIURL/api/v1/clusters/$1/services" --data "[{\"ServiceInfo\":{\"service_name\":\"FLUME\"}}]";
  fi
}

defineServiceComponent () {
  if curl -s -u $USERID:$PASSWD "$AMBARIURL/api/v1/clusters/$1/services/FLUME" | grep components | cut -d : -f 2 | grep -q "\[ \]" ; then
    echo "Defining FLUME_HANDLER service component"
    curl -u $USERID:$PASSWD -X POST "$AMBARIURL/api/v1/clusters/$1/services?ServiceInfo/service_name=FLUME" --data "{\"components\":[{\"ServiceComponentInfo\":{\"component_name\":\"FLUME_HANDLER\"}}]}";
  else
    echo "FLUME_HANDLER service component already defined."
  fi
}

defineHostComponent () {
  if ! curl -s -u $USERID:$PASSWD "$AMBARIURL/api/v1/clusters/$2/hosts/$1" | grep component_name | cut -d : -f 2 | grep -q "FLUME_HANDLER" ; then
    echo "Defining FLUME_HANDLER host component on $1"
    curl -u $USERID:$PASSWD -X POST "$AMBARIURL/api/v1/clusters/$2/hosts?Hosts/host_name=$1" --data "{\"host_components\":[{\"HostRoles\":{\"component_name\":\"FLUME_HANDLER\"}}]}";
  else
    echo "FLUME_HANDLER host component already defined on $1."
  fi
}

installService () {
  if curl -s -u $USERID:$PASSWD "$AMBARIURL/api/v1/clusters/$1/services/FLUME" | grep state | cut -d : -f 2 | grep -q "INIT" ; then
    echo "Installing FLUME_HANDLER service"
    curl -u $USERID:$PASSWD -X PUT "$AMBARIURL/api/v1/clusters/$1/services?ServiceInfo/state=INIT&ServiceInfo/service_name=FLUME" --data "{\"RequestInfo\": {\"context\" :\"Install Flume Service\"}, \"Body\": {\"ServiceInfo\": {\"state\": \"INSTALLED\"}}}";
  else
    echo "FLUME_HANDLER already installed."
  fi
}

startService () {
  if curl -s -u $USERID:$PASSWD "$AMBARIURL/api/v1/clusters/$1/services/FLUME" | grep state | cut -d : -f 2 | grep -q "STARTED" ; then
    echo "FLUME_HANDLER already started."
  else
    echo "Starting FLUME_HANDLER service"
    curl -u $USERID:$PASSWD -X PUT "$AMBARIURL/api/v1/clusters/$1/services?ServiceInfo/state=INSTALLED&ServiceInfo/service_name=FLUME" --data "{\"RequestInfo\": {\"context\" :\"Start Flume Service\"}, \"Body\": {\"ServiceInfo\": {\"state\": \"STARTED\"}}}";
  fi
}


defineService $4
defineServiceComponent  $4
defineHostComponent $2 $4
# defineHostComponent $3 $4
installService $4
startService $4
