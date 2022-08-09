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

import nifi_ca_util, os, time

from resource_management.core.exceptions import ComponentIsNotRunning
from resource_management.core.resources.system import Directory, Execute
from resource_management.core.sudo import kill, read_file, path_isfile, unlink
from resource_management.libraries.functions.check_process_status import check_process_status
from resource_management.libraries.script.script import Script
from resource_management.core.resources import File
from signal import SIGTERM, SIGKILL

class CertificateAuthority(Script):
  def install(self, env):
    import params
    import status_params

    self.install_packages(env)

    #Be sure ca script is in cache
    nifi_ca_util.get_toolkit_script('tls-toolkit.sh')

  def configure(self, env):
    import params
    import status_params
    env.set_params(params)
    env.set_params(status_params)

    #create the log, pid, conf dirs if not already present
    Directory([status_params.nifi_pid_dir, params.nifi_node_log_dir, params.nifi_config_dir],
      owner=params.nifi_user,
      group=params.nifi_group,
      create_parents=True
    )

    ca_json = os.path.join(params.nifi_config_dir, 'nifi-certificate-authority.json')
    ca_dict = nifi_ca_util.load(ca_json)
    nifi_ca_util.overlay(ca_dict, params.nifi_ca_config)
    nifi_ca_util.dump(ca_json, ca_dict)

    Directory([params.nifi_config_dir],
        owner=params.nifi_user,
        group=params.nifi_group,
        create_parents=True,
        recursive_ownership=True
    )

  def invalidate_ca_server(self, env):
    import params
    ca_json = os.path.join(params.nifi_config_dir, 'nifi-certificate-authority.json')
    nifi_ca_util.move_store(nifi_ca_util.load(ca_json), 'keyStore')
    unlink(ca_json)
    
  def status(self, env):
    import status_params
    check_process_status(status_params.nifi_ca_pid_file)

  def start(self, env):
    import params
    import status_params

    self.configure(env)
    ca_server_script = nifi_ca_util.get_toolkit_script('tls-toolkit.sh')
    run_ca_script = os.path.join(os.path.dirname(__file__), 'run_ca.sh')
    Directory([params.nifi_config_dir],
        owner=params.nifi_user,
        group=params.nifi_group,
        create_parents=True,
        recursive_ownership=True
    )

    File(ca_server_script, mode=0755)
    File(run_ca_script, mode=0755) 
    Execute((run_ca_script, params.jdk64_home, ca_server_script, params.nifi_config_dir + '/nifi-certificate-authority.json', params.nifi_ca_log_file_stdout, params.nifi_ca_log_file_stderr, status_params.nifi_ca_pid_file), user=params.nifi_user)
    if not os.path.isfile(status_params.nifi_ca_pid_file):
      raise Exception('Expected pid file to exist')

  def stop(self, env):
    import status_params

    if path_isfile(status_params.nifi_ca_pid_file):
      try:
        self.status(env)
        pid = int(read_file(status_params.nifi_ca_pid_file))
        for i in range(25):
          kill(pid, SIGTERM)
          time.sleep(1)
          self.status(env)
        kill(pid, SIGKILL)
        time.sleep(5)
        self.status(env)
      except ComponentIsNotRunning:
        unlink(status_params.nifi_ca_pid_file)

if __name__ == "__main__":
  CertificateAuthority().execute()
