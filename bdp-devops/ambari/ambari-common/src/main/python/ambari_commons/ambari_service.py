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

import win32service

from ambari_commons.os_windows import WinService


AMBARI_VERSION_VAR = "AMBARI_VERSION_VAR"

ENV_PYTHON_PATH = "PYTHONPATH"


class AmbariService(WinService):
  _svc_name_ = "Ambari Service"
  _svc_display_name_ = "Ambari Service"
  _svc_description_ = "Ambari Service"

  def _adjustPythonPath(self, current_dir):
    pass

  # Sets the current dir and adjusts the PYTHONPATH env variable before calling SvcDoRun()
  def SvcRun(self):
    self.ReportServiceStatus(win32service.SERVICE_START_PENDING)

    import servicemanager

    parser = self._InitOptionsParser()
    (self.options, args) = parser.parse_args()

    try:
      is_debugging = servicemanager.Debugging()
    except:
      is_debugging = False

    if not is_debugging:
      # Save the current dir, or the script dir if none set (typical for services)
      script_path = os.path.dirname(__file__.replace('/', os.sep))
      # the script resides in the sbin/ambari_commons subdir
      self.options.current_dir = os.path.normpath(script_path + "\\..\\..")
      os.chdir(self.options.current_dir)
    else:
      self.options.current_dir = os.getcwd()

    self._adjustPythonPath(self.options.current_dir)

    self.SvcDoRun()
    pass

  # Override to customize the command-line arguments
  def _InitOptionsParser(self):
    pass
