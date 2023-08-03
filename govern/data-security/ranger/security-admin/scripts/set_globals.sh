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

#If it is a manual install, then it is recommended to run this for every install/upgrade, before the setup.sh is called
#This script will create the appropriate soft links for folders and files
#This script will not override existing configuration or log files.
#This script creates the required folders in /etc/ranger, /var/log/ranger and other folders.
#This will also create the ranger linux user and groups if required.

#This script needs to be run as root
PROPFILE=$PWD/install.properties
propertyValue=''

if [ ! $? = "0" ];then
	log "$PROPFILE file not found....!!";
	exit 1;
fi
get_prop(){
	validateProperty=$(sed '/^\#/d' $2 | grep "^$1\s*="  | tail -n 1) # for validation
	if  test -z "$validateProperty" ; then log "[E] '$1' not found in $2 file while getting....!!"; exit 1; fi
	value=$(echo $validateProperty | cut -d "=" -f2-)
	echo $value
}

if [ ! -w /etc/passwd ]; then
	echo "ERROR: Please run this script as root"
	exit 1
fi

#Go to the current build directory
cd `dirname $0`
if [ ! -d ews ]; then
	echo "ERROR: The script needs to be in the installed directory for this version"
	exit 1
fi

curDt=`date '+%Y%m%d%H%M%S'`
LOGFILE=set_globals.log.$curDt

log() {
	local prefix="[$(date +%Y/%m/%d\ %H:%M:%S)]: "
	echo "${prefix} $@" >> $LOGFILE
	echo "${prefix} $@"
}

#Create the ranger users and groups (if needed)
unix_user=$(get_prop 'unix_user' $PROPFILE)
unix_group=$(get_prop 'unix_group' $PROPFILE)

#create group if it does not exist
egrep "^$unix_group" /etc/group >& /dev/null
if [ $? -ne 0 ]; then
	groupadd ${unix_group}
	ret=$?
	if [ $ret -ne 0 ] && [ $ret -ne 9 ]; then
		echo "Error creating group $unix_group"
		exit 1
	fi
fi
#create user if it does not exists
id -u ${unix_user} > /dev/null 2>&1
if [ $? -ne 0 ]; then
    useradd ${unix_user} -g ${unix_group} -m
else
	usermod -g ${unix_group} ${unix_user}
fi

chown -R $unix_user *


#Create etc conf folders
if [ ! -d /etc/ranger/admin/conf ]; then
	#Create the conf file /etc and copy either from package conf or conf.dist
	mkdir -p /etc/ranger/admin/conf
	if [ -d ews/webapp/WEB-INF/classes/conf ]; then
		#If conf already exists, then move it to /etc...
		cp -r ews/webapp/WEB-INF/classes/conf/* /etc/ranger/admin/conf
	else
		#Let's copy conf.dist to /etc/ranger/admin/conf
		cp -r ews/webapp/WEB-INF/classes/conf.dist/* /etc/ranger/admin/conf
	fi
	chmod 750 /etc/ranger/admin/conf
	chown -R $unix_user:$unix_group /etc/ranger/admin/conf
fi

log "[I] Soft linking /etc/ranger/admin/conf to ews/webapp/WEB-INF/classes/conf"
mv -f ews/webapp/WEB-INF/classes/conf ews/webapp/WEB-INF/classes/conf.$curDt 2> /dev/null
ln -sf /etc/ranger/admin/conf ews/webapp/WEB-INF/classes/conf

#Create the log folder
if [ ! -d /var/log/ranger/admin ]; then
	mkdir -p /var/log/ranger/admin
	if [ -d ews/logs ]; then
		if [ -n "$(ls ews/logs/ 2>/dev/null)" ]; then
			cp -r ews/logs/* /var/log/ranger/admin
		fi
	fi
fi

if [ -d /var/log/ranger/admin ]; then
	chown -R $unix_user:$unix_group /var/log/ranger
    chown -R $unix_user:$unix_group /var/log/ranger/admin
    chmod 755 /var/log/ranger
    chmod 755 /var/log/ranger/admin
fi

mv -f ews/logs ews/webapp/logs.$curDt 2> /dev/null
ln -sf /var/log/ranger/admin ews/logs
