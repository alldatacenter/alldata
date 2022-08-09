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

import sys
from resource_management import *

from mysql_service import mysql_service

class MysqlServer(Script):

  if System.get_instance().os_family == "suse":
    daemon_name = 'mysql'
  else:
    daemon_name = 'mysqld'

  def install(self, env):
    self.install_packages(env)
    self.configure(env)

  def configure(self, env):
    import params
    env.set_params(params)

    mysql_service(daemon_name=self.daemon_name, action='start')

    File(params.mysql_adduser_path,
         mode=0755,
         content=StaticFile('addMysqlUser.sh')
    )

    # Autoescaping
    cmd = ("bash", "-x", params.mysql_adduser_path, self.daemon_name,
           params.hive_metastore_user_name, params.hive_metastore_user_passwd, params.mysql_host[0])

    Execute(cmd,
            tries=3,
            try_sleep=5,
            path='/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin',
            logoutput=True
    )

    mysql_service(daemon_name=self.daemon_name, action='stop')

  def start(self, env):
    import params
    env.set_params(params)

    mysql_service(daemon_name=self.daemon_name, action = 'start')

  def stop(self, env):
    import params
    env.set_params(params)

    mysql_service(daemon_name=self.daemon_name, action = 'stop')

  def status(self, env):
    mysql_service(daemon_name=self.daemon_name, action = 'status')

if __name__ == "__main__":
  MysqlServer().execute()
