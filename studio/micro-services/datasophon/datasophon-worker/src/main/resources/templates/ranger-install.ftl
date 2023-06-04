PYTHON_COMMAND_INVOKER=python
#DB_FLAVOR=MYSQL|ORACLE|POSTGRES|MSSQL|SQLA
DB_FLAVOR=MYSQL

SQL_CONNECTOR_JAR=${rangerHome}/mysql-connector-java-5.1.34.jar

db_root_user=root
db_root_password=${rootPassword}
db_host=${dbHost}
#SSL config
db_ssl_enabled=false
db_ssl_required=false
db_ssl_verifyServerCertificate=false
#db_ssl_auth_type=1-way|2-way, where 1-way represents standard one way ssl authentication and 2-way represents mutual ssl authentication
db_ssl_auth_type=2-way
javax_net_ssl_keyStore=
javax_net_ssl_keyStorePassword=
javax_net_ssl_trustStore=
javax_net_ssl_trustStorePassword=
#
# DB UserId used for the Ranger schema
#
db_name=${database}
db_user=${rangerUser}
db_password=${rangerPassword}


rangerAdmin_password=admin123
rangerTagsync_password=admin123
rangerUsersync_password=admin123
keyadmin_password=admin123


#Source for Audit Store. Currently solr and elasticsearch are supported.
# * audit_store is solr
#audit_store=solr

# * audit_solr_url Elasticsearch Host(s). E.g. 127.0.0.1
audit_elasticsearch_urls=
audit_elasticsearch_port=
audit_elasticsearch_protocol=
audit_elasticsearch_user=
audit_elasticsearch_password=
audit_elasticsearch_index=
audit_elasticsearch_bootstrap_enabled=true

audit_solr_urls=
audit_solr_user=
audit_solr_password=
audit_solr_zookeepers=

audit_solr_collection_name=ranger_audits
#solr Properties for cloud mode
audit_solr_config_name=ranger_audits
audit_solr_no_shards=1
audit_solr_no_replica=1
audit_solr_max_shards_per_node=1
audit_solr_acl_user_list_sasl=solr,infra-solr
audit_solr_bootstrap_enabled=true

#------------------------- DB CONFIG - END ----------------------------------

#
# ------- PolicyManager CONFIG ----------------
#

policymgr_external_url=${rangerAdminUrl}
policymgr_http_enabled=true
policymgr_https_keystore_file=
policymgr_https_keystore_keyalias=rangeradmin
policymgr_https_keystore_password=

#Add Supported Components list below separated by semi-colon, default value is empty string to support all components
#Example :  policymgr_supportedcomponents=hive,hbase,hdfs
policymgr_supportedcomponents=


unix_user=ranger
unix_user_pwd=ranger
unix_group=ranger

#LDAP|ACTIVE_DIRECTORY|UNIX|NONE
authentication_method=NONE
remoteLoginEnabled=true
authServiceHostName=localhost
authServicePort=5151
ranger_unixauth_keystore=keystore.jks
ranger_unixauth_keystore_password=password
ranger_unixauth_truststore=cacerts
ranger_unixauth_truststore_password=changeit

####LDAP settings - Required only if have selected LDAP authentication ####
#
# Sample Settings
#
#xa_ldap_url=ldap://127.0.0.1:389
#xa_ldap_userDNpattern=uid={0},ou=users,dc=xasecure,dc=net
#xa_ldap_groupSearchBase=ou=groups,dc=xasecure,dc=net
#xa_ldap_groupSearchFilter=(member=uid={0},ou=users,dc=xasecure,dc=net)
#xa_ldap_groupRoleAttribute=cn
#xa_ldap_base_dn=dc=xasecure,dc=net
#xa_ldap_bind_dn=cn=admin,ou=users,dc=xasecure,dc=net
#xa_ldap_bind_password=
#xa_ldap_referral=follow|ignore
#xa_ldap_userSearchFilter=(uid={0})

xa_ldap_url=
xa_ldap_userDNpattern=
xa_ldap_groupSearchBase=
xa_ldap_groupSearchFilter=
xa_ldap_groupRoleAttribute=
xa_ldap_base_dn=
xa_ldap_bind_dn=
xa_ldap_bind_password=
xa_ldap_referral=
xa_ldap_userSearchFilter=
####ACTIVE_DIRECTORY settings - Required only if have selected AD authentication ####
#
# Sample Settings
#
#xa_ldap_ad_domain=xasecure.net
#xa_ldap_ad_url=ldap://127.0.0.1:389
#xa_ldap_ad_base_dn=dc=xasecure,dc=net
#xa_ldap_ad_bind_dn=cn=administrator,ou=users,dc=xasecure,dc=net
#xa_ldap_ad_bind_password=
#xa_ldap_ad_referral=follow|ignore
#xa_ldap_ad_userSearchFilter=(sAMAccountName={0})

xa_ldap_ad_domain=
xa_ldap_ad_url=
xa_ldap_ad_base_dn=
xa_ldap_ad_bind_dn=
xa_ldap_ad_bind_password=
xa_ldap_ad_referral=
xa_ldap_ad_userSearchFilter=

#------------ Kerberos Config -----------------
spnego_principal=<#if spnegoPrincipal??>${spnegoPrincipal}</#if>
spnego_keytab=<#if spnegoKeytab??>${spnegoKeytab}</#if>
token_valid=30
cookie_domain=
cookie_path=/
admin_principal=<#if adminPrincipal??>${adminPrincipal}</#if>
admin_keytab=<#if adminKeytab??>${adminKeytab}</#if>
lookup_principal=<#if adminPrincipal??>${adminPrincipal}</#if>
lookup_keytab=<#if adminKeytab??>${adminKeytab}</#if>
hadoop_conf=<#if hadoopHome??>${hadoopHome}</#if>/etc/hadoop/conf
#
#-------- SSO CONFIG - Start ------------------
#
sso_enabled=false
sso_providerurl=https://127.0.0.1:8443/gateway/knoxsso/api/v1/websso
sso_publickey=

#
#-------- SSO CONFIG - END ------------------

# Custom log directory path
RANGER_ADMIN_LOG_DIR=$PWD

# PID file path
RANGER_PID_DIR_PATH=/var/run/ranger

# #################  DO NOT MODIFY ANY VARIABLES BELOW #########################
#
# --- These deployment variables are not to be modified unless you understand the full impact of the changes
#
################################################################################
XAPOLICYMGR_DIR=$PWD
app_home=$PWD/ews/webapp
TMPFILE=$PWD/.fi_tmp
LOGFILE=$PWD/logfile
LOGFILES="$LOGFILE"

JAVA_BIN='java'
JAVA_VERSION_REQUIRED='1.8'
JAVA_ORACLE='Java(TM) SE Runtime Environment'

ranger_admin_max_heap_size=1g
#retry DB and Java patches after the given time in seconds.
PATCH_RETRY_INTERVAL=120
STALE_PATCH_ENTRY_HOLD_TIME=10

#mysql_create_user_file="${r"${PWD}"}/db/mysql/create_dev_user.sql
mysql_core_file=db/mysql/optimized/current/ranger_core_db_mysql.sql
mysql_audit_file=db/mysql/xa_audit_db.sql
#mysql_asset_file="${r"${PWD}"}/db/mysql/reset_asset.sql

#oracle_create_user_file="${r"${PWD}"}/db/oracle/create_dev_user_oracle.sql
oracle_core_file=db/oracle/optimized/current/ranger_core_db_oracle.sql
oracle_audit_file=db/oracle/xa_audit_db_oracle.sql
#oracle_asset_file="${r"${PWD}"}/db/oracle/reset_asset_oracle.sql
#
postgres_core_file=db/postgres/optimized/current/ranger_core_db_postgres.sql
postgres_audit_file=db/postgres/xa_audit_db_postgres.sql
#
sqlserver_core_file=db/sqlserver/optimized/current/ranger_core_db_sqlserver.sql
sqlserver_audit_file=db/sqlserver/xa_audit_db_sqlserver.sql
#
sqlanywhere_core_file=db/sqlanywhere/optimized/current/ranger_core_db_sqlanywhere.sql
sqlanywhere_audit_file=db/sqlanywhere/xa_audit_db_sqlanywhere.sql
cred_keystore_filename=$app_home/WEB-INF/classes/conf/.jceks/rangeradmin.jceks