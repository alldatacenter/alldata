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

ret=`hadoop credential create ${alias} -value ${pass} -provider jceks://file${jceksFile} 2>&1`
res=`echo $ret | grep 'already exist'`

if ! [ "${res}" == "" ]
then
   echo "Credential file already exists,recreating the file..."
   hadoop credential delete ${alias} -provider jceks://file${jceksFile}
   hadoop credential create ${alias} -value ${pass} -provider jceks://file${jceksFile}
fi
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

hive_dir=/usr/hdp/current/hive
hive_lib_dir=${hive_dir}/lib
hive_conf_dir=/etc/hive/conf
hive_srv_conf_dir=/etc/hive/conf.server
hive_bin_dir=${hive_dir}/bin


if [ -d ${hive_srv_conf_dir} -o -L ${hive_srv_conf_dir} ]
then
	hive_cli_conf_dir=${hive_conf_dir}
	hive_conf_dir=${hive_srv_conf_dir}
else
	hive_srv_conf_dir=${hive_conf_dir}
	hive_cli_conf_dir="${hive_conf_dir}"
fi

hdp_dir=/usr/hdp/current/hadoop
hdp_lib_dir=${hdp_dir}/lib
hdp_conf_dir=/etc/hadoop/conf

export CONFIG_FILE_OWNER="hive:hadoop"


if [ ! -d "${hdp_dir}" ]
then
	echo "ERROR: Invalid HADOOP HOME Directory: [${hdp_dir}]. Exiting ..."
	exit 1
fi

#echo "Hadoop Configuration Path: ${hdp_conf_dir}"

if [ ! -f ${hdp_conf_dir}/hadoop-env.sh ]
then
	echo "ERROR: Invalid HADOOP CONF Directory: [${hdp_conf_dir}]."
	echo "ERROR: Unable to locate: hadoop-env.sh. Exiting ..."
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
	echo "[E] SQL CONNECTOR FILE : $SQL_CONNECTOR_JAR does not exists" ; exit 1;
fi
#copying sql connector jar file to lib directory
cp $SQL_CONNECTOR_JAR ${install_dir}/lib

#
# --- Backup current configuration for backup - START
#

COMPONENT_NAME=hive

XASECURE_VERSION=`cat ${install_dir}/version`

CFG_DIR=${hive_conf_dir}
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

#
# --- Backup current configuration for backup  - END
#


dt=`date '+%Y%m%d%H%M%S'`
for f in ${install_dir}/conf/*
do
	if [ -f ${f} ]
	then
		fn=`basename $f`
		if [ ! -f ${hive_conf_dir}/${fn} ]
		then
			echo "+cp ${f} ${hive_conf_dir}/${fn}"
			cp ${f} ${hive_conf_dir}/${fn}
		else
			echo "WARN: ${fn} already exists in the ${hive_conf_dir} - Using existing configuration ${fn}"
		fi
	fi
done


#echo "Hadoop XASecure Library Path: ${hdp_lib_dir}"

if [ ! -d ${hive_lib_dir} ]
then
	echo "+mkdir -p ${hive_lib_dir}"
	mkdir -p ${hive_lib_dir}
fi

for f in ${install_dir}/dist/*.jar ${install_dir}/lib/*.jar
do
	if [ -f ${f} ]
	then
		fn=`basename $f`
		echo "+cp ${f} ${hive_lib_dir}/${fn}"
		cp ${f} ${hive_lib_dir}/${fn}
	fi
done

#
# Copy the SSL parameters
#

CredFile=`grep '^CREDENTIAL_PROVIDER_FILE' ${install_dir}/install.properties | awk -F= '{ print $2 }'`

if ! [ `echo ${CredFile} | grep '^/.*'` ]
then
  echo "ERROR:Please enter the Credential File Store with proper file path"
  exit 1
fi

dirno=`echo ${CredFile}| awk -F"/" '{ print NF}'`

if [ ${dirno} -gt 2 ];
then
 pardir=`echo ${CredFile} |  awk -F'/[^/]*$' '{ print $1 }'`
 if [ ! -d  ${pardir} ];
 then
   mkdir -p ${pardir}
   if [ $? -eq 0 ];
   then
     chmod go+rx ${pardir}
   else
     echo "ERROR: Unable to create credential store file path"
   fi
 fi
fi

#
# Generate Credential Provider file and Credential for SSL KEYSTORE AND TRUSTSTORE
#

sslkeystoreAlias="sslKeyStore"

sslkeystoreCred=`grep '^SSL_KEYSTORE_PASSWORD' ${install_dir}/install.properties | awk -F= '{ print $2 }'`

create_jceks ${sslkeystoreAlias} ${sslkeystoreCred} ${CredFile}


ssltruststoreAlias="sslTrustStore"

ssltruststoreCred=`grep '^SSL_TRUSTSTORE_PASSWORD' ${install_dir}/install.properties | awk -F= '{ print $2 }'`

create_jceks ${ssltruststoreAlias} ${ssltruststoreCred} ${CredFile}

chmod go+rx ${pardir}
chmod go+r ${CredFile}
chown ${CONFIG_FILE_OWNER} ${CredFile} 

PROP_ARGS="-p  ${install_dir}/install.properties"

to_file="${install_dir}/install.properties"
for f in ${install_dir}/installer/conf/*-changes.cfg
do
        if [ -f ${f} ]
        then
                fn=`basename $f`
                orgfn=`echo $fn | sed -e 's:-changes.cfg:.xml:'`
                fullpathorgfn="${hive_conf_dir}/${orgfn}"

                if [ ! -f ${fullpathorgfn} -a -f ${install_dir}/template/configuration.xml ]
                then
                        cp ${install_dir}/template/configuration.xml ${fullpathorgfn}
                fi

                if [ ! -f ${fullpathorgfn} ]
                then
                        echo "ERROR: Unable to find ${fullpathorgfn}"
                        exit 1
                fi
                archivefn="${hive_conf_dir}/.${orgfn}.${dt}"
                newfn="${hive_conf_dir}/.${orgfn}-new.${dt}"
                cp ${fullpathorgfn} ${archivefn}
                if [ $? -eq 0 ]
                then
                	cp="${install_dir}/installer/lib/*:${hdp_dir}/*:${hdp_lib_dir}/*"
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

chmod go-rwx ${hive_conf_dir}/xasecure-policymgr-ssl.xml
chown ${CONFIG_FILE_OWNER} ${hive_conf_dir}/xasecure-policymgr-ssl.xml

exit 0
