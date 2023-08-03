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

INSTALL_BASE=$PWD

MOD_NAME="ranger-usersync"

INSTALL_DIR=${INSTALL_BASE}

curDt=`date '+%Y%m%d%H%M%S'`
LOGFILE=setup.log.$curDt

log() {
   local prefix="[$(date +%Y/%m/%d\ %H:%M:%S)]: "
   echo "${prefix} $@" >> $LOGFILE
   echo "${prefix} $@"
}

# Ensure that the user is root
MY_ID=`id -u`
if [ "${MY_ID}" -ne 0 ]
then
  echo "ERROR: You must run this script as root user."
  exit 1
fi

# Ensure JAVA_HOME is set
if [ "${JAVA_HOME}" == "" ]
then
  echo "ERROR: JAVA_HOME environment property not defined, aborting installation"
  exit 2
fi

# Grep configuration properties from install.properties
cdir=`dirname $0`

check_ret_status(){
	if [ $1 -ne 0 ]; then
		log "[E] $2";
		exit 1;
	fi
}

get_prop(){
	validateProperty=$(sed '/^\#/d' $2 | grep "^$1\s*="  | tail -n 1) # for validation
	if  test -z "$validateProperty" ; then log "[E] '$1' not found in $2 file while getting....!!"; exit 1; fi
	value=$(echo $validateProperty | cut -d "=" -f2-)
	if [[ $1 == *password* ]]
        then
                echo $value
        else
                echo $value | tr -d \'\"
        fi
}

PROPFILE=$PWD/install.properties
if [ ! -f "${PROPFILE}" ]; then
    echo "$PROPFILE file not found....!!"
    exit 1;
fi

unix_user=$(get_prop 'unix_user' $PROPFILE)
unix_group=$(get_prop 'unix_group' $PROPFILE)

SYNC_LDAP_BIND_KEYSTOREPATH=`grep '^[ \t]*CRED_KEYSTORE_FILENAME[ \t]*=' ${cdir}/install.properties | sed -e 's:^[ \t]*CRED_KEYSTORE_FILENAME[ \t]*=[ \t]*::'`

# END Grep configuration properties from install.properties
# Store POLICY_MGR user password in credential store
SYNC_POLICY_MGR_ALIAS="policymgr.user.password"
SYNC_POLICY_MGR_PASSWORD="rangerusersync"
SYNC_POLICY_MGR_USERNAME="rangerusersync"
count=0
while :
do
	if [ $count -gt 2 ]
	then
		log "[E] Unable to continue as correct input is not provided in 3 attempts."
		exit 1
	fi
	printf "Please enter policymgr username: "
	read SYNC_POLICY_MGR_USERNAME
	if [[ "${SYNC_POLICY_MGR_USERNAME}" != "" ]]
	then
		break;
	fi
done
while :
do
	if [ $count -gt 2 ]
	then
		log "[E] Unable to continue as correct input is not provided in 3 attempts."
		exit 1
	fi
	printf "Please enter policymgr password: "
	stty -echo
	read SYNC_POLICY_MGR_PASSWORD
	stty echo
	if [[ "${SYNC_POLICY_MGR_PASSWORD}" != "" ]]
	then
		break;
	fi
done
if [[ "${SYNC_POLICY_MGR_ALIAS}" != ""  && "${SYNC_LDAP_BIND_KEYSTOREPATH}" != "" &&  "${SYNC_POLICY_MGR_PASSWORD}" != ""  &&  "${SYNC_POLICY_MGR_USERNAME}" != "" ]]
then
        log "[I] Storing policymgr usersync password in credential store"
        mkdir -p `dirname "${SYNC_LDAP_BIND_KEYSTOREPATH}"`
        chown ${unix_user}:${unix_group} `dirname "${SYNC_LDAP_BIND_KEYSTOREPATH}"`
        $JAVA_HOME/bin/java -cp "lib/*" org.apache.ranger.credentialapi.buildks create "$SYNC_POLICY_MGR_ALIAS" -value "$SYNC_POLICY_MGR_PASSWORD" -provider jceks://file$SYNC_LDAP_BIND_KEYSTOREPATH
fi

# Create $INSTALL_DIR/conf/unixauthservice.properties

CFG_FILE="${cdir}/conf/unixauthservice.properties"
NEW_CFG_FILE=${cdir}/conf/unixauthservice.properties.tmp

if [ -f  ${CFG_FILE}  ]
then
    sed \
    -e "s|^\( *userSync.policyMgrUserName *=\).*|\1 ${SYNC_POLICY_MGR_USERNAME}|" \
	-e "s|^\( *userSync.policyMgrKeystore *=\).*|\1 ${SYNC_LDAP_BIND_KEYSTOREPATH}|" \
	-e "s|^\( *userSync.policyMgrAlias *=\).*|\1 ${SYNC_POLICY_MGR_ALIAS}|" \
	${CFG_FILE} > ${NEW_CFG_FILE}

    echo "<${logdir}> ${CFG_FILE} > ${NEW_CFG_FILE}"
else
    echo "ERROR: Required file, not found: ${CFG_FILE}, Aborting installation"
    exit 8
fi

mv ${cdir}/conf/unixauthservice.properties ${cdir}/conf/unixauthservice.properties.${curDt}
mv ${cdir}/conf/unixauthservice.properties.tmp ${cdir}/conf/unixauthservice.properties

#END Create $INSTALL_DIR/conf/unixauthservice.properties