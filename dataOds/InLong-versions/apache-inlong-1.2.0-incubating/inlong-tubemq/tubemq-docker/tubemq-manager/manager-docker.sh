#!/bin/sh
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
#

file_path=$(cd "$(dirname "$0")";pwd)
# config
cat <<EOF > ${file_path}/../conf/application.properties
spring.jpa.hibernate.ddl-auto=update
spring.mvc.pathmatch.matching-strategy = ANT_PATH_MATCHER
# configuration for admin
topic.config.schedule=0/5 * * * * ?
broker.reload.schedule=0/5 * * * * ?
# mysql configuration for manager
spring.datasource.url=jdbc:mysql://$MYSQL_HOST:$MYSQL_PORT/apache_inlong_tubemq?useSSL=false
spring.datasource.username=$MYSQL_USER
spring.datasource.password=$MYSQL_PASSWD
# server port
server.port=8089
EOF
# start
sh ${file_path}/start-manager.sh
# init cluster
until $(curl --output /dev/null --silent --head --fail http://localhost:8089); do
    sleep 3
done
curl --header "Content-Type: application/json" --request POST --data \
'{"masterIp":"'"$TUBE_MASTER_IP"'","clusterName":"inlong","masterPort":"'"$TUBE_MASTER_PORT"'","masterWebPort":"'"$TUBE_MASTER_WEB_PORT"'","createUser":"manager","token":"'"$TUBE_MASTER_TOKEN"'"}' \
http://localhost:8089/v1/cluster?method=add

# keep alive
tail -F ${file_path}/../logs/info.log
