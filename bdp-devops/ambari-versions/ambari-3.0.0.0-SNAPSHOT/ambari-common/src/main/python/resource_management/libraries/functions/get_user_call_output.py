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
import sys
import tempfile
from resource_management.core import shell
from resource_management.core.logger import Logger
from resource_management.core.exceptions import ExecutionFailed

def get_user_call_output(command, user, quiet=False, is_checked_call=True, **call_kwargs):
  """
  This function eliminates only output of command inside the su, ignoring the su ouput itself.
  This is useful since some users have motd messages setup by default on su -l. 
  
  @return: code, stdout, stderr
  """
  command_string = shell.string_cmd_from_args_list(command) if isinstance(command, (list, tuple)) else command
  out_files = []
  
  try:
    out_files.append(tempfile.NamedTemporaryFile())
    out_files.append(tempfile.NamedTemporaryFile())
    
    # other user should be able to write to it
    for f in out_files:
      os.chmod(f.name, 0666)
    
    command_string += " 1>" + out_files[0].name
    command_string += " 2>" + out_files[1].name
    
    code, _ = shell.call(shell.as_user(command_string, user), quiet=quiet, **call_kwargs)
    
    files_output = []
    for f in out_files:
      files_output.append(f.read().decode("utf-8").strip('\n'))
      
    if code:
      all_output = files_output[1] + '\n' + files_output[0]
      err_msg = Logger.filter_text(("Execution of '%s' returned %d. %s") % (command_string, code, all_output))
      
      if is_checked_call:
        raise ExecutionFailed(err_msg, code, files_output[0], files_output[1])
      else:
        Logger.warning(err_msg)

    result = code, files_output[0], files_output[1]
    
    caller_filename = sys._getframe(1).f_code.co_filename
    is_internal_call = shell.NOT_LOGGED_FOLDER in caller_filename
    if quiet == False or (quiet == None and not is_internal_call):
      log_msg = "{0} returned {1}".format(get_user_call_output.__name__, result)
      Logger.info(log_msg)

    return result
  finally:
    for f in out_files:
      f.close()
      
  
