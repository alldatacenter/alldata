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
import alert_disk_space
from mock.mock import patch, MagicMock
from ambari_commons.os_check import OSCheck
from stacks.utils.RMFTestCase import *

from only_for_platform import get_platform, not_for_platform, os_distro_value, PLATFORM_WINDOWS

if get_platform() != PLATFORM_WINDOWS:
  from pwd import getpwnam

class TestAlertDiskSpace(RMFTestCase):

  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
  @patch('alert_disk_space._get_disk_usage')
  @patch("os.path.isdir")
  @patch.object(OSCheck, "get_os_family", new = MagicMock(return_value = 'redhat'))
  def test_linux_flow(self, isdir_mock, disk_usage_mock):
    isdir_mock.return_value = False

    # / OK
    disk_usage_mock.return_value = alert_disk_space.DiskInfo(
      total = 21673930752L, used = 5695861760L,
      free = 15978068992L, path="/")

    configurations = {'{{cluster-env/stack_name}}': 'HDP',
      '{{cluster-env/stack_root}}': '{"HDP":"/usr/hdp"}'}

    res = alert_disk_space.execute(configurations=configurations)

    self.assertEqual(res,
      ('OK', ['Capacity Used: [26.28%, 5.7 GB], Capacity Total: [21.7 GB], path=/']))

    # / WARNING
    disk_usage_mock.return_value = alert_disk_space.DiskInfo(
      total = 21673930752L, used = 14521533603L,
      free = 7152397149L, path="/")

    res = alert_disk_space.execute(configurations = configurations)
    self.assertEqual(res, (
      'WARNING',
      ['Capacity Used: [67.00%, 14.5 GB], Capacity Total: [21.7 GB], path=/']))

    # / CRITICAL
    disk_usage_mock.return_value = alert_disk_space.DiskInfo(
      total = 21673930752L, used = 20590234214L,
      free = 1083696538, path="/")

    res = alert_disk_space.execute(configurations = configurations)
    self.assertEqual(res, ('CRITICAL',
    ['Capacity Used: [95.00%, 20.6 GB], Capacity Total: [21.7 GB], path=/']))

    # / OK but < 5GB
    disk_usage_mock.return_value = alert_disk_space.DiskInfo(
      total = 5418482688L, used = 1625544806L,
      free = 3792937882L, path="/")

    res = alert_disk_space.execute(configurations = configurations)
    self.assertEqual(res, ('WARNING', [
      'Capacity Used: [30.00%, 1.6 GB], Capacity Total: [5.4 GB], path=/. Total free space is less than 5.0 GB']))

    # trigger isdir(<stack-root>) to True
    isdir_mock.return_value = True

    # / OK
    disk_usage_mock.return_value = alert_disk_space.DiskInfo(
      total = 21673930752L, used = 5695861760L,
      free = 15978068992L, path="/usr/hdp")

    res = alert_disk_space.execute(configurations = configurations)
    self.assertEqual(res,
      ('OK', ['Capacity Used: [26.28%, 5.7 GB], Capacity Total: [21.7 GB], path=/usr/hdp']))

    # <stack-root> < 5GB
    disk_usage_mock.return_value = alert_disk_space.DiskInfo(
      total = 5418482688L, used = 1625544806L,
      free = 3792937882L, path="/usr/hdp")

    res = alert_disk_space.execute(configurations = configurations)
    self.assertEqual(res, (
      'WARNING', ["Capacity Used: [30.00%, 1.6 GB], Capacity Total: [5.4 GB], path=/usr/hdp. Total free space is less than 5.0 GB"]))
