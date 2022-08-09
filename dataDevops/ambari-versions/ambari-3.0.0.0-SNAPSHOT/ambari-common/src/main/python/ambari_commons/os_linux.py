#!/usr/bin/env python

'''
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
'''
import getpass

import os
import pwd
import shlex
from ambari_commons import subprocess32

from ambari_commons.logging_utils import print_info_msg, print_warning_msg


NR_CHMOD_CMD = 'chmod {0} {1} {2}'
NR_CHOWN_CMD = 'chown {0} {1} {2}'
WARN_MSG = "Command {0} returned exit code {1} with message: {2}"

ULIMIT_CMD = "ulimit -n"


def os_run_os_command(cmd, env=None, shell=False, cwd=None):
  print_info_msg('about to run command: ' + str(cmd))
  if type(cmd) == str:
    cmd = shlex.split(cmd)
  process = subprocess32.Popen(cmd,
                             stdout=subprocess32.PIPE,
                             stdin=subprocess32.PIPE,
                             stderr=subprocess32.PIPE,
                             env=env,
                             cwd=cwd,
                             shell=shell
  )
  print_info_msg("\nprocess_pid=" + str(process.pid))
  (stdoutdata, stderrdata) = process.communicate()
  return process.returncode, stdoutdata, stderrdata

def os_change_owner(filePath, user, recursive):
  if recursive:
    params = " -R -L"
  else:
    params = ""
  command = NR_CHOWN_CMD.format(params, user, filePath)
  retcode, out, err = os_run_os_command(command)
  if retcode != 0:
    print_warning_msg(WARN_MSG.format(command, filePath, err))

def os_is_root():
  '''
  Checks effective UUID
  Returns True if a program is running under root-level privileges.
  '''
  return os.geteuid() == 0

def os_set_file_permissions(file, mod, recursive, user):
  if recursive:
    params = " -R "
  else:
    params = ""
  command = NR_CHMOD_CMD.format(params, mod, file)
  retcode, out, err = os_run_os_command(command)
  if retcode != 0:
    print_warning_msg(WARN_MSG.format(command, file, err))
  os_change_owner(file, user, recursive)

def os_set_open_files_limit(maxOpenFiles):
  command = "%s %s" % (ULIMIT_CMD, str(maxOpenFiles))
  os_run_os_command(command)

def os_getpass(prompt):
  return getpass.unix_getpass(prompt)
