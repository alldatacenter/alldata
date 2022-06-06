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

from resource_management import *
from status_params import *

# server configurations
config = Script.get_config()

# This is expected to be of the form #.#.#.#
stack_version_unformatted = config['hostLevelParams']['stack_version']
stack_version_formatted = format_stack_version(stack_version_unformatted)

stack_root = None
hive_conf_dir = None
hive_home = None
hive_lib_dir = None
hive_log_dir = None
hive_opts = None
hcat_home = None
hcat_config_dir = None
hive_bin = None

try:
  stack_root = os.path.abspath(os.path.join(os.environ["HADOOP_HOME"],".."))
  hive_conf_dir = os.environ["HIVE_CONF_DIR"]
  hive_home = os.environ["HIVE_HOME"]
  hive_lib_dir = os.environ["HIVE_LIB_DIR"]
  hive_log_dir = os.environ["HIVE_LOG_DIR"]
  hive_opts = os.environ["HIVE_OPTS"]
  hcat_home = os.environ["HCAT_HOME"]
  hcat_config_dir = os.environ["WEBHCAT_CONF_DIR"]
  hive_bin = os.path.join(hive_home, "bin")
except:
  pass

hive_env_sh_template = config['configurations']['hive-env']['content']
hive_warehouse_dir = config['configurations']['hive-site']['hive.metastore.warehouse.dir']
hadoop_user = config["configurations"]["cluster-env"]["hadoop.user.name"]
hive_user = hadoop_user
hcat_user = hadoop_user

hive_metastore_db_type = config['configurations']['hive-env']['hive_database_type']
hive_metastore_user_name = config['configurations']['hive-site']['javax.jdo.option.ConnectionUserName']
hive_metastore_user_passwd = config['configurations']['hive-site']['javax.jdo.option.ConnectionPassword']

hive_execution_engine = config["configurations"]["hive-site"]["hive.execution.engine"]

######## Metastore Schema
init_metastore_schema = not config['configurations']['hive-site']['datanucleus.autoCreateSchema']

service_map = {
  "metastore" : hive_metastore_win_service_name,
  "client" : hive_client_win_service_name,
  "hiveserver2" : hive_server_win_service_name,
  "templeton" : webhcat_server_win_service_name
}
