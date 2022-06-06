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
import os
import mysql_users
from resource_management import *

from mysql_service import mysql_service
from mysql_utils import mysql_configure


class MysqlServer(Script):
  def install(self, env):
    import params
    self.install_packages(env)
    self.configure(env)

  def clean(self, env):
    import params
    env.set_params(params)
    mysql_users.mysql_deluser()

  def configure(self, env):
    import params
    env.set_params(params)
    mysql_configure()

  def start(self, env, rolling_restart=False):
    import params
    env.set_params(params)
    mysql_service(daemon_name=params.daemon_name, action='start')

  def stop(self, env, rolling_restart=False):
    import params
    env.set_params(params)
    mysql_service(daemon_name=params.daemon_name, action='stop')

  def status(self, env):
    import status_params
    env.set_params(status_params)

    mysql_service(daemon_name=status_params.daemon_name, action='status')


if __name__ == "__main__":
  MysqlServer().execute()
