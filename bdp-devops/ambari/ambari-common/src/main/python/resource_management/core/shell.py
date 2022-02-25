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

__all__ = ["non_blocking_call", "checked_call", "call", "quote_bash_args", "as_user", "as_sudo"]

import time
import copy
import os
import select
import sys
import logging
import string
from ambari_commons import subprocess32
import threading
import traceback
from exceptions import Fail, ExecutionFailed, ExecuteTimeoutException
from resource_management.core.logger import Logger
from resource_management.core import utils
from ambari_commons.constants import AMBARI_SUDO_BINARY
from resource_management.core.signal_utils import TerminateStrategy, terminate_process

# use quiet=True calls from this folder (logs get too messy duplicating the resources with its commands)
NOT_LOGGED_FOLDER = 'resource_management/core'
EXPORT_PLACEHOLDER = "[RMF_EXPORT_PLACEHOLDER]"
ENV_PLACEHOLDER = "[RMF_ENV_PLACEHOLDER]"

PLACEHOLDERS_TO_STR = {
  EXPORT_PLACEHOLDER: "export {env_str} > /dev/null ; ",
  ENV_PLACEHOLDER: "{env_str}"
}

def log_function_call(function):
  def inner(command, **kwargs):
    caller_filename = sys._getframe(1).f_code.co_filename
    # quiet = can be False/True or None -- which means undefined yet
    quiet = kwargs['quiet'] if 'quiet' in kwargs else None
    is_internal_call = NOT_LOGGED_FOLDER in caller_filename
    
    if quiet == False or (quiet == None and not is_internal_call):
      command_repr = Logger._get_resource_name_repr(command)
      log_msg = Logger.get_function_repr("{0}[{1}]".format(function.__name__, command_repr), kwargs)
      Logger.info(log_msg)
      
    # logoutput=False - never log
    # logoutput=True - log in INFO level
    # logouput=None - log in DEBUG level
    # logouput=not-specified - log in DEBUG level, not counting internal calls
    if 'logoutput' in function.func_code.co_varnames:
      kwargs['logoutput'] = ('logoutput' in kwargs and kwargs['logoutput'] and Logger.isEnabledFor(logging.INFO)) or \
        ('logoutput' in kwargs and kwargs['logoutput']==None and Logger.isEnabledFor(logging.DEBUG)) or \
        (not 'logoutput' in kwargs and not is_internal_call and Logger.isEnabledFor(logging.DEBUG))
       
    result = function(command, **kwargs)
    
    if quiet == False or (quiet == None and not is_internal_call):
      log_msg = "{0} returned {1}".format(function.__name__, result)
      Logger.info(log_msg)
      
    return result
    
  return inner

def preexec_fn():
  processId = os.getpid()
  try:
    os.setpgid(processId, processId)
  except:
    Logger.exception('setpgid({0}, {0}) failed'.format(processId))
    raise

@log_function_call
def checked_call(command, quiet=False, logoutput=None, stdout=subprocess32.PIPE,stderr=subprocess32.STDOUT,
         cwd=None, env=None, preexec_fn=preexec_fn, user=None, wait_for_finish=True, timeout=None, on_timeout=None,
         path=None, sudo=False, on_new_line=None, tries=1, try_sleep=0, timeout_kill_strategy=TerminateStrategy.TERMINATE_PARENT, returns=[0]):
  """
  Execute the shell command and throw an exception on failure.
  @throws Fail
  @return: return_code, output
  """
  return _call_wrapper(command, logoutput=logoutput, throw_on_failure=True, stdout=stdout, stderr=stderr,
                              cwd=cwd, env=env, preexec_fn=preexec_fn, user=user, wait_for_finish=wait_for_finish, 
                              on_timeout=on_timeout, timeout=timeout, path=path, sudo=sudo, on_new_line=on_new_line,
                              tries=tries, try_sleep=try_sleep, timeout_kill_strategy=timeout_kill_strategy, returns=returns)
  
@log_function_call
def call(command, quiet=False, logoutput=None, stdout=subprocess32.PIPE,stderr=subprocess32.STDOUT,
         cwd=None, env=None, preexec_fn=preexec_fn, user=None, wait_for_finish=True, timeout=None, on_timeout=None,
         path=None, sudo=False, on_new_line=None, tries=1, try_sleep=0, timeout_kill_strategy=TerminateStrategy.TERMINATE_PARENT, returns=[0]):
  """
  Execute the shell command despite failures.
  @return: return_code, output
  """
  return _call_wrapper(command, logoutput=logoutput, throw_on_failure=False, stdout=stdout, stderr=stderr,
                              cwd=cwd, env=env, preexec_fn=preexec_fn, user=user, wait_for_finish=wait_for_finish, 
                              on_timeout=on_timeout, timeout=timeout, path=path, sudo=sudo, on_new_line=on_new_line,
                              tries=tries, try_sleep=try_sleep, timeout_kill_strategy=timeout_kill_strategy, returns=returns)

@log_function_call
def non_blocking_call(command, quiet=False, stdout=None, stderr=None,
         cwd=None, env=None, preexec_fn=preexec_fn, user=None, timeout=None, path=None, sudo=False):
  """
  Execute the shell command and don't wait until it's completion
  
  @return: process object -- Popen instance 
  (use proc.stdout.readline to read output in cycle, don't foget to proc.stdout.close(),
  to get return code use proc.wait() and after that proc.returncode)
  """
  return _call_wrapper(command, logoutput=False, throw_on_failure=True, stdout=stdout, stderr=stderr, 
                              cwd=cwd, env=env, preexec_fn=preexec_fn, user=user, wait_for_finish=False, 
                              on_timeout=None, timeout=timeout, path=path, sudo=sudo, on_new_line=None, 
                              tries=1, try_sleep=0)

def _call_wrapper(command, **kwargs):
  tries = kwargs['tries']
  try_sleep = kwargs['try_sleep']
  timeout = kwargs['timeout']
  on_timeout = kwargs['on_timeout']
  throw_on_failure = kwargs['throw_on_failure']
  
  for i in range (0, tries):
    is_last_try = (i == tries-1)

    if not is_last_try:
      kwargs_copy = copy.copy(kwargs)
      kwargs_copy['throw_on_failure'] = True # we need to know when non-last try fails, to handle retries
    else:
      kwargs_copy = kwargs
    
    try:    
      try:
        result = _call(command, **kwargs_copy)
        break
      except ExecuteTimeoutException as ex:     
        if on_timeout:
          Logger.info("Executing '{0}'. Reason: {1}".format(on_timeout, str(ex)))
          result = checked_call(on_timeout)
        else:
          raise
    except Fail as ex:
      if is_last_try: # last try
        raise
      else:
        Logger.info("Retrying after {0} seconds. Reason: {1}".format(try_sleep, str(ex)))
        time.sleep(try_sleep)
      
  return result

def _call(command, logoutput=None, throw_on_failure=True, stdout=subprocess32.PIPE,stderr=subprocess32.STDOUT,
         cwd=None, env=None, preexec_fn=preexec_fn, user=None, wait_for_finish=True, timeout=None, on_timeout=None, 
         path=None, sudo=False, on_new_line=None, tries=1, try_sleep=0, timeout_kill_strategy=TerminateStrategy.TERMINATE_PARENT, returns=[0]):
  """
  Execute shell command
  
  @param command: list/tuple of arguments (recommended as more safe - don't need to escape) 
  or string of the command to execute
  @param logoutput: boolean, whether command output should be logged of not
  @param throw_on_failure: if true, when return code is not zero exception is thrown
  @param stdout,stderr: 
    subprocess32.PIPE - enable output to variable
    subprocess32.STDOUT - redirect to stdout
    None - disable output to variable, and output to Python out straightly (even if logoutput is False)
    {int fd} - redirect to file with descriptor.
    {string filename} - redirects to a file with name.
  """
  command_alias = Logger.format_command_for_output(command)
  command_alias = string_cmd_from_args_list(command_alias) if isinstance(command_alias, (list, tuple)) else command_alias
  
  # Append current PATH to env['PATH']
  env = _add_current_path_to_env(env)

  # Append path to env['PATH']
  if path:
    path = os.pathsep.join(path) if isinstance(path, (list, tuple)) else path
    env['PATH'] = os.pathsep.join([env['PATH'], path])

  if sudo and user:
    raise ValueError("Only one from sudo or user argument could be set to True")

  # prepare command cmd
  if sudo:
    command = as_sudo(command, env=env)
  elif user:
    command = as_user(command, user, env=env)
    
  # convert to string and escape
  if isinstance(command, (list, tuple)):
    command = string_cmd_from_args_list(command)
    
  # replace placeholder from as_sudo / as_user if present
  env_str = _get_environment_str(env)
  for placeholder, replacement in PLACEHOLDERS_TO_STR.iteritems():
    command = command.replace(placeholder, replacement.format(env_str=env_str))

  # --noprofile is used to preserve PATH set for ambari-agent
  subprocess32_command = ["/bin/bash","--login","--noprofile","-c", command]

  # don't create stdout and stderr pipes, because forked process will not be able to use them if current process dies
  # creating pipes may lead to the forked process silent crash
  if not wait_for_finish:
    stdout = None
    stderr = None

  files_to_close = []
  if isinstance(stdout, basestring):
    stdout = open(stdout, 'wb')
    files_to_close.append(stdout)
  if isinstance(stderr, basestring):
    stderr = open(stderr, 'wb')
    files_to_close.append(stderr)
  
  try:
    proc = subprocess32.Popen(subprocess32_command, stdout=stdout, stderr=stderr,
                            cwd=cwd, env=env, shell=False, close_fds=True,
                            preexec_fn=preexec_fn)
    
    if timeout:
      timeout_event = threading.Event()
      timer = threading.Timer(timeout, _on_timeout, [proc, timeout_event, timeout_kill_strategy])
      timer.start()
      
    if not wait_for_finish:
      return proc
      
    # in case logoutput == False, never log.
    logoutput = logoutput is True and Logger.logger.isEnabledFor(logging.INFO) or logoutput is None and Logger.logger.isEnabledFor(logging.DEBUG)
    read_set = []
    
    if stdout == subprocess32.PIPE:
      read_set.append(proc.stdout)
    if stderr == subprocess32.PIPE:
      read_set.append(proc.stderr)
    
    fd_to_string = {
      proc.stdout: "",
      proc.stderr: ""
    }
    all_output = ""
                  
    while read_set:

      is_proccess_running = proc.poll() is None
      ready, _, _ = select.select(read_set, [], [], 1)

      if not is_proccess_running and not ready:
        break

      for out_fd in read_set:
        if out_fd in ready:
          line = os.read(out_fd.fileno(), 1024)
          
          if not line:
            read_set = copy.copy(read_set)
            read_set.remove(out_fd)
            out_fd.close()
            continue
          
          fd_to_string[out_fd] += line
          all_output += line
            
          if on_new_line:
            try:
              on_new_line(line, out_fd == proc.stderr)
            except Exception:
              err_msg = "Caused by on_new_line function failed with exception for input argument '{0}':\n{1}".format(line, traceback.format_exc())
              raise Fail(err_msg)
            
          if logoutput:
            sys.stdout.write(line)
            sys.stdout.flush()

    # Wait for process to terminate
    if not timeout or not timeout_event.is_set():
      proc.wait()

  finally:
    for fp in files_to_close:
      fp.close()
      
  out = fd_to_string[proc.stdout].strip('\n')
  err = fd_to_string[proc.stderr].strip('\n')
  all_output = all_output.strip('\n')
  
  if timeout: 
    if not timeout_event.is_set():
      timer.cancel()
    # timeout occurred
    else:
      err_msg = "Execution of '{0}' was killed due timeout after {1} seconds".format(command, timeout)
      raise ExecuteTimeoutException(err_msg)
   
  code = proc.returncode
  
  if throw_on_failure and not code in returns:
    err_msg = Logger.filter_text("Execution of '{0}' returned {1}. {2}".format(command_alias, code, all_output))
    raise ExecutionFailed(err_msg, code, out, err)
  
  # if separate stderr is enabled (by default it's redirected to out)
  if stderr == subprocess32.PIPE:
    return code, out, err
  
  return code, out


def as_sudo(command, env=None, auto_escape=True):
  """
  command - list or tuple of arguments.
  env - when run as part of Execute resource, this SHOULD NOT be used.
  It automatically gets replaced later by call, checked_call. This should be used in not_if, only_if
  """
  if isinstance(command, (list, tuple)):
    command = string_cmd_from_args_list(command, auto_escape=auto_escape)
  else:
    # Since ambari user sudoer privileges may be restricted,
    # without having /bin/bash permission, and /bin/su permission.
    # Running interpreted shell commands in scope of 'sudo' is not possible.
    #   
    # In that case while passing string,
    # any bash symbols eventually added to command like && || ; < > | << >> would cause problems.
    err_msg = Logger.filter_text("String command '{0}' cannot be run as sudo. Please supply the command as a tuple of arguments".format(command))
    raise Fail(err_msg)

  env = _get_environment_str(_add_current_path_to_env(env)) if env else ENV_PLACEHOLDER
  return "{0} {1} -H -E {2}".format(_get_sudo_binary(), env, command)


def as_user(command, user, env=None, auto_escape=True):
  if isinstance(command, (list, tuple)):
    command = string_cmd_from_args_list(command, auto_escape=auto_escape)

  export_env = "export {0} ; ".format(_get_environment_str(_add_current_path_to_env(env))) if env else EXPORT_PLACEHOLDER
  return "{0} su {1} -l -s /bin/bash -c {2}".format(_get_sudo_binary(), user, quote_bash_args(export_env + command))


def quote_bash_args(command):
  if not command:
    return "''"
  
  if not isinstance(command, basestring):
    raise Fail("Command should be a list of strings, found '{0}' in command list elements".format(str(command)))
  
  valid = set(string.ascii_letters + string.digits + '@%_-+=:,./')
  for char in command:
    if char not in valid:
      return "'" + command.replace("'", "'\"'\"'") + "'"
  return command

def _add_current_path_to_env(env):
  result = {} if not env else env
  
  if not 'PATH' in result:
    result['PATH'] = os.environ['PATH']
    
  # don't append current env if already there
  if not set(os.environ['PATH'].split(os.pathsep)).issubset(result['PATH'].split(os.pathsep)):
    result['PATH'] = os.pathsep.join([os.environ['PATH'], result['PATH']])
  
  return result

def _get_sudo_binary():
  return AMBARI_SUDO_BINARY
  
def _get_environment_str(env):
  return reduce(lambda str,x: '{0} {1}={2}'.format(str,x,quote_bash_args(env[x])), env, '')

def string_cmd_from_args_list(command, auto_escape=True):
  if auto_escape:
    escape_func = lambda x:quote_bash_args(x)
    return ' '.join(escape_func(x) for x in command)
  else:
    return ' '.join(command)

def _on_timeout(proc, timeout_event, terminate_strategy):
  timeout_event.set()
  terminate_process(proc, terminate_strategy)