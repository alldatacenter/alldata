#!/bin/bash
#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# Initialize the configuration files of inlong components

INLONG_HOME=$(
  cd $(dirname $0)
  cd ..
  pwd
)
source $INLONG_HOME/conf/inlong.conf

init_inlong_agent() {
  echo "Init agent configuration parameters"
  cd $INLONG_HOME/inlong-agent/conf
  sed -i 's/agent.local.ip=.*/'''agent.local.ip=${local_ip}'''/g' agent.properties
  sed -i 's/agent.http.port=.*/'''agent.http.port=${agent_port}'''/g' agent.properties
  sed -i 's/agent.manager.vip.http.host=.*/'''agent.manager.vip.http.host=${manager_server_hostname}'''/g' agent.properties
  sed -i 's/agent.manager.vip.http.port=.*/'''agent.manager.vip.http.port=${manager_server_port}'''/g' agent.properties
  sed -i 's/audit.proxys=.*/'''audit.proxys=${audit_proxys_ip}:${audit_proxys_port}'''/g' agent.properties
}

init_inlong_audit() {
  echo "Init audit configuration parameters"
  cd $INLONG_HOME/inlong-audit/conf
  sed -i 's#jdbc:mysql://.*apache_inlong_audit#'''jdbc:mysql://${spring_datasource_hostname}:${spring_datasource_port}/apache_inlong_audit'''#g' application.properties
  sed -i 's/spring.datasource.druid.username=.*/'''spring.datasource.druid.username=${spring_datasource_username}'''/g' application.properties
  sed -i 's/spring.datasource.druid.password=.*/'''spring.datasource.druid.password=${spring_datasource_password}'''/g' application.properties
  if [ $mq_type == "pulsar" ]; then
    sed -i 's#pulsar://.*#'''${pulsar_service_url}'''#g' audit-proxy-pulsar.conf
    sed -i 's#pulsar://.*#'''${pulsar_service_url}'''#g' application.properties
  fi
  if [ $mq_type == "tubemq" ]; then
    sed -i 's/agent1.sinks.tube-sink-msg1.master-host-port-list=.*/'''agent1.sinks.tube-sink-msg1.master-host-port-list=${tube_master_url}'''/g' audit-proxy-tube.conf
    sed -i 's/agent1.sinks.tube-sink-msg2.master-host-port-list=.*/'''agent1.sinks.tube-sink-msg2.master-host-port-list=${tube_master_url}'''/g' audit-proxy-tube.conf
    sed -i 's/audit.tube.masterlist=.*/'''audit.tube.masterlist=${tube_master_url}'''/g' application.properties
  fi
}

init_inlong_dataproxy() {
  echo "Init dataproxy configuration parameters"
  cd $INLONG_HOME/inlong-dataproxy/conf
  sed -i 's/manager.hosts=.*/'''manager.hosts=${manager_server_hostname}:${manager_server_port}'''/g' common.properties
  sed -i 's/audit.proxys=.*/'''audit.proxys=${audit_proxys_ip}:${audit_proxys_port}'''/g' common.properties
}

init_inlong_manager() {
  echo "Init inlong manager configuration"
  cd $INLONG_HOME/inlong-manager/conf
  sed -i 's/spring.profiles.active=.*/'''spring.profiles.active=${spring_profiles_active}'''/g' application.properties
  sed -i 's/server.port=.*/'''server.port=${manager_server_port}'''/g' application.properties
  if [ $spring_profiles_active == "dev" ]; then
    sed -i 's#jdbc:mysql://.*apache_inlong_manager#'''jdbc:mysql://${spring_datasource_hostname}:${spring_datasource_port}/apache_inlong_manager'''#g' application-dev.properties
    sed -i 's/spring.datasource.druid.username=.*/'''spring.datasource.druid.username=${spring_datasource_username}'''/g' application-dev.properties
    sed -i 's/spring.datasource.druid.password=.*/'''spring.datasource.druid.password=${spring_datasource_password}'''/g' application-dev.properties
  fi
  if [ $spring_profiles_active == "prod" ]; then
    sed -i 's#jdbc:mysql://.*apache_inlong_manager#'''jdbc:mysql://${spring_datasource_hostname}:${spring_datasource_port}/apache_inlong_manager'''#g' application-prod.properties
    sed -i 's/spring.datasource.druid.username=.*/'''spring.datasource.druid.username=${spring_datasource_username}'''/g' application-prod.properties
    sed -i 's/spring.datasource.druid.password=.*/'''spring.datasource.druid.password=${spring_datasource_password}'''/g' application-prod.properties
  fi
}

if [ $# -eq 0 ]; then
  init_inlong_agent
  init_inlong_audit
  init_inlong_dataproxy
  init_inlong_manager
else
  init_inlong_$1
fi
