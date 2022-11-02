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

# Python Imports

# Ambari Commons & Resource Management imports
import os
from resource_management.core.logger import Logger
from resource_management.libraries.functions.format import format
from resource_management.core.resources.system import File, Execute
from resource_management.libraries.functions import get_user_call_output
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl
from ambari_commons import OSConst

# Local Imports
from hive_service import check_fs_root


@OsFamilyFuncImpl(os_family=OSConst.WINSRV_FAMILY)
def hive_service_interactive(name, action='start', upgrade_type=None):
  pass


@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def hive_service_interactive(name, action='start', upgrade_type=None):
  import params

  pid_file = format("{hive_pid_dir}/{hive_interactive_pid}")
  cmd = format("{start_hiveserver2_interactive_path} {hive_pid_dir}/hive-server2-interactive.out {hive_log_dir}/hive-server2-interactive.err {pid_file} {hive_server_interactive_conf_dir} {hive_log_dir}")

  pid = get_user_call_output.get_user_call_output(format("cat {pid_file}"), user=params.hive_user, is_checked_call=False)[1]
  process_id_exists_command = format("ls {pid_file} >/dev/null 2>&1 && ps -p {pid} >/dev/null 2>&1")

  if action == 'start':
    check_fs_root(params.hive_server_interactive_conf_dir, params.execute_path_hive_interactive)
    daemon_cmd = cmd
    hadoop_home = params.hadoop_home
    hive_interactive_bin = "hive2"

    Execute(daemon_cmd,
            user = params.hive_user,
            environment = { 'HADOOP_HOME': hadoop_home, 'JAVA_HOME': params.java64_home, 'HIVE_BIN': hive_interactive_bin },
            path = params.execute_path,
            not_if = process_id_exists_command)

    if params.hive_jdbc_driver == "com.mysql.jdbc.Driver" or \
        params.hive_jdbc_driver == "org.postgresql.Driver" or \
        params.hive_jdbc_driver == "oracle.jdbc.driver.OracleDriver":

      path_to_jdbc = params.target_hive_interactive
      if not params.jdbc_jar_name:
        path_to_jdbc = format("{hive_interactive_lib}/") + \
                       params.default_connectors_map[params.hive_jdbc_driver] if params.hive_jdbc_driver in params.default_connectors_map else None
        if not os.path.isfile(path_to_jdbc):
          path_to_jdbc = format("{hive_interactive_lib}/") + "*"
          error_message = "Error! Sorry, but we can't find jdbc driver with default name " + params.default_connectors_map[params.hive_jdbc_driver] + \
                " in hive lib dir. So, db connection check can fail. Please run 'ambari-server setup --jdbc-db={db_name} --jdbc-driver={path_to_jdbc} on server host.'"
          Logger.error(error_message)

      db_connection_check_command = format(
        "{java64_home}/bin/java -cp {check_db_connection_jar}:{path_to_jdbc} org.apache.ambari.server.DBConnectionVerification '{hive_jdbc_connection_url}' {hive_metastore_user_name} {hive_metastore_user_passwd!p} {hive_jdbc_driver}")
      Execute(db_connection_check_command,
              path='/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin', tries=5, try_sleep=10)
  elif action == 'stop':

    daemon_kill_cmd = format("{sudo} kill {pid}")
    daemon_hard_kill_cmd = format("{sudo} kill -9 {pid}")

    Execute(daemon_kill_cmd,
            not_if = format("! ({process_id_exists_command})")
            )

    # check if stopped the process, otherwise send hard kill command.
    try:
      Execute(format("! ({process_id_exists_command})"),
              tries=10,
              try_sleep=3,
              )
    except:
      Execute(daemon_hard_kill_cmd,
              not_if = format("! ({process_id_exists_command}) ")
              )

    # check if stopped the process, else fail the task
    Execute(format("! ({process_id_exists_command})"),
            tries=20,
            try_sleep=3,
            )

    File(pid_file,
         action = "delete"
         )
