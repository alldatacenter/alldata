#!/usr/bin/env python

'''
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
'''

import os
import re
import fileinput

from ambari_commons.exceptions import FatalException
from ambari_commons.logging_utils import print_info_msg, print_warning_msg, print_error_msg, get_verbose
from ambari_commons.os_utils import is_root
from ambari_server.serverConfiguration import get_ambari_properties, get_stack_location
from ambari_server.serverUtils import is_server_runing

#
# Stack enable/disable
#

def enable_stack_version(stack_name, stack_versions):
  if not is_root():
    err = 'Ambari-server enable-stack should be run with ' \
          'root-level privileges'
    raise FatalException(4, err)

  try:
    print_info_msg("stack name requested: " + str(stack_name))
    print_info_msg("stack version requested: " + str(stack_versions))
  except IndexError:
    raise FatalException("Invalid stack version passed")

  retcode = update_stack_metainfo(stack_name,stack_versions)

  if not retcode == 0:
    raise FatalException(retcode, 'Stack enable request failed.')

  return retcode

def update_stack_metainfo(stack_name, stack_versions):
  properties = get_ambari_properties()
  if properties == -1:
    print_error_msg("Error getting ambari properties")
    return -1

  stack_location = get_stack_location(properties)
  print_info_msg ("stack location: "+ stack_location)

  stack_root = os.path.join(stack_location, stack_name)
  print_info_msg ("stack root: "+ stack_root)
  if not os.path.exists(stack_root):
    print_error_msg("stack directory does not exists: " + stack_root)
    return -1

  for stack in stack_versions:
    if stack  not in os.listdir(stack_root):
      print_error_msg ("The requested stack version: " + stack + " is not available in the HDP stack")
      return -1

  for directory in os.listdir(stack_root):
    print_info_msg("directory found: " + directory)
    metainfo_file = os.path.join(stack_root, directory, "metainfo.xml")
    print_info_msg("looking for metainfo file: " + metainfo_file)
    if not os.path.exists(metainfo_file):
      print_error_msg("Could not find metainfo file in the path " + metainfo_file)
      continue
    if directory in stack_versions:
       print_info_msg ("updating stack to active for: " + directory )
       replace(metainfo_file,"<active>false</active>","<active>true</active>")
    else:
       print_info_msg ("updating stack to inactive for: " + directory )
       replace(metainfo_file,"<active>true</active>","<active>false</active>")
  return 0

def replace(file_path, pattern, subst):
   for line in fileinput.input(file_path, inplace=1):
      line = re.sub(pattern,subst, line.rstrip())
      print(line)


