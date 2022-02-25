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
import os

from resource_management.core.resources import Execute
from resource_management.core.providers import Provider
from resource_management.libraries.functions.format import format
from resource_management.core.environment import Environment
from resource_management.core.shell import quote_bash_args

class ExecuteHadoopProvider(Provider):
  def action_run(self):
    conf_dir = self.resource.conf_dir
    command = self.resource.command
    
    if isinstance(command, (list, tuple)):
      command = ' '.join(quote_bash_args(x) for x in command)

    Execute (format("hadoop --config {conf_dir} {command}"),
      user        = self.resource.user,
      tries       = self.resource.tries,
      try_sleep   = self.resource.try_sleep,
      logoutput   = self.resource.logoutput,
      path        = self.resource.bin_dir,
      environment = self.resource.environment,
    )
