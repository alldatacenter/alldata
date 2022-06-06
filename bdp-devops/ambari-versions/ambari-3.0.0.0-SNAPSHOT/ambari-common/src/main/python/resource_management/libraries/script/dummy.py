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

__all__ = ["Dummy"]

# Python Imports
import os
import re

# Local Imports
from resource_management.libraries.script.script import Script
from resource_management.core.resources.system import Directory, File, Execute
from ambari_commons.constants import AMBARI_SUDO_BINARY
from resource_management.core.exceptions import ComponentIsNotRunning
from resource_management.core.logger import Logger


from resource_management.libraries.functions.default import default
from resource_management.libraries.functions import get_kinit_path
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions import StackFeature

class Dummy(Script):
  """
  Dummy component to be used for performance testing since doesn't actually run a service.
  Reports status command based on the existence of a pid file.
  """

  # Whether or not configs have been loaded already in the prepare() method.
  loaded = False

  def prepare(self):
    # During restart commands which executes stop + start, avoid loading multiple times.
    if self.loaded:
      return
    self.loaded = True

    self.config = Script.get_config()
    # Cannot rely on system hostname since will run multiple Ambari agents on the same host.
    self.host_name = self.config["agentLevelParams"]["hostname"]

    # Should still define self.component_name which is needed for status commands.
    if "role" in self.config:
      self.component_name = self.config["role"]

    self.pid_file = "/var/run/%s/%s.pid" % (self.host_name, self.component_name)
    self.user = "root"
    self.user_group = "root"
    self.sudo = AMBARI_SUDO_BINARY

    print "Host: %s" % self.host_name
    print "Component: %s" % self.component_name
    print "Pid File: %s" % self.pid_file

  def install(self, env):
    print "Install"
    self.prepare()
    """
    component_name = self.get_component_name()
    repo_info = str(default("/hostLevelParams/repoInfo", "1.1.1.1-1"))
    matches = re.findall(r"([\d\.]+\-\d+)", repo_info)
    version = matches[0] if matches and len(matches) > 0 else "1.1.1.1-1"

    from resource_management.libraries.functions import stack_tools
    (stack_selector_name, stack_selector_path, stack_selector_package) = stack_tools.get_stack_tool(stack_tools.STACK_SELECTOR_NAME)
    command = 'ambari-python-wrap {0} install {1}'.format(stack_selector_path, version)
    Execute(command)

    if component_name:
      conf_select.select("PERF", component_name, version)
      stack_select.select(component_name, version)
    """

  def configure(self, env):
    print "Configure"
    self.prepare()

  def start(self, env, upgrade_type=None):
    print "Start"
    self.prepare()

    if self.config['configurations']['cluster-env']['security_enabled'] :
      print "Executing kinit... "
      kinit_path_local = get_kinit_path(default('/configurations/kerberos-env/executable_search_paths', None))
      principal_replaced = self.config['configurations'][self.principal_conf_name][self.principal_name].replace("_HOST", self.host_name)
      keytab_path_replaced = self.config['configurations'][self.keytab_conf_name][self.keytab_name].replace("_HOST", self.host_name)
      Execute("%s -kt %s %s" % (kinit_path_local, keytab_path_replaced, principal_replaced),
              user="root")

    if not os.path.isfile(self.pid_file):
      print "Creating pid file: %s" % self.pid_file

      Directory(os.path.dirname(self.pid_file),
                owner=self.user,
                group=self.user_group,
                mode=0755,
                create_parents=True
                )

      File(self.pid_file,
           owner=self.user,
           content=""
           )

  def stop(self, env, upgrade_type=None):
    print "Stop"
    self.prepare()

    if os.path.isfile(self.pid_file):
      print "Deleting pid file: %s" % self.pid_file
      Execute("%s rm -rf %s" % (self.sudo, self.pid_file))

  def status(self, env):
    print "Status"
    self.prepare()

    if not os.path.isfile(self.pid_file):
      raise ComponentIsNotRunning()

  def get_component_name(self):
    """
    To be overridden by subclasses.
     Returns a string with the component name used in selecting the version.
    """
    pass