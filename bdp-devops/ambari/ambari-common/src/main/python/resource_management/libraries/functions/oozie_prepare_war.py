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

import os

from resource_management.core.logger import Logger
from resource_management.core import shell
from resource_management.core.exceptions import Fail
from resource_management.core.resources.system import File
from resource_management.libraries.functions import format
from resource_management.libraries.functions import get_user_call_output

def prepare_war(params):
  """
  Attempt to call prepare-war command if the marker files don't exist or their content doesn't equal the expected.
  The marker file for a command is stored in <stack-root>/current/oozie-server/.prepare_war_cmd
  The marker file for a content of libext folder is stored in <stack-root>/current/oozie-server/.war_libext_content
  """

  prepare_war_cmd_file = format("{oozie_home}/.prepare_war_cmd")
  libext_content_file = format("{oozie_home}/.war_libext_content")
  list_libext_command = format("ls -l {oozie_libext_dir}") + " | awk '{print $9, $5}' | awk 'NF > 0'"

  # DON'T CHANGE THE VALUE SINCE IT'S USED TO DETERMINE WHETHER TO RUN THE COMMAND OR NOT BY READING THE MARKER FILE.
  # Oozie tmp dir should be /var/tmp/oozie and is already created by a function above.
  command = format("cd {oozie_tmp_dir} && {oozie_setup_sh} prepare-war {oozie_secure}").strip()
  # oozie_setup_sh and oozie_setup_sh_current are different during Ambaripreupload
  command_to_file = format("cd {oozie_tmp_dir} && {oozie_setup_sh_current} prepare-war {oozie_secure}").strip()

  run_prepare_war = False
  if os.path.exists(prepare_war_cmd_file):
    cmd = ""
    with open(prepare_war_cmd_file, "r") as f:
      cmd = f.readline().strip()

    if command_to_file != cmd:
      run_prepare_war = True
      Logger.info(format("Will run prepare war cmd since marker file {prepare_war_cmd_file} has contents which differ.\n" \
                         "Expected: {command_to_file}.\nActual: {cmd}."))
  else:
    run_prepare_war = True
    Logger.info(format("Will run prepare war cmd since marker file {prepare_war_cmd_file} is missing."))

  return_code, libext_content, error_output = get_user_call_output.get_user_call_output(list_libext_command, user=params.oozie_user)
  libext_content = libext_content.strip()

  if run_prepare_war == False:
    if os.path.exists(libext_content_file):
      old_content = ""
      with open(libext_content_file, "r") as f:
        old_content = f.read().strip()

      if libext_content != old_content:
        run_prepare_war = True
        Logger.info(format("Will run prepare war cmd since marker file {libext_content_file} has contents which differ.\n" \
                           "Content of the folder {oozie_libext_dir} changed."))
    else:
      run_prepare_war = True
      Logger.info(format("Will run prepare war cmd since marker file {libext_content_file} is missing."))

  if run_prepare_war:
    # Time-consuming to run
    return_code, output = shell.call(command, user=params.oozie_user)
    if output is None:
      output = ""

    if return_code != 0 or "New Oozie WAR file with added".lower() not in output.lower():
      message = "Unexpected Oozie WAR preparation output {0}".format(output)
      Logger.error(message)
      raise Fail(message)

    # Generate marker files
    File(prepare_war_cmd_file,
         content=command_to_file,
         mode=0644,
         )
    File(libext_content_file,
         content=libext_content,
         mode=0644,
         )
  else:
    Logger.info(format("No need to run prepare-war since marker file {prepare_war_cmd_file} already exists."))
