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

if [ $# -ne 3 ]; then
	echo "Usage: $0 <db_user> <db_password> <db_database> [db_host]"
	exit 1
fi

db_user=$1
db_password=$2
db_database=$3

#db_user=cignifi
#db_password=cignifi
#db_database=cignifi_dev


set -x
#First drop the database and recreate i
echo "y" | mysqladmin -u $db_user -p$db_password drop $db_database
mysqladmin -u $db_user -p$db_password create $db_database

#Create the schema
mysql -u $db_user -p$db_password $db_database < schema_mysql.sql

#Add seed users
mysql -u $db_user -p$db_password $db_database < mysql_seed_data.sql

