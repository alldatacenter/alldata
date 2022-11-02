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

from resource_management.core import shell
from resource_management import functions
from resource_management.libraries.functions.default import default
from subprocess import PIPE

class Klist:
  @staticmethod
  def find_in_search_path():
    return Klist(functions.get_klist_path(default('/configurations/kerberos-env/executable_search_paths', None)))

  def __init__(self, klist_cmd="klist"):
    self.klist_cmd = klist_cmd

  def list_principals(self, keytab_path):
    code, out, err = shell.call(self._command('-k', keytab_path), stdout = PIPE, stderr = PIPE, timeout = 5, sudo=True)
    if code != 0 or (not out.startswith("Keytab name")):
      raise Exception('Cannot run klist: %s. Exit code: %d. Error: %s' % (self.klist_cmd, code, err))
    return set(line.split()[1] for line in out.split("\n")[3:])

  def _command(self, *args):
    cmd = [self.klist_cmd]
    cmd.extend(args)
    return cmd