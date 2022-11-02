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

import os.path
import logging
from ambari_commons import subprocess32
from resource_management.core import shell
from resource_management.core.shell import call
from resource_management.core.exceptions import ExecuteTimeoutException, Fail
from ambari_commons.shell import shellRunner
from Facter import Facter
from ambari_commons.os_check import OSConst
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl
from AmbariConfig import AmbariConfig
from resource_management.core.sudo import path_isfile

logger = logging.getLogger()


class Hardware:
  SSH_KEY_PATTERN = 'ssh.*key'
  WINDOWS_GET_DRIVES_CMD = "foreach ($drive in [System.IO.DriveInfo]::getdrives()){$available = $drive.TotalFreeSpace;$used = $drive.TotalSize-$drive.TotalFreeSpace;$percent = ($used*100)/$drive.TotalSize;$size = $drive.TotalSize;$type = $drive.DriveFormat;$mountpoint = $drive.RootDirectory.FullName;echo \"$available $used $percent% $size $type $mountpoint\"}"
  CHECK_REMOTE_MOUNTS_KEY = 'agent.check.remote.mounts'
  CHECK_REMOTE_MOUNTS_TIMEOUT_KEY = 'agent.check.mounts.timeout'
  CHECK_REMOTE_MOUNTS_TIMEOUT_DEFAULT = '10'
  IGNORE_ROOT_MOUNTS = ["proc", "dev", "sys", "boot", "home"]
  IGNORE_DEVICES = ["proc", "tmpfs", "cgroup", "mqueue", "shm"]
  LINUX_PATH_SEP = "/"

  def __init__(self, config=None, cache_info=True):
    """
    Initialize hardware object with available metrics. Metrics cache could be
     disabled by setting cache_info to False

    :param config Ambari Agent Configuration
    :param cache_info initialize hardware dictionary with available metrics

    :type config AmbariConfig
    :type cache_info bool
    """
    self.config = config
    self._hardware = None

    if cache_info:
      self._cache_hardware_info()

  def _cache_hardware_info(self):
    """
    Creating cache with hardware information
    """
    logger.info("Initializing host system information.")
    self._hardware = {
      'mounts': self.osdisks()
    }
    self._hardware.update(Facter(self.config).facterInfo())
    logger.info("Host system information: %s", self._hardware)

  def _parse_df(self, lines):
    """
      Generator, which parses df command output and yields parsed entities

      Expected string format:
       device fs_type disk_size used_size available_size capacity_used_percents mount_point

    :type lines list[str]
    :rtype collections.Iterable
    """
    titles = ["device", "type", "size", "used", "available", "percent", "mountpoint"]

    for line in lines:
      line_split = line.split()
      if len(line_split) != 7:
        continue

      yield dict(zip(titles, line_split))

  def _get_mount_check_timeout(self):
    """Return timeout for df call command"""
    if self.config and self.config.has_option(AmbariConfig.AMBARI_PROPERTIES_CATEGORY, Hardware.CHECK_REMOTE_MOUNTS_TIMEOUT_KEY) \
      and self.config.get(AmbariConfig.AMBARI_PROPERTIES_CATEGORY, Hardware.CHECK_REMOTE_MOUNTS_TIMEOUT_KEY) != "0":

      return self.config.get(AmbariConfig.AMBARI_PROPERTIES_CATEGORY, Hardware.CHECK_REMOTE_MOUNTS_TIMEOUT_KEY)

    return Hardware.CHECK_REMOTE_MOUNTS_TIMEOUT_DEFAULT

  def _check_remote_mounts(self):
    """Verify if remote mount allowed to be processed or not"""
    if self.config and self.config.has_option(AmbariConfig.AMBARI_PROPERTIES_CATEGORY, Hardware.CHECK_REMOTE_MOUNTS_KEY) and \
      self.config.get(AmbariConfig.AMBARI_PROPERTIES_CATEGORY, Hardware.CHECK_REMOTE_MOUNTS_KEY).lower() == "true":

      return True

    return False

  def _is_mount_blacklisted(self, blacklist, mount_point):
    """
    Verify if particular mount point is in the black list.

    :return True if mount_point or a part of mount point is in the blacklist, otherwise return False

     Example:
       Mounts: /, /mnt/my_mount, /mnt/my_mount/sub_mount
       Blacklist: /mnt/my_mount
       Result: /

    :type blacklist list
    :type mount_point str
    :rtype bool
    """

    if not blacklist or not mount_point:
      return False

    # in this way we excluding possibility
    mount_point_elements = mount_point.split(self.LINUX_PATH_SEP)

    for el in blacklist:
      el_list = el.split(self.LINUX_PATH_SEP)
      # making patch elements comparision
      if el_list == mount_point_elements[:len(el_list)]:
        return True

    return False

  @OsFamilyFuncImpl(OsFamilyImpl.DEFAULT)
  def osdisks(self):
    """ Run df to find out the disks on the host. Only works on linux
    platforms. Note that this parser ignores any filesystems with spaces
    and any mounts with spaces. """
    timeout = self._get_mount_check_timeout()
    command = ["timeout", timeout, "df", "-kPT"]
    blacklisted_mount_points = []

    if self.config:
      ignore_mount_value = self.config.get("agent", "ignore_mount_points", default="")
      blacklisted_mount_points = [item.strip() for item in ignore_mount_value.split(",") if len(item.strip()) != 0]

    if not self._check_remote_mounts():
      command.append("-l")

    try:
      code, out, err = shell.call(command, stdout=subprocess32.PIPE, stderr=subprocess32.PIPE, timeout=int(timeout), quiet=True)
      dfdata = out
    except Exception as ex:
      logger.warn("Checking disk usage failed: " + str(ex))
      dfdata = ''

    result_mounts = []
    ignored_mounts = []

    for mount in self._parse_df(dfdata.splitlines()):
      """
      We need to filter mounts by several parameters:
       - mounted device is not in the ignored list
       - is accessible to user under which current process running
       - it is not file-mount (docker environment)
       - mount path or a part of mount path is not in the blacklist
      """
      if mount["device"] not in self.IGNORE_DEVICES and\
         mount["mountpoint"].strip()[1:].split("/")[0] not in self.IGNORE_ROOT_MOUNTS and\
         self._chk_writable_mount(mount['mountpoint']) and\
         not path_isfile(mount["mountpoint"]) and\
         not self._is_mount_blacklisted(blacklisted_mount_points, mount["mountpoint"]):

        result_mounts.append(mount)
      else:
        ignored_mounts.append(mount)

    if len(ignored_mounts) > 0:
      ignore_list = [el["mountpoint"] for el in ignored_mounts]
      logger.info("Some mount points were ignored: {0}".format(', '.join(ignore_list)))

    return result_mounts

  def _chk_writable_mount(self, mount_point):
    if os.geteuid() == 0:
      return os.access(mount_point, os.W_OK)
    else:
      try:
        # test if mount point is writable for current user
        call_result = call(['test', '-w', mount_point],
                           sudo=True,
                           timeout=int(Hardware.CHECK_REMOTE_MOUNTS_TIMEOUT_DEFAULT) / 2,
                           quiet=not logger.isEnabledFor(logging.DEBUG))
        return call_result and call_result[0] == 0
      except ExecuteTimeoutException:
        logger.exception("Exception happened while checking mount {0}".format(mount_point))
        return False
      except Fail:
        logger.exception("Exception happened while checking mount {0}".format(mount_point))
        return False
    
  @OsFamilyFuncImpl(OSConst.WINSRV_FAMILY)
  def osdisks(self):
    mounts = []
    runner = shellRunner()
    command_result = runner.runPowershell(script_block=Hardware.WINDOWS_GET_DRIVES_CMD)
    if command_result.exitCode != 0:
      return mounts
    else:
      for drive in [line for line in command_result.output.split(os.linesep) if line != '']:
        available, used, percent, size, fs_type, mountpoint = drive.split(" ")
        mounts.append({"available": available,
                       "used": used,
                       "percent": percent,
                       "size": size,
                       "type": fs_type,
                       "mountpoint": mountpoint})

    return mounts

  def get(self, invalidate_cache=False):
    """
    Getting cached hardware information

    :param invalidate_cache resets hardware metrics cache
    :type invalidate_cache bool
    """
    if invalidate_cache:
      self._hardware = None

    if not self._hardware:
      self._cache_hardware_info()

    return self._hardware


def main():
  from resource_management.core.logger import Logger
  Logger.initialize_logger()

  print Hardware().get()


if __name__ == '__main__':
  main()
