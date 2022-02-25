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
import re
import os
import ambari_simplejson as json # simplejson is much faster comparing to Python 2.6 json module and has the same functions set.
import socket
import traceback

from resource_management.libraries.script import Script
from resource_management.libraries.functions.default import default
from resource_management.core import shell
from resource_management.core.exceptions import Fail
from resource_management.core.logger import Logger
from ambari_agent.FileCache import FileCache
from ambari_agent.AmbariConfig import AmbariConfig

agent_config = AmbariConfig()


class ExecuteTask:
  """
  Encapsulate a task that can be executed on the agent.
  An equivalent class exists in the Java server-side, called ExecuteTask.java
  """

  def __init__(self, t):
    """
    @:param t: Dictionary with string representation
    """
    self.type = t["type"] if "type" in t else None
    self.hosts = t["hosts"] if "hosts" in t else None
    self.script = t["script"] if "script" in t else None
    self.function = t["function"] if "function" in t else None
    self.command = t["command"] if "command" in t else None

  def __str__(self):
    inner = []
    if self.type:
      inner.append("Type: %s" % str(self.type))
    if self.script and self.function:
      inner.append("Script: %s - Function: %s" % (str(self.script), str(self.function)))
    elif self.command:
      inner.append("Command: %s" % str(self.command))
    return "Task. %s" % ", ".join(inner)


def replace_variables(cmd, host_name, version):
  if cmd:
    cmd = cmd.replace("{{host_name}}", "{host_name}")
    cmd = cmd.replace("0.0.0.0", "{host_name}")
    cmd = cmd.replace("{{version}}", "{version}")
    cmd = format(cmd)
  return cmd


def resolve_ambari_config():
  config_path = os.path.abspath(AmbariConfig.getConfigFile())
  try:
    if os.path.exists(config_path):
      agent_config.read(config_path)
    else:
      raise Exception("No config found at %s" % str(config_path))
  except Exception, err:
    traceback.print_exc()
    Logger.warning(err)


class ExecuteUpgradeTasks(Script):
  """
  This script is a part of Rolling Upgrade workflow and is described at
  appropriate design doc.

  It executes tasks used for rolling upgrades.
  """

  def actionexecute(self, env):
    resolve_ambari_config()

    # Parse parameters from command json file.
    config = Script.get_config()

    host_name = socket.gethostname()
    version = default('/roleParams/version', None)

    # These 2 variables are optional
    service_package_folder = default('/commandParams/service_package_folder', None)
    if service_package_folder is None:
      service_package_folder = default('/serviceLevelParams/service_package_folder', None)
    hooks_folder = default('/commandParams/hooks_folder', None)

    tasks = json.loads(config['roleParams']['tasks'])
    if tasks:
      for t in tasks:
        task = ExecuteTask(t)
        Logger.info(str(task))

        # If a (script, function) exists, it overwrites the command.
        if task.script and task.function:
          file_cache = FileCache(agent_config)

          if service_package_folder and hooks_folder:
            command_paths = {
              "commandParams": {
                "service_package_folder": service_package_folder,
              },
              "clusterLevelParams": {
                   "hooks_folder": hooks_folder
              },
              "ambariLevelParams": {
                "jdk_location": default('/ambariLevelParams/jdk_location', "")
              }
            } 

            base_dir = file_cache.get_service_base_dir(command_paths)
          else:
            base_dir = file_cache.get_custom_actions_base_dir({
              "ambariLevelParams": {
                "jdk_location": default('/ambariLevelParams/jdk_location', "")
              }
            })

          script_path = os.path.join(base_dir, task.script)
          if not os.path.exists(script_path):
            message = "Script %s does not exist" % str(script_path)
            raise Fail(message)

          # Notice that the script_path is now the fully qualified path, and the
          # same command-#.json file is used.
          # Also, the python wrapper is used, since it sets up the correct environment variables
          command_params = ["/usr/bin/ambari-python-wrap",
                            script_path,
                            task.function,
                            self.command_data_file,
                            self.basedir,
                            self.stroutfile,
                            self.logging_level,
                            Script.get_tmp_dir()]

          task.command = "source /var/lib/ambari-agent/ambari-env.sh ; " + " ".join(command_params)
          # Replace redundant whitespace to make the unit tests easier to validate
          task.command = re.sub("\s+", " ", task.command).strip()

        if task.command:
          task.command = replace_variables(task.command, host_name, version)
          shell.checked_call(task.command, logoutput=True, quiet=True)


if __name__ == "__main__":
  ExecuteUpgradeTasks().execute()
