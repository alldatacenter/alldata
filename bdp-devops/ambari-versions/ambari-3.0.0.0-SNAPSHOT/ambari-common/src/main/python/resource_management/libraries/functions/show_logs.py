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
__all__ = ["show_logs"]

from resource_management.core.resources.system import Execute
from resource_management.libraries.functions.format import format

LAST_LINES_DEFAULT_OUTPUT_COUNT = 40

def show_logs(log_dir, user, lines_count=LAST_LINES_DEFAULT_OUTPUT_COUNT, mask="*"):
  """
  This should be used in 'except' block of start or stop Execute of services or of command which checks if start was fine.
  It allows to give additional debugging information without need to have access to log files.
  
  Don't forget to call "raise" after using the function or else the original exception will be masked.
  """
  
  Execute(format("find {log_dir} -maxdepth 1 -type f -name '{mask}' -exec echo '==> {{}} <==' \; -exec tail -n {lines_count} {{}} \;"),
          logoutput = True,
          ignore_failures = True, # if this fails should not overwrite the actual exception
          user = user, # need to have permissions to read log files
  )