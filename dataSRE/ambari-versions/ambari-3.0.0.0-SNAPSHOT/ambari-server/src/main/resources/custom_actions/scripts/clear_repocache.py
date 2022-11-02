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
from resource_management import Script, Execute, format
from ambari_commons.os_check import OSCheck
from resource_management.core import shell
from resource_management.core.logger import Logger

class ClearRepoCache(Script):

  def actionexecute(self, env):
    config = Script.get_config()
    structured_output = {}
    cmd = self.get_clearcache_cmd()

    Logger.info("Clearing repository cache")
    code, output = shell.call(cmd, sudo = True)
    if 0 == code:
      structured_output["clear_repocache"] = {"exit_code" : 0, "message": format("Repository cache successfully cleared!")}
    else:
      structured_output["clear_repocache"] = {"exit_code": code, "message": "Failed to clear repository cache! {0}".format(str(output))}
    self.put_structured_out(structured_output)

  def get_clearcache_cmd(self):
    if OSCheck.is_redhat_family():
      Logger.info("Clear repository cache for the RedHat OS family");
      return ("/usr/bin/yum", "clean", "all")
    elif OSCheck.is_suse_family():
      Logger.info("Clear repository cache for the SUSE OS family");
      return ('/usr/bin/zypper', 'refresh')
    elif OSCheck.is_ubuntu_family():
      Logger.info("Clear repository cache for the Ubuntu OS family");
      return ('/usr/bin/apt-get', 'update')
    else:
      raise Exception("Unsupported OS family: '{0}' ".format(OSCheck.get_os_family()))

if __name__ == "__main__":
  ClearRepoCache().execute()
