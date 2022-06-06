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

Ambari Agent

"""

from resource_management.core.resources.system import Execute
from resource_management.core.logger import Logger
from resource_management.libraries.functions import format

class ZkMigrator:
  def __init__(self, zk_host, java_exec, java_home, jaas_file, user):
    self.zk_host = zk_host
    self.java_exec = java_exec
    self.java_home = java_home
    self.jaas_file = jaas_file
    self.user = user
    self.zkmigrator_jar = "/var/lib/ambari-agent/tools/zkmigrator.jar"

  def set_acls(self, znode, acl, tries=3):
    Logger.info(format("Setting ACL on znode {znode} to {acl}"))
    Execute(
      self._acl_command(znode, acl), \
      user=self.user, \
      environment={ 'JAVA_HOME': self.java_home }, \
      logoutput=True, \
      tries=tries)

  def delete_node(self, znode, tries=3):
    Logger.info(format("Removing znode {znode}"))
    Execute(
      self._delete_command(znode), \
      user=self.user, \
      environment={ 'JAVA_HOME': self.java_home }, \
      logoutput=True, \
      tries=tries)

  def _acl_command(self, znode, acl):
    return "{0} -Djava.security.auth.login.config={1} -jar {2} -connection-string {3} -znode {4} -acl {5}".format( \
      self.java_exec, self.jaas_file, self.zkmigrator_jar, self.zk_host, znode, acl)

  def _delete_command(self, znode):
    return "{0} -Djava.security.auth.login.config={1} -jar {2} -connection-string {3} -znode {4} -delete".format( \
      self.java_exec, self.jaas_file, self.zkmigrator_jar, self.zk_host, znode)
