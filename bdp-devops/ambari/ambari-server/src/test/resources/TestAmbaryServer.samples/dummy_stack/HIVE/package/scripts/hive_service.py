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


def hive_service(
    name,
    action='start'):

  import params

  if name == 'metastore':
    pid_file = format("{hive_pid_dir}/{hive_metastore_pid}")
    cmd = format(
      "env HADOOP_HOME={hadoop_home} JAVA_HOME={java64_home} {start_metastore_path} {hive_log_dir}/hive.out {hive_log_dir}/hive.log {pid_file} {hive_server_conf_dir}")
  elif name == 'hiveserver2':
    pid_file = format("{hive_pid_dir}/{hive_pid}")
    cmd = format(
      "env JAVA_HOME={java64_home} {start_hiveserver2_path} {hive_log_dir}/hive-server2.out {hive_log_dir}/hive-server2.log {pid_file} {hive_server_conf_dir}")

  if action == 'start':
    demon_cmd = format("{cmd}")
    no_op_test = format("ls {pid_file} >/dev/null 2>&1 && ps `cat {pid_file}` >/dev/null 2>&1")
    Execute(demon_cmd,
            user=params.hive_user,
            not_if=no_op_test
    )

    if params.hive_jdbc_driver == "com.mysql.jdbc.Driver" or params.hive_jdbc_driver == "oracle.jdbc.driver.OracleDriver":
      db_connection_check_command = format(
        "{java64_home}/bin/java -cp {check_db_connection_jar}:/usr/share/java/{jdbc_jar_name} org.apache.ambari.server.DBConnectionVerification '{hive_jdbc_connection_url}' {hive_metastore_user_name} {hive_metastore_user_passwd} {hive_jdbc_driver}")
      Execute(db_connection_check_command,
              path='/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin')

  elif action == 'stop':
    demon_cmd = format("kill `cat {pid_file}` >/dev/null 2>&1 && rm -f {pid_file}")
    Execute(demon_cmd)

