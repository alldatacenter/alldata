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
from unittest import TestCase
from mock.mock import patch

from resource_management.libraries.functions import file_system
import resource_management.core.providers.mount
from resource_management.core.logger import Logger


class TestFileSystem(TestCase):
  """
  Test the functionality of the file_system.py file that checks for the mount point of a path.
  """

  class MOUNT_TYPE:
    SINGLE_ROOT = 1
    MULT_DRIVE_CONFLICT = 2
    MULT_DRIVE_DISTINCT = 3
    ONE_SEGMENT_MOUNT = 4
    SAME_PREFIX_MOUNTS = 5

  def _get_mount(self, type):
    """
    /hadoop/hdfs/data will always be on the root

    If the type is MULT_DRIVE_CONFLICT:
    /hadoop/hdfs/data/1 is on /dev/sda1
    /hadoop/hdfs/data/2 is on /dev/sda1

    If the type is MULT_DRIVE_DISTINCT:
    /hadoop/hdfs/data/1 is on /dev/sda1
    /hadoop/hdfs/data/2 is on /dev/sda2
    """
    out = "/dev/mapper/VolGroup-lv_root on / type ext4 (rw)" + os.linesep + \
          "proc on /proc type proc (rw)" + os.linesep + \
          "sysfs on /sys type sysfs (rw)" + os.linesep + \
          "devpts on /dev/pts type devpts (rw,gid=5,mode=620)" + os.linesep + \
          "tmpfs on /dev/shm type tmpfs (rw)" + os.linesep + \
          "/dev/sda1 on /boot type ext4 (rw)" + os.linesep + \
          "none on /proc/sys/fs/binfmt_misc type binfmt_misc (rw)" + os.linesep + \
          "sunrpc on /var/lib/nfs/rpc_pipefs type rpc_pipefs (rw)" + os.linesep + \
          "/vagrant on /vagrant type vboxsf (uid=501,gid=501,rw)"

    if type == self.MOUNT_TYPE.MULT_DRIVE_CONFLICT:
      out += os.linesep + \
             "/dev/sda1 on /hadoop/hdfs type ext4 (rw)"
    elif type == self.MOUNT_TYPE.MULT_DRIVE_DISTINCT:
      out += os.linesep + \
             "/dev/sda1 on /hadoop/hdfs/data/1 type ext4 (rw)" + os.linesep + \
             "/dev/sda2 on /hadoop/hdfs/data/2 type ext4 (rw)"
    elif type == self.MOUNT_TYPE.ONE_SEGMENT_MOUNT:
      out += os.linesep + \
             "/dev/sda1 on /hadoop type ext4 (rw)"
    elif type == self.MOUNT_TYPE.SAME_PREFIX_MOUNTS:
      out += os.linesep + \
             "/dev/sda1 on /hadoop/hdfs/data type ext4 (rw)" + os.linesep + \
             "/dev/sda2 on /hadoop/hdfs/data1 type ext4 (rw)"

    out_array = [x.split(' ') for x in out.strip().split('\n')]
    mount_val = []
    for m in out_array:
      if len(m) >= 6 and m[1] == "on" and m[3] == "type":
        x = dict(
          device=m[0],
          mount_point=m[2],
          fstype=m[4],
          options=m[5][1:-1].split(',') if len(m[5]) >= 2 else []
        )
        mount_val.append(x)

    return mount_val

  @patch.object(Logger, "info")
  @patch.object(Logger, "error")
  def test_invalid(self, log_error, log_info):
    """
    Testing when parameters are invalid or missing.
    """
    mount_point = file_system.get_mount_point_for_dir(None)
    self.assertEqual(mount_point, None)

    mount_point = file_system.get_mount_point_for_dir("")
    self.assertEqual(mount_point, None)

    mount_point = file_system.get_mount_point_for_dir("  ")
    self.assertEqual(mount_point, None)

  @patch.object(Logger, "info")
  @patch.object(Logger, "error")
  @patch('resource_management.core.providers.mount.get_mounted')
  def test_at_root(self, mounted_mock, log_error, log_info):
    """
    Testing when the path is mounted on the root.
    """
    mounted_mock.return_value = self._get_mount(self.MOUNT_TYPE.SINGLE_ROOT)

    # refresh cached mounts
    file_system.get_and_cache_mount_points(True)

    mount_point = file_system.get_mount_point_for_dir("/hadoop/hdfs/data")
    self.assertEqual(mount_point, "/")

  @patch.object(Logger, "info")
  @patch.object(Logger, "error")
  @patch('resource_management.core.providers.mount.get_mounted')
  def test_at_drive(self, mounted_mock, log_error, log_info):
    """
    Testing when the path is mounted on a virtual file system not at the root.
    """
    mounted_mock.return_value = self._get_mount(self.MOUNT_TYPE.MULT_DRIVE_DISTINCT)

    # refresh cached mounts
    file_system.get_and_cache_mount_points(True)

    mount_point = file_system.get_mount_point_for_dir("/hadoop/hdfs/data/1")
    self.assertEqual(mount_point, "/hadoop/hdfs/data/1")

    mount_point = file_system.get_mount_point_for_dir("/hadoop/hdfs/data/2")
    self.assertEqual(mount_point, "/hadoop/hdfs/data/2")

  @patch.object(Logger, "info")
  @patch.object(Logger, "error")
  @patch('resource_management.core.providers.mount.get_mounted')
  def test_one_segment_mount(self, mounted_mock, log_error, log_info):
    """
    Testing when the path has one segment.
    """
    mounted_mock.return_value = self._get_mount(self.MOUNT_TYPE.ONE_SEGMENT_MOUNT)

    # refresh cached mounts
    file_system.get_and_cache_mount_points(True)

    mount_point = file_system.get_mount_point_for_dir("/hadoop/hdfs/data/1")
    self.assertEqual(mount_point, "/hadoop")

  @patch.object(Logger, "info")
  @patch.object(Logger, "error")
  @patch('resource_management.core.providers.mount.get_mounted')
  def test_same_prefix(self, mounted_mock, log_error, log_info):
    """
    Testing when two mount points have the same prefix.
    """
    mounted_mock.return_value = self._get_mount(self.MOUNT_TYPE.SAME_PREFIX_MOUNTS)

    # refresh cached mounts
    file_system.get_and_cache_mount_points(True)

    mount_point = file_system.get_mount_point_for_dir("/hadoop/hdfs/data1")
    self.assertEqual(mount_point, "/hadoop/hdfs/data1")

  @patch.object(Logger, "info")
  @patch.object(Logger, "error")
  @patch('resource_management.core.providers.mount.get_mounted')
  def test_get_mount_point_for_dir_with_mounsts(self, mounted_mock, log_error, log_info):
      """
      Testing get_mount_point_for_dir when mounsts are provided.
      """
      mounted_mock.return_value = self._get_mount(self.MOUNT_TYPE.SAME_PREFIX_MOUNTS)

      # refresh cached mounts
      file_system.get_and_cache_mount_points(True)

      mount_point = file_system.get_mount_point_for_dir("/hadoop/hdfs/data1")
      self.assertEqual(mount_point, "/hadoop/hdfs/data1")

      # Should use provided mounts, not fetch via get_and_cache_mount_points
      mount_point = file_system.get_mount_point_for_dir("/hadoop/hdfs/data1", ["/", "/hadoop"])
      self.assertEqual(mount_point, "/hadoop")
