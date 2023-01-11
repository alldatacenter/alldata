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

#
# Script to reset mysql database
#

#if [ $# -lt 1 ]; then
#       echo "Usage: $0 <db_root_password> [db_host] <db_name>"
#       exit 1
#fi
#
#db_root_password=$1
#db_host="localhost"
#if [ "$2" != "" ]; then
#    db_host="$2"
#fi
#db_name="xa_logger"
#if [ "$3" != "" ]; then
#    db_name="$3"
#fi

audit_db_user=xaadmin
audit_db_password=xaadmin
audit_db_file=xa_audit_db.sql
audit_db_name=xa_logger
echo "Importing database file $audit_db_file.sql ...  "
set -x
mysql -u $audit_db_user  --password=$audit_db_password  -e "create database IF NOT EXISTS $audit_db_name"
mysql -u $audit_db_user  --password=$audit_db_password --database=$audit_db_name < $audit_db_file
