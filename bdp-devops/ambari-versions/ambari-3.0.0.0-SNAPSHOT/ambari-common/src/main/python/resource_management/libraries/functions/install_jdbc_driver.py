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

import os

from ambari_commons.inet_utils import download_file
from ambari_commons.os_utils import copy_file, search_file
from resource_management.core.logger import Logger


__all__ = ["ensure_jdbc_driver_is_in_classpath"]


def ensure_jdbc_driver_is_in_classpath(dest_dir, cache_location, driver_url, driver_files):
  #Attempt to find the JDBC driver installed locally
  #If not, attempt to download it from the server resources URL
  for driver_file in driver_files:
    dest_path = os.path.join(dest_dir, driver_file)
    Logger.info("JDBC driver file(s) {0}: Attempting to copy from {1} or download from {2} to {3}".format(
      str(driver_files), cache_location, driver_url, dest_dir))
    if not os.path.exists(dest_path):
      search_path = os.environ["PATH"]
      if cache_location:
        search_path += os.pathsep + cache_location  #The locally installed version takes precedence over the cache

      local_path = search_file(driver_file, search_path)
      if not local_path:
        download_file(driver_url + "/" + driver_file, dest_path)
      else:
        copy_file(local_path, dest_path)
