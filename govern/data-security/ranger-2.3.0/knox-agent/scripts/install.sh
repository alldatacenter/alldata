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


function create_jceks()
{
       alias=$1
       pass=$2
       jceksFile=$3
       java -cp "${install_dir}/cred/lib/*" org.apache.ranger.credentialapi.buildks create ${alias} -value ${pass} -provider jceks://file${jceksFile}
}

#Update Properties to File
#$1 -> propertyName $2 -> newPropertyValue $3 -> fileName
updatePropertyToFile(){
	sed -i 's@^'$1'=[^ ]*$@'$1'='$2'@g' $3
	#validate=`sed -i 's/^'$1'=[^ ]*$/'$1'='$2'/g' $3`	#for validation
	validate=$(sed '/^\#/d' $3 | grep "^$1"  | tail -n 1 | cut -d "=" -f2-) # for validation
	#echo 'V1:'$validate
	if test -z "$validate" ; then echo "[E] '$1' not found in $3 file while Updating....!!"; exit 1; fi
	echo "[I] File $3 Updated successfully : {'$1'}"
}


MY_ID=`id -u`

if [ "${MY_ID}" -ne 0 ]
then
  echo "ERROR: You must run the installation as root user."
  exit 1
fi

install_dir=`dirname $0`

[ "${install_dir}" = "." ] && install_dir=`pwd`

#echo "Current Install Directory: [${install_dir}]"

#verify sql-connector path is valid
SQL_CONNECTOR_JAR=`grep '^SQL_CONNECTOR_JAR'  ${install_dir}/install.properties | awk -F= '{ print $2 }'`
echo "[I] Checking SQL CONNECTOR FILE : $SQL_CONNECTOR_JAR"
if test -f "$SQL_CONNECTOR_JAR"; then
	echo "[I] SQL CONNECTOR FILE : $SQL_CONNECTOR_JAR file found"
else
	echo "[E] SQL CONNECTOR FILE : $SQL_CONNECTOR_JAR not found, aborting installation"
  exit 1
fi
#copying sql connector jar file to lib directory
cp $SQL_CONNECTOR_JAR ${install_dir}/lib

KNOX_HOME=`grep 'KNOX_HOME'  ${install_dir}/install.properties | awk -F= '{ print $2 }'`
if [ "${KNOX_HOME}" == "" ]
then
  echo "ERROR: KNOX_HOME property not defined, aborting installation"
  exit 1
fi

if [ ! -d ${KNOX_HOME} ]
then
  echo "ERROR: directory ${KNOX_HOME} does not exist"
  exit 1
fi

KNOX_EXT=${KNOX_HOME}/ext
if [ ! -d ${KNOX_EXT} ]
then
  echo "ERROR: Knox ext directory ${KNOX_EXT} does not exist"
  exit 1
fi

KNOX_CONF=${KNOX_HOME}/conf
if [ ! -d ${KNOX_CONF} ]
then
  echo "ERROR: Knox conf directory ${KNOX_CONF} does not exist"
  exit 1
fi

# copy lib, dist jar files in to KNOX_EXT
echo "Copying knox agent lib, dist jars to ${KNOX_EXT}"
cp lib/*.jar ${KNOX_EXT}
cp dist/*.jar ${KNOX_EXT}

# copy sql connector jar  in to KNOX_EXT
echo "Copying db connector jar to ${KNOX_EXT}"
cp ${SQL_CONNECTOR_JAR} ${KNOX_EXT}

CONFIG_FILE_OWNER="knox:hadoop"

# --- Backup current configuration for backup - START

COMPONENT_NAME=knox

XASECURE_VERSION=`cat ${install_dir}/version`

CFG_DIR=${KNOX_CONF}
XASECURE_ROOT=/etc/xasecure/${COMPONENT_NAME}
BACKUP_TYPE=pre
CUR_VERSION_FILE=${XASECURE_ROOT}/.current_version
CUR_CFG_DIR_FILE=${XASECURE_ROOT}/.config_dir
PRE_INSTALL_CONFIG=${XASECURE_ROOT}/${BACKUP_TYPE}-${XASECURE_VERSION}

backup_dt=`date '+%Y%m%d%H%M%S'`

if [ -d "${PRE_INSTALL_CONFIG}" ]
then
	PRE_INSTALL_CONFIG="${PRE_INSTALL_CONFIG}.${backup_dt}"
fi

# back up prior config back up
if [ -d ${CFG_DIR} ]
then
	( cd ${CFG_DIR} ; find . -print | cpio -pdm ${PRE_INSTALL_CONFIG} )
	[ -f ${CUR_VERSION_FILE} ] && mv ${CUR_VERSION_FILE} ${CUR_VERSION_FILE}-${backup_dt}
	echo ${XASECURE_VERSION} > ${CUR_VERSION_FILE}
	echo ${CFG_DIR} > ${CUR_CFG_DIR_FILE}
else
	echo "ERROR: Unable to find configuration directory: [${CFG_DIR}]"
	exit 1
fi

cp -f ${install_dir}/uninstall.sh ${XASECURE_ROOT}/

# --- Backup current configuration for backup  - END



dt=`date '+%Y%m%d%H%M%S'`
for f in ${install_dir}/conf/*
do
	if [ -f ${f} ]
	then
		fn=`basename $f`
		if [ ! -f ${KNOX_CONF}/${fn} ]
		then
			echo "+cp ${f} ${KNOX_CONF}/${fn}"
			cp ${f} ${KNOX_CONF}/${fn}
		else
			echo "WARN: ${fn} already exists in the ${KNOX_CONF} - Using existing configuration ${fn}"
		fi
	fi
done


# create new config files based on *-changes.cfg files

PROP_ARGS="-p  ${install_dir}/install.properties"

for f in ${install_dir}/installer/conf/*-changes.cfg
do
	if [ -f ${f} ]
	then
		fn=`basename $f`
		orgfn=`echo $fn | sed -e 's:-changes.cfg:.xml:'`
		fullpathorgfn="${KNOX_CONF}/${orgfn}"
		if [ ! -f ${fullpathorgfn} ]
		then
			echo "ERROR: Unable to find ${fullpathorgfn}"
			exit 1
		fi
		archivefn="${KNOX_CONF}/.${orgfn}.${dt}"
		newfn="${KNOX_CONF}/.${orgfn}-new.${dt}"
		cp ${fullpathorgfn} ${archivefn}
		if [ $? -eq 0 ]
		then
			cp="${install_dir}/cred/lib/*:${install_dir}/installer/lib/*:/usr/lib/hadoop/*:/usr/lib/hadoop/lib/*"
			java -cp "${cp}" org.apache.ranger.utils.install.XmlConfigChanger -i ${archivefn} -o ${newfn} -c ${f} ${PROP_ARGS}
			if [ $? -eq 0 ]
			then
				diff -w ${newfn} ${fullpathorgfn} > /dev/null 2>&1
				if [ $? -ne 0 ]
				then
					#echo "Changing config file:  ${fullpathorgfn} with following changes:"
					#echo "==============================================================="
					#diff -w ${newfn} ${fullpathorgfn}
					#echo "==============================================================="
					echo "NOTE: Current config file: ${fullpathorgfn} is being saved as ${archivefn}"
					#echo "==============================================================="
					cp ${newfn} ${fullpathorgfn}
				fi
			else
				echo "ERROR: Unable to make changes to config. file: ${fullpathorgfn}"
				echo "exiting ...."
				exit 1
			fi
			else
			echo "ERROR: Unable to save config. file: ${fullpathorgfn}  to ${archivefn}"
			echo "exiting ...."
			exit 1
		fi
	fi
done

chmod go-rwx ${KNOX_CONF}/xasecure-policymgr-ssl.xml
chown ${CONFIG_FILE_OWNER} ${KNOX_CONF}/xasecure-policymgr-ssl.xml

#
# -- Cred Changes
#

CredFile=`grep '^CREDENTIAL_PROVIDER_FILE' ${install_dir}/install.properties | awk -F= '{ print $2 }'`

if ! [ `echo ${CredFile} | grep '^/.*'` ]
then
  echo "Please enter the Credential File Store with proper file path"
  exit 1
fi

#
# Generate Credential Provider file and Credential for SSL KEYSTORE AND TRUSTSTORE
#
sslKeystorePasswordAlias="sslKeyStorePassword"

sslKeystorePassword=`grep '^SSL_KEYSTORE_PASSWORD' ${install_dir}/install.properties | awk -F= '{ print $2 }'`

create_jceks ${sslKeystorePasswordAlias} ${sslKeystorePassword} ${CredFile}


sslTruststorePasswordAlias="sslTrustStorePassword"

sslTruststorePassword=`grep '^SSL_TRUSTSTORE_PASSWORD' ${install_dir}/install.properties | awk -F= '{ print $2 }'`

create_jceks ${sslTruststorePasswordAlias} ${sslTruststorePassword} ${CredFile}

chown ${CONFIG_FILE_OWNER} ${CredFile} 

#
# -- End - Cred Changes
#


# update topology files - replace <name>AclsAuthz</name> with <name>XASecurePDPKnox</name>
# ${PRE_INSTALL_CONFIG}/topologies/topologies/*.xml
for fn in `ls ${PRE_INSTALL_CONFIG}/topologies/*.xml`
do
  tn=`basename ${fn}`
  echo "Updating topology file ${KNOX_CONF}/topologies/${tn}"
  cat $fn | sed -e 's-<name>AclsAuthz</name>-<name>XASecurePDPKnox</name>-' > ${KNOX_CONF}/topologies/$tn
done

echo "Restarting Knox"
su -l knox ${KNOX_HOME}/bin/gateway.sh stop
su -l knox ${KNOX_HOME}/bin/gateway.sh start

exit 0
