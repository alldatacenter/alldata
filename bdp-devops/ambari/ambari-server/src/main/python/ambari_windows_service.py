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
import optparse

import os
import sys

from ambari_commons.ambari_service import AmbariService
from ambari_commons.logging_utils import set_silent, set_verbose, print_info_msg
from ambari_commons.os_utils import remove_file
from ambari_commons.os_windows import SvcStatusCallback
from ambari_server.serverConfiguration import configDefaults, get_ambari_properties, get_value_from_properties, \
  DEBUG_MODE_KEY, PID_NAME, SERVER_OUT_FILE_KEY, SUSPEND_START_MODE_KEY, VERBOSE_OUTPUT_KEY


class AmbariServerService(AmbariService):
  AmbariService._svc_name_ = "Ambari Server"
  AmbariService._svc_display_name_ = "Ambari Server"
  AmbariService._svc_description_ = "Ambari Server"

  # Adds the necessary script dir to the Python's modules path
  def _adjustPythonPath(self, current_dir):
    python_path = os.path.join(current_dir, "sbin")
    sys.path.insert(0, python_path)
    print_info_msg("sys.path=" + str(sys.path))

  def SvcDoRun(self):
    from ambari_server_main import server_process_main

    scmStatus = SvcStatusCallback(self)

    properties = get_ambari_properties()
    self.options.verbose = get_value_from_properties(properties, VERBOSE_OUTPUT_KEY, False)
    self.options.debug = get_value_from_properties(properties, DEBUG_MODE_KEY, False)
    self.options.suspend_start = get_value_from_properties(properties, SUSPEND_START_MODE_KEY, False)

    # set verbose
    set_verbose(self.options.verbose)

    self.redirect_output_streams()

    childProc = server_process_main(self.options, scmStatus)

    if not self._StopOrWaitForChildProcessToFinish(childProc):
      return

    pid_file_path = os.path.join(configDefaults.PID_DIR, PID_NAME)
    remove_file(pid_file_path)
    pass

  def _InitOptionsParser(self):
    # No command-line options needed when starting as a service
    return optparse.OptionParser()

  def redirect_output_streams(self):
    properties = get_ambari_properties()

    outFilePath = properties[SERVER_OUT_FILE_KEY]
    if (outFilePath is None or outFilePath == ""):
      outFilePath = configDefaults.SERVER_OUT_FILE

    self._RedirectOutputStreamsToFile(outFilePath)
    pass

def ctrlHandler(ctrlType):
  AmbariServerService.DefCtrlCHandler()
  return True

def svcsetup(register_service, username=None, password=None):
  AmbariServerService.set_ctrl_c_handler(ctrlHandler)

  scriptFile, ext = os.path.splitext(__file__.replace('/', os.sep))
  classPath = scriptFile + "." + AmbariServerService.__name__

  # We don't save the password between 'setup' runs, so we can't run Install every time. We run Install only if the
  # operator acknowledged changing the username or if the service is not yet installed
  if (register_service or AmbariServerService.QueryStatus() == "not installed"):
    return AmbariServerService.Install(classPath=classPath, username=username, password=password)
  else:
    return 0
