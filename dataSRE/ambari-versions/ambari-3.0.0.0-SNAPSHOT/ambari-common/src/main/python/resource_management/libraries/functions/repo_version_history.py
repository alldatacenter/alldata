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
from resource_management.core.logger import Logger

"""
Repository version file is used while executing install_packages.py
That allows us to get actual installed version even during reinstalling existing
 repository version (when <stack-selector-tool> output does not change before and after
 installing packages).

Format:
2.3.0.0,2.3.0.0-5678
2.2.0.0,2.2.0.0-1699

First value in each string should be normalized (contain no build number)
"""


# Mapping file used to store repository versions without a build number, and the actual version it corresponded to.
# E.g., HDP 2.2.0.0 => HDP 2.2.0.0-2041
REPO_VERSION_HISTORY_FILE = "/var/lib/ambari-agent/data/repo_version_history.csv"


def read_actual_version_from_history_file(repository_version):
  """
  :param repository_version: normalized repo version (without build number) as received from the server
  Search the repository version history file for a line that contains repository_version,actual_version
  Notice that the parts are delimited by a comma.
  :return: Return the actual_version if found, otherwise, return None.
  """
  actual_version = None
  if os.path.isfile(REPO_VERSION_HISTORY_FILE):
    with open(REPO_VERSION_HISTORY_FILE, "r") as f:
      for line in reversed(f.readlines()):
        line_parts = line.split(",")
        if line_parts and len(line_parts) == 2 and line_parts[0] == repository_version:
          item = line_parts[1].strip()
          if item != "":
            actual_version = item
            break
  return actual_version


def write_actual_version_to_history_file(repository_version, actual_version):
  """
  Save the tuple of repository_version,actual_version to the repo version history file if the repository_version
  doesn't already exist
  :param repository_version: normalized repo version (without build number) as received from the server
  :param actual_version: Repo version with the build number, as determined using <stack-selector-tool>
  :returns Return True if appended the values to the file, otherwise, return False.
  """
  wrote_value = False
  if repository_version is None or actual_version is None:
    return

  if repository_version == "" or actual_version == "":
    return

  value = repository_version + "," + actual_version
  key_exists = False
  try:
    if os.path.isfile(REPO_VERSION_HISTORY_FILE):
      with open(REPO_VERSION_HISTORY_FILE, "r") as f:
        for line in f.readlines():
          line_parts = line.split(",")
          if line_parts and len(line_parts) == 2 and line_parts[0] == repository_version and line_parts[1] == actual_version:
            key_exists = True
            break

    if not key_exists:
      with open(REPO_VERSION_HISTORY_FILE, "a") as f:
        f.write(repository_version + "," + actual_version + "\n")
        wrote_value = True
    if wrote_value:
      Logger.info("Appended value \"{0}\" to file {1} to track this as a new version.".format(value, REPO_VERSION_HISTORY_FILE))
  except Exception, err:
    Logger.error("Failed to write to file {0} the value: {1}. Error: {2}".format(REPO_VERSION_HISTORY_FILE, value, str(err)))

  return wrote_value