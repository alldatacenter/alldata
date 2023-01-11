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

COMPONENT_NAME=hadoop
CFG_DIR=/etc/${COMPONENT_NAME}/conf
XASECURE_ROOT=/etc/xasecure/${COMPONENT_NAME}
BACKUP_TYPE=pre
CUR_VERSION_FILE=${XASECURE_ROOT}/.current_version
if [ -f ${CUR_VERSION_FILE} ]
then
	XASECURE_VERSION=`cat ${CUR_VERSION_FILE}`
	PRE_INSTALL_CONFIG=${XASECURE_ROOT}/${BACKUP_TYPE}-${XASECURE_VERSION}
	dt=`date '+%Y%m%d%H%M%S'`
	if [ -d "${PRE_INSTALL_CONFIG}" ]
	then
		[ -d ${CFG_DIR} ] && mv ${CFG_DIR} ${CFG_DIR}-${dt}
		( cd ${PRE_INSTALL_CONFIG} ; find . -print | cpio -pdm ${CFG_DIR} )
		[ -f ${CUR_VERSION_FILE} ] && mv ${CUR_VERSION_FILE} ${CUR_VERSION_FILE}-uninstalled-${dt}
		echo "XASecure version - ${XASECURE_VERSION} has been uninstalled successfully."
	else
		echo "ERROR: Unable to find pre-install configuration directory: [${PRE_INSTALL_CONFIG}]"
		exit 1
	fi
else
	cd ${CFG_DIR}
	saved_files=`find . -type f -name '.*' |  sort | grep -v -- '-new.' | grep '[0-9]*$' | grep -v -- '-[0-9]*$' | sed -e 's:\.[0-9]*$::' | sed -e 's:^./::' | sort -u`
	dt=`date '+%Y%m%d%H%M%S'`
	if [ "${saved_files}" != "" ]
	then
	        for f in ${saved_files}
	        do
	                oldf=`ls ${f}.[0-9]* | sort | head -1`
	                if [ -f "${oldf}" ]
	                then
	                        nf=`echo ${f} | sed -e 's:^\.::'`
	                        if [ -f "${nf}" ]
	                        then
	                                echo "+cp -p ${nf} .${nf}-${dt}"
	                                cp -p ${nf} .${nf}-${dt}
	                                echo "+cp ${oldf} ${nf}"
	                                cp ${oldf} ${nf}
	                        else
	                                echo "ERROR: ${nf} not found to save. However, old file is being recovered."
	                                echo "+cp -p ${oldf} ${nf}"
	                                cp -p ${oldf} ${nf}
	                        fi
	                fi
	        done
	        echo "XASecure configuration has been uninstalled successfully."
	fi
fi
