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

import os
import time

from ambari_commons.constants import UPGRADE_TYPE_ROLLING
from resource_management.core.logger import Logger
from resource_management.core import shell
from resource_management.libraries.functions.format import format
from resource_management.core.resources.system import File, Execute
from resource_management.core.resources.service import Service
from resource_management.core.exceptions import Fail
from resource_management.core.shell import as_user
from resource_management.libraries.functions.hive_check import check_thrift_port_sasl
from resource_management.libraries.functions import get_user_call_output
from resource_management.libraries.functions.show_logs import show_logs
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.functions.stack_features import check_stack_feature

from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl
from ambari_commons import OSConst


@OsFamilyFuncImpl(os_family=OSConst.WINSRV_FAMILY)
def hive_service(name, action='start', upgrade_type=None):
  import params
  if name == 'metastore':
    if action == 'start' or action == 'stop':
      Service(params.hive_metastore_win_service_name, action=action)

  if name == 'hiveserver2':
    if action == 'start' or action == 'stop':
      Service(params.hive_server_win_service_name, action=action)


@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def hive_service(name, action='start', upgrade_type=None):

  import params

  if name == 'metastore':
    pid_file = format("{hive_pid_dir}/{hive_metastore_pid}")
    cmd = format("{start_metastore_path} {hive_log_dir}/hive.out {hive_log_dir}/hive.err {pid_file} {hive_server_conf_dir} {hive_log_dir}")
  elif name == 'hiveserver2':
    pid_file = format("{hive_pid_dir}/{hive_pid}")
    cmd = format("{start_hiveserver2_path} {hive_log_dir}/hive-server2.out {hive_log_dir}/hive-server2.err {pid_file} {hive_server_conf_dir} {hive_log_dir}")


    if params.security_enabled and check_stack_feature(StackFeature.HIVE_SERVER2_KERBERIZED_ENV, params.version_for_stack_feature_checks):
      hive_kinit_cmd = format("{kinit_path_local} -kt {hive_server2_keytab} {hive_principal}; ")
      Execute(hive_kinit_cmd, user=params.hive_user)

  pid = get_user_call_output.get_user_call_output(format("cat {pid_file}"), user=params.hive_user, is_checked_call=False)[1]
  process_id_exists_command = format("ls {pid_file} >/dev/null 2>&1 && ps -p {pid} >/dev/null 2>&1")

  if action == 'start':
    if name == 'hiveserver2':
      check_fs_root(params.hive_server_conf_dir, params.execute_path)

    daemon_cmd = cmd
    hadoop_home = params.hadoop_home
    hive_bin = "hive"

    # upgrading hiveserver2 (rolling_restart) means that there is an existing,
    # de-registering hiveserver2; the pid will still exist, but the new
    # hiveserver is spinning up on a new port, so the pid will be re-written
    if upgrade_type == UPGRADE_TYPE_ROLLING:
      process_id_exists_command = None

      if params.version and params.stack_root:
        hadoop_home = format("{stack_root}/{version}/hadoop")
        hive_bin = os.path.join(params.hive_bin, hive_bin)
      
    Execute(daemon_cmd, 
      user = params.hive_user,
      environment = { 'HADOOP_HOME': hadoop_home, 'JAVA_HOME': params.java64_home, 'HIVE_BIN': hive_bin },
      path = params.execute_path,
      not_if = process_id_exists_command)

    if params.hive_jdbc_driver == "com.mysql.jdbc.Driver" or \
       params.hive_jdbc_driver == "org.postgresql.Driver" or \
       params.hive_jdbc_driver == "oracle.jdbc.driver.OracleDriver":

      validation_called = False

      if params.hive_jdbc_target is not None:
        validation_called = True
        validate_connection(params.hive_jdbc_target, params.hive_lib)
      if params.hive2_jdbc_target is not None:
        validation_called = True
        validate_connection(params.hive2_jdbc_target, params.hive_server2_hive2_lib)

      if not validation_called:
        emessage = "ERROR! DB connection check should be executed at least one time!"
        Logger.error(emessage)

  elif action == 'stop':

    daemon_kill_cmd = format("{sudo} kill {pid}")
    daemon_hard_kill_cmd = format("{sudo} kill -9 {pid}")

    Execute(daemon_kill_cmd,
      not_if = format("! ({process_id_exists_command})")
    )

    wait_time = 5
    Execute(daemon_hard_kill_cmd,
      not_if = format("! ({process_id_exists_command}) || ( sleep {wait_time} && ! ({process_id_exists_command}) )"),
      ignore_failures = True
    )

    try:
      # check if stopped the process, else fail the task
      Execute(format("! ({process_id_exists_command})"),
        tries=20,
        try_sleep=3,
      )
    except:
      show_logs(params.hive_log_dir, params.hive_user)
      raise

    File(pid_file,
         action = "delete"
    )

def validate_connection(target_path_to_jdbc, hive_lib_path):
  import params

  path_to_jdbc = target_path_to_jdbc
  if not params.jdbc_jar_name:
    path_to_jdbc = format("{hive_lib_path}/") + \
                   params.default_connectors_map[params.hive_jdbc_driver] if params.hive_jdbc_driver in params.default_connectors_map else None
    if not os.path.isfile(path_to_jdbc):
      path_to_jdbc = format("{hive_lib_path}/") + "*"
      error_message = "Error! Sorry, but we can't find jdbc driver with default name " + params.default_connectors_map[params.hive_jdbc_driver] + \
                      " in hive lib dir. So, db connection check can fail. Please run 'ambari-server setup --jdbc-db={db_name} --jdbc-driver={path_to_jdbc} on server host.'"
      Logger.error(error_message)

  db_connection_check_command = format(
    "{java64_home}/bin/java -cp {check_db_connection_jar}:{path_to_jdbc} org.apache.ambari.server.DBConnectionVerification '{hive_jdbc_connection_url}' {hive_metastore_user_name} {hive_metastore_user_passwd!p} {hive_jdbc_driver}")

  try:
    Execute(db_connection_check_command,
            path='/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin', tries=5, try_sleep=10)
  except:
    show_logs(params.hive_log_dir, params.hive_user)
    raise


def check_fs_root(conf_dir, execution_path):
  import params

  if not params.fs_root.startswith("hdfs://"):
    Logger.info("Skipping fs root check as fs_root does not start with hdfs://")
    return

  metatool_cmd = format("hive --config {conf_dir} --service metatool")
  cmd = as_user(format("{metatool_cmd} -listFSRoot", env={'PATH': execution_path}), params.hive_user) \
        + format(" 2>/dev/null | grep hdfs:// | cut -f1,2,3 -d '/' | grep -v '{fs_root}' | head -1")
  code, out = shell.call(cmd)

  if code == 0 and out.strip() != "" and params.fs_root.strip() != out.strip():
    out = out.strip()
    cmd = format("{metatool_cmd} -updateLocation {fs_root} {out}")
    Execute(cmd,
            user=params.hive_user,
            environment={'PATH': execution_path}
    )

