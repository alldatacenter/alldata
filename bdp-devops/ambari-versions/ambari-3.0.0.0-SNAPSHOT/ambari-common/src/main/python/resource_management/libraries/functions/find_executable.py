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

__all__ = ["find_executable"]
from find_path import find_path


def find_executable(search_directories, filename):
  """
  Searches for the specified executable using a list of specified search paths or, if None, a default
  set of paths:
    /usr/bin
    /usr/kerberos/bin
    /usr/sbin
    /usr/lib/mit/bin
    /usr/lib/mit/sbin

  @param search_directories: comma separated list or a list of (absolute paths to) directories to search (in order of preference)
  @param filename: the name of the file for which to search
  @return: the absolute path to the specified executable; or, if not found just the specified executable name
  """
  if isinstance(search_directories, unicode):
    search_directories = map(str.strip, search_directories.encode("ascii").split(","))
  elif isinstance(search_directories, str):
    search_directories = map(str.strip, search_directories.split(","))
  elif not isinstance(search_directories, list):
    search_directories = ["/usr/bin", "/usr/kerberos/bin", "/usr/sbin", '/usr/lib/mit/bin',
                          '/usr/lib/mit/sbin']

  path = find_path(search_directories, filename)
  return path if path else filename
