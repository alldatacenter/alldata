#!/usr/bin/env python
"""
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

"""

import status_params
import ambari_simplejson as json # simplejson is much faster comparing to Python 2.6 json module and has the same functions set.
import os

from urlparse import urlparse

from ambari_commons.constants import AMBARI_SUDO_BINARY
from ambari_commons.os_check import OSCheck

from resource_management.libraries.resources.hdfs_resource import HdfsResource
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.is_empty import is_empty
from resource_management.libraries.functions.copy_tarball import STACK_ROOT_PATTERN, STACK_NAME_PATTERN, STACK_VERSION_PATTERN
from resource_management.libraries.functions import get_kinit_path
from resource_management.libraries.functions.get_not_managed_resources import get_not_managed_resources
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.functions import upgrade_summary
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions.stack_features import get_stack_feature_version
from resource_management.libraries.functions.get_port_from_url import get_port_from_url
from resource_management.libraries.functions.expect import expect
from resource_management.libraries import functions
from resource_management.libraries.functions.setup_atlas_hook import has_atlas_in_cluster

# Default log4j version; put config files under /etc/hive/conf
log4j_version = '1'

# server configurations
config = Script.get_config()
tmp_dir = Script.get_tmp_dir()
sudo = AMBARI_SUDO_BINARY

stack_root = status_params.stack_root
stack_name = status_params.stack_name
stack_name_uppercase = stack_name.upper()
agent_stack_retry_on_unavailability = config['hostLevelParams']['agent_stack_retry_on_unavailability']
agent_stack_retry_count = expect("/hostLevelParams/agent_stack_retry_count", int)

# Needed since this is an Atlas Hook service.
cluster_name = config['clusterName']

# node hostname
hostname = config["hostname"]

# This is expected to be of the form #.#.#.#
stack_version_unformatted = status_params.stack_version_unformatted
stack_version_formatted_major = status_params.stack_version_formatted_major

# this is not available on INSTALL action because <stack-selector-tool> is not available
stack_version_formatted = functions.get_stack_version('hive-server2')

# New Cluster Stack Version that is defined during the RESTART of a Rolling Upgrade.
# It cannot be used during the initial Cluser Install because the version is not yet known.
version = default("/commandParams/version", None)

# get the correct version to use for checking stack features
version_for_stack_feature_checks = get_stack_feature_version(config)

# Upgrade direction
upgrade_direction = default("/commandParams/upgrade_direction", None)
stack_supports_ranger_kerberos = check_stack_feature(StackFeature.RANGER_KERBEROS_SUPPORT, version_for_stack_feature_checks)
stack_supports_ranger_audit_db = check_stack_feature(StackFeature.RANGER_AUDIT_DB_SUPPORT, version_for_stack_feature_checks)

# component ROLE directory (like hive-metastore or hive-server2-hive2)
component_directory = status_params.component_directory
component_directory_interactive = status_params.component_directory_interactive

hadoop_home = '/usr/lib/hadoop'
hive_bin = '/usr/lib/hive/bin'
hive_schematool_ver_bin = '/usr/lib/hive/bin'
hive_schematool_bin = '/usr/lib/hive/bin'
hive_lib = '/usr/lib/hive/lib'
hive_version_lib = '/usr/lib/hive/lib'
#hadoop_home = format('{stack_root}/current/hadoop-client')
#hive_bin = format('{stack_root}/current/{component_directory}/bin')
#hive_schematool_ver_bin = format('{stack_root}/{version}/hive/bin')
#hive_schematool_bin = format('{stack_root}/current/{component_directory}/bin')
#hive_lib = format('{stack_root}/current/{component_directory}/lib')
#hive_version_lib = format('{stack_root}/{version}/hive/lib')
hive_var_lib = '/var/lib/hive'
hive_user_home_dir = "/home/hive"

# starting on stacks where HSI is supported, we need to begin using the 'hive2' schematool
hive_server2_hive2_dir = None
hive_server2_hive2_lib = None

version = default("/commandParams/version", None)

if check_stack_feature(StackFeature.HIVE_SERVER_INTERACTIVE, version_for_stack_feature_checks):
  # the name of the hiveserver2-hive2 component
  hive_server2_hive2_component = status_params.SERVER_ROLE_DIRECTORY_MAP["HIVE_SERVER_INTERACTIVE"]

  # when using the version, we can just specify the component as "hive2"
  hive_schematool_ver_bin = format('{stack_root}/{version}/hive2/bin')

  # use the schematool which ships with hive2
  hive_schematool_bin = format('{stack_root}/current/{hive_server2_hive2_component}/bin')

  # <stack-root>/<version>/hive2 (as opposed to <stack-root>/<version>/hive)
  hive_server2_hive2_dir = format('{stack_root}/current/{hive_server2_hive2_component}')

  # <stack-root>/<version>/hive2 (as opposed to <stack-root>/<version>/hive)
  hive_server2_hive2_version_dir = format('{stack_root}/{version}/hive2')

  # <stack-root>/current/hive-server2-hive2/lib -> <stack-root>/<version>/hive2/lib
  hive_server2_hive2_lib = format('{hive_server2_hive2_dir}/lib')

  # <stack-root>/<version>/hive2/lib
  hive_server2_hive2_version_lib = format('{hive_server2_hive2_version_dir}/lib')


hive_interactive_bin = format('{stack_root}/current/{component_directory_interactive}/bin')
hive_interactive_lib = format('{stack_root}/current/{component_directory_interactive}/lib')

# Hive Interactive related paths
hive_interactive_var_lib = '/var/lib/hive2'

# These tar folders were used in previous stack versions, e.g., HDP 2.1
hadoop_streaming_jars = '/usr/lib/hadoop-mapreduce/hadoop-streaming-*.jar'
pig_tar_file = format('/usr/share/{stack_name_uppercase}-webhcat/pig.tar.gz')
hive_tar_file = format('/usr/share/{stack_name_uppercase}-webhcat/hive.tar.gz')
sqoop_tar_file = format('/usr/share/{stack_name_uppercase}-webhcat/sqoop*.tar.gz')

hive_metastore_site_supported = False
hive_etc_dir_prefix = "/etc/hive"
hive_interactive_etc_dir_prefix = "/etc/hive2"
limits_conf_dir = "/etc/security/limits.d"

hive_user_nofile_limit = default("/configurations/hive-env/hive_user_nofile_limit", "32000")
hive_user_nproc_limit = default("/configurations/hive-env/hive_user_nproc_limit", "16000")

# use the directories from status_params as they are already calculated for
# the correct stack version
hadoop_conf_dir = status_params.hadoop_conf_dir
hadoop_bin_dir = status_params.hadoop_bin_dir
webhcat_conf_dir = status_params.webhcat_conf_dir
hive_conf_dir = status_params.hive_conf_dir
hive_home_dir = status_params.hive_home_dir
hive_config_dir = status_params.hive_config_dir
hive_client_conf_dir = status_params.hive_client_conf_dir
hive_server_conf_dir = status_params.hive_server_conf_dir

hcat_conf_dir = '/etc/hive-hcatalog/conf'
config_dir = '/etc/hive-webhcat/conf'
hcat_lib = '/usr/lib/hive-hcatalog/share/hcatalog'
webhcat_bin_dir = '/usr/lib/hive-hcatalog/sbin'

# --- Tarballs ---
# DON'T CHANGE THESE VARIABLE NAMES
# Values don't change from those in copy_tarball.py
webhcat_apps_dir = "/apps/webhcat"
hive_tar_source = "{0}/{1}/hive/hive.tar.gz".format(STACK_ROOT_PATTERN, STACK_VERSION_PATTERN)
pig_tar_source = "{0}/{1}/pig/pig.tar.gz".format(STACK_ROOT_PATTERN, STACK_VERSION_PATTERN)
hive_tar_dest_file = "/{0}/apps/{1}/hive/hive.tar.gz".format(STACK_NAME_PATTERN,STACK_VERSION_PATTERN)
pig_tar_dest_file = "/{0}/apps/{1}/pig/pig.tar.gz".format(STACK_NAME_PATTERN, STACK_VERSION_PATTERN)

hadoop_streaming_tar_source = "{0}/{1}/hadoop-mapreduce/hadoop-streaming.jar".format(STACK_ROOT_PATTERN, STACK_VERSION_PATTERN)
sqoop_tar_source = "{0}/{1}/sqoop/sqoop.tar.gz".format(STACK_ROOT_PATTERN, STACK_VERSION_PATTERN)
hadoop_streaming_tar_dest_dir = "/{0}/apps/{1}/mapreduce/".format(STACK_NAME_PATTERN,STACK_VERSION_PATTERN)
sqoop_tar_dest_dir = "/{0}/apps/{1}/sqoop/".format(STACK_NAME_PATTERN, STACK_VERSION_PATTERN)

tarballs_mode = 0444

purge_tables = "false"
# Starting from stack version for feature hive_purge_table drop should be executed with purge
purge_tables = 'true'

# this is NOT a typo.  Configs for hcatalog/webhcat point to a
# specific directory which is NOT called 'conf'
# FIXME: ODPi
# hcat_conf_dir = format('{stack_root}/current/hive-webhcat/etc/hcatalog')
# config_dir = format('{stack_root}/current/hive-webhcat/etc/webhcat')
hcat_conf_dir = format('/etc/hive-hcatalog/conf')
config_dir = format('/etc/hive-webhcat/conf')

hive_metastore_site_supported = True

execute_path = os.environ['PATH'] + os.pathsep + hive_bin + os.pathsep + hadoop_bin_dir

hive_metastore_user_name = config['configurations']['hive-site']['javax.jdo.option.ConnectionUserName']
hive_jdbc_connection_url = config['configurations']['hive-site']['javax.jdo.option.ConnectionURL']

hive_metastore_user_passwd = config['configurations']['hive-site']['javax.jdo.option.ConnectionPassword']
hive_metastore_user_passwd = unicode(hive_metastore_user_passwd) if not is_empty(hive_metastore_user_passwd) else hive_metastore_user_passwd
hive_metastore_db_type = config['configurations']['hive-env']['hive_database_type']

#HACK Temporarily use dbType=azuredb while invoking schematool
if hive_metastore_db_type == "mssql":
  hive_metastore_db_type = "azuredb"

#users
hive_user = config['configurations']['hive-env']['hive_user']

#JDBC driver jar name
hive_jdbc_driver = config['configurations']['hive-site']['javax.jdo.option.ConnectionDriverName']
jdk_location = config['hostLevelParams']['jdk_location']
java_share_dir = '/usr/share/java'
hive_database_name = config['configurations']['hive-env']['hive_database_name']
hive_database = config['configurations']['hive-env']['hive_database']
hive_use_existing_db = hive_database.startswith('Existing')

default_connectors_map = { "com.microsoft.sqlserver.jdbc.SQLServerDriver":"sqljdbc4.jar",
                           "com.mysql.jdbc.Driver":"mysql-connector-java.jar",
                           "org.postgresql.Driver":"postgresql-jdbc.jar",
                           "oracle.jdbc.driver.OracleDriver":"ojdbc.jar",
                           "sap.jdbc4.sqlanywhere.IDriver":"sajdbc4.jar"}

# NOT SURE THAT IT'S A GOOD IDEA TO USE PATH TO CLASS IN DRIVER, MAYBE IT WILL BE BETTER TO USE DB TYPE.
# BECAUSE PATH TO CLASSES COULD BE CHANGED
sqla_db_used = False
hive_previous_jdbc_jar_name = None
if hive_jdbc_driver == "com.microsoft.sqlserver.jdbc.SQLServerDriver":
  jdbc_jar_name = default("/hostLevelParams/custom_mssql_jdbc_name", None)
  hive_previous_jdbc_jar_name = default("/hostLevelParams/previous_custom_mssql_jdbc_name", None)
elif hive_jdbc_driver == "com.mysql.jdbc.Driver":
  jdbc_jar_name = default("/hostLevelParams/custom_mysql_jdbc_name", None)
  hive_previous_jdbc_jar_name = default("/hostLevelParams/previous_custom_mysql_jdbc_name", None)
elif hive_jdbc_driver == "org.postgresql.Driver":
  jdbc_jar_name = default("/hostLevelParams/custom_postgres_jdbc_name", None)
  hive_previous_jdbc_jar_name = default("/hostLevelParams/previous_custom_postgres_jdbc_name", None)
elif hive_jdbc_driver == "oracle.jdbc.driver.OracleDriver":
  jdbc_jar_name = default("/hostLevelParams/custom_oracle_jdbc_name", None)
  hive_previous_jdbc_jar_name = default("/hostLevelParams/previous_custom_oracle_jdbc_name", None)
elif hive_jdbc_driver == "sap.jdbc4.sqlanywhere.IDriver":
  jdbc_jar_name = default("/hostLevelParams/custom_sqlanywhere_jdbc_name", None)
  hive_previous_jdbc_jar_name = default("/hostLevelParams/previous_custom_sqlanywhere_jdbc_name", None)
  sqla_db_used = True

default_mysql_jar_name = "mysql-connector-java.jar"
default_mysql_target = format("{hive_lib}/{default_mysql_jar_name}")
hive_previous_jdbc_jar = format("{hive_lib}/{hive_previous_jdbc_jar_name}")
if not hive_use_existing_db:
  jdbc_jar_name = default_mysql_jar_name


downloaded_custom_connector = format("{tmp_dir}/{jdbc_jar_name}")

hive_jdbc_target = format("{hive_lib}/{jdbc_jar_name}")
hive2_jdbc_target = None
if hive_server2_hive2_dir:
  hive2_jdbc_target = format("{hive_server2_hive2_lib}/{jdbc_jar_name}")

# during upgrade / downgrade, use the specific version to copy the JDBC JAR to
if upgrade_direction:
  hive_jdbc_target = format("{hive_version_lib}/{jdbc_jar_name}")
  hive2_jdbc_target = format("{hive_server2_hive2_version_lib}/{jdbc_jar_name}") if hive2_jdbc_target is not None else None


hive2_previous_jdbc_jar = format("{hive_server2_hive2_lib}/{hive_previous_jdbc_jar_name}") if hive_server2_hive2_lib is not None else None
driver_curl_source = format("{jdk_location}/{jdbc_jar_name}")

# normally, the JDBC driver would be referenced by <stack-root>/current/.../foo.jar
# but in RU if <stack-selector-tool> is called and the restart fails, then this means that current pointer
# is now pointing to the upgraded version location; that's bad for the cp command
version_for_source_jdbc_file = upgrade_summary.get_source_version(default_version = version_for_stack_feature_checks)
source_jdbc_file = format("{stack_root}/{version_for_source_jdbc_file}/hive/lib/{jdbc_jar_name}")

check_db_connection_jar_name = "DBConnectionVerification.jar"
check_db_connection_jar = format("/usr/lib/ambari-agent/{check_db_connection_jar_name}")
hive_jdbc_drivers_list = ["com.microsoft.sqlserver.jdbc.SQLServerDriver","com.mysql.jdbc.Driver",
                          "org.postgresql.Driver","oracle.jdbc.driver.OracleDriver","sap.jdbc4.sqlanywhere.IDriver"]

prepackaged_jdbc_name = "ojdbc6.jar"
prepackaged_ojdbc_symlink = format("{hive_lib}/{prepackaged_jdbc_name}")
templeton_port = config['configurations']['webhcat-site']['templeton.port']

#constants for type2 jdbc
jdbc_libs_dir = format("{hive_lib}/native/lib64")
lib_dir_available = os.path.exists(jdbc_libs_dir)

if sqla_db_used:
  jars_path_in_archive = format("{tmp_dir}/sqla-client-jdbc/java/*")
  libs_path_in_archive = format("{tmp_dir}/sqla-client-jdbc/native/lib64/*")
  downloaded_custom_connector = format("{tmp_dir}/{jdbc_jar_name}")
  libs_in_hive_lib = format("{jdbc_libs_dir}/*")


# Start, Common Hosts and Ports
ambari_server_hostname = config['clusterHostInfo']['ambari_server_host'][0]

hive_metastore_hosts = default('/clusterHostInfo/hive_metastore_host', [])
hive_metastore_host = hive_metastore_hosts[0] if len(hive_metastore_hosts) > 0 else None
hive_metastore_port = get_port_from_url(config['configurations']['hive-site']['hive.metastore.uris'])

hive_server_hosts = default("/clusterHostInfo/hive_server_host", [])
hive_server_host = hive_server_hosts[0] if len(hive_server_hosts) > 0 else None

hive_server_interactive_hosts = default('/clusterHostInfo/hive_server_interactive_hosts', [])
hive_server_interactive_host = hive_server_interactive_hosts[0] if len(hive_server_interactive_hosts) > 0 else None
# End, Common Hosts and Ports

hive_transport_mode = config['configurations']['hive-site']['hive.server2.transport.mode']

if hive_transport_mode.lower() == "http":
  hive_server_port = config['configurations']['hive-site']['hive.server2.thrift.http.port']
else:
  hive_server_port = default('/configurations/hive-site/hive.server2.thrift.port',"10000")

hive_url = format("jdbc:hive2://{hive_server_host}:{hive_server_port}")
hive_http_endpoint = default('/configurations/hive-site/hive.server2.thrift.http.path', "cliservice")
hive_server_principal = config['configurations']['hive-site']['hive.server2.authentication.kerberos.principal']
hive_server2_authentication = config['configurations']['hive-site']['hive.server2.authentication']

# ssl options
hive_ssl = default('/configurations/hive-site/hive.server2.use.SSL', False)
hive_ssl_keystore_path = default('/configurations/hive-site/hive.server2.keystore.path', None)
hive_ssl_keystore_password = default('/configurations/hive-site/hive.server2.keystore.password', None)

smokeuser = config['configurations']['cluster-env']['smokeuser']
smoke_test_sql = format("{tmp_dir}/hiveserver2.sql")
smoke_test_path = format("{tmp_dir}/hiveserver2Smoke.sh")
smoke_user_keytab = config['configurations']['cluster-env']['smokeuser_keytab']
smokeuser_principal = config['configurations']['cluster-env']['smokeuser_principal_name']

fs_root = config['configurations']['core-site']['fs.defaultFS']
security_enabled = config['configurations']['cluster-env']['security_enabled']

kinit_path_local = get_kinit_path(default('/configurations/kerberos-env/executable_search_paths', None))
hive_metastore_keytab_path = config['configurations']['hive-site']['hive.metastore.kerberos.keytab.file']
hive_metastore_principal = config['configurations']['hive-site']['hive.metastore.kerberos.principal']

hive_server2_keytab = config['configurations']['hive-site']['hive.server2.authentication.kerberos.keytab']

#hive_env
hive_log_dir = config['configurations']['hive-env']['hive_log_dir']
hive_pid_dir = status_params.hive_pid_dir
hive_pid = status_params.hive_pid
hive_interactive_pid = status_params.hive_interactive_pid

#Default conf dir for client
hive_conf_dirs_list = [hive_client_conf_dir]

# These are the folders to which the configs will be written to.
ranger_hive_component = status_params.SERVER_ROLE_DIRECTORY_MAP['HIVE_SERVER']
if status_params.role == "HIVE_METASTORE" and hive_metastore_hosts is not None and hostname in hive_metastore_hosts:
  hive_conf_dirs_list.append(hive_server_conf_dir)
elif status_params.role == "HIVE_SERVER" and hive_server_hosts is not None and hostname in hive_server_host:
  hive_conf_dirs_list.append(hive_server_conf_dir)
elif status_params.role == "HIVE_SERVER_INTERACTIVE" and hive_server_interactive_hosts is not None and hostname in hive_server_interactive_hosts:
  hive_conf_dirs_list.append(status_params.hive_server_interactive_conf_dir)
  ranger_hive_component = status_params.SERVER_ROLE_DIRECTORY_MAP['HIVE_SERVER_INTERACTIVE']
# log4j version is 2 for hive2; put config files under /etc/hive2/conf
if status_params.role == "HIVE_SERVER_INTERACTIVE":
  log4j_version = '2'

#Starting hiveserver2
start_hiveserver2_script = 'startHiveserver2.sh.j2'

##Starting metastore
start_metastore_script = 'startMetastore.sh'
hive_metastore_pid = status_params.hive_metastore_pid

# Hive Server Interactive
slider_am_container_mb = default("/configurations/hive-interactive-env/slider_am_container_mb", 341)

hdfs_user = config['configurations']['hadoop-env']['hdfs_user']
yarn_user = config['configurations']['yarn-env']['yarn_user']
user_group = config['configurations']['cluster-env']['user_group']
artifact_dir = format("{tmp_dir}/AMBARI-artifacts/")
# Need this for yarn.nodemanager.recovery.dir in yarn-site
yarn_log_dir_prefix = config['configurations']['yarn-env']['yarn_log_dir_prefix']

target_hive_interactive = format("{hive_interactive_lib}/{jdbc_jar_name}")
hive_intaractive_previous_jdbc_jar = format("{hive_interactive_lib}/{hive_previous_jdbc_jar_name}")
jars_in_hive_lib = format("{hive_lib}/*.jar")

start_hiveserver2_path = format("{tmp_dir}/start_hiveserver2_script")
start_metastore_path = format("{tmp_dir}/start_metastore_script")

hadoop_heapsize = config['configurations']['hadoop-env']['hadoop_heapsize']

if 'role' in config and config['role'] in ["HIVE_SERVER", "HIVE_METASTORE"]:
  hive_heapsize = config['configurations']['hive-env']['hive.heapsize']
else:
  hive_heapsize = config['configurations']['hive-env']['hive.client.heapsize']

hive_metastore_heapsize = config['configurations']['hive-env']['hive.metastore.heapsize']

java64_home = config['hostLevelParams']['java_home']
java_version = expect("/hostLevelParams/java_version", int)

##### MYSQL
db_name = config['configurations']['hive-env']['hive_database_name']
mysql_group = 'mysql'
mysql_host = config['clusterHostInfo']['hive_mysql_host']

mysql_adduser_path = format("{tmp_dir}/addMysqlUser.sh")
mysql_deluser_path = format("{tmp_dir}/removeMysqlUser.sh")

#### Metastore
# initialize the schema only if not in an upgrade/downgrade
init_metastore_schema = upgrade_direction is None

########## HCAT
hcat_dbroot = hcat_lib

hcat_user = config['configurations']['hive-env']['hcat_user']
webhcat_user = config['configurations']['hive-env']['webhcat_user']

hcat_pid_dir = status_params.hcat_pid_dir
hcat_log_dir = config['configurations']['hive-env']['hcat_log_dir']
hcat_env_sh_template = config['configurations']['hcat-env']['content']

#hive-log4j.properties.template
if (('hive-log4j' in config['configurations']) and ('content' in config['configurations']['hive-log4j'])):
  log4j_props = config['configurations']['hive-log4j']['content']
else:
  log4j_props = None

#webhcat-log4j.properties.template
if (('webhcat-log4j' in config['configurations']) and ('content' in config['configurations']['webhcat-log4j'])):
  log4j_webhcat_props = config['configurations']['webhcat-log4j']['content']
else:
  log4j_webhcat_props = None

#hive-exec-log4j.properties.template
if (('hive-exec-log4j' in config['configurations']) and ('content' in config['configurations']['hive-exec-log4j'])):
  log4j_exec_props = config['configurations']['hive-exec-log4j']['content']
else:
  log4j_exec_props = None

daemon_name = status_params.daemon_name
process_name = status_params.process_name
hive_env_sh_template = config['configurations']['hive-env']['content']

hive_hdfs_user_dir = format("/user/{hive_user}")
hive_hdfs_user_mode = 0755
hive_apps_whs_dir = config['configurations']['hive-site']["hive.metastore.warehouse.dir"]
whs_dir_protocol = urlparse(hive_apps_whs_dir).scheme
hive_exec_scratchdir = config['configurations']['hive-site']["hive.exec.scratchdir"]
#for create_hdfs_directory
hdfs_user_keytab = config['configurations']['hadoop-env']['hdfs_user_keytab']
hdfs_principal_name = default('/configurations/hadoop-env/hdfs_principal_name', 'missing_principal').replace("_HOST", hostname)

# Tez-related properties
tez_user = config['configurations']['tez-env']['tez_user']

# Tez jars
tez_local_api_jars = '/usr/lib/tez/tez*.jar'
tez_local_lib_jars = '/usr/lib/tez/lib/*.jar'

# Tez libraries
tez_lib_uris = default("/configurations/tez-site/tez.lib.uris", None)

if OSCheck.is_ubuntu_family():
  mysql_configname = '/etc/mysql/my.cnf'
else:
  mysql_configname = '/etc/my.cnf'

mysql_user = 'mysql'

# Hive security
hive_authorization_enabled = config['configurations']['hive-site']['hive.security.authorization.enabled']

mysql_jdbc_driver_jar = "/usr/share/java/mysql-connector-java.jar"

hive_site_config = dict(config['configurations']['hive-site'])

########################################################
############# AMS related params #####################
########################################################
ams_collector_hosts = default("/clusterHostInfo/metrics_collector_hosts", [])
if 'cluster-env' in config['configurations'] and \
    'metrics_collector_external_hosts' in config['configurations']['cluster-env']:
  ams_collector_hosts = config['configurations']['cluster-env']['metrics_collector_external_hosts']
  set_instanceId = "true"
else:
  ams_collector_hosts = ",".join(default("/clusterHostInfo/metrics_collector_hosts", []))

has_metric_collector = not len(ams_collector_hosts) == 0
if has_metric_collector:
  metric_collector_host = ams_collector_hosts[0]
  if 'cluster-env' in config['configurations'] and \
      'metrics_collector_external_port' in config['configurations']['cluster-env']:
    metric_collector_port = config['configurations']['cluster-env']['metrics_collector_external_port']
  else:
    metric_collector_web_address = default("/configurations/ams-site/timeline.metrics.service.webapp.address", "0.0.0.0:6188")
    if metric_collector_web_address.find(':') != -1:
      metric_collector_port = metric_collector_web_address.split(':')[1]
    else:
      metric_collector_port = '6188'
  if default("/configurations/ams-site/timeline.metrics.service.http.policy", "HTTP_ONLY") == "HTTPS_ONLY":
    metric_collector_protocol = 'https'
  else:
    metric_collector_protocol = 'http'
  metric_truststore_path= default("/configurations/ams-ssl-client/ssl.client.truststore.location", "")
  metric_truststore_type= default("/configurations/ams-ssl-client/ssl.client.truststore.type", "")
  metric_truststore_password= default("/configurations/ams-ssl-client/ssl.client.truststore.password", "")

metrics_report_interval = default("/configurations/ams-site/timeline.metrics.sink.report.interval", 60)
metrics_collection_period = default("/configurations/ams-site/timeline.metrics.sink.collection.period", 10)

host_in_memory_aggregation = default("/configurations/ams-site/timeline.metrics.host.inmemory.aggregation", True)
host_in_memory_aggregation_port = default("/configurations/ams-site/timeline.metrics.host.inmemory.aggregation.port", 61888)
is_aggregation_https_enabled = False
if default("/configurations/ams-site/timeline.metrics.host.inmemory.aggregation.http.policy", "HTTP_ONLY") == "HTTPS_ONLY":
  host_in_memory_aggregation_protocol = 'https'
  is_aggregation_https_enabled = True
else:
  host_in_memory_aggregation_protocol = 'http'

########################################################
############# Atlas related params #####################
########################################################
#region Atlas Hooks
hive_atlas_application_properties = default('/configurations/hive-atlas-application.properties', {})

if has_atlas_in_cluster():
  atlas_hook_filename = default('/configurations/atlas-env/metadata_conf_file', 'atlas-application.properties')
#endregion

########################################################
########### WebHCat related params #####################
########################################################

webhcat_env_sh_template = config['configurations']['webhcat-env']['content']
templeton_log_dir = config['configurations']['hive-env']['hcat_log_dir']
templeton_pid_dir = status_params.hcat_pid_dir

webhcat_pid_file = status_params.webhcat_pid_file

templeton_jar = config['configurations']['webhcat-site']['templeton.jar']


webhcat_server_host = config['clusterHostInfo']['webhcat_server_host']

hcat_hdfs_user_dir = format("/user/{hcat_user}")
hcat_hdfs_user_mode = 0755
webhcat_hdfs_user_dir = format("/user/{webhcat_user}")
webhcat_hdfs_user_mode = 0755
#for create_hdfs_directory
security_param = "true" if security_enabled else "false"



hdfs_site = config['configurations']['hdfs-site']
default_fs = config['configurations']['core-site']['fs.defaultFS']

dfs_type = default("/commandParams/dfs_type", "")

import functools
#create partial functions with common arguments for every HdfsResource call
#to create hdfs directory we need to call params.HdfsResource in code
HdfsResource = functools.partial(
 HdfsResource,
  user = hdfs_user,
  hdfs_resource_ignore_file = "/var/lib/ambari-agent/data/.hdfs_resource_ignore",
  security_enabled = security_enabled,
  keytab = hdfs_user_keytab,
  kinit_path_local = kinit_path_local,
  hadoop_bin_dir = hadoop_bin_dir,
  hadoop_conf_dir = hadoop_conf_dir,
  principal_name = hdfs_principal_name,
  hdfs_site = hdfs_site,
  default_fs = default_fs,
  immutable_paths = get_not_managed_resources(),
  dfs_type = dfs_type
 )

# Hive Interactive related
hive_interactive_hosts = default('/clusterHostInfo/hive_server_interactive_hosts', [])
has_hive_interactive = len(hive_interactive_hosts) > 0
if has_hive_interactive:
  llap_daemon_log4j = config['configurations']['llap-daemon-log4j']['content']
  llap_cli_log4j2 = config['configurations']['llap-cli-log4j2']['content']
  hive_log4j2 = config['configurations']['hive-log4j2']['content']
  hive_exec_log4j2 = config['configurations']['hive-exec-log4j2']['content']
  beeline_log4j2 = config['configurations']['beeline-log4j2']['content']

  hive_server_interactive_conf_dir = status_params.hive_server_interactive_conf_dir
  execute_path_hive_interactive = os.path.join(os.environ['PATH'], hive_interactive_bin, hadoop_bin_dir)
  start_hiveserver2_interactive_script = 'startHiveserver2Interactive.sh.j2'
  start_hiveserver2_interactive_path = format("{tmp_dir}/start_hiveserver2_interactive_script")
  hive_interactive_env_sh_template = config['configurations']['hive-interactive-env']['content']
  hive_interactive_enabled = default('/configurations/hive-interactive-env/enable_hive_interactive', False)
  llap_app_java_opts = default('/configurations/hive-interactive-env/llap_java_opts', '-XX:+AlwaysPreTouch {% if java_version > 7 %}-XX:+UseG1GC -XX:TLABSize=8m -XX:+ResizeTLAB -XX:+UseNUMA -XX:+AggressiveOpts -XX:MetaspaceSize=1024m -XX:InitiatingHeapOccupancyPercent=80 -XX:MaxGCPauseMillis=200{% else %}-XX:+PrintGCDetails -verbose:gc -XX:+PrintGCTimeStamps -XX:+UseNUMA -XX:+UseParallelGC{% endif %}')

  # Service check related
  if hive_transport_mode.lower() == "http":
    hive_server_interactive_port = config['configurations']['hive-interactive-site']['hive.server2.thrift.http.port']
  else:
    hive_server_interactive_port = default('/configurations/hive-interactive-site/hive.server2.thrift.port',"10500")
  # Tez for Hive interactive related
  tez_interactive_config_dir = "/etc/tez_hive2/conf"
  tez_interactive_user = config['configurations']['tez-env']['tez_user']
  num_retries_for_checking_llap_status = default('/configurations/hive-interactive-env/num_retries_for_checking_llap_status', 10)
  # Used in LLAP slider package creation
  num_llap_nodes = config['configurations']['hive-interactive-env']['num_llap_nodes']
  llap_daemon_container_size = config['configurations']['hive-interactive-site']['hive.llap.daemon.yarn.container.mb']
  llap_log_level = config['configurations']['hive-interactive-env']['llap_log_level']
  hive_llap_io_mem_size = config['configurations']['hive-interactive-site']['hive.llap.io.memory.size']
  llap_heap_size = config['configurations']['hive-interactive-env']['llap_heap_size']
  llap_app_name = config['configurations']['hive-interactive-env']['llap_app_name']
  hive_llap_principal = None
  if security_enabled:
    hive_llap_keytab_file = config['configurations']['hive-interactive-site']['hive.llap.zk.sm.keytab.file']
    hive_llap_principal = (config['configurations']['hive-interactive-site']['hive.llap.zk.sm.principal']).replace('_HOST',hostname.lower())
  pass

# ranger host
ranger_admin_hosts = default("/clusterHostInfo/ranger_admin_hosts", [])
has_ranger_admin = not len(ranger_admin_hosts) == 0
xml_configurations_supported = config['configurations']['ranger-env']['xml_configurations_supported']

#ranger hive properties
policymgr_mgr_url = config['configurations']['admin-properties']['policymgr_external_url']
if 'admin-properties' in config['configurations'] and 'policymgr_external_url' in config['configurations']['admin-properties'] and policymgr_mgr_url.endswith('/'):
  policymgr_mgr_url = policymgr_mgr_url.rstrip('/')
xa_audit_db_name = default('/configurations/admin-properties/audit_db_name', 'ranger_audits')
xa_audit_db_user = default('/configurations/admin-properties/audit_db_user', 'rangerlogger')
xa_db_host = config['configurations']['admin-properties']['db_host']
repo_name = str(config['clusterName']) + '_hive'

jdbc_driver_class_name = config['configurations']['ranger-hive-plugin-properties']['jdbc.driverClassName']
common_name_for_certificate = config['configurations']['ranger-hive-plugin-properties']['common.name.for.certificate']

repo_config_username = config['configurations']['ranger-hive-plugin-properties']['REPOSITORY_CONFIG_USERNAME']

ranger_env = config['configurations']['ranger-env']
ranger_plugin_properties = config['configurations']['ranger-hive-plugin-properties']
policy_user = config['configurations']['ranger-hive-plugin-properties']['policy_user']

if security_enabled:
  hive_principal = hive_server_principal.replace('_HOST',hostname.lower())
  hive_keytab = config['configurations']['hive-site']['hive.server2.authentication.kerberos.keytab']

#For curl command in ranger plugin to get db connector
if has_ranger_admin:
  enable_ranger_hive = (config['configurations']['hive-env']['hive_security_authorization'].lower() == 'ranger')
  repo_config_password = unicode(config['configurations']['ranger-hive-plugin-properties']['REPOSITORY_CONFIG_PASSWORD'])
  xa_audit_db_flavor = (config['configurations']['admin-properties']['DB_FLAVOR']).lower()
  ranger_previous_jdbc_jar_name = None

  if stack_supports_ranger_audit_db:
    if xa_audit_db_flavor and xa_audit_db_flavor == 'mysql':
      ranger_jdbc_jar_name = default("/hostLevelParams/custom_mysql_jdbc_name", None)
      ranger_previous_jdbc_jar_name = default("/hostLevelParams/previous_custom_mysql_jdbc_name", None)
      audit_jdbc_url = format('jdbc:mysql://{xa_db_host}/{xa_audit_db_name}')
      jdbc_driver = "com.mysql.jdbc.Driver"
    elif xa_audit_db_flavor and xa_audit_db_flavor == 'oracle':
      ranger_jdbc_jar_name = default("/hostLevelParams/custom_oracle_jdbc_name", None)
      ranger_previous_jdbc_jar_name = default("/hostLevelParams/previous_custom_oracle_jdbc_name", None)
      colon_count = xa_db_host.count(':')
      if colon_count == 2 or colon_count == 0:
        audit_jdbc_url = format('jdbc:oracle:thin:@{xa_db_host}')
      else:
        audit_jdbc_url = format('jdbc:oracle:thin:@//{xa_db_host}')
      jdbc_driver = "oracle.jdbc.OracleDriver"
    elif xa_audit_db_flavor and xa_audit_db_flavor == 'postgres':
      ranger_jdbc_jar_name = default("/hostLevelParams/custom_postgres_jdbc_name", None)
      ranger_previous_jdbc_jar_name = default("/hostLevelParams/previous_custom_postgres_jdbc_name", None)
      audit_jdbc_url = format('jdbc:postgresql://{xa_db_host}/{xa_audit_db_name}')
      jdbc_driver = "org.postgresql.Driver"
    elif xa_audit_db_flavor and xa_audit_db_flavor == 'mssql':
      ranger_jdbc_jar_name = default("/hostLevelParams/custom_mssql_jdbc_name", None)
      ranger_previous_jdbc_jar_name = default("/hostLevelParams/previous_custom_mssql_jdbc_name", None)
      audit_jdbc_url = format('jdbc:sqlserver://{xa_db_host};databaseName={xa_audit_db_name}')
      jdbc_driver = "com.microsoft.sqlserver.jdbc.SQLServerDriver"
    elif xa_audit_db_flavor and xa_audit_db_flavor == 'sqla':
      ranger_jdbc_jar_name = default("/hostLevelParams/custom_sqlanywhere_jdbc_name", None)
      ranger_previous_jdbc_jar_name = default("/hostLevelParams/previous_custom_sqlanywhere_jdbc_name", None)
      audit_jdbc_url = format('jdbc:sqlanywhere:database={xa_audit_db_name};host={xa_db_host}')
      jdbc_driver = "sap.jdbc4.sqlanywhere.IDriver"

  ranger_downloaded_custom_connector = format("{tmp_dir}/{ranger_jdbc_jar_name}") if stack_supports_ranger_audit_db else None
  ranger_driver_curl_source = format("{jdk_location}/{ranger_jdbc_jar_name}") if stack_supports_ranger_audit_db else None
  ranger_driver_curl_target = format("{hive_lib}/{ranger_jdbc_jar_name}") if stack_supports_ranger_audit_db else None
  ranger_previous_jdbc_jar = format("{hive_lib}/{ranger_previous_jdbc_jar_name}") if stack_supports_ranger_audit_db else None
  sql_connector_jar = ''

  hive_ranger_plugin_config = {
    'username': repo_config_username,
    'password': repo_config_password,
    'jdbc.driverClassName': jdbc_driver_class_name,
    'jdbc.url': format("{hive_url}/default;principal={hive_principal}") if security_enabled else hive_url,
    'commonNameForCertificate': common_name_for_certificate
  }

  hive_ranger_plugin_repo = {
    'isActive': 'true',
    'config': json.dumps(hive_ranger_plugin_config),
    'description': 'hive repo',
    'name': repo_name,
    'repositoryType': 'hive',
    'assetType': '3'
  }

  if stack_supports_ranger_kerberos and security_enabled:
    hive_ranger_plugin_config['policy.download.auth.users'] = hive_user
    hive_ranger_plugin_config['tag.download.auth.users'] = hive_user
    hive_ranger_plugin_config['policy.grantrevoke.auth.users'] = hive_user

  if stack_supports_ranger_kerberos:
    hive_ranger_plugin_config['ambari.service.check.user'] = policy_user

    hive_ranger_plugin_repo = {
      'isEnabled': 'true',
      'configs': hive_ranger_plugin_config,
      'description': 'hive repo',
      'name': repo_name,
      'type': 'hive'
    }


  xa_audit_db_is_enabled = False
  xa_audit_db_password = ''
  if not is_empty(config['configurations']['admin-properties']['audit_db_password']) and stack_supports_ranger_audit_db:
    xa_audit_db_password = unicode(config['configurations']['admin-properties']['audit_db_password'])
  ranger_audit_solr_urls = config['configurations']['ranger-admin-site']['ranger.audit.solr.urls']
  if xml_configurations_supported and stack_supports_ranger_audit_db:
    xa_audit_db_is_enabled = config['configurations']['ranger-hive-audit']['xasecure.audit.destination.db']
  xa_audit_hdfs_is_enabled = config['configurations']['ranger-hive-audit']['xasecure.audit.destination.hdfs'] if xml_configurations_supported else None
  ssl_keystore_password = unicode(config['configurations']['ranger-hive-policymgr-ssl']['xasecure.policymgr.clientssl.keystore.password']) if xml_configurations_supported else None
  ssl_truststore_password = unicode(config['configurations']['ranger-hive-policymgr-ssl']['xasecure.policymgr.clientssl.truststore.password']) if xml_configurations_supported else None
  credential_file = format('/etc/ranger/{repo_name}/cred.jceks') if xml_configurations_supported else None

  #For SQLA explicitly disable audit to DB for Ranger
  if xa_audit_db_flavor == 'sqla':
    xa_audit_db_is_enabled = False

