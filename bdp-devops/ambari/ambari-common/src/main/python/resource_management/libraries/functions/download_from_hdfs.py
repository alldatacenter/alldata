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

__all__ = ["download_from_hdfs", ]

import os
import uuid
import tempfile
import re

from resource_management.libraries.script.script import Script
from resource_management.libraries.resources.hdfs_resource import HdfsResource
from resource_management.libraries.functions.default import default
from resource_management.core import shell
from resource_management.core.logger import Logger


def download_from_hdfs(source_file, dest_path, user_group, owner, download_type="file", file_mode=0444, force_execute=False,
                       replace_existing_files=False):
  """
  :param source_file: the source file path
  :param dest_path: the destination path
  :param user_group: Group to own the directory.
  :param owner: File owner
  :param download_type: file or directory
  :param file_mode: File permission
  :param force_execute: If true, will execute the HDFS commands immediately, otherwise, will defer to the calling function.
  :param replace_existing_files: If true, will replace existing files even if they are the same size
  :return: Will return True if successful, otherwise, False.
  """
  import params

  Logger.info("Called download_from_hdfs source in HDFS: {0} , local destination path: {1}".format(source_file, dest_path))

  # The destination directory must already exist
  if not os.path.exists(dest_path):
    Logger.error("Cannot copy {0} because destination directory {1} does not exist.".format(source_file, dest_path))
    return False

  filename = os.path.basename(source_file)
  dest_file = os.path.join(dest_path, filename)

  params.HdfsResource(dest_file,
                      type=download_type,
                      action="download_on_execute",
                      source=source_file,
                      group=user_group,
                      owner=owner,
                      mode=file_mode,
                      replace_existing_files=replace_existing_files,
  )

  Logger.info("Will attempt to copy from DFS at {0} to local file system {1}.".format(source_file, dest_file))

  # For improved performance, force_execute should be False so that it is delayed and combined with other calls.
  if force_execute:
    params.HdfsResource(None, action="execute")

  return True
