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

from resource_management import *
import os
import glob

def install_tez_jars():
  import params

  destination_hdfs_dirs = get_tez_hdfs_dir_paths(params.tez_lib_uris)

  # If tez libraries are to be stored in hdfs
  if destination_hdfs_dirs:
    for hdfs_dir in destination_hdfs_dirs:
      params.HdfsResource(hdfs_dir,
                           type="directory",
                           action="create_on_execute",
                           owner=params.tez_user,
                           mode=0755
      )

    app_dir_path = None
    lib_dir_path = None

    if len(destination_hdfs_dirs) > 0:
      for path in destination_hdfs_dirs:
        if 'lib' in path:
          lib_dir_path = path
        else:
          app_dir_path = path
        pass
      pass
    pass

    tez_jars = {}
    if app_dir_path:
      tez_jars[params.tez_local_api_jars] = app_dir_path
    if lib_dir_path:
      tez_jars[params.tez_local_lib_jars] = lib_dir_path

    for src_file_regex, dest_dir in tez_jars.iteritems():
      for src_filepath in glob.glob(src_file_regex):
        src_filename = os.path.basename(src_filepath)
        params.HdfsResource(format("{dest_dir}/{src_filename}"),
                            type="file",
                            action="create_on_execute",
                            source=src_filepath,
                            mode=0755,
                            owner=params.tez_user
         )
        
    for src_file_regex, dest_dir in tez_jars.iteritems():
      for src_filepath in glob.glob(src_file_regex):
        src_filename = os.path.basename(src_filepath)
        params.HdfsResource(format("{dest_dir}/{src_filename}"),
                            type="file",
                            action="create_on_execute",
                            source=src_filepath,
                            mode=0755,
                            owner=params.tez_user
         )
    params.HdfsResource(None, action="execute")


def get_tez_hdfs_dir_paths(tez_lib_uris = None):
  hdfs_path_prefix = 'hdfs://'
  lib_dir_paths = []
  if tez_lib_uris and tez_lib_uris.strip().find(hdfs_path_prefix, 0) != -1:
    dir_paths = tez_lib_uris.split(',')
    for path in dir_paths:
      if not "tez.tar.gz" in path:
        lib_dir_path = path.replace(hdfs_path_prefix, '')
        lib_dir_path = lib_dir_path if lib_dir_path.endswith(os.sep) else lib_dir_path + os.sep
        lib_dir_paths.append(lib_dir_path)
      else:
        lib_dir_path = path.replace(hdfs_path_prefix, '')
        lib_dir_paths.append(os.path.dirname(lib_dir_path))
    pass
  pass

  return lib_dir_paths
