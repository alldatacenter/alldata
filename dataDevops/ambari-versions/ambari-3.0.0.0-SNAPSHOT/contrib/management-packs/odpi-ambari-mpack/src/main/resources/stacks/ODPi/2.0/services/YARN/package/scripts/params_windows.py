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

from resource_management import *
from resource_management.libraries import functions
import os
from status_params import *

# server configurations
config = Script.get_config()

hadoop_user = config["configurations"]["cluster-env"]["hadoop.user.name"]
yarn_user = hadoop_user
hdfs_user = hadoop_user
smokeuser = hadoop_user
config_dir = os.environ["HADOOP_CONF_DIR"]
hadoop_home = os.environ["HADOOP_HOME"]

yarn_home = os.environ["HADOOP_YARN_HOME"]

hadoop_ssl_enabled = default("/configurations/core-site/hadoop.ssl.enabled", False)
_authentication = config['configurations']['core-site']['hadoop.security.authentication']
security_enabled = ( not is_empty(_authentication) and _authentication == 'kerberos')
smoke_user_keytab = config['configurations']['hadoop-env']['smokeuser_keytab']
kinit_path_local = functions.get_kinit_path(default('/configurations/kerberos-env/executable_search_paths', None))
rm_host = config['clusterHostInfo']['rm_host'][0]
rm_port = config['configurations']['yarn-site']['yarn.resourcemanager.webapp.address'].split(':')[-1]
rm_https_port = "8090"
rm_webui_address = format("{rm_host}:{rm_port}")
rm_webui_https_address = format("{rm_host}:{rm_https_port}")

hs_host = config['clusterHostInfo']['hs_host'][0]
hs_port = config['configurations']['mapred-site']['mapreduce.jobhistory.webapp.address'].split(':')[-1]
hs_webui_address = format("{hs_host}:{hs_port}")

hadoop_mapred2_jar_location = os.path.join(os.environ["HADOOP_COMMON_HOME"], "share", "hadoop", "mapreduce")
hadoopMapredExamplesJarName = "hadoop-mapreduce-examples-2.*.jar"

exclude_hosts = default("/clusterHostInfo/decom_nm_hosts", [])
exclude_file_path = default("/configurations/yarn-site/yarn.resourcemanager.nodes.exclude-path","/etc/hadoop/conf/yarn.exclude")
update_files_only = default("/commandParams/update_files_only", False)

nm_hosts = default("/clusterHostInfo/nm_hosts", [])
#incude file
include_file_path = default("/configurations/yarn-site/yarn.resourcemanager.nodes.include-path", None)
include_hosts = None
manage_include_files = default("/configurations/yarn-site/manage.include.files", False)
if include_file_path and manage_include_files:
  include_hosts = list(set(nm_hosts) - set(exclude_hosts))
