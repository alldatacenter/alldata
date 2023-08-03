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
# Replacing authorizer to storm.yaml configuration file ...
#
STORM_DIR=/etc/storm
STORM_CONFIG_FILE=storm.yaml

dt=`date '+%Y%m%d%H%M%S'`
CONFIG_FILE=${STORM_DIR}/${STORM_CONFIG_FILE}
ARCHIVE_FILE=${STORM_DIR}/.${STORM_CONFIG_FILE}.${dt}

cp ${CONFIG_FILE} ${ARCHIVE_FILE}

awk -F: 'BEGIN {
	configured = 0 ;
}
{ 
	if ($1 == "nimbus.authorizer") {
		if ($2 ~ /^[ \t]*"org.apache.storm.security.auth.authorizer.SimpleACLAuthorizer"[ \t]*$/) {
			configured = 1 ;
			printf("%s\n",$0) ;
		}
		else {
			printf("#%s\n",$0);
			printf("nimbus.authorizer: \"org.apache.storm.security.auth.authorizer.SimpleACLAuthorizer\"\n") ;
			configured = 1 ;
		}
	}
	else {
		printf("%s\n",$0) ;
	}
}
END {
	if (configured == 0) {
		printf("nimbus.authorizer: \"org.apache.storm.security.auth.authorizer.SimpleACLAuthorizer\"\n") ;
	}
}' ${ARCHIVE_FILE} > ${ARCHIVE_FILE}.new 

if [ ! -z ${ARCHIVE_FILE}.new ] 
then
	cat ${ARCHIVE_FILE}.new > ${CONFIG_FILE}
	rm -f ${ARCHIVE_FILE}.new
	echo "Apache Ranger Plugin has been uninstalled from Storm Service. Please restart Storm nimbus and ui services ..."
else
	echo "ERROR: ${ARCHIVE_FILE}.new file has not created successfully."
	exit 1
fi

exit 0
