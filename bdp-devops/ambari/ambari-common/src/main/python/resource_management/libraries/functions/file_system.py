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
__all__ = ["get_and_cache_mount_points", "get_mount_point_for_dir"]
import os
from resource_management.core.logger import Logger
from resource_management.core.providers import mount

# Global variable
mounts = None


def get_and_cache_mount_points(refresh=False):
  """
  :param refresh: Boolean flag indicating whether to refresh the "mounts" variable if already cached.
  :return: Returns the "mounts" variable. Calculates and caches it the first time if it is None or the "refresh" param
  is set to True.
  """
  global mounts

  if mounts is not None and not refresh:
    return mounts
  else:
    mounts = mount.get_mounted()
    for m in mounts:
      if m["mount_point"] is not None:
        m["mount_point"] = m["mount_point"].rstrip()
    Logger.info("Host contains mounts: %s." % str([m["mount_point"] for m in mounts]))
    return mounts


def get_mount_point_for_dir(dir, mount_points = None):
  """
  :param dir: Directory to check, even if it doesn't exist.
  :return: Returns the closest mount point as a string for the directory. if the "dir" variable is None, will return None.
  If the directory does not exist, will return "/".
  """
  best_mount_found = None
  if dir:
    dir = dir.strip()

    cached_mounts = [m['mount_point'] for m in get_and_cache_mount_points()] if mount_points is None else mount_points

    # If the path is "/hadoop/hdfs/data", then possible matches for mounts could be
    # "/", "/hadoop/hdfs", and "/hadoop/hdfs/data".
    # So take the one with the greatest number of segments.
    for m in cached_mounts:
      # Ensure that the mount path and the dir path ends with "/"
      # The mount point "/hadoop" should not match the path "/hadoop1"
      if os.path.join(dir, "").startswith(os.path.join(m, "")):
        if best_mount_found is None:
          best_mount_found = m
        elif os.path.join(best_mount_found, "").count(os.path.sep) < os.path.join(m, "").count(os.path.sep):
          best_mount_found = m

  Logger.info("Mount point for directory %s is %s" % (str(dir), str(best_mount_found)))
  return best_mount_found
