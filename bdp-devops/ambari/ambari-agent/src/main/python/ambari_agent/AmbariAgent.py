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

import os
import sys
from ambari_commons import subprocess32

AGENT_AUTO_RESTART_EXIT_CODE = 77


def get_logger():
  import logging

  _logger = logging.getLogger(__name__)
  _logger.handlers = []

  # pattern used at ambari-agent#start to output messages from script. DO NOT change it without changing grep regex
  formatter = logging.Formatter("%(levelname)s: %(message)s")
  handler = logging.StreamHandler()
  handler.setLevel(logging.WARNING)
  handler.setFormatter(formatter)

  _logger.addHandler(handler)

  return _logger


logger = get_logger()


if "PYTHON_BIN" in os.environ:
  AGENT_SCRIPT = os.path.join(os.environ["PYTHON_BIN"], "site-packages/ambari_agent/main.py")
else:
  AGENT_SCRIPT = "/usr/lib/ambari-agent/lib/ambari_agent/main.py"

if "AMBARI_PID_DIR" in os.environ:
  AGENT_PID_FILE = os.path.join(os.environ["AMBARI_PID_DIR"], "ambari-agent.pid")
else:
  AGENT_PID_FILE = "/var/run/ambari-agent/ambari-agent.pid"

# AGENT_AUTO_RESTART_EXIT_CODE = 77 is exit code which we return when restart_agent() is called
status = AGENT_AUTO_RESTART_EXIT_CODE


def check_native_libs_support():
  not_loaded_extensions = []

  from ambari_simplejson import c_extension
  if not c_extension.is_loaded():
    not_loaded_extensions.append("simplejson")

  if subprocess32._posixsubprocess is None:
    not_loaded_extensions.append("subprocess32")

  if not_loaded_extensions:
    logger.warning("Some native extensions not available for module(s): {}, it may affect execution performance".format(",".join(not_loaded_extensions)))


def main():
  global status

  if "PYTHON" in os.environ:
    python = os.environ["PYTHON"]
  else:
    print("Key 'PYTHON' is not defined in environment variables")
    sys.exit(1)

  args = list(sys.argv)
  del args[0]

  merged_args = [python, AGENT_SCRIPT] + args

  while status == AGENT_AUTO_RESTART_EXIT_CODE:
    check_native_libs_support()
    main_process = subprocess32.Popen(merged_args)
    main_process.communicate()
    status = main_process.returncode
    if os.path.isfile(AGENT_PID_FILE) and status == AGENT_AUTO_RESTART_EXIT_CODE:
      os.remove(AGENT_PID_FILE)


if __name__ == "__main__":
    main()
