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
import re

from resource_management.libraries.functions.decorator import retry
from resource_management.core import shell
from resource_management.core.exceptions import Fail


@retry(times=10, sleep_time=2)
def call_and_match_output(command, regex_expression, err_message, **call_kwargs):
  """
  Call the command and performs a regex match on the output for the specified expression.
  :param command: Command to call
  :param regex_expression: Regex expression to search in the output
  """
  code, out = shell.call(command, logoutput=True, quiet=False, **call_kwargs)
  if not (out and re.search(regex_expression, out, re.IGNORECASE)):
    raise Fail(err_message)