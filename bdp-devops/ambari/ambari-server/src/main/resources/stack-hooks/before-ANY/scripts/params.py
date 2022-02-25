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

import collections
import re
import os
import ast

import ambari_simplejson as json # simplejson is much faster comparing to Python 2.6 json module and has the same functions set.

from resource_management.libraries.script import Script
from resource_management.libraries.functions import default
from resource_management.libraries.functions import format
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions import format_jvm_option
from resource_management.libraries.functions.is_empty import is_empty
from resource_management.libraries.functions.version import format_stack_version
from resource_management.libraries.functions.expect import expect
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions.stack_features import get_stack_feature_version
from resource_management.libraries.functions.get_architecture import get_architecture
from ambari_commons.constants import AMBARI_SUDO_BINARY
from resource_management.libraries.functions.namenode_ha_utils import get_properties_for_all_nameservices, namenode_federation_enabled


config = Script.get_config()
tmp_dir = Script.get_tmp_dir()

stack_root = Script.get_stack_root()

architecture = get_architecture()

dfs_type = default("/clusterLevelParams/dfs_type", "")

artifact_dir = format("{tmp_dir}/AMBARI-artifacts/")
jdk_name = default("/ambariLevelParams/jdk_name", None)
java_home = config['ambariLevelParams']['java_home']
java_version = expect("/ambariLevelParams/java_version", int)
jdk_location = config['ambariLevelParams']['jdk_location']

hadoop_custom_extensions_enabled = default("/configurations/core-site/hadoop.custom-extensions.enabled", False)

sudo = AMBARI_SUDO_BINARY

ambari_server_hostname = config['ambariLevelParams']['ambari_server_host']

stack_version_unformatted = config['clusterLevelParams']['stack_version']
stack_version_formatted = format_stack_version(stack_version_unformatted)

upgrade_type = Script.get_upgrade_type(default("/commandParams/upgrade_type", ""))
version = default("/commandParams/version", None)
# Handle upgrade and downgrade
if (upgrade_type is not None) and version:
  stack_version_formatted = format_stack_version(version)
ambari_java_home = default("/commandParams/ambari_java_home", None)
ambari_jdk_name = default("/commandParams/ambari_jdk_name", None)

security_enabled = config['configurations']['cluster-env']['security_enabled']
hdfs_user = config['configurations']['hadoop-env']['hdfs_user']

# Some datanode settings
dfs_dn_addr = default('/configurations/hdfs-site/dfs.datanode.address', None)
dfs_dn_http_addr = default('/configurations/hdfs-site/dfs.datanode.http.address', None)
dfs_dn_https_addr = default('/configurations/hdfs-site/dfs.datanode.https.address', None)
dfs_http_policy = default('/configurations/hdfs-site/dfs.http.policy', None)
secure_dn_ports_are_in_use = False

def get_port(address):
  """
  Extracts port from the address like 0.0.0.0:1019
  """
  if address is None:
    return None
  m = re.search(r'(?:http(?:s)?://)?([\w\d.]*):(\d{1,5})', address)
  if m is not None:
    return int(m.group(2))
  else:
    return None

def is_secure_port(port):
  """
  Returns True if port is root-owned at *nix systems
  """
  if port is not None:
    return port < 1024
  else:
    return False

# upgrades would cause these directories to have a version instead of "current"
# which would cause a lot of problems when writing out hadoop-env.sh; instead
# force the use of "current" in the hook
hdfs_user_nofile_limit = default("/configurations/hadoop-env/hdfs_user_nofile_limit", "128000")
hadoop_home = stack_select.get_hadoop_dir("home")
hadoop_libexec_dir = stack_select.get_hadoop_dir("libexec")
hadoop_lib_home = stack_select.get_hadoop_dir("lib")

ozone_manager_hosts = default("/clusterHostInfo/ozone_manager_hosts", [])
has_ozone = not len(ozone_manager_hosts) == 0
if version:
  hadoop_ozone_home = os.path.join(stack_root, version, "hadoop-ozone")
else:
  hadoop_ozone_home = os.path.join(stack_root, "current", "hadoop-ozone")

hadoop_dir = "/etc/hadoop"
hadoop_java_io_tmpdir = os.path.join(tmp_dir, "hadoop_java_io_tmpdir")
datanode_max_locked_memory = config['configurations']['hdfs-site']['dfs.datanode.max.locked.memory']
is_datanode_max_locked_memory_set = not is_empty(config['configurations']['hdfs-site']['dfs.datanode.max.locked.memory'])

mapreduce_libs_path = "/usr/hdp/current/hadoop-mapreduce-client/*"

if not security_enabled:
  hadoop_secure_dn_user = '""'
else:
  dfs_dn_port = get_port(dfs_dn_addr)
  dfs_dn_http_port = get_port(dfs_dn_http_addr)
  dfs_dn_https_port = get_port(dfs_dn_https_addr)
  # We try to avoid inability to start datanode as a plain user due to usage of root-owned ports
  if dfs_http_policy == "HTTPS_ONLY":
    secure_dn_ports_are_in_use = is_secure_port(dfs_dn_port) or is_secure_port(dfs_dn_https_port)
  elif dfs_http_policy == "HTTP_AND_HTTPS":
    secure_dn_ports_are_in_use = is_secure_port(dfs_dn_port) or is_secure_port(dfs_dn_http_port) or is_secure_port(dfs_dn_https_port)
  else:   # params.dfs_http_policy == "HTTP_ONLY" or not defined:
    secure_dn_ports_are_in_use = is_secure_port(dfs_dn_port) or is_secure_port(dfs_dn_http_port)
  if secure_dn_ports_are_in_use:
    hadoop_secure_dn_user = hdfs_user
  else:
    hadoop_secure_dn_user = '""'

#hadoop params
hdfs_log_dir_prefix = config['configurations']['hadoop-env']['hdfs_log_dir_prefix']
hadoop_pid_dir_prefix = config['configurations']['hadoop-env']['hadoop_pid_dir_prefix']
hadoop_root_logger = config['configurations']['hadoop-env']['hadoop_root_logger']

jsvc_path = "/usr/lib/bigtop-utils"

hadoop_heapsize = config['configurations']['hadoop-env']['hadoop_heapsize']
namenode_heapsize = config['configurations']['hadoop-env']['namenode_heapsize']
namenode_opt_newsize = config['configurations']['hadoop-env']['namenode_opt_newsize']
namenode_opt_maxnewsize = config['configurations']['hadoop-env']['namenode_opt_maxnewsize']
namenode_opt_permsize = format_jvm_option("/configurations/hadoop-env/namenode_opt_permsize","128m")
namenode_opt_maxpermsize = format_jvm_option("/configurations/hadoop-env/namenode_opt_maxpermsize","256m")

jtnode_opt_newsize = "200m"
jtnode_opt_maxnewsize = "200m"
jtnode_heapsize =  "1024m"
ttnode_heapsize = "1024m"

dtnode_heapsize = config['configurations']['hadoop-env']['dtnode_heapsize']
nfsgateway_heapsize = config['configurations']['hadoop-env']['nfsgateway_heapsize']
mapred_pid_dir_prefix = default("/configurations/mapred-env/mapred_pid_dir_prefix","/var/run/hadoop-mapreduce")
mapred_log_dir_prefix = default("/configurations/mapred-env/mapred_log_dir_prefix","/var/log/hadoop-mapreduce")
hadoop_env_sh_template = config['configurations']['hadoop-env']['content']

#users and groups
hbase_user = config['configurations']['hbase-env']['hbase_user']
smoke_user =  config['configurations']['cluster-env']['smokeuser']
gmetad_user = config['configurations']['ganglia-env']["gmetad_user"]
gmond_user = config['configurations']['ganglia-env']["gmond_user"]
tez_user = config['configurations']['tez-env']["tez_user"]
oozie_user = config['configurations']['oozie-env']["oozie_user"]
falcon_user = config['configurations']['falcon-env']["falcon_user"]
ranger_user = config['configurations']['ranger-env']["ranger_user"]
zeppelin_user = config['configurations']['zeppelin-env']["zeppelin_user"]
zeppelin_group = config['configurations']['zeppelin-env']["zeppelin_group"]

user_group = config['configurations']['cluster-env']['user_group']

ganglia_server_hosts = default("/clusterHostInfo/ganglia_server_hosts", [])
namenode_host = default("/clusterHostInfo/namenode_hosts", [])
hbase_master_hosts = default("/clusterHostInfo/hbase_master_hosts", [])
oozie_servers = default("/clusterHostInfo/oozie_server", [])
falcon_server_hosts = default("/clusterHostInfo/falcon_server_hosts", [])
ranger_admin_hosts = default("/clusterHostInfo/ranger_admin_hosts", [])
zeppelin_master_hosts = default("/clusterHostInfo/zeppelin_master_hosts", [])

# get the correct version to use for checking stack features
version_for_stack_feature_checks = get_stack_feature_version(config)


has_namenode = not len(namenode_host) == 0
has_ganglia_server = not len(ganglia_server_hosts) == 0
has_tez = 'tez-site' in config['configurations']
has_hbase_masters = not len(hbase_master_hosts) == 0
has_oozie_server = not len(oozie_servers) == 0
has_falcon_server_hosts = not len(falcon_server_hosts) == 0
has_ranger_admin = not len(ranger_admin_hosts) == 0
has_zeppelin_master = not len(zeppelin_master_hosts) == 0
stack_supports_zk_security = check_stack_feature(StackFeature.SECURE_ZOOKEEPER, version_for_stack_feature_checks)

hostname = config['agentLevelParams']['hostname']
hdfs_site = config['configurations']['hdfs-site']

# HDFS High Availability properties
dfs_ha_enabled = False
dfs_ha_nameservices = default('/configurations/hdfs-site/dfs.internal.nameservices', None)
if dfs_ha_nameservices is None:
  dfs_ha_nameservices = default('/configurations/hdfs-site/dfs.nameservices', None)

# on stacks without any filesystem there is no hdfs-site
dfs_ha_namenode_ids_all_ns = get_properties_for_all_nameservices(hdfs_site, 'dfs.ha.namenodes') if 'hdfs-site' in config['configurations'] else {}
dfs_ha_automatic_failover_enabled = default("/configurations/hdfs-site/dfs.ha.automatic-failover.enabled", False)

# Values for the current Host
namenode_id = None
namenode_rpc = None

dfs_ha_namemodes_ids_list = []
other_namenode_id = None

for ns, dfs_ha_namenode_ids in dfs_ha_namenode_ids_all_ns.iteritems():
  found = False
  if not is_empty(dfs_ha_namenode_ids):
    dfs_ha_namemodes_ids_list = dfs_ha_namenode_ids.split(",")
    dfs_ha_namenode_ids_array_len = len(dfs_ha_namemodes_ids_list)
    if dfs_ha_namenode_ids_array_len > 1:
      dfs_ha_enabled = True
  if dfs_ha_enabled:
    for nn_id in dfs_ha_namemodes_ids_list:
      nn_host = config['configurations']['hdfs-site'][format('dfs.namenode.rpc-address.{ns}.{nn_id}')]
      if hostname in nn_host:
        namenode_id = nn_id
        namenode_rpc = nn_host
        found = True
    # With HA enabled namenode_address is recomputed
    namenode_address = format('hdfs://{ns}')

    # Calculate the namenode id of the other namenode. This is needed during RU to initiate an HA failover using ZKFC.
    if namenode_id is not None and len(dfs_ha_namemodes_ids_list) == 2:
      other_namenode_id = list(set(dfs_ha_namemodes_ids_list) - set([namenode_id]))[0]

  if found:
    break

if has_namenode or dfs_type == 'HCFS':
    hadoop_conf_dir = conf_select.get_hadoop_conf_dir()
    hadoop_conf_secure_dir = os.path.join(hadoop_conf_dir, "secure")

hbase_tmp_dir = "/tmp/hbase-hbase"

proxyuser_group = default("/configurations/hadoop-env/proxyuser_group","users")
ranger_group = config['configurations']['ranger-env']['ranger_group']
dfs_cluster_administrators_group = config['configurations']['hdfs-site']["dfs.cluster.administrators"]

sysprep_skip_create_users_and_groups = default("/configurations/cluster-env/sysprep_skip_create_users_and_groups", False)
ignore_groupsusers_create = default("/configurations/cluster-env/ignore_groupsusers_create", False)
fetch_nonlocal_groups = config['configurations']['cluster-env']["fetch_nonlocal_groups"]

smoke_user_dirs = format("/tmp/hadoop-{smoke_user},/tmp/hsperfdata_{smoke_user},/home/{smoke_user},/tmp/{smoke_user},/tmp/sqoop-{smoke_user}")
if has_hbase_masters:
  hbase_user_dirs = format("/home/{hbase_user},/tmp/{hbase_user},/usr/bin/{hbase_user},/var/log/{hbase_user},{hbase_tmp_dir}")
#repo params
repo_info = config['hostLevelParams']['repoInfo']
service_repo_info = default("/hostLevelParams/service_repo_info",None)

user_to_groups_dict = {}

#Append new user-group mapping to the dict
try:
  user_group_map = ast.literal_eval(config['clusterLevelParams']['user_groups'])
  for key in user_group_map.iterkeys():
    user_to_groups_dict[key] = user_group_map[key]
except ValueError:
  print('User Group mapping (user_group) is missing in the hostLevelParams')

user_to_gid_dict = collections.defaultdict(lambda:user_group)

user_list = json.loads(config['clusterLevelParams']['user_list'])
group_list = json.loads(config['clusterLevelParams']['group_list'])
host_sys_prepped = default("/ambariLevelParams/host_sys_prepped", False)

tez_am_view_acls = config['configurations']['tez-site']["tez.am.view-acls"]
override_uid = str(default("/configurations/cluster-env/override_uid", "true")).lower()

# if NN HA on secure clutser, access Zookeper securely
if stack_supports_zk_security and dfs_ha_enabled and security_enabled:
    hadoop_zkfc_opts=format("-Dzookeeper.sasl.client=true -Dzookeeper.sasl.client.username=zookeeper -Djava.security.auth.login.config={hadoop_conf_secure_dir}/hdfs_jaas.conf -Dzookeeper.sasl.clientconfig=Client")
