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
#	echo "Usage: $0 <db_root_password> [db_host]"
#	exit 1
#fi
#
#db_root_password=$1
#db_host="localhost"
#if [ "$2" != "" ]; then
#    db_host="$2"
#fi

db_user=xaadmin
db_password=xaadmin
db_file=xa_core_db.sql
db_core_file=xa_core_db.sql
db_name=xa_db

MYSQL_BIN='mysql'
MYSQL_HOST='localhost'

mysqlexec="${MYSQL_BIN} -u ${db_user} --password=${db_password} -h ${MYSQL_HOST} ${db_name}"


#echo "Importing database file $db_file ...  "
#set -x
#mysql -u $db_user  --password=$db_password < $db_file


log() {  
   local prefix="[$(date +%Y/%m/%d\ %H:%M:%S)]: "
   echo "${prefix} $@" 
} 

check_ret_status(){
  if [ $1 -ne 0 ]; then
    log "[E] $2"; 
    exit 1; 
  fi
}

import_db () {

  log "[I] Verifying Database: $db_name";
  existdb=`${MYSQL_BIN} -u ${db_user} --password=$db_password -h $MYSQL_HOST -B --skip-column-names -e  "show databases like '${db_name}' ;"`

  if [ "${existdb}" = "${db_name}" ]
  then
    log "[I] - database ${db_name} already exists. deleting ..."
    $MYSQL_BIN -u $db_user --password=$db_password -h $MYSQL_HOST -e "drop database $db_name"  

  fi

    log "[I] Creating Database: $db_name";
    $MYSQL_BIN -u $db_user --password=$db_password -h $MYSQL_HOST -e "create database $db_name"  
    check_ret_status $? "Creating database Failed.."
  
  
    log "[I] Importing Core Database file: $db_core_file "
      $MYSQL_BIN -u $db_user --password=$db_password -h $MYSQL_HOST $db_name < $db_core_file
      check_ret_status $? "Importing Database Failed.."
  
    log "[I] Importing Database file : $db_core_file DONE";
}

run_patches(){
  log "[I] - starting upgradedb ... "

  DBVERSION_CATALOG_CREATION=create_dbversion_catalog.sql

  mysqlexec="${MYSQL_BIN} -u ${db_user} --password=${db_password} -h ${MYSQL_HOST} ${db_name}"
  
  if [ -f ${DBVERSION_CATALOG_CREATION} ]
  then
    log "[I] Verifying database version catalog table .... "
    ${mysqlexec} < ${DBVERSION_CATALOG_CREATION} 
  fi
    
  dt=`date '+%s'`
  tempFile=/tmp/sql_${dt}_$$.sql
  sqlfiles=`ls -1 patches/*.sql 2> /dev/null | awk -F/ '{ print $NF }' | awk -F- '{ print $1, $0 }' | sort -k1 -n | awk '{ printf("patches/%s\n",$2) ; }'`
  for sql in ${sqlfiles}
  do
    if [ -f ${sql} ]
    then
      bn=`basename ${sql}`
      version=`echo ${bn} | awk -F'-' '{ print $1 }'`
      if [ "${version}" != "" ]
      then
        c=`${mysqlexec} -B --skip-column-names -e "select count(id) from x_db_version_h where version = '${version}' and active = 'Y'"`
        check_ret_status $? "DBVerionCheck - ${version} Failed."
        if [ ${c} -eq 0 ]
        then
          cat ${sql} > ${tempFile}
          echo >> ${tempFile}
          echo "insert into x_db_version_h (version, inst_at, inst_by, updated_at, updated_by) values ( '${version}', now(), user(), now(), user()) ;" >> ${tempFile}
          log "[I] - patch [${version}] is being applied."
          ${mysqlexec} < ${tempFile}
          check_ret_status $? "Update patch - ${version} Failed. See sql file : [${tempFile}]"
          rm -f ${tempFile}
        else
          log "[I] - patch [${version}] is already applied. Skipping ..."
        fi
      fi
    fi
  done
}


import_db
run_patches   






