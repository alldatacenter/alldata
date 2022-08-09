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

# Used to add hive access to the needed components
def mysql_adduser():
  import params
  
  File(params.mysql_adduser_path,
       mode=0755,
       content=StaticFile('addMysqlUser.sh')
  )
  hive_server_host = format("{hive_server_host}")
  hive_metastore_host = format("{hive_metastore_host}")

  add_metastore_cmd = "bash -x {mysql_adduser_path} {daemon_name} {hive_metastore_user_name} {hive_metastore_user_passwd!p} {hive_metastore_host}"
  add_hiveserver_cmd = "bash -x {mysql_adduser_path} {daemon_name} {hive_metastore_user_name} {hive_metastore_user_passwd!p} {hive_server_host}"
  if (hive_server_host == hive_metastore_host):
    cmd = format(add_hiveserver_cmd)
  else:
    cmd = format(add_hiveserver_cmd + ";" + add_metastore_cmd)
  Execute(cmd,
          tries=3,
          try_sleep=5,
          logoutput=False,
          path='/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'
  )

# Removes hive access from components
def mysql_deluser():
  import params
  
  File(params.mysql_deluser_path,
       mode=0755,
       content=StaticFile('removeMysqlUser.sh')
  )
  hive_server_host = format("{hive_server_host}")
  hive_metastore_host = format("{hive_metastore_host}")

  del_hiveserver_cmd = "bash -x {mysql_deluser_path} {daemon_name} {hive_metastore_user_name} {hive_server_host}"
  del_metastore_cmd = "bash -x {mysql_deluser_path} {daemon_name} {hive_metastore_user_name} {hive_metastore_host}"
  if (hive_server_host == hive_metastore_host):
    cmd = format(del_hiveserver_cmd)
  else:
    cmd = format(
      del_hiveserver_cmd + ";" + del_metastore_cmd)
  Execute(cmd,
          tries=3,
          try_sleep=5,
          path='/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin',
  )

