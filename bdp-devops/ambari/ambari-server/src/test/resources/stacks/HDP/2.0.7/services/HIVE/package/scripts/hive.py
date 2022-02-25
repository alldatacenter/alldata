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
import sys


def hive(name=None):
  import params

  if name == 'metastore' or name == 'hiveserver2':
    hive_config_dir = params.hive_server_conf_dir
    config_file_mode = 0600
    jdbc_connector()
  else:
    hive_config_dir = params.hive_conf_dir
    config_file_mode = 0644

  Directory(hive_config_dir,
            owner=params.hive_user,
            group=params.user_group,
            create_parents = True
  )

  XmlConfig("hive-site.xml",
            conf_dir=hive_config_dir,
            configurations=params.config['configurations']['hive-site'],
            configuration_attributes=params.config['configuration_attributes']['hive-site'],
            owner=params.hive_user,
            group=params.user_group,
            mode=config_file_mode
  )

  cmd = format("/bin/sh -c 'cd /usr/lib/ambari-agent/ && curl -kf --retry 5 "
               "{jdk_location}/{check_db_connection_jar_name} -o {check_db_connection_jar_name}'")

  Execute(cmd,
          not_if=format("[ -f {check_db_connection_jar_name}]"))

  if name == 'metastore':
    File(params.start_metastore_path,
         mode=0755,
         content=StaticFile('startMetastore.sh')
    )

  elif name == 'hiveserver2':
    File(params.start_hiveserver2_path,
         mode=0755,
         content=StaticFile('startHiveserver2.sh')
    )

  if name != "client":
    crt_directory(params.hive_pid_dir)
    crt_directory(params.hive_log_dir)
    crt_directory(params.hive_var_lib)

  File(format("{hive_config_dir}/hive-env.sh"),
       owner=params.hive_user,
       group=params.user_group,
       content=Template('hive-env.sh.j2', conf_dir=hive_config_dir)
  )

  crt_file(format("{hive_conf_dir}/hive-default.xml.template"))
  crt_file(format("{hive_conf_dir}/hive-env.sh.template"))
  crt_file(format("{hive_conf_dir}/hive-exec-log4j.properties.template"))
  crt_file(format("{hive_conf_dir}/hive-log4j.properties.template"))


def crt_directory(name):
  import params

  Directory(name,
            create_parents = True,
            owner=params.hive_user,
            group=params.user_group,
            mode=0755)


def crt_file(name):
  import params

  File(name,
       owner=params.hive_user,
       group=params.user_group
  )


def jdbc_connector():
  import params

  if params.hive_jdbc_driver == "com.mysql.jdbc.Driver":
    cmd = format("hive mkdir -p {artifact_dir} ; cp /usr/share/java/{jdbc_jar_name} {target}")

    Execute(cmd,
            not_if=format("test -f {target}"),
            creates=params.target,
            path=["/bin", "/usr/bin/"])

  elif params.hive_jdbc_driver == "oracle.jdbc.driver.OracleDriver":
    cmd = format(
      "mkdir -p {artifact_dir} ; curl -kf --retry 10 {driver_curl_source} -o {driver_curl_target} &&  "
      "cp {driver_curl_target} {target}")

    Execute(cmd,
            not_if=format("test -f {target}"),
            path=["/bin", "/usr/bin/"])
