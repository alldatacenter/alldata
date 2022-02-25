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

import logging
import os
import pprint
import threading
import sys

import ambari_simplejson as json

from ambari_commons import subprocess32
from ambari_commons import shell

from Grep import Grep
from BackgroundCommandExecutionHandle import BackgroundCommandExecutionHandle
from resource_management.libraries.functions.log_process_information import log_process_information


class PythonExecutor(object):
  """
  Performs functionality for executing python scripts.
  Warning: class maintains internal state. As a result, instances should not be
  used as a singleton for a concurrent execution of python scripts
  """
  NO_ERROR = "none"

  def __init__(self, tmp_dir, config):
    self.logger = logging.getLogger()
    self.grep = Grep()
    self.event = threading.Event()
    self.python_process_has_been_killed = False
    self.tmpDir = tmp_dir
    self.config = config
    self.log_max_symbols_size = self.config.log_max_symbols_size

  def open_subprocess32_files(self, tmp_out_file, tmp_err_file, override_output_files, backup_log_files=True):
    mode = "w" if override_output_files else "a"

    if override_output_files and backup_log_files:
      self.back_up_log_file_if_exists(tmp_out_file)
      self.back_up_log_file_if_exists(tmp_err_file)

    return open(tmp_out_file, mode), open(tmp_err_file, mode)

  def back_up_log_file_if_exists(self, file_path):
    if os.path.isfile(file_path):
      counter = 0
      while True:
        # Find backup name that is not used yet (saves logs from multiple command retries)
        backup_name = file_path + "." + str(counter)
        if not os.path.isfile(backup_name):
          break
        counter += 1
      os.rename(file_path, backup_name)

  def run_file(self, script, script_params, tmp_out_file, tmp_err_file,
               timeout, tmp_structed_outfile, callback, task_id,
               override_output_files=True, backup_log_files=True, handle=None,
               log_info_on_failure=True):
    """
    Executes the specified python file in a separate subprocess32.
    Method returns only when the subprocess32 is finished.
    Params arg is a list of script parameters
    Timeout meaning: how many seconds should pass before script execution
    is forcibly terminated
    override_output_files option defines whether stdout/stderr files will be
    recreated or appended.
    The structured out file, however, is preserved during multiple invocations that use the same file.
    """
    python_command = self.python_command(script, script_params)
    if self.logger.isEnabledFor(logging.DEBUG):
      self.logger.debug("Running command %s", pprint.pformat(python_command))

    def background_executor():
      logger = logging.getLogger()
      process_out, process_err = self.open_subprocess32_files(tmp_out_file, tmp_err_file, True)

      logger.debug("Starting process command %s", python_command)
      p = self.launch_python_subprocess32(python_command, process_out, process_err)

      logger.debug("Process has been started. Pid = %s", p.pid)

      handle.pid = p.pid
      handle.status = BackgroundCommandExecutionHandle.RUNNING_STATUS
      handle.on_background_command_started(handle.command['taskId'], p.pid)

      p.communicate()

      handle.exitCode = p.returncode
      process_condensed_result = self.prepare_process_result(p.returncode, tmp_out_file, tmp_err_file, tmp_structed_outfile)
      logger.debug("Calling callback with args %s", process_condensed_result)
      handle.on_background_command_complete_callback(process_condensed_result, handle)
      logger.debug("Exiting from thread for holder pid %s", handle.pid)

    if handle is None:
      tmpout, tmperr = self.open_subprocess32_files(tmp_out_file, tmp_err_file, override_output_files, backup_log_files)

      process = self.launch_python_subprocess32(python_command, tmpout, tmperr)
      # map task_id to pid
      callback(task_id, process.pid)
      self.logger.debug("Launching watchdog thread")
      self.event.clear()
      self.python_process_has_been_killed = False
      thread = threading.Thread(target=self.python_watchdog_func, args=(process, timeout))
      thread.start()
      # Waiting for the process to be either finished or killed
      process.communicate()
      self.event.set()
      thread.join()
      result = self.prepare_process_result(process.returncode, tmp_out_file, tmp_err_file, tmp_structed_outfile, timeout=timeout)

      if log_info_on_failure and result['exitcode']:
        self.on_failure(python_command, result)

      return result
    else:
      background = threading.Thread(target=background_executor, args=())
      background.start()
      return {"exitcode": 777}

  def on_failure(self, python_command, result):
    """
    Log some useful information after task failure.
    """
    self.logger.info("Command %s failed with exitcode=%s", pprint.pformat(python_command), result['exitcode'])
    log_process_information(self.logger)

  def prepare_process_result(self, returncode, tmpoutfile, tmperrfile, tmpstructedoutfile, timeout=None):
    out, error, structured_out = self.read_result_from_files(tmpoutfile, tmperrfile, tmpstructedoutfile)

    if self.python_process_has_been_killed:
      error = "{error}\nPython script has been killed due to timeout{timeout_details}".format(
        error=error,
        timeout_details="" if not timeout else " after waiting {} secs".format(timeout)
      )
      returncode = 999
    result = self.condense_output(out, error, returncode, structured_out)
    self.logger.debug("Result: %s", result)
    return result

  def read_result_from_files(self, out_path, err_path, structured_out_path):
    out = open(out_path, 'r').read()
    error = open(err_path, 'r').read()
    try:
      with open(structured_out_path, 'r') as fp:
        structured_out = json.load(fp)
    except (TypeError, ValueError):
      structured_out = {
        "msg": "Unable to read structured output from " + structured_out_path
      }
      self.logger.warn(structured_out)
    except (OSError, IOError):
      structured_out = {}
    return out, error, structured_out

  def launch_python_subprocess32(self, command, tmpout, tmperr):
    """
    Creates subprocess32 with given parameters. This functionality was moved to separate method
    to make possible unit testing
    """
    command_env = dict(os.environ)
    return subprocess32.Popen(command, stdout=tmpout, stderr=tmperr, close_fds=True, env=command_env,
                              preexec_fn=lambda: os.setpgid(0, 0))

  def is_successful(self, return_code):
    return not self.python_process_has_been_killed and return_code == 0

  def python_command(self, script, script_params):
    """
    :type script str
    :type script_params list|set
    """
    python_command = [sys.executable, script] + script_params
    return python_command

  def condense_output(self, stdout, stderr, ret_code, structured_out):
    return {
      "exitcode": ret_code,
      "stdout": self.grep.tail_by_symbols(stdout, self.log_max_symbols_size) if self.log_max_symbols_size else stdout,
      "stderr": self.grep.tail_by_symbols(stderr, self.log_max_symbols_size) if self.log_max_symbols_size else stderr,
      "structuredOut": structured_out
    }

  def python_watchdog_func(self, process, timeout):
    self.event.wait(timeout)
    if process.returncode is None:
      self.logger.error("Executed command with pid {} timed out and will be killed".format(process.pid))
      shell.kill_process_with_children(process.pid)
      self.python_process_has_been_killed = True
