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

import re
import os
import shutil
import string
import resource
from ambari_commons import OSCheck
from string import Template

if OSCheck.is_windows_family():
  pass
else:
  import pwd

if OSCheck.is_windows_family():
  from ambari_commons.os_windows import os_change_owner, os_getpass, os_is_root, os_run_os_command, \
    os_set_open_files_limit, os_set_file_permissions
else:
  # MacOS not supported
  from ambari_commons.os_linux import os_change_owner, os_getpass, os_is_root, os_run_os_command, \
    os_set_open_files_limit, os_set_file_permissions
  pass

from ambari_commons.exceptions import FatalException
from ambari_commons.logging_utils import print_info_msg, print_warning_msg

def current_user():
  if OSCheck.is_windows_family():
    return None
  else:
    return pwd.getpwuid(os.geteuid())[0]

def get_used_ram():
  """
  Returns resident RAM used by current process in kilobytes
  """
  return resource.getrusage(resource.RUSAGE_SELF).ru_maxrss

def is_valid_filepath(filepath):
  if not filepath or not os.path.exists(filepath) or os.path.isdir(filepath):
    print 'Invalid path, please provide the absolute file path.'
    return False
  else:
    return True

def quote_path(filepath):
  if(filepath.find(' ') != -1):
    filepath_ret = '"' + filepath + '"'
  else:
    filepath_ret = filepath
  return filepath_ret

def trim_uri(file_uri):
  if file_uri.startswith("file:///"):
    return file_uri[8:].replace("/", os.sep)
  return file_uri

def _search_file(filename, search_path, pathsep):
  for path in string.split(search_path, pathsep):
    candidate = os.path.join(path, filename)
    if os.path.exists(candidate):
      return os.path.abspath(candidate)
  return None

def search_file(filename, search_path, pathsep=os.pathsep):
  """ Given a search path, find file with requested name """
  return _search_file(filename, search_path, pathsep)

def copy_file(src, dest_file):
  try:
    shutil.copyfile(src, dest_file)
  except Exception, e:
    err = "Can not copy file {0} to {1} due to: {2} . Please check file " \
              "permissions and free disk space.".format(src, dest_file, e.message)
    raise FatalException(1, err)

def copy_files(files, dest_dir):
  if os.path.isdir(dest_dir):
    for filepath in files:
      shutil.copy(filepath, dest_dir)
    return 0
  else:
    return -1

def remove_file(filePath):
  if os.path.exists(filePath):
    try:
      os.remove(filePath)
    except Exception, e:
      print_warning_msg('Unable to remove file: ' + str(e))
      return 1
  pass
  return 0

def set_file_permissions(file, mod, user, recursive):
  if os.path.exists(file):
    os_set_file_permissions(file, mod, recursive, user)
  else:
    print_info_msg("File %s does not exist" % file)

def run_os_command(cmd, env=None, cwd=None):
  return os_run_os_command(cmd, env, False, cwd)

def run_in_shell(cmd, env=None, cwd=None):
  return os_run_os_command(cmd, env, True, cwd)

def is_root():
  return os_is_root()

# Proxy to the os implementation
def change_owner(filePath, user, recursive):
  os_change_owner(filePath, user, recursive)

# Proxy to the os implementation
def set_open_files_limit(maxOpenFiles):
  os_set_open_files_limit(maxOpenFiles)

def get_password(prompt):
  return os_getpass(prompt)

def find_in_path(file):
  full_path = _search_file(file, os.environ["PATH"], os.pathsep)
  if full_path is None:
    raise Exception("File {0} not found in PATH".format(file))
  return full_path

def extract_path_component(path, path_fragment):
  iFragment = path.find(path_fragment)
  if iFragment != -1:
    iComponentStart = 0
    while iComponentStart < iFragment:
      iComponentStartTemp = path.find(os.pathsep, iComponentStart)
      if iComponentStartTemp == -1 or iComponentStartTemp > iFragment:
        break
      iComponentStart = iComponentStartTemp

    iComponentEnd = path.find(os.pathsep, iFragment)
    if iComponentEnd == -1:
      iComponentEnd = len(path)

    path_component = path[iComponentStart:iComponentEnd]
    return path_component
  else:
    return None

# Gets the full path of the ambari repo file for the current OS
def get_ambari_repo_file_full_name():
  if OSCheck.is_ubuntu_family():
    ambari_repo_file = "/etc/apt/sources.list.d/ambari.list"
  elif OSCheck.is_redhat_family():
    ambari_repo_file = "/etc/yum.repos.d/ambari.repo"
  elif OSCheck.is_suse_family():
    ambari_repo_file = "/etc/zypp/repos.d/ambari.repo"
  elif OSCheck.is_windows_family():
    ambari_repo_file = os.path.join(os.environ[ChocolateyConsts.CHOCOLATEY_INSTALL_VAR_NAME],
                                    ChocolateyConsts.CHOCOLATEY_CONFIG_DIR, ChocolateyConsts.CHOCOLATEY_CONFIG_FILENAME)
  else:
    raise Exception('Ambari repo file path not set for current OS.')

  return ambari_repo_file

# Gets the owner of the specified file
def get_file_owner(file_full_name):
  if OSCheck.is_windows_family():
    return ""
  else:
      return pwd.getpwuid(os.stat(file_full_name).st_uid).pw_name
    
def parse_log4j_file(filename):
  def translate_praceholders(fmt):
    # escape their markers
    fmt = fmt.replace('%', '%%')
    
    fmt = re.sub(r'\${(.+?)}', r'%(\1)s', fmt)
    
    return fmt
  
  properties = {}
  
  Template.idpattern = r'[_a-z][_a-z0-9\.]*'
  with open(filename, "rb") as fp:
    lines = fp.readlines()
    
  for line in lines:
    line = line.strip()
    
    if not line or line.startswith("#"):
      continue
    
    # should we raise exception here?
    if not "=" in line:
      continue
    
    splited_values = line.split("=")
    properties[splited_values[0].strip()] = translate_praceholders("=".join(splited_values[1:]).strip()) % properties
    
  return properties

#
# Chololatey package manager constants for Windows
#
class ChocolateyConsts:
  CHOCOLATEY_INSTALL_VAR_NAME = "ChocolateyInstall"
  CHOCOLATEY_CONFIG_DIR = "config"
  CHOCOLATEY_CONFIG_FILENAME = "chocolatey.config"