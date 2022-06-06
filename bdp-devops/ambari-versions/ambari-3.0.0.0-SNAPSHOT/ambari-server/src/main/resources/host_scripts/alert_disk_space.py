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

import collections
import os
import platform
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl
from ambari_commons import OSConst
from resource_management.libraries.functions import stack_tools

DiskInfo = collections.namedtuple('DiskInfo', 'total used free path')

# script parameter keys
MIN_FREE_SPACE_KEY = "minimum.free.space"
PERCENT_USED_WARNING_KEY = "percent.used.space.warning.threshold"
PERCENT_USED_CRITICAL_KEY = "percent.free.space.critical.threshold"

# defaults in case no script parameters are passed
MIN_FREE_SPACE_DEFAULT = 5000000000L
PERCENT_USED_WARNING_DEFAULT = 50
PERCENT_USED_CRITICAL_DEFAULT = 80

STACK_NAME = '{{cluster-env/stack_name}}'
STACK_ROOT = '{{cluster-env/stack_root}}'

def get_tokens():
  """
  Returns a tuple of tokens in the format {{site/property}} that will be used
  to build the dictionary passed into execute
  """
  return (STACK_NAME, STACK_ROOT)


@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def execute(configurations={}, parameters={}, host_name=None):
  """
  Performs advanced disk checks under Linux. This will first attempt to
  check the HDP installation directories if they exist. If they do not exist,
  it will default to checking /

  Returns a tuple containing the result code and a pre-formatted result label

  Keyword arguments:
  configurations (dictionary): a mapping of configuration key to value
  parameters (dictionary): a mapping of script parameter key to value
  host_name (string): the name of this host where the alert is running
  """

  if configurations is None:
    return (('UNKNOWN', ['There were no configurations supplied to the script.']))

  if not STACK_NAME in configurations or not STACK_ROOT in configurations:
    return (('UNKNOWN', ['cluster-env/stack_name and cluster-env/stack_root are required']))

  path = stack_tools.get_stack_root(configurations[STACK_NAME], configurations[STACK_ROOT])

  try:
    disk_usage = _get_disk_usage(path)
    result_code, label = _get_warnings_for_partition(parameters, disk_usage)
  except NotImplementedError, platform_error:
    return 'CRITICAL', [str(platform_error)]

  return result_code, [label]


def _get_warnings_for_partition(parameters, disk_usage):

  # start with hard coded defaults
  min_free_space = MIN_FREE_SPACE_DEFAULT
  warning_percent = PERCENT_USED_WARNING_DEFAULT
  critical_percent = PERCENT_USED_CRITICAL_DEFAULT

  # parse script parameters
  if MIN_FREE_SPACE_KEY in parameters:
    # long(float(5e9)) seems like gson likes scientific notation
    min_free_space = long(float(parameters[MIN_FREE_SPACE_KEY]))

  if PERCENT_USED_WARNING_KEY in parameters:
    warning_percent = float(parameters[PERCENT_USED_WARNING_KEY])

  if PERCENT_USED_CRITICAL_KEY in parameters:
    critical_percent = float(parameters[PERCENT_USED_CRITICAL_KEY])


  if disk_usage is None or disk_usage.total == 0:
    return 'CRITICAL', ['Unable to determine the disk usage']

  result_code = 'OK'
  percent = disk_usage.used / float(disk_usage.total) * 100
  if percent > critical_percent:
    result_code = 'CRITICAL'
  elif percent > warning_percent:
    result_code = 'WARNING'

  label = 'Capacity Used: [{0:.2f}%, {1}], Capacity Total: [{2}]'.format(
    percent, _get_formatted_size(disk_usage.used),
    _get_formatted_size(disk_usage.total))

  if disk_usage.path is not None:
    label += ", path=" + disk_usage.path

  if result_code == 'OK':
    # Check absolute disk space value
    if disk_usage.free < min_free_space:
      result_code = 'WARNING'
      label += '. Total free space is less than {0}'.format(_get_formatted_size(min_free_space))

  return result_code, label


@OsFamilyFuncImpl(os_family=OSConst.WINSRV_FAMILY)
def execute(configurations={}, parameters={}, host_name=None):
  """
  Performs simplified disk checks under Windows
  Returns a tuple containing the result code and a pre-formatted result label

  Keyword arguments:
  configurations (dictionary): a mapping of configuration key to value
  parameters (dictionary): a mapping of script parameter key to value
  host_name (string): the name of this host where the alert is running
  """
  try:
    disk_usage = _get_disk_usage()
    result = _get_warnings_for_partition(parameters, disk_usage)
  except NotImplementedError, platform_error:
    result = ('CRITICAL', [str(platform_error)])
  return result


@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def _get_disk_usage(path='/'):
  """
  returns a named tuple that contains the total, used, and free disk space
  in bytes. Linux implementation.
  """
  used = 0
  total = 0
  free = 0

  if 'statvfs' in dir(os):
    disk_stats = os.statvfs(path)
    free = disk_stats.f_bavail * disk_stats.f_frsize
    total = disk_stats.f_blocks * disk_stats.f_frsize
    used = (disk_stats.f_blocks - disk_stats.f_bfree) * disk_stats.f_frsize
  else:
    raise NotImplementedError("{0} is not a supported platform for this alert".format(platform.platform()))

  return DiskInfo(total=total, used=used, free=free, path=path)


@OsFamilyFuncImpl(os_family=OSConst.WINSRV_FAMILY)
def _get_disk_usage(path=None):
  """
  returns a named tuple that contains the total, used, and free disk space
  in bytes. Windows implementation
  """
  import string
  import ctypes

  used = 0
  total = 0
  free = 0
  drives = []
  bitmask = ctypes.windll.kernel32.GetLogicalDrives()
  for letter in string.uppercase:
    if bitmask & 1:
      drives.append(letter)
    bitmask >>= 1
  for drive in drives:
    free_bytes = ctypes.c_ulonglong(0)
    total_bytes = ctypes.c_ulonglong(0)
    ctypes.windll.kernel32.GetDiskFreeSpaceExW(ctypes.c_wchar_p(drive + ":\\"),
                                               None, ctypes.pointer(total_bytes),
                                               ctypes.pointer(free_bytes))
    total += total_bytes.value
    free += free_bytes.value
    used += total_bytes.value - free_bytes.value

  return DiskInfo(total=total, used=used, free=free, path=None)


def _get_formatted_size(bytes):
  """
  formats the supplied bytes 
  """
  if bytes < 1000:
    return '%i' % bytes + ' B'
  elif 1000 <= bytes < 1000000:
    return '%.1f' % (bytes / 1000.0) + ' KB'
  elif 1000000 <= bytes < 1000000000:
    return '%.1f' % (bytes / 1000000.0) + ' MB'
  elif 1000000000 <= bytes < 1000000000000:
    return '%.1f' % (bytes / 1000000000.0) + ' GB'
  else:
    return '%.1f' % (bytes / 1000000000000.0) + ' TB'

if __name__ == '__main__':
    print _get_disk_usage(os.getcwd())
