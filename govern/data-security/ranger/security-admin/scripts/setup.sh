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
# -------------------------------------------------------------------------------------
#
# Ranger Admin Setup Script
#
# This script will install policymanager webapplication under tomcat and also, initialize the database with ranger users/tables.

usage() {
        [ "$*" ] && echo "$0: $*"
        sed -n '/^##/,/^$/s/^## \{0,1\}//p' "$0"
        exit 2
} 2>/dev/null

log() {
        local prefix="$(date +%Y-%m-%d\ %H:%M:%S,%3N) "
        echo "${prefix} $@" >> $LOGFILE
        echo "${prefix} $@"
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

PROPFILE=${RANGER_ADMIN_CONF:-$PWD}/install.properties
if [ ! -f "${PROPFILE}" ]; then
        echo "$PROPFILE file not found....!!"
        exit 1;
fi

LOGFILE=$(eval echo " $(get_prop 'LOGFILE' $PROPFILE)")

PYTHON_COMMAND_INVOKER=$(get_prop 'PYTHON_COMMAND_INVOKER' $PROPFILE)
DB_FLAVOR=$(get_prop 'DB_FLAVOR' $PROPFILE)
SQL_CONNECTOR_JAR=$(get_prop 'SQL_CONNECTOR_JAR' $PROPFILE)
db_root_user=$(get_prop 'db_root_user' $PROPFILE)
db_root_password=$(get_prop 'db_root_password' $PROPFILE)
db_host=$(get_prop 'db_host' $PROPFILE)
db_name=$(get_prop 'db_name' $PROPFILE)
db_user=$(get_prop 'db_user' $PROPFILE)
db_password=$(get_prop 'db_password' $PROPFILE)
db_ssl_enabled=$(get_prop 'db_ssl_enabled' $PROPFILE)
db_ssl_required=$(get_prop 'db_ssl_required' $PROPFILE)
db_ssl_verifyServerCertificate=$(get_prop 'db_ssl_verifyServerCertificate' $PROPFILE)
db_ssl_auth_type=$(get_prop 'db_ssl_auth_type' $PROPFILE)
db_ssl_certificate_file=$(get_prop 'db_ssl_certificate_file' $PROPFILE)
javax_net_ssl_trustStore_type=$(get_prop 'javax_net_ssl_trustStore_type' $PROPFILE)
javax_net_ssl_keyStore_type=$(get_prop 'javax_net_ssl_keyStore_type' $PROPFILE)
rangerAdmin_password=$(get_prop 'rangerAdmin_password' $PROPFILE)
rangerTagsync_password=$(get_prop 'rangerTagsync_password' $PROPFILE)
rangerUsersync_password=$(get_prop 'rangerUsersync_password' $PROPFILE)
keyadmin_password=$(get_prop 'keyadmin_password' $PROPFILE)
javax_net_ssl_keyStore=$(get_prop 'javax_net_ssl_keyStore' $PROPFILE)
javax_net_ssl_keyStorePassword=$(get_prop 'javax_net_ssl_keyStorePassword' $PROPFILE)
javax_net_ssl_trustStore=$(get_prop 'javax_net_ssl_trustStore' $PROPFILE)
javax_net_ssl_trustStorePassword=$(get_prop 'javax_net_ssl_trustStorePassword' $PROPFILE)
audit_store=$(get_prop 'audit_store' $PROPFILE)
audit_elasticsearch_urls=$(get_prop 'audit_elasticsearch_urls' $PROPFILE)
audit_elasticsearch_port=$(get_prop 'audit_elasticsearch_port' $PROPFILE)
audit_elasticsearch_user=$(get_prop 'audit_elasticsearch_user' $PROPFILE)
audit_elasticsearch_password=$(get_prop 'audit_elasticsearch_password' $PROPFILE)
audit_elasticsearch_index=$(get_prop 'audit_elasticsearch_index' $PROPFILE)
audit_elasticsearch_bootstrap_enabled=$(get_prop 'audit_elasticsearch_bootstrap_enabled' $PROPFILE)
audit_solr_urls=$(get_prop 'audit_solr_urls' $PROPFILE)
audit_solr_user=$(get_prop 'audit_solr_user' $PROPFILE)
audit_solr_password=$(get_prop 'audit_solr_password' $PROPFILE)
audit_solr_zookeepers=$(get_prop 'audit_solr_zookeepers' $PROPFILE)
audit_cloudwatch_region=$(get_prop 'audit_cloudwatch_region' $PROPFILE)
audit_cloudwatch_log_group=$(get_prop 'audit_cloudwatch_log_group' $PROPFILE)
audit_cloudwatch_log_stream_prefix=$(get_prop 'audit_cloudwatch_log_stream_prefix' $PROPFILE)
policymgr_external_url=$(get_prop 'policymgr_external_url' $PROPFILE)
policymgr_http_enabled=$(get_prop 'policymgr_http_enabled' $PROPFILE)
policymgr_https_keystore_file=$(get_prop 'policymgr_https_keystore_file' $PROPFILE)
policymgr_https_keystore_keyalias=$(get_prop 'policymgr_https_keystore_keyalias' $PROPFILE)
policymgr_https_keystore_password=$(get_prop 'policymgr_https_keystore_password' $PROPFILE)
policymgr_supportedcomponents=$(get_prop 'policymgr_supportedcomponents' $PROPFILE)
unix_user=$(get_prop 'unix_user' $PROPFILE)
unix_user_pwd=$(get_prop 'unix_user_pwd' $PROPFILE)
unix_group=$(get_prop 'unix_group' $PROPFILE)
authentication_method=$(get_prop 'authentication_method' $PROPFILE)
remoteLoginEnabled=$(get_prop 'remoteLoginEnabled' $PROPFILE)
authServiceHostName=$(get_prop 'authServiceHostName' $PROPFILE)
authServicePort=$(get_prop 'authServicePort' $PROPFILE)
ranger_unixauth_keystore=$(get_prop 'ranger_unixauth_keystore' $PROPFILE)
ranger_unixauth_keystore_password=$(get_prop 'ranger_unixauth_keystore_password' $PROPFILE)
ranger_unixauth_truststore=$(get_prop 'ranger_unixauth_truststore' $PROPFILE)
ranger_unixauth_truststore_password=$(get_prop 'ranger_unixauth_truststore_password' $PROPFILE)
xa_ldap_url=$(get_prop 'xa_ldap_url' $PROPFILE)
xa_ldap_userDNpattern=$(get_prop 'xa_ldap_userDNpattern' $PROPFILE)
xa_ldap_groupSearchBase=$(get_prop 'xa_ldap_groupSearchBase' $PROPFILE)
xa_ldap_groupSearchFilter=$(get_prop 'xa_ldap_groupSearchFilter' $PROPFILE)
xa_ldap_groupRoleAttribute=$(get_prop 'xa_ldap_groupRoleAttribute' $PROPFILE)
xa_ldap_base_dn=$(get_prop 'xa_ldap_base_dn' $PROPFILE)
xa_ldap_bind_dn=$(get_prop 'xa_ldap_bind_dn' $PROPFILE)
xa_ldap_bind_password=$(get_prop 'xa_ldap_bind_password' $PROPFILE)
xa_ldap_referral=$(get_prop 'xa_ldap_referral' $PROPFILE)
xa_ldap_userSearchFilter=$(get_prop 'xa_ldap_userSearchFilter' $PROPFILE)
xa_ldap_ad_domain=$(get_prop 'xa_ldap_ad_domain' $PROPFILE)
xa_ldap_ad_url=$(get_prop 'xa_ldap_ad_url' $PROPFILE)
xa_ldap_ad_base_dn=$(get_prop 'xa_ldap_ad_base_dn' $PROPFILE)
xa_ldap_ad_bind_dn=$(get_prop 'xa_ldap_ad_bind_dn' $PROPFILE)
xa_ldap_ad_bind_password=$(get_prop 'xa_ldap_ad_bind_password' $PROPFILE)
xa_ldap_ad_referral=$(get_prop 'xa_ldap_ad_referral' $PROPFILE)
xa_ldap_ad_userSearchFilter=$(get_prop 'xa_ldap_ad_userSearchFilter' $PROPFILE)
XAPOLICYMGR_DIR=$(eval echo "$(get_prop 'XAPOLICYMGR_DIR' $PROPFILE)")
app_home=$(eval echo "$(get_prop 'app_home' $PROPFILE)")
TMPFILE=$(eval echo "$(get_prop 'TMPFILE' $PROPFILE)")
LOGFILES=$(eval echo "$(get_prop 'LOGFILES' $PROPFILE)")
JAVA_BIN=$(get_prop 'JAVA_BIN' $PROPFILE)
JAVA_VERSION_REQUIRED=$(get_prop 'JAVA_VERSION_REQUIRED' $PROPFILE)
JAVA_ORACLE=$(get_prop 'JAVA_ORACLE' $PROPFILE)
mysql_core_file=$(get_prop 'mysql_core_file' $PROPFILE)
mysql_audit_file=$(get_prop 'mysql_audit_file' $PROPFILE)
oracle_core_file=$(get_prop 'oracle_core_file' $PROPFILE)
oracle_audit_file=$(get_prop 'oracle_audit_file' $PROPFILE)
postgres_core_file=$(get_prop 'postgres_core_file' $PROPFILE)
postgres_audit_file=$(get_prop 'postgres_audit_file' $PROPFILE)
sqlserver_core_file=$(get_prop 'sqlserver_core_file' $PROPFILE)
sqlserver_audit_file=$(get_prop 'sqlserver_audit_file' $PROPFILE)
sqlanywhere_core_file=$(get_prop 'sqlanywhere_core_file' $PROPFILE)
sqlanywhere_audit_file=$(get_prop 'sqlanywhere_audit_file' $PROPFILE)
cred_keystore_filename=$(eval echo "$(get_prop 'cred_keystore_filename' $PROPFILE)")
sso_enabled=$(get_prop 'sso_enabled' $PROPFILE)
sso_providerurl=$(get_prop 'sso_providerurl' $PROPFILE)
sso_publickey=$(get_prop 'sso_publickey' $PROPFILE)
RANGER_ADMIN_LOG_DIR=$(eval echo "$(get_prop 'RANGER_ADMIN_LOG_DIR' $PROPFILE)")
RANGER_ADMIN_LOGBACK_CONF_FILE=$(eval echo "$(get_prop 'RANGER_ADMIN_LOGBACK_CONF_FILE' $PROPFILE)")
RANGER_PID_DIR_PATH=$(eval echo "$(get_prop 'RANGER_PID_DIR_PATH' $PROPFILE)")

spnego_principal=$(get_prop 'spnego_principal' $PROPFILE)
spnego_keytab=$(get_prop 'spnego_keytab' $PROPFILE)
token_valid=$(get_prop 'token_valid' $PROPFILE)
cookie_domain=$(get_prop 'cookie_domain' $PROPFILE)
cookie_path=$(get_prop 'cookie_path' $PROPFILE)
admin_principal=$(get_prop 'admin_principal' $PROPFILE)
admin_keytab=$(get_prop 'admin_keytab' $PROPFILE)
lookup_principal=$(get_prop 'lookup_principal' $PROPFILE)
lookup_keytab=$(get_prop 'lookup_keytab' $PROPFILE)
hadoop_conf=$(get_prop 'hadoop_conf' $PROPFILE)
audit_solr_collection_name=$(get_prop 'audit_solr_collection_name' $PROPFILE)
audit_solr_config_name=$(get_prop 'audit_solr_config_name' $PROPFILE)
audit_solr_configset_location=$(get_prop 'audit_solr_configset_location' $PROPFILE)
audit_solr_no_shards=$(get_prop 'audit_solr_no_shards' $PROPFILE)
audit_solr_no_replica=$(get_prop 'audit_solr_no_replica' $PROPFILE)
audit_solr_max_shards_per_node=$(get_prop 'audit_solr_max_shards_per_node' $PROPFILE)
audit_solr_acl_user_list_sasl=$(get_prop 'audit_solr_acl_user_list_sasl' $PROPFILE)
audit_solr_bootstrap_enabled=$(get_prop 'audit_solr_bootstrap_enabled' $PROPFILE)

DB_HOST="${db_host}"

check_ret_status(){
	if [ $1 -ne 0 ]; then
		log "[E] $2";
		exit 1;
	fi
}

check_ret_status_for_groupadd(){
# 9 is the response if the group exists
        if [ $1 -ne 0 ] && [ $1 -ne 9 ]; then
                log "[E] $2";
                exit 1;
        fi
}

check_user_pwd(){
        if [ -z "$1" ]; then
                log "[E] The unix user password is empty. Please set user password.";
                exit 1;
        fi
}

is_command () {
        log "[I] check if command $1 exists"
        type "$1" >/dev/null
}

get_distro(){
	log "[I] Checking distribution name.."
	ver=$(cat /etc/*{issues,release,version} 2> /dev/null)
	if [[ $(echo $ver | grep DISTRIB_ID) ]]; then
                DIST_NAME=$(lsb_release -si)
	else
                DIST_NAME=$(echo $ver | cut -d ' ' -f 1 | sort -u | head -1)
	fi
	export $DIST_NAME
	log "[I] Found distribution : $DIST_NAME"

}
#Get Properties from File without erroring out if property is not there
#$1 -> propertyName $2 -> fileName $3 -> variableName $4 -> failIfNotFound
getPropertyFromFileNoExit(){
	validateProperty=$(sed '/^\#/d' $2 | grep "^$1\s*="  | tail -n 1) # for validation
	if  test -z "$validateProperty" ; then 
		log "[E] '$1' not found in $2 file while getting....!!";
		if [ $4 == "true" ] ; then
                        exit 1;
		else
                        value=""
		fi
	else
		value=$(echo $validateProperty | cut -d "=" -f2-)
	fi
	eval $3="'$value'"
}
#Get Properties from File
#$1 -> propertyName $2 -> fileName $3 -> variableName
getPropertyFromFile(){
	validateProperty=$(sed '/^\#/d' $2 | grep "^$1\s*="  | tail -n 1) # for validation
	if  test -z "$validateProperty" ; then log "[E] '$1' not found in $2 file while getting....!!"; exit 1; fi
	value=$(echo $validateProperty | cut -d "=" -f2-)
	eval $3="'$value'"
}

#Update Properties to File
#$1 -> propertyName $2 -> newPropertyValue $3 -> fileName
updatePropertyToFilePy(){
    $PYTHON_COMMAND_INVOKER update_property.py $1 "${2}" $3
        check_ret_status $? "Update property failed for: " $1
}

init_variables(){
	curDt=`date '+%Y%m%d%H%M%S'`
	VERSION=`cat ${PWD}/version`
	XAPOLICYMGR_DIR=$PWD
	RANGER_ADMIN_INITD=ranger-admin-initd
	RANGER_ADMIN=ranger-admin
	INSTALL_DIR=${XAPOLICYMGR_DIR}
	WEBAPP_ROOT=${INSTALL_DIR}/ews/webapp
	DB_FLAVOR=`echo $DB_FLAVOR | tr '[:lower:]' '[:upper:]'`
	if [ "${DB_FLAVOR}" == "" ]
	then
		DB_FLAVOR="MYSQL"
	fi
	log "[I] DB_FLAVOR=${DB_FLAVOR}"
	audit_store=`echo $audit_store | tr '[:upper:]' '[:lower:]'`
	log "[I] Audit source=${audit_store}"
	if [ "${audit_store}" == "solr" ] ;then
		if [ "${audit_solr_urls}" == "" ] ;then
			log "[I] Please provide valid URL for 'solr' audit store!"
			exit 1
		fi
	fi
	if [ "${audit_store}" == "elasticsearch" ] ;then
		if [ "${audit_elasticsearch_urls}" == "" ] ;then
			log "[I] Please provide valid URL for 'elasticsearch' audit store!"
			exit 1
		fi
		if [ "${audit_elasticsearch_port}" == "" ] ;then
			log "[I] Please provide valid port for 'elasticsearch' audit store!"
			exit 1
		fi
	fi

	if [ "${audit_store}" == "cloudwatch" ] ;then
		if [ "${audit_cloudwatch_region}" == "" ] ;then
			log "[I] Please provide valid region for 'amazon cloudwatch' audit store!"
			exit 1
		fi
		if [ "${audit_cloudwatch_log_group}" == "" ] ;then
			log "[I] Please provide valid log-group for 'amazon cloudwatch' audit store!"
			exit 1
		fi
	fi

	db_ssl_enabled=`echo $db_ssl_enabled | tr '[:upper:]' '[:lower:]'`
	if [ "${db_ssl_enabled}" != "true" ]
	then
		db_ssl_enabled="false"
		db_ssl_required="false"
		db_ssl_verifyServerCertificate="false"
		db_ssl_auth_type="2-way"
		db_ssl_certificate_file=''
		javax_net_ssl_trustStore_type='jks'
		javax_net_ssl_keyStore_type='jks'
	fi
	if [ "${db_ssl_enabled}" == "true" ]
	then
		db_ssl_required=`echo $db_ssl_required | tr '[:upper:]' '[:lower:]'`
		db_ssl_verifyServerCertificate=`echo $db_ssl_verifyServerCertificate | tr '[:upper:]' '[:lower:]'`
		db_ssl_auth_type=`echo $db_ssl_auth_type | tr '[:upper:]' '[:lower:]'`
		javax_net_ssl_trustStore_type=`echo $javax_net_ssl_trustStore_type | tr '[:upper:]' '[:lower:]'`
		javax_net_ssl_keyStore_type=`echo $javax_net_ssl_keyStore_type | tr '[:upper:]' '[:lower:]'`
		if [ "${db_ssl_required}" != "true" ]
		then
			db_ssl_required="false"
		fi
		if [ "${db_ssl_verifyServerCertificate}" != "true" ]
		then
			db_ssl_verifyServerCertificate="false"
		fi
		if [ "${db_ssl_auth_type}" != "1-way" ]
		then
			db_ssl_auth_type="2-way"
		fi
		if [ "${javax_net_ssl_trustStore_type}" == "" ]
		then
			javax_net_ssl_trustStore_type="jks"
		fi
		if [ "${javax_net_ssl_keyStore_type}" == "" ]
		then
			javax_net_ssl_keyStore_type="jks"
		fi
	fi
}

check_python_command() {
		if is_command ${PYTHON_COMMAND_INVOKER} ; then
			log "[I] '${PYTHON_COMMAND_INVOKER}' command found"
		else
			log "[E] '${PYTHON_COMMAND_INVOKER}' command not found"
		exit 1;
		fi
}

run_dba_steps(){
	getPropertyFromFileNoExit 'setup_mode' $PROPFILE setup_mode false
	if [ "x${setup_mode}x" == "xSeparateDBAx" ]; then
		log "[I] Setup mode is set to SeparateDBA. Not Running DBA steps. Please run dba_script.py before running setup..!";
	else
		log "[I] Setup mode is not set. Running DBA steps..";
                $PYTHON_COMMAND_INVOKER dba_script.py -q
        fi
}
check_ranger_version(){
        $PYTHON_COMMAND_INVOKER db_setup.py -checkupgrade
}
check_db_connector() {
	log "[I] Checking ${DB_FLAVOR} CONNECTOR FILE : ${SQL_CONNECTOR_JAR}"
	if test -f "$SQL_CONNECTOR_JAR"; then
		log "[I] ${DB_FLAVOR} CONNECTOR FILE : $SQL_CONNECTOR_JAR file found"
	else
		log "[E] ${DB_FLAVOR} CONNECTOR FILE : $SQL_CONNECTOR_JAR does not exists" ; exit 1;
	fi
}
check_java_version() {
	#Check for JAVA_HOME
	if [ "${JAVA_HOME}" == "" ]
	then
		log "[E] JAVA_HOME environment property not defined, aborting installation."
		exit 1
	fi
        export JAVA_BIN=${JAVA_HOME}/bin/java

	if is_command ${JAVA_BIN} ; then
		log "[I] '${JAVA_BIN}' command found"
	else
                log "[E] '${JAVA_BIN}' command not found"
                exit 1;
	fi

	version=$("$JAVA_BIN" -version 2>&1 | awk -F '"' '/version/ {print $2}')
	major=`echo ${version} | cut -d. -f1`
	minor=`echo ${version} | cut -d. -f2`
	current_java_version="$major.$minor"
	num_current_java_version=`echo $current_java_version|awk ' { printf("%3.2f\n", $0); } '`
	JAVA_VERSION_REQUIRED=`echo $JAVA_VERSION_REQUIRED | awk '{gsub(/ /,"")}1'`
	JAVA_VERSION_REQUIRED=`echo $JAVA_VERSION_REQUIRED | awk '{gsub(/'"'"'/,"")}1'`
	num_required_java_version=`echo $JAVA_VERSION_REQUIRED|awk ' { printf("%3.2f\n", $0); } '`
	if [ `echo "$num_current_java_version < $num_required_java_version" | bc` -eq 1 ];then
		log "[E] The java version must be greater than or equal to $JAVA_VERSION_REQUIRED, the current java version is $version"
		exit 1;
	fi
}

sanity_check_files() {

	if test -d $app_home; then
		log "[I] $app_home folder found"
	else
		log "[E] $app_home does not exists" ; exit 1;
        fi
	if [ "${DB_FLAVOR}" == "MYSQL" ]
        then
		if test -f $mysql_core_file; then
			log "[I] $mysql_core_file file found"
		else
			log "[E] $mysql_core_file does not exists" ; exit 1;
		fi
	fi
	if [ "${DB_FLAVOR}" == "ORACLE" ]
        then
                if test -f ${oracle_core_file}; then
			log "[I] ${oracle_core_file} file found"
                else
                        log "[E] ${oracle_core_file} does not exists" ; exit 1;
                fi
        fi
        if [ "${DB_FLAVOR}" == "POSTGRES" ]
        then
                if test -f ${postgres_core_file}; then
			log "[I] ${postgres_core_file} file found"
                else
                        log "[E] ${postgres_core_file} does not exists" ; exit 1;
                fi
        fi
        if [ "${DB_FLAVOR}" == "MSSQL" ]
        then
                if test -f ${sqlserver_core_file}; then
			log "[I] ${sqlserver_core_file} file found"
                else
                        log "[E] ${sqlserver_core_file} does not exists" ; exit 1;
                fi
        fi
	if [ "${DB_FLAVOR}" == "SQLA" ]
	then
		if [ "${LD_LIBRARY_PATH}" == "" ]
		then
			log "[E] LD_LIBRARY_PATH environment property not defined, aborting installation."
			exit 1
		fi
		if test -f ${sqlanywhere_core_file}; then
			log "[I] ${sqlanywhere_core_file} file found"
		else
			log "[E] ${sqlanywhere_core_file} does not exists" ; exit 1;
		fi
	fi
}

create_rollback_point() {
        DATE=`date`
        BAK_FILE=$APP-$VERSION.$DATE.bak
        log "Creating backup file : $BAK_FILE"
        cp "$APP" "$BAK_FILE"
}

copy_db_connector(){
	log "[I] Copying ${DB_FLAVOR} Connector to $app_home/WEB-INF/lib ";
        cp -f $SQL_CONNECTOR_JAR $app_home/WEB-INF/lib
	check_ret_status $? "Copying ${DB_FLAVOR} Connector to $app_home/WEB-INF/lib failed"
	log "[I] Copying ${DB_FLAVOR} Connector to $app_home/WEB-INF/lib DONE";
}

update_properties() {
	newPropertyValue=''
	echo "export JAVA_HOME=${JAVA_HOME}" > ${WEBAPP_ROOT}/WEB-INF/classes/conf/java_home.sh
	chmod a+rx ${WEBAPP_ROOT}/WEB-INF/classes/conf/java_home.sh

	to_file_ranger=$app_home/WEB-INF/classes/conf/ranger-admin-site.xml
	if test -f $to_file_ranger; then
		log "[I] $to_file_ranger file found"
	else
		log "[E] $to_file_ranger does not exists" ; exit 1;
        fi

	to_file_default=$app_home/WEB-INF/classes/conf/ranger-admin-default-site.xml
	if test -f $to_file_default; then
		log "[I] $to_file_default file found"
	else
		log "[E] $to_file_default does not exists" ; exit 1;
        fi

	if [ "${spnego_principal}" != "" ]
	then
                propertyName=ranger.spnego.kerberos.principal
                newPropertyValue="${spnego_principal}"
                updatePropertyToFilePy $propertyName $newPropertyValue $to_file_ranger
	fi
	
        if [ "${audit_solr_collection_name}" != "" ]
        then
               propertyName=ranger.audit.solr.collection.name
               newPropertyValue="${audit_solr_collection_name}"
               updatePropertyToFilePy $propertyName $newPropertyValue $to_file_ranger
        fi

        if [ "${audit_solr_config_name}" != "" ]
        then
               propertyName=ranger.audit.solr.config.name
               newPropertyValue="${audit_solr_config_name}"
               updatePropertyToFilePy $propertyName $newPropertyValue $to_file_ranger
        fi

		if [ "${audit_solr_configset_location}" != "" ]
        then
               propertyName=ranger.audit.solr.configset.location
               newPropertyValue="${audit_solr_configset_location}"
               updatePropertyToFilePy $propertyName $newPropertyValue $to_file_ranger
        fi

        if [ "${audit_solr_no_shards}" != "" ]
        then
               propertyName=ranger.audit.solr.no.shards
               newPropertyValue="${audit_solr_no_shards}"
               updatePropertyToFilePy $propertyName $newPropertyValue $to_file_ranger
        fi


        if [ "${audit_solr_max_shards_per_node}" != "" ]
        then
               propertyName=ranger.audit.solr.max.shards.per.node
               newPropertyValue="${audit_solr_max_shards_per_node}"
               updatePropertyToFilePy $propertyName $newPropertyValue $to_file_ranger
        fi

        if [ "${audit_solr_no_replica}" != "" ]
        then
               propertyName=ranger.audit.solr.no.replica
               newPropertyValue="${audit_solr_no_replica}"
               updatePropertyToFilePy $propertyName $newPropertyValue $to_file_ranger
        fi
	
	if [ "${audit_solr_acl_user_list_sasl}" != "" ]
        then
               propertyName=ranger.audit.solr.acl.user.list.sasl
               newPropertyValue="${audit_solr_acl_user_list_sasl}"
               updatePropertyToFilePy $propertyName $newPropertyValue $to_file_ranger
        fi


	if [ "${spnego_keytab}" != "" ]
	then
                propertyName=ranger.spnego.kerberos.keytab
                newPropertyValue="${spnego_keytab}"
                updatePropertyToFilePy $propertyName $newPropertyValue $to_file_ranger
	fi

	if [ "${token_valid}" != "" ]
	then
                propertyName=ranger.admin.kerberos.token.valid.seconds
                newPropertyValue="${token_valid}"
                updatePropertyToFilePy $propertyName $newPropertyValue $to_file_ranger
	fi
	
	if [ "${cookie_domain}" != "" ]
	then
                propertyName=ranger.admin.kerberos.cookie.domain
                newPropertyValue="${cookie_domain}"
                updatePropertyToFilePy $propertyName $newPropertyValue $to_file_ranger
	fi

	if [ "${cookie_path}" != "" ]
	then
                propertyName=ranger.admin.kerberos.cookie.path
                newPropertyValue="${cookie_path}"
                updatePropertyToFilePy $propertyName $newPropertyValue $to_file_ranger
	fi

	if [ "${admin_principal}" != "" ]
	then
                propertyName=ranger.admin.kerberos.principal
                newPropertyValue="${admin_principal}"
                updatePropertyToFilePy $propertyName $newPropertyValue $to_file_ranger
	fi
	
	if [ "${admin_keytab}" != "" ]
	then
                propertyName=ranger.admin.kerberos.keytab
                newPropertyValue="${admin_keytab}"
                updatePropertyToFilePy $propertyName $newPropertyValue $to_file_ranger
	fi

	if [ "${lookup_principal}" != "" ]
        then
                propertyName=ranger.lookup.kerberos.principal
                newPropertyValue="${lookup_principal}"
                updatePropertyToFilePy $propertyName $newPropertyValue $to_file_ranger
	fi

	if [ "${lookup_keytab}" != "" ]
	then
                propertyName=ranger.lookup.kerberos.keytab
                newPropertyValue="${lookup_keytab}"
                updatePropertyToFilePy $propertyName $newPropertyValue $to_file_ranger
	fi

	if [ "${db_ssl_enabled}" != "" ]
	then
		propertyName=ranger.db.ssl.enabled
		newPropertyValue="${db_ssl_enabled}"
		updatePropertyToFilePy $propertyName $newPropertyValue $to_file_default

		propertyName=ranger.db.ssl.required
		newPropertyValue="${db_ssl_required}"
		updatePropertyToFilePy $propertyName $newPropertyValue $to_file_default

		propertyName=ranger.db.ssl.verifyServerCertificate
		newPropertyValue="${db_ssl_verifyServerCertificate}"
		updatePropertyToFilePy $propertyName $newPropertyValue $to_file_default

		propertyName=ranger.db.ssl.auth.type
		newPropertyValue="${db_ssl_auth_type}"
		updatePropertyToFilePy $propertyName $newPropertyValue $to_file_default

		if [ "${db_ssl_certificate_file}" != "" ]
		then
			propertyName=ranger.db.ssl.certificateFile
			newPropertyValue="${db_ssl_certificate_file}"
			updatePropertyToFilePy $propertyName $newPropertyValue $to_file_default
		fi

		propertyName=ranger.truststore.file.type
		newPropertyValue="${javax_net_ssl_trustStore_type}"
		updatePropertyToFilePy $propertyName $newPropertyValue $to_file_default

		propertyName=ranger.keystore.file.type
		newPropertyValue="${javax_net_ssl_keyStore_type}"
		updatePropertyToFilePy $propertyName $newPropertyValue $to_file_default
	fi

	if [ "${DB_FLAVOR}" == "MYSQL" ]
	then
		propertyName=ranger.jpa.jdbc.url
		newPropertyValue="jdbc:log4jdbc:mysql://${DB_HOST}/${db_name}"
		updatePropertyToFilePy $propertyName $newPropertyValue $to_file_ranger

		propertyName=ranger.jpa.jdbc.dialect
		newPropertyValue="org.eclipse.persistence.platform.database.MySQLPlatform"
		updatePropertyToFilePy $propertyName $newPropertyValue $to_file_default

		propertyName=ranger.jpa.audit.jdbc.dialect
		newPropertyValue="org.eclipse.persistence.platform.database.MySQLPlatform"
		updatePropertyToFilePy $propertyName $newPropertyValue $to_file_default

		propertyName=ranger.jpa.jdbc.driver
		newPropertyValue="net.sf.log4jdbc.DriverSpy"
		updatePropertyToFilePy $propertyName $newPropertyValue $to_file_ranger

		propertyName=ranger.jpa.audit.jdbc.driver
		newPropertyValue="net.sf.log4jdbc.DriverSpy"
		updatePropertyToFilePy $propertyName $newPropertyValue $to_file_default

		propertyName=ranger.jpa.jdbc.preferredtestquery
		newPropertyValue="select 1"
		updatePropertyToFilePy $propertyName "${newPropertyValue}" $to_file_default
	fi
	if [ "${DB_FLAVOR}" == "ORACLE" ]
	then
		propertyName=ranger.jpa.jdbc.url
		count=$(grep -o ":" <<< "$DB_HOST" | wc -l)
		#if [[ ${count} -eq 2 ]] ; then
		if [ ${count} -eq 2 ] || [ ${count} -eq 0 ]; then
			#jdbc:oracle:thin:@[HOST][:PORT]:SID or #jdbc:oracle:thin:@GL
			newPropertyValue="jdbc:oracle:thin:@${DB_HOST}"
		else
			#jdbc:oracle:thin:@//[HOST][:PORT]/SERVICE
			newPropertyValue="jdbc:oracle:thin:@//${DB_HOST}"
		fi
		updatePropertyToFilePy $propertyName $newPropertyValue $to_file_ranger

		propertyName=ranger.jpa.jdbc.dialect
		newPropertyValue="org.eclipse.persistence.platform.database.OraclePlatform"
		updatePropertyToFilePy $propertyName $newPropertyValue $to_file_default

		propertyName=ranger.jpa.audit.jdbc.dialect
		newPropertyValue="org.eclipse.persistence.platform.database.OraclePlatform"
		updatePropertyToFilePy $propertyName $newPropertyValue $to_file_default

		propertyName=ranger.jpa.jdbc.driver
		newPropertyValue="oracle.jdbc.OracleDriver"
		updatePropertyToFilePy $propertyName $newPropertyValue $to_file_ranger

		propertyName=ranger.jpa.audit.jdbc.driver
		newPropertyValue="oracle.jdbc.OracleDriver"
		updatePropertyToFilePy $propertyName $newPropertyValue $to_file_default

		propertyName=ranger.jpa.jdbc.preferredtestquery
		newPropertyValue="select 1 from dual"
		updatePropertyToFilePy $propertyName "${newPropertyValue}" $to_file_default
	fi
	if [ "${DB_FLAVOR}" == "POSTGRES" ]
	then

		if [ "${db_ssl_enabled}" == "true" ]
		then
			if test -f $db_ssl_certificate_file; then
				propertyName=ranger.jpa.jdbc.url
				newPropertyValue="jdbc:postgresql://${DB_HOST}/${db_name}?ssl=true&sslmode=verify-full&sslrootcert=${db_ssl_certificate_file}"
				updatePropertyToFilePy $propertyName $newPropertyValue $to_file_ranger
			else
				propertyName=ranger.jpa.jdbc.url
				newPropertyValue="jdbc:postgresql://${DB_HOST}/${db_name}?ssl=true&sslmode=verify-full&sslfactory=org.postgresql.ssl.DefaultJavaSSLFactory"
				updatePropertyToFilePy $propertyName $newPropertyValue $to_file_ranger
			fi
		else
			propertyName=ranger.jpa.jdbc.url
			newPropertyValue="jdbc:postgresql://${DB_HOST}/${db_name}"
			updatePropertyToFilePy $propertyName $newPropertyValue $to_file_ranger
		fi

		propertyName=ranger.jpa.jdbc.dialect
		newPropertyValue="org.eclipse.persistence.platform.database.PostgreSQLPlatform"
		updatePropertyToFilePy $propertyName $newPropertyValue $to_file_default

		propertyName=ranger.jpa.audit.jdbc.dialect
		newPropertyValue="org.eclipse.persistence.platform.database.PostgreSQLPlatform"
		updatePropertyToFilePy $propertyName $newPropertyValue $to_file_default

		propertyName=ranger.jpa.jdbc.driver
		newPropertyValue="org.postgresql.Driver"
		updatePropertyToFilePy $propertyName $newPropertyValue $to_file_ranger

		propertyName=ranger.jpa.audit.jdbc.driver
		newPropertyValue="org.postgresql.Driver"
		updatePropertyToFilePy $propertyName $newPropertyValue $to_file_default

		propertyName=ranger.jpa.jdbc.preferredtestquery
		newPropertyValue="select 1"
		updatePropertyToFilePy $propertyName "${newPropertyValue}" $to_file_default
	fi

	if [ "${DB_FLAVOR}" == "MSSQL" ]
	then
		propertyName=ranger.jpa.jdbc.url
		newPropertyValue="jdbc:sqlserver://${DB_HOST};databaseName=${db_name}"
		updatePropertyToFilePy $propertyName $newPropertyValue $to_file_ranger

		propertyName=ranger.jpa.jdbc.dialect
		newPropertyValue="org.eclipse.persistence.platform.database.SQLServerPlatform"
		updatePropertyToFilePy $propertyName $newPropertyValue $to_file_default

		propertyName=ranger.jpa.audit.jdbc.dialect
		newPropertyValue="org.eclipse.persistence.platform.database.SQLServerPlatform"
		updatePropertyToFilePy $propertyName $newPropertyValue $to_file_default

		propertyName=ranger.jpa.jdbc.driver
		newPropertyValue="com.microsoft.sqlserver.jdbc.SQLServerDriver"
		updatePropertyToFilePy $propertyName $newPropertyValue $to_file_ranger

		propertyName=ranger.jpa.audit.jdbc.driver
		newPropertyValue="com.microsoft.sqlserver.jdbc.SQLServerDriver"
		updatePropertyToFilePy $propertyName $newPropertyValue $to_file_default

		propertyName=ranger.jpa.jdbc.preferredtestquery
		newPropertyValue="select 1"
		updatePropertyToFilePy $propertyName "${newPropertyValue}" $to_file_default
	fi

	if [ "${DB_FLAVOR}" == "SQLA" ]
	then
		propertyName=ranger.jpa.jdbc.url
		newPropertyValue="jdbc:sqlanywhere:database=${db_name};host=${DB_HOST}"
		updatePropertyToFilePy $propertyName $newPropertyValue $to_file_ranger

		propertyName=ranger.jpa.jdbc.dialect
		newPropertyValue="org.eclipse.persistence.platform.database.SQLAnywherePlatform"
		updatePropertyToFilePy $propertyName $newPropertyValue $to_file_default

		propertyName=ranger.jpa.audit.jdbc.dialect
		newPropertyValue="org.eclipse.persistence.platform.database.SQLAnywherePlatform"
		updatePropertyToFilePy $propertyName $newPropertyValue $to_file_default

		propertyName=ranger.jpa.jdbc.driver
		newPropertyValue="sap.jdbc4.sqlanywhere.IDriver"
		updatePropertyToFilePy $propertyName $newPropertyValue $to_file_ranger

		propertyName=ranger.jpa.audit.jdbc.driver
		newPropertyValue="sap.jdbc4.sqlanywhere.IDriver"
		updatePropertyToFilePy $propertyName $newPropertyValue $to_file_default

		propertyName=ranger.jpa.jdbc.preferredtestquery
		newPropertyValue="select 1"
		updatePropertyToFilePy $propertyName "${newPropertyValue}" $to_file_default
	fi

	if [ "${audit_store}" == "solr" ]
	then
		propertyName=ranger.audit.solr.urls
		newPropertyValue=${audit_solr_urls}
		updatePropertyToFilePy $propertyName $newPropertyValue $to_file_ranger

		propertyName=ranger.audit.solr.bootstrap.enabled
		newPropertyValue=${audit_solr_bootstrap_enabled}
		updatePropertyToFilePy $propertyName $newPropertyValue $to_file_ranger
	fi

	if [ "${audit_store}" == "elasticsearch" ]
	then
		propertyName=ranger.audit.elasticsearch.urls
		newPropertyValue=${audit_elasticsearch_urls}
		updatePropertyToFilePy $propertyName $newPropertyValue $to_file_ranger

		propertyName=ranger.audit.elasticsearch.port
		newPropertyValue=${audit_elasticsearch_port}
		updatePropertyToFilePy $propertyName $newPropertyValue $to_file_ranger

		propertyName=ranger.audit.elasticsearch.user
		newPropertyValue=${audit_elasticsearch_user}
		updatePropertyToFilePy $propertyName $newPropertyValue $to_file_ranger

		propertyName=ranger.audit.elasticsearch.password
		newPropertyValue=${audit_elasticsearch_password}
		updatePropertyToFilePy $propertyName $newPropertyValue $to_file_ranger

		propertyName=ranger.audit.elasticsearch.index
		newPropertyValue=${audit_elasticsearch_index}
		updatePropertyToFilePy $propertyName $newPropertyValue $to_file_ranger

		propertyName=ranger.audit.elasticsearch.bootstrap.enabled
		newPropertyValue=${audit_elasticsearch_bootstrap_enabled}
		updatePropertyToFilePy $propertyName $newPropertyValue $to_file_ranger

	fi

	if [ "${audit_store}" == "cloudwatch" ]
	then
		propertyName=ranger.audit.amazon_cloudwatch.region
		newPropertyValue=${audit_cloudwatch_region}
		updatePropertyToFilePy $propertyName $newPropertyValue $to_file_ranger

		propertyName=ranger.audit.amazon_cloudwatch.log_group
		newPropertyValue=${audit_cloudwatch_log_group}
		updatePropertyToFilePy $propertyName $newPropertyValue $to_file_ranger

		propertyName=ranger.audit.amazon_cloudwatch.log_stream_prefix
		newPropertyValue=${audit_cloudwatch_log_stream_prefix}
		updatePropertyToFilePy $propertyName $newPropertyValue $to_file_ranger
	fi

	if [ "${audit_store}" != "" ]
	then
		propertyName=ranger.audit.source.type
		newPropertyValue=${audit_store}
		updatePropertyToFilePy $propertyName $newPropertyValue $to_file_ranger
	fi

	propertyName=ranger.externalurl
	newPropertyValue="${policymgr_external_url}"
	updatePropertyToFilePy $propertyName $newPropertyValue $to_file_ranger

	propertyName=ranger.service.http.enabled
	newPropertyValue="${policymgr_http_enabled}"
	updatePropertyToFilePy $propertyName $newPropertyValue $to_file_ranger

	propertyName=ranger.supportedcomponents
	newPropertyValue="${policymgr_supportedcomponents}"
	updatePropertyToFilePy $propertyName $newPropertyValue $to_file_ranger

	propertyName=ranger.jpa.jdbc.user
	newPropertyValue="${db_user}"
	updatePropertyToFilePy $propertyName $newPropertyValue $to_file_ranger

	##########

	keystore="${cred_keystore_filename}"

	echo "Starting configuration for Ranger DB credentials:"

	db_password_alias=ranger.db.password

	if [ "${keystore}" != "" ]
	then
		mkdir -p `dirname "${keystore}"`
		$PYTHON_COMMAND_INVOKER ranger_credential_helper.py -l "cred/lib/*" -f "$keystore" -k "$db_password_alias" -v "$db_password" -c 1

		propertyName=ranger.credential.provider.path
		newPropertyValue="${keystore}"
		updatePropertyToFilePy $propertyName $newPropertyValue $to_file_default

		propertyName=ranger.jpa.jdbc.credential.alias
		newPropertyValue="${db_password_alias}"
		updatePropertyToFilePy $propertyName $newPropertyValue $to_file_default

		propertyName=ranger.credential.provider.path
		newPropertyValue="${keystore}"
		updatePropertyToFilePy $propertyName $newPropertyValue $to_file_ranger

		propertyName=ranger.jpa.jdbc.password
		newPropertyValue="_"
		updatePropertyToFilePy $propertyName $newPropertyValue $to_file_ranger

		if test -f "${keystore}"; then
			#echo "$keystore found."
			chown -R ${unix_user}:${unix_group} ${keystore}
			chmod 640 ${keystore}
		else
			propertyName=ranger.jpa.jdbc.password
			newPropertyValue="${db_password}"
			updatePropertyToFilePy $propertyName $newPropertyValue $to_file_ranger
		fi
	else
		propertyName=ranger.jpa.jdbc.password
		newPropertyValue="${db_password}"
		updatePropertyToFilePy $propertyName $newPropertyValue $to_file_ranger
	fi

	###########
	if [ "${audit_store}" == "solr" ]
	then
		if [ "${audit_solr_zookeepers}" != "" ]
		then
			propertyName=ranger.audit.solr.zookeepers
			newPropertyValue=${audit_solr_zookeepers}
			updatePropertyToFilePy $propertyName $newPropertyValue $to_file_ranger
		fi
		if [ "${audit_solr_user}" != "" ] && [ "${audit_solr_password}" != "" ]
		then
			propertyName=ranger.solr.audit.user
			newPropertyValue=${audit_solr_user}
			updatePropertyToFilePy $propertyName $newPropertyValue $to_file_ranger

			if [ "${keystore}" != "" ]
			then
				echo "Starting configuration for solr credentials:"
				mkdir -p `dirname "${keystore}"`
				audit_solr_password_alias=ranger.solr.password

				$PYTHON_COMMAND_INVOKER ranger_credential_helper.py -l "cred/lib/*" -f "$keystore" -k "$audit_solr_password_alias" -v "$audit_solr_password" -c 1

				propertyName=ranger.solr.audit.credential.alias
				newPropertyValue="${audit_solr_password_alias}"
				updatePropertyToFilePy $propertyName $newPropertyValue $to_file_default

				propertyName=ranger.solr.audit.user.password
				newPropertyValue="_"
				updatePropertyToFilePy $propertyName $newPropertyValue $to_file_ranger

				if test -f "${keystore}"; then
					chown -R ${unix_user}:${unix_group} ${keystore}
				else
					propertyName=ranger.solr.audit.user.password
					newPropertyValue="${audit_solr_password}"
					updatePropertyToFilePy $propertyName $newPropertyValue $to_file_ranger
				fi
			else
				propertyName=ranger.solr.audit.user.password
				newPropertyValue="${audit_solr_password}"
				updatePropertyToFilePy $propertyName $newPropertyValue $to_file_ranger
			fi
		fi
	fi

	if [ "${sso_enabled}" == "" ]
	then
		sso_enabled="false"
	fi

	sso_enabled=`echo $sso_enabled | tr '[:upper:]' '[:lower:]'`

	if [ "${sso_enabled}" == "true" ]
	then
		if [ "${sso_providerurl}" == "" ] || [ "${sso_publickey}" == "" ]
		then
			log "[E] Please provide valid values in SSO config properties!";
			exit 1
		fi
		propertyName=ranger.sso.enabled
		newPropertyValue="${sso_enabled}"
		updatePropertyToFilePy $propertyName $newPropertyValue $to_file_ranger
	 
		propertyName=ranger.sso.providerurl
		newPropertyValue="${sso_providerurl}"
		updatePropertyToFilePy $propertyName $newPropertyValue $to_file_ranger
	 
		propertyName=ranger.sso.publicKey
		newPropertyValue="${sso_publickey}"
		updatePropertyToFilePy $propertyName $newPropertyValue $to_file_ranger
	 
	 else
                propertyName=ranger.sso.enabled
                newPropertyValue="false"
                updatePropertyToFilePy $propertyName $newPropertyValue $to_file_ranger

	fi
	if [ "${javax_net_ssl_keyStore}" != "" ]  && [ "${javax_net_ssl_keyStorePassword}" != "" ]
	then
		javax_net_ssl_keyStoreAlias=keyStoreAlias

		propertyName=ranger.keystore.file
		newPropertyValue="${javax_net_ssl_keyStore}"
		updatePropertyToFilePy $propertyName $newPropertyValue $to_file_default

		propertyName=ranger.keystore.alias
		newPropertyValue="${javax_net_ssl_keyStoreAlias}"
		updatePropertyToFilePy $propertyName $newPropertyValue $to_file_default

		if [ "${keystore}" != "" ]
		then
			propertyName=ranger.keystore.password
			newPropertyValue="_"
			updatePropertyToFilePy $propertyName $newPropertyValue $to_file_default

			$PYTHON_COMMAND_INVOKER ranger_credential_helper.py -l "cred/lib/*" -f "$keystore" -k "$javax_net_ssl_keyStoreAlias" -v "$javax_net_ssl_keyStorePassword" -c 1

			if test -f "${keystore}"; then
				chown -R ${unix_user}:${unix_group} ${keystore}
			else
				propertyName=ranger.keystore.password
				newPropertyValue="${javax_net_ssl_keyStorePassword}"
				updatePropertyToFilePy $propertyName $newPropertyValue $to_file_default
			fi
		else
			propertyName=ranger.keystore.password
			newPropertyValue="${javax_net_ssl_keyStorePassword}"
			updatePropertyToFilePy $propertyName $newPropertyValue $to_file_default
		fi

	fi
	if [ "${javax_net_ssl_trustStore}" != "" ]  && [ "${javax_net_ssl_trustStorePassword}" != "" ]
	then
		javax_net_ssl_trustStoreAlias=trustStoreAlias

		propertyName=ranger.truststore.file
		newPropertyValue="${javax_net_ssl_trustStore}"
		updatePropertyToFilePy $propertyName $newPropertyValue $to_file_default

		propertyName=ranger.truststore.alias
		newPropertyValue="${javax_net_ssl_trustStoreAlias}"
		updatePropertyToFilePy $propertyName $newPropertyValue $to_file_default
		if [ "${keystore}" != "" ]
		then
			propertyName=ranger.truststore.password
			newPropertyValue="_"
			updatePropertyToFilePy $propertyName $newPropertyValue $to_file_default

			$PYTHON_COMMAND_INVOKER ranger_credential_helper.py -l "cred/lib/*" -f "$keystore" -k "$javax_net_ssl_trustStoreAlias" -v "$javax_net_ssl_trustStorePassword" -c 1

			if test -f "${keystore}"; then
				chown -R ${unix_user}:${unix_group} ${keystore}
			else
				propertyName=ranger.truststore.password
				newPropertyValue="${javax_net_ssl_trustStorePassword}"
				updatePropertyToFilePy $propertyName $newPropertyValue $to_file_default
			fi
		else
			propertyName=ranger.truststore.password
			newPropertyValue="${javax_net_ssl_trustStorePassword}"
			updatePropertyToFilePy $propertyName $newPropertyValue $to_file_default
		fi

	fi
	if [ "${policymgr_http_enabled}" == "false" ]
	then
		if [ "${policymgr_https_keystore_keyalias}" == "" ]
		then
			policymgr_https_keystore_keyalias=rangeradmin
		fi
		if [ "${policymgr_https_keystore_file}" != "" ] && [ "${policymgr_https_keystore_password}" != "" ]
		then
			propertyName=ranger.service.https.attrib.ssl.enabled
			newPropertyValue="true"
			updatePropertyToFilePy $propertyName $newPropertyValue $to_file_ranger

			propertyName=ranger.service.https.attrib.client.auth
			newPropertyValue="want"
			updatePropertyToFilePy $propertyName $newPropertyValue $to_file_default

			propertyName=ranger.service.https.attrib.keystore.file
			newPropertyValue="${policymgr_https_keystore_file}"
			updatePropertyToFilePy $propertyName $newPropertyValue $to_file_ranger

			propertyName=ranger.service.https.attrib.keystore.keyalias
			newPropertyValue="${policymgr_https_keystore_keyalias}"
			updatePropertyToFilePy $propertyName $newPropertyValue $to_file_ranger

			policymgr_https_keystore_credential_alias=keyStoreCredentialAlias
			propertyName=ranger.service.https.attrib.keystore.credential.alias
			newPropertyValue="${policymgr_https_keystore_credential_alias}"
			updatePropertyToFilePy $propertyName $newPropertyValue $to_file_ranger

			if [ "${keystore}" != "" ]
			then
				propertyName=ranger.service.https.attrib.keystore.pass
				newPropertyValue="_"
				updatePropertyToFilePy $propertyName $newPropertyValue $to_file_ranger
				$PYTHON_COMMAND_INVOKER ranger_credential_helper.py -l "cred/lib/*" -f "$keystore" -k "$policymgr_https_keystore_credential_alias" -v "$policymgr_https_keystore_password" -c 1

				if test -f "${keystore}"; then
					chown -R ${unix_user}:${unix_group} ${keystore}
				else
					propertyName=ranger.service.https.attrib.keystore.pass
					newPropertyValue="${policymgr_https_keystore_password}"
					updatePropertyToFilePy $propertyName $newPropertyValue $to_file_ranger
				fi
			else
				propertyName=ranger.service.https.attrib.keystore.pass
				newPropertyValue="${policymgr_https_keystore_password}"
				updatePropertyToFilePy $propertyName $newPropertyValue $to_file_ranger
			fi
		fi
	fi

	if [ "${ranger_unixauth_keystore}" != "" ] && [ "${ranger_unixauth_keystore_password}" != "" ]
	then
		propertyName=ranger.unixauth.keystore
		newPropertyValue="${ranger_unixauth_keystore}"
		updatePropertyToFilePy $propertyName $newPropertyValue $to_file_default

		ranger_unixauth_keystore_alias=unixAuthKeyStoreAlias
		propertyName=ranger.unixauth.keystore.credential.alias
		newPropertyValue="${ranger_unixauth_keystore_alias}"
		updatePropertyToFilePy $propertyName $newPropertyValue $to_file_default
		if [ "${keystore}" != "" ]
		then
			propertyName=ranger.unixauth.keystore.password
			newPropertyValue="_"
			updatePropertyToFilePy $propertyName $newPropertyValue $to_file_default
			$PYTHON_COMMAND_INVOKER ranger_credential_helper.py -l "cred/lib/*" -f "$keystore" -k "$ranger_unixauth_keystore_alias" -v "$ranger_unixauth_keystore_password" -c 1

			if test -f "${keystore}"; then
				chown -R ${unix_user}:${unix_group} ${keystore}
			else
				propertyName=ranger.unixauth.keystore.password
				newPropertyValue="${ranger_unixauth_keystore_password}"
				updatePropertyToFilePy $propertyName $newPropertyValue $to_file_default
			fi
		else
			propertyName=ranger.unixauth.keystore.password
			newPropertyValue="${ranger_unixauth_keystore_password}"
			updatePropertyToFilePy $propertyName $newPropertyValue $to_file_default
		fi
	fi

	if [ "${ranger_unixauth_truststore}" != "" ] && [ "${ranger_unixauth_truststore_password}" != "" ]
	then
		propertyName=ranger.unixauth.truststore
		newPropertyValue="${ranger_unixauth_truststore}"
		updatePropertyToFilePy $propertyName $newPropertyValue $to_file_default

		ranger_unixauth_truststore_alias=unixAuthTrustStoreAlias
		propertyName=ranger.unixauth.truststore.credential.alias
		newPropertyValue="${ranger_unixauth_truststore_alias}"
		updatePropertyToFilePy $propertyName $newPropertyValue $to_file_default

		if [ "${keystore}" != "" ]
		then
			propertyName=ranger.unixauth.truststore.password
			newPropertyValue="_"
			updatePropertyToFilePy $propertyName $newPropertyValue $to_file_default
			$PYTHON_COMMAND_INVOKER ranger_credential_helper.py -l "cred/lib/*" -f "$keystore" -k "$ranger_unixauth_truststore_alias" -v "$ranger_unixauth_truststore_password" -c 1

			if test -f $keystore; then
				chown -R ${unix_user}:${unix_group} ${keystore}
			else
				propertyName=ranger.unixauth.truststore.password
				newPropertyValue="${ranger_unixauth_truststore_password}"
				updatePropertyToFilePy $propertyName $newPropertyValue $to_file_default
			fi
		else
			propertyName=ranger.unixauth.truststore.password
			newPropertyValue="${ranger_unixauth_truststore_password}"
			updatePropertyToFilePy $propertyName $newPropertyValue $to_file_default
		fi
	fi

}

do_unixauth_setup() {

        ldap_file=$app_home/WEB-INF/classes/conf/ranger-admin-site.xml
        if test -f $ldap_file; then
                log "[I] $ldap_file file found"

                propertyName=ranger.authentication.method
                newPropertyValue="${authentication_method}"
                updatePropertyToFilePy $propertyName $newPropertyValue $ldap_file

                propertyName=ranger.unixauth.remote.login.enabled
                newPropertyValue="${remoteLoginEnabled}"
                updatePropertyToFilePy $propertyName $newPropertyValue $ldap_file

                propertyName=ranger.unixauth.service.hostname
                newPropertyValue="${authServiceHostName}"
                updatePropertyToFilePy $propertyName $newPropertyValue $ldap_file

                propertyName=ranger.unixauth.service.port
                newPropertyValue="${authServicePort}"
                updatePropertyToFilePy $propertyName $newPropertyValue $ldap_file
	else
		log "[E] $ldap_file does not exists" ; exit 1;
	fi
}

do_authentication_setup(){
	log "[I] Starting setup based on user authentication method=$authentication_method";
	./setup_authentication.sh $authentication_method $app_home

        if [ $authentication_method = "LDAP" ] ; then
	log "[I] Loading LDAP attributes and properties";
		newPropertyValue=''
		ldap_file=$app_home/WEB-INF/classes/conf/ranger-admin-site.xml
		if test -f $ldap_file; then
			log "[I] $ldap_file file found"
			propertyName=ranger.ldap.url
			newPropertyValue="${xa_ldap_url}"
			updatePropertyToFilePy $propertyName $newPropertyValue $ldap_file

			propertyName=ranger.ldap.user.dnpattern
			newPropertyValue="${xa_ldap_userDNpattern}"
			updatePropertyToFilePy $propertyName $newPropertyValue $ldap_file

			propertyName=ranger.ldap.group.searchbase
			newPropertyValue="${xa_ldap_groupSearchBase}"
			updatePropertyToFilePy $propertyName $newPropertyValue $ldap_file

			propertyName=ranger.ldap.group.searchfilter
			newPropertyValue="${xa_ldap_groupSearchFilter}"
			updatePropertyToFilePy $propertyName $newPropertyValue $ldap_file

			propertyName=ranger.ldap.group.roleattribute
			newPropertyValue="${xa_ldap_groupRoleAttribute}"
			updatePropertyToFilePy $propertyName $newPropertyValue $ldap_file

			propertyName=ranger.authentication.method
			newPropertyValue="${authentication_method}"
			updatePropertyToFilePy $propertyName $newPropertyValue $ldap_file

			if [ "${xa_ldap_base_dn}" != "" ] && [ "${xa_ldap_bind_dn}" != "" ]  && [ "${xa_ldap_bind_password}" != "" ]
			then
				$PYTHON_COMMAND_INVOKER dba_script.py ${xa_ldap_bind_password} 'LDAP' 'password_validation'
				if [ "$?" != "0" ]
				then
					exit 1
				fi

				propertyName=ranger.ldap.base.dn
				newPropertyValue="${xa_ldap_base_dn}"
				updatePropertyToFilePy $propertyName $newPropertyValue $ldap_file

				propertyName=ranger.ldap.bind.dn
				newPropertyValue="${xa_ldap_bind_dn}"
				updatePropertyToFilePy $propertyName $newPropertyValue $ldap_file

				propertyName=ranger.ldap.referral
				newPropertyValue="${xa_ldap_referral}"
				updatePropertyToFilePy $propertyName $newPropertyValue $ldap_file

				propertyName=ranger.ldap.user.searchfilter
				newPropertyValue="${xa_ldap_userSearchFilter}"
				updatePropertyToFilePy $propertyName $newPropertyValue $ldap_file

				keystore="${cred_keystore_filename}"

				if [ "${keystore}" != "" ]
				then
					mkdir -p `dirname "${keystore}"`

					ldap_password_alias=ranger.ldap.binddn.password
					$PYTHON_COMMAND_INVOKER ranger_credential_helper.py -l "cred/lib/*" -f "$keystore" -k "$ldap_password_alias" -v "$xa_ldap_bind_password" -c 1

					to_file_default=$app_home/WEB-INF/classes/conf/ranger-admin-default-site.xml

					if test -f $to_file_default; then
						propertyName=ranger.credential.provider.path
						newPropertyValue="${keystore}"
						updatePropertyToFilePy $propertyName $newPropertyValue $to_file_default

						propertyName=ranger.ldap.binddn.credential.alias
						newPropertyValue="${ldap_password_alias}"
						updatePropertyToFilePy $propertyName $newPropertyValue $to_file_default

						propertyName=ranger.ldap.bind.password
						newPropertyValue="_"
						updatePropertyToFilePy $propertyName $newPropertyValue $ldap_file
					else
						log "[E] $to_file_default does not exists" ; exit 1;
					fi

					if test -f $keystore; then
						#echo "$keystore found."
						chown -R ${unix_user}:${unix_group} ${keystore}
						chmod 640 ${keystore}
					else
						propertyName=ranger.ldap.bind.password
						newPropertyValue="${xa_ldap_bind_password}"
						updatePropertyToFilePy $propertyName $newPropertyValue $ldap_file
					fi
				else
					propertyName=ranger.ldap.bind.password
					newPropertyValue="${xa_ldap_bind_password}"
					updatePropertyToFilePy $propertyName $newPropertyValue $ldap_file
				fi
			fi
		else
			log "[E] $ldap_file does not exists" ; exit 1;
                fi
	fi
        if [ $authentication_method = "ACTIVE_DIRECTORY" ] ; then
	log "[I] Loading ACTIVE DIRECTORY attributes and properties";
		newPropertyValue=''
		ldap_file=$app_home/WEB-INF/classes/conf/ranger-admin-site.xml
		if test -f $ldap_file; then
			log "[I] $ldap_file file found"
			propertyName=ranger.ldap.ad.url
			newPropertyValue="${xa_ldap_ad_url}"
			updatePropertyToFilePy $propertyName $newPropertyValue $ldap_file

			propertyName=ranger.ldap.ad.domain
			newPropertyValue="${xa_ldap_ad_domain}"
			updatePropertyToFilePy $propertyName $newPropertyValue $ldap_file

			propertyName=ranger.authentication.method
			newPropertyValue="${authentication_method}"
			updatePropertyToFilePy $propertyName $newPropertyValue $ldap_file

			if [ "${xa_ldap_ad_base_dn}" != "" ] && [ "${xa_ldap_ad_bind_dn}" != "" ]  && [ "${xa_ldap_ad_bind_password}" != "" ]
			then
				$PYTHON_COMMAND_INVOKER dba_script.py ${xa_ldap_ad_bind_password} 'AD' 'password_validation'
				if [ "$?" != "0" ]
				then
					exit 1
				fi
				propertyName=ranger.ldap.ad.base.dn
				newPropertyValue="${xa_ldap_ad_base_dn}"
				updatePropertyToFilePy $propertyName $newPropertyValue $ldap_file

				propertyName=ranger.ldap.ad.bind.dn
				newPropertyValue="${xa_ldap_ad_bind_dn}"
				updatePropertyToFilePy $propertyName $newPropertyValue $ldap_file

				propertyName=ranger.ldap.ad.referral
				newPropertyValue="${xa_ldap_ad_referral}"
				updatePropertyToFilePy $propertyName $newPropertyValue $ldap_file

				propertyName=ranger.ldap.ad.user.searchfilter
				newPropertyValue="${xa_ldap_ad_userSearchFilter}"
				updatePropertyToFilePy $propertyName $newPropertyValue $ldap_file

				keystore="${cred_keystore_filename}"

				if [ "${keystore}" != "" ]
				then
					mkdir -p `dirname "${keystore}"`

					ad_password_alias=ranger.ad.binddn.password
					$PYTHON_COMMAND_INVOKER ranger_credential_helper.py -l "cred/lib/*" -f "$keystore" -k "$ad_password_alias" -v "$xa_ldap_ad_bind_password" -c 1

					to_file_default=$app_home/WEB-INF/classes/conf/ranger-admin-default-site.xml

					if test -f $to_file_default; then
						propertyName=ranger.credential.provider.path
						newPropertyValue="${keystore}"
						updatePropertyToFilePy $propertyName $newPropertyValue $to_file_default

						propertyName=ranger.ldap.ad.binddn.credential.alias
						newPropertyValue="${ad_password_alias}"
						updatePropertyToFilePy $propertyName $newPropertyValue $to_file_default

						propertyName=ranger.ldap.ad.bind.password
						newPropertyValue="_"
						updatePropertyToFilePy $propertyName $newPropertyValue $ldap_file
					else
						log "[E] $to_file_default does not exists" ; exit 1;
					fi

					if test -f $keystore; then
						#echo "$keystore found."
						chown -R ${unix_user}:${unix_group} ${keystore}
						chmod 640 ${keystore}
					else
						propertyName=ranger.ldap.ad.bind.password
						newPropertyValue="${xa_ldap_ad_bind_password}"
						updatePropertyToFilePy $propertyName $newPropertyValue $ldap_file
					fi
				else
					propertyName=ranger.ldap.ad.bind.password
					newPropertyValue="${xa_ldap_ad_bind_password}"
					updatePropertyToFilePy $propertyName $newPropertyValue $ldap_file
				fi
			fi
		else
			log "[E] $ldap_file does not exists" ; exit 1;
		fi
        fi
        if [ $authentication_method = "UNIX" ] ; then
                do_unixauth_setup
        fi

        if [ $authentication_method = "NONE" ] ; then
                newPropertyValue='NONE'
                ldap_file=$app_home/WEB-INF/classes/conf/ranger-admin-site.xml
                if test -f $ldap_file; then
                        propertyName=ranger.authentication.method
                        newPropertyValue="${authentication_method}"
                        updatePropertyToFilePy $propertyName $newPropertyValue $ldap_file
                fi
        fi
	
        log "[I] Finished setup based on user authentication method=$authentication_method";
}
#=====================================================================
setup_unix_user_group(){
	log "[I] Setting up UNIX user : ${unix_user} and group: ${unix_group}"

	#create group if it does not exist
	egrep "^$unix_group" /etc/group >& /dev/null
	if [ $? -ne 0 ]
	then
		groupadd ${unix_group}
		check_ret_status_for_groupadd $? "Creating group ${unix_group} failed"
	fi

	#create user if it does not exists
	id -u ${unix_user} > /dev/null 2>&1
	if [ $? -ne 0 ]
	then
		check_user_pwd ${unix_user_pwd}
                log "[I] Creating new user and adding to group"
                useradd ${unix_user} -g ${unix_group} -m
		check_ret_status $? "useradd ${unix_user} failed"

		passwdtmpfile=passwd.tmp
		if [  -f "$passwdtmpfile" ]; then
			rm -rf  ${passwdtmpfile}
		fi
		cat> ${passwdtmpfile} << EOF
${unix_user}:${unix_user_pwd}
EOF
		chpasswd <  ${passwdtmpfile}
		rm -rf  ${passwdtmpfile}
	else
                useringroup=`id ${unix_user}`
                useringrouparr=(${useringroup// / })
                if [[  ${useringrouparr[1]} =~ "(${unix_group})" ]]
		then
			log "[I] the ${unix_user} user already exists and belongs to group ${unix_group}"
		else
			log "[I] User already exists, adding it to group ${unix_group}"
			usermod -g ${unix_group} ${unix_user}
		fi
	fi
	log "[I] Setting up UNIX user : ${unix_user} and group: ${unix_group} DONE"
}

setup_install_files(){
	log "[I] Setting up installation files and directory";
	if [ ! -d ${WEBAPP_ROOT}/WEB-INF/classes/conf ]; then
                log "[I] Copying ${WEBAPP_ROOT}/WEB-INF/classes/conf.dist ${WEBAPP_ROOT}/WEB-INF/classes/conf"
                mkdir -p ${WEBAPP_ROOT}/WEB-INF/classes/conf
                cp ${WEBAPP_ROOT}/WEB-INF/classes/conf.dist/* ${WEBAPP_ROOT}/WEB-INF/classes/conf
	fi

        echo "export RANGER_HADOOP_CONF_DIR=${hadoop_conf}" > ${WEBAPP_ROOT}/WEB-INF/classes/conf/ranger-admin-env-hadoopconfdir.sh
        chmod a+rx ${WEBAPP_ROOT}/WEB-INF/classes/conf/ranger-admin-env-hadoopconfdir.sh
	
	hadoop_conf_file=${hadoop_conf}/core-site.xml
        ranger_hadoop_conf_file=${WEBAPP_ROOT}/WEB-INF/classes/conf/core-site.xml

	if [ -d ${WEBAPP_ROOT}/WEB-INF/classes/conf ]; then
		chown -R ${unix_user} ${WEBAPP_ROOT}/WEB-INF/classes/conf
		if [ "${hadoop_conf}" == "" ]
		then
			log "[WARN] Property hadoop_conf not found. Creating blank core-site.xml."
			echo "<configuration></configuration>" > ${ranger_hadoop_conf_file}
		else
			if [ -f ${hadoop_conf_file} ]; then
                                ln -sf ${hadoop_conf_file} ${ranger_hadoop_conf_file}
                        else
                                log "[WARN] core-site.xml file not found in provided hadoop_conf path. Creating blank core-site.xml"
				echo "<configuration></configuration>" > ${ranger_hadoop_conf_file}
                        fi
		fi
	fi

	if [ ! -d ${WEBAPP_ROOT}/WEB-INF/classes/lib ]; then
                log "[I] Creating ${WEBAPP_ROOT}/WEB-INF/classes/lib"
                mkdir -p ${WEBAPP_ROOT}/WEB-INF/classes/lib
	fi
	if [ -d ${WEBAPP_ROOT}/WEB-INF/classes/lib ]; then
		chown -R ${unix_user} ${WEBAPP_ROOT}/WEB-INF/classes/lib
	fi

	if [ -d /etc/init.d ]; then
                log "[I] Setting up init.d"
                cp ${INSTALL_DIR}/ews/${RANGER_ADMIN_INITD} /etc/init.d/${RANGER_ADMIN}
                chmod ug+rx /etc/init.d/${RANGER_ADMIN}

                if [ -d /etc/rc2.d ]
                then
		RC_DIR=/etc/rc2.d
		log "[I] Creating script S88${RANGER_ADMIN}/K90${RANGER_ADMIN} in $RC_DIR directory .... "
		rm -f $RC_DIR/S88${RANGER_ADMIN}  $RC_DIR/K90${RANGER_ADMIN}
		ln -s /etc/init.d/${RANGER_ADMIN} $RC_DIR/S88${RANGER_ADMIN}
		ln -s /etc/init.d/${RANGER_ADMIN} $RC_DIR/K90${RANGER_ADMIN}
                fi

                if [ -d /etc/rc3.d ]
                then
		RC_DIR=/etc/rc3.d
		log "[I] Creating script S88${RANGER_ADMIN}/K90${RANGER_ADMIN} in $RC_DIR directory .... "
		rm -f $RC_DIR/S88${RANGER_ADMIN}  $RC_DIR/K90${RANGER_ADMIN}
		ln -s /etc/init.d/${RANGER_ADMIN} $RC_DIR/S88${RANGER_ADMIN}
		ln -s /etc/init.d/${RANGER_ADMIN} $RC_DIR/K90${RANGER_ADMIN}
                fi

                # SUSE has rc2.d and rc3.d under /etc/rc.d
                if [ -d /etc/rc.d/rc2.d ]
                then
		RC_DIR=/etc/rc.d/rc2.d
		log "[I] Creating script S88${RANGER_ADMIN}/K90${RANGER_ADMIN} in $RC_DIR directory .... "
		rm -f $RC_DIR/S88${RANGER_ADMIN}  $RC_DIR/K90${RANGER_ADMIN}
		ln -s /etc/init.d/${RANGER_ADMIN} $RC_DIR/S88${RANGER_ADMIN}
		ln -s /etc/init.d/${RANGER_ADMIN} $RC_DIR/K90${RANGER_ADMIN}
                fi

                if [ -d /etc/rc.d/rc3.d ]
                then
		RC_DIR=/etc/rc.d/rc3.d
		log "[I] Creating script S88${RANGER_ADMIN}/K90${RANGER_ADMIN} in $RC_DIR directory .... "
		rm -f $RC_DIR/S88${RANGER_ADMIN}  $RC_DIR/K90${RANGER_ADMIN}
		ln -s /etc/init.d/${RANGER_ADMIN} $RC_DIR/S88${RANGER_ADMIN}
		ln -s /etc/init.d/${RANGER_ADMIN} $RC_DIR/K90${RANGER_ADMIN}
                fi
	fi
	if [  -f /etc/init.d/${RANGER_ADMIN} ]; then
		if [ "${unix_user}" != "" ]; then
			sed  's/^LINUX_USER=.*$/LINUX_USER='${unix_user}'/g' -i  /etc/init.d/${RANGER_ADMIN}
		fi
	fi

	if [ -z "${RANGER_ADMIN_LOGBACK_CONF_FILE}" ]; then
		RANGER_ADMIN_LOGBACK_CONF_FILE=${WEBAPP_ROOT}/WEB-INF/classes/conf/logback.xml
	fi
	echo "export RANGER_ADMIN_LOGBACK_CONF_FILE=${RANGER_ADMIN_LOGBACK_CONF_FILE}" > ${WEBAPP_ROOT}/WEB-INF/classes/conf/ranger-admin-env-logback-conf-file.sh
	chmod a+rx ${WEBAPP_ROOT}/WEB-INF/classes/conf/ranger-admin-env-logback-conf-file.sh
	log "[I] RANGER ADMIN LOGBACK CONF FILE : ${RANGER_ADMIN_LOGBACK_CONF_FILE}"

	if [ -z "${RANGER_ADMIN_LOG_DIR}" ] || [ ${RANGER_ADMIN_LOG_DIR} == ${XAPOLICYMGR_DIR} ]; then 
                RANGER_ADMIN_LOG_DIR=${XAPOLICYMGR_DIR}/ews/logs;
        fi

        if [ ! -d ${RANGER_ADMIN_LOG_DIR} ]; then
                log "[I] ${RANGER_ADMIN_LOG_DIR} Ranger Log folder"
                mkdir -p ${RANGER_ADMIN_LOG_DIR}
        fi

        if [ -d ${RANGER_ADMIN_LOG_DIR} ]; then
                chown -R ${unix_user} ${RANGER_ADMIN_LOG_DIR}
        fi

        echo "export RANGER_ADMIN_LOG_DIR=${RANGER_ADMIN_LOG_DIR}" > ${WEBAPP_ROOT}/WEB-INF/classes/conf/ranger-admin-env-logdir.sh
        chmod a+rx ${WEBAPP_ROOT}/WEB-INF/classes/conf/ranger-admin-env-logdir.sh
		
        if [ -z "${RANGER_PID_DIR_PATH}" ]
        then
                RANGER_PID_DIR_PATH=/var/run/ranger
        fi

        if [ ! -d ${RANGER_PID_DIR_PATH} ]; then
                log "[I]Creating Ranger PID folder: ${RANGER_PID_DIR_PATH}"
                mkdir -p ${RANGER_PID_DIR_PATH}
                if [ ! $? = "0" ];then
                        log "Make $RANGER_PID_DIR_PATH failure....!!";
                        exit 1;
		fi
        fi
		
        chown -R ${unix_user} ${RANGER_PID_DIR_PATH}
		
        echo "export RANGER_PID_DIR_PATH=${RANGER_PID_DIR_PATH}" > ${WEBAPP_ROOT}/WEB-INF/classes/conf/ranger-admin-env-piddir.sh
        echo "export RANGER_USER=${unix_user}" >> ${WEBAPP_ROOT}/WEB-INF/classes/conf/ranger-admin-env-piddir.sh
        chmod a+rx ${WEBAPP_ROOT}/WEB-INF/classes/conf/ranger-admin-env-piddir.sh

	if [ "${db_ssl_verifyServerCertificate}" == "true" ]
	then
		if [ "${db_ssl_auth_type}" == "1-way" ]
		then
			DB_SSL_PARAM="' -Djavax.net.ssl.trustStore=${javax_net_ssl_trustStore} -Djavax.net.ssl.trustStorePassword=${javax_net_ssl_trustStorePassword} -Djavax.net.ssl.trustStoreType=${javax_net_ssl_trustStore_type} '"
		else
			DB_SSL_PARAM="' -Djavax.net.ssl.keyStore=${javax_net_ssl_keyStore} -Djavax.net.ssl.keyStorePassword=${javax_net_ssl_keyStorePassword} -Djavax.net.ssl.keyStoreType={javax_net_ssl_keyStore_type}  -Djavax.net.ssl.trustStore=${javax_net_ssl_trustStore} -Djavax.net.ssl.trustStorePassword=${javax_net_ssl_trustStorePassword} -Djavax.net.ssl.trustStoreType=${javax_net_ssl_trustStore_type} '"
		fi
		echo "export DB_SSL_PARAM=${DB_SSL_PARAM}" > ${WEBAPP_ROOT}/WEB-INF/classes/conf/ranger-admin-env-dbsslparam.sh
                chmod a+rx ${WEBAPP_ROOT}/WEB-INF/classes/conf/ranger-admin-env-dbsslparam.sh
	else
		if [ -f ${WEBAPP_ROOT}/WEB-INF/classes/conf/ranger-admin-env-dbsslparam.sh ]; then
			DB_SSL_PARAM=""
			echo "export DB_SSL_PARAM=${DB_SSL_PARAM}" > ${WEBAPP_ROOT}/WEB-INF/classes/conf/ranger-admin-env-dbsslparam.sh
			chmod a+rx ${WEBAPP_ROOT}/WEB-INF/classes/conf/ranger-admin-env-dbsslparam.sh
		fi
	fi
	log "[I] Setting up installation files and directory DONE";

	if [ ! -f ${INSTALL_DIR}/rpm ]; then
                if [ -d ${INSTALL_DIR} ]
                then
		chown -R ${unix_user}:${unix_group} ${INSTALL_DIR}
		chown -R ${unix_user}:${unix_group} ${INSTALL_DIR}/*
                fi
	fi

	# Copy ranger-admin-services to /usr/bin
	if [ ! \( -e /usr/bin/ranger-admin \) ]
	then
		ln -sf ${INSTALL_DIR}/ews/ranger-admin-services.sh /usr/bin/ranger-admin
		chmod ug+rx /usr/bin/ranger-admin	
	fi
}
python_command_for_change_password(){
        $PYTHON_COMMAND_INVOKER db_setup.py -changepassword -pair "${1}" "${2}" "${3}" -pair "${4}" "${5}" "${6}" -pair "${7}" "${8}" "${9}" -pair "${10}" "${11}" "${12}"
}
validateDefaultUsersPassword(){
        if [ "${2}" == "" ]
        then
                log "[E] validatePassword(). Password for ${1} user cannot be blank"
                exit 1
        elif ! [[ ${#2} -ge 8 && "$2" =~ [A-Z] && "$2" =~ [a-z] && "$2" =~ [0-9] ]] || [[ "${2}" =~ [\"\`\\"'"] ]]
        then
                log "[E] validatePassword(). ${1} password change failed. Password should be minimum 8 characters, at least one uppercase letter, one lowercase letter and one numeric. Unsupported special characters are \\\`'\""
                exit 1
        fi
}

change_default_users_password(){
	if [ "${rangerAdmin_password}" != "admin" ] && [ "${rangerTagsync_password}" != "rangertagsync" ] && [ "${rangerUsersync_password}" != "rangerusersync" ] && [ "${keyadmin_password}" != "keyadmin" ]
	then
		python_command_for_change_password  'admin' 'admin' "${rangerAdmin_password}" 'rangertagsync' 'rangertagsync' "${rangerTagsync_password}" 'rangerusersync' 'rangerusersync' "${rangerUsersync_password}" 'keyadmin' 'keyadmin' "${keyadmin_password}"
		if [ "$?" != "0" ]
		then
			exit 1
		fi
	else
		if [ "${rangerAdmin_password}" != "admin" ]
		then
			python_command_for_change_password  'admin' 'admin' "${rangerAdmin_password}"
			if [ "$?" != "0" ]
			then
				exit 1
			fi
		fi
		if [ "${rangerTagsync_password}" != "rangertagsync" ]
		then
			python_command_for_change_password 'rangertagsync' 'rangertagsync' "${rangerTagsync_password}"
			if [ "$?" != "0" ]
			then
				exit 1
			fi
		fi
		if [ "${rangerUsersync_password}" != "rangerusersync" ]
		then
			python_command_for_change_password 'rangerusersync' 'rangerusersync' "${rangerUsersync_password}"
			if [ "$?" != "0" ]
			then
				exit 1
			fi
		fi
		if [ "${keyadmin_password}" != "keyadmin" ]
		then
			python_command_for_change_password 'keyadmin' 'keyadmin' "${keyadmin_password}"
			if [ "$?" != "0" ]
			then
				exit 1
			fi
		fi
	fi
}
log " --------- Running Ranger PolicyManager Web Application Install Script --------- "
log "[I] uname=`uname`"
log "[I] hostname=`hostname`"
init_variables
get_distro
check_java_version
check_db_connector
setup_unix_user_group
setup_install_files
sanity_check_files
copy_db_connector
check_python_command
check_ranger_version
if [ "$?" != "0" ]
then
	validateDefaultUsersPassword 'admin' "${rangerAdmin_password}"
	validateDefaultUsersPassword 'rangertagsync' "${rangerTagsync_password}"
	validateDefaultUsersPassword 'rangerusersync' "${rangerUsersync_password}"
	validateDefaultUsersPassword 'keyadmin' "${keyadmin_password}"
fi
run_dba_steps
if [ "$?" == "0" ]
then
	update_properties
	do_authentication_setup
else
	log "[E] DB schema setup failed! Please contact Administrator."
	exit 1
fi
if [ "$?" == "0" ]
then
	echo "ln -sf ${WEBAPP_ROOT}/WEB-INF/classes/conf ${INSTALL_DIR}/conf"
	ln -sf ${WEBAPP_ROOT}/WEB-INF/classes/conf ${INSTALL_DIR}/conf
else
	exit 1
fi
if [ "$?" == "0" ]
then
	$PYTHON_COMMAND_INVOKER db_setup.py
	if [ "$?" == "0" ]
	then
		$PYTHON_COMMAND_INVOKER db_setup.py -javapatch
		if [ "$?" == "0" ]
		then
			change_default_users_password
		fi
	else
		exit 1
	fi
else
	exit 1
fi
echo "Installation of Ranger PolicyManager Web Application is completed."
