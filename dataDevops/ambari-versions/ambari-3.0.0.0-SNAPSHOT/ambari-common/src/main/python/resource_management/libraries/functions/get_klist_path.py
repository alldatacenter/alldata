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

__all__ = ["get_klist_path"]
from find_executable import find_executable


def get_klist_path(search_directories=None):
  """
  Searches for the klist executable using the specified search paths or a default set of of paths to search

  @param search_directories: comma separated list or a list of (absolute paths to) directories to search (in order of preference)
  :return: the path to the found klist executable
  """
  return find_executable(search_directories, "klist")
