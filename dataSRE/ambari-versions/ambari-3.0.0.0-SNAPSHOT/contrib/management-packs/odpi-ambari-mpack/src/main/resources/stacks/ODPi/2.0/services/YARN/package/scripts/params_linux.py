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

Ambari Agent

"""
import os

from resource_management.libraries.script.script import Script
from resource_management.libraries.resources.hdfs_resource import HdfsResource
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions import format
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions.stack_features import get_stack_feature_version
from resource_management.libraries.functions import get_kinit_path
from resource_management.libraries.functions.get_not_managed_resources import get_not_managed_resources
from resource_management.libraries.functions.version import format_stack_version
from resource_management.libraries.functions.default import default
from resource_management.libraries import functions
from resource_management.libraries.functions import is_empty

import status_params

# a map of the Ambari role to the component name
# for use with <stack-root>/current/<component>
MAPR_SERVER_ROLE_DIRECTORY_MAP = {
  'HISTORYSERVER' : 'hadoop-mapreduce-historyserver',
  'MAPREDUCE2_CLIENT' : 'hadoop-mapreduce-client',
}

YARN_SERVER_ROLE_DIRECTORY_MAP = {
  'APP_TIMELINE_SERVER' : 'hadoop-yarn-timelineserver',
  'NODEMANAGER' : 'hadoop-yarn-nodemanager',
  'RESOURCEMANAGER' : 'hadoop-yarn-resourcemanager',
  'YARN_CLIENT' : 'hadoop-yarn-client'
}

# server configurations
config = Script.get_config()
tmp_dir = Script.get_tmp_dir()

stack_name = status_params.stack_name
stack_root = Script.get_stack_root()
tarball_map = default("/configurations/cluster-env/tarball_map", None)

config_path = os.path.join(stack_root, "current/hadoop-client/conf")
config_dir = os.path.realpath(config_path)

# This is expected to be of the form #.#.#.#
stack_version_unformatted = config['hostLevelParams']['stack_version']
stack_version_formatted_major = format_stack_version(stack_version_unformatted)
stack_version_formatted = functions.get_stack_version('hadoop-yarn-resourcemanager')

stack_supports_ru = stack_version_formatted_major and check_stack_feature(StackFeature.ROLLING_UPGRADE, stack_version_formatted_major)
stack_supports_timeline_state_store = stack_version_formatted_major and check_stack_feature(StackFeature.TIMELINE_STATE_STORE, stack_version_formatted_major)

# New Cluster Stack Version that is defined during the RESTART of a Stack Upgrade.
# It cannot be used during the initial Cluser Install because the version is not yet known.
version = default("/commandParams/version", None)

# get the correct version to use for checking stack features
version_for_stack_feature_checks = get_stack_feature_version(config)

stack_supports_ranger_kerberos = check_stack_feature(StackFeature.RANGER_KERBEROS_SUPPORT, version_for_stack_feature_checks)
stack_supports_ranger_audit_db = check_stack_feature(StackFeature.RANGER_AUDIT_DB_SUPPORT, version_for_stack_feature_checks)

hostname = config['hostname']

# hadoop default parameters
hadoop_libexec_dir = stack_select.get_hadoop_dir("libexec")
hadoop_bin = stack_select.get_hadoop_dir("sbin")
hadoop_bin_dir = stack_select.get_hadoop_dir("bin")
hadoop_conf_dir = conf_select.get_hadoop_conf_dir()
hadoop_yarn_home = '/usr/lib/hadoop-yarn'
hadoop_mapred2_jar_location = "/usr/lib/hadoop-mapreduce"
mapred_bin = "/usr/lib/hadoop-mapreduce/sbin"
yarn_bin = "/usr/lib/hadoop-yarn/sbin"
yarn_container_bin = "/usr/lib/hadoop-yarn/bin"
hadoop_java_io_tmpdir = os.path.join(tmp_dir, "hadoop_java_io_tmpdir")

# hadoop parameters stack supporting rolling_uprade
if stack_supports_ru:
  # MapR directory root
  mapred_role_root = "hadoop-mapreduce-client"
  command_role = default("/role", "")
  if command_role in MAPR_SERVER_ROLE_DIRECTORY_MAP:
    mapred_role_root = MAPR_SERVER_ROLE_DIRECTORY_MAP[command_role]

  # YARN directory root
  yarn_role_root = "hadoop-yarn-client"
  if command_role in YARN_SERVER_ROLE_DIRECTORY_MAP:
    yarn_role_root = YARN_SERVER_ROLE_DIRECTORY_MAP[command_role]

  hadoop_mapred2_jar_location = format("{stack_root}/current/{mapred_role_root}")
  mapred_bin = format("{stack_root}/current/{mapred_role_root}/sbin")

  hadoop_yarn_home = format("{stack_root}/current/{yarn_role_root}")
  yarn_bin = format("{stack_root}/current/{yarn_role_root}/sbin")
  yarn_container_bin = format("{stack_root}/current/{yarn_role_root}/bin")

if stack_supports_timeline_state_store:
  # Timeline Service property that was added timeline_state_store stack feature
  ats_leveldb_state_store_dir = config['configurations']['yarn-site']['yarn.timeline-service.leveldb-state-store.path']

# ats 1.5 properties
entity_groupfs_active_dir = config['configurations']['yarn-site']['yarn.timeline-service.entity-group-fs-store.active-dir']
entity_groupfs_active_dir_mode = 01777
entity_groupfs_store_dir = config['configurations']['yarn-site']['yarn.timeline-service.entity-group-fs-store.done-dir']
entity_groupfs_store_dir_mode = 0700

hadoop_conf_secure_dir = os.path.join(hadoop_conf_dir, "secure")

limits_conf_dir = "/etc/security/limits.d"
yarn_user_nofile_limit = default("/configurations/yarn-env/yarn_user_nofile_limit", "32768")
yarn_user_nproc_limit = default("/configurations/yarn-env/yarn_user_nproc_limit", "65536")

mapred_user_nofile_limit = default("/configurations/mapred-env/mapred_user_nofile_limit", "32768")
mapred_user_nproc_limit = default("/configurations/mapred-env/mapred_user_nproc_limit", "65536")

execute_path = os.environ['PATH'] + os.pathsep + hadoop_bin_dir + os.pathsep + yarn_container_bin

ulimit_cmd = "ulimit -c unlimited;"

mapred_user = status_params.mapred_user
yarn_user = status_params.yarn_user
hdfs_user = config['configurations']['hadoop-env']['hdfs_user']
hdfs_tmp_dir = config['configurations']['hadoop-env']['hdfs_tmp_dir']

smokeuser = config['configurations']['cluster-env']['smokeuser']
smokeuser_principal = config['configurations']['cluster-env']['smokeuser_principal_name']
smoke_hdfs_user_mode = 0770
security_enabled = config['configurations']['cluster-env']['security_enabled']
nm_security_marker_dir = "/var/lib/hadoop-yarn"
nm_security_marker = format('{nm_security_marker_dir}/nm_security_enabled')
current_nm_security_state = os.path.isfile(nm_security_marker)
toggle_nm_security = (current_nm_security_state and not security_enabled) or (not current_nm_security_state and security_enabled)
smoke_user_keytab = config['configurations']['cluster-env']['smokeuser_keytab']

yarn_executor_container_group = config['configurations']['yarn-site']['yarn.nodemanager.linux-container-executor.group']
yarn_nodemanager_container_executor_class =  config['configurations']['yarn-site']['yarn.nodemanager.container-executor.class']
is_linux_container_executor = (yarn_nodemanager_container_executor_class == 'org.apache.hadoop.yarn.server.nodemanager.LinuxContainerExecutor')
container_executor_mode = 06050 if is_linux_container_executor else 02050
kinit_path_local = get_kinit_path(default('/configurations/kerberos-env/executable_search_paths', None))
yarn_http_policy = config['configurations']['yarn-site']['yarn.http.policy']
yarn_https_on = (yarn_http_policy.upper() == 'HTTPS_ONLY')
rm_hosts = config['clusterHostInfo']['rm_host']
rm_host = rm_hosts[0]
rm_port = config['configurations']['yarn-site']['yarn.resourcemanager.webapp.address'].split(':')[-1]
rm_https_port = default('/configurations/yarn-site/yarn.resourcemanager.webapp.https.address', ":8090").split(':')[-1]

java64_home = config['hostLevelParams']['java_home']
hadoop_ssl_enabled = default("/configurations/core-site/hadoop.ssl.enabled", False)

yarn_heapsize = config['configurations']['yarn-env']['yarn_heapsize']
resourcemanager_heapsize = config['configurations']['yarn-env']['resourcemanager_heapsize']
nodemanager_heapsize = config['configurations']['yarn-env']['nodemanager_heapsize']
apptimelineserver_heapsize = default("/configurations/yarn-env/apptimelineserver_heapsize", 1024)
ats_leveldb_dir = config['configurations']['yarn-site']['yarn.timeline-service.leveldb-timeline-store.path']
ats_leveldb_lock_file = os.path.join(ats_leveldb_dir, "leveldb-timeline-store.ldb", "LOCK")
yarn_log_dir_prefix = config['configurations']['yarn-env']['yarn_log_dir_prefix']
yarn_pid_dir_prefix = status_params.yarn_pid_dir_prefix
mapred_pid_dir_prefix = status_params.mapred_pid_dir_prefix
mapred_log_dir_prefix = config['configurations']['mapred-env']['mapred_log_dir_prefix']
mapred_env_sh_template = config['configurations']['mapred-env']['content']
yarn_env_sh_template = config['configurations']['yarn-env']['content']
yarn_nodemanager_recovery_dir = default('/configurations/yarn-site/yarn.nodemanager.recovery.dir', None)
service_check_queue_name = default('/configurations/yarn-env/service_check.queue.name', 'default')

if len(rm_hosts) > 1:
  additional_rm_host = rm_hosts[1]
  rm_webui_address = format("{rm_host}:{rm_port},{additional_rm_host}:{rm_port}")
  rm_webui_https_address = format("{rm_host}:{rm_https_port},{additional_rm_host}:{rm_https_port}")
else:
  rm_webui_address = format("{rm_host}:{rm_port}")
  rm_webui_https_address = format("{rm_host}:{rm_https_port}")

nm_webui_address = config['configurations']['yarn-site']['yarn.nodemanager.webapp.address']
hs_webui_address = config['configurations']['mapred-site']['mapreduce.jobhistory.webapp.address']
nm_address = config['configurations']['yarn-site']['yarn.nodemanager.address']  # still contains 0.0.0.0
if hostname and nm_address and nm_address.startswith("0.0.0.0:"):
  nm_address = nm_address.replace("0.0.0.0", hostname)

# Initialize lists of work directories.
nm_local_dirs = default("/configurations/yarn-site/yarn.nodemanager.local-dirs", "")
nm_log_dirs = default("/configurations/yarn-site/yarn.nodemanager.log-dirs", "")

nm_local_dirs_list = nm_local_dirs.split(',')
nm_log_dirs_list = nm_log_dirs.split(',')

nm_log_dir_to_mount_file = "/var/lib/ambari-agent/data/yarn/yarn_log_dir_mount.hist"
nm_local_dir_to_mount_file = "/var/lib/ambari-agent/data/yarn/yarn_local_dir_mount.hist"

distrAppJarName = "hadoop-yarn-applications-distributedshell-2.*.jar"
hadoopMapredExamplesJarName = "hadoop-mapreduce-examples-2.*.jar"

entity_file_history_directory = "/tmp/entity-file-history/active"

yarn_pid_dir = status_params.yarn_pid_dir
mapred_pid_dir = status_params.mapred_pid_dir

mapred_log_dir = format("{mapred_log_dir_prefix}/{mapred_user}")
yarn_log_dir = format("{yarn_log_dir_prefix}/{yarn_user}")
mapred_job_summary_log = format("{mapred_log_dir_prefix}/{mapred_user}/hadoop-mapreduce.jobsummary.log")
yarn_job_summary_log = format("{yarn_log_dir_prefix}/{yarn_user}/hadoop-mapreduce.jobsummary.log")

user_group = config['configurations']['cluster-env']['user_group']

#exclude file
exclude_hosts = default("/clusterHostInfo/decom_nm_hosts", [])
exclude_file_path = default("/configurations/yarn-site/yarn.resourcemanager.nodes.exclude-path","/etc/hadoop/conf/yarn.exclude")
rm_nodes_exclude_dir = os.path.dirname(exclude_file_path)

ats_host = set(default("/clusterHostInfo/app_timeline_server_hosts", []))
has_ats = not len(ats_host) == 0

nm_hosts = default("/clusterHostInfo/nm_hosts", [])

#incude file
include_file_path = default("/configurations/yarn-site/yarn.resourcemanager.nodes.include-path", None)
include_hosts = None
manage_include_files = default("/configurations/yarn-site/manage.include.files", False)
if include_file_path and manage_include_files:
  rm_nodes_include_dir = os.path.dirname(include_file_path)
  include_hosts = list(set(nm_hosts) - set(exclude_hosts))

# don't using len(nm_hosts) here, because check can take too much time on large clusters
number_of_nm = 1

# default kinit commands
rm_kinit_cmd = ""
yarn_timelineservice_kinit_cmd = ""
nodemanager_kinit_cmd = ""

if security_enabled:
  rm_principal_name = config['configurations']['yarn-site']['yarn.resourcemanager.principal']
  rm_principal_name = rm_principal_name.replace('_HOST',hostname.lower())
  rm_keytab = config['configurations']['yarn-site']['yarn.resourcemanager.keytab']
  rm_kinit_cmd = format("{kinit_path_local} -kt {rm_keytab} {rm_principal_name};")

  # YARN timeline security options
  if has_ats:
    _yarn_timelineservice_principal_name = config['configurations']['yarn-site']['yarn.timeline-service.principal']
    _yarn_timelineservice_principal_name = _yarn_timelineservice_principal_name.replace('_HOST', hostname.lower())
    _yarn_timelineservice_keytab = config['configurations']['yarn-site']['yarn.timeline-service.keytab']
    yarn_timelineservice_kinit_cmd = format("{kinit_path_local} -kt {_yarn_timelineservice_keytab} {_yarn_timelineservice_principal_name};")

  if 'yarn.nodemanager.principal' in config['configurations']['yarn-site']:
    _nodemanager_principal_name = default('/configurations/yarn-site/yarn.nodemanager.principal', None)
    if _nodemanager_principal_name:
      _nodemanager_principal_name = _nodemanager_principal_name.replace('_HOST', hostname.lower())

    _nodemanager_keytab = config['configurations']['yarn-site']['yarn.nodemanager.keytab']
    nodemanager_kinit_cmd = format("{kinit_path_local} -kt {_nodemanager_keytab} {_nodemanager_principal_name};")


yarn_log_aggregation_enabled = config['configurations']['yarn-site']['yarn.log-aggregation-enable']
yarn_nm_app_log_dir =  config['configurations']['yarn-site']['yarn.nodemanager.remote-app-log-dir']
mapreduce_jobhistory_intermediate_done_dir = config['configurations']['mapred-site']['mapreduce.jobhistory.intermediate-done-dir']
mapreduce_jobhistory_done_dir = config['configurations']['mapred-site']['mapreduce.jobhistory.done-dir']
jobhistory_heapsize = default("/configurations/mapred-env/jobhistory_heapsize", "900")
jhs_leveldb_state_store_dir = default('/configurations/mapred-site/mapreduce.jobhistory.recovery.store.leveldb.path', "/hadoop/mapreduce/jhs")

# Tez-related properties
tez_user = config['configurations']['tez-env']['tez_user']

# Tez jars
tez_local_api_jars = '/usr/lib/tez/tez*.jar'
tez_local_lib_jars = '/usr/lib/tez/lib/*.jar'
app_dir_files = {tez_local_api_jars:None}

# Tez libraries
tez_lib_uris = default("/configurations/tez-site/tez.lib.uris", None)

#for create_hdfs_directory
hdfs_user_keytab = config['configurations']['hadoop-env']['hdfs_user_keytab']
hdfs_principal_name = config['configurations']['hadoop-env']['hdfs_principal_name']



hdfs_site = config['configurations']['hdfs-site']
default_fs = config['configurations']['core-site']['fs.defaultFS']
is_webhdfs_enabled = hdfs_site['dfs.webhdfs.enabled']

# Path to file that contains list of HDFS resources to be skipped during processing
hdfs_resource_ignore_file = "/var/lib/ambari-agent/data/.hdfs_resource_ignore"

dfs_type = default("/commandParams/dfs_type", "")


import functools
#create partial functions with common arguments for every HdfsResource call
#to create/delete hdfs directory/file/copyfromlocal we need to call params.HdfsResource in code
HdfsResource = functools.partial(
  HdfsResource,
  user=hdfs_user,
  hdfs_resource_ignore_file = hdfs_resource_ignore_file,
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
update_files_only = default("/commandParams/update_files_only",False)

mapred_tt_group = default("/configurations/mapred-site/mapreduce.tasktracker.group", user_group)

#taskcontroller.cfg

mapred_local_dir = "/tmp/hadoop-mapred/mapred/local"
hdfs_log_dir_prefix = config['configurations']['hadoop-env']['hdfs_log_dir_prefix']
min_user_id = config['configurations']['yarn-env']['min_user_id']

# Node labels
node_labels_dir = default("/configurations/yarn-site/yarn.node-labels.fs-store.root-dir", None)
node_label_enable = config['configurations']['yarn-site']['yarn.node-labels.enabled']

cgroups_dir = "/cgroups_test/cpu"

# ***********************  RANGER PLUGIN CHANGES ***********************
# ranger host
ranger_admin_hosts = default("/clusterHostInfo/ranger_admin_hosts", [])
has_ranger_admin = not len(ranger_admin_hosts) == 0
xml_configurations_supported = config['configurations']['ranger-env']['xml_configurations_supported']
ambari_server_hostname = config['clusterHostInfo']['ambari_server_host'][0]
# hostname of the active HDFS HA Namenode (only used when HA is enabled)
dfs_ha_namenode_active = default("/configurations/hadoop-env/dfs_ha_initial_namenode_active", None)
if dfs_ha_namenode_active is not None: 
  namenode_hostname = dfs_ha_namenode_active
else:
  namenode_hostname = config['clusterHostInfo']['namenode_host'][0]

ranger_admin_log_dir = default("/configurations/ranger-env/ranger_admin_log_dir","/var/log/ranger/admin")

scheme = 'http' if not yarn_https_on else 'https'
yarn_rm_address = config['configurations']['yarn-site']['yarn.resourcemanager.webapp.address'] if not yarn_https_on else config['configurations']['yarn-site']['yarn.resourcemanager.webapp.https.address']
rm_active_port = rm_https_port if yarn_https_on else rm_port

rm_ha_enabled = False
rm_ha_ids_list = []
rm_webapp_addresses_list = [yarn_rm_address]
rm_ha_ids = default("/configurations/yarn-site/yarn.resourcemanager.ha.rm-ids", None)

if rm_ha_ids:
  rm_ha_ids_list = rm_ha_ids.split(",")
  if len(rm_ha_ids_list) > 1:
    rm_ha_enabled = True

if rm_ha_enabled:
  rm_webapp_addresses_list = []
  for rm_id in rm_ha_ids_list:
    rm_webapp_address_property = format('yarn.resourcemanager.webapp.address.{rm_id}') if not yarn_https_on else format('yarn.resourcemanager.webapp.https.address.{rm_id}')
    rm_webapp_address = config['configurations']['yarn-site'][rm_webapp_address_property]
    rm_webapp_addresses_list.append(rm_webapp_address)

#ranger yarn properties
if has_ranger_admin:
  is_supported_yarn_ranger = config['configurations']['yarn-env']['is_supported_yarn_ranger']

  if is_supported_yarn_ranger:
    enable_ranger_yarn = (config['configurations']['ranger-yarn-plugin-properties']['ranger-yarn-plugin-enabled'].lower() == 'yes')
    policymgr_mgr_url = config['configurations']['admin-properties']['policymgr_external_url']
    if 'admin-properties' in config['configurations'] and 'policymgr_external_url' in config['configurations']['admin-properties'] and policymgr_mgr_url.endswith('/'):
      policymgr_mgr_url = policymgr_mgr_url.rstrip('/')
    xa_audit_db_flavor = (config['configurations']['admin-properties']['DB_FLAVOR']).lower()
    xa_audit_db_name = default('/configurations/admin-properties/audit_db_name', 'ranger_audits')
    xa_audit_db_user = default('/configurations/admin-properties/audit_db_user', 'rangerlogger')
    xa_audit_db_password = ''
    if not is_empty(config['configurations']['admin-properties']['audit_db_password']) and stack_supports_ranger_audit_db:
      xa_audit_db_password = unicode(config['configurations']['admin-properties']['audit_db_password'])
    xa_db_host = config['configurations']['admin-properties']['db_host']
    repo_name = str(config['clusterName']) + '_yarn'

    ranger_env = config['configurations']['ranger-env']
    ranger_plugin_properties = config['configurations']['ranger-yarn-plugin-properties']
    policy_user = config['configurations']['ranger-yarn-plugin-properties']['policy_user']
    yarn_rest_url = config['configurations']['yarn-site']['yarn.resourcemanager.webapp.address']  

    ranger_plugin_config = {
      'username' : config['configurations']['ranger-yarn-plugin-properties']['REPOSITORY_CONFIG_USERNAME'],
      'password' : unicode(config['configurations']['ranger-yarn-plugin-properties']['REPOSITORY_CONFIG_PASSWORD']),
      'yarn.url' : format('{scheme}://{yarn_rest_url}'),
      'commonNameForCertificate' : config['configurations']['ranger-yarn-plugin-properties']['common.name.for.certificate']
    }

    yarn_ranger_plugin_repo = {
      'isEnabled': 'true',
      'configs': ranger_plugin_config,
      'description': 'yarn repo',
      'name': repo_name,
      'repositoryType': 'yarn',
      'type': 'yarn',
      'assetType': '1'
    }

    if stack_supports_ranger_kerberos:
      ranger_plugin_config['ambari.service.check.user'] = policy_user
      ranger_plugin_config['hadoop.security.authentication'] = 'kerberos' if security_enabled else 'simple'

    if stack_supports_ranger_kerberos and security_enabled:
      ranger_plugin_config['policy.download.auth.users'] = yarn_user
      ranger_plugin_config['tag.download.auth.users'] = yarn_user

    #For curl command in ranger plugin to get db connector
    jdk_location = config['hostLevelParams']['jdk_location']
    java_share_dir = '/usr/share/java'
    previous_jdbc_jar_name = None
    if stack_supports_ranger_audit_db:
      if xa_audit_db_flavor and xa_audit_db_flavor == 'mysql':
        jdbc_jar_name = default("/hostLevelParams/custom_mysql_jdbc_name", None)
        previous_jdbc_jar_name = default("/hostLevelParams/previous_custom_mysql_jdbc_name", None)
        audit_jdbc_url = format('jdbc:mysql://{xa_db_host}/{xa_audit_db_name}')
        jdbc_driver = "com.mysql.jdbc.Driver"
      elif xa_audit_db_flavor and xa_audit_db_flavor == 'oracle':
        jdbc_jar_name = default("/hostLevelParams/custom_oracle_jdbc_name", None)
        previous_jdbc_jar_name = default("/hostLevelParams/previous_custom_oracle_jdbc_name", None)
        colon_count = xa_db_host.count(':')
        if colon_count == 2 or colon_count == 0:
          audit_jdbc_url = format('jdbc:oracle:thin:@{xa_db_host}')
        else:
          audit_jdbc_url = format('jdbc:oracle:thin:@//{xa_db_host}')
        jdbc_driver = "oracle.jdbc.OracleDriver"
      elif xa_audit_db_flavor and xa_audit_db_flavor == 'postgres':
        jdbc_jar_name = default("/hostLevelParams/custom_postgres_jdbc_name", None)
        previous_jdbc_jar_name = default("/hostLevelParams/previous_custom_postgres_jdbc_name", None)
        audit_jdbc_url = format('jdbc:postgresql://{xa_db_host}/{xa_audit_db_name}')
        jdbc_driver = "org.postgresql.Driver"
      elif xa_audit_db_flavor and xa_audit_db_flavor == 'mssql':
        jdbc_jar_name = default("/hostLevelParams/custom_mssql_jdbc_name", None)
        previous_jdbc_jar_name = default("/hostLevelParams/previous_custom_mssql_jdbc_name", None)
        audit_jdbc_url = format('jdbc:sqlserver://{xa_db_host};databaseName={xa_audit_db_name}')
        jdbc_driver = "com.microsoft.sqlserver.jdbc.SQLServerDriver"
      elif xa_audit_db_flavor and xa_audit_db_flavor == 'sqla':
        jdbc_jar_name = default("/hostLevelParams/custom_sqlanywhere_jdbc_name", None)
        previous_jdbc_jar_name = default("/hostLevelParams/previous_custom_sqlanywhere_jdbc_name", None)
        audit_jdbc_url = format('jdbc:sqlanywhere:database={xa_audit_db_name};host={xa_db_host}')
        jdbc_driver = "sap.jdbc4.sqlanywhere.IDriver"

    downloaded_custom_connector = format("{tmp_dir}/{jdbc_jar_name}") if stack_supports_ranger_audit_db else None
    driver_curl_source = format("{jdk_location}/{jdbc_jar_name}") if stack_supports_ranger_audit_db else None
    driver_curl_target = format("{hadoop_yarn_home}/lib/{jdbc_jar_name}") if stack_supports_ranger_audit_db else None
    previous_jdbc_jar = format("{hadoop_yarn_home}/lib/{previous_jdbc_jar_name}") if stack_supports_ranger_audit_db else None

    xa_audit_db_is_enabled = False
    ranger_audit_solr_urls = config['configurations']['ranger-admin-site']['ranger.audit.solr.urls']
    if xml_configurations_supported and stack_supports_ranger_audit_db:
      xa_audit_db_is_enabled = config['configurations']['ranger-yarn-audit']['xasecure.audit.destination.db']
    xa_audit_hdfs_is_enabled = config['configurations']['ranger-yarn-audit']['xasecure.audit.destination.hdfs'] if xml_configurations_supported else None
    ssl_keystore_password = unicode(config['configurations']['ranger-yarn-policymgr-ssl']['xasecure.policymgr.clientssl.keystore.password']) if xml_configurations_supported else None
    ssl_truststore_password = unicode(config['configurations']['ranger-yarn-policymgr-ssl']['xasecure.policymgr.clientssl.truststore.password']) if xml_configurations_supported else None
    credential_file = format('/etc/ranger/{repo_name}/cred.jceks') if xml_configurations_supported else None

    #For SQLA explicitly disable audit to DB for Ranger
    if xa_audit_db_flavor == 'sqla':
      xa_audit_db_is_enabled = False
