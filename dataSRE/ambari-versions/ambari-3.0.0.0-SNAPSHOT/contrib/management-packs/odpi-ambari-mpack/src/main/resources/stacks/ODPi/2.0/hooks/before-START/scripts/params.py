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

import os

from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions import default
from resource_management.libraries.functions import format_jvm_option
from resource_management.libraries.functions import format
from resource_management.libraries.functions.version import format_stack_version, compare_versions
from ambari_commons.os_check import OSCheck
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions import get_kinit_path
from resource_management.libraries.functions.get_not_managed_resources import get_not_managed_resources
from resource_management.libraries.resources.hdfs_resource import HdfsResource

config = Script.get_config()

host_sys_prepped = default("/hostLevelParams/host_sys_prepped", False)

stack_version_unformatted = config['hostLevelParams']['stack_version']
stack_version_formatted = format_stack_version(stack_version_unformatted)

dfs_type = default("/commandParams/dfs_type", "")
hadoop_conf_dir = "/etc/hadoop/conf"

component_list = default("/localComponents", [])

hdfs_tmp_dir = config['configurations']['hadoop-env']['hdfs_tmp_dir']

# hadoop default params
mapreduce_libs_path = "/usr/lib/hadoop-mapreduce/*"

hadoop_libexec_dir = stack_select.get_hadoop_dir("libexec")
hadoop_lib_home = stack_select.get_hadoop_dir("lib")
hadoop_bin = stack_select.get_hadoop_dir("sbin")
hadoop_home = '/usr'
create_lib_snappy_symlinks = True

# HDP 2.2+ params
if Script.is_stack_greater_or_equal("2.2"):
  mapreduce_libs_path = "/usr/hdp/current/hadoop-mapreduce-client/*"
  hadoop_home = stack_select.get_hadoop_dir("home")
  create_lib_snappy_symlinks = False
  
current_service = config['serviceName']

#security params
security_enabled = config['configurations']['cluster-env']['security_enabled']

#users and groups
has_hadoop_env = 'hadoop-env' in config['configurations']
mapred_user = config['configurations']['mapred-env']['mapred_user']
hdfs_user = config['configurations']['hadoop-env']['hdfs_user']
yarn_user = config['configurations']['yarn-env']['yarn_user']

user_group = config['configurations']['cluster-env']['user_group']

#hosts
hostname = config["hostname"]
ambari_server_hostname = config['clusterHostInfo']['ambari_server_host'][0]
rm_host = default("/clusterHostInfo/rm_host", [])
slave_hosts = default("/clusterHostInfo/slave_hosts", [])
oozie_servers = default("/clusterHostInfo/oozie_server", [])
hcat_server_hosts = default("/clusterHostInfo/webhcat_server_host", [])
hive_server_host =  default("/clusterHostInfo/hive_server_host", [])
hbase_master_hosts = default("/clusterHostInfo/hbase_master_hosts", [])
hs_host = default("/clusterHostInfo/hs_host", [])
jtnode_host = default("/clusterHostInfo/jtnode_host", [])
namenode_host = default("/clusterHostInfo/namenode_host", [])
zk_hosts = default("/clusterHostInfo/zookeeper_hosts", [])
ganglia_server_hosts = default("/clusterHostInfo/ganglia_server_host", [])
ams_collector_hosts = default("/clusterHostInfo/metrics_collector_hosts", [])
if 'cluster-env' in config['configurations'] and \
    'metrics_collector_external_hosts' in config['configurations']['cluster-env']:
  ams_collector_hosts = config['configurations']['cluster-env']['metrics_collector_external_hosts']
  set_instanceId = "true"
else:
  ams_collector_hosts = ",".join(default("/clusterHostInfo/metrics_collector_hosts", []))

has_namenode = not len(namenode_host) == 0
has_resourcemanager = not len(rm_host) == 0
has_slaves = not len(slave_hosts) == 0
has_oozie_server = not len(oozie_servers) == 0
has_hcat_server_host = not len(hcat_server_hosts) == 0
has_hive_server_host = not len(hive_server_host) == 0
has_hbase_masters = not len(hbase_master_hosts) == 0
has_zk_host = not len(zk_hosts) == 0
has_ganglia_server = not len(ganglia_server_hosts) == 0
has_metric_collector = not len(ams_collector_hosts) == 0

is_namenode_master = hostname in namenode_host
is_jtnode_master = hostname in jtnode_host
is_rmnode_master = hostname in rm_host
is_hsnode_master = hostname in hs_host
is_hbase_master = hostname in hbase_master_hosts
is_slave = hostname in slave_hosts
if has_ganglia_server:
  ganglia_server_host = ganglia_server_hosts[0]
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

  pass
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

# Cluster Zookeeper quorum
zookeeper_quorum = None
if has_zk_host:
  if 'zoo.cfg' in config['configurations'] and 'clientPort' in config['configurations']['zoo.cfg']:
    zookeeper_clientPort = config['configurations']['zoo.cfg']['clientPort']
  else:
    zookeeper_clientPort = '2181'
  zookeeper_quorum = (':' + zookeeper_clientPort + ',').join(config['clusterHostInfo']['zookeeper_hosts'])
  # last port config
  zookeeper_quorum += ':' + zookeeper_clientPort

#hadoop params

if has_namenode or dfs_type == 'HCFS':
  hadoop_tmp_dir = format("/tmp/hadoop-{hdfs_user}")
  hadoop_conf_dir = conf_select.get_hadoop_conf_dir(force_latest_on_upgrade=True)
  task_log4j_properties_location = os.path.join(hadoop_conf_dir, "task-log4j.properties")

hadoop_pid_dir_prefix = config['configurations']['hadoop-env']['hadoop_pid_dir_prefix']
hdfs_log_dir_prefix = config['configurations']['hadoop-env']['hdfs_log_dir_prefix']
hbase_tmp_dir = "/tmp/hbase-hbase"
#db params
server_db_name = config['hostLevelParams']['db_name']
db_driver_filename = config['hostLevelParams']['db_driver_filename']
oracle_driver_url = config['hostLevelParams']['oracle_jdbc_url']
mysql_driver_url = config['hostLevelParams']['mysql_jdbc_url']
ambari_server_resources = config['hostLevelParams']['jdk_location']
oracle_driver_symlink_url = format("{ambari_server_resources}oracle-jdbc-driver.jar")
mysql_driver_symlink_url = format("{ambari_server_resources}mysql-jdbc-driver.jar")

ambari_db_rca_url = config['hostLevelParams']['ambari_db_rca_url'][0]
ambari_db_rca_driver = config['hostLevelParams']['ambari_db_rca_driver'][0]
ambari_db_rca_username = config['hostLevelParams']['ambari_db_rca_username'][0]
ambari_db_rca_password = config['hostLevelParams']['ambari_db_rca_password'][0]

if has_namenode and 'rca_enabled' in config['configurations']['hadoop-env']:
  rca_enabled =  config['configurations']['hadoop-env']['rca_enabled']
else:
  rca_enabled = False
rca_disabled_prefix = "###"
if rca_enabled == True:
  rca_prefix = ""
else:
  rca_prefix = rca_disabled_prefix

#hadoop-env.sh
java_home = config['hostLevelParams']['java_home']

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
mapred_pid_dir_prefix = default("/configurations/mapred-env/mapred_pid_dir_prefix","/var/run/hadoop-mapreduce")
mapred_log_dir_prefix = default("/configurations/mapred-env/mapred_log_dir_prefix","/var/log/hadoop-mapreduce")

#log4j.properties

yarn_log_dir_prefix = default("/configurations/yarn-env/yarn_log_dir_prefix","/var/log/hadoop-yarn")

dfs_hosts = default('/configurations/hdfs-site/dfs.hosts', None)

#log4j.properties
if (('hdfs-log4j' in config['configurations']) and ('content' in config['configurations']['hdfs-log4j'])):
  log4j_props = config['configurations']['hdfs-log4j']['content']
  if (('yarn-log4j' in config['configurations']) and ('content' in config['configurations']['yarn-log4j'])):
    log4j_props += config['configurations']['yarn-log4j']['content']
else:
  log4j_props = None

refresh_topology = False
command_params = config["commandParams"] if "commandParams" in config else None
if command_params is not None:
  refresh_topology = bool(command_params["refresh_topology"]) if "refresh_topology" in command_params else False
  
ambari_libs_dir = "/var/lib/ambari-agent/lib"
is_webhdfs_enabled = config['configurations']['hdfs-site']['dfs.webhdfs.enabled']
default_fs = config['configurations']['core-site']['fs.defaultFS']

#host info
all_hosts = default("/clusterHostInfo/all_hosts", [])
all_racks = default("/clusterHostInfo/all_racks", [])
all_ipv4_ips = default("/clusterHostInfo/all_ipv4_ips", [])
slave_hosts = default("/clusterHostInfo/slave_hosts", [])

#topology files
net_topology_script_file_path = "/etc/hadoop/conf/topology_script.py"
net_topology_script_dir = os.path.dirname(net_topology_script_file_path)
net_topology_mapping_data_file_name = 'topology_mappings.data'
net_topology_mapping_data_file_path = os.path.join(net_topology_script_dir, net_topology_mapping_data_file_name)

#Added logic to create /tmp and /user directory for HCFS stack.  
has_core_site = 'core-site' in config['configurations']
hdfs_user_keytab = config['configurations']['hadoop-env']['hdfs_user_keytab']
kinit_path_local = get_kinit_path()
stack_version_unformatted = config['hostLevelParams']['stack_version']
stack_version_formatted = format_stack_version(stack_version_unformatted)
hadoop_bin_dir = stack_select.get_hadoop_dir("bin")
hdfs_principal_name = default('/configurations/hadoop-env/hdfs_principal_name', None)
hdfs_site = config['configurations']['hdfs-site']
default_fs = config['configurations']['core-site']['fs.defaultFS']
smoke_user =  config['configurations']['cluster-env']['smokeuser']
smoke_hdfs_user_dir = format("/user/{smoke_user}")
smoke_hdfs_user_mode = 0770


##### Namenode RPC ports - metrics config section start #####

# Figure out the rpc ports for current namenode
nn_rpc_client_port = None
nn_rpc_dn_port = None
nn_rpc_healthcheck_port = None

namenode_id = None
namenode_rpc = None

dfs_ha_enabled = False
dfs_ha_nameservices = default('/configurations/hdfs-site/dfs.internal.nameservices', None)
if dfs_ha_nameservices is None:
  dfs_ha_nameservices = default('/configurations/hdfs-site/dfs.nameservices', None)
dfs_ha_namenode_ids = default(format("/configurations/hdfs-site/dfs.ha.namenodes.{dfs_ha_nameservices}"), None)

dfs_ha_namemodes_ids_list = []
other_namenode_id = None

if dfs_ha_namenode_ids:
 dfs_ha_namemodes_ids_list = dfs_ha_namenode_ids.split(",")
 dfs_ha_namenode_ids_array_len = len(dfs_ha_namemodes_ids_list)
 if dfs_ha_namenode_ids_array_len > 1:
   dfs_ha_enabled = True

if dfs_ha_enabled:
 for nn_id in dfs_ha_namemodes_ids_list:
   nn_host = config['configurations']['hdfs-site'][format('dfs.namenode.rpc-address.{dfs_ha_nameservices}.{nn_id}')]
   if hostname in nn_host:
     namenode_id = nn_id
     namenode_rpc = nn_host
   pass
 pass
else:
 namenode_rpc = default('/configurations/hdfs-site/dfs.namenode.rpc-address', None)

if namenode_rpc:
 nn_rpc_client_port = namenode_rpc.split(':')[1].strip()

if dfs_ha_enabled:
 dfs_service_rpc_address = default(format('/configurations/hdfs-site/dfs.namenode.servicerpc-address.{dfs_ha_nameservices}.{namenode_id}'), None)
 dfs_lifeline_rpc_address = default(format('/configurations/hdfs-site/dfs.namenode.lifeline.rpc-address.{dfs_ha_nameservices}.{namenode_id}'), None)
else:
 dfs_service_rpc_address = default('/configurations/hdfs-site/dfs.namenode.servicerpc-address', None)
 dfs_lifeline_rpc_address = default(format('/configurations/hdfs-site/dfs.namenode.lifeline.rpc-address'), None)

if dfs_service_rpc_address:
 nn_rpc_dn_port = dfs_service_rpc_address.split(':')[1].strip()

if dfs_lifeline_rpc_address:
 nn_rpc_healthcheck_port = dfs_lifeline_rpc_address.split(':')[1].strip()

is_nn_client_port_configured = False if nn_rpc_client_port is None else True
is_nn_dn_port_configured = False if nn_rpc_dn_port is None else True
is_nn_healthcheck_port_configured = False if nn_rpc_healthcheck_port is None else True

##### end #####

import functools
#create partial functions with common arguments for every HdfsResource call
#to create/delete/copyfromlocal hdfs directories/files we need to call params.HdfsResource in code
HdfsResource = functools.partial(
  HdfsResource,
  user=hdfs_user,
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
